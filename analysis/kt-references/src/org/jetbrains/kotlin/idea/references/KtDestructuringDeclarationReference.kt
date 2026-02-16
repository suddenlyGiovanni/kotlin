/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.resolution.KtResolvable

@OptIn(KtExperimentalApi::class)
@SubclassOptInRequired(KtImplementationDetail::class)
abstract class KtDestructuringDeclarationReference(
    element: KtDestructuringDeclarationEntry
) : KtMultiReference<KtDestructuringDeclarationEntry>(element), KtResolvable {

    override fun getRangeInElement() = TextRange(0, element.textLength)

    // TODO(KT-82708): Should be dropped
    override fun resolve(): PsiElement? = element

    override val resolvesByNames: Collection<Name>
        get() {
            val destructuringParent = element.parent as? KtDestructuringDeclaration ?: return emptyList()
            val componentIndex = destructuringParent.entries.indexOf(element) + 1
            return listOfNotNull(
                element.nameAsName,
                element.initializer?.getReferencedNameAsName(),
                Name.identifier("component$componentIndex"),
            )
        }
}
