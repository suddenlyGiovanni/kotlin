/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.originalForReflectiveCall
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.lower.SyntheticAccessorLowering.Companion.isAccessible
import org.jetbrains.kotlin.backend.jvm.unboxInlineClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.overrides.isNonPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

/**
 * This lowering replaces member accesses that are illegal according to JVM accessibility rules with corresponding calls to the
 * `java.lang.reflect` API. The primary use-case is to facilitate the design of the "Evaluate expression..." mechanism in the JVM debugger.
 * Here, a code fragment is compiled _as if_ in the context of a breakpoint. Hence, it is compiled against an existing class hierarchy and
 * any access to private or otherwise inaccessible members that are "perceived" to be in scope must be transformed. The ordinary IR pipeline
 * would introduce an accessor next to the access_ee_, but that is assumed to not be possible here: the accessee is deserialized from class
 * files that cannot be modified at this point.
 *
 * The lowering looks for the following member accesses and determines their legality through the need for an accessor, had this been
 * an ordinary compilation:
 *
 * - {extension, static, super*} methods, {extension} property accessors,
 *     functions on companion objects
 * - field accesses
 * - constructor invocations
 * - companion object access
 *
 * Super calls, private or not, are not allowed from outside the class hierarchy of the involved classes, so it's emulated in fragment
 * compilation by the use of `invokespecial` - see [generateInvokeSpecialForCall] below.
 */
