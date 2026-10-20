package io.github.aedev.flow.ui.components.categories

import android.content.res.Resources
import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.R
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.io.IOException

private const val RATE_LIMITED = "rate limited"
private const val FAILED = "failed"

class CategoryErrorsTest {
    private val resources =
        mockk<Resources> {
            every { getQuantityString(R.plurals.categories_rate_limited, 1) } returns RATE_LIMITED
            every { getString(R.string.error_failed_to_load_videos) } returns FAILED
        }

    private fun refusal(status: HttpStatusCode): Throwable {
        val response = mockk<HttpResponse>(relaxed = true) { every { this@mockk.status } returns status }
        return ResponseException(response, "{\"error\":{}}")
    }

    @Test
    fun `a rate limit or a server error asks the viewer to wait`() {
        assertThat(categoryErrorMessage(resources, refusal(HttpStatusCode.TooManyRequests))).isEqualTo(RATE_LIMITED)
        assertThat(categoryErrorMessage(resources, refusal(HttpStatusCode.ServiceUnavailable))).isEqualTo(RATE_LIMITED)
    }

    @Test
    fun `anything else is the plain failure, never the exception text`() {
        assertThat(categoryErrorMessage(resources, refusal(HttpStatusCode.BadRequest))).isEqualTo(FAILED)
        assertThat(categoryErrorMessage(resources, IOException("Unable to resolve host"))).isEqualTo(FAILED)
    }
}
