package tv.nani.launcher

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var background: ImageView
    private lateinit var status: TextView
    private lateinit var spinner: ProgressBar
    private lateinit var carousel: Carousel

    private var loadedOnce = false
    private var escapeFired = false

    private companion object {
        const val ESCAPE_HOLD_MS = 3_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        background = findViewById(R.id.background)
        status = findViewById(R.id.status)
        spinner = findViewById(R.id.spinner)
        carousel = Carousel(
            poster = findViewById(R.id.poster),
            title = findViewById(R.id.title),
            subtitle = findViewById(R.id.subtitle),
            dots = findViewById(R.id.dots),
            scope = lifecycleScope
        )

        // Install/update ke baad bhi (bina reboot) screen-on listener chalu ho
        AutoStart.ensureService(this, "activity")

        load()
    }

    override fun onResume() {
        super.onResume()
        carousel.start()
        // OTT app se wapas aane pe list + badges refresh. Pehli baar skip —
        // onCreate already chala chuka hai.
        if (loadedOnce) load(silent = true)
    }

    override fun onPause() {
        // OTT app chal raha ho tab background me slide ghumane ka koi matlab nahi
        carousel.stop()
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> carousel.next()
            KeyEvent.KEYCODE_DPAD_LEFT -> carousel.prev()
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A -> {
                // Long-press pe repeat events aate hain — sirf pehla
                if (event.repeatCount == 0) carousel.current()?.let { open(it) }
            }
            // Ye TV ka home hai — Back dabane pe kuch nahi (warna app band →
            // khaali screen). Back 3 sec PAKADNA = bahar nikalne ka raasta
            // (settings / doosre apps). Android ka default long-press 0.5 sec hai —
            // Nani se galti se ho jaata tha, isliye apna lamba threshold.
            KeyEvent.KEYCODE_BACK -> {
                val held = event.eventTime - event.downTime
                if (event.repeatCount > 0 && !escapeFired && held >= ESCAPE_HOLD_MS) {
                    escapeFired = true
                    HomeWatcher.openStockLauncher(this)
                }
            }
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Back chhoda: kuch nahi. super ko mat do, warna onBackPressed → app band
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            escapeFired = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun load(silent: Boolean = false) {
        if (!silent) {
            spinner.visibility = View.VISIBLE
            status.visibility = View.GONE
        }

        lifecycleScope.launch {
            val (config, fromNetwork) = Repo(this@MainActivity).load()
            spinner.visibility = View.GONE
            loadedOnce = true

            if (config == null || config.shows.isEmpty()) {
                status.visibility = View.VISIBLE
                status.text = getString(R.string.no_data)
                return@launch
            }

            status.visibility = View.GONE

            config.wallpaper?.let {
                background.load(it) { crossfade(true) }
            }

            val accent = runCatching { Color.parseColor(config.accent) }
                .getOrDefault(Color.parseColor("#E6C068"))

            carousel.setShows(config.shows, accent, config.slideSeconds * 1000L)

            if (!fromNetwork && !silent) {
                Toast.makeText(this@MainActivity, R.string.offline, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun open(show: Show) {
        // -p wala behaviour: chooser dialog kabhi nahi aayega
        val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse(show.url)).apply {
            setPackage(show.pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (deepLink.resolveActivity(packageManager) != null) {
            startActivity(deepLink)
            return
        }

        // Deep link toota (app update, URL badla) — app toh khol do,
        // Nani ko kam se kam sahi app me land karna chahiye
        val fallback = packageManager.getLeanbackLaunchIntentForPackage(show.pkg)
            ?: packageManager.getLaunchIntentForPackage(show.pkg)

        if (fallback != null) {
            startActivity(fallback)
        } else {
            Toast.makeText(this, R.string.app_missing, Toast.LENGTH_LONG).show()
        }
    }
}
