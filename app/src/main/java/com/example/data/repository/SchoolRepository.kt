package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.local.ClientErrorLogItem
import com.example.data.local.LocalStorage
import com.example.data.local.OfflineLoginResult
import com.example.data.remote.GasApiClient
import com.example.data.remote.GasResponse
import com.example.data.remote.boolVal
import com.example.data.remote.doubleVal
import com.example.data.remote.intVal
import com.example.data.remote.str
import com.example.domain.model.AcYearInfo
import com.example.domain.model.AcYearSample
import com.example.domain.model.ArchiveYearItem
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.BirthdayStudent
import com.example.domain.model.CertificateHistoryItem
import com.example.domain.model.ClassDivisionPair
import com.example.domain.model.ClassTeacherItem
import com.example.domain.model.DashboardStats
import com.example.domain.model.DiaryEntry
import com.example.domain.model.EarlyLeaveRecord
import com.example.domain.model.FeeRecord
import com.example.domain.model.NoticeItem
import com.example.domain.model.PendingSaveItem
import com.example.domain.model.ScholarshipRecord
import com.example.domain.model.Student
import com.example.domain.model.TransportInfo
import com.example.domain.model.UserAccount
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import com.example.worker.SgsWorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.util.UUID

data class StudentAuditChange(
    val time: String,
    val user: String,
    val role: String,
    val field: String,
    val oldVal: String,
    val newVal: String
)

data class StudentAuditLogRow(
    val time: String,
    val user: String,
    val role: String,
    val action: String
)

data class StudentAuditPromoRow(
    val time: String,
    val from: String,
    val to: String,
    val by: String,
    val undone: Boolean
)

data class StudentAuditBundle(
    val hasAuditSheet: Boolean = true,
    val changes: List<StudentAuditChange> = emptyList(),
    val logs: List<StudentAuditLogRow> = emptyList(),
    val promotions: List<StudentAuditPromoRow> = emptyList()
)

sealed class SmartSaveResult {
    data class Success(val response: GasResponse, val serial: String = "") : SmartSaveResult()
    data class Queued(val message: String, val queueSize: Int) : SmartSaveResult()
    data class Error(val message: String) : SmartSaveResult()
}

