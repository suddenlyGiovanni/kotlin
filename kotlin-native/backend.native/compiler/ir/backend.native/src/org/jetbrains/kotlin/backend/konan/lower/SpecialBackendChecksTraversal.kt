/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.Closure
import org.jetbrains.kotlin.backend.common.lower.ClosureAnnotator
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cgen.*
import org.jetbrains.kotlin.backend.konan.checkers.EscapeAnalysisChecker
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.allOverriddenFunctions
import org.jetbrains.kotlin.backend.konan.ir.getSuperClassNotAny
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.isClass
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NativeStandardInteropNames.objCActionClassId
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.File

/**
 * Kotlin/Native-specific language checks. Most importantly, it checks C/Objective-C interop restrictions.
 * TODO: Should be moved to compiler frontend after K2.
 */
internal class SpecialBackendChecksTraversal(
        private val context: PhaseContext,
        private val symbols: KonanSymbols,
        private val irBuiltIns: IrBuiltIns,
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(BackendChecker(context, symbols, irBuiltIns, irFile))
        // EscapeAnalysisChecker only makes sense when compiling stdlib.
        irFile.acceptChildrenVoid(EscapeAnalysisChecker(context, symbols, irFile))
    }
}

private class BackendChecker(
        private val context: PhaseContext,
        val symbols: KonanSymbols,
        val irBuiltIns: IrBuiltIns,
        private val irFile: IrFile,
) : IrVisitorVoid() {
    val target = context.config.target

    fun reportError(location: IrElement, message: String): Nothing =
            context.reportCompilationError(message, irFile, location)

    private val outerDeclarations = mutableListOf<IrDeclaration>()

    private val outerClass: IrClass? get() = outerDeclarations.last { it is IrClass } as? IrClass
    private val outerFunction: IrFunction? get() = outerDeclarations.last { it is IrFunction } as? IrFunction

    private val outerAnnotators = mutableListOf<Lazy<ClosureAnnotator>>()
    private val functionAnnotators = mutableMapOf<IrFunction, Lazy<ClosureAnnotator>>()
    private val functionClosures = mutableMapOf<IrFunction, Closure>()

    private fun captures(function: IrFunction): List<IrValueDeclaration> {
        if (function.visibility != DescriptorVisibilities.LOCAL) return emptyList()
        val closure = functionClosures.getOrPut(function) {
            functionAnnotators[function]!!.value.getFunctionClosure(function)
        }
        return closure.capturedValues.map { it.owner }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        outerDeclarations.push(declaration)
        try {
            super.visitDeclaration(declaration)
        } finally {
            outerDeclarations.pop()
        }
    }

    override fun visitBody(body: IrBody) {
        val declaration = outerDeclarations.peek()!!
        if ((declaration as? IrFunction)?.visibility == DescriptorVisibilities.LOCAL) {
            functionAnnotators[declaration] = outerAnnotators.peek()!!
            super.visitBody(body)
            return
        }

        outerAnnotators.push(lazy { ClosureAnnotator(body, declaration) })
        try {
            super.visitBody(body)
        } finally {
            outerAnnotators.pop()
        }
    }

    private fun IrConstructor.isOverrideInit() =
            this.annotations.hasAnnotation(InteropFqNames.objCOverrideInit)

    // Already migrated to FIR Checker: FirNativeObjCOverrideInitChecker.checkCanGenerateOverrideInit()
    private fun checkCanGenerateOverrideInit(irClass: IrClass, constructor: IrConstructor) {
        val superClass = irClass.getSuperClassNotAny()!!
        val superConstructors = superClass.constructors.filter {
            constructor.overridesConstructor(it)
        }.toList()

        val superConstructor = superConstructors.singleOrNull() ?: run {
            val annotation = InteropFqNames.objCOverrideInit
            if (superConstructors.isEmpty())
                reportError(constructor,
                        """
                            constructor with @$annotation doesn't override any super class constructor.
                            It must completely match by parameter names and types.""".trimIndent()
                )
            else
                reportError(constructor,
                        "constructor with @$annotation matches more than one of super constructors"
                )
        }

        val initMethod = superConstructor.getObjCInitMethod()!!

        // Remove fake overrides of this init method, also check for explicit overriding:
        irClass.declarations.forEach {
            if (it is IrSimpleFunction && initMethod.symbol in it.overriddenSymbols && it.isReal) {
                val annotation = InteropFqNames.objCOverrideInit
                reportError(constructor,
                        "constructor with @$annotation overrides initializer that is already overridden explicitly"
                )
            }
        }
    }

    private fun IrConstructor.overridesConstructor(other: IrConstructor) =
            this.parameters.size == other.parameters.size &&
                    this.parameters.zip(other.parameters).all { (l, r) ->
                        l.name == r.name && l.type == r.type
                    }

    // Already migrated to FIR Checker: FirNativeObjCActionChecker.checkCanGenerateActionImp()
    private fun checkCanGenerateActionImp(function: IrSimpleFunction) {
        val action = "@${objCActionClassId.asFqNameString()}"

        function.parameters.filter { it.kind == IrParameterKind.ExtensionReceiver || it.kind == IrParameterKind.Context }.forEach {
            reportError(it, "$action method must not have ${it.kind} parameters")
        }

        function.parameters.filter { it.kind == IrParameterKind.Regular }.forEach {
            val kotlinType = it.type
            if (!kotlinType.isObjCObjectType())
                reportError(it, "Unexpected $action method parameter type: ${it.type.classFqName}\n" +
                        "Only Objective-C object types are supported here")
        }

        val returnType = function.returnType
        if (!returnType.isUnit())
            reportError(function, "Unexpected $action method return type: ${returnType.classFqName}\n" +
                    "Only 'Unit' is supported here")

        checkCanGenerateFunctionImp(function)
    }

    // Already migrated to FIR Checker: FirNativeObjCOutletChecker.checkCanGenerateOutletSetterImp()
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun checkCanGenerateOutletSetterImp(property: IrProperty) {

        val outlet = "@${InteropFqNames.objCOutlet}"

        if (!property.isVar)
            reportError(property, "$outlet property must be var")

        property.getter?.parameters?.filter { it.kind == IrParameterKind.ExtensionReceiver || it.kind == IrParameterKind.Context }?.forEach {
            reportError(it, "$outlet must not have ${it.kind} parameters")
        }

        val type = property.descriptor.type
        if (!type.isObjCObjectType())
            reportError(property, "Unexpected $outlet type: ${property.getter?.returnType?.classFqName}\n" +
                    "Only Objective-C object types are supported here")

        checkCanGenerateFunctionImp(property.setter!!)
    }

    private fun checkCanGenerateFunctionImp(function: IrFunction) {
        if (function.nonDispatchParameters.size > 2)
            reportError(function, "Only 0, 1 or 2 parameters are supported here")
    }

    private fun IrClass.hasFields() =
            this.declarations.any {
                when (it) {
                    is IrField -> it.isReal
                    is IrProperty -> it.isReal && it.backingField != null
                    else -> false
                }
            }

    private fun checkKotlinObjCClass(irClass: IrClass) {
        for (declaration in irClass.declarations) {
            if (declaration is IrSimpleFunction && declaration.annotations.hasAnnotation(objCActionClassId.asSingleFqName()))
                checkCanGenerateActionImp(declaration)
            if (declaration is IrProperty && declaration.annotations.hasAnnotation(InteropFqNames.objCOutlet))
                checkCanGenerateOutletSetterImp(declaration)
            if (declaration is IrConstructor && declaration.isOverrideInit())
                checkCanGenerateOverrideInit(irClass, declaration)
            if (declaration is IrSimpleFunction && declaration.isReal) {
                for (overriddenSymbol in declaration.overriddenSymbols)
                    overriddenSymbol.owner.getExternalObjCMethodInfo()?.selector?.let {
                        checkCanGenerateCFunction(
                                function = declaration,
                                signature = overriddenSymbol.owner,
                                isObjCMethod = true,
                                location = declaration
                        )
                    }
            }
        }

        val kind = irClass.kind
        if (kind != ClassKind.CLASS && kind != ClassKind.OBJECT)
            reportError(irClass, "Only classes are supported as subtypes of Objective-C types")

        if (!irClass.isFinalClass)
            reportError(irClass, "Non-final Kotlin subclasses of Objective-C classes are not yet supported")

        irClass.companionObject()?.let {
            if (it.hasFields() || it.getSuperClassNotAny()?.hasFields() == true)
                reportError(irClass, "Fields are not supported for Companion of subclass of ObjC type")
        }

        if (irClass.isObjCMetaClass() && (irClass.isCompanion && !irClass.parentAsClass.isKotlinObjCClass() || irClass.kind.isClass)) {
            reportError(irClass, "Only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses")
        }

        var hasObjCClassSupertype = false
        irClass.defaultType.superTypes().forEach {
            val clazz = it.classOrFail.owner
            if (!clazz.isObjCClass())
                reportError(irClass, "Mixing Kotlin and Objective-C supertypes is not supported")

            if (clazz.kind == ClassKind.CLASS)
                hasObjCClassSupertype = true
        }

        if (!hasObjCClassSupertype)
            reportError(irClass, "Kotlin implementation of Objective-C protocol must have Objective-C superclass (e.g. NSObject)")

        val methodsOfAny = symbols.any.owner.declarations.filterIsInstance<IrSimpleFunction>().toSet()

        irClass.declarations.filterIsInstance<IrSimpleFunction>().filter { it.isReal }.forEach { method ->
            val overriddenMethodOfAny = method.allOverriddenFunctions.firstOrNull {
                it in methodsOfAny
            }

            if (overriddenMethodOfAny != null) {
                val correspondingObjCMethod = when (method.name.asString()) {
                    "toString" -> "'description'"
                    "hashCode" -> "'hash'"
                    "equals" -> "'isEqual:'"
                    else -> "corresponding Objective-C method"
                }

                reportError(
                        method,
                        "can't override '${method.name}', override $correspondingObjCMethod instead",
                )
            }
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        expression.acceptChildrenVoid(this)

        val constructedClass = outerClass!!

        if (!constructedClass.isObjCClass())
            return

        constructedClass.parent.let { parent ->
            if (parent is IrClass && parent.isObjCClass() && constructedClass.isCompanion) {
                // Note: it is actually not used; getting values of such objects is handled by code generator
                // in [FunctionGenerationContext.getObjectValue].

                return
            }
        }

        val delegatingCallConstructingClass = expression.symbol.owner.constructedClass
        if (!constructedClass.isExternalObjCClass() &&
                delegatingCallConstructingClass.isExternalObjCClass()) {
            // Calling super constructor from Kotlin Objective-C class.

            expression.symbol.owner.getObjCInitMethod()?.let { initMethod ->

                if (!expression.symbol.owner.objCConstructorIsDesignated())
                    reportError(expression, "Unable to call non-designated initializer as super constructor")

                checkCanGenerateObjCCall(
                        method = initMethod,
                        call = expression,
                        arguments = expression.arguments
                )
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        expression.acceptChildrenVoid(this)

        val callee = expression.symbol.owner
        val initMethod = callee.getObjCInitMethod()
        if (initMethod != null) {
            checkCanGenerateObjCCall(
                    method = initMethod,
                    call = expression,
                    arguments = expression.arguments
            )
        }

        if (callee.returnType.isNativePointed(symbols)) {
            reportError(expression, "Native interop types constructors must not be called directly")
        }
    }

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)
        if (declaration.isFakeOverride) return // Can't happen now, just trying to be future-proof here.

        val parent = declaration.parent

        if (parent is IrClass && parent.defaultType.isNativePointed(symbols) && parent.symbol != symbols.nativePointed) {
            reportError(declaration, "Subclasses of ${InteropFqNames.nativePointed} cannot have properties with backing fields")
        }
    }

    private val cwd = File(".").absoluteFile

    private fun reportBoundFunctionReferenceError(expression: IrExpression, callee: IrFunction, captures: List<IrExpression>): Nothing {
        val formattedCaptures = if (captures.isEmpty())
            ""
        else ", but captures at:\n    ${captures.joinToString("\n    ") {
            val location = it.getCompilerMessageLocation(irFile)!!
            val relativePath = File(location.path).descendantRelativeTo(cwd).path
            val capturedValueName = if (it is IrGetValue) ": ${it.symbol.owner.name.asString()}" else ""
            "$relativePath:${location.line}:${location.column}$capturedValueName"
        }}"

        reportError(expression,
                "${callee.fqNameForIrSerialization} must take an unbound, non-capturing function or lambda$formattedCaptures")
    }

    override fun visitCall(expression: IrCall) {
        expression.acceptChildrenVoid(this)

        val callee = expression.symbol.owner

        callee.getObjCFactoryInitMethodInfo()?.let { _ ->
            checkCanGenerateObjCCall(
                    method = callee,
                    call = expression,
                    arguments = expression.arguments
            )
        }

        callee.getExternalObjCMethodInfo()?.let { _ ->
            val isInteropStubsFile = irFile.annotations.hasAnnotation(InteropFqNames.interopStubs)

            // Special case: bridge from Objective-C method implementation template to Kotlin method;
            // handled in CodeGeneratorVisitor.callVirtual.
            val useKotlinDispatch = isInteropStubsFile &&
                    outerFunction!!.annotations.hasAnnotation(FqName("kotlin.native.internal.ExportForCppRuntime"))

            if (!useKotlinDispatch) {
                if (expression.superQualifierSymbol?.owner?.isObjCMetaClass() == true)
                    reportError(expression, "Super calls to Objective-C meta classes are not supported yet")

                if (expression.superQualifierSymbol?.owner?.isInterface == true)
                    reportError(expression, "Super calls to Objective-C protocols are not allowed")

                checkCanGenerateObjCCall(
                        method = callee,
                        call = expression,
                        arguments = expression.arguments
                )
            }
        }

        if (callee.annotations.hasAnnotation(RuntimeNames.cCall))
            checkCanGenerateCCall(expression, isInvoke = false)

        when (val intrinsicType = tryGetIntrinsicType(expression)) {
            IntrinsicType.INTEROP_STATIC_C_FUNCTION -> {
                val (target, captures) = getUnboundReferencedFunction(expression.arguments[0]!!)

                if (target == null || target.symbol !is IrSimpleFunctionSymbol)
                    reportBoundFunctionReferenceError(expression, callee, captures)

                val signatureTypes = target.parameters.map { it.type } + target.returnType

                callee.typeParameters.indices.forEach { index ->
                    val typeArgument = expression.typeArguments[index]!!
                    val signatureType = signatureTypes[index]
                    if (typeArgument.classifierOrNull != signatureType.classifierOrNull ||
                            typeArgument.isMarkedNullable() != signatureType.isMarkedNullable()
                    ) {
                        reportError(expression, "C function signature element mismatch: " +
                                "expected '${signatureTypes[index].classFqName}', got '${expression.typeArguments[index]!!.classFqName}'")
                    }
                }

                checkCanGenerateCFunctionPointer(target as IrSimpleFunction, expression)
            }
            IntrinsicType.INTEROP_FUNPTR_INVOKE -> {
                checkCanGenerateCCall(expression, isInvoke = true)
            }
            IntrinsicType.INTEROP_SIGN_EXTEND, IntrinsicType.INTEROP_NARROW -> {

                val integerTypePredicates = arrayOf(
                        IrType::isByte, IrType::isShort, IrType::isInt, IrType::isLong
                )

                val receiver = expression.arguments[0]!!
                val typeOperand = expression.getSingleTypeArgument()

                val receiverTypeIndex = integerTypePredicates.indexOfFirst { it(receiver.type) }
                val typeOperandIndex = integerTypePredicates.indexOfFirst { it(typeOperand) }

                if (receiverTypeIndex == -1)
                    reportError(receiver, "Receiver's type ${receiver.type.classFqName} is not an integer type")

                if (typeOperandIndex == -1)
                    reportError(expression, "Type argument ${typeOperand.classFqName} is not an integer type")

                when (intrinsicType) {
                    IntrinsicType.INTEROP_SIGN_EXTEND -> if (receiverTypeIndex > typeOperandIndex)
                        reportError(expression, "unable to sign extend ${receiver.type.classFqName} to ${typeOperand.classFqName}")

                    IntrinsicType.INTEROP_NARROW -> if (receiverTypeIndex < typeOperandIndex)
                        reportError(expression, "unable to narrow ${receiver.type.classFqName} to ${typeOperand.classFqName}")
                }
            }
            IntrinsicType.INTEROP_CONVERT -> {
                val integerClasses = symbols.allIntegerClasses
                val typeOperand = expression.typeArguments[0]!!
                val receiverType = expression.symbol.owner.parameters[0].type

                if (typeOperand !is IrSimpleType || typeOperand.classifier !in integerClasses || typeOperand.isNullable())
                    reportError(expression, "unable to convert ${receiverType.classFqName} to ${typeOperand.classFqName}")
            }
            IntrinsicType.WORKER_EXECUTE -> {
                val (function, captures) = getUnboundReferencedFunction(expression.arguments[3]!!)
                if (function == null)
                    reportBoundFunctionReferenceError(expression, callee, captures)
            }
            else -> when {
                callee.symbol == symbols.createCleaner -> {
                    val (function, captures) = getUnboundReferencedFunction(expression.arguments[1]!!)
                    if (function == null)
                        reportBoundFunctionReferenceError(expression, callee, captures)
                }
                callee.symbol == symbols.immutableBlobOf -> {
                    val arg = expression.arguments.single()
                            ?: reportError(expression, "expected at least one element")
                    val varargs = arg as IrVararg
                    val elements = varargs.elements
                    if (elements.any { it is IrSpreadElement })
                        reportError(varargs, "no spread elements allowed here")
                    elements.forEach {
                        if (it !is IrConst)
                            reportError(varargs, "all elements of binary blob must be constants")
                        val value = it.value as Short
                        if (value < 0 || value > 0xff)
                            reportError(it, "incorrect value for binary data: $value")
                    }
                }
                Symbols.isTypeOfIntrinsic(callee.symbol) ->
                    checkIrKType(expression, expression.typeArguments[0]!!)
            }
        }
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression) {
        expression.acceptChildrenVoid(this)

        checkCanReferenceFunction(expression.function, expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        expression.acceptChildrenVoid(this)

        checkCanReferenceFunction(expression.symbol.owner, expression)
    }

    private fun checkCanReferenceFunction(callee: IrFunction, expression: IrExpression) {
        // Corresponds to the check in [KotlinToCCallBuilder.handleArgumentForVarargParameter].
        if (callee.parameters.any { it.isVararg }) {
            if (callee.annotations.hasAnnotation(RuntimeNames.cCall))
                reportError(expression, "callable references to variadic C functions are not supported")
            if (callee is IrConstructor && callee.getObjCInitMethod() != null
                    || callee.getObjCFactoryInitMethodInfo() != null
                    || callee.getExternalObjCMethodInfo() != null
            ) {
                reportError(expression, "callable references to variadic Objective-C methods are not supported")
            }
        }
    }

    private data class ReferencedFunctionWithCapture(val function: IrFunction?, val captures: List<IrExpression>)

    private fun searchForReferences(function: IrFunction, targetValues: Set<IrValueDeclaration>): List<IrExpression> {
        if (targetValues.isEmpty()) return emptyList()

        val result = mutableListOf<IrExpression>()
        function.acceptChildrenVoid(object: IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitGetValue(expression: IrGetValue) {
                if (expression.symbol.owner in targetValues)
                    result += expression
            }
        })
        return result
    }

    private fun getUnboundReferencedFunction(expression: IrExpression) = when (expression) {
        is IrFunctionReference -> {
            val arguments = expression.getArgumentsWithIr()
            val capturedVariables = captures(expression.symbol.owner)
            ReferencedFunctionWithCapture(
                    expression.symbol.owner.takeIf { arguments.isEmpty() && capturedVariables.isEmpty() },
                    arguments.map { it.second } + searchForReferences(expression.symbol.owner, capturedVariables.toSet())
            )
        }
        is IrRichFunctionReference -> {
            val arguments = expression.boundValues
            val capturedVariables = captures(expression.invokeFunction)
            ReferencedFunctionWithCapture(
                    expression.invokeFunction.takeIf { arguments.isEmpty() && capturedVariables.isEmpty() },
                    arguments + searchForReferences(expression.invokeFunction, capturedVariables.toSet())
            )
        }
        is IrFunctionExpression -> {
            val capturedVariables = captures(expression.function)
            ReferencedFunctionWithCapture(
                    expression.function.takeIf { capturedVariables.isEmpty() },
                    searchForReferences(expression.function, capturedVariables.toSet())
            )
        }
        else -> ReferencedFunctionWithCapture(null, emptyList())
    }

    private fun IrCall.getSingleTypeArgument(): IrType {
        val typeParameter = symbol.owner.typeParameters.single()
        return typeArguments[typeParameter.index]!!
    }

    private fun checkIrKType(
            irElement: IrElement,
            type: IrType,
            seenTypeParameters: MutableSet<IrTypeParameter> = mutableSetOf()
    ) {
        if (type !is IrSimpleType)
            return
        val classifier = type.classifier
        if (classifier is IrTypeParameterSymbol
                && !classifier.owner.isReified /* Reified may be substituted with valid types later */)
            checkIrKTypeParameter(irElement, classifier.owner, seenTypeParameters)

        type.arguments.forEach {
            if (it is IrTypeProjection)
                checkIrKType(irElement, it.type, seenTypeParameters)
        }
    }

    private fun checkIrKTypeParameter(
            irElement: IrElement,
            typeParameter: IrTypeParameter,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ) {
        if (!seenTypeParameters.add(typeParameter))
            reportError(irElement, "Non-reified type parameters with recursive bounds are not supported yet: ${typeParameter.render()}")
        typeParameter.superTypes.forEach { checkIrKType(irElement, it, seenTypeParameters) }
        seenTypeParameters.remove(typeParameter)
    }

    override fun visitClass(declaration: IrClass) {
        if (!declaration.isExpect && declaration.isKotlinObjCClass()) {
            checkKotlinObjCClass(declaration)
        }
        super.visitClass(declaration)

    }
}

