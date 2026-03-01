package com.example.myradio.playback

import androidx.media3.common.PlaybackException

class StreamRetryManager(
    private val maxRetries: Int = 5,
    private val baseDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 16000L
) {

    private var retryCount = 0
    private var lastErrorWasPermanent = false

    data class RetryDecision(
        val shouldRetry: Boolean,
        val delayMs: Long,
        val attempt: Int,
        val maxAttempts: Int,
        val isPermanentError: Boolean
    )

    fun onError(exception: PlaybackException): RetryDecision {
        if (isPermanentError(exception)) {
            lastErrorWasPermanent = true
            return RetryDecision(
                shouldRetry = false,
                delayMs = 0,
                attempt = retryCount,
                maxAttempts = maxRetries,
                isPermanentError = true
            )
        }

        lastErrorWasPermanent = false
        retryCount++

        if (retryCount > maxRetries) {
            return RetryDecision(
                shouldRetry = false,
                delayMs = 0,
                attempt = retryCount,
                maxAttempts = maxRetries,
                isPermanentError = false
            )
        }

        return RetryDecision(
            shouldRetry = true,
            delayMs = getDelayMs(),
            attempt = retryCount,
            maxAttempts = maxRetries,
            isPermanentError = false
        )
    }

    fun onSuccess() {
        reset()
    }

    fun reset() {
        retryCount = 0
        lastErrorWasPermanent = false
    }

    private fun getDelayMs(): Long {
        val delay = baseDelayMs * (1L shl (retryCount - 1).coerceAtMost(4))
        return delay.coerceAtMost(maxDelayMs)
    }

    private fun isPermanentError(exception: PlaybackException): Boolean {
        val cause = exception.cause
        if (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
            return when (cause.responseCode) {
                403, 404, 410 -> true
                else -> false
            }
        }
        return false
    }
}
