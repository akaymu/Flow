package io.github.aedev.flow.ui.components.categories

import android.content.res.Resources
import io.github.aedev.flow.R
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode

/**
 * What a failed Explore load says. Exception text, which carries the request URL and body, never reaches the screen.
 * The rate-limit message is a single-quantity plural because that is how the translation platform stores it.
 */
internal fun categoryErrorMessage(
    resources: Resources,
    error: Throwable,
): String =
    if (error.isRateLimitOrOutage()) {
        resources.getQuantityString(R.plurals.categories_rate_limited, 1)
    } else {
        resources.getString(R.string.error_failed_to_load_videos)
    }

/** A refusal or outage on YouTube's side: nothing the viewer can fix except waiting. */
internal fun Throwable.isRateLimitOrOutage(): Boolean {
    val status = (this as? ResponseException)?.response?.status ?: return false
    return status == HttpStatusCode.TooManyRequests || status.value >= HttpStatusCode.InternalServerError.value
}