private fun BackendChecker.checkCanGenerateCCall(expression: IrCall, isInvoke: Boolean) {
    val callee = expression.symbol.owner

    if (isInvoke) {
        for ((idx, param) in callee.parameters.filter { it.kind == IrParameterKind.Regular }.withIndex()) {
            checkCanMapCalleeFunctionParameter(
                    type = expression.typeArguments[idx]!!,
                    isObjCMethod = false,
                    variadic = false,
                    parameter = null,
                    argument = expression.arguments[param]!!)
        }

        val returnType = expression.typeArguments.last()!!
        checkCanMapReturnType(returnType, TypeLocation.FunctionCallResult(expression))
    } else {
        checkCanAddArguments(expression.arguments, callee, isObjCMethod = false)

        checkCanMapReturnType(callee.returnType, TypeLocation.FunctionCallResult(expression))
    }
}

private fun BackendChecker.checkCanAddArguments(arguments: List<IrExpression?>, callee: IrFunction, isObjCMethod: Boolean) {
    for ((argument, parameter) in arguments.zip(callee.parameters)) {
        if (parameter.isVararg)
            checkCanHandleArgumentForVarargParameter(argument, isObjCMethod)
        else
            checkCanMapCalleeFunctionParameter(parameter.type, isObjCMethod, variadic = false, parameter = parameter, argument = argument!!)
    }
}

