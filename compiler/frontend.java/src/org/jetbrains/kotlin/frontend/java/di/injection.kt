/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.frontend.java.di

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.toKotlinVersion
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.frontend.di.configureIncrementalCompilation
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.di.configureStandardResolveComponents
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.components.*
import org.jetbrains.kotlin.load.java.lazy.JavaResolverSettings
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.JvmDiagnosticComponents
import org.jetbrains.kotlin.resolve.jvm.extensions.SyntheticJavaResolveExtension
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.multiplatform.OptionalAnnotationPackageFragmentProvider
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.AbsentDescriptorHandler
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.optimization.OptimizingOptions

fun createContainerForLazyResolveWithJava(
    jvmPlatform: TargetPlatform,
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    moduleContentScope: GlobalSearchScope,
    moduleClassResolver: ModuleClassResolver,
    targetEnvironment: TargetEnvironment,
    lookupTracker: LookupTracker,
    expectActualTracker: ExpectActualTracker,
    inlineConstTracker: InlineConstTracker,
    enumWhenTracker: EnumWhenTracker,
    packagePartProvider: PackagePartProvider,
    languageVersionSettings: LanguageVersionSettings,
    useBuiltInsProvider: Boolean,
    configureJavaClassFinder: (StorageComponentContainer.() -> Unit)? = null,
    javaClassTracker: JavaClassesTracker? = null,
    implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter? = null,
    sealedInheritorsProvider: SealedClassInheritorsProvider = CliSealedClassInheritorsProvider,
    optimizingOptions: OptimizingOptions? = null,
    absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>? = null
): StorageComponentContainer = createContainer("LazyResolveWithJava", JvmPlatformAnalyzerServices) {
    configureModule(
        moduleContext, jvmPlatform, JvmPlatformAnalyzerServices, bindingTrace, languageVersionSettings,
        sealedInheritorsProvider, optimizingOptions, absentDescriptorHandlerClass
    )

    configureIncrementalCompilation(lookupTracker, expectActualTracker, inlineConstTracker, enumWhenTracker)
    configureStandardResolveComponents()

    useInstance(moduleContentScope)
    useInstance(packagePartProvider)
    useInstance(declarationProviderFactory)
    useInstanceIfNotNull(implicitsResolutionFilter)

    useInstance(VirtualFileFinderFactory.getInstance(moduleContext.project).create(moduleContentScope))

    configureJavaSpecificComponents(
        moduleContext, moduleClassResolver, languageVersionSettings, configureJavaClassFinder,
        javaClassTracker, useBuiltInsProvider
    )

    targetEnvironment.configure(this)
}.apply {
    initializeJavaSpecificComponents(bindingTrace)
}

fun StorageComponentContainer.initializeJavaSpecificComponents(bindingTrace: BindingTrace) {
    get<AbstractJavaClassFinder>().initialize(
        bindingTrace, codeAnalyzer = get(), languageVersionSettings = get(), jvmTarget = get()
    )
}

fun StorageComponentContainer.configureJavaSpecificComponents(
    moduleContext: ModuleContext,
    moduleClassResolver: ModuleClassResolver,
    languageVersionSettings: LanguageVersionSettings,
    configureJavaClassFinder: (StorageComponentContainer.() -> Unit)?,
    javaClassTracker: JavaClassesTracker?,
    useBuiltInsProvider: Boolean
) {
    useImpl<JavaDescriptorResolver>()
    useImpl<DeserializationComponentsForJava>()
    useInstance(JavaPropertyInitializerEvaluatorImpl)
    useImpl<SignaturePropagatorImpl>()
    useImpl<TraceBasedErrorReporter>()
    useInstance(InternalFlexibleTypeTransformer)
    useInstance(JavaDeprecationSettings)
    useInstance(moduleClassResolver)

    useInstance(SyntheticJavaResolveExtension.getProvider(moduleContext.project))

    if (configureJavaClassFinder != null) {
        configureJavaClassFinder()
    } else {
        useImpl<JavaClassFinderImpl>()
        useImpl<LazyResolveBasedCache>()
        useImpl<JavaSourceElementFactoryImpl>()
    }

    useInstance(
        languageVersionSettings.getFlag(JvmAnalysisFlags.javaTypeEnhancementState)
            ?: JavaTypeEnhancementState.getDefault(languageVersionSettings.toKotlinVersion())
    )

    val builtIns = moduleContext.module.builtIns
    if (useBuiltInsProvider && builtIns is JvmBuiltIns) {
        // TODO(dsavvinov): make sure that useBuiltInsProvider == true <=> builtIns is JvmBuiltIns
        // Currently, that's not the case at least in IDE unit-tests, because they do not set-up
        // dependency on SDK properly, see KT-43828
        useInstance(builtIns.customizer)
        useImpl<JvmBuiltInsPackageFragmentProvider>()
    }
    useImpl<OptionalAnnotationPackageFragmentProvider>()

    useInstance(javaClassTracker ?: JavaClassesTracker.Default)
    useInstance(
        JavaResolverSettings.create(
            correctNullabilityForNotNullTypeParameter = languageVersionSettings.supportsFeature(LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated),
            typeEnhancementImprovementsInStrictMode = languageVersionSettings.supportsFeature(LanguageFeature.TypeEnhancementImprovementsInStrictMode),
            ignoreNullabilityForErasedValueParameters = languageVersionSettings.supportsFeature(LanguageFeature.IgnoreNullabilityForErasedValueParameters),
            enhancePrimitiveArrays = languageVersionSettings.supportsFeature(LanguageFeature.EnhanceNullabilityOfPrimitiveArrays),
        )
    )
    useInstance(JavaModuleResolver.getInstance(moduleContext.project))

    useImpl<FilesByFacadeFqNameIndexer>()
    useImpl<JvmDiagnosticComponents>()
}

fun ComponentProvider.initJvmBuiltInsForTopDownAnalysis() {
    get<JvmBuiltIns>().initialize(get<ModuleDescriptor>(), get<LanguageVersionSettings>())
}

fun JvmBuiltIns.initialize(module: ModuleDescriptor, languageVersionSettings: LanguageVersionSettings) {
    initialize(module, languageVersionSettings.supportsFeature(LanguageFeature.AdditionalBuiltInsMembers))
}
