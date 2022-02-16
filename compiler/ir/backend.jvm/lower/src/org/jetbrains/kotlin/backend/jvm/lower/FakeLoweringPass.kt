/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

private var fakePhases = 0

fun makeFakePhase(): NamedCompilerPhase<JvmBackendContext, IrFile> {
    val id = fakePhases++
    return makeIrFilePhase(
        ::FakeLoweringPass,
        name = "FakeLoweringPass$id",
        description = "Fake pass #$id"
    )
}

private class FakeLoweringPass(private val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    private var work = 0

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitCall(expression: IrCall): IrExpression {
        work += expression.hashCode()
        return super.visitCall(expression)
    }
}