private fun BackendChecker.checkCanUnwrapVariadicArguments(elements: List<IrVarargElement>, isObjCMethod: Boolean) {
    for (element in elements) when (element) {
        is IrExpression ->
            checkCanMapCalleeFunctionParameter(element.type, isObjCMethod, variadic = true, parameter = null, argument = element)
        is IrSpreadElement -> {
            val expression = element.expression
            if (expression is IrCall && expression.symbol == symbols.arrayOf)
                checkCanHandleArgumentForVarargParameter(expression.arguments[0], isObjCMethod)
            else
                reportError(element, "When calling variadic " +
                        (if (isObjCMethod) "Objective-C methods " else "C functions ") +
                        "spread operator is supported only for *arrayOf(...)")
        }
    }
}

private fun BackendChecker.checkCanHandleArgumentForVarargParameter(argument: IrExpression?, isObjCMethod: Boolean) {
    when (argument) {
        is IrVararg -> checkCanUnwrapVariadicArguments(argument.elements, isObjCMethod)

        is IrGetValue -> {
            /* This is possible when using named arguments with reordering, i.e.
             *
             *   foo(second = *arrayOf(...), first = ...)
             *
             * psi2ir generates as
             *
             *   val secondTmp = *arrayOf(...)
             *   val firstTmp = ...
             *   foo(firstTmp, secondTmp)
             *
             *
             **/

            val variable = argument.symbol.owner
            if (variable is IrVariable && variable.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE && !variable.isVar)
                checkCanUnwrapVariadicArguments((variable.initializer as IrVararg).elements, isObjCMethod)
        }
    }
}

