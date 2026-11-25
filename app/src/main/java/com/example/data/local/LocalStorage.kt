package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.domain.model.HolidayItem
import com.example.domain.model.MessageTemplate
import com.example.domain.model.OfflineCredRecord
import com.example.domain.model.PendingSaveItem
import com.example.domain.model.Student
import com.example.domain.model.UserSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Serializable
data class OfflineFailState(
    val count: Int = 0,
    val lockedUntil: Long = 0L
)

@Serializable
data class ClientErrorLogItem(
    val v: String,
    val pg: String,
    val ua: String,
    val t: String,
    val m: String,
    val s: String = "",
    val c: Int = 1
)

sealed class OfflineLoginResult {
    data class Success(val session: UserSession) : OfflineLoginResult()
    data object NotFoundOrExpired : OfflineLoginResult()
    data class WrongPassword(val attemptsLeft: Int) : OfflineLoginResult()
    data class Locked(val minutesLeft: Int) : OfflineLoginResult()
}

class LocalStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sgs_school_prefs_v2", Context.MODE_PRIVATE)

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    companion object {
        const val DEFAULT_SHEETS_URL =
            "https://script.google.com/macros/s/AKfycbwxuyhQrvIUeghpGyqsK4SXvqlPv1rfcSalV90b67LIhDCgo4k3j0bK7ef3ZZUCdEA/exec"
        private const val KEY_SHEETS_URL = "sgs_url"
        private const val KEY_SESSION = "sgs_session_v2"
        private const val KEY_OFFLINE_CREDS = "sgs_offcred_v1"
        private const val KEY_OFFLINE_FAILS = "sgs_offfails_v1"
        private const val KEY_CLIENT_ERRORS = "sgs_client_errs_v1"
        private const val KEY_STUDENT_CACHE = "sgs_student_cache_v1"
        private const val KEY_STUDENT_CACHE_TIME = "sgs_student_cache_time_v1"
        private const val KEY_RECENT_SEARCH = "sgs_recent_search_v1"
        private const val KEY_PENDING_SAVES = "sgs_pending_saves_v1"
        private const val KEY_AC_YEAR = "sgs_ac_year_v1"
        private const val KEY_DARK_THEME = "sgs_dark_theme_v1"
        private const val KEY_BIOMETRIC_ENABLED = "sgs_bio_enabled_v1"
        private const val KEY_BIOMETRIC_USER = "sgs_bio_user_v1"
        private const val KEY_REMINDERS_ENABLED = "sgs_reminders_enabled_v1"
        private const val KEY_LAST_REMINDER_DATE = "sgs_last_reminder_date_v1"
        private const val KEY_HOLIDAYS = "sgs_holidays_v1"
        private const val KEY_MESSAGE_TEMPLATES = "sgs_msg_templates_v1"
        private const val KEY_PINNED_QUICK_PREFIX = "sgs_pinned_quick_v1:"
        private const val KEY_AUTO_LOCK_MINUTES = "sgs_auto_lock_mins_v1"
        private const val KEY_LAST_BACKGROUND_MS = "sgs_last_bg_ms_v1"
        private const val SWR_PREFIX = "sgs_swr_v1965:"
        private const val CACHE_TTL_MS = 30L * 60L * 1000L
        private const val OFFLINE_CRED_TTL_MS = 3L * 24L * 3600L * 1000L
        private const val OFFLINE_MAX_FAILS = 5
        private const val OFFLINE_LOCK_MS = 5L * 60L * 1000L
        private const val SWR_MAX_AGE_MS = 24L * 3600L * 1000L
        private const val SWR_ROSTER_MAX_AGE_MS = 3L * 24L * 3600L * 1000L
        private val SWR_NEVER = Regex("fee|thakbaki|arrear|payment|receipt", RegexOption.IGNORE_CASE)
    }

    fun getSheetsUrl(): String =
        prefs.getString(KEY_SHEETS_URL, DEFAULT_SHEETS_URL)?.trim().takeUnless { it.isNullOrBlank() }
            ?: DEFAULT_SHEETS_URL

    fun saveSheetsUrl(url: String) {
        prefs.edit().putString(KEY_SHEETS_URL, url.trim()).apply()
    }

    fun getAcYear(): String = prefs.getString(KEY_AC_YEAR, "2026-27") ?: "2026-27"

    fun saveAcYear(year: String) {
        if (year.isNotBlank()) prefs.edit().putString(KEY_AC_YEAR, year.trim()).apply()
    }

    fun getIsDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, true)

    fun saveIsDarkTheme(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, dark).apply()
    }

    fun getBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun saveBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getBiometricUsername(): String = prefs.getString(KEY_BIOMETRIC_USER, "") ?: ""

    fun saveBiometricUsername(username: String) {
        prefs.edit().putString(KEY_BIOMETRIC_USER, username.trim()).apply()
    }

    fun getValidBiometricCredential(): OfflineCredRecord? {
        if (!getBiometricEnabled()) return null
        val uname = getBiometricUsername()
        if (uname.isBlank()) return null
        val rec = getOfflineCredential(uname) ?: return null
        return if (rec.role.equals("super", ignoreCase = true)) null else rec
    }

    fun getRemindersEnabled(): Boolean = prefs.getBoolean(KEY_REMINDERS_ENABLED, true)

    fun saveRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
    }

    fun getLastReminderDate(): String = prefs.getString(KEY_LAST_REMINDER_DATE, "") ?: ""

    fun saveLastReminderDate(dateIso: String) {
        prefs.edit().putString(KEY_LAST_REMINDER_DATE, dateIso).apply()
    }

    fun loadHolidays(): List<HolidayItem> {
        val raw = prefs.getString(KEY_HOLIDAYS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<HolidayItem>>(raw).sortedBy { it.dateIso }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHolidays(holidays: List<HolidayItem>) {
        val sorted = holidays.distinctBy { it.dateIso }.sortedBy { it.dateIso }
        prefs.edit().putString(KEY_HOLIDAYS, json.encodeToString(sorted)).apply()
    }

    fun defaultMessageTemplates(): List<MessageTemplate> = listOf(
        MessageTemplate(
            id = "tpl_pta",
            title = "पालक सभा निमंत्रण",
            body = "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी {नाव} ({वर्ग}, Roll {रोल}) यांच्या पालकांना कळविण्यात येते की, शाळेत पालक सभा आयोजित केली आहे. कृपया वेळेत उपस्थित राहावे.",
            isDefault = true
        ),
        MessageTemplate(
            id = "tpl_exam",
            title = "परीक्षा वेळापत्रक सूचना",
            body = "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी {नाव} ({वर्ग}) यांची आगामी परीक्षा वेळापत्रक जाहीर झाले आहे. कृपया नियमित अभ्यास व उपस्थितीची काळजी घ्यावी.",
            isDefault = true
        ),
        MessageTemplate(
            id = "tpl_fee",
            title = "फी भरणा आठवण",
            body = "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी {नाव} ({वर्ग}, रजि. क्र. {नोंद}) यांची उर्वरित शैक्षणिक फी कृपया लवकरात लवकर शाळेत जमा करावी ही विनंती.",
            isDefault = true
        ),
        MessageTemplate(
            id = "tpl_trip",
            title = "उपक्रम / सहल संमती",
            body = "नमस्कार, विद्यार्थी {नाव} ({वर्ग}, Roll {रोल}) यांना शालेय शैक्षणिक उपक्रमात सहभागी होण्यासाठी पालकांचे संमतीपत्र पाठवावे. — श्री. गो. से. हायस्कूल, पाचोरा",
            isDefault = true
        ),
        MessageTemplate(
            id = "tpl_praise",
            title = "कौतुक व अभिनंदन",
            body = "🌟 अभिनंदन! विद्यार्थी {नाव} ({वर्ग}, Roll {रोल}) यांनी शाळेतील उपक्रमात उत्तम कामगिरी केली आहे. विद्यालयातर्फे हार्दिक शुभेच्छा! — श्री. गो. से. हायस्कूल, पाचोरा",
            isDefault = true
        )
    )

    fun loadMessageTemplates(): List<MessageTemplate> {
        val raw = prefs.getString(KEY_MESSAGE_TEMPLATES, null)
        if (raw == null) {
            val defaults = defaultMessageTemplates()
            saveMessageTemplates(defaults)
            return defaults
        }
        return try {
            json.decodeFromString<List<MessageTemplate>>(raw)
        } catch (_: Exception) {
            defaultMessageTemplates()
        }
    }

    fun saveMessageTemplates(list: List<MessageTemplate>) {
        prefs.edit().putString(KEY_MESSAGE_TEMPLATES, json.encodeToString(list)).apply()
    }

    fun resetDefaultMessageTemplates(): List<MessageTemplate> {
        val defaults = defaultMessageTemplates()
        saveMessageTemplates(defaults)
        return defaults
    }

    fun loadPinnedQuickKeys(username: String): List<String> {
        val u = username.trim().lowercase().ifBlank { "default" }
        val raw = prefs.getString("$KEY_PINNED_QUICK_PREFIX$u", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePinnedQuickKeys(username: String, keys: List<String>) {
        val u = username.trim().lowercase().ifBlank { "default" }
        prefs.edit().putString("$KEY_PINNED_QUICK_PREFIX$u", json.encodeToString(keys.distinct())).apply()
    }

    fun getAutoLockMinutes(): Int = prefs.getInt(KEY_AUTO_LOCK_MINUTES, 0)

    fun saveAutoLockMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_MINUTES, minutes.coerceAtLeast(0)).apply()
    }

    fun getLastBackgroundMs(): Long = prefs.getLong(KEY_LAST_BACKGROUND_MS, 0L)

    fun saveLastBackgroundMs(ms: Long) {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_MS, ms).apply()
    }

    fun saveSession(session: UserSession?) {
        if (session == null) {
            prefs.edit().remove(KEY_SESSION).apply()
        } else {
            prefs.edit().putString(KEY_SESSION, json.encodeToString(session)).apply()
        }
    }

    fun loadSession(): UserSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            val s = json.decodeFromString<UserSession>(raw)
            if (s.exp > System.currentTimeMillis()) s else {
                prefs.edit().remove(KEY_SESSION).apply()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    // ===== 3-Day Offline Login Credentials (PBKDF2-HMAC-SHA256) + 5-Fail / 5-Min Lockout =====
    private fun loadAllOfflineCreds(): MutableMap<String, OfflineCredRecord> {
        val raw = prefs.getString(KEY_OFFLINE_CREDS, null) ?: return mutableMapOf()
        return try {
            json.decodeFromString<Map<String, OfflineCredRecord>>(raw).toMutableMap()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun writeOfflineCreds(map: Map<String, OfflineCredRecord>) {
        prefs.edit().putString(KEY_OFFLINE_CREDS, json.encodeToString(map)).apply()
    }

    private fun loadAllOfflineFails(): MutableMap<String, OfflineFailState> {
        val raw = prefs.getString(KEY_OFFLINE_FAILS, null) ?: return mutableMapOf()
        return try {
            json.decodeFromString<Map<String, OfflineFailState>>(raw).toMutableMap()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun writeOfflineFails(map: Map<String, OfflineFailState>) {
        prefs.edit().putString(KEY_OFFLINE_FAILS, json.encodeToString(map)).apply()
    }

    fun clearOfflineFailures(username: String) {
        val key = username.trim().lowercase()
        val fails = loadAllOfflineFails()
        if (fails.remove(key) != null) writeOfflineFails(fails)
    }

    fun saveOfflineCredential(username: String, password: String, session: UserSession) {
        if (session.roleKey.lowercase() == "super") return // Never allow offline login for super
        val key = username.trim().lowercase()
        if (key.isEmpty() || password.isEmpty()) return
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iterations = 12000
        val hash = pbkdf2(password, salt, iterations)
        val map = loadAllOfflineCreds()
        map[key] = OfflineCredRecord(
            username = session.username,
            role = session.roleKey,
            label = session.label,
            assignedClass = session.assignedClass,
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP),
            iterations = iterations,
            exp = System.currentTimeMillis() + OFFLINE_CRED_TTL_MS
        )
        writeOfflineCreds(map)
        clearOfflineFailures(key)
    }

    fun deleteOfflineCredential(username: String) {
        val key = username.trim().lowercase()
        val map = loadAllOfflineCreds()
        if (map.remove(key) != null) writeOfflineCreds(map)
        clearOfflineFailures(key)
    }

    fun getOfflineCredential(username: String): OfflineCredRecord? {
        val key = username.trim().lowercase()
        val map = loadAllOfflineCreds()
        val rec = map[key] ?: return null
        if (rec.exp <= System.currentTimeMillis()) {
            map.remove(key)
            writeOfflineCreds(map)
            return null
        }
        return rec
    }

    fun verifyOfflineLoginResult(username: String, password: String, nowMs: Long = System.currentTimeMillis()): OfflineLoginResult {
        val key = username.trim().lowercase()
        val rec = getOfflineCredential(key) ?: return OfflineLoginResult.NotFoundOrExpired
        if (rec.role.lowercase() == "super") return OfflineLoginResult.NotFoundOrExpired

        val fails = loadAllOfflineFails()
        val fState = fails[key]
        if (fState != null && fState.lockedUntil > nowMs) {
            val mins = ((fState.lockedUntil - nowMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
            return OfflineLoginResult.Locked(mins)
        }

        val salt = Base64.decode(rec.saltBase64, Base64.NO_WRAP)
        val expected = rec.hashBase64
        val actual = Base64.encodeToString(pbkdf2(password, salt, rec.iterations), Base64.NO_WRAP)
        if (!constantTimeEquals(expected, actual)) {
            val prevCount = if (fState != null && fState.lockedUntil <= nowMs && fState.count >= OFFLINE_MAX_FAILS) 0 else (fState?.count ?: 0)
            val nextCount = prevCount + 1
            if (nextCount >= OFFLINE_MAX_FAILS) {
                fails[key] = OfflineFailState(count = nextCount, lockedUntil = nowMs + OFFLINE_LOCK_MS)
                writeOfflineFails(fails)
                return OfflineLoginResult.Locked(5)
            } else {
                fails[key] = OfflineFailState(count = nextCount, lockedUntil = 0L)
                writeOfflineFails(fails)
                return OfflineLoginResult.WrongPassword(OFFLINE_MAX_FAILS - nextCount)
            }
        }

        if (fails.remove(key) != null) writeOfflineFails(fails)
        return OfflineLoginResult.Success(
            UserSession(
                username = rec.username,
                roleKey = rec.role,
                label = rec.label,
                assignedClass = rec.assignedClass,
                token = "",
                exp = rec.exp,
                offline = true
            )
        )
    }

    fun verifyOfflineLogin(username: String, password: String): UserSession? =
        (verifyOfflineLoginResult(username, password) as? OfflineLoginResult.Success)?.session

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    // ===== Client Error Log Queue (V19.62) =====
    fun loadClientErrors(): List<ClientErrorLogItem> {
        val raw = prefs.getString(KEY_CLIENT_ERRORS, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun appendClientError(item: ClientErrorLogItem) {
        val list = loadClientErrors().toMutableList()
        val idx = list.indexOfFirst { it.t == item.t && it.m == item.m && it.pg == item.pg }
        if (idx >= 0) {
            val prev = list[idx]
            list[idx] = prev.copy(c = (prev.c + item.c).coerceAtMost(9999))
        } else {
            list.add(item)
            while (list.size > 25) list.removeAt(0)
        }
        prefs.edit().putString(KEY_CLIENT_ERRORS, json.encodeToString(list)).apply()
    }

    fun clearClientErrors() {
        prefs.edit().remove(KEY_CLIENT_ERRORS).apply()
    }

    // ===== Student Bulk Cache & Recent Searches =====
    fun saveStudentCache(students: List<Student>) {
        prefs.edit()
            .putString(KEY_STUDENT_CACHE, json.encodeToString(students))
            .putLong(KEY_STUDENT_CACHE_TIME, System.currentTimeMillis())
            .apply()
    }

    fun loadStudentCache(): Pair<List<Student>, Long>? {
        val raw = prefs.getString(KEY_STUDENT_CACHE, null) ?: return null
        val loadedAt = prefs.getLong(KEY_STUDENT_CACHE_TIME, 0L)
        if (System.currentTimeMillis() - loadedAt > CACHE_TTL_MS) return null
        return try {
            json.decodeFromString<List<Student>>(raw) to loadedAt
        } catch (_: Exception) {
            null
        }
    }

    fun clearStudentCache() {
        prefs.edit().remove(KEY_STUDENT_CACHE).remove(KEY_STUDENT_CACHE_TIME).apply()
    }

    fun loadRecentSearches(): List<Student> {
        val raw = prefs.getString(KEY_RECENT_SEARCH, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentSearch(student: Student) {
        val list = loadRecentSearches().filterNot { it.regNo.equals(student.regNo, ignoreCase = true) }.toMutableList()
        list.add(0, student)
        prefs.edit().putString(KEY_RECENT_SEARCH, json.encodeToString(list.take(6))).apply()
    }

    // ===== Stale-While-Revalidate (SWR) Cache =====
    fun swrRead(username: String, key: String): String? {
        if (SWR_NEVER.containsMatchIn(key)) return null
        val fullKey = "$SWR_PREFIX$username:$key"
        val raw = prefs.getString(fullKey, null) ?: return null
        val ts = prefs.getLong("${fullKey}_ts", 0L)
        val maxAge = if (key.contains("tchRoster")) SWR_ROSTER_MAX_AGE_MS else SWR_MAX_AGE_MS
        if (System.currentTimeMillis() - ts > maxAge) {
            prefs.edit().remove(fullKey).remove("${fullKey}_ts").apply()
            return null
        }
        return raw
    }

    fun swrWrite(username: String, key: String, payload: String) {
        if (SWR_NEVER.containsMatchIn(key) || payload.length > 300_000) return
        val fullKey = "$SWR_PREFIX$username:$key"
        prefs.edit()
            .putString(fullKey, payload)
            .putLong("${fullKey}_ts", System.currentTimeMillis())
            .apply()
    }

    fun swrClearAll() {
        val ed = prefs.edit()
        prefs.all.keys.filter { it.startsWith(SWR_PREFIX) }.forEach { ed.remove(it) }
        ed.apply()
    }

    // ===== Offline Pending Saves Queue =====
    fun loadPendingSaves(): List<PendingSaveItem> {
        val raw = prefs.getString(KEY_PENDING_SAVES, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePendingSaves(items: List<PendingSaveItem>) {
        prefs.edit().putString(KEY_PENDING_SAVES, json.encodeToString(items)).apply()
    }

    fun exportPendingSavesJson(): String = json.encodeToString(loadPendingSaves())
}
