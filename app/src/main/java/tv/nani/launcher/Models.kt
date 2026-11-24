package tv.nani.launcher

/**
 * Static config JSON (GitHub raw) ka shape. Badge yahan stored nahi hai —
 * app `schedule` + aaj ka din dekhke khud decide karta hai (Badge.kt).
 */
data class Show(
    /** Stable id — refresh pe carousel isi se same serial pe rukta hai */
    val id: String,
    /** Bada naam (Telugu) */
    val name: String,
    /** Chhota subtitle naam, khaali ho sakta hai */
    val nameEn: String,
    /** "Star Maa" jaisa, khaali ho sakta hai */
    val channel: String,
    /** OTT app ka dikhane wala naam ("JioHotstar"), khaali ho sakta hai */
    val ott: String,
    /** config ke "logos" se — channel ka logo URL, khaali ho toh text */
    val channelLogo: String,
    /** config ke "logos" se — OTT ka logo URL, khaali ho toh text */
    val ottLogo: String,
    /** Carousel poster, 1280x720. Khaali ho sakta hai */
    val imageLandscape: String,
    /** Fallback jab landscape na ho / load fail ho. Khaali ho sakta hai */
    val imagePortrait: String,
    /** Poster me naam pehle se likha hai (Zee5) — tab bada title chhupao */
    val titleInImage: Boolean,
    val url: String,
    val pkg: String,
    /** daily | mon-fri | mon-sat | weekly:sat | none */
    val schedule: String
)

data class Config(
    /** Config kab badla — sirf logging/debug ke liye */
    val updated: String,
    val wallpaper: String?,
    val accent: String,
    /** Carousel me har serial kitne second dikhe */
    val slideSeconds: Int,
    val shows: List<Show>
)
