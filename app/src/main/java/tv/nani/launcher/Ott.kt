package tv.nani.launcher

/**
 * config.json me `"ott": "hotstar"` likho — package naam yaad rakhne ki
 * zarurat nahi. Naya OTT add karna ho toh yahan entry + AndroidManifest ke
 * <queries> me package, dono chahiye.
 */
object Ott {

    /**
     * [titleInPoster]: is OTT ke landscape posters me serial ka naam image me
     * hi likha hota hai — app bada title dobara nahi dikhata. Config me
     * `"title_in_image"` se har serial ke liye badla ja sakta hai.
     */
    data class Info(val displayName: String, val pkg: String, val titleInPoster: Boolean = false)

    private val HOTSTAR = Info("JioHotstar", "in.startv.hotstar")
    private val ZEE5 = Info("Zee5", "com.graymatrix.did", titleInPoster = true)
    private val AHA = Info("aha", "ahaflix.tv")
    private val SUNNXT = Info("Sun NXT", "com.suntv.sunnxt")
    private val ETVWIN = Info("ETV Win", "com.etvwin.androidtv")
    private val SONYLIV = Info("SonyLIV", "com.sonyliv")
    private val NETFLIX = Info("Netflix", "com.netflix.ninja")
    private val PRIME = Info("Prime Video", "com.amazon.amazonvideo.livingroom")
    private val YOUTUBE = Info("YouTube", "com.google.android.youtube.tv")
    private val SMARTTUBE = Info("SmartTube", "org.smarttube.stable")

    // Key: lowercase, bina space/dash ke — "Sun NXT", "sun-nxt", "sunnxt" sab chalega
    private val KNOWN = mapOf(
        "hotstar" to HOTSTAR, "jiohotstar" to HOTSTAR,
        "zee5" to ZEE5,
        "aha" to AHA,
        "sunnxt" to SUNNXT,
        "etvwin" to ETVWIN,
        "sonyliv" to SONYLIV,
        "netflix" to NETFLIX,
        "prime" to PRIME, "primevideo" to PRIME, "amazonprime" to PRIME,
        "youtube" to YOUTUBE,
        "smarttube" to SMARTTUBE
    )

    fun find(name: String): Info? =
        KNOWN[name.lowercase().filter { it.isLetterOrDigit() }]
}
