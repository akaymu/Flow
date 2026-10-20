package io.github.aedev.flow.player.media

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import io.github.aedev.flow.player.error.StreamDenialClassifier
import io.github.aedev.flow.player.stream.CaptionTrackResolver
import io.github.aedev.flow.utils.potoken.WebPoTokenSession
import kotlinx.coroutines.runBlocking

private const val TAG = "MediaLoader"

/**
 * Attaches a WEB PO token to a machine-translated caption fetch. Resolved per open on the loader
 * thread rather than when the track list is built, so playback start never waits on a mint and
 * every retry carries a fresh token. A failed mint sends the request as before.
 */
@UnstableApi
internal object TranslatedCaptionAttestation : ResolvingDataSource.Resolver {
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val url = dataSpec.uri.toString()
        val videoId = StreamDenialClassifier.queryParam(url, "v")?.takeIf { it.isNotBlank() } ?: return dataSpec
        val startMs = SystemClock.elapsedRealtime()
        val poToken =
            runBlocking { WebPoTokenSession.mintBounded(videoId) }
                ?.playerRequestPoToken
                ?.takeIf { it.isNotBlank() }
        val mintMs = SystemClock.elapsedRealtime() - startMs
        if (poToken == null) {
            Log.w(TAG, "No PO token for a translated caption after ${mintMs}ms; fetching it unattested")
            return dataSpec
        }
        Log.w(TAG, "Translated caption attested: pot len=${poToken.length} after ${mintMs}ms")
        return dataSpec.withUri(Uri.parse(CaptionTrackResolver.withSubsPoToken(url, poToken)))
    }
}
