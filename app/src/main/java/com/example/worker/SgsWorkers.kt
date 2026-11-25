package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.LocalStorage
import com.example.data.remote.GasApiClient
import com.example.data.repository.SchoolRepository
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class SgsSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val storage = LocalStorage(applicationContext)
        val pending = storage.loadPendingSaves()
        if (pending.isEmpty()) return Result.success()

        val api = GasApiClient(applicationContext)
        if (!api.isOnline()) return Result.retry()

        val repo = SchoolRepository(applicationContext, storage, api)
        val session = repo.currentSession
        if (session == null || session.offline || session.token.isBlank()) {
            return Result.success()
        }

        val (sent, failed) = repo.flushPendingSaves()
        repo.flushClientErrors()
        if (sent > 0) {
            val leftMsg = if (failed > 0) " (बाकी: $failed)" else ""
            NotificationHelper.showNotification(
                context = applicationContext,
                channelId = NotificationHelper.CHANNEL_SYNC,
                notificationId = 1001,
                title = "☁️ SGS अ‍ॅप ऑटो-सिंक पूर्ण",
                body = "$sent प्रलंबित नोंदी सर्व्हरवर यशस्वीरित्या जतन झाल्या.$leftMsg"
            )
        }
        return if (failed > 0) Result.retry() else Result.success()
    }
}

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val storage = LocalStorage(applicationContext)
        if (!storage.getRemindersEnabled()) return Result.success()

        val todayIso = MarathiUtils.todayIso()
        if (storage.getLastReminderDate() == todayIso) return Result.success()

        val api = GasApiClient(applicationContext)
        val repo = SchoolRepository(applicationContext, storage, api)
        val session = repo.currentSession ?: return Result.success()

        val lines = mutableListOf<String>()

        // 1. Check student birthdays today from local cache
        val todayMmDd = todayIso.drop(5) // MM-DD
        val cached = storage.loadStudentCache()?.first.orEmpty()
        val bdays = cached.filter { s ->
            val iso = MarathiUtils.isoDateOrEmpty(s.dob)
            iso.length >= 10 && iso.drop(5) == todayMmDd
        }
        if (bdays.isNotEmpty()) {
            val names = bdays.take(3).joinToString(", ") { it.fullName } +
                if (bdays.size > 3) " व इतर ${bdays.size - 3}" else ""
            lines.add("🎂 आज वाढदिवस (${bdays.size}): $names")
        }

        // 2. Check pending attendance on working days after 11:00 AM (skip Sundays & saved school holidays)
        val nowTime = LocalTime.now()
        val isOffDay = MarathiUtils.isHolidayOrSunday(todayIso, storage.loadHolidays())
        if (!isOffDay && nowTime.hour >= 11 && api.isOnline() && !session.offline) {
            if (session.role == UserRole.TEACHER && session.iyatta.isNotBlank()) {
                val r = repo.request(
                    mapOf(
                        "action" to "getTeacherDashboard",
                        "iyatta" to session.iyatta,
                        "tukdi" to session.tukdi
                    )
                )
                val marked = r.raw["attMarkedToday"]?.toString()?.contains("true") == true
                if (r.isOk && !marked) {
                    lines.add("⏰ आजची वर्ग हजेरी (${MarathiUtils.classToMr(session.iyatta)} ${session.tukdi}) भरणे बाकी आहे.")
                }
            } else if (session.role == UserRole.SUPER || session.role == UserRole.MASTER) {
                val r = repo.request(mapOf("action" to "getPendingAttendance"))
                val pendingArr = r.raw["pending"] as? kotlinx.serialization.json.JsonArray
                val pendingCount = pendingArr?.size ?: 0
                if (r.isOk && pendingCount > 0) {
                    lines.add("📋 आज $pendingCount वर्गांची हजेरी भरणे बाकी आहे.")
                }
            }
        }

        // 3. Pending offline saves reminder
        val pendingSavesCount = storage.loadPendingSaves().size
        if (pendingSavesCount > 0) {
            lines.add("⏳ अ‍ॅपमध्ये $pendingSavesCount नोंदी सिंक होणे बाकी आहेत.")
        }

        if (lines.isNotEmpty()) {
            storage.saveLastReminderDate(todayIso)
            NotificationHelper.showNotification(
                context = applicationContext,
                channelId = NotificationHelper.CHANNEL_REMINDER,
                notificationId = 1002,
                title = "🏫 SGS शाळा दैनंदिन सूचना",
                body = lines.joinToString("\n")
            )
        }
        return Result.success()
    }
}

object SgsWorkScheduler {
    private const val UNIQUE_SYNC_WORK = "sgs_pending_sync_work"
    private const val UNIQUE_REMINDER_WORK = "sgs_daily_reminder_work"

    fun enqueuePendingSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val req = OneTimeWorkRequestBuilder<SgsSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_SYNC_WORK, ExistingWorkPolicy.REPLACE, req)
    }

    fun scheduleDailyReminders(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(UNIQUE_REMINDER_WORK)
            return
        }
        val req = PeriodicWorkRequestBuilder<DailyReminderWorker>(12, TimeUnit.HOURS)
            .build()
        wm.enqueueUniquePeriodicWork(
            UNIQUE_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}
