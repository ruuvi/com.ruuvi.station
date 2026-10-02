package com.ruuvi.station.network.domain

import com.ruuvi.station.network.domain.NetworkRequestExecutor.NetworkJobManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class NetworkJobManagerTest {
    private val manager = NetworkJobManager()
    private val jobs = mutableListOf<Job>()

    @After
    fun cancelOutstandingJobs() = runBlocking {
        withTimeout(TIMEOUT.milliseconds) {
            jobs.forEach { it.cancel() }
            manager.cancelAndJoinAll()
            jobs.joinAll()
        }
    }

    @Test
    fun `unknown job is not running and cancellation is a no-op`() = runBlocking {
        assertFalse(manager.isJobRunning(1))

        manager.cancelJob(1)
        withTimeout(TIMEOUT.milliseconds) { manager.cancelAndJoinAll() }

        assertFalse(manager.isJobRunning(1))
    }

    @Test
    fun `registered job runs until it completes`() {
        val job = activeJob()
        manager.registerJob(1, job)

        assertTrue(manager.isJobRunning(1))

        job.complete()

        assertFalse(manager.isJobRunning(1))
    }

    @Test
    fun `registering the same id does not replace an active job`() {
        val original = activeJob()
        val replacement = activeJob()
        manager.registerJob(1, original)
        manager.registerJob(1, replacement)

        manager.cancelJob(1)

        assertTrue(original.isCancelled)
        assertTrue(replacement.isActive)
        assertFalse(manager.isJobRunning(1))
    }

    @Test
    fun `completed and cancelled jobs can be replaced`() {
        val finishedJobs = listOf(
            activeJob().apply { complete() },
            activeJob().apply { cancel() },
        )
        finishedJobs.forEachIndexed { id, finished ->
            manager.registerJob(id, finished)
            assertFalse(manager.isJobRunning(id))
            val replacement = activeJob()

            manager.registerJob(id, replacement)

            assertTrue(manager.isJobRunning(id))
            manager.cancelJob(id)
            assertTrue(replacement.isCancelled)
            assertFalse(manager.isJobRunning(id))
        }
    }

    @Test
    fun `lazy job is not running before start and can be cancelled without executing`() = runBlocking {
        withTimeout(TIMEOUT.milliseconds) {
            var executed = false
            val job = launch(start = CoroutineStart.LAZY) {
                executed = true
            }
            manager.registerJob(1, job)
            assertFalse(manager.isJobRunning(1))

            manager.cancelJob(1)
            job.join()

            assertTrue(job.isCancelled)
            assertFalse(executed)
            assertFalse(manager.isJobRunning(1))
        }
    }

    @Test
    fun `started lazy job is running until its work completes`() = runBlocking {
        withTimeout(TIMEOUT.milliseconds) {
            val release = CompletableDeferred<Unit>()
            val job = launch(start = CoroutineStart.LAZY) { release.await() }
            manager.registerJob(1, job)
            assertFalse(manager.isJobRunning(1))

            job.start()

            assertTrue(manager.isJobRunning(1))
            release.complete(Unit)
            job.join()
            assertFalse(manager.isJobRunning(1))
        }
    }

    @Test
    fun `cancel all waits for coroutine cleanup and cancels every registered job`() = runBlocking {
        withTimeout(TIMEOUT.milliseconds) {
            val cleanupStarted = CompletableDeferred<Unit>()
            val finishCleanup = CompletableDeferred<Unit>()
            val first = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        cleanupStarted.complete(Unit)
                        finishCleanup.await()
                    }
                }
            }
            val second = activeJob()
            manager.registerJob(1, first)
            manager.registerJob(2, second)
            val cancellation = launch { manager.cancelAndJoinAll() }
            try {
                cleanupStarted.await()
                assertFalse(cancellation.isCompleted)
                assertFalse(first.isCompleted)

                finishCleanup.complete(Unit)
                cancellation.join()

                assertTrue(first.isCompleted)
                assertTrue(first.isCancelled)
                assertTrue(second.isCancelled)
                assertFalse(manager.isJobRunning(1))
                assertFalse(manager.isJobRunning(2))
                manager.cancelAndJoinAll()
            } finally {
                finishCleanup.complete(Unit)
            }
        }
    }

    @Test
    fun `cancel all stops a replacement registered while the old job is finishing`() = runBlocking {
        withTimeout(TIMEOUT.milliseconds) {
            val cleanupStarted = CompletableDeferred<Unit>()
            val finishCleanup = CompletableDeferred<Unit>()
            val original = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        cleanupStarted.complete(Unit)
                        finishCleanup.await()
                    }
                }
            }
            manager.registerJob(1, original)
            val cancellation = launch { manager.cancelAndJoinAll() }
            try {
                cleanupStarted.await()
                val replacement = activeJob()
                manager.registerJob(1, replacement)
                assertTrue(replacement.isCancelled)
                assertFalse(manager.isJobRunning(1))

                finishCleanup.complete(Unit)
                cancellation.join()

                assertTrue(original.isCancelled)
                assertTrue(original.isCompleted)
                assertTrue(replacement.isCancelled)
                assertFalse(replacement.isActive)
                assertFalse(manager.isJobRunning(1))
            } finally {
                finishCleanup.complete(Unit)
            }
        }
    }

    private fun activeJob() = Job().also { jobs.add(it) }

    companion object {
        private const val TIMEOUT = 5_000L
    }
}
