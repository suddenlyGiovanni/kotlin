/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CleanableBindingContext

internal class PsiToIrContextImpl(
        config: KonanConfig,
        private val moduleDescriptor: ModuleDescriptor,
        override val bindingContext: BindingContext,
) : BasicPhaseContext(config), PsiToIrContext {
    // TODO: Invalidate properly in dispose method.
    override val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)

    override val reflectionTypes: KonanReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KonanReflectionTypes(moduleDescriptor)
    }

    override val builtIns: KonanBuiltIns by lazy(LazyThreadSafetyMode.PUBLICATION) {
        moduleDescriptor.builtIns as KonanBuiltIns
    }

    override fun dispose() {
        val originalBindingContext = bindingContext as? CleanableBindingContext
                ?: error("BindingContext should be cleanable in K/N IR to avoid leaking memory: $bindingContext")
        originalBindingContext.clear()
    }
}

internal val PsiToIrPhase = createSimpleNamedCompilerPhase<PsiToIrContext, PsiToIrInput, PsiToIrOutput>(
        "PsiToIr",
        postactions = getDefaultIrActions(),
        outputIfNotEnabled = { _, _, _, _ -> error("PsiToIr phase cannot be disabled") }
) { context, input ->
    context.psiToIr(input)
}