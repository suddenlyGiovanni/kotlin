/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.imports.Importable

interface ImplementationKindOwner : TypeRef, Importable {
    var kind: ImplementationKind?
    val allParents: List<ImplementationKindOwner>

    val needPureAbstractElement: Boolean
        get() = kind?.isInterface != true &&
                allParents.none<ImplementationKindOwner> { it.kind == ImplementationKind.AbstractClass || it.kind == ImplementationKind.SealedClass }
}
