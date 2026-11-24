package tv.nani.launcher

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.dispose
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen carousel — ek waqt pe ek serial, har [slideMs] me agla.
 *
 * Nani ke liye do baatein zaroori hain:
 *  - Remote dabate hi auto-slide ruk jaye ([RESUME_AFTER_MS]), taaki chunte
 *    waqt screen na badle.
 *  - OK theek us pal dabe jab slide badli, toh wahi serial khule jo wo dekh
 *    rahi thi, naya wala nahi ([OK_GRACE_MS]).
 */
class Carousel(
    private val poster: ImageView,
    private val title: TextView,
    private val subtitle: TextView,
    private val dots: LinearLayout,
    private val scope: CoroutineScope
) {
    private var shows: List<Show> = emptyList()
    private var accent = Color.WHITE
    private var slideMs = 3000L

    private var index = 0
    private var prevIndex = 0
    private var lastAutoAdvanceAt = 0L

    private var running = false
    private var autoJob: Job? = null

    fun setShows(list: List<Show>, accent: Int, slideMs: Long) {
        // Refresh (onResume) pe wahi serial dikhta rahe jo pehle tha
        val currentId = shows.getOrNull(index)?.id
        shows = list
        this.accent = accent
        this.slideMs = slideMs
        index = list.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        prevIndex = index

        buildDots()
        render(crossfade = false)
        if (running) schedule(slideMs)
    }

    fun start() {
        running = true
        schedule(slideMs)
    }

    fun stop() {
        running = false
        autoJob?.cancel()
    }

    fun next() = manualMove(+1)
    fun prev() = manualMove(-1)

    /** Wo serial jo Nani abhi dekh rahi hai (OK dabane pe yahi khulega). */
    fun current(): Show? {
        if (shows.isEmpty()) return null
        val justFlipped = SystemClock.uptimeMillis() - lastAutoAdvanceAt < OK_GRACE_MS
        return shows[if (justFlipped) prevIndex else index]
    }

    private fun manualMove(step: Int) {
        if (shows.size < 2) return
        go(step)
        lastAutoAdvanceAt = 0L
        schedule(RESUME_AFTER_MS)
    }

    private fun schedule(firstDelay: Long) {
        autoJob?.cancel()
        if (!running || shows.size < 2) return
        autoJob = scope.launch {
            delay(firstDelay)
            while (true) {
                go(+1)
                lastAutoAdvanceAt = SystemClock.uptimeMillis()
                delay(slideMs)
            }
        }
    }

    private fun go(step: Int) {
        prevIndex = index
        index = Math.floorMod(index + step, shows.size)
        render(crossfade = true)
    }

    private fun render(crossfade: Boolean) {
        val show = shows.getOrNull(index) ?: return

        title.text = show.name.ifBlank { show.nameEn }
        // Zee5 jaise posters me serial ka naam image me hi likha hota hai —
        // wahan bada title dobara mat dikhao. Poster fail hua toh wapas aa jaata hai.
        val hasPoster = show.imageLandscape.isNotBlank() || show.imagePortrait.isNotBlank()
        setTitleVisible(!(show.titleInImage && hasPoster))

        // "Chamanthi · Zee Telugu · Zee5 · New Episode Today" — jo khaali hai wo
        // chhoot jaata hai. Agar title hi English naam hai (Telugu khaali) aur wo
        // dikh raha hai, toh dobara mat likho.
        val badgeText = Badge.textFor(show.schedule)?.let { title.resources.getString(it) }
        val sub = listOf(
            show.nameEn.takeIf { show.name.isNotBlank() || show.titleInImage },
            show.channel,
            show.ott,
            badgeText
        )
            .filter { !it.isNullOrBlank() }
            .joinToString(" · ")
        subtitle.text = sub
        subtitle.visibility = if (sub.isEmpty()) View.GONE else View.VISIBLE

        loadPoster(show, crossfade)

        prefetch(shows.getOrNull(Math.floorMod(index + 1, shows.size)))
        updateDots()
    }

    /** Landscape pehle; na ho ya fail ho toh portrait; wo bhi nahi toh khaali. */
    private fun loadPoster(show: Show, crossfade: Boolean) {
        if (show.imageLandscape.isBlank()) {
            loadPortrait(show, crossfade)
            return
        }
        loadInto(show.imageLandscape, ImageView.ScaleType.CENTER_CROP, crossfade) {
            // Fail hone tak slide badal chuki ho sakti hai — tab kuch mat karo
            if (shows.getOrNull(index)?.id == show.id) loadPortrait(show, crossfade)
        }
    }

    private fun loadPortrait(show: Show, crossfade: Boolean) {
        if (show.imagePortrait.isBlank()) {
            noPoster(show)
            return
        }
        // Portrait ko kaato mat — poora dikhao, side me background
        loadInto(show.imagePortrait, ImageView.ScaleType.FIT_CENTER, crossfade) {
            if (shows.getOrNull(index)?.id == show.id) noPoster(show)
        }
    }

    /** Koi poster nahi chala — wallpaper/dark background + bada naam. */
    private fun noPoster(show: Show) {
        poster.dispose()
        poster.setImageDrawable(null)
        // Naam image me tha, par image hi nahi aayi — title wapas dikhao
        if (show.titleInImage) setTitleVisible(true)
    }

    private fun setTitleVisible(visible: Boolean) {
        title.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun loadInto(
        url: String,
        scale: ImageView.ScaleType,
        crossfade: Boolean,
        onFail: () -> Unit
    ) {
        poster.scaleType = scale
        poster.load(url) {
            // Purana poster hi placeholder — beech me blank flash nahi
            placeholder(poster.drawable)
            crossfade(if (crossfade) CROSSFADE_MS else 0)
            size(POSTER_W, POSTER_H)
            listener(onError = { _, _ -> onFail() })
        }
    }

    private fun prefetch(show: Show?) {
        val url = show?.imageLandscape?.ifBlank { show.imagePortrait }
        if (url.isNullOrBlank()) return
        val ctx = poster.context
        ctx.imageLoader.enqueue(
            ImageRequest.Builder(ctx).data(url).size(POSTER_W, POSTER_H).build()
        )
    }

    private fun buildDots() {
        dots.removeAllViews()
        if (shows.size < 2) {
            dots.visibility = View.GONE
            return
        }
        dots.visibility = View.VISIBLE
        val density = dots.resources.displayMetrics.density
        repeat(shows.size) {
            dots.addView(View(dots.context).apply {
                // Har dot ka apna drawable — shared state ka jhanjhat nahi
                background = GradientDrawable().apply { cornerRadius = 8 * density }
                layoutParams = LinearLayout.LayoutParams(
                    (16 * density).toInt(), (16 * density).toInt()
                ).apply { marginEnd = (12 * density).toInt() }
            })
        }
    }

    private fun updateDots() {
        val density = dots.resources.displayMetrics.density
        for (i in 0 until dots.childCount) {
            val dot = dots.getChildAt(i)
            val active = i == index
            (dot.background as GradientDrawable).setColor(if (active) accent else DOT_INACTIVE)
            dot.layoutParams = dot.layoutParams.apply {
                width = ((if (active) 44 else 16) * density).toInt()
            }
        }
    }

    private companion object {
        const val RESUME_AFTER_MS = 15_000L
        const val OK_GRACE_MS = 800L
        const val CROSSFADE_MS = 500
        // 912MB RAM — 1280x720 bitmap ~3.7MB, full-HD se aadha
        const val POSTER_W = 1280
        const val POSTER_H = 720
        val DOT_INACTIVE = Color.parseColor("#66FFFFFF")
    }
}
