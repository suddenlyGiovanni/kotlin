/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.sir.impl

import org.jetbrains.kotlin.sir.*

internal class SirFunctionImpl(
    override val origin: SirOrigin,
    override val visibility: SirVisibility,
    override val documentation: String?,
    override val attributes: MutableList<SirAttribute>,
    override val bridges: MutableList<SirBridge>,
    override var body: SirFunctionBody?,
    override val errorType: SirType,
    override val isOverride: Boolean,
    override val isInstance: Boolean,
    override val modality: SirModality,
    override val name: String,
    override val extensionReceiverParameter: SirParameter?,
    override val parameters: MutableList<SirParameter>,
    override val returnType: SirType,
    override val fixity: SirFixity?,
) : SirFunction() {
    override lateinit var parent: SirDeclarationParent
}
