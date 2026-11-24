package tv.nani.launcher

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Network pehle, cache baad me. GitHub down ho ya wifi na ho toh
 * Nani ko blank screen nahi dikhega — last good config chalega.
 */
class Repo(ctx: Context) {

    private companion object {
        const val TAG = "NaniTV"
    }

    private val cache = File(ctx.filesDir, "config.json")

    suspend fun load(): Pair<Config?, Boolean> = withContext(Dispatchers.IO) {
        val fresh = fetch()
        if (fresh != null) {
            val parsed = parse(fresh)
            if (parsed != null) {
                Log.d(TAG, "fetch success: HTTP ${HttpURLConnection.HTTP_OK}, ${parsed.shows.size} shows parsed (updated=${parsed.updated.ifBlank { "?" }})")
                runCatching { cache.writeText(fresh) }
                return@withContext parsed to true
            }
            Log.d(TAG, "fetch fail: response parse nahi hua (invalid JSON?)")
        }
        val cached = runCatching { cache.readText() }.getOrNull()
        val fromCache = cached?.let { parse(it) }
        if (fromCache != null) {
            Log.d(TAG, "cache se load hua: ${fromCache.shows.size} shows (updated=${fromCache.updated.ifBlank { "?" }})")
        } else {
            Log.d(TAG, "cache se load fail: ${if (cached == null) "cache file nahi hai" else "cache parse nahi hua"}")
        }
        fromCache to false
    }

    private fun fetch(): String? {
        Log.d(TAG, "fetch start: ${BuildConfig.CONFIG_URL}")
        return runCatching {
            val conn = (URL(BuildConfig.CONFIG_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }
            try {
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "fetch fail: HTTP $code")
                    return null
                }
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.onFailure {
            Log.d(TAG, "fetch fail: ${it.javaClass.simpleName}: ${it.message}")
        }.getOrNull()
    }

    private fun parse(raw: String): Config? = runCatching {
        val root = JSONObject(raw)
        val arr = root.getJSONArray("shows")

        val shows = ArrayList<Show>(arr.length())
        for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i)
            // "enabled": false → chhupa do, entry delete karne ki zarurat nahi
            if (!s.optBoolean("enabled", true)) continue
            // "ott": "hotstar" → package khud mil jaata hai. "package" diya ho toh wahi.
            val ottRaw = s.str("ott")
            val ott = Ott.find(ottRaw)
            // url aur package na ho toh wo serial skip — poora screen nahi girta
            val url = s.str("url")
            val pkg = s.str("package").ifBlank { ott?.pkg.orEmpty() }
            if (url.isBlank() || pkg.isBlank()) {
                Log.d(TAG, "skip show '${s.str("name")}': url/package nahi (ott='$ottRaw')")
                continue
            }

            shows += Show(
                id = s.str("id").ifBlank { url },
                name = s.str("name"),
                nameEn = s.str("name_en"),
                channel = s.str("channel"),
                ott = ott?.displayName ?: ottRaw,
                // Purana "image" field bhi chalega
                imageLandscape = s.str("image_landscape").ifBlank { s.str("image") },
                imagePortrait = s.str("image_portrait"),
                // Config me diya ho toh wahi, warna OTT ka default (Zee5 = true)
                titleInImage = if (s.has("title_in_image")) s.optBoolean("title_in_image")
                               else ott?.titleInPoster ?: false,
                url = url,
                pkg = pkg,
                schedule = s.str("schedule").ifBlank { "daily" }
            )
        }

        Config(
            updated = root.str("updated"),
            wallpaper = root.str("wallpaper").takeIf { it.isNotBlank() },
            accent = root.str("accent").ifBlank { "#E6C068" },
            slideSeconds = root.optInt("slide_seconds", 3).coerceIn(2, 30),
            shows = shows
        )
    }.getOrNull()

    /**
     * Trimmed string, ya "". Android ka optString() JSON `null` ko "null"
     * text bana deta hai — `"wallpaper": null` pe "null" URL load hota tha.
     */
    private fun JSONObject.str(key: String): String =
        if (isNull(key)) "" else optString(key).trim()
}
