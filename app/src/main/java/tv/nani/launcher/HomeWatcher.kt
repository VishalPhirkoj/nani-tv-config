package tv.nani.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Haier ka firmware remote ke Home button pe hamesha Android TV launcher
 * kholta hai — default-home / HOME role ignore karta hai. Ye service dekhti hai
 * ki Android TV launcher saamne aaya, aur Nani TV ko wapas le aati hai.
 *
 * Events (res/xml/home_watcher.xml): window-state (kaunsa app saamne hai —
 * sirf package naam) + focus (Android TV launcher pe button dabna). Screen ka
 * content kabhi nahi padha jaata.
 *
 * Bahar nikalna: Nani TV me Back 3 sec pakdo → [openStockLauncher]. Phir:
 *  - Android TV home pe [IDLE_MS] tak koi button nahi dabaya → Nani TV wapas
 *  - Har button dabne pe timer dobara shuru (koi chala raha hai toh mat kudo)
 *  - Settings/doosra app khula hai toh beech me mat aao; wapas Android home
 *    pe aaye aur time ho chuka toh Nani TV
 *
 * adb se ek baar enable (AnyDesk wali service ke saath ':' se):
 *   adb shell settings put secure enabled_accessibility_services <existing>:tv.nani.launcher/tv.nani.launcher.HomeWatcher
 *   adb shell settings put secure accessibility_enabled 1
 */
class HomeWatcher : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())
    private var lastBringAt = 0L
    /** Abhi screen pe kaunsa app hai (system UI popups ko chhod ke) */
    private var foregroundPkg: String? = null

    private val idleCheck = Runnable { checkIdle() }

    override fun onServiceConnected() {
        instance = this
        Log.d(AutoStart.TAG, "home watcher connected")
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val type = event.eventType

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg !in IGNORED_PKGS) {
            foregroundPkg = pkg
        }
        if (pkg != STOCK_LAUNCHER) return
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_VIEW_FOCUSED) return

        val now = SystemClock.elapsedRealtime()
        if (now < escapeUntil) {
            // Escape chal raha hai aur koi Android home chala raha hai — timer badhao
            escapeUntil = now + IDLE_MS
            scheduleIdleCheck()
            return
        }
        bring(if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) "home button" else "launcher focus")
    }

    override fun onInterrupt() {}

    private fun bring(why: String) {
        val now = SystemClock.elapsedRealtime()
        // Ek Home pe kai events aate hain — ek hi baar laao
        if (now - lastBringAt < DEBOUNCE_MS) return
        lastBringAt = now
        AutoStart.bringToFront(this, why)
    }

    private fun scheduleIdleCheck() {
        main.removeCallbacks(idleCheck)
        val wait = escapeUntil - SystemClock.elapsedRealtime()
        main.postDelayed(idleCheck, wait.coerceAtLeast(0L) + 200L)
    }

    private fun checkIdle() {
        if (SystemClock.elapsedRealtime() < escapeUntil) {
            scheduleIdleCheck() // beech me kisi ne button dabaya tha
            return
        }
        if (foregroundPkg == STOCK_LAUNCHER) {
            bring("android home idle ${IDLE_MS / 1000}s")
        } else {
            Log.d(AutoStart.TAG, "escape khatam, par '$foregroundPkg' khula hai — beech me nahi aaye")
        }
    }

    companion object {
        const val STOCK_LAUNCHER = "com.google.android.tvlauncher"
        private const val IDLE_MS = 2 * 60_000L
        private const val DEBOUNCE_MS = 1_500L

        // Volume bar, toasts jaise popups — inse "kaunsa app saamne hai" mat badlo
        private val IGNORED_PKGS = setOf("com.android.systemui", "android")

        /** Same process me MainActivity set karti hai */
        @Volatile private var escapeUntil = 0L
        @Volatile private var instance: HomeWatcher? = null

        /** Settings/doosre apps ke liye Android TV launcher kholo, watcher idle tak chup. */
        fun openStockLauncher(ctx: Context) {
            escapeUntil = SystemClock.elapsedRealtime() + IDLE_MS
            Log.d(AutoStart.TAG, "escape: Android home, ${IDLE_MS / 1000}s idle pe wapas")
            instance?.let { w -> w.main.post { w.scheduleIdleCheck() } }
            val i = ctx.packageManager.getLaunchIntentForPackage(STOCK_LAUNCHER)
                ?: Intent(Intent.ACTION_MAIN).setPackage(STOCK_LAUNCHER)
                    .addCategory(Intent.CATEGORY_HOME)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { ctx.startActivity(i) }
                .onFailure { Log.d(AutoStart.TAG, "escape fail: ${it.message}") }
        }
    }
}
