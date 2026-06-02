package org.partiql.scribe.integ.executor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.partiql.scribe.integ.engine.EngineClient
import org.partiql.scribe.integ.engine.ExecutionResult
import org.partiql.scribe.integ.engine.Status
import org.partiql.scribe.integ.loader.Target
import org.partiql.scribe.integ.loader.TestCase

data class TestResult(
    val testCase: TestCase,
    val execution: ExecutionResult,
)

class TestExecutor(
    private val engines: Map<Target, EngineClient>,
    private val concurrency: Int = 5,
    private val skipLists: Map<Target, SkipList> = emptyMap(),
) {

    fun execute(testCases: List<TestCase>): List<TestResult> = runBlocking(Dispatchers.IO) {
        val semaphores = engines.keys.associateWith { Semaphore(concurrency) }

        testCases.map { testCase ->
            async {
                val skipList = skipLists[testCase.target] ?: SkipList.empty()

                when {
                    testCase.isSkipped -> TestResult(
                        testCase = testCase,
                        execution = ExecutionResult(status = Status.SKIPPED),
                    )
                    skipList.isSkipped(testCase.name) -> TestResult(
                        testCase = testCase,
                        execution = ExecutionResult(status = Status.SKIPPED),
                    )
                    else -> {
                        val engine = engines[testCase.target]
                            ?: return@async TestResult(
                                testCase = testCase,
                                execution = ExecutionResult(
                                    status = Status.SKIPPED,
                                    error = "No engine configured for ${testCase.target}",
                                ),
                            )
                        val semaphore = semaphores[testCase.target]!!
                        semaphore.withPermit {
                            val result = engine.execute(testCase.sql)
                            val finalResult = if (result.status == Status.FAILED && skipList.isExpectedFailure(testCase.name)) {
                                result.copy(status = Status.EXPECTED_FAIL)
                            } else {
                                result
                            }
                            TestResult(testCase = testCase, execution = finalResult)
                        }
                    }
                }
            }
        }.awaitAll()
    }
}