class SchoolRepository(
    private val context: Context,
    val storage: LocalStorage,
    val api: GasApiClient
) {
    companion object {
        val QUEUEABLE_ACTIONS = setOf(
            "saveAttendance", "saveFees", "saveDiary", "saveTransport",
            "saveStudentContact", "saveNotice", "saveEarlyLeave",
            "markEarlyLeaveOut", "saveScholarship"
        )
        const val APP_VERSION = "V19.65"

        private val LEGACY_USERS = mapOf(
            "user_1" to Triple("Pass@1234", "master", "Master User"),
            "user_2" to Triple("Pass@1234", "deo", "DEO User"),
            "user_3" to Triple("Pass@1234", "cert", "Certificate User"),
            "user_s" to Triple("Super@1234", "super", "Super Master User"),
            "teacher_1" to Triple("Pass@1234", "teacher", "वर्ग शिक्षक")
        )
    }

    var currentSession: UserSession? = storage.loadSession()
        private set

    var onAuthExpired: (() -> Unit)? = null

    fun isOnline(): Boolean = api.isOnline()

    fun getUrl(): String = storage.getSheetsUrl()
    fun setUrl(url: String) = storage.saveSheetsUrl(url)

    private fun attachAuth(params: MutableMap<String, String>) {
        val s = currentSession ?: return
        if (s.token.isNotBlank()) params["token"] = s.token
        params.putIfAbsent("requesterRole", s.roleKey)
        params.putIfAbsent("requesterUser", s.username)
        params.putIfAbsent("auditUser", s.username)
        params.putIfAbsent("auditRole", s.roleKey)
        if (s.role == UserRole.TEACHER) {
            params.putIfAbsent("teacherIyatta", s.iyatta)
            params.putIfAbsent("teacherTukdi", s.tukdi)
        }
    }

    suspend fun request(
        params: Map<String, String>,
        usePost: Boolean = false,
        longTimeout: Boolean = false
    ): GasResponse {
        val s = currentSession
        if (s != null && s.offline && params["action"] != "login" && params["action"] != "ping" && api.isOnline()) {
            return GasResponse(
                status = "error",
                code = "relogin",
                message = "🌐 नेट आले आहे — नवीन माहिती/Save साठी पुन्हा Login करा (ऑफलाइन Login मध्ये सर्व्हरशी संपर्क होत नाही).",
                raw = buildJsonObject { put("status", "error"); put("code", "relogin") }
            )
        }
        val mutable = params.toMutableMap()
        if (mutable["action"] != "login" && mutable["action"] != "ping") {
            attachAuth(mutable)
        }
        val resp = if (usePost) api.post(getUrl(), mutable, longTimeout) else api.get(getUrl(), mutable, longTimeout)
        if (resp.code == "auth") {
            onAuthExpired?.invoke()
        }
        return resp
    }

    suspend fun swrFetch(
        key: String,
        params: Map<String, String>,
        onStale: ((GasResponse) -> Unit)? = null
    ): Pair<GasResponse, Boolean> {
        val uname = currentSession?.username.orEmpty()
        if (uname.isNotBlank()) {
            val cached = storage.swrRead(uname, key)
            if (cached != null) {
                try {
                    val obj = storage.json.parseToJsonElement(cached).jsonObject
                    val staleResp = GasResponse("ok", "", "", obj)
                    onStale?.invoke(staleResp)
                    val fresh = request(params)
                    if (fresh.isOk) {
                        storage.swrWrite(uname, key, fresh.raw.toString())
                        return fresh to false
                    }
                    return staleResp to true
                } catch (_: Exception) {
                }
            }
        }
        val fresh = request(params)
        if (fresh.isOk && uname.isNotBlank()) {
            storage.swrWrite(uname, key, fresh.raw.toString())
        }
        return fresh to false
    }

    // ===== Authentication =====
    suspend fun preConnect(): GasResponse = request(mapOf("action" to "ping"))

    suspend fun login(username: String, password: String, rememberOffline: Boolean): Result<UserSession> {
        val u = username.trim()
        if (u.isEmpty() || password.isEmpty()) {
            return Result.failure(Exception("Username व Password आवश्यक आहे."))
        }
        if (!api.isOnline()) {
            return when (val off = storage.verifyOfflineLoginResult(u, password)) {
                is OfflineLoginResult.Success -> {
                    currentSession = off.session
                    storage.saveSession(off.session)
                    Result.success(off.session)
                }
                is OfflineLoginResult.Locked ->
                    Result.failure(Exception("🔒 ऑफलाइन Login ${off.minutesLeft} मिनिटांसाठी लॉक आहे (५ चुकीचे प्रयत्न)."))
                is OfflineLoginResult.WrongPassword ->
                    Result.failure(Exception("❌ Password चुकीचा आहे (ऑफलाइन प्रयत्न बाकी: ${off.attemptsLeft})."))
                OfflineLoginResult.NotFoundOrExpired ->
                    Result.failure(Exception("📴 इंटरनेट नाही आणि या डिव्हाइसवर ३ दिवसांचा ऑफलाइन Login जतन नाही."))
            }
        }

        var lastResp: GasResponse? = null
        val delays = listOf(0L, 1200L, 2500L)
        for (d in delays) {
            if (d > 0) delay(d)
            val r = api.get(getUrl(), mapOf("action" to "login", "username" to u, "password" to password))
            lastResp = r
            if (r.isOk && r.raw["user"] is JsonObject) {
                val uObj = r.raw["user"]!!.jsonObject
                val roleKey = uObj.str("role").trim().lowercase()
                if (UserRole.fromKey(roleKey) == null) {
                    return Result.failure(Exception("या User चा Role अमान्य आहे: $roleKey"))
                }
                val session = UserSession(
                    username = uObj.str("username").ifBlank { u },
                    roleKey = roleKey,
                    label = uObj.str("label").ifBlank { u },
                    assignedClass = uObj.str("assignedClass"),
                    token = r.raw.str("token"),
                    exp = r.raw.str("exp").toLongOrNull() ?: (System.currentTimeMillis() + 12 * 3600 * 1000L),
                    offline = false
                )
                currentSession = session
                storage.saveSession(session)
                storage.clearOfflineFailures(u)
                if (rememberOffline && session.role != UserRole.SUPER) {
                    storage.saveOfflineCredential(u, password, session)
                    storage.saveBiometricUsername(session.username)
                }
                return Result.success(session)
            }
            if (r.code == "badcred" || r.code == "locked" || r.code == "badrole") {
                if (r.code == "badcred") storage.deleteOfflineCredential(u)
                return Result.failure(Exception(r.message.ifBlank { "Username किंवा Password चुकीचा आहे." }))
            }
            if (!r.isNetworkError) break
        }

        // Offline fallback if server unreachable
        when (val off = storage.verifyOfflineLoginResult(u, password)) {
            is OfflineLoginResult.Success -> {
                currentSession = off.session
                storage.saveSession(off.session)
                return Result.success(off.session)
            }
            is OfflineLoginResult.Locked ->
                return Result.failure(Exception("🔒 ऑफलाइन Login ${off.minutesLeft} मिनिटांसाठी लॉक आहे."))
            is OfflineLoginResult.WrongPassword ->
                return Result.failure(Exception("❌ Password चुकीचा आहे (ऑफलाइन प्रयत्न बाकी: ${off.attemptsLeft})."))
            OfflineLoginResult.NotFoundOrExpired -> {}
        }

        // Legacy fallback when AUTH_ENFORCE is disabled on server
        val legacy = LEGACY_USERS[u]
        if (legacy != null && legacy.first == password) {
            val session = UserSession(
                username = u,
                roleKey = legacy.second,
                label = legacy.third,
                assignedClass = if (u == "teacher_1") "7th|अ" else "",
                token = "",
                exp = System.currentTimeMillis() + 12 * 3600 * 1000L,
                offline = false
            )
            currentSession = session
            storage.saveSession(session)
            return Result.success(session)
        }

        return Result.failure(Exception(lastResp?.message?.ifBlank { "सर्व्हरशी संपर्क होऊ शकला नाही." } ?: "Login अयशस्वी"))
    }

    fun loginWithBiometricCredential(): Result<UserSession> {
        val rec = storage.getValidBiometricCredential()
            ?: return Result.failure(Exception("⚠️ बायोमेट्रिक Login साठी जतन केलेली नोंद सापडली नाही."))
        val session = UserSession(
            username = rec.username,
            roleKey = rec.role,
            label = rec.label,
            assignedClass = rec.assignedClass,
            token = "",
            exp = rec.exp,
            offline = true
        )
        currentSession = session
        storage.saveSession(session)
        return Result.success(session)
    }

    fun logout(keepOffline: Boolean = false) {
        val uname = currentSession?.username
        if (!keepOffline) {
            storage.swrClearAll()
            if (!uname.isNullOrBlank()) storage.deleteOfflineCredential(uname)
        }
        currentSession = null
        storage.saveSession(null)
    }

    // ===== Smart Save + Offline Queue =====
    suspend fun smartSave(action: String, params: Map<String, String>, summary: String): SmartSaveResult {
        val s = currentSession
        val queueable = action in QUEUEABLE_ACTIONS
        val fullParams = params.toMutableMap()
        fullParams["action"] = action
        if (queueable && !fullParams.containsKey("reqId")) {
            fullParams["reqId"] = "r${System.currentTimeMillis().toString(36)}${UUID.randomUUID().toString().take(5)}"
        }

        if (queueable && (!api.isOnline() || s?.offline == true)) {
            return enqueuePending(action, fullParams, summary)
        }

        val totalQuerySize = fullParams.entries.sumOf { it.key.length + it.value.length + 2 }
        val usePost = totalQuerySize > 6500
        val resp = request(fullParams, usePost = usePost)

        if (queueable && resp.isNetworkError) {
            return enqueuePending(action, fullParams, summary)
        }

        return if (resp.isOk) {
            SmartSaveResult.Success(resp, resp.raw.str("serial"))
        } else {
            SmartSaveResult.Error(resp.message.ifBlank { "Save अयशस्वी झाले." })
        }
    }

    private fun enqueuePending(action: String, params: Map<String, String>, summary: String): SmartSaveResult {
        val list = storage.loadPendingSaves().toMutableList()
        val cleanParams = params.filterKeys { it != "token" }
        val item = PendingSaveItem(
            reqId = cleanParams["reqId"] ?: UUID.randomUUID().toString(),
            action = action,
            params = cleanParams,
            queuedAt = System.currentTimeMillis(),
            by = currentSession?.username.orEmpty(),
            summary = summary
        )
        list.add(item)
        storage.savePendingSaves(list)
        SgsWorkScheduler.enqueuePendingSync(context)
        val msg = if (currentSession?.offline == true && api.isOnline()) {
            "📴 ऑफलाइन Login — माहिती फोनमध्ये Pending ठेवली आहे; पुन्हा Login केल्यावर Save होईल. (Pending: ${list.size})"
        } else {
            "📶 नेट उपलब्ध नाही — माहिती फोनमध्ये Pending ठेवली आहे; नेट आल्यावर आपोआप Save होईल. (Pending: ${list.size})"
        }
        return SmartSaveResult.Queued(msg, list.size)
    }

    suspend fun flushPendingSaves(): Pair<Int, Int> {
        val s = currentSession ?: return 0 to 0
        if (s.offline || !api.isOnline()) return 0 to 0
        val list = storage.loadPendingSaves().toMutableList()
        if (list.isEmpty()) return 0 to 0

        var sentCount = 0
        var failCount = 0
        val remaining = mutableListOf<PendingSaveItem>()

        for (item in list) {
            if (item.by.isNotBlank() && item.by != s.username) {
                remaining.add(item)
                continue
            }
            val resp = request(item.params, usePost = item.params.toString().length > 6500)
            if (resp.isOk) {
                sentCount++
            } else if (resp.isNetworkError) {
                remaining.add(item)
                failCount++
            } else {
                remaining.add(item.copy(failed = true, lastError = resp.message))
                failCount++
            }
        }
        storage.savePendingSaves(remaining)
        return sentCount to failCount
    }

    // ===== Students Bulk Cache & Search =====
    suspend fun bulkLoadStudents(): Result<List<Student>> {
        val resp = request(mapOf("action" to "getAll"))
        if (!resp.isOk) return Result.failure(Exception(resp.message.ifBlank { "विद्यार्थी यादी Load झाली नाही." }))
        val arr = resp.raw["data"] as? JsonArray ?: JsonArray(emptyList())
        val list = arr.mapNotNull { el ->
            (el as? JsonObject)?.let { parseStudent(it) }
        }
        storage.saveStudentCache(list)
        return Result.success(list)
    }

    suspend fun searchStudentDirect(query: String): Result<Student?> {
        val cached = storage.loadStudentCache()?.first
        val qLower = query.trim().lowercase()
        val hit = cached?.firstOrNull {
            it.regNo.trim().lowercase() == qLower ||
                it.studentId.trim().lowercase() == qLower ||
                it.aadhar.trim().lowercase() == qLower
        }
        if (hit != null) return Result.success(hit)

        val resp = request(mapOf("action" to "search", "q" to query.trim()))
        if (resp.status == "found" && resp.raw["data"] is JsonObject) {
            val st = parseStudent(resp.raw["data"]!!.jsonObject, resp.raw.intVal("rowIndex"))
            return Result.success(st)
        }
        if (resp.status == "notfound") return Result.success(null)
        return Result.failure(Exception(resp.message.ifBlank { "शोध अयशस्वी." }))
    }

    // ===== Photo Compression (<10 KB JPEG) & Chunked Upload =====
    suspend fun compressAndUploadPhoto(uri: Uri, regNo: String): Result<String> = withContext(Dispatchers.IO) {
        if (regNo.isBlank()) return@withContext Result.failure(Exception("फोटो Upload साठी Reg No आवश्यक आहे."))
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext Result.failure(Exception("फोटो उघडता आला नाही."))
        val original = inputStream.use { BitmapFactory.decodeStream(it) }
            ?: return@withContext Result.failure(Exception("फोटो वाचता आला नाही."))

        var width = 200
        var height = ((original.height.toFloat() / original.width.toFloat()) * width).toInt().coerceAtLeast(1)
        if (height > 260) {
            height = 240
            width = ((original.width.toFloat() / original.height.toFloat()) * height).toInt().coerceAtLeast(1)
        }
        var scaled = Bitmap.createScaledBitmap(original, width, height, true)
        var quality = 75
        var jpegBytes: ByteArray
        do {
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            jpegBytes = baos.toByteArray()
            if (jpegBytes.size <= 9800) break
            quality -= 12
            if (quality < 25) {
                width = (width * 0.8f).toInt().coerceAtLeast(90)
                height = (height * 0.8f).toInt().coerceAtLeast(110)
                scaled = Bitmap.createScaledBitmap(original, width, height, true)
                quality = 65
            }
        } while (jpegBytes.size > 9800 && width > 90)

        if (jpegBytes.size > 10240) {
            return@withContext Result.failure(Exception("फोटो १० KB पेक्षा छोटा करता आला नाही."))
        }

        val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        // Chunk upload via GET (matches Code.gs startPhotoChunkUpload / savePhotoChunk / finishPhotoChunkUpload)
        val chunkSize = 1800
        val chunks = b64.chunked(chunkSize)
        val uploadId = "ph_${System.currentTimeMillis()}"

        val startR = request(
            mapOf(
                "action" to "photoChunkStart",
                "uploadId" to uploadId,
                "regNo" to regNo,
                "mimeType" to "image/jpeg",
                "count" to chunks.size.toString()
            )
        )
        if (!startR.isOk) return@withContext Result.failure(Exception(startR.message.ifBlank { "Photo start failed" }))

        for ((idx, ch) in chunks.withIndex()) {
            val cr = request(
                mapOf(
                    "action" to "photoChunk",
                    "uploadId" to uploadId,
                    "idx" to idx.toString(),
                    "chunk" to ch
                )
            )
            if (!cr.isOk) return@withContext Result.failure(Exception("Chunk ${idx + 1} failed: ${cr.message}"))
        }

        val finR = request(
            mapOf(
                "action" to "photoChunkFinish",
                "uploadId" to uploadId,
                "regNo" to regNo,
                "mimeType" to "image/jpeg",
                "count" to chunks.size.toString()
            )
        )
        if (!finR.isOk) return@withContext Result.failure(Exception(finR.message.ifBlank { "Photo finish failed" }))
        val photoUrl = finR.raw.str("photoUrl")
        Result.success(photoUrl)
    }

    // ===== JSON Parsers =====
    fun parseStudent(obj: JsonObject, fallbackRow: Int = 0): Student {
        return Student(
            rowIndex = obj.intVal("_rowIndex", fallbackRow),
            acYear = obj.str("acYear"),
            regNo = obj.str("regNo"),
            studentId = obj.str("studentId"),
            bookNo = obj.str("bookNo"),
            aadhar = obj.str("aadhar"),
            iyatta = obj.str("iyatta"),
            tukdi = obj.str("tukdi"),
            pen = obj.str("pen"),
            rollNo = obj.str("rollNo"),
            fullName = obj.str("fullName").ifBlank { obj.str("firstName") },
            motherName = obj.str("motherName"),
            gender = obj.str("gender"),
            religion = obj.str("religion"),
            caste = obj.str("caste"),
            subcaste = obj.str("subcaste"),
            dob = obj.str("dob"),
            dobWords = obj.str("dobWords"),
            nationality = obj.str("nationality").ifBlank { "भारतीय" },
            motherTongue = obj.str("motherTongue").ifBlank { "मराठी" },
            birthVillage = obj.str("birthVillage"),
            prevSchool = obj.str("prevSchool"),
            admissionDate = obj.str("admissionDate"),
            admissionClass = obj.str("admissionClass"),
            contact = obj.str("contact"),
            address = obj.str("address"),
            photoUrl = obj.str("photoUrl"),
            whatsappMobile = obj.str("whatsappMobile"),
            alternateMobile = obj.str("alternateMobile"),
            category = obj.str("category"),
            minority = obj.str("minority")
        )
    }

    fun parseClassStudentsResponse(resp: GasResponse): List<Student> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull { (it as? JsonObject)?.let { o -> parseStudent(o) } }
    }

    fun parseDashboardStats(resp: GasResponse): DashboardStats {
        val countsObj = resp.raw["classCounts"] as? JsonObject
        val classMap = countsObj?.entries?.associate { it.key to (it.value.toString().trim('"').toIntOrNull() ?: 0) }.orEmpty()
        return DashboardStats(
            totalStudents = resp.raw.intVal("totalStudents"),
            totalGirls = resp.raw.intVal("totalGirls"),
            totalBoys = resp.raw.intVal("totalBoys"),
            todayLC = resp.raw.intVal("todayLC"),
            todayBF = resp.raw.intVal("todayBF"),
            monthCerts = resp.raw.intVal("monthCerts"),
            classCounts = classMap
        )
    }

    fun parseClassList(resp: GasResponse): List<ClassDivisionPair> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            ClassDivisionPair(o.str("iyatta"), o.str("tukdi"))
        }
    }

    fun parseBirthdays(resp: GasResponse): List<BirthdayStudent> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            BirthdayStudent(
                regNo = o.str("regNo"),
                studentId = o.str("studentId"),
                rollNo = o.str("rollNo"),
                fullName = o.str("fullName"),
                iyatta = o.str("iyatta"),
                tukdi = o.str("tukdi"),
                dob = o.str("dob"),
                age = o.str("age").toIntOrNull(),
                whatsappMobile = o.str("whatsappMobile"),
                alternateMobile = o.str("alternateMobile"),
                contact = o.str("contact")
            )
        }
    }

    fun parseNotices(resp: GasResponse): List<NoticeItem> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            NoticeItem(
                timestamp = o.str("timestamp"),
                date = o.str("date"),
                title = o.str("title"),
                message = o.str("message"),
                postedBy = o.str("postedBy"),
                targetClass = o.str("targetClass")
            )
        }
    }

    fun parseAttendanceList(resp: GasResponse): List<AttendanceRecord> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            AttendanceRecord(
                date = o.str("date"),
                iyatta = o.str("iyatta"),
                tukdi = o.str("tukdi"),
                regNo = o.str("regNo"),
                studentId = o.str("studentId"),
                fullName = o.str("fullName"),
                reason = o.str("reason"),
                markedBy = o.str("markedBy")
            )
        }
    }

    fun parseDiaryList(resp: GasResponse): List<DiaryEntry> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            DiaryEntry(
                timestamp = o.str("timestamp"),
                date = o.str("date"),
                regNo = o.str("regNo"),
                studentId = o.str("studentId"),
                fullName = o.str("fullName"),
                iyatta = o.str("iyatta"),
                tukdi = o.str("tukdi"),
                type = o.str("type"),
                remark = o.str("remark"),
                enteredBy = o.str("enteredBy")
            )
        }
    }

    fun parseTransportList(resp: GasResponse): List<TransportInfo> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            TransportInfo(
                regNo = o.str("regNo"),
                studentId = o.str("studentId"),
                fullName = o.str("fullName"),
                iyatta = o.str("iyatta"),
                tukdi = o.str("tukdi"),
                nativeVillage = o.str("nativeVillage"),
                travelMode = o.str("travelMode"),
                transportContact = o.str("transportContact"),
                updatedBy = o.str("updatedBy"),
                sourceYear = resp.raw.str("sourceYear"),
                archived = resp.raw.boolVal("archived")
            )
        }
    }

    fun parseFeesList(resp: GasResponse): List<FeeRecord> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            FeeRecord(
                date = o.str("date"),
                regNo = o.str("regNo"),
                studentId = o.str("studentId"),
                fullName = o.str("fullName"),
                iyatta = o.str("iyatta"),
                tukdi = o.str("tukdi"),
                acYear = o.str("acYear"),
                totalFee = o.doubleVal("totalFee"),
                feePaid = o.doubleVal("feePaid"),
                openingDue = o.doubleVal("openingDue"),
                pendingFee = o.doubleVal("pendingFee"),
                updatedBy = o.str("updatedBy")
            )
        }
    }

    fun parseEarlyLeaveList(resp: GasResponse): List<EarlyLeaveRecord> {
        val arr = resp.raw["records"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            EarlyLeaveRecord(
                date = o.str("date"),
                iyatta = o.str("iyatta"),
                tukdi = o.str("tukdi"),
                regNo = o.str("regNo"),
                studentId = o.str("studentId"),
                fullName = o.str("fullName"),
                rollNo = o.str("rollNo"),
                type = o.str("type"),
                period = o.intVal("period", 1),
                markedBy = o.str("markedBy"),
                outStatus = o.str("outStatus"),
                outBy = o.str("outBy"),
                outAt = o.str("outAt"),
                whatsappMobile = o.str("whatsappMobile"),
                alternateMobile = o.str("alternateMobile"),
                contact = o.str("contact")
            )
        }
    }

    fun parseScholarshipList(resp: GasResponse): List<ScholarshipRecord> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            ScholarshipRecord(
                id = o.str("id"),
                timestamp = o.str("timestamp"),
                acYear = o.str("acYear"),
                regNo = o.str("regNo"),
                studentId = o.str("studentId"),
                fullName = o.str("fullName"),
                iyatta = o.str("iyatta"),
                tukdi = o.str("tukdi"),
                name = o.str("name"),
                scheme = o.str("scheme"),
                amount = o.doubleVal("amount"),
                status = o.str("status").ifBlank { "प्रलंबित" },
                remark = o.str("remark"),
                enteredBy = o.str("enteredBy")
            )
        }
    }

    fun parseClassTeachers(resp: GasResponse): List<ClassTeacherItem> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            ClassTeacherItem(
                classKey = o.str("classKey"),
                classMr = o.str("classMr"),
                div = o.str("div"),
                nameEn = o.str("nameEn"),
                nameMr = o.str("nameMr"),
                mobile = o.str("mobile"),
                shortName = o.str("short")
            )
        }
    }

    fun parseCertHistory(resp: GasResponse): List<CertificateHistoryItem> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            CertificateHistoryItem(
                type = o.str("type"),
                serial = o.str("serial"),
                date = o.str("date"),
                regNo = o.str("regNo"),
                fullName = o.str("firstName"),
                detail = o.str("detail")
            )
        }
    }

    fun parseUsers(resp: GasResponse): List<UserAccount> {
        val arr = resp.raw["data"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            UserAccount(
                rowIndex = o.intVal("rowIndex"),
                username = o.str("username"),
                password = o.str("password"),
                role = o.str("role"),
                label = o.str("label"),
                assignedClass = o.str("assignedClass"),
                mobile = o.str("mobile")
            )
        }
    }

    fun parseAcYearInfo(resp: GasResponse): AcYearInfo {
        val raw = resp.raw
        val countsObj = (raw["counts"] as? JsonObject) ?: (raw["breakdown"] as? JsonObject)
        val counts = countsObj?.entries?.associate { it.key to (it.value.toString().trim('"').toIntOrNull() ?: 0) }.orEmpty()
        val ac = raw.str("current").ifBlank { raw.str("currentAcYear") }.ifBlank { raw.str("acYear") }.ifBlank { storage.getAcYear() }
        storage.saveAcYear(ac)
        val sampleArr = raw["sample"] as? JsonArray
        val samples = sampleArr?.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            AcYearSample(o.str("regNo"), o.str("name"), o.str("year"))
        }.orEmpty()
        return AcYearInfo(
            currentAcYear = ac,
            dateYear = raw.str("dateYear"),
            fromProperty = raw.boolVal("fromProperty"),
            aheadOfDate = raw.boolVal("aheadOfDate"),
            behindDate = raw.boolVal("behindDate"),
            mismatchCount = raw.intVal("mismatch", raw.intVal("mismatchCount")),
            futureCount = raw.intVal("future"),
            promoPendingCount = raw.intVal("promoPending"),
            canSync = raw.boolVal("canSync"),
            breakdown = counts,
            studentsCount = raw.intVal("total", raw.intVal("studentsCount")),
            sample = samples
        )
    }

    fun parseStudentAudit(resp: GasResponse): StudentAuditBundle {
        val raw = resp.raw
        val hasSheet = raw["hasAuditSheet"]?.toString()?.contains("false") != true
        val changesArr = (raw["changes"] as? JsonArray) ?: (raw["data"] as? JsonArray) ?: JsonArray(emptyList())
        val changes = changesArr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            StudentAuditChange(
                time = o.str("time").ifBlank { o.str("ts") },
                user = o.str("user"),
                role = o.str("role"),
                field = o.str("field").ifBlank { o.str("action") },
                oldVal = o.str("oldVal").ifBlank { o.str("old") },
                newVal = o.str("newVal").ifBlank { o.str("new") }
            )
        }
        val logsArr = raw["logs"] as? JsonArray ?: JsonArray(emptyList())
        val logs = logsArr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            StudentAuditLogRow(o.str("time"), o.str("user"), o.str("role"), o.str("action"))
        }
        val promoArr = raw["promotions"] as? JsonArray ?: JsonArray(emptyList())
        val promos = promoArr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            StudentAuditPromoRow(
                time = o.str("time"),
                from = o.str("from"),
                to = o.str("to"),
                by = o.str("by"),
                undone = o.boolVal("undone")
            )
        }
        return StudentAuditBundle(hasAuditSheet = hasSheet, changes = changes, logs = logs, promotions = promos)
    }

    fun parseArchiveYears(resp: GasResponse): List<ArchiveYearItem> {
        val arr = resp.raw["years"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            when (el) {
                is JsonObject -> ArchiveYearItem(
                    year = el.str("year"),
                    current = el.boolVal("current"),
                    archived = el.boolVal("archived"),
                    hasFile = el.boolVal("hasFile")
                )
                else -> {
                    val y = el.toString().trim('"')
                    if (y.isNotBlank()) ArchiveYearItem(year = y) else null
                }
            }
        }
    }

    suspend fun fetchPhotoBitmap(regNo: String): Bitmap? = withContext(Dispatchers.IO) {
        if (regNo.isBlank()) return@withContext null
        val r = request(mapOf("action" to "getPhotoData", "regNo" to regNo))
        if (!r.isOk) return@withContext null
        val dataUrl = r.raw.str("dataUrl")
        val b64 = dataUrl.substringAfter("base64,", "")
        if (b64.isBlank()) return@withContext null
        try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun logClientError(page: String, type: String, message: String, stack: String = "") {
        storage.appendClientError(
            ClientErrorLogItem(
                v = APP_VERSION,
                pg = page.take(40),
                ua = "Android/${android.os.Build.VERSION.RELEASE}",
                t = type.take(20),
                m = message.take(300),
                s = stack.take(500),
                c = 1
            )
        )
    }

    suspend fun flushClientErrors() {
        val s = currentSession ?: return
        if (s.offline || s.token.isBlank() || !api.isOnline()) return
        val errs = storage.loadClientErrors()
        if (errs.isEmpty()) return
        val payload = storage.json.encodeToString(errs.take(10))
        val r = request(mapOf("action" to "logClientErrors", "errors" to payload), usePost = payload.length > 3000)
        if (r.isOk) {
            storage.clearClientErrors()
        }
    }
}