private fun BackendChecker.checkCanGenerateObjCCall(
        method: IrSimpleFunction,
        call: IrFunctionAccessExpression,
        arguments: List<IrExpression?>
) {
    checkCanAddArguments(arguments, method, isObjCMethod = true)

    checkCanMapReturnType(method.returnType, TypeLocation.FunctionCallResult(call))
}

private fun BackendChecker.checkParameter(it: IrValueParameter, functionParameter: IrValueParameter,
                                          isObjCMethod: Boolean, location: IrElement) {
    val typeLocation = if (isObjCMethod)
        TypeLocation.ObjCMethodParameter(functionParameter)
    else
        TypeLocation.FunctionPointerParameter(it.indexInParameters, location)

    if (functionParameter.isVararg)
        reportError(typeLocation.element, if (isObjCMethod) {
            "overriding variadic Objective-C methods is not supported"
        } else {
            "variadic function pointers are not supported"
        })

    checkCanMapFunctionParameterType(it.type, variadic = false, location = typeLocation)
}

private fun BackendChecker.checkCanGenerateCFunction(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        isObjCMethod: Boolean,
        location: IrElement
) {
    signature.parameters.forEach {
        checkParameter(it, function.parameters[it.indexInParameters], isObjCMethod, location)
    }
}

private fun BackendChecker.checkCanGenerateCFunctionPointer(function: IrSimpleFunction, expression: IrExpression) =
        checkCanGenerateCFunction(
                function = function,
                signature = function,
                isObjCMethod = false,
                location = expression
        )

