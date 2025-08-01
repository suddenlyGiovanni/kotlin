/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.DoNothingICReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.reportPerformanceData
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.dirtyFiles.getClasspathChanges
import org.jetbrains.kotlin.incremental.dirtyFiles.getRemovedClassesChanges
import org.jetbrains.kotlin.incremental.js.*
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import java.io.File

fun makeJsIncrementally(
    cachesDir: File,
    sourceRoots: Iterable<File>,
    args: K2JSCompilerArguments,
    buildHistoryFile: File,
    messageCollector: MessageCollector = MessageCollector.NONE,
    reporter: ICReporter = DoNothingICReporter,
    scopeExpansion: CompileScopeExpansionMode = CompileScopeExpansionMode.NEVER,
    modulesApiHistory: ModulesApiHistory = EmptyModulesApiHistory,
    providedChangedFiles: ChangedFiles? = null
) {
    val allKotlinFiles = sourceRoots.asSequence().flatMap { it.walk() }
        .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }.toList()

    val buildReporter = BuildReporter(icReporter = reporter, buildMetricsReporter = DoNothingBuildMetricsReporter)
    withJsIC(args) {
        val compiler = IncrementalJsCompilerRunner(
            cachesDir, buildReporter,
            buildHistoryFile = buildHistoryFile,
            modulesApiHistory = modulesApiHistory,
            scopeExpansion = scopeExpansion
        )
        compiler.compile(allKotlinFiles, args, messageCollector, providedChangedFiles ?: ChangedFiles.DeterminableFiles.ToBeComputed)
    }
}

@Suppress("DEPRECATION")
inline fun <R> withJsIC(args: CommonCompilerArguments, enabled: Boolean = true, fn: () -> R): R {
    val isJsEnabledBackup = IncrementalCompilation.isEnabledForJs()
    IncrementalCompilation.setIsEnabledForJs(true)

    try {
        if (args.incrementalCompilation == null) {
            args.incrementalCompilation = enabled
        }
        return fn()
    } finally {
        IncrementalCompilation.setIsEnabledForJs(isJsEnabledBackup)
    }
}