@PhaseDescription(
    name = "SpecialAccess",
    prerequisite = [JvmDefaultParameterCleaner::class]
)
internal class SpecialAccessLowering(
    val context: JvmBackendContext
) : IrElementTransformerVoidWithContext(), FileLoweringPass {

    private lateinit var inlineScopeResolver: IrInlineScopeResolver

    override fun lower(irFile: IrFile) {
        if (context.evaluatorData == null) return
        inlineScopeResolver = irFile.findInlineCallSites(context)
        irFile.transformChildrenVoid(this)
    }

    // Wrapper for the logic from SyntheticAccessorLowering
    private fun IrSymbol.isAccessible(withSuper: Boolean = false): Boolean {
        return isAccessible(context, currentScope, inlineScopeResolver, withSuper, null, fromOtherClassLoader = true)
    }

    // Fragments are transformed in a post-order traversal: children first,
    // then parent. This obscures, in particular, dispatch receivers, that go
    // from `IrGetObjectValue` calls to blocks implementing the corresponding
    // reflective access. We record these _before_ transformation, in order to
    // later predict the compilation strategy for fields. See the uses of
    // `fieldLocationAndReceiver`.
    val callsOnCompanionObjects: MutableMap<IrCall, IrClassSymbol> = mutableMapOf()

    private fun recordCompanionObjectAsDispatchReceiver(expression: IrCall) {
        val dispatchReceiver = expression.dispatchReceiver as? IrGetField ?: return
        val dispatchReceiverType = dispatchReceiver.symbol.owner.type as? IrSimpleType ?: return
        val klass = dispatchReceiverType.classOrNull
        if (klass != null && klass.owner.isCompanion) {
            callsOnCompanionObjects[expression] = klass
        }
    }

    /**
     * Fragment traversal
     */

    override fun visitCall(expression: IrCall): IrExpression {
        recordCompanionObjectAsDispatchReceiver(expression)
        expression.transformChildrenVoid(this)

        val superQualifier: IrClassSymbol? = expression.superQualifierSymbol
        val callee = expression.symbol

        if (callee.isAccessible(withSuper = superQualifier != null)) {
            return expression
        }

        return when {
            expression.symbol.owner.isGetter -> generateReflectiveAccessForGetter(expression)
            expression.symbol.owner.isSetter -> generateReflectiveAccessForSetter(expression)
            expression.dispatchReceiver == null -> generateReflectiveStaticCall(expression)
            superQualifier != null -> generateInvokeSpecialForCall(expression, superQualifier)
            else -> generateReflectiveMethodInvocation(expression)
        }
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        expression.transformChildrenVoid(this)

        val field = expression.symbol
        return if (field.isAccessible()) {
            expression
        } else {
            generateReflectiveFieldGet(expression)
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        expression.transformChildrenVoid(this)

        val field = expression.symbol
        return if (field.isAccessible()) {
            expression
        } else if (field.owner.correspondingPropertySymbol?.owner?.isConst == true || (field.owner.isFromJava() && field.owner.isFinal)) {
            generateThrowIllegalAccessException(expression)
        } else {
            generateReflectiveFieldSet(expression)
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        expression.transformChildrenVoid(this)

        val callee = expression.symbol
        return if (callee.isAccessible()) {
            expression
        } else {
            generateReflectiveConstructorInvocation(expression)
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        expression.transformChildrenVoid(this)

        val callee = expression.symbol
        return if (callee.isAccessible()) {
            expression
        } else {
            generateReflectiveAccessForCompanion(expression)
        }
    }

    /**
     * IR Generation for java.lang.reflect.{field, method, constructor} API
     */

    private val symbols = context.symbols
    private val reflectSymbols = symbols.javaLangReflectSymbols

    private fun IrBuilderWithScope.javaClassObject(klass: IrType): IrExpression =
        irCall(symbols.kClassJavaPropertyGetter).apply {
            arguments[0] =
                IrClassReferenceImpl(
                    startOffset, endOffset,
                    context.irBuiltIns.kClassClass.starProjectedType,
                    context.irBuiltIns.kClassClass,
                    klass
                )
        }

    private fun IrBuilderWithScope.javaClassObject(type: Type): IrExpression =
        irCall(symbols.getClassByDescriptor).apply {
            arguments[0] = irString(type.descriptor)
        }


    private fun IrBuilderWithScope.getDeclaredField(declaringClass: IrExpression, fieldName: String): IrExpression =
        irCall(reflectSymbols.getDeclaredField).apply {
            arguments[0] = declaringClass
            arguments[1] = irString(fieldName)
        }

    private fun IrBuilderWithScope.fieldSetAccessible(field: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectFieldSetAccessible).apply {
            arguments[0] = field
            arguments[1] = irTrue()
        }

    private fun IrBuilderWithScope.fieldSet(fieldObject: IrExpression, receiver: IrExpression, value: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectFieldSet).apply {
            arguments[0] = fieldObject
            arguments[1] = receiver
            arguments[2] = value
        }

    private fun IrBuilderWithScope.fieldGet(fieldObject: IrExpression, receiver: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectFieldGet).apply {
            arguments[0] = fieldObject
            arguments[1] = receiver
        }

    private fun createBuilder(startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET) =
        context.createJvmIrBuilder(currentScope!!, startOffset, endOffset)

    private fun IrBuilderWithScope.irVararg(
        elementType: IrType,
        values: List<IrExpression>
    ): IrExpression {
        return IrArrayBuilder(createBuilder(), context.irBuiltIns.arrayClass.typeWith(elementType)).apply {
            for (value in values) {
                +value
            }
        }.build()
    }

    private fun IrBuilderWithScope.getDeclaredMethod(
        declaringClass: IrExpression,
        signature: JvmMethodSignature,
    ): IrExpression =
        irCall(reflectSymbols.getDeclaredMethod).apply {
            arguments[0] = declaringClass
            arguments[1] = irString(signature.asmMethod.name)
            arguments[2] = irVararg(symbols.javaLangClass.defaultType, signature.parameters.map { javaClassObject(it) })
        }

    private fun IrBuilderWithScope.methodSetAccessible(method: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectMethodSetAccessible).apply {
            arguments[0] = method
            arguments[1] = irTrue()
        }

    private fun IrBuilderWithScope.methodInvoke(
        method: IrExpression,
        receiver: IrExpression,
        arguments: List<IrExpression>
    ): IrCall =
        irCall(reflectSymbols.javaLangReflectMethodInvoke).apply {
            this.arguments[0] = method
            this.arguments[1] = receiver
            this.arguments[2] = irVararg(context.irBuiltIns.anyNType, arguments.map { coerceToUnboxed(it) })
        }

    private fun IrBuilderWithScope.getDeclaredConstructor(
        declaringClass: IrExpression,
        signature: JvmMethodSignature
    ): IrExpression =
        irCall(reflectSymbols.getDeclaredConstructor).apply {
            arguments[0] = declaringClass
            arguments[1] = irVararg(symbols.javaLangClass.defaultType, signature.parameters.map { javaClassObject(it) })
        }


    private fun IrBuilderWithScope.constructorSetAccessible(constructor: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectConstructorSetAccessible).apply {
            arguments[0] = constructor
            arguments[1] = irTrue()
        }

    private fun IrBuilderWithScope.constructorNewInstance(constructor: IrExpression, arguments: List<IrExpression>): IrExpression =
        irCall(reflectSymbols.javaLangReflectConstructorNewInstance).apply {
            this.arguments[0] = constructor
            this.arguments[1] = irVararg(context.irBuiltIns.anyNType, arguments.map { coerceToUnboxed(it) })
        }

    /**
     * Specific reflective "patches"
     */

    private fun generateReflectiveMethodInvocation(
        originalCall: IrCall,
        declaringClass: IrType,
        signature: JvmMethodSignature,
        receiver: IrExpression?, // null => static method on `declaringClass`
        arguments: List<IrExpression>,
        returnType: IrType,
        symbol: IrSimpleFunctionSymbol,
    ): IrExpression {
        val classPartStubOrThis = declaringClass.classPartForMultifileFacadeOrThis
        val signatureToLookup = if (classPartStubOrThis != declaringClass && DescriptorVisibilities.isPrivate(symbol.owner.visibility)) {
            JvmMethodSignature(
                Method(
                    "${signature.asmMethod.name}$${classPartStubOrThis.classOrFail.owner.name}",
                    signature.asmMethod.descriptor
                ),
                signature.parameters
            )
        } else {
            signature
        }
        return context.createJvmIrBuilder(symbol).irBlock(resultType = returnType) {
            val methodVar =
                createTmpVariable(
                    getDeclaredMethod(javaClassObject(classPartStubOrThis), signatureToLookup),
                    nameHint = "method",
                    irType = reflectSymbols.javaLangReflectMethod.defaultType
                )
            +methodSetAccessible(irGet(methodVar))
            +coerceResult(
                methodInvoke(
                    irGet(methodVar),
                    receiver ?: irNull(),
                    arguments.map { coerceToUnboxed(it) })
                    .also { it.originalForReflectiveCall = originalCall },
                returnType
            ).also { it.originalForReflectiveCall = originalCall }
        }
    }

    private fun IrBuilderWithScope.coerceToUnboxed(expression: IrExpression): IrCall =
        irCall(symbols.unsafeCoerceIntrinsic).apply {
            typeArguments[0] = expression.type
            typeArguments[1] = expression.type.unboxInlineClass()
            arguments[0] = expression
        }

    private fun generateReflectiveMethodInvocation(call: IrCall): IrExpression {
        val targetFunction = call.symbol.owner
        val arguments = (targetFunction.parameters zip call.arguments).mapNotNull { (param, arg) ->
            when {
                param.kind != IrParameterKind.DispatchReceiver -> arg
                targetFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> arg
                else -> null
            }
        }

        return generateReflectiveMethodInvocation(
            call,
            getDeclaredClassType(call),
            context.defaultMethodSignatureMapper.mapSignatureSkipGeneric(targetFunction),
            call.dispatchReceiver,
            arguments,
            call.type,
            call.symbol
        )
    }

    private fun generateReflectiveStaticCall(call: IrCall): IrExpression {
        assert(call.dispatchReceiver == null) { "Assumed-to-be static call with a dispatch receiver" }
        return generateReflectiveMethodInvocation(
            call,
            call.symbol.owner.parentAsClass.defaultType,
            context.defaultMethodSignatureMapper.mapSignatureSkipGeneric(call.symbol.owner),
            null, // static call
            call.nonDispatchArguments.filterNotNull(),
            call.type,
            call.symbol
        )
    }

    private fun generateReflectiveConstructorInvocation(call: IrConstructorCall): IrExpression =
        context.createJvmIrBuilder(call.symbol)
            .irBlock(resultType = call.type) {
                val constructorVar =
                    createTmpVariable(
                        getDeclaredConstructor(
                            javaClassObject(call.symbol.owner.parentAsClass.defaultType),
                            this@SpecialAccessLowering.context.defaultMethodSignatureMapper.mapSignatureSkipGeneric(call.symbol.owner)
                        ),
                        nameHint = "constructor",
                        irType = reflectSymbols.javaLangReflectConstructor.defaultType
                    )
                +constructorSetAccessible(irGet(constructorVar))
                +constructorNewInstance(irGet(constructorVar), call.nonDispatchArguments.filterNotNull())
            }

    private fun generateReflectiveFieldGet(
        declaringClass: IrType,
        fieldName: String,
        fieldType: IrType,
        instance: IrExpression?, // null ==> static field on `declaringClass`
        symbol: IrSymbol,
    ): IrExpression =
        context.createJvmIrBuilder(symbol)
            .irBlock(resultType = fieldType) {
                val classVar = createTmpVariable(
                    javaClassObject(declaringClass.classPartForMultifileFacadeOrThis),
                    nameHint = "klass",
                    irType = symbols.kClassJavaPropertyGetter.returnType
                )
                val fieldVar = createTmpVariable(
                    getDeclaredField(irGet(classVar), fieldName),
                    nameHint = "field",
                    irType = reflectSymbols.javaLangReflectField.defaultType
                )
                +fieldSetAccessible(irGet(fieldVar))
                +coerceResult(fieldGet(irGet(fieldVar), instance ?: irGet(classVar)), fieldType)
            }

    private val IrType.classPartForMultifileFacadeOrThis: IrType
        get() {
            if (classOrNull?.owner?.origin != IrDeclarationOrigin.JVM_MULTIFILE_CLASS) return this
            val facadeSource = classOrNull?.owner?.source as? FacadeClassSource ?: return this
            return IrFactoryImpl.createClass(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.SYNTHETIC_FILE_CLASS,
                symbol = IrClassSymbolImpl(),
                name = facadeSource.className.fqNameForTopLevelClassMaybeWithDollars.shortName(),
                kind = ClassKind.CLASS,
                visibility = DescriptorVisibilities.PUBLIC,
                modality = Modality.FINAL
            ).also {
                it.parent = classOrFail.owner.parent
                it.createThisReceiverParameter()
            }.defaultType
        }

    private fun IrBuilderWithScope.coerceResult(value: IrExpression, type: IrType) =
        irCall(symbols.handleResultOfReflectiveAccess).apply {
            arguments[0] = value
            typeArguments[0] = type
        }


    private fun generateReflectiveFieldGet(getField: IrGetField): IrExpression =
        generateReflectiveFieldGet(
            getField.symbol.owner.parentClassOrNull!!.defaultType,
            getField.symbol.owner.name.asString(),
            getField.type,
            getField.receiver,
            getField.symbol
        )

    private fun generateReflectiveFieldSet(
        declaringClass: IrType,
        fieldName: String,
        value: IrExpression,
        type: IrType,
        instance: IrExpression?,
        symbol: IrSymbol
    ): IrExpression {
        return context.createJvmIrBuilder(symbol)
            .irBlock(resultType = type) {
                val fieldVar =
                    createTmpVariable(
                        getDeclaredField(
                            javaClassObject(declaringClass.classPartForMultifileFacadeOrThis),
                            fieldName
                        ),
                        nameHint = "field",
                        irType = reflectSymbols.javaLangReflectField.defaultType
                    )
                +fieldSetAccessible(irGet(fieldVar))
                +fieldSet(irGet(fieldVar), instance ?: irNull(), coerceToUnboxed(value))
            }
    }

    private fun generateReflectiveFieldSet(setField: IrSetField): IrExpression =
        generateReflectiveFieldSet(
            setField.symbol.owner.parentClassOrNull!!.defaultType,
            setField.symbol.owner.name.asString(),
            setField.value,
            setField.type,
            setField.receiver,
            setField.symbol,
        )

    private fun isPresentInBytecode(accessor: IrSimpleFunction): Boolean {
        val property = accessor.correspondingPropertySymbol!!.owner
        // Normally, all the accessors which not present in bytecode are lowered during `JvmPropertiesLowering`.
        // But `JvmPropertiesLowering` lowering relies on `IrProperty#needsAccessor,` which is correct only under the assumption
        // of "normal" visibility rules.
        // It is not the case when we compile code fragment from the debugger, so we need to handle special cases here.
        return property.isNonPrivate
                || property.isDelegated
                || (context.generatorExtensions as StubGeneratorExtensions).isAccessorWithExplicitImplementation(accessor)
    }

    // Returns a pair of the _type_ containing the field and the _instance_ on
    // which the field should be accessed. The instance is `null` if the field
    // is static. If the field is on a companion object it will be generated on
    // the corresponding owning class (recall, at this point the field has been
    // absolutely determined to be inaccessible to outside code).
    private fun fieldLocationAndReceiver(call: IrCall): Pair<IrType, IrExpression?> {
        callsOnCompanionObjects[call]?.let {
            val parentAsClass = it.owner.parentAsClass
            if (!parentAsClass.isJvmInterface) {
                return parentAsClass.defaultType to null
            }
        }

        val type = getDeclaredClassType(call)
        return type to call.dispatchReceiver
    }

    private fun getDeclaredClassType(call: IrCall) =
        call.superQualifierSymbol?.defaultType ?: call.symbol.owner.resolveFakeOverrideOrFail().parentAsClass.defaultType

    private fun generateReflectiveAccessForGetter(call: IrCall): IrExpression {
        val realGetter = call.symbol.owner.resolveFakeOverrideOrFail()

        if (isPresentInBytecode(realGetter)) {
            return generateReflectiveMethodInvocation(
                call,
                realGetter.parentAsClass.defaultType,
                context.defaultMethodSignatureMapper.mapSignatureSkipGeneric(realGetter),
                call.dispatchReceiver,
                call.nonDispatchArguments.filterNotNull(),
                realGetter.returnType,
                realGetter.symbol
            )
        }

        val (fieldLocation, instance) = fieldLocationAndReceiver(call)
        return generateReflectiveFieldGet(
            fieldLocation,
            realGetter.correspondingPropertySymbol!!.owner.name.asString(),
            realGetter.returnType,
            instance,
            call.symbol,
        )
    }

    private fun generateReflectiveAccessForSetter(call: IrCall): IrExpression {
        val realSetter = call.symbol.owner.resolveFakeOverrideOrFail()

        if (isPresentInBytecode(realSetter)) {
            return generateReflectiveMethodInvocation(
                call,
                realSetter.parentAsClass.defaultType,
                context.defaultMethodSignatureMapper.mapSignatureSkipGeneric(realSetter),
                call.dispatchReceiver,
                call.nonDispatchArguments.filterNotNull(),
                realSetter.returnType,
                call.symbol
            )
        }

        val (fieldLocation, receiver) = fieldLocationAndReceiver(call)
        return generateReflectiveFieldSet(
            fieldLocation,
            realSetter.correspondingPropertySymbol!!.owner.name.asString(),
            call.arguments.last()!!,
            call.type,
            receiver,
            call.symbol
        )
    }

    private fun generateThrowIllegalAccessException(setField: IrSetField): IrExpression {
        return context.createJvmIrBuilder(setField.symbol).irBlock {
            +irCall(symbols.throwIllegalAccessException).apply {
                arguments[0] = irString("Can not set final field")
            }
        }
    }


    // This is needed to coerce the codegen to emit a very specific
    // invokespecial instruction to target a super-call that is otherwise
    // illegal on the JVM. However! The byte code from this compilation is
    // not run on a JVM: it is interpreted by eval4j. Eval4j handles
    // invokespecial via JDI from which it *is* possible to do the required
    // super call.
    private fun generateInvokeSpecialForCall(expression: IrCall, superQualifier: IrClassSymbol): IrExpression {
        val jvmSignature = context.defaultMethodSignatureMapper.mapSignatureSkipGeneric(expression.symbol.owner)
        val owner = superQualifier.owner
        val builder = context.createJvmIrBuilder(expression.symbol)

        // invokeSpecial(owner: String, name: String, descriptor: String, isInterface: Boolean): T
        return builder.irCall(symbols.jvmDebuggerInvokeSpecialIntrinsic).apply {
            this.type = expression.symbol.owner.returnType
            arguments[0] = expression.dispatchReceiver
            arguments[1] = builder.irString("${owner.packageFqName}/${owner.name}")
            arguments[2] = builder.irString(jvmSignature.asmMethod.name)
            arguments[3] = builder.irString(jvmSignature.asmMethod.descriptor)
            arguments[4] = builder.irFalse()
            // A workaround to pass the initial call arguments. Elements of this array
            // will be extracted and passed to the bytecode generator right before
            // generating the bytecode for invokeSpecial itself.
            val args = with(context.irBuiltIns) {
                builder.irArray(arrayClass.typeWith(anyNType)) {
                    expression.nonDispatchArguments.forEach { add(it!!) }
                }
            }
            arguments[5] = args
        }
    }

    private fun generateReflectiveAccessForCompanion(call: IrGetObjectValue): IrExpression =
        generateReflectiveFieldGet(
            call.symbol.owner.parentAsClass.defaultType,
            "Companion",
            call.type,
            null,
            call.symbol
        )
}