private fun BackendChecker.checkCanMapCalleeFunctionParameter(
        type: IrType,
        isObjCMethod: Boolean,
        variadic: Boolean,
        parameter: IrValueParameter?,
        argument: IrExpression
) {
    val classifier = type.classifierOrNull
    when {
        classifier?.isClassWithFqName(InteropFqNames.cValues.toUnsafe()) == true || // Note: this should not be accepted, but is required for compatibility
                classifier?.isClassWithFqName(InteropFqNames.cValuesRef.toUnsafe()) == true -> return

        classifier == symbols.string && (variadic || parameter?.isCStringParameter() == true) -> {
            if (variadic && isObjCMethod) {
                reportError(argument, "Passing String as variadic Objective-C argument is ambiguous; " +
                        "cast it to NSString or pass with '.cstr' as C string")
                // TODO: consider reporting a warning for C functions.
            }
        }

        classifier == symbols.string && parameter?.isWCStringParameter() == true -> return

        else -> checkCanMapFunctionParameterType(type, variadic = variadic, location = TypeLocation.FunctionArgument(argument))
    }
}

private sealed class TypeLocation(val element: IrElement) {
    class FunctionArgument(val argument: IrExpression) : TypeLocation(argument)
    class FunctionCallResult(val call: IrFunctionAccessExpression) : TypeLocation(call)