class IncrementalJsCompilerRunner(
    workingDir: File,
    reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    buildHistoryFile: File?,
    private val modulesApiHistory: ModulesApiHistory,
    private val scopeExpansion: CompileScopeExpansionMode = CompileScopeExpansionMode.NEVER,
    icFeatures: IncrementalCompilationFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION,
) : IncrementalCompilerRunner<K2JSCompilerArguments, IncrementalJsCachesManager>(
    workingDir,
    "caches-js",
    reporter,
    buildHistoryFile = buildHistoryFile,
    outputDirs = null,
    icFeatures = icFeatures,
) {
    override val shouldTrackChangesInLookupCache
        get() = false

    override val shouldStoreFullFqNamesInLookupCache
        get() = icFeatures.withAbiSnapshot

    override fun createCacheManager(icContext: IncrementalCompilationContext, args: K2JSCompilerArguments) =
        IncrementalJsCachesManager(icContext, KlibMetadataSerializerProtocol, cacheDirectory)

    override fun destinationDir(args: K2JSCompilerArguments): File {
        return File(args.outputDir!!)
    }

    override fun calculateSourcesToCompile(
        caches: IncrementalJsCachesManager,
        changedFiles: ChangedFiles.DeterminableFiles.Known,
        args: K2JSCompilerArguments,
        messageCollector: MessageCollector,
        classpathAbiSnapshots: Map<String, AbiSnapshot> //Ignore for now
    ): CompilationMode {
        if (buildHistoryFile == null) {
            error("The build is configured to use the build-history based IC approach, but doesn't specify the buildHistoryFile")
        }
        if (!icFeatures.withAbiSnapshot && !buildHistoryFile.isFile) {
            return CompilationMode.Rebuild(BuildAttribute.NO_BUILD_HISTORY)
        }
        val lastBuildInfo = BuildInfo.read(lastBuildInfoFile, messageCollector) ?: return CompilationMode.Rebuild(BuildAttribute.INVALID_LAST_BUILD_INFO)

        val dirtyFiles = dirtyFilesProvider.getInitializedDirtyFiles(caches, changedFiles)

        val libs = (args.libraries ?: "").split(File.pathSeparator).map { File(it) }
        //TODO(valtman) check for JS
        val classpathChanges = getClasspathChanges(
            libs, changedFiles, lastBuildInfo, modulesApiHistory, reporter,
            mapOf(), false, caches.platformCache,
            caches.lookupCache.lookupSymbols.map { if (it.scope.isBlank()) it.name else it.scope }.distinct()
        )

        when (classpathChanges) {
            is ChangesEither.Unknown -> {
                reporter.info { "Could not get classpath's changes: ${classpathChanges.reason}" }
                return CompilationMode.Rebuild(classpathChanges.reason)
            }
            is ChangesEither.Known -> {
                dirtyFiles.addByDirtySymbols(classpathChanges.lookupSymbols)
                dirtyFiles.addByDirtyClasses(classpathChanges.fqNames)
            }
        }

        val removedClassesChanges = getRemovedClassesChanges(caches, changedFiles, kotlinSourceFilesExtensions, reporter)
        dirtyFiles.addByDirtySymbols(removedClassesChanges.dirtyLookupSymbols)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNames)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNamesForceRecompile)

        if (dirtyFiles.isEmpty() && changedFiles.removed.isNotEmpty()) {
            return CompilationMode.Rebuild(BuildAttribute.DEP_CHANGE_REMOVED_ENTRY)
        }
        return CompilationMode.Incremental(dirtyFiles)
    }

    override fun makeServices(
        args: K2JSCompilerArguments,
        lookupTracker: LookupTracker,
        expectActualTracker: ExpectActualTracker,
        fileMappingTracker: ICFileMappingTracker,
        caches: IncrementalJsCachesManager,
        dirtySources: Set<File>,
        isIncremental: Boolean
    ): Services.Builder =
        super.makeServices(args, lookupTracker, expectActualTracker, fileMappingTracker, caches, dirtySources, isIncremental).apply {
            if (isIncremental) {
                if (scopeExpansion == CompileScopeExpansionMode.ALWAYS) {
                    val nextRoundChecker = IncrementalNextRoundCheckerImpl(caches, dirtySources)
                    register(IncrementalNextRoundChecker::class.java, nextRoundChecker)
                }
                register(IncrementalDataProvider::class.java, IncrementalDataProviderFromCache(caches.platformCache))
            }

            register(IncrementalResultsConsumer::class.java, IncrementalResultsConsumerImpl())
        }

    override fun updateCaches(
        services: Services,
        caches: IncrementalJsCachesManager,
        generatedFiles: List<GeneratedFile>,
        changesCollector: ChangesCollector
    ) {
        val incrementalResults = services[IncrementalResultsConsumer::class.java] as IncrementalResultsConsumerImpl

        val jsCache = caches.platformCache
        jsCache.header = incrementalResults.headerMetadata

        jsCache.compareAndUpdate(incrementalResults, changesCollector)
        jsCache.clearCacheForRemovedClasses(changesCollector)
    }

    override fun runCompiler(
        sourcesToCompile: List<File>,
        args: K2JSCompilerArguments,
        caches: IncrementalJsCachesManager,
        services: Services,
        messageCollector: MessageCollector,
        allSources: List<File>,
        isIncremental: Boolean
    ): Pair<ExitCode, Collection<File>> {
        val freeArgsBackup = args.freeArgs

        val compiler = K2JSCompiler()
        return try {
            args.freeArgs += sourcesToCompile.map { it.absolutePath }
            compiler.exec(messageCollector, services, args) to sourcesToCompile
        } finally {
            args.freeArgs = freeArgsBackup
            reporter.reportPerformanceData(compiler.defaultPerformanceManager.unitStats)
        }
    }

    override fun additionalDirtyFiles(
        caches: IncrementalJsCachesManager,
        generatedFiles: List<GeneratedFile>,
        services: Services
    ): Iterable<File> {
        val additionalDirtyFiles: Set<File> =
            when (scopeExpansion) {
                CompileScopeExpansionMode.ALWAYS ->
                    (services[IncrementalNextRoundChecker::class.java] as IncrementalNextRoundCheckerImpl).newDirtySources
                CompileScopeExpansionMode.NEVER ->
                    emptySet()
            }

        return additionalDirtyFiles + super.additionalDirtyFiles(caches, generatedFiles, services)
    }

    inner class IncrementalNextRoundCheckerImpl(
        private val caches: IncrementalJsCachesManager,
        private val sourcesToCompile: Set<File>
    ) : IncrementalNextRoundChecker {
        val newDirtySources = HashSet<File>()

        private val emptyByteArray = ByteArray(0)
        private val translatedFiles = HashMap<File, TranslationResultValue>()

        override fun checkProtoChanges(sourceFile: File, packagePartMetadata: ByteArray) {
            translatedFiles[sourceFile] = TranslationResultValue(packagePartMetadata, emptyByteArray, emptyByteArray)
        }

        override fun shouldGoToNextRound(): Boolean {
            val changesCollector = ChangesCollector()
            // todo: split compare and update (or cache comparing)
            caches.platformCache.compare(translatedFiles, changesCollector)
            val (dirtyLookupSymbols, dirtyClassFqNames) =
                changesCollector.getChangedAndImpactedSymbols(listOf(caches.platformCache), reporter)
            // todo unify with main cycle
            newDirtySources.addAll(mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, reporter, excludes = sourcesToCompile))
            newDirtySources.addAll(
                mapClassesFqNamesToFiles(
                    listOf(caches.platformCache),
                    dirtyClassFqNames,
                    reporter,
                    excludes = sourcesToCompile
                )
            )
            return newDirtySources.isNotEmpty()
        }
    }
}
