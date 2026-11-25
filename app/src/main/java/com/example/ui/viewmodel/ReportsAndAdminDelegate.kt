package com.example.ui.viewmodel

import com.example.data.remote.boolVal
import com.example.data.remote.doubleVal
import com.example.data.remote.intVal
import com.example.data.remote.str
import com.example.data.repository.SchoolRepository
import com.example.domain.model.ArchiveYearItem
import com.example.domain.model.ClassDivisionPair
import com.example.domain.model.ReportClassGroup
import com.example.domain.model.Student
import com.example.domain.model.UserAccount
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import com.example.domain.util.ReportUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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

class ReportsAndAdminDelegate(
    private val repo: SchoolRepository,
    private val scope: CoroutineScope,
    private val onToast: (BannerMessage) -> Unit
) {
    private val _reportState = MutableStateFlow(ReportUiState())
    val reportState: StateFlow<ReportUiState> = _reportState.asStateFlow()

    private val _statsState = MutableStateFlow(StatsUiState())
    val statsState: StateFlow<StatsUiState> = _statsState.asStateFlow()

    private val _attAnaMonth = MutableStateFlow(MarathiUtils.currentMonthIso())
    val attAnaMonth: StateFlow<String> = _attAnaMonth.asStateFlow()
    private val _attAnaOverview = MutableStateFlow<List<List<String>>>(emptyList())
    val attAnaOverview: StateFlow<List<List<String>>> = _attAnaOverview.asStateFlow()
    private val _attAnaStudents = MutableStateFlow<List<TeacherAttStreakStudent>>(emptyList())
    val attAnaStudents: StateFlow<List<TeacherAttStreakStudent>> = _attAnaStudents.asStateFlow()
    private val _attAnaStreaks = MutableStateFlow<List<TeacherAttStreakStudent>>(emptyList())
    val attAnaStreaks: StateFlow<List<TeacherAttStreakStudent>> = _attAnaStreaks.asStateFlow()
    private val _attAnaSummary = MutableStateFlow("")
    val attAnaSummary: StateFlow<String> = _attAnaSummary.asStateFlow()

    private val _adminState = MutableStateFlow(AdminAndQcUiState())
    val adminState: StateFlow<AdminAndQcUiState> = _adminState.asStateFlow()

    // ===== Report Builder =====
    fun initReport() {
        val s = repo.currentSession ?: return
        scope.launch {
            val cr = repo.request(mapOf("action" to "getClassList"))
            if (cr.isOk) {
                val list = repo.parseClassList(cr)
                if (s.role == UserRole.TEACHER) {
                    _reportState.update {
                        it.copy(
                            classes = list,
                            selectedClasses = if (s.iyatta.isNotBlank()) setOf(s.iyatta) else it.selectedClasses,
                            selectedDivisions = if (s.tukdi.isNotBlank()) setOf(s.tukdi) else it.selectedDivisions
                        )
                    }
                } else {
                    _reportState.update { it.copy(classes = list) }
                }
            }
        }
    }

    fun toggleReportClass(cls: String) {
        _reportState.update { st ->
            val next = st.selectedClasses.toMutableSet()
            if (!next.add(cls)) next.remove(cls)
            st.copy(selectedClasses = next)
        }
    }

    fun toggleReportDivision(div: String) {
        _reportState.update { st ->
            val next = st.selectedDivisions.toMutableSet()
            if (!next.add(div)) next.remove(div)
            st.copy(selectedDivisions = next)
        }
    }

    fun setAllReportClasses(all: Boolean) {
        _reportState.update { it.copy(selectedClasses = if (all) MarathiUtils.CLASSES.toSet() else emptySet()) }
    }

    fun setAllReportDivisions(all: Boolean) {
        _reportState.update { it.copy(selectedDivisions = if (all) MarathiUtils.DIVISIONS.toSet() else emptySet()) }
    }

    fun toggleReportField(key: String) {
        _reportState.update { st ->
            st.copy(fields = st.fields.map { if (it.key == key) it.copy(selected = !it.selected) else it })
        }
    }

    fun moveReportField(key: String, delta: Int) {
        _reportState.update { st ->
            val sorted = st.fields.sortedBy { it.order }.toMutableList()
            val idx = sorted.indexOfFirst { it.key == key }
            val targetIdx = idx + delta
            if (idx >= 0 && targetIdx in sorted.indices) {
                val tmp = sorted[idx]
                sorted[idx] = sorted[targetIdx]
                sorted[targetIdx] = tmp
                val reordered = sorted.mapIndexed { i, opt -> opt.copy(order = i + 1) }
                st.copy(fields = reordered)
            } else st
        }
    }

    fun updateReportConfig(
        sortField: String? = null,
        purpose: String? = null,
        feePaidFilter: String? = null,
        feeDueFilter: String? = null,
        landscapePdf: Boolean? = null
    ) {
        _reportState.update {
            it.copy(
                sortField = sortField ?: it.sortField,
                purpose = purpose ?: it.purpose,
                feePaidFilter = feePaidFilter ?: it.feePaidFilter,
                feeDueFilter = feeDueFilter ?: it.feeDueFilter,
                landscapePdf = landscapePdf ?: it.landscapePdf
            )
        }
    }

    fun runReport() {
        val st = _reportState.value
        val selectedCols = st.fields.filter { it.selected }.sortedBy { it.order }
        if (selectedCols.isEmpty()) {
            _reportState.update { it.copy(error = "⚠️ किमान एक कॉलम निवडा.") }
            return
        }
        scope.launch {
            _reportState.update { it.copy(loading = true, error = null) }
            var students = repo.storage.loadStudentCache()?.first
            if (students.isNullOrEmpty()) {
                val r = repo.bulkLoadStudents()
                students = r.getOrNull() ?: emptyList()
            }
            if (st.selectedClasses.isNotEmpty()) {
                students = students.filter { it.iyatta.trim() in st.selectedClasses }
            }
            if (st.selectedDivisions.isNotEmpty()) {
                students = students.filter { it.tukdi.trim() in st.selectedDivisions }
            }

            val needFees = selectedCols.any { it.key == "feePaid" || it.key == "feeDue" } ||
                st.feePaidFilter.isNotBlank() || st.feeDueFilter.isNotBlank()
            val feeMap = mutableMapOf<String, Pair<Double, Double>>() // regNo -> (paid, due)
            if (needFees) {
                val sess = repo.currentSession
                val q = mutableMapOf("action" to "getFees", "nocache" to "1")
                if (sess?.role == UserRole.TEACHER) {
                    q["iyatta"] = sess.iyatta
                    q["tukdi"] = sess.tukdi
                }
                val fr = repo.request(q)
                if (fr.isOk) {
                    val totalFee = fr.raw.doubleVal("totalFeePerStudent", 1000.0)
                    val arr = fr.raw["data"] as? JsonArray ?: JsonArray(emptyList())
                    arr.forEach { el ->
                        val o = el as? JsonObject ?: return@forEach
                        feeMap[o.str("regNo")] = o.doubleVal("feePaid") to o.doubleVal("pendingFee")
                    }
                    students.forEach { stu ->
                        feeMap.putIfAbsent(stu.regNo, 0.0 to totalFee)
                    }
                }
            }

            val paidCond = st.feePaidFilter.trim()
            val dueCond = st.feeDueFilter.trim()
            if (paidCond.isNotBlank() || dueCond.isNotBlank()) {
                students = students.filter { stu ->
                    val (paid, due) = feeMap[stu.regNo] ?: (0.0 to 0.0)
                    val okPaid = when (paidCond) {
                        "zero" -> paid <= 0.0
                        "nonzero" -> paid > 0.0
                        else -> true
                    }
                    val okDue = when (dueCond) {
                        "zero" -> due <= 0.0
                        "nonzero" -> due > 0.0
                        else -> true
                    }
                    okPaid && okDue
                }
            }

            fun mapStudentRow(idx: Int, stu: Student): List<String> {
                val (paid, due) = feeMap[stu.regNo] ?: (0.0 to 0.0)
                return listOf((idx + 1).toString()) + selectedCols.map { col ->
                    when (col.key) {
                        "regNo" -> stu.regNo
                        "rollNo" -> stu.rollNo
                        "fullName" -> stu.fullName
                        "iyatta" -> MarathiUtils.classToMr(stu.iyatta)
                        "tukdi" -> stu.tukdi
                        "gender" -> stu.gender
                        "dob" -> MarathiUtils.fmtDate(stu.dob)
                        "category" -> stu.category
                        "caste" -> stu.caste
                        "aadhar" -> stu.aadhar
                        "studentId" -> stu.studentId
                        "pen" -> stu.pen
                        "whatsappMobile" -> stu.whatsappMobile.ifBlank { stu.contact }
                        "alternateMobile" -> stu.alternateMobile
                        "address" -> stu.address
                        "motherName" -> stu.motherName
                        "feePaid" -> MarathiUtils.fmtRupees(paid)
                        "feeDue" -> MarathiUtils.fmtRupees(due)
                        else -> ""
                    }
                }
            }

            val grouped = ReportUtils.groupReportStudents(students, st.sortField)
            val groupedRows = grouped.map { (pair, grpStudents) ->
                ReportClassGroup(
                    iyatta = pair.iyatta,
                    tukdi = pair.tukdi,
                    rows = grpStudents.mapIndexed { idx, stu -> mapStudentRow(idx, stu) }
                )
            }
            val flatStudents = grouped.flatMap { it.second }
            val headers = listOf("अ.क्र.") + selectedCols.map { it.label }
            val rows = flatStudents.mapIndexed { idx, stu -> mapStudentRow(idx, stu) }
            _reportState.update { it.copy(loading = false, headers = headers, rows = rows, groupedRows = groupedRows) }
        }
    }

    // ===== Stats Report =====
    fun loadStatsReport() {
        scope.launch {
            _statsState.update { it.copy(loading = true, error = null) }
            val r = repo.request(mapOf("action" to "getStatsReport"))
            if (!r.isOk) {
                _statsState.update { it.copy(loading = false, error = r.message) }
                return@launch
            }
            val arr = r.raw["classes"] as? JsonArray ?: JsonArray(emptyList())
            val list = arr.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val fObj = o["female"] as? JsonObject
                val mObj = o["male"] as? JsonObject
                val aObj = o["ageCounts"] as? JsonObject
                StatsClassRow(
                    iyatta = o.str("iyatta"),
                    tukdi = o.str("tukdi"),
                    total = o.intVal("total"),
                    absent = o.intVal("absent"),
                    fees = o.doubleVal("fees"),
                    femaleCat = fObj?.entries?.associate { it.key to (it.value.toString().trim('"').toIntOrNull() ?: 0) }.orEmpty(),
                    maleCat = mObj?.entries?.associate { it.key to (it.value.toString().trim('"').toIntOrNull() ?: 0) }.orEmpty(),
                    ageCounts = aObj?.entries?.mapNotNull {
                        val k = it.key.toIntOrNull() ?: return@mapNotNull null
                        val v = it.value.toString().trim('"').toIntOrNull() ?: 0
                        k to v
                    }?.toMap().orEmpty()
                )
            }
            _statsState.update { it.copy(loading = false, classes = list, statusText = "✅ अद्ययावत माहिती") }
        }
    }

    // ===== School-wide Attendance Analytics =====
    fun loadAttendanceAnalytics(iyatta: String, tukdi: String, month: String, minRun: Int) {
        _attAnaMonth.value = month
        scope.launch {
            val (ovR, _) = repo.swrFetch("attaOv:$month", mapOf("action" to "getAttendanceOverview", "month" to month))
            if (ovR.isOk) {
                val arr = (ovR.raw["rows"] as? JsonArray) ?: (ovR.raw["classes"] as? JsonArray) ?: JsonArray(emptyList())
                _attAnaOverview.value = arr.mapNotNull { el ->
                    val o = el as? JsonObject ?: return@mapNotNull null
                    val avg = o.str("avgPct").ifBlank { "—" }
                    listOf(
                        "${MarathiUtils.classToMr(o.str("iyatta"))} ${o.str("tukdi")}".trim(),
                        o.str("students").ifBlank { "0" },
                        o.str("days").ifBlank { o.str("workingDays") },
                        if (avg == "—") "—" else "$avg%",
                        o.str("absentDays"),
                        MarathiUtils.fmtDate(o.str("lastDate")),
                        o.str("todayAbsent")
                    )
                }
            }
            if (iyatta.isNotBlank()) {
                val r = repo.request(
                    mapOf("action" to "getAttendanceAnalytics", "iyatta" to iyatta, "tukdi" to tukdi, "month" to month, "minRun" to minRun.toString())
                )
                if (r.isOk) {
                    val wd = r.raw.intVal("workingDays")
                    val avg = r.raw.str("avgPct")
                    val stArr = r.raw["students"] as? JsonArray ?: JsonArray(emptyList())
                    val strArr = r.raw["streaks"] as? JsonArray ?: JsonArray(emptyList())
                    _attAnaStudents.value = stArr.mapNotNull { (it as? JsonObject)?.let(::parseStreak) }
                    _attAnaStreaks.value = strArr.mapNotNull { (it as? JsonObject)?.let(::parseStreak) }
                    _attAnaSummary.value = "कार्यदिवस: $wd | सरासरी उपस्थिती: ${avg.ifBlank { "—" }}% | सलग $minRun+ दिवस गैरहजर: ${_attAnaStreaks.value.size}"
                }
            }
        }
    }

    private fun parseStreak(o: JsonObject) = TeacherAttStreakStudent(
        rollNo = o.str("rollNo"), fullName = o.str("fullName"), tukdi = o.str("tukdi"),
        workingDays = o.intVal("workingDays"), absent = o.intVal("absent"),
        pct = o.str("pct").toDoubleOrNull(), maxRun = o.intVal("maxRun"),
        mobile = o.str("mobile"), from = o.str("from"), to = o.str("to")
    )

    // ===== Users (Super Only) =====
    fun loadUsers() {
        scope.launch {
            _adminState.update { it.copy(usersLoading = true) }
            val r = repo.request(mapOf("action" to "getUsers"))
            _adminState.update {
                it.copy(
                    usersLoading = false,
                    users = if (r.isOk) repo.parseUsers(r) else it.users,
                    userStatus = if (!r.isOk) BannerMessage.Err(r.message) else null
                )
            }
        }
    }

    fun saveUser(account: UserAccount) {
        scope.launch {
            _adminState.update { it.copy(userStatus = BannerMessage.Info("⏳ जतन होत आहे...")) }
            val r = repo.request(
                mapOf(
                    "action" to "saveUser",
                    "rowIndex" to account.rowIndex.toString(),
                    "username" to account.username.trim(),
                    "password" to account.password,
                    "role" to account.role,
                    "label" to account.label.trim(),
                    "assignedClass" to account.assignedClass.trim(),
                    "mobile" to account.mobile.trim()
                )
            )
            if (r.isOk) {
                _adminState.update { it.copy(userStatus = BannerMessage.Ok("✅ User Save झाला!")) }
                loadUsers()
            } else {
                _adminState.update { it.copy(userStatus = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    fun deleteUser(username: String) {
        scope.launch {
            val r = repo.request(mapOf("action" to "deleteUser", "username" to username.trim()))
            if (r.isOk) {
                _adminState.update { it.copy(userStatus = BannerMessage.Ok("✅ User डिलीट झाला.")) }
                loadUsers()
            } else {
                _adminState.update { it.copy(userStatus = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    fun changeMyPassword(oldPass: String, newPass: String) {
        scope.launch {
            val r = repo.request(mapOf("action" to "changePassword", "oldPassword" to oldPass, "newPassword" to newPass))
            _adminState.update {
                it.copy(
                    userStatus = if (r.isOk) BannerMessage.Ok("✅ Password बदलला!") else BannerMessage.Err("❌ ${r.message}")
                )
            }
        }
    }

    // ===== Archive (Read-only) =====
    fun loadArchiveYears() {
        scope.launch {
            _adminState.update { it.copy(archiveLoading = true) }
            val r = repo.request(mapOf("action" to "getArchiveYears"))
            val items = if (r.isOk) repo.parseArchiveYears(r) else emptyList()
            val yrs = items.map { it.year }
            val sel = yrs.firstOrNull().orEmpty()
            val cmpA = yrs.getOrNull(1) ?: sel
            val cmpB = yrs.firstOrNull().orEmpty()
            _adminState.update {
                it.copy(
                    archiveLoading = false,
                    archiveYearItems = items,
                    archiveYears = yrs,
                    selectedArchiveYear = sel,
                    archiveCompareYearA = cmpA,
                    archiveCompareYearB = cmpB
                )
            }
            if (sel.isNotBlank()) loadArchiveReport(sel, _adminState.value.archiveSection)
        }
    }

    fun loadArchiveReport(year: String, section: String) {
        val sec = if (section == "tr") "transport" else section
        scope.launch {
            _adminState.update { it.copy(archiveLoading = true, selectedArchiveYear = year, archiveSection = sec, archiveSummary = "") }
            when (sec) {
                "fees" -> {
                    val r = repo.request(mapOf("action" to "getArchiveReport", "year" to year, "section" to "fees"))
                    if (!r.isOk) {
                        _adminState.update { it.copy(archiveLoading = false, archiveSummary = "❌ ${r.message}", archiveHeaders = emptyList(), archiveRows = emptyList()) }
                        return@launch
                    }
                    val tot = r.raw["total"] as? JsonObject
                    val summary = "विद्यार्थी: ${tot?.intVal("students") ?: 0} | जमा करणारे: ${tot?.intVal("payers") ?: 0} | एकूण जमा: ${MarathiUtils.fmtRupees(tot?.doubleVal("paid") ?: 0.0)} | बाकी: ${MarathiUtils.fmtRupees(tot?.doubleVal("balance") ?: 0.0)}"
                    val rows = (r.raw["rows"] as? JsonArray)?.mapNotNull { el ->
                        val a = el as? JsonArray ?: return@mapNotNull null
                        listOf(
                            a.getOrNull(0)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(1)?.toString()?.trim('"').orEmpty(),
                            "${MarathiUtils.classToMr(a.getOrNull(2)?.toString()?.trim('"'))} ${a.getOrNull(3)?.toString()?.trim('"').orEmpty()}".trim(),
                            a.getOrNull(4)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(5)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(6)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(7)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(8)?.toString()?.trim('"').orEmpty()
                        )
                    }.orEmpty()
                    _adminState.update {
                        it.copy(
                            archiveLoading = false,
                            archiveSummary = summary,
                            archiveHeaders = listOf("Reg No", "नाव", "इयत्ता", "एकूण फी", "मागील बाकी", "जमा", "बाकी", "हप्ते"),
                            archiveRows = rows
                        )
                    }
                }
                "transport" -> {
                    val r = repo.request(mapOf("action" to "getArchiveReport", "year" to year, "section" to "transport"))
                    if (!r.isOk) {
                        _adminState.update { it.copy(archiveLoading = false, archiveSummary = "❌ ${r.message}", archiveHeaders = emptyList(), archiveRows = emptyList()) }
                        return@launch
                    }
                    val rows = (r.raw["rows"] as? JsonArray)?.mapNotNull { el ->
                        val a = el as? JsonArray ?: return@mapNotNull null
                        listOf(
                            a.getOrNull(0)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(1)?.toString()?.trim('"').orEmpty(),
                            "${MarathiUtils.classToMr(a.getOrNull(2)?.toString()?.trim('"'))} ${a.getOrNull(3)?.toString()?.trim('"').orEmpty()}".trim(),
                            a.getOrNull(4)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(5)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(6)?.toString()?.trim('"').orEmpty()
                        )
                    }.orEmpty()
                    _adminState.update {
                        it.copy(
                            archiveLoading = false,
                            archiveSummary = "एकूण प्रवास नोंदी: ${rows.size}",
                            archiveHeaders = listOf("Reg No", "नाव", "इयत्ता", "गाव", "साधन", "संपर्क"),
                            archiveRows = rows
                        )
                    }
                }
                "sch" -> {
                    val r = repo.request(mapOf("action" to "getScholarship", "year" to year))
                    val list = if (r.isOk) repo.parseScholarshipList(r) else emptyList()
                    val rows = list.map { listOf(it.regNo, it.fullName, "${MarathiUtils.classToMr(it.iyatta)} ${it.tukdi}".trim(), it.name, MarathiUtils.fmtRupees(it.amount), it.status) }
                    _adminState.update {
                        it.copy(
                            archiveLoading = false,
                            archiveSummary = if (r.isOk) "एकूण शिष्यवृत्ती नोंदी: ${rows.size}" else "❌ ${r.message}",
                            archiveHeaders = listOf("Reg No", "नाव", "इयत्ता", "शिष्यवृत्ती", "रक्कम", "स्थिती"),
                            archiveRows = rows
                        )
                    }
                }
                "att" -> {
                    val ys = year.substringBefore("-").toIntOrNull() ?: 2026
                    val outRows = mutableListOf<List<String>>()
                    for (i in 0 until 12) {
                        var m = 6 + i
                        var yy = ys
                        if (m > 12) { m -= 12; yy = ys + 1 }
                        val ym = "$yy-${m.toString().padStart(2, '0')}"
                        val ovR = repo.request(mapOf("action" to "getAttendanceOverview", "month" to ym))
                        if (ovR.isOk) {
                            val arr = (ovR.raw["rows"] as? JsonArray) ?: JsonArray(emptyList())
                            arr.forEach { el ->
                                val o = el as? JsonObject ?: return@forEach
                                outRows.add(
                                    listOf(
                                        ym,
                                        "${MarathiUtils.classToMr(o.str("iyatta"))} ${o.str("tukdi")}".trim(),
                                        o.str("students"),
                                        o.str("days"),
                                        o.str("avgPct").ifBlank { "—" } + "%",
                                        o.str("absentDays")
                                    )
                                )
                            }
                        }
                    }
                    _adminState.update {
                        it.copy(
                            archiveLoading = false,
                            archiveSummary = "$year मधील हजेरी सारांश (${outRows.size} वर्ग-महिना नोंदी)",
                            archiveHeaders = listOf("महिना", "वर्ग", "विद्यार्थी", "कार्यदिवस", "सरासरी %", "गैरहजर दिवस"),
                            archiveRows = outRows
                        )
                    }
                }
                "el" -> {
                    val ys = year.substringBefore("-").toIntOrNull() ?: 2026
                    val elR = repo.request(mapOf("action" to "getEarlyLeave", "date" to "$ys-07-01"))
                    val recs = if (elR.isOk) repo.parseEarlyLeaveList(elR) else emptyList()
                    val rows = recs.map { listOf(MarathiUtils.fmtDate(it.date), it.regNo, it.fullName, "${MarathiUtils.classToMr(it.iyatta)} ${it.tukdi}".trim(), it.type, it.outStatus) }
                    _adminState.update {
                        it.copy(
                            archiveLoading = false,
                            archiveSummary = if (elR.isOk) "नोंदी: ${rows.size}" else "❌ ${elR.message}",
                            archiveHeaders = listOf("दिनांक", "Reg No", "नाव", "इयत्ता", "प्रकार", "स्थिती"),
                            archiveRows = rows
                        )
                    }
                }
                else -> _adminState.update { it.copy(archiveLoading = false) }
            }
        }
    }

    fun compareArchiveYears(yearA: String, yearB: String) {
        if (yearA.isBlank() || yearB.isBlank()) return
        scope.launch {
            _adminState.update { it.copy(archiveLoading = true, archiveSection = "cmp", archiveCompareYearA = yearA, archiveCompareYearB = yearB) }
            val rA = repo.request(mapOf("action" to "getArchiveReport", "year" to yearA, "section" to "fees"))
            val rB = repo.request(mapOf("action" to "getArchiveReport", "year" to yearB, "section" to "fees"))
            val tA = rA.raw["total"] as? JsonObject
            val tB = rB.raw["total"] as? JsonObject
            val cA = (rA.raw["classes"] as? JsonArray)?.mapNotNull { it as? JsonObject }?.associateBy { it.str("cls") }.orEmpty()
            val cB = (rB.raw["classes"] as? JsonArray)?.mapNotNull { it as? JsonObject }?.associateBy { it.str("cls") }.orEmpty()
            val allCls = (cA.keys + cB.keys).distinct()
            val rows = allCls.map { c ->
                val a = cA[c]
                val b = cB[c]
                listOf(
                    MarathiUtils.classToMr(c),
                    "${a?.intVal("students") ?: 0} / ${MarathiUtils.fmtRupees(a?.doubleVal("paid") ?: 0.0)}",
                    "${b?.intVal("students") ?: 0} / ${MarathiUtils.fmtRupees(b?.doubleVal("paid") ?: 0.0)}",
                    MarathiUtils.fmtRupees((b?.doubleVal("paid") ?: 0.0) - (a?.doubleVal("paid") ?: 0.0))
                )
            }
            val summary = "$yearA: जमा ${MarathiUtils.fmtRupees(tA?.doubleVal("paid") ?: 0.0)} vs $yearB: जमा ${MarathiUtils.fmtRupees(tB?.doubleVal("paid") ?: 0.0)}"
            _adminState.update {
                it.copy(
                    archiveLoading = false,
                    archiveSummary = summary,
                    archiveHeaders = listOf("इयत्ता", "$yearA (विद्यार्थी / जमा)", "$yearB (विद्यार्थी / जमा)", "फरक"),
                    archiveRows = rows
                )
            }
        }
    }

    fun backupArchiveFile(year: String) {
        scope.launch {
            _adminState.update { it.copy(archiveLoading = true) }
            val r = repo.request(mapOf("action" to "backupArchiveFile", "year" to year), longTimeout = true)
            _adminState.update {
                it.copy(
                    archiveLoading = false,
                    archiveHealthText = if (r.isOk) {
                        val ex = r.raw["extra"] as? JsonObject
                        "✅ Archive Backup प्रत बनली: ${ex?.str("name").orEmpty()}"
                    } else "❌ ${r.message}"
                )
            }
        }
    }

    fun checkArchiveHealth(year: String) {
        scope.launch {
            val r = repo.request(mapOf("action" to "getArchiveHealth", "year" to year))
            if (!r.isOk) {
                _adminState.update { it.copy(archiveHealthText = "❌ ${r.message}") }
                return@launch
            }
            val probs = (r.raw["problems"] as? JsonArray)?.map { it.toString().trim('"') }.orEmpty()
            val warns = (r.raw["warnings"] as? JsonArray)?.map { it.toString().trim('"') }.orEmpty()
            val txt = buildString {
                if (probs.isEmpty()) appendLine("✅ Archive $year आरोग्य ठीक आहे (Sheets: ${r.raw.intVal("sheetCount")})")
                else appendLine("❌ समस्या: ${probs.joinToString("; ")}")
                if (warns.isNotEmpty()) appendLine("⚠️ इशारे: ${warns.joinToString("; ")}")
                val inBk = r.raw.boolVal("inBackupList")
                append("Backup यादीत: ${if (inBk) "होय ✅" else "नाही ⚠️"}")
            }.trim()
            _adminState.update { it.copy(archiveHealthText = txt) }
        }
    }

    // ===== Super Admin =====
    fun loadSuperAdminAll() {
        scope.launch {
            val sec = repo.request(mapOf("action" to "getSecurityStatus"))
            val secLines = if (sec.isOk) listOf(
                (if (sec.raw.boolVal("tokenActive")) "✅" else "⚠️") + " Server Token Session: " + if (sec.raw.boolVal("tokenActive")) "सक्रिय" else "निष्क्रिय",
                (if (sec.raw.boolVal("enforced")) "✅" else "⚠️") + " Token सक्ती: " + if (sec.raw.boolVal("enforced")) "चालू" else "बंद",
                (if (sec.raw.boolVal("passwordsHashed")) "✅" else "ℹ️") + " Passwords Hashed: " + sec.raw.boolVal("passwordsHashed")
            ) else listOf("❌ सुरक्षा स्थिती Load झाली नाही")

            val bk = repo.request(mapOf("action" to "getBackupStatus"))
            val bkInfo = if (bk.isOk) {
                val last = bk.raw["last"] as? JsonObject
                "वेळापत्रक: ${bk.raw.str("schedule")} | शेवटचा Backup: ${last?.str("stamp").orEmpty().ifBlank { last?.str("time").orEmpty() }.ifBlank { "नाही" }}"
            } else "Backup स्थिती Load झाली नाही"

            val dr = repo.request(mapOf("action" to "getRestoreDrill"))
            val drLast = dr.raw["last"] as? JsonObject
            val drInfo = if (dr.isOk && drLast != null) {
                val det = (drLast["details"] as? JsonArray)?.take(3)?.joinToString(" | ") { it.toString().trim('"') }.orEmpty()
                "स्तर: ${drLast.str("level")} | फाइल्स: ${drLast.str("files")}" + if (det.isNotBlank()) "\n$det" else ""
            } else "अजून तपासणी झालेली नाही"

            val sz = repo.request(mapOf("action" to "getSheetSizes"))
            val szList = (sz.raw["sheets"] as? JsonArray)?.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                o.str("name") to o.intVal("rows")
            }.orEmpty()

            val promo = repo.request(mapOf("action" to "previewPromotion"))
            val pClasses = (promo.raw["classes"] as? JsonArray)?.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                val sug = o.str("suggestedNext")
                PromotionClassRow(o.str("iyatta"), o.intVal("count"), sug, sug)
            }.orEmpty()
            val pBatches = (promo.raw["recentBatches"] as? JsonArray)?.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                PromoBatchItem(o.str("batchId"), o.str("time"), o.intVal("count"), o.str("by"))
            }.orEmpty()
            val aBatches = (promo.raw["archiveBatches"] as? JsonArray)?.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                ArchiveBatchItem(o.str("batchId"), o.str("iyatta"), o.intVal("count"), o.str("sheet"))
            }.orEmpty()

            _adminState.update {
                it.copy(
                    securityLines = secLines,
                    backupInfo = bkInfo,
                    backupSchedule = bk.raw.str("schedule").ifBlank { "off" },
                    backupExtraIds = bk.raw.str("extraIds"),
                    backupAlertEmail = bk.raw.str("alertEmail"),
                    restoreDrillInfo = drInfo,
                    sheetSizes = szList,
                    promoClasses = pClasses,
                    promoBatches = pBatches,
                    archiveBatches = aBatches
                )
            }
        }
    }

    fun loadPendingFeesAll() {
        scope.launch {
            _adminState.update { it.copy(superStatus = BannerMessage.Info("⏳ थकबाकी यादी Load होत आहे...")) }
            val r = repo.request(mapOf("action" to "getPendingFeesAll", "nocache" to "1"))
            if (!r.isOk) {
                _adminState.update { it.copy(superStatus = BannerMessage.Err("❌ ${r.message}")) }
                return@launch
            }
            val totalFee = r.raw.doubleVal("totalFee", 1000.0)
            val sumObj = r.raw["summary"] as? JsonObject
            val arr = r.raw["data"] as? JsonArray ?: JsonArray(emptyList())
            val list = arr.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                PendingFeeRow(
                    fullName = o.str("fullName"), iyatta = o.str("iyatta"), tukdi = o.str("tukdi"),
                    rollNo = o.str("rollNo"), regNo = o.str("regNo"),
                    paid = o.doubleVal("paid"), opening = o.doubleVal("opening"),
                    pending = o.doubleVal("pending"), mobile = o.str("mobile"), lastPaidDate = o.str("lastPaidDate")
                )
            }
            val summaryTxt = "एकूण फी ₹${totalFee.toInt()} | थकबाकीदार: ${sumObj?.intVal("pendingCount") ?: list.size} | एकूण थकबाकी: ₹${sumObj?.intVal("pendingTotal") ?: 0} | जमा: ₹${sumObj?.intVal("collectedTotal") ?: 0}"
            _adminState.update {
                it.copy(pendingFeesTotalSetting = totalFee, pendingFees = list, pendingFeesSummary = summaryTxt, superStatus = null)
            }
        }
    }

    fun setPromotionTarget(iyatta: String, target: String) {
        _adminState.update { st ->
            st.copy(promoClasses = st.promoClasses.map { if (it.iyatta == iyatta) it.copy(selectedTarget = target) else it })
        }
    }

    fun runPromotion(newYear: String) {
        val mapObj = buildJsonObject {
            _adminState.value.promoClasses.forEach { if (it.selectedTarget.isNotBlank()) put(it.iyatta, it.selectedTarget) }
        }
        scope.launch {
            _adminState.update { it.copy(superStatus = BannerMessage.Info("⏳ वर्ग-बढती चालू आहे...")) }
            val r = repo.request(mapOf("action" to "promoteStudents", "mapJson" to mapObj.toString(), "newAcYear" to newYear.trim(), "confirm" to "PROMOTE"), longTimeout = true)
            if (r.isOk) {
                repo.storage.clearStudentCache()
                _adminState.update { it.copy(superStatus = BannerMessage.Ok("✅ वर्ग-बढती पूर्ण! ${r.raw.str("count")} विद्यार्थी हलवले.")) }
                loadSuperAdminAll()
            } else {
                _adminState.update { it.copy(superStatus = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    fun undoPromotion(batchId: String) {
        scope.launch {
            val r = repo.request(mapOf("action" to "undoPromotion", "batchId" to batchId), longTimeout = true)
            if (r.isOk) {
                repo.storage.clearStudentCache()
                _adminState.update { it.copy(superStatus = BannerMessage.Ok("✅ बढती Undo झाली.")) }
                loadSuperAdminAll()
            } else {
                _adminState.update { it.copy(superStatus = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    fun runSuperAction(action: String, params: Map<String, String>, successLabel: String, longTimeout: Boolean = true) {
        scope.launch {
            _adminState.update { it.copy(superStatus = BannerMessage.Info("⏳ प्रक्रिया चालू आहे...")) }
            val q = params.toMutableMap().apply { put("action", action) }
            val r = repo.request(q, longTimeout = longTimeout)
            if (r.isOk) {
                repo.storage.clearStudentCache()
                _adminState.update { it.copy(superStatus = BannerMessage.Ok("✅ $successLabel ${r.message}")) }
                loadSuperAdminAll()
            } else {
                _adminState.update { it.copy(superStatus = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    fun setBackupSchedule(mode: String, extraIds: String, alertEmail: String) {
        runSuperAction(
            "setBackupSchedule",
            mapOf("mode" to mode, "extraIds" to extraIds.trim(), "alertEmail" to alertEmail.trim()),
            "Backup वेळापत्रक अद्ययावत ($mode)",
            longTimeout = false
        )
    }

    fun runRestoreDrill() {
        scope.launch {
            _adminState.update { it.copy(restoreDrillRunning = true, superStatus = BannerMessage.Info("⏳ Backup पुनर्संचयन तपासणी चालू...")) }
            for (step in 1..6) {
                val r = repo.request(mapOf("action" to "runRestoreDrill"), longTimeout = true)
                if (!r.isOk) {
                    _adminState.update { it.copy(restoreDrillRunning = false, superStatus = BannerMessage.Err("❌ ${r.message}")) }
                    return@launch
                }
                if (r.raw.boolVal("complete")) {
                    val last = r.raw["last"] as? JsonObject
                    val det = (last?.get("details") as? JsonArray)?.joinToString("\n") { it.toString().trim('"') }.orEmpty()
                    _adminState.update {
                        it.copy(
                            restoreDrillRunning = false,
                            restoreDrillInfo = "स्तर: ${last?.str("level") ?: "ok"} | फाइल्स: ${last?.str("files") ?: "—"}\n$det",
                            superStatus = BannerMessage.Ok("✅ Backup पुनर्संचयन तपासणी पूर्ण!")
                        )
                    }
                    return@launch
                }
                delay(400)
            }
            _adminState.update { it.copy(restoreDrillRunning = false, superStatus = BannerMessage.Info("⏳ तपासणी अंशतः झाली — पुन्हा दाबा.")) }
        }
    }

    fun shrinkSheets() = runSuperAction("shrinkSheets", mapOf("confirm" to "SHRINK"), "रिकाम्या ओळी/स्तंभ साफ केले!")
    fun rebuildFeesSummary(year: String) = runSuperAction("rebuildFeesSummary", mapOf("year" to year.trim()), "$year फी सारांश पुन्हा बांधला!")
    fun renumberRolls(classes: List<String>, order: String) {
        val arr = buildJsonArray { classes.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }.toString()
        runSuperAction("renumberRolls", mapOf("confirm" to "RENUMBER", "order" to order, "classesJson" to arr), "रोल नं. क्रमवारी पूर्ण!")
    }
    fun restorePassedOut(batchId: String) = runSuperAction("restorePassedOut", mapOf("batchId" to batchId), "Pass-out पुनर्संचयित!")
    fun migrateLogs() = runSuperAction("migrateLogs", mapOf("confirm" to "LOG"), "Log मासिक स्थलांतर पूर्ण!")
    fun archiveLogNow(days: Int = 90) = runSuperAction("archiveLogNow", mapOf("days" to days.toString()), "जुने Log Archive केले!")

    fun stopPhotoRecall() {
        _adminState.update { it.copy(photoRecallStopRequested = true) }
    }

    fun runPhotoRecall(dry: Boolean) {
        scope.launch {
            _adminState.update { it.copy(photoRecallRunning = true, photoRecallStopRequested = false, photoRecallReport = "⏳ फोटो-लिंक तपासणी चालू...") }
            var runId = ""
            for (step in 1..25) {
                if (_adminState.value.photoRecallStopRequested) {
                    _adminState.update { it.copy(photoRecallRunning = false, photoRecallStopRequested = false, photoRecallReport = "⏹ थांबवले (${step - 1} टप्पे झाले).") }
                    return@launch
                }
                val q = mutableMapOf("action" to "photoRecall", "dry" to if (dry) "1" else "0")
                if (runId.isNotBlank()) q["runId"] = runId
                val r = repo.request(q, longTimeout = true)
                if (!r.isOk) {
                    _adminState.update { it.copy(photoRecallRunning = false, photoRecallReport = "❌ ${r.message}") }
                    return@launch
                }
                runId = r.raw.str("runId")
                val rep = r.raw["report"] as? JsonObject
                if (r.raw.boolVal("done")) {
                    val summary = "फाइल्स: ${rep?.str("files") ?: "—"} | जुळलेले: ${rep?.str("matched") ?: "—"} | अद्ययावत: ${rep?.str("updated") ?: "—"} | फोटो नसलेले: ${rep?.str("studentsWithoutPhoto") ?: "—"}"
                    if (!dry) repo.storage.clearStudentCache()
                    _adminState.update { it.copy(photoRecallRunning = false, photoRecallStopRequested = false, photoRecallReport = "✅ पूर्ण! $summary") }
                    return@launch
                }
                _adminState.update { it.copy(photoRecallReport = "⏳ टप्पा $step चालू... जुळलेले: ${rep?.str("matched") ?: "..."}") }
                delay(350)
            }
            _adminState.update { it.copy(photoRecallRunning = false, photoRecallReport = "⏹ वेळ मर्यादा — पुन्हा चालवा.") }
        }
    }

    // ===== Data QC & Year-End Wizard =====
    fun loadDataQcTab(
        tab: String,
        days: Int = _adminState.value.usageDays,
        teachers: Int = _adminState.value.usageTeachers
    ) {
        _adminState.update { it.copy(qcTab = tab, qcLoading = true, usageDays = days, usageTeachers = teachers) }
        scope.launch {
            when (tab) {
                "q" -> {
                    val r = repo.request(mapOf("action" to "getDataQuality"))
                    val countsObj = r.raw["counts"] as? JsonObject
                    val counts = countsObj?.entries?.associate { it.key to (it.value.toString().trim('"').toIntOrNull() ?: 0) }.orEmpty()
                    val clsArr = r.raw["classes"] as? JsonArray ?: JsonArray(emptyList())
                    val clsRows = clsArr.mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        val bt = o["byType"] as? JsonObject
                        listOf(
                            MarathiUtils.classToMr(o.str("cls")),
                            o.str("students"),
                            o.str("withIssues"),
                            bt?.str("dup_reg")?.ifBlank { "0" } ?: "0",
                            bt?.str("dob_missing")?.ifBlank { "0" } ?: "0",
                            bt?.str("wa_missing")?.ifBlank { "0" } ?: "0",
                            bt?.str("photo_missing")?.ifBlank { "0" } ?: "0"
                        )
                    }
                    _adminState.update {
                        it.copy(
                            qcLoading = false,
                            qcTotalStudents = r.raw.intVal("total"),
                            qcStudentsWithIssues = r.raw.intVal("studentsWithIssues"),
                            qcAgeTol = r.raw.intVal("ageTol", 3),
                            qcSummaryCounts = counts,
                            qcClassSummaryRows = clsRows
                        )
                    }
                }
                "y" -> {
                    val r = repo.request(mapOf("action" to "getYearEndCheck"))
                    val warns = (r.raw["warnings"] as? JsonArray)?.map { it.toString().trim('"') }.orEmpty()
                    val tot = r.raw["total"] as? JsonObject
                    val bk = r.raw["backup"] as? JsonObject
                    val header = "सध्याचे वर्ष: ${r.raw.str("current")} → नवीन वर्ष: ${r.raw.str("suggestedNew")} | विद्यार्थी: ${tot?.intVal("students") ?: 0} | थकबाकी: ${tot?.intVal("withDues") ?: 0} (${MarathiUtils.fmtRupees(tot?.doubleVal("dueSum") ?: 0.0)}) | LC बाकी (${r.raw.str("lastClass")}): ${tot?.intVal("lcPending") ?: 0}"
                    val bkCheck = "Backup स्थिती (४८ तास)" to if (bk?.boolVal("ok") == true) "✅ ताजा Backup (${bk.str("hoursAgo")} तास आधी)" else "⚠️ ताजा Backup नाही"
                    val clsArr = r.raw["classes"] as? JsonArray ?: JsonArray(emptyList())
                    val clsRows = clsArr.mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        listOf(
                            MarathiUtils.classToMr(o.str("cls")),
                            o.str("next"),
                            o.str("students"),
                            "${o.str("withDues")} (₹${o.str("dueSum")})",
                            "${o.str("withLC")} / बाकी ${o.str("lcPending")}",
                            o.str("withDqIssues")
                        )
                    }
                    val duesRows = (r.raw["dues"] as? JsonArray)?.mapNotNull { el ->
                        val a = el as? JsonArray ?: return@mapNotNull null
                        listOf(
                            a.getOrNull(0)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(1)?.toString()?.trim('"').orEmpty(),
                            "${MarathiUtils.classToMr(a.getOrNull(2)?.toString()?.trim('"'))} ${a.getOrNull(3)?.toString()?.trim('"').orEmpty()}".trim(),
                            a.getOrNull(5)?.toString()?.trim('"').orEmpty()
                        )
                    }.orEmpty()
                    val lcRows = (r.raw["lc"] as? JsonArray)?.mapNotNull { el ->
                        val a = el as? JsonArray ?: return@mapNotNull null
                        listOf(
                            a.getOrNull(0)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(1)?.toString()?.trim('"').orEmpty(),
                            "${MarathiUtils.classToMr(a.getOrNull(2)?.toString()?.trim('"'))} ${a.getOrNull(3)?.toString()?.trim('"').orEmpty()}".trim(),
                            a.getOrNull(4)?.toString()?.trim('"').orEmpty(),
                            a.getOrNull(5)?.toString()?.trim('"').orEmpty()
                        )
                    }.orEmpty()
                    _adminState.update {
                        it.copy(
                            qcLoading = false,
                            yearEndSummaryHeader = header,
                            yearEndWarnings = warns,
                            yearEndChecks = listOf(bkCheck),
                            yearEndClassHeaders = listOf("इयत्ता", "पुढील इयत्ता", "विद्यार्थी", "थकबाकी", "LC", "डेटा त्रुटी"),
                            yearEndClassRows = clsRows,
                            yearEndDuesRows = duesRows,
                            yearEndLcRows = lcRows
                        )
                    }
                }
                "c" -> {
                    val r = repo.request(mapOf("action" to "getSheetCapacity", "nocache" to "1"))
                    val pct = r.raw.doubleVal("pct")
                    val level = r.raw.str("level").ifBlank { "ok" }
                    val topArr = r.raw["top"] as? JsonArray ?: JsonArray(emptyList())
                    val rows = topArr.mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        listOf(o.str("name"), o.intVal("cells").toString())
                    }
                    val archArr = r.raw["archivable"] as? JsonArray ?: JsonArray(emptyList())
                    val archRows = archArr.mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        listOf(o.str("year"), o.intVal("cells").toString())
                    }
                    val sumTxt = "वापर: ${r.raw.intVal("total")} / ${r.raw.intVal("limit", 10000000)} cells ($pct%) | Sheets: ${r.raw.intVal("sheets")}"
                    _adminState.update {
                        it.copy(
                            qcLoading = false,
                            sheetCapSummary = sumTxt,
                            sheetCapPct = pct,
                            sheetCapLevel = level,
                            sheetCapRows = rows,
                            sheetCapArchivableRows = archRows
                        )
                    }
                }
                "e" -> {
                    val r = repo.request(mapOf("action" to "getClientErrors"))
                    val arr = r.raw["data"] as? JsonArray ?: JsonArray(emptyList())
                    val rows = arr.mapNotNull {
                        val o = it as? JsonObject ?: return@mapNotNull null
                        listOf(o.str("type"), o.str("message"), o.str("count"), o.str("last"))
                    }
                    val csvRows = arr.mapNotNull {
                        val o = it as? JsonObject ?: return@mapNotNull null
                        listOf(o.str("type"), o.str("message"), o.str("count"), o.str("users"), o.str("last"), o.str("vers"), o.str("pages"), o.str("stack"))
                    }
                    val sumTxt = "एकूण त्रुटी: ${r.raw.intVal("total")} (वाचलेल्या नोंदी: ${r.raw.intVal("rowsRead")})"
                    _adminState.update { it.copy(qcLoading = false, clientErrSummary = sumTxt, clientErrorRows = rows, clientErrorCsvRows = csvRows) }
                }
                "u" -> {
                    val r = repo.request(mapOf("action" to "getUsageStats", "days" to days.toString(), "teachers" to teachers.toString()))
                    val arr = r.raw["actions"] as? JsonArray ?: JsonArray(emptyList())
                    val rows = arr.mapNotNull {
                        val o = it as? JsonObject ?: return@mapNotNull null
                        listOf(o.str("action"), o.str("est"), o.str("avgMs") + "ms", o.str("err"), o.str("lockFails"))
                    }
                    val scArr = r.raw["scenarios"] as? JsonArray ?: JsonArray(emptyList())
                    val scRows = scArr.mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        listOf("${o.str("windowMin")} मि.", "${o.str("util")}%", "${o.str("maxWaitSec")}s", o.str("level"))
                    }
                    val tips = (r.raw["tips"] as? JsonArray)?.map { it.toString().trim('"') }.orEmpty()
                    val summary = "मागील $days दिवस ($teachers शिक्षक): अंदाजित विनंत्या ${r.raw.str("totalEst")} | त्रुटी: ${r.raw.str("totalErr")} | Lock अपयश: ${r.raw.str("totalLockFails")}"
                    _adminState.update {
                        it.copy(
                            qcLoading = false,
                            usageSummaryText = summary,
                            usageRows = rows,
                            usageScenarioRows = scRows,
                            usageTips = tips
                        )
                    }
                }
                "g" -> loadYearEndWizard()
            }
        }
    }

    fun loadQcDetail(type: String) {
        scope.launch {
            val r = repo.request(mapOf("action" to "getDataQuality", "type" to type))
            val arr = (r.raw["rows"] as? JsonArray) ?: (r.raw["data"] as? JsonArray) ?: JsonArray(emptyList())
            val rows = arr.mapNotNull { el ->
                when (el) {
                    is JsonArray -> listOf(
                        el.getOrNull(1)?.toString()?.trim('"').orEmpty(),
                        el.getOrNull(2)?.toString()?.trim('"').orEmpty(),
                        "${MarathiUtils.classToMr(el.getOrNull(3)?.toString()?.trim('"'))} ${el.getOrNull(4)?.toString()?.trim('"').orEmpty()}".trim(),
                        el.getOrNull(6)?.toString()?.trim('"').orEmpty()
                    )
                    is JsonObject -> listOf(
                        el.str("regNo"),
                        el.str("fullName"),
                        "${el.str("iyatta")} ${el.str("tukdi")}".trim(),
                        el.str("detail")
                    )
                    else -> null
                }
            }
            _adminState.update { it.copy(qcDetailType = type, qcDetailTotal = r.raw.intVal("total", rows.size), qcDetailRows = rows) }
        }
    }

    fun loadYearEndWizard() {
        scope.launch {
            _adminState.update { it.copy(qcLoading = true) }
            val r = repo.request(mapOf("action" to "getYearEndWizard"))
            if (!r.isOk) {
                _adminState.update { it.copy(qcLoading = false, wizardStatus = BannerMessage.Err("❌ ${r.message}")) }
                return@launch
            }
            val arr = r.raw["steps"] as? JsonArray ?: JsonArray(emptyList())
            val titles = listOf(
                "ताजा Backup" to "Drive मध्ये पूर्ण प्रत बनते.",
                "डेटा तपासणी" to "दुबार Reg No, DOB, WhatsApp, वय तपासणी.",
                "थकबाकी आढावा" to "पुढच्या वर्षी जाण्यापूर्वी थकबाकी तपासा.",
                "शेवटच्या वर्गाची LC" to "१० वी च्या विद्यार्थ्यांची LC तपासा.",
                "Pass-out Archive" to "शेवटचा वर्ग PassedOut मध्ये हलवा.",
                "वर्ग-बढती" to "सर्व विद्यार्थी पुढील वर्गात हलवा.",
                "नवे वर्ष सुरू करा" to "नवे शैक्षणिक वर्ष सुरू करा.",
                "जुने वर्ष Archive" to "जुन्या वर्षाची Sheets Archive मध्ये हलवा.",
                "अंतिम पडताळणी" to "सर्व वर्ग व नोंदींची अंतिम तपासणी."
            )
            fun formatStepInfo(n: Int, info: JsonObject?): String {
                if (info == null) return ""
                return when (n) {
                    1 -> if (info.boolVal("fresh")) "✅ ताजा Backup उपलब्ध (${info.str("hoursAgo")} तास आधी)" else "⚠️ ताजा Backup नाही"
                    2 -> "डेटा-त्रुटी असलेले विद्यार्थी: ${info.intVal("issues")}"
                    3 -> "थकबाकी विद्यार्थी: ${info.intVal("count")} | एकूण: ₹${info.intVal("total")}"
                    4 -> "${info.str("lastClass")} एकूण: ${info.intVal("lastCount")} | LC बाकी: ${info.intVal("lcPending")}"
                    5 -> "${info.str("lastClass")} (${info.str("label")}): ${info.intVal("count")} विद्यार्थी"
                    6 -> "बढती झालेल्या नोंदी: ${info.intVal("promoted")}"
                    7 -> "सध्याचे वर्ष: ${info.str("cur")} → नवे वर्ष: ${info.str("newYear")}"
                    8 -> "Archive वर्ष: ${info.str("year")}"
                    else -> ""
                }
            }
            val steps = arr.mapNotNull {
                val o = it as? JsonObject ?: return@mapNotNull null
                val n = o.intVal("n", 1)
                val pair = titles.getOrNull(n - 1) ?: ("पायरी $n" to "")
                YearEndStepState(
                    n = n,
                    title = pair.first,
                    what = pair.second,
                    undoInfo = "",
                    status = o.str("status"),
                    by = o.str("by"),
                    reason = o.str("reason"),
                    infoSummary = formatStepInfo(n, o["info"] as? JsonObject)
                )
            }
            val vObj = r.raw["verify"] as? JsonObject
            val vRows = (vObj?.get("classes") as? JsonArray)?.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                listOf(
                    MarathiUtils.classToMr(o.str("cls")),
                    o.str("before").ifBlank { "—" },
                    o.str("expected").ifBlank { "—" },
                    o.str("actual"),
                    if (o.boolVal("ok")) "✅" else "⚠️"
                )
            }.orEmpty()
            val poObj = vObj?.get("passedOut") as? JsonObject
            val poSummary = if (poObj != null) "${poObj.str("sheet")}: अपेक्षित ${poObj.str("expected")} / वास्तविक ${poObj.str("actual")}" else ""

            val promoR = repo.request(mapOf("action" to "previewPromotion"))
            val pMap = mutableMapOf<String, String>()
            var pTotal = 0
            val pLines = (promoR.raw["classes"] as? JsonArray)?.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val iy = o.str("iyatta")
                val sug = o.str("suggestedNext")
                val cnt = o.intVal("count")
                if (sug.isNotBlank()) pMap[iy] = sug
                pTotal += cnt
                "${MarathiUtils.classToMr(iy)} ($cnt) → ${MarathiUtils.classToMr(sug.ifBlank { "—" })}"
            }.orEmpty()

            _adminState.update {
                it.copy(
                    qcLoading = false,
                    wizardClosingYear = r.raw.str("closing"),
                    wizardNewYear = r.raw.str("newYear"),
                    wizardCurrentYear = r.raw.str("current"),
                    wizardLastClass = r.raw.str("lastClass"),
                    wizardComplete = r.raw.boolVal("complete"),
                    wizardSteps = steps,
                    wizardVerifySummary = if (vObj != null) "सध्याचे वर्ष: ${vObj.str("current")} | न जुळलेले विद्यार्थी: ${vObj.intVal("notNewYear")}" else "",
                    wizardVerifyRows = vRows,
                    wizardVerifyPassedOut = poSummary,
                    wizardVerifyNotNewYear = vObj?.intVal("notNewYear") ?: 0,
                    wizardVerifyAllOk = vObj?.boolVal("allOk") == true,
                    wizardPromoPreview = pLines.joinToString(", "),
                    wizardPromoMap = pMap,
                    wizardPromoTotal = pTotal
                )
            }
        }
    }

    fun runWizardPromotion() {
        val st = _adminState.value
        val mapObj = buildJsonObject {
            st.wizardPromoMap.forEach { (k, v) -> if (v.isNotBlank()) put(k, v) }
        }
        scope.launch {
            _adminState.update { it.copy(wizardStatus = BannerMessage.Info("⏳ वर्ग-बढती चालू...")) }
            val r = repo.request(mapOf("action" to "promoteStudents", "mapJson" to mapObj.toString(), "newAcYear" to st.wizardNewYear, "confirm" to "PROMOTE"), longTimeout = true)
            if (r.isOk) {
                repo.storage.clearStudentCache()
                _adminState.update { it.copy(wizardStatus = BannerMessage.Ok("✅ वर्ग-बढती पूर्ण!")) }
                loadYearEndWizard()
            } else {
                _adminState.update { it.copy(wizardStatus = BannerMessage.Err("❌ ${r.message}")) }
            }
        }
    }

    fun setWizardStepStatus(step: Int, status: String, reason: String = "") {
        scope.launch {
            val r = repo.request(
                mapOf(
                    "action" to "setYearEndStep",
                    "year" to _adminState.value.wizardClosingYear,
                    "step" to step.toString(),
                    "status" to status,
                    "reason" to reason
                )
            )
            if (r.isOk) loadYearEndWizard() else onToast(BannerMessage.Err(r.message))
        }
    }
}
