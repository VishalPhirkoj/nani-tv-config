package tv.nani.launcher

import androidx.annotation.StringRes
import java.util.Calendar

/**
 * Aaj naya episode hai ya nahi — schedule se. Hotstar/Zee5 ka koi public
 * API nahi hai, isliye ye hi sabse reliable tareeka hai.
 *
 * Device ka local time use hota hai — TV IST pe set hai.
 */
object Badge {

    // Calendar.DAY_OF_WEEK: SUNDAY = 1 ... SATURDAY = 7. Raw numbers ki
    // jagah constants, taaki Sunday=1 wali galti na ho.
    private val DAY_NAMES = mapOf(
        "mon" to Calendar.MONDAY,
        "tue" to Calendar.TUESDAY,
        "wed" to Calendar.WEDNESDAY,
        "thu" to Calendar.THURSDAY,
        "fri" to Calendar.FRIDAY,
        "sat" to Calendar.SATURDAY,
        "sun" to Calendar.SUNDAY
    )

    private val MON_FRI = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY
    )

    private val MON_SAT = MON_FRI + Calendar.SATURDAY

    /** Badge ka string resource id, ya null agar aaj badge nahi dikhana. */
    @StringRes
    fun textFor(schedule: String): Int? =
        textFor(schedule, Calendar.getInstance().get(Calendar.DAY_OF_WEEK))

    /** [dayOfWeek] = Calendar.DAY_OF_WEEK value. Alag rakha hai taaki test ho sake. */
    @StringRes
    internal fun textFor(schedule: String, dayOfWeek: Int): Int? {
        val s = schedule.trim().lowercase()

        val airsToday = when {
            s == "daily" -> true
            s == "mon-fri" -> dayOfWeek in MON_FRI
            s == "mon-sat" -> dayOfWeek in MON_SAT
            s.startsWith("weekly:") -> {
                // "weekly:saturday" bhi chalega — pehle 3 akshar hi dekhte hain
                val day = s.substringAfter(':').trim().take(3)
                DAY_NAMES[day] == dayOfWeek
            }
            else -> false // "none" ya unknown
        }

        return if (airsToday) R.string.badge_new_episode else null
    }
}
