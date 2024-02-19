import kotlinx.coroutines.*
import java.time.Duration
import kotlin.system.measureTimeMillis

class AsyncStatusChecker(private val client: Client) : Handler {

    override fun performOperation(id: String): ApplicationStatusResponse {
        var response: ApplicationStatusResponse? = null
        val job = GlobalScope.launch {
            val deferreds = listOf(
                async { client.getApplicationStatus1(id) },
                async { client.getApplicationStatus2(id) }
            )
            val result = deferreds.awaitFirstNonNullResult()
            response = when (result) {
                is Response.Success -> ApplicationStatusResponse.Success(result.applicationId, result.applicationStatus)
                is Response.Failure -> ApplicationStatusResponse.Failure(null, 1) // Simplified error handling
                else -> ApplicationStatusResponse.Failure(null, 0)
            }
        }

        runBlocking {
            withTimeoutOrNull(Duration.ofSeconds(15).toMillis()) {
                job.join()
            } ?: return ApplicationStatusResponse.Failure(Duration.ofSeconds(15), 0) // Timeout handling
        }

        return response!!
    }

    private suspend fun List<Deferred<Response>>.awaitFirstNonNullResult(): Response? {
        val responses = this.mapNotNull { it.awaitOrNull() }
        return responses.firstOrNull()
    }

    private fun <T> Deferred<T>.awaitOrNull(): T? {
        return try {
            this.await()
        } catch (e: Exception) {
            null
        }
    }
}
