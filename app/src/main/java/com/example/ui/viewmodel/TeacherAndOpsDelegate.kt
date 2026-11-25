package com.example.ui.viewmodel

import com.example.data.remote.boolVal
import com.example.data.remote.doubleVal
import com.example.data.remote.intVal
import com.example.data.remote.str
import com.example.data.repository.SchoolRepository
import com.example.data.repository.SmartSaveResult
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.ClassDivisionPair
import com.example.domain.model.ClassTeacherItem
import com.example.domain.model.EarlyLeaveRecord
import com.example.domain.model.NoticeItem
import com.example.domain.model.Student
import com.example.domain.model.TransportInfo
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class TeacherAndOpsDelegate(
    private val repo: SchoolRepository,
    private val scope: CoroutineScope,
    private val onToast: (BannerMessage) -> Unit,
    private val onRefreshQueue: () -> Unit
) {
    private val _dashState = MutableStateFlow(DashUiState(dailyDate = MarathiUtils.todayIso()))
    val dashState: StateFlow<DashUiState> = _dashState.asStateFlow()

    private val _teacherState = MutableStateFlow(
        TeacherUiState(
            attDate = MarathiUtils.todayIso(),
            anaMonth = MarathiUtils.currentMonthIso(),
            reportFrom = MarathiUtils.todayIso(),
            reportTo = MarathiUtils.todayIso()
        )
    )
    val teacherState: StateFlow<TeacherUiState> = _teacherState.asStateFlow()

    private val _earlyLeaveState = MutableStateFlow(EarlyLeaveUiState(date = MarathiUtils.todayIso()))
    val earlyLeaveState: StateFlow<EarlyLeaveUiState> = _earlyLeaveState.asStateFlow()

    private val _scholarshipState = MutableStateFlow(ScholarshipUiState())
    val scholarshipState: StateFlow<ScholarshipUiState> = _scholarshipState.asStateFlow()

    private val _maintenanceStatus = MutableStateFlow<BannerMessage?>(null)
    val maintenanceStatus: StateFlow<BannerMessage?> = _maintenanceStatus.asStateFlow()

    private val _classInfoStudents = MutableStateFlow<List<Student>>(emptyList())
    val classInfoStudents: StateFlow<List<Student>> = _classInfoStudents.asStateFlow()

    private val _classList = MutableStateFlow<List<ClassDivisionPair>>(emptyList())
    val classList: StateFlow<List<ClassDivisionPair>> = _classList.asStateFlow()

    private val _classTeachers = MutableStateFlow<List<ClassTeacherItem>>(emptyList())
    val classTeachers: StateFlow<List<ClassTeacherItem>> = _classTeachers.asStateFlow()

    // ===== Dashboard =====
    fun loadDashboard() {
        val role = repo.currentSession?.role ?: return
        if (role != UserRole.SUPER && role != UserRole.MASTER) return
        scope.launch {
            _dashState.update { it.copy(loading = true) }
            val (statsR, _) = repo.swrFetch("dashStats", mapOf("action" to "getDashboardStats")) { stale ->
                _dashState.update { it.copy(stats = repo.parseDashboardStats(stale)) }
            }
            if (statsR.isOk) {
                val stats = repo.parseDashboardStats(statsR)
                val yearArr = statsR.raw["yearCounts"] as? JsonObject
                val years = yearArr?.entries?.map { it.key to (it.value.toString().trim('"').toIntOrNull() ?: 0) }
                    ?.sortedByDescending { it.first } ?: emptyList()
                _dashState.update { it.copy(stats = stats, yearAdmissions = years) }
            }
            val anaR = repo.request(mapOf("action" to "getAnalyticsData"))
            if (anaR.isOk) {
                val genderObj = anaR.raw["genderCounts"] as? JsonObject
                val girls = genderObj?.entries?.filter {
                    val k = it.key.lowercase()
                    k == "female" || k.contains("मुलगी") || k.contains("girl") || k.contains("स्त्री")
                }?.sumOf { it.value.toString().trim('"').toIntOrNull() ?: 0 } ?: 0
                val boys = genderObj?.entries?.filter {
                    val k = it.key.lowercase()
                    k == "male" || k.contains("मुलगा") || k.contains("boy") || k.contains("पुरुष")
                }?.sumOf { it.value.toString().trim('"').toIntOrNull() ?: 0 } ?: 0
                val relObj = anaR.raw["religionCounts"] as? JsonObject
                val relList = relObj?.entries?.map { it.key to (it.value.toString().trim('"').toIntOrNull() ?: 0) }
                    ?.sortedByDescending { it.second } ?: emptyList()
                val lcObj = anaR.raw["lcReasonCounts"] as? JsonObject
                val lcList = lcObj?.entries?.map { it.key to (it.value.toString().trim('"').toIntOrNull() ?: 0) } ?: emptyList()
                val admObj = anaR.raw["admissionsByYear"] as? JsonObject
                val admList = admObj?.entries?.map { it.key to (it.value.toString().trim('"').toIntOrNull() ?: 0) }
                    ?.sortedByDescending { it.first } ?: emptyList()
                _dashState.update { st ->
                    st.copy(
                        stats = st.stats.copy(
                            totalGirls = if (girls > 0) girls else st.stats.totalGirls,
                            totalBoys = if (boys > 0) boys else st.stats.totalBoys
                        ),
                        yearAdmissions = if (admList.isNotEmpty()) admList else st.yearAdmissions,
                        religionCounts = relList,
                        lcReasonCounts = lcList
                    )
                }
            }
            val capR = repo.request(mapOf("action" to "getSheetCapacity"))
            if (capR.isOk) {
                _dashState.update {
                    it.copy(
                        sheetCapPct = capR.raw.doubleVal("pct"),
                        sheetCapLevel = capR.raw.str("level").ifBlank { "ok" }
                    )
                }
            }
            val acResp = repo.request(mapOf("action" to "getAcYearInfo"))
            if (acResp.isOk) {
                _dashState.update { it.copy(acYearInfo = repo.parseAcYearInfo(acResp)) }
            }
            val (pendR, _) = repo.swrFetch("mstAttPend:${MarathiUtils.todayIso()}", mapOf("action" to "getAttendancePending"))
            if (pendR.isOk) {
                fun parseAttClsArr(key: String): List<PendingAttClass> {
                    val arr = pendR.raw[key] as? JsonArray ?: return emptyList()
                    return arr.mapNotNull {
                        val o = it as? JsonObject ?: return@mapNotNull null
                        PendingAttClass(
                            iyatta = o.str("iyatta"),
                            tukdi = o.str("tukdi"),
                            teacher = o.str("label").ifBlank { o.str("teacher") },
                            mobile = o.str("mobile"),
                            username = o.str("username"),
                            time = o.str("time")
                        )
                    }
                }
                val unArr = pendR.raw["unassigned"] as? JsonArray ?: JsonArray(emptyList())
                val unassigned = unArr.mapNotNull {
                    val o = it as? JsonObject ?: return@mapNotNull null
                    ClassDivisionPair(o.str("iyatta"), o.str("tukdi"))
                }
                _dashState.update {
                    it.copy(
                        pendingAtt = parseAttClsArr("pending"),
                        doneAtt = parseAttClsArr("done"),
                        unassignedAtt = unassigned,
                        attPendingIsSunday = pendR.raw.boolVal("isSunday"),
                        attPendingTotalExpected = pendR.raw.intVal("totalExpected")
                    )
                }
            }
            val bdR = repo.request(mapOf("action" to "getTodayBirthdays"))
            if (bdR.isOk) _dashState.update { it.copy(birthdays = repo.parseBirthdays(bdR)) }

            val notR = repo.request(mapOf("action" to "getNotices"))
            if (notR.isOk) _dashState.update { it.copy(notices = repo.parseNotices(notR)) }

            _dashState.update { it.copy(loading = false) }
            loadDailySummary(_dashState.value.dailyDate)
        }
    }

    fun updateDailyDate(date: String) {
        _dashState.update { it.copy(dailyDate = date) }
    }

    fun loadDailySummary(date: String = _dashState.value.dailyDate) {
        val d = date.ifBlank { MarathiUtils.todayIso() }
        scope.launch {
            _dashState.update { it.copy(dailyLoading = true, dailyError = null) }
            val r = repo.request(mapOf("action" to "getDailySummary", "date" to d))
            if (!r.isOk) {
                _dashState.update { it.copy(dailyLoading = false, dailyError = r.message.ifBlank { "Load Failed" }) }
                return@launch
            }
            val attObj = r.raw["attendance"] as? JsonObject
            val totalsObj = attObj?.get("totals") as? JsonObject
            val classesArr = attObj?.get("classes") as? JsonArray ?: JsonArray(emptyList())
            val pendArr = attObj?.get("pending") as? JsonArray ?: JsonArray(emptyList())
            val noTeachArr = attObj?.get("noTeacher") as? JsonArray ?: JsonArray(emptyList())
            val elObj = r.raw["earlyLeave"] as? JsonObject
            val feeObj = r.raw["fees"] as? JsonObject
            val feeByClsArr = feeObj?.get("byClass") as? JsonArray ?: JsonArray(emptyList())
            val chObj = r.raw["changes"] as? JsonObject
            val notArr = r.raw["notices"] as? JsonArray ?: JsonArray(emptyList())

            val rows = classesArr.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                DailyAttClassRow(
                    o.str("iyatta"), o.str("tukdi"),
                    o.intVal("total"), o.intVal("present"), o.intVal("absent"),
                    o.str("pct").toDoubleOrNull()
                )
            }
            val pend = pendArr.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                PendingAttClass(
                    iyatta = o.str("iyatta"),
                    tukdi = o.str("tukdi"),
                    teacher = o.str("teacher").ifBlank { o.str("label") },
                    mobile = o.str("mobile")
                )
            }
            val noTeachers = noTeachArr.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                ClassDivisionPair(o.str("iyatta"), o.str("tukdi"))
            }
            val feesByCls = feeByClsArr.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                val lbl = "${MarathiUtils.classToMr(o.str("iyatta"))} ${o.str("tukdi")}".trim()
                lbl to o.doubleVal("total")
            }
            val noticeTitles = notArr.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                o.str("title").ifBlank { o.str("message").take(40) }
            }

            val present = totalsObj?.intVal("present") ?: 0
            val total = totalsObj?.intVal("total") ?: 0
            val absent = totalsObj?.intVal("absent") ?: 0
            val pct = totalsObj?.str("pct")?.toDoubleOrNull()
            val feesTotal = feeObj?.doubleVal("total") ?: 0.0
            val feesCount = feeObj?.intVal("count") ?: 0
            val elTotal = elObj?.intVal("total") ?: 0
            val elGone = elObj?.intVal("gone") ?: 0
            val elPerm = elObj?.intVal("permitted") ?: 0
            val elNotGone = elObj?.intVal("notGone") ?: 0
            val elOther = elObj?.intVal("other") ?: 0
            val changesTotal = chObj?.intVal("total") ?: 0
            val changesStudents = chObj?.intVal("students") ?: 0

            val msgText = buildString {
                appendLine("*श्री. गो. से. हायस्कूल, पाचोरा*")
                appendLine("📅 *रोजचा सारांश — ${MarathiUtils.fmtDate(d)}*")
                if (r.message.isNotBlank()) appendLine("ℹ️ ${r.message}")
                if (r.raw.str("dayType") == "normal") {
                    appendLine("✅ उपस्थिती: $present/$total" + (pct?.let { " ($it%)" } ?: "") + " | गैरहजर: $absent")
                    if (pend.isNotEmpty()) {
                        appendLine("⏳ हजेरी बाकी (${pend.size}): ${pend.joinToString(", ") { "${MarathiUtils.classToMr(it.iyatta)} ${it.tukdi}" }}")
                    }
                    if (noTeachers.isNotEmpty()) {
                        appendLine("ℹ️ शिक्षक नसलेले (${noTeachers.size}): ${noTeachers.joinToString(", ") { "${MarathiUtils.classToMr(it.iyatta)} ${it.tukdi}" }}")
                    }
                }
                if (elTotal > 0) {
                    appendLine("🚪 घरी गेले: $elGone / एकूण $elTotal (परवानगी $elPerm | इतर $elOther | न गेलेले $elNotGone)")
                }
                append("💰 फी जमा: ${MarathiUtils.fmtRupees(feesTotal)} ($feesCount नोंदी)")
                if (feesByCls.isNotEmpty()) {
                    append(" — " + feesByCls.joinToString(", ") { "${it.first}: ${MarathiUtils.fmtRupees(it.second)}" })
                }
                appendLine()
                appendLine("📢 नोटीस: ${notArr.size}  |  🧑‍🎓 विद्यार्थी-बदल: $changesTotal ($changesStudents विद्यार्थी)")
            }.trim()

            _dashState.update {
                it.copy(
                    dailyLoading = false,
                    dailySummary = DailySummaryData(
                        date = d,
                        dayType = r.raw.str("dayType"),
                        message = r.message,
                        present = present,
                        total = total,
                        absent = absent,
                        pct = pct,
                        pendingClasses = pend,
                        noTeacherClasses = noTeachers,
                        classRows = rows,
                        elTotal = elTotal,
                        elGone = elGone,
                        elPermitted = elPerm,
                        elNotGone = elNotGone,
                        elOther = elOther,
                        feesTotal = feesTotal,
                        feesCount = feesCount,
                        feesByClass = feesByCls,
                        noticesCount = notArr.size,
                        noticeTitles = noticeTitles,
                        changesTotal = changesTotal,
                        changesStudents = changesStudents,
                        messageText = msgText,
                        raw = r.raw
                    )
                )
            }
        }
    }

    fun updateNoticeDraft(title: String? = null, msg: String? = null, target: String? = null) {
        _dashState.update {
            it.copy(
                noticeTitle = title ?: it.noticeTitle,
                noticeMessage = msg ?: it.noticeMessage,
                noticeTarget = target ?: it.noticeTarget
            )
        }
    }

    fun sendNotice() {
        val d = _dashState.value
        if (d.noticeTitle.isBlank() || d.noticeMessage.isBlank()) {
            _dashState.update { it.copy(noticeStatus = BannerMessage.Err("⚠️ शीर्षक व संदेश आवश्यक आहे.")) }
            return
        }
        scope.launch {
            _dashState.update { it.copy(noticeStatus = BannerMessage.Info("⏳ पाठवत आहे...")) }
            val res = repo.smartSave(
                "saveNotice",
                mapOf(
                    "title" to d.noticeTitle.trim(),
                    "message" to d.noticeMessage.trim(),
                    "targetClass" to d.noticeTarget,
                    "postedBy" to (repo.currentSession?.username ?: "master")
                ),
                "Notice: ${d.noticeTitle.trim()}"
            )
            when (res) {
                is SmartSaveResult.Success -> {
                    _dashState.update {
                        it.copy(
                            noticeTitle = "",
                            noticeMessage = "",
                            noticeStatus = BannerMessage.Ok("✅ नोटीस पाठवली!")
                        )
                    }
                    val notR = repo.request(mapOf("action" to "getNotices"))
                    if (notR.isOk) _dashState.update { it.copy(notices = repo.parseNotices(notR)) }
                }
                is SmartSaveResult.Queued -> {
                    onRefreshQueue()
                    _dashState.update { it.copy(noticeStatus = BannerMessage.Queued(res.message)) }
                }
                is SmartSaveResult.Error -> _dashState.update { it.copy(noticeStatus = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun syncStudentsAcYear(newYear: String) {
        scope.launch {
            _dashState.update { it.copy(syncStatus = BannerMessage.Info("⏳ शैक्षणिक वर्ष जुळवत आहे...")) }
            val r = repo.request(mapOf("action" to "syncStudentsAcYear", "newYear" to newYear, "confirm" to "SYNC"))
            if (r.isOk) {
                repo.storage.clearStudentCache()
                _dashState.update { it.copy(syncStatus = BannerMessage.Ok("✅ ${r.message.ifBlank { "सर्व विद्यार्थ्यांचे शैक्षणिक वर्ष $newYear केले!" }}")) }
                loadDashboard()
            } else {
                _dashState.update { it.copy(syncStatus = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    // ===== Teacher Workspace =====
    fun loadTeacherWorkspace(forceRoster: Boolean = false) {
        val s = repo.currentSession ?: return
        if (s.role != UserRole.TEACHER) return
        scope.launch {
            _teacherState.update { it.copy(loading = true) }
            val (rosR, _) = repo.swrFetch(
                "tchRoster:${s.iyatta}|${s.tukdi}",
                mapOf("action" to "getClassStudents", "iyatta" to s.iyatta, "tukdi" to s.tukdi, "nocache" to if (forceRoster) "1" else "")
            ) { stale ->
                _teacherState.update { it.copy(roster = repo.parseClassStudentsResponse(stale)) }
            }
            if (rosR.isOk) {
                _teacherState.update { it.copy(roster = repo.parseClassStudentsResponse(rosR)) }
            }

            val dashR = repo.request(mapOf("action" to "getTeacherDashboard", "iyatta" to s.iyatta, "tukdi" to s.tukdi))
            if (dashR.isOk) {
                _teacherState.update {
                    it.copy(
                        attMarkedToday = dashR.raw.boolVal("attMarkedToday"),
                        todayAbsentCount = dashR.raw.intVal("todayAbsentCount"),
                        classFeesTotal = dashR.raw.doubleVal("classFeesTotal")
                    )
                }
            }
            val notR = repo.request(mapOf("action" to "getNotices", "iyatta" to s.iyatta, "tukdi" to s.tukdi))
            if (notR.isOk) _teacherState.update { it.copy(notices = repo.parseNotices(notR)) }

            loadTeacherAttendance(_teacherState.value.attDate)
            loadTeacherFees()
            _teacherState.update { it.copy(loading = false) }
        }
    }

    fun updateTeacherRosterSearch(q: String) {
        _teacherState.update { it.copy(rosterSearch = q) }
    }

    fun loadTeacherAttendance(date: String) {
        val s = repo.currentSession ?: return
        _teacherState.update { it.copy(attDate = date) }
        scope.launch {
            val r = repo.request(mapOf("action" to "getAttendance", "iyatta" to s.iyatta, "tukdi" to s.tukdi, "date" to date))
            if (r.isOk) {
                val list = repo.parseAttendanceList(r)
                val map = list.associate { it.regNo to it.reason }
                _teacherState.update { it.copy(attSavedList = list, absentReasons = map) }
            }
        }
    }

    fun toggleTeacherAbsent(regNo: String, isAbsent: Boolean) {
        _teacherState.update { st ->
            val next = st.absentReasons.toMutableMap()
            if (isAbsent) next.putIfAbsent(regNo, "") else next.remove(regNo)
            st.copy(absentReasons = next)
        }
    }

    fun updateTeacherAbsentReason(regNo: String, reason: String) {
        _teacherState.update { st ->
            val next = st.absentReasons.toMutableMap()
            next[regNo] = reason
            st.copy(absentReasons = next)
        }
    }

    fun saveTeacherAttendance() {
        val s = repo.currentSession ?: return
        val st = _teacherState.value
        if (st.attDate.isBlank()) {
            _teacherState.update { it.copy(attStatus = BannerMessage.Err("⚠️ तारीख निवडा.")) }
            return
        }
        val arr = buildJsonArray {
            st.absentReasons.forEach { (reg, reason) ->
                val stu = st.roster.firstOrNull { it.regNo == reg }
                add(buildJsonObject {
                    put("regNo", reg)
                    put("studentId", stu?.studentId.orEmpty())
                    put("fullName", stu?.fullName.orEmpty())
                    put("reason", reason)
                })
            }
        }
        scope.launch {
            _teacherState.update { it.copy(attStatus = BannerMessage.Info("⏳ Save होत आहे...")) }
            val res = repo.smartSave(
                "saveAttendance",
                mapOf(
                    "iyatta" to s.iyatta,
                    "tukdi" to s.tukdi,
                    "date" to st.attDate,
                    "absentJson" to arr.toString(),
                    "markedBy" to s.username
                ),
                "हजेरी ${MarathiUtils.classToMr(s.iyatta)} ${s.tukdi} (${st.absentReasons.size} गैरहजर)"
            )
            when (res) {
                is SmartSaveResult.Success -> {
                    _teacherState.update { it.copy(attStatus = BannerMessage.Ok("✅ हजेरी Save झाली (${st.absentReasons.size} अनुपस्थित).")) }
                    loadTeacherAttendance(st.attDate)
                }
                is SmartSaveResult.Queued -> {
                    onRefreshQueue()
                    _teacherState.update { it.copy(attStatus = BannerMessage.Queued(res.message)) }
                }
                is SmartSaveResult.Error -> _teacherState.update { it.copy(attStatus = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun loadTeacherAttRangeReport(from: String, to: String) {
        val s = repo.currentSession ?: return
        _teacherState.update { it.copy(reportFrom = from, reportTo = to) }
        scope.launch {
            val r = repo.request(mapOf("action" to "getAttendance", "iyatta" to s.iyatta, "tukdi" to s.tukdi, "dateFrom" to from, "dateTo" to to))
            if (r.isOk) {
                _teacherState.update { it.copy(reportRecords = repo.parseAttendanceList(r)) }
            }
        }
    }

    fun setDiaryQueryOptions(full: Boolean, archive: Boolean) {
        _teacherState.update { it.copy(diaryFull = full, diaryArchive = archive) }
        val reg = _teacherState.value.selectedDiaryRegNo
        if (reg.isNotBlank()) selectTeacherDiaryStudent(reg)
    }

    fun selectTeacherDiaryStudent(regNo: String) {
        _teacherState.update { it.copy(selectedDiaryRegNo = regNo, diaryStatus = null, transportStatus = null) }
        if (regNo.isBlank()) return
        val full = _teacherState.value.diaryFull
        val arch = _teacherState.value.diaryArchive
        scope.launch {
            val trR = repo.request(mapOf("action" to "getTransport", "regNo" to regNo))
            val tr = if (trR.isOk) repo.parseTransportList(trR).firstOrNull() ?: TransportInfo(regNo = regNo) else TransportInfo(regNo = regNo)
            val dyParams = mutableMapOf("action" to "getDiary", "regNo" to regNo)
            if (full) dyParams["full"] = "1"
            if (arch) dyParams["archive"] = "1"
            val dyR = repo.request(dyParams)
            val dy = if (dyR.isOk) repo.parseDiaryList(dyR) else emptyList()
            _teacherState.update { it.copy(transportInfo = tr, diaryEntries = dy) }
        }
    }

    fun updateTransportDraft(village: String? = null, mode: String? = null, contact: String? = null) {
        _teacherState.update {
            it.copy(
                transportInfo = it.transportInfo.copy(
                    nativeVillage = village ?: it.transportInfo.nativeVillage,
                    travelMode = mode ?: it.transportInfo.travelMode,
                    transportContact = contact ?: it.transportInfo.transportContact
                )
            )
        }
    }

    fun saveTeacherTransport() {
        val s = repo.currentSession ?: return
        val st = _teacherState.value
        val stu = st.roster.firstOrNull { it.regNo == st.selectedDiaryRegNo } ?: return
        val tr = st.transportInfo
        scope.launch {
            _teacherState.update { it.copy(transportStatus = BannerMessage.Info("⏳ Save होत आहे...")) }
            val res = repo.smartSave(
                "saveTransport",
                mapOf(
                    "regNo" to stu.regNo,
                    "studentId" to stu.studentId,
                    "fullName" to stu.fullName,
                    "iyatta" to s.iyatta,
                    "tukdi" to s.tukdi,
                    "nativeVillage" to tr.nativeVillage.trim(),
                    "travelMode" to tr.travelMode,
                    "transportContact" to tr.transportContact.trim(),
                    "updatedBy" to s.username
                ),
                "प्रवास: ${stu.fullName}"
            )
            when (res) {
                is SmartSaveResult.Success -> _teacherState.update { it.copy(transportStatus = BannerMessage.Ok("✅ प्रवास माहिती Save झाली.")) }
                is SmartSaveResult.Queued -> {
                    onRefreshQueue()
                    _teacherState.update { it.copy(transportStatus = BannerMessage.Queued(res.message)) }
                }
                is SmartSaveResult.Error -> _teacherState.update { it.copy(transportStatus = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun updateDiaryDraft(type: String? = null, remark: String? = null) {
        _teacherState.update { it.copy(diaryType = type ?: it.diaryType, diaryRemark = remark ?: it.diaryRemark) }
    }

    fun saveTeacherDiary() {
        val s = repo.currentSession ?: return
        val st = _teacherState.value
        val stu = st.roster.firstOrNull { it.regNo == st.selectedDiaryRegNo } ?: return
        if (st.diaryRemark.isBlank()) {
            _teacherState.update { it.copy(diaryStatus = BannerMessage.Err("⚠️ शेरा / तपशील लिहा.")) }
            return
        }
        scope.launch {
            _teacherState.update { it.copy(diaryStatus = BannerMessage.Info("⏳ Save होत आहे...")) }
            val res = repo.smartSave(
                "saveDiary",
                mapOf(
                    "regNo" to stu.regNo,
                    "studentId" to stu.studentId,
                    "fullName" to stu.fullName,
                    "iyatta" to s.iyatta,
                    "tukdi" to s.tukdi,
                    "type" to st.diaryType,
                    "remark" to st.diaryRemark.trim(),
                    "enteredBy" to s.username
                ),
                "डायरी: ${stu.fullName}"
            )
            when (res) {
                is SmartSaveResult.Success -> {
                    _teacherState.update { it.copy(diaryRemark = "", diaryStatus = BannerMessage.Ok("✅ डायरी नोंद जोडली!")) }
                    selectTeacherDiaryStudent(stu.regNo)
                }
                is SmartSaveResult.Queued -> {
                    onRefreshQueue()
                    _teacherState.update { it.copy(diaryStatus = BannerMessage.Queued(res.message)) }
                }
                is SmartSaveResult.Error -> _teacherState.update { it.copy(diaryStatus = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun loadTeacherFees() {
        val s = repo.currentSession ?: return
        scope.launch {
            val r = repo.request(mapOf("action" to "getFees", "iyatta" to s.iyatta, "tukdi" to s.tukdi))
            if (r.isOk) {
                val list = repo.parseFeesList(r)
                val totalFee = r.raw.doubleVal("totalFeePerStudent", 1000.0)
                _teacherState.update { it.copy(feesList = list, totalFeePerStudent = totalFee) }
            }
        }
    }

    fun selectTeacherFeeStudent(regNo: String) {
        _teacherState.update { it.copy(selectedFeeRegNo = regNo, feeStatus = null) }
        if (regNo.isBlank()) return
        val stu = _teacherState.value.roster.firstOrNull { it.regNo == regNo }
        scope.launch {
            val r = repo.request(mapOf("action" to "getFees", "regNo" to regNo, "acYear" to (stu?.acYear.orEmpty())))
            if (r.isOk) _teacherState.update { it.copy(studentFeeHistory = repo.parseFeesList(r)) }
        }
    }

    fun updateFeeAmountInput(amt: String) {
        _teacherState.update { it.copy(feeAmountInput = amt) }
    }

    fun saveTeacherFee() {
        val s = repo.currentSession ?: return
        val st = _teacherState.value
        val stu = st.roster.firstOrNull { it.regNo == st.selectedFeeRegNo } ?: return
        val amt = st.feeAmountInput.trim().toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            _teacherState.update { it.copy(feeStatus = BannerMessage.Err("⚠️ जमा रक्कम बरोबर टाका.")) }
            return
        }
        scope.launch {
            _teacherState.update { it.copy(feeStatus = BannerMessage.Info("⏳ Save होत आहे...")) }
            val res = repo.smartSave(
                "saveFees",
                mapOf(
                    "regNo" to stu.regNo,
                    "studentId" to stu.studentId,
                    "fullName" to stu.fullName,
                    "iyatta" to s.iyatta,
                    "tukdi" to s.tukdi,
                    "acYear" to stu.acYear,
                    "amountPaid" to st.feeAmountInput.trim(),
                    "enteredBy" to s.username
                ),
                "फी: ${stu.fullName} ₹${st.feeAmountInput.trim()}"
            )
            when (res) {
                is SmartSaveResult.Success -> {
                    _teacherState.update { it.copy(feeAmountInput = "", feeStatus = BannerMessage.Ok("✅ फी जमा नोंद Save झाली.")) }
                    loadTeacherFees()
                    selectTeacherFeeStudent(stu.regNo)
                }
                is SmartSaveResult.Queued -> {
                    onRefreshQueue()
                    _teacherState.update { it.copy(feeStatus = BannerMessage.Queued(res.message)) }
                }
                is SmartSaveResult.Error -> _teacherState.update { it.copy(feeStatus = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun loadTeacherAnalytics(month: String = _teacherState.value.anaMonth, minRun: Int = _teacherState.value.anaMinRun) {
        val s = repo.currentSession ?: return
        scope.launch {
            _teacherState.update { it.copy(anaLoading = true, anaMonth = month, anaMinRun = minRun) }
            val r = repo.request(
                mapOf(
                    "action" to "getAttendanceAnalytics",
                    "iyatta" to s.iyatta,
                    "tukdi" to s.tukdi,
                    "month" to month,
                    "minRun" to minRun.toString()
                )
            )
            if (r.isOk) {
                val stArr = r.raw["students"] as? JsonArray ?: JsonArray(emptyList())
                val strArr = r.raw["streaks"] as? JsonArray ?: JsonArray(emptyList())
                val students = stArr.mapNotNull { (it as? JsonObject)?.let(::parseStreakRow) }
                val streaks = strArr.mapNotNull { (it as? JsonObject)?.let(::parseStreakRow) }
                _teacherState.update {
                    it.copy(
                        anaLoading = false,
                        anaWorkingDays = r.raw.intVal("workingDays"),
                        anaAvgPct = r.raw.str("avgPct").toDoubleOrNull(),
                        anaStudents = students,
                        anaStreaks = streaks
                    )
                }
            } else {
                _teacherState.update { it.copy(anaLoading = false) }
                onToast(BannerMessage.Err(r.message))
            }
        }
    }

    private fun parseStreakRow(o: JsonObject): TeacherAttStreakStudent = TeacherAttStreakStudent(
        rollNo = o.str("rollNo"),
        fullName = o.str("fullName"),
        tukdi = o.str("tukdi"),
        workingDays = o.intVal("workingDays"),
        absent = o.intVal("absent"),
        pct = o.str("pct").toDoubleOrNull(),
        maxRun = o.intVal("maxRun"),
        mobile = o.str("mobile"),
        from = o.str("from"),
        to = o.str("to")
    )

    fun updateTeacherMaintQuery(q: String) {
        _teacherState.update { it.copy(maintSearchRegNo = q) }
    }

    fun searchTeacherMaintStudent() {
        val reg = _teacherState.value.maintSearchRegNo.trim()
        val match = _teacherState.value.roster.firstOrNull { it.regNo.equals(reg, true) || it.studentId.equals(reg, true) }
        _teacherState.update {
            it.copy(
                maintStudent = match,
                maintNewDiv = match?.tukdi?.ifBlank { "अ" } ?: "अ",
                maintStatus = if (match == null) BannerMessage.Err("तुमच्या वर्गात विद्यार्थी सापडला नाही.") else null
            )
        }
    }

    fun updateTeacherMaintDiv(div: String) {
        _teacherState.update { it.copy(maintNewDiv = div) }
    }

    fun saveTeacherDivisionChange() {
        val s = repo.currentSession ?: return
        val st = _teacherState.value
        val stu = st.maintStudent ?: return
        scope.launch {
            _teacherState.update { it.copy(maintStatus = BannerMessage.Info("⏳ बदलत आहे...")) }
            val r = repo.request(
                mapOf(
                    "action" to "updateClassDivision",
                    "regNo" to stu.regNo,
                    "iyatta" to s.iyatta,
                    "tukdi" to st.maintNewDiv
                )
            )
            if (r.isOk) {
                _teacherState.update { it.copy(maintStatus = BannerMessage.Ok("✅ तुकडी बदलली.")) }
                loadTeacherWorkspace(forceRoster = true)
            } else {
                _teacherState.update { it.copy(maintStatus = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    // ===== Early Leave (Staff & Peon) =====
    fun initEarlyLeave() {
        val s = repo.currentSession ?: return
        if (s.role == UserRole.PEON) {
            loadPeonEarlyLeave()
            return
        }
        scope.launch {
            if (_earlyLeaveState.value.classes.isEmpty()) {
                val cr = repo.request(mapOf("action" to "getClassList"))
                if (cr.isOk) {
                    val list = repo.parseClassList(cr)
                    _classList.value = list
                    _earlyLeaveState.update { it.copy(classes = list) }
                }
            }
            if (s.role == UserRole.TEACHER) {
                _earlyLeaveState.update { it.copy(selectedClassKey = "${s.iyatta}|${s.tukdi}") }
            }
            loadEarlyLeaveData()
        }
    }

    fun loadPeonEarlyLeave() {
        scope.launch {
            _earlyLeaveState.update { it.copy(loading = true) }
            val r = repo.request(mapOf("action" to "getEarlyLeavePeon"))
            if (r.isOk) {
                _earlyLeaveState.update { it.copy(loading = false, peonRecords = repo.parseEarlyLeaveList(r)) }
            } else {
                _earlyLeaveState.update { it.copy(loading = false, status = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    fun markPeonOut(rec: EarlyLeaveRecord, outStatus: String) {
        scope.launch {
            val res = repo.smartSave(
                "markEarlyLeaveOut",
                mapOf(
                    "date" to rec.date,
                    "regNo" to rec.regNo,
                    "status" to outStatus
                ),
                "Gate Out: ${rec.fullName} ($outStatus)"
            )
            when (res) {
                is SmartSaveResult.Success -> loadPeonEarlyLeave()
                is SmartSaveResult.Queued -> {
                    onRefreshQueue()
                    onToast(BannerMessage.Queued(res.message))
                }
                is SmartSaveResult.Error -> onToast(BannerMessage.Err(res.message))
            }
        }
    }

    fun setEarlyLeaveFilters(date: String? = null, classKey: String? = null, filter: String? = null) {
        _earlyLeaveState.update {
            it.copy(
                date = date ?: it.date,
                selectedClassKey = classKey ?: it.selectedClassKey,
                filter = filter ?: it.filter
            )
        }
        if (date != null || classKey != null) loadEarlyLeaveData()
    }

    fun loadEarlyLeaveData() {
        val st = _earlyLeaveState.value
        val date = st.date.ifBlank { MarathiUtils.todayIso() }
        val parts = st.selectedClassKey.split("|")
        val all = parts.getOrNull(0) == "ALL" || parts.getOrNull(0).isNullOrBlank()
        val iy = if (all) "" else parts[0]
        val tk = if (all) "" else parts.getOrNull(1).orEmpty()

        scope.launch {
            _earlyLeaveState.update { it.copy(loading = true, draftChanges = emptyMap()) }
            val q = mutableMapOf("action" to "getEarlyLeave", "date" to date)
            if (!all) {
                q["iyatta"] = iy
                q["tukdi"] = tk
            }
            val recR = repo.request(q)
            val rosR = if (!all) repo.request(mapOf("action" to "getClassStudents", "iyatta" to iy, "tukdi" to tk)) else null
            if (recR.isOk) {
                val recs = repo.parseEarlyLeaveList(recR)
                val absArr = recR.raw["absentRegNos"] as? JsonArray ?: JsonArray(emptyList())
                val absSet = absArr.map { it.toString().trim('"').lowercase() }.toSet()
                val ros = if (rosR?.isOk == true) repo.parseClassStudentsResponse(rosR) else emptyList()
                _earlyLeaveState.update {
                    it.copy(
                        loading = false,
                        records = recs,
                        absentRegNos = absSet,
                        roster = ros,
                        archived = recR.raw.boolVal("archived"),
                        status = null
                    )
                }
            } else {
                _earlyLeaveState.update { it.copy(loading = false, status = BannerMessage.Err("❌ ${recR.message}")) }
            }
        }
    }

    fun setEarlyLeaveDraft(regNo: String, type: String, period: Int) {
        _earlyLeaveState.update { st ->
            val next = st.draftChanges.toMutableMap()
            next[regNo] = type to period
            st.copy(draftChanges = next)
        }
    }

    fun saveEarlyLeave() {
        val st = _earlyLeaveState.value
        val parts = st.selectedClassKey.split("|")
        val iy = parts.getOrNull(0).orEmpty()
        val tk = parts.getOrNull(1).orEmpty()
        if (iy.isBlank() || iy == "ALL") return

        val entries = mutableListOf<JsonObject>()
        val removes = mutableListOf<String>()
        val savedMap = st.records.associateBy { it.regNo }

        for ((reg, pair) in st.draftChanges) {
            if (pair.first.isBlank()) {
                if (savedMap.containsKey(reg)) removes.add(reg)
            } else {
                entries.add(buildJsonObject {
                    put("regNo", reg)
                    put("type", pair.first)
                    put("period", pair.second.coerceIn(1, 9))
                })
            }
        }
        if (entries.isEmpty() && removes.isEmpty()) {
            _earlyLeaveState.update { it.copy(status = BannerMessage.Info("ℹ️ Save करण्यासाठी बदल नाही.")) }
            return
        }
        scope.launch {
            _earlyLeaveState.update { it.copy(status = BannerMessage.Info("⏳ Save होत आहे...")) }
            val entriesJson = buildJsonArray { entries.forEach { add(it) } }.toString()
            val removeJson = buildJsonArray { removes.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }.toString()
            val res = repo.smartSave(
                "saveEarlyLeave",
                mapOf(
                    "date" to st.date,
                    "iyatta" to iy,
                    "tukdi" to tk,
                    "entriesJson" to entriesJson,
                    "removeJson" to removeJson
                ),
                "Early Leave ${MarathiUtils.classToMr(iy)} $tk"
            )
            when (res) {
                is SmartSaveResult.Success -> {
                    _earlyLeaveState.update { it.copy(status = BannerMessage.Ok("✅ घरी जाणाऱ्यांची नोंद Save झाली.")) }
                    loadEarlyLeaveData()
                }
                is SmartSaveResult.Queued -> {
                    onRefreshQueue()
                    _earlyLeaveState.update { it.copy(status = BannerMessage.Queued(res.message)) }
                }
                is SmartSaveResult.Error -> _earlyLeaveState.update { it.copy(status = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    // ===== Scholarship =====
    fun initScholarship() {
        val s = repo.currentSession ?: return
        scope.launch {
            if (_scholarshipState.value.classes.isEmpty()) {
                val cr = repo.request(mapOf("action" to "getClassList"))
                if (cr.isOk) {
                    val list = repo.parseClassList(cr)
                    _classList.value = list
                    _scholarshipState.update { it.copy(classes = list) }
                }
            }
            if (s.role == UserRole.TEACHER) {
                setScholarshipClass("${s.iyatta}|${s.tukdi}")
            } else {
                loadScholarships()
            }
        }
    }

    fun setScholarshipClass(classKey: String) {
        _scholarshipState.update { it.copy(selectedClassKey = classKey) }
        val parts = classKey.split("|")
        val all = parts.getOrNull(0) == "ALL" || parts.getOrNull(0).isNullOrBlank()
        scope.launch {
            if (!all) {
                val rr = repo.request(mapOf("action" to "getClassStudents", "iyatta" to parts[0], "tukdi" to parts.getOrNull(1).orEmpty()))
                if (rr.isOk) _scholarshipState.update { it.copy(roster = repo.parseClassStudentsResponse(rr)) }
            } else {
                _scholarshipState.update { it.copy(roster = emptyList()) }
            }
            loadScholarships()
        }
    }

    fun setScholarshipFilter(status: String) {
        _scholarshipState.update { it.copy(filterStatus = status) }
    }

    fun updateScholarshipForm(
        editId: String? = null,
        regNo: String? = null,
        name: String? = null,
        scheme: String? = null,
        amount: String? = null,
        status: String? = null,
        remark: String? = null
    ) {
        _scholarshipState.update {
            it.copy(
                editId = editId ?: it.editId,
                formRegNo = regNo ?: it.formRegNo,
                formName = name ?: it.formName,
                formScheme = scheme ?: it.formScheme,
                formAmount = amount ?: it.formAmount,
                formStatus = status ?: it.formStatus,
                formRemark = remark ?: it.formRemark
            )
        }
    }

    fun resetScholarshipForm() {
        _scholarshipState.update {
            it.copy(editId = "", formRegNo = "", formName = "", formScheme = "", formAmount = "", formStatus = "प्रलंबित", formRemark = "")
        }
    }

    fun loadScholarships() {
        val parts = _scholarshipState.value.selectedClassKey.split("|")
        val all = parts.getOrNull(0) == "ALL" || parts.getOrNull(0).isNullOrBlank()
        scope.launch {
            _scholarshipState.update { it.copy(loading = true) }
            val q = mutableMapOf("action" to "getScholarship")
            if (!all) {
                q["iyatta"] = parts[0]
                q["tukdi"] = parts.getOrNull(1).orEmpty()
            }
            val r = repo.request(q)
            if (r.isOk) {
                _scholarshipState.update { it.copy(loading = false, records = repo.parseScholarshipList(r)) }
            } else {
                _scholarshipState.update { it.copy(loading = false, statusBanner = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    fun saveScholarship() {
        val st = _scholarshipState.value
        if (st.formRegNo.isBlank() || st.formName.isBlank()) {
            _scholarshipState.update { it.copy(statusBanner = BannerMessage.Err("⚠️ विद्यार्थी व शिष्यवृत्तीचे नाव आवश्यक आहे.")) }
            return
        }
        scope.launch {
            _scholarshipState.update { it.copy(statusBanner = BannerMessage.Info("⏳ Save होत आहे...")) }
            val res = repo.smartSave(
                "saveScholarship",
                mapOf(
                    "id" to st.editId,
                    "regNo" to st.formRegNo,
                    "scholarshipName" to st.formName.trim(),
                    "scheme" to st.formScheme.trim(),
                    "amount" to st.formAmount.trim(),
                    "status" to st.formStatus,
                    "remark" to st.formRemark.trim()
                ),
                "शिष्यवृत्ती: ${st.formName}"
            )
            when (res) {
                is SmartSaveResult.Success -> {
                    resetScholarshipForm()
                    _scholarshipState.update { it.copy(statusBanner = BannerMessage.Ok("✅ शिष्यवृत्ती नोंद Save झाली.")) }
                    loadScholarships()
                }
                is SmartSaveResult.Queued -> {
                    onRefreshQueue()
                    _scholarshipState.update { it.copy(statusBanner = BannerMessage.Queued(res.message)) }
                }
                is SmartSaveResult.Error -> _scholarshipState.update { it.copy(statusBanner = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun deleteScholarship(id: String) {
        scope.launch {
            val r = repo.request(mapOf("action" to "deleteScholarship", "id" to id))
            if (r.isOk) {
                onToast(BannerMessage.Ok("✅ नोंद मिटवली."))
                loadScholarships()
            } else {
                onToast(BannerMessage.Err("❌ ${r.message}"))
            }
        }
    }

    // ===== Class Info, Teachers & Maintenance =====
    fun loadClassListAndTeachers() {
        scope.launch {
            val cr = repo.request(mapOf("action" to "getClassList"))
            if (cr.isOk) _classList.value = repo.parseClassList(cr)
            val tr = repo.request(mapOf("action" to "getClassTeachers"))
            if (tr.isOk) _classTeachers.value = repo.parseClassTeachers(tr)
        }
    }

    fun loadClassInfoRoster(iyatta: String, tukdi: String) {
        scope.launch {
            val r = repo.request(mapOf("action" to "getClassStudents", "iyatta" to iyatta, "tukdi" to tukdi))
            if (r.isOk) _classInfoStudents.value = repo.parseClassStudentsResponse(r)
        }
    }

    fun updateClassDivision(regNo: String, newClass: String, newDiv: String) {
        scope.launch {
            _maintenanceStatus.value = BannerMessage.Info("⏳ अद्ययावत होत आहे...")
            val r = repo.request(
                mapOf("action" to "updateClassDivision", "regNo" to regNo, "iyatta" to newClass, "tukdi" to newDiv)
            )
            _maintenanceStatus.value = if (r.isOk) {
                repo.storage.clearStudentCache()
                BannerMessage.Ok("✅ वर्ग/तुकडी बदलली!")
            } else BannerMessage.Err("❌ ${r.message}")
        }
    }

    fun saveTotalFeeSetting(amount: String) {
        scope.launch {
            _maintenanceStatus.value = BannerMessage.Info("⏳ फी जतन होत आहे...")
            val r = repo.request(mapOf("action" to "saveTotalFee", "totalFee" to amount.trim()))
            _maintenanceStatus.value = if (r.isOk) BannerMessage.Ok("✅ एकूण फी ₹$amount जतन झाली!") else BannerMessage.Err("❌ ${r.message}")
        }
    }

    fun deleteStudentPermanent(regNo: String) {
        scope.launch {
            _maintenanceStatus.value = BannerMessage.Info("⏳ डिलीट करत आहे...")
            val r = repo.request(mapOf("action" to "deleteStudent", "regNo" to regNo.trim()))
            _maintenanceStatus.value = if (r.isOk) {
                repo.storage.clearStudentCache()
                BannerMessage.Ok("✅ विद्यार्थी डिलीट झाला (Backup सुरक्षित).")
            } else BannerMessage.Err("❌ ${r.message}")
        }
    }
}