    class FunctionPointerParameter(val index: Int, element: IrElement) : TypeLocation(element)
    class FunctionPointerReturnValue(element: IrElement) : TypeLocation(element)

    class ObjCMethodParameter(element: IrElement) : TypeLocation(element)
    class ObjCMethodReturnValue(element: IrElement) : TypeLocation(element)

    class BlockParameter(val index: Int, val blockLocation: TypeLocation) : TypeLocation(blockLocation.element)
    class BlockReturnValue(val blockLocation: TypeLocation) : TypeLocation(blockLocation.element)
}

private fun BackendChecker.checkCanMapFunctionParameterType(type: IrType, variadic: Boolean, location: TypeLocation) {
    if (!type.isUnit() || variadic)
        checkCanMapType(type, variadic = variadic, location = location)
}

private fun BackendChecker.checkCanMapReturnType(type: IrType, location: TypeLocation) {
    if (!type.isUnit())
        checkCanMapType(type, variadic = false, location = location)
}

private fun BackendChecker.checkCanMapBlockType(type: IrType, location: TypeLocation) {
    when (val returnTypeArgument = (type as IrSimpleType).arguments.last()) {
        is IrTypeProjection -> if (returnTypeArgument.variance == Variance.INVARIANT)
            checkCanMapReturnType(returnTypeArgument.type, TypeLocation.BlockReturnValue(location))
        else
            reportUnsupportedType("${returnTypeArgument.variance.label}-variance of return type", type, location)

        is IrStarProjection -> reportUnsupportedType("* as return type", type, location)
    }
    type.arguments.dropLast(1).forEachIndexed { index, argument ->
        when (argument) {
            is IrTypeProjection -> if (argument.variance == Variance.INVARIANT)
                checkCanMapType(argument.type, variadic = false, location = TypeLocation.BlockParameter(index, location))
            else
                reportUnsupportedType("${argument.variance.label}-variance of ${index + 1} parameter type", type, location)

            is IrStarProjection -> reportUnsupportedType("* as ${index + 1} parameter type", type, location)
        }
    }
}

