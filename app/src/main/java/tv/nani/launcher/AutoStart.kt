package tv.nani.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * TV on hote hi Nani TV saamne aaye.
 *
 *  - Cold boot  → [BootReceiver] → [WakeService] → [BOOT_DELAY_MS] baad app
 *  - Standby se jaagna (screen on) → [WakeService] ka receiver → [SCREEN_ON_DELAY_MS] baad app
 *
 * SCREEN_ON sirf chalti hui process ko milta hai (manifest se nahi), isliye
 * WakeService foreground me chalti rehti hai.
 *
 * Android 10+ pe background se activity kholne ke liye SYSTEM_ALERT_WINDOW
 * appop chahiye — TV pe settings screen nahi hai, adb se ek baar:
 *   adb shell appops set tv.nani.launcher SYSTEM_ALERT_WINDOW allow
 */
object AutoStart {
    const val TAG = "NaniTV"

    // Google TV ka home pehle load ho le, warna wo hamare upar aa jaata hai
    const val BOOT_DELAY_MS = 10_000L
    const val SCREEN_ON_DELAY_MS = 3_000L

    fun ensureService(ctx: Context, reason: String) {
        val i = Intent(ctx, WakeService::class.java).putExtra(WakeService.EXTRA_REASON, reason)
        ContextCompat.startForegroundService(ctx, i)
    }

    fun bringToFront(ctx: Context, reason: String) {
        Log.d(TAG, "auto-start: $reason → MainActivity")
        val i = Intent(ctx, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )
        runCatching { ctx.startActivity(i) }
            .onFailure { Log.d(TAG, "auto-start fail: ${it.message}") }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Log.d(AutoStart.TAG, "boot broadcast: ${intent.action}")
        AutoStart.ensureService(ctx, WakeService.REASON_BOOT)
    }
}

class WakeService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private var registered = false

    private val screenOn = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            Log.d(AutoStart.TAG, "screen on")
            main.removeCallbacksAndMessages(null)
            main.postDelayed(
                { AutoStart.bringToFront(this@WakeService, "screen on") },
                AutoStart.SCREEN_ON_DELAY_MS
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        ContextCompat.registerReceiver(
            this, screenOn, IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        registered = true
        Log.d(AutoStart.TAG, "wake service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra(EXTRA_REASON) == REASON_BOOT) {
            main.postDelayed(
                { AutoStart.bringToFront(this, "boot") },
                AutoStart.BOOT_DELAY_MS
            )
        }
        // System maare toh dobara chalao
        return START_STICKY
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        if (registered) unregisterReceiver(screenOn)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Auto start", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this, NOTIF_ID, n,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
    }

    companion object {
        const val EXTRA_REASON = "reason"
        const val REASON_BOOT = "boot"
        private const val CHANNEL = "autostart"
        private const val NOTIF_ID = 1
    }
}
