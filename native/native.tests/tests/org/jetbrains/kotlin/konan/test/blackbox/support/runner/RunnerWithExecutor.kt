/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.ProcessLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.AbstractRunner.AbstractRun
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.native.executors.ExecuteRequest
import org.jetbrains.kotlin.native.executors.Executor
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files
import kotlin.io.path.Path

internal open class RunnerWithExecutor(
    private val executor: Executor,
    private val testRun: TestRun
) : AbstractRunner<Unit>() {
    private val executable get() = testRun.executable

    private val outputFilter: TestOutputFilter
        get() = testRun.checks.testFiltering.testOutputFilter

    private val programArgs = mutableListOf<String>().apply {
        add(executable.executable.executableFile.absolutePath)
        testRun.runParameters.forEach { it.applyTo(this) }
    }

    private fun inputStreamFromTestParameter(): InputStream? =
        testRun.runParameters.firstIsInstanceOrNull<TestRunParameter.WithInputData>()
            ?.let {
                ByteArrayInputStream(it.inputDataFile.readBytes())
            }

    override fun buildRun() = AbstractRun {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val workingDirectory = executable.executable.executableFile.parentFile
        val request = ExecuteRequest(programArgs[0]).apply {
            this.args.addAll(programArgs.drop(1))
            this.workingDirectory = workingDirectory
            inputStreamFromTestParameter()?.let {
                this.stdin = it
            }
            this.stdout = stdout
            this.stderr = stderr
            this.timeout = testRun.checks.executionTimeoutCheck.timeout
        }
        val response = executor.execute(request)

        if (response.exitCode != 0) {
            val stderrString = stderr.toString("UTF-8")
            val regex = "^Minidump written to \"(.*)\"$".toRegex()
            val matchResult = regex.find(stderrString)
            val minidumpFileName = matchResult?.groups?.get(1)?.value
            if (minidumpFileName != null) {
                val minidumpAnalyzerOutput = ByteArrayOutputStream()
                runProcess(
                    ProcessLevelProperty.MINIDUMP_ANALYZER.readValue(),
                    executable.executable.executableFile.absolutePath, minidumpFileName,
                ) {
                    this.workingDirectory = workingDirectory
                    this.stdout = minidumpAnalyzerOutput
                    this.stderr = minidumpAnalyzerOutput
                }
                stderr.write(minidumpAnalyzerOutput.toByteArray())
                Files.write(Path("$minidumpFileName.log"), minidumpAnalyzerOutput.toByteArray())
            }
        }

        RunResult(
            testExecutable = executable,
            exitCode = response.exitCode,
            timeout = request.timeout,
            duration = response.executionTime,
            hasFinishedOnTime = response.exitCode != null,
            processOutput = ProcessOutput(
                stdOut = outputFilter.filter(stdout.toString("UTF-8")),
                stdErr = stderr.toString("UTF-8")
            )
        )
    }

    override fun buildResultHandler(runResult: RunResult): ResultHandler = ResultHandler(
        runResult = runResult,
        checks = testRun.checks,
        testRun = testRun,
        loggedParameters = getLoggedParameters()
    )

    override fun getLoggedParameters() = LoggedData.TestRunParameters(
        compilationToolCall = executable.loggedCompilationToolCall,
        testCaseId = testRun.testCase.id,
        runArgs = programArgs,
        runParameters = testRun.runParameters
    )

    override fun handleUnexpectedFailure(t: Throwable) = JUnit5Assertions.fail {
        LoggedData.TestRunUnexpectedFailure(getLoggedParameters(), t)
            .withErrorMessage("Test execution failed with unexpected exception.")
    }
}