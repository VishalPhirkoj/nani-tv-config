package tv.nani.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Haier ka firmware remote ke Home button pe hamesha Android TV launcher
 * kholta hai — default-home / HOME role ignore karta hai. Ye service dekhti hai
 * ki Android TV launcher saamne aaya, aur Nani TV ko wapas le aati hai.
 *
 * Sirf [STOCK_LAUNCHER] package ke window-change events aate hain
 * (res/xml/home_watcher.xml) — screen ka content kabhi nahi padha jaata.
 *
 * adb se ek baar enable:
 *   adb shell settings put secure enabled_accessibility_services tv.nani.launcher/tv.nani.launcher.HomeWatcher
 *   adb shell settings put secure accessibility_enabled 1
 *
 * Bahar nikalna: Nani TV me Back lamba dabao → [openStockLauncher], aur
 * [ESCAPE_MS] tak ye service kuch nahi karti.
 */
class HomeWatcher : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName != STOCK_LAUNCHER) return
        if (SystemClock.elapsedRealtime() < escapeUntil) {
            Log.d(AutoStart.TAG, "stock launcher — escape window, chhod diya")
            return
        }
        AutoStart.bringToFront(this, "home button")
    }

    override fun onInterrupt() {}

    companion object {
        const val STOCK_LAUNCHER = "com.google.android.tvlauncher"
        private const val ESCAPE_MS = 2 * 60_000L

        /** Same process me MainActivity set karti hai */
        @Volatile private var escapeUntil = 0L

        /** Settings/doosre apps ke liye Android TV launcher kholo, watcher thodi der chup. */
        fun openStockLauncher(ctx: Context) {
            escapeUntil = SystemClock.elapsedRealtime() + ESCAPE_MS
            Log.d(AutoStart.TAG, "escape: stock launcher ${ESCAPE_MS / 1000}s ke liye")
            val i = ctx.packageManager.getLaunchIntentForPackage(STOCK_LAUNCHER)
                ?: Intent(Intent.ACTION_MAIN).setPackage(STOCK_LAUNCHER)
                    .addCategory(Intent.CATEGORY_HOME)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { ctx.startActivity(i) }
                .onFailure { Log.d(AutoStart.TAG, "escape fail: ${it.message}") }
        }
    }
}
