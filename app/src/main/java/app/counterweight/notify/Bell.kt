package app.counterweight.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.counterweight.MainActivity
import app.counterweight.R
import app.counterweight.container
import app.counterweight.domain.Engine
import app.counterweight.domain.day
import app.counterweight.localized
import app.counterweight.ru
import app.counterweight.ui.Words
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * The chamber's two small bells. In the morning: a stone whose day has come to be checked, or else the
 * weight of the day. At the hour of worries: only if stones wait on the shelf for it.
 */
object Bell {
    private const val CHANNEL = "bell"
    const val MORNING = "morning"
    const val HOUR = "hour"
    const val OPEN = "open"

    fun channel(c: Context) {
        val ch = NotificationChannel(CHANNEL, c.localized().let { if (it.ru) "Колокольчик палаты" else "The chamber's bell" }, NotificationManager.IMPORTANCE_DEFAULT)
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** The next time the clock reaches [hour] after [now]. */
    fun next(hour: Int, now: Long, zone: ZoneId): Long {
        val today = LocalDate.ofEpochDay(now.day(zone))
        val t = today.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return if (t > now) t else today.plusDays(1).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    }

    /** Plans both bells; called on opening the app, on changing an hour, and after each ring. */
    suspend fun plan(c: Context) {
        val app = c.applicationContext
        val st = app.container.settings.now()
        val wm = WorkManager.getInstance(app)
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        for ((which, hour) in listOf(MORNING to st.morning, HOUR to st.worryHour)) {
            if (!st.onboarded || hour < 0) { wm.cancelUniqueWork(which); continue }
            val req = OneTimeWorkRequestBuilder<BellWorker>().setInitialDelay(next(hour, now, zone) - now, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("which" to which)).build()
            wm.enqueueUniqueWork(which, ExistingWorkPolicy.REPLACE, req)
        }
    }

    fun post(c: Context, id: Int, title: String, text: String, open: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val pi = PendingIntent.getActivity(c, id, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(OPEN, open), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_weight)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(c).notify(id, n)
    }
}

class BellWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val c = ctx.container
        val st = c.settings.now()
        val s = c.repo.refresh()
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val w = Words(ctx.localized().ru, st.gender)
        if (st.onboarded) when (inputData.getString("which")) {
            Bell.MORNING -> {
                val due = s.due(now.day(zone))
                if (due.isNotEmpty()) {
                    val st0 = due.first()
                    val what = st0.words.ifBlank { c.catalog.worry(st0.worry)?.let { w.t(it.text) } ?: "" }
                    Bell.post(ctx, 1, w.t("Поверка весов", "Checking the scales"), w.t("Настал день узнать: «$what». Ну что — сбылось?", "The day has come to find out: “$what”. So, did it happen?"), "ledger")
                } else Engine.ofTheDay(s, c.catalog, now.day(zone), st.salt)?.let { key ->
                    c.catalog.weight(key)?.let { wt -> Bell.post(ctx, 1, w.t("Гирька дня", "Weight of the day"), w.t(wt.text), "scale") }
                }
            }
            Bell.HOUR -> {
                val n = s.waiting.size
                if (n > 0) Bell.post(ctx, 2, w.t("Час тревог", "The hour for worries"),
                    if (w.ru) "На полке ${w.n(n, "камень", "камня", "камней", "", "")}. Можно взвесить — или убедиться, что они стали легче." else "$n ${if (n == 1) "stone waits" else "stones wait"} on the shelf. Weigh them now, or see if they've got lighter.",
                    "shelf")
            }
        }
        Bell.plan(ctx)
        return Result.success()
    }
}