private fun BackendChecker.checkCanMapType(type: IrType, variadic: Boolean, location: TypeLocation) =
        checkCanMapType(type, variadic, location) { reportUnsupportedType(it, type, location) }

private fun BackendChecker.checkCanMapType(
        type: IrType,
        variadic: Boolean,
        typeLocation: TypeLocation,
        reportUnsupportedType: (String) -> Nothing
) {
    when {
        type.isBoolean() -> {
            if (cBoolType(target) == null)
                reportUnsupportedType("unavailable on target platform")
        }

        type.isByte() -> return
        type.isShort() -> return
        type.isInt() -> return
        type.isLong() -> return
        type.isFloat() -> return
        type.isDouble() -> return
        type.isCPointer(symbols) -> return
        type.isTypeOfNullLiteral() && variadic -> return
        type.isUByte() -> return
        type.isUShort() -> return
        type.isUInt() -> return
        type.isULong() -> return
        type.isVector() -> return

        type.isCEnumType() -> return

        type.isCValue(symbols) -> if (type.isNullable())
            reportUnsupportedType("must not be nullable")
        else {
            val kotlinClass = (type as IrSimpleType).arguments.singleOrNull()?.typeOrNull?.getClass()
                    ?: reportUnsupportedType("must be parameterized with concrete class")

            kotlinClass.getCStructSpelling()
                    ?: reportUnsupportedType("not a structure or too complex")
        }

        type.isNativePointed(symbols) -> return

        type.isFunction() -> if (variadic)
            reportUnsupportedType("not supported as variadic argument")
        else
            checkCanMapBlockType(type, location = typeLocation)

        type.isObjCReferenceType(target, irBuiltIns) -> return

        else -> reportUnsupportedType("doesn't correspond to any C type")
    }
}

private fun BackendChecker.reportUnsupportedType(reason: String, type: IrType, location: TypeLocation): Nothing {
    // TODO: report errors in frontend instead.
    fun TypeLocation.render(): String = when (this) {
        is TypeLocation.FunctionArgument -> ""
        is TypeLocation.FunctionCallResult -> " of return value"
        is TypeLocation.FunctionPointerParameter -> " of callback parameter ${index + 1}"
        is TypeLocation.FunctionPointerReturnValue -> " of callback return value"
        is TypeLocation.ObjCMethodParameter -> " of overridden Objective-C method parameter"
        is TypeLocation.ObjCMethodReturnValue -> " of overridden Objective-C method return value"
        is TypeLocation.BlockParameter -> " of ${index + 1} parameter in Objective-C block type${blockLocation.render()}"
        is TypeLocation.BlockReturnValue -> " of return value of Objective-C block type${blockLocation.render()}"
    }

    val typeLocation: String = location.render()

    reportError(location.element, "type ${type.render()} $typeLocation is not supported here" +
            if (reason.isNotEmpty()) ": $reason" else "")
}
