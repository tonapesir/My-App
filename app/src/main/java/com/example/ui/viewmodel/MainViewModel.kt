package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalStorage
import com.example.data.remote.GasApiClient
import com.example.data.remote.doubleVal
import com.example.data.remote.intVal
import com.example.data.remote.str
import com.example.data.repository.SchoolRepository
import com.example.data.repository.SmartSaveResult
import com.example.domain.model.AppPage
import com.example.domain.model.AttendanceCertFormData
import com.example.domain.model.BirthdayStudent
import com.example.domain.model.BonafideFormData
import com.example.domain.model.HolidayItem
import com.example.domain.model.LcFormData
import com.example.domain.model.MessageTemplate
import com.example.domain.model.Student
import com.example.domain.model.TeacherTab
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import com.example.worker.SgsWorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = LocalStorage(application)
    private val api = GasApiClient(application)
    val repo = SchoolRepository(application, storage, api)

    private val _shellState = MutableStateFlow(
        ShellUiState(
            session = repo.currentSession,
            currentPage = repo.currentSession?.let { AppPage.defaultFor(it.role) } ?: AppPage.DASHBOARD,
            sheetsUrl = repo.getUrl(),
            isOnline = repo.isOnline(),
            isOfflineBanner = repo.currentSession?.offline == true || !repo.isOnline(),
            pendingSaves = storage.loadPendingSaves(),
            cacheCount = storage.loadStudentCache()?.first?.size ?: 0,
            isDarkTheme = storage.getIsDarkTheme(),
            biometricEnabled = storage.getBiometricEnabled(),
            isBiometricLocked = repo.currentSession != null && storage.getBiometricEnabled(),
            remindersEnabled = storage.getRemindersEnabled(),
            holidays = storage.loadHolidays(),
            pinnedQuickKeys = repo.currentSession?.let { storage.loadPinnedQuickKeys(it.username) }.orEmpty(),
            messageTemplates = storage.loadMessageTemplates(),
            autoLockMinutes = storage.getAutoLockMinutes()
        )
    )
    val shellState: StateFlow<ShellUiState> = _shellState.asStateFlow()

    private val _loginState = MutableStateFlow(
        storage.getValidBiometricCredential().let { rec ->
            LoginUiState(
                biometricQuickAvailable = rec != null,
                biometricQuickLabel = rec?.let { "${it.label} (${it.username})" }.orEmpty()
            )
        }
    )
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _studentEntryState = MutableStateFlow(
        StudentEntryUiState(
            form = Student(acYear = storage.getAcYear(), admissionDate = MarathiUtils.todayIso())
        )
    )
    val studentEntryState: StateFlow<StudentEntryUiState> = _studentEntryState.asStateFlow()

    private val _certState = MutableStateFlow(
        CertFormsUiState(
            lc = LcFormData(lcDate = MarathiUtils.todayIso(), leaveDate = MarathiUtils.todayIso()),
            bf = BonafideFormData(acYear = storage.getAcYear(), bfDate = MarathiUtils.todayIso()),
            at = AttendanceCertFormData(acYear = storage.getAcYear(), atDate = MarathiUtils.todayIso())
        )
    )
    val certState: StateFlow<CertFormsUiState> = _certState.asStateFlow()

    private val _searchProfileState = MutableStateFlow(
        SearchAndProfileUiState(recentSearches = storage.loadRecentSearches())
    )
    val searchProfileState: StateFlow<SearchAndProfileUiState> = _searchProfileState.asStateFlow()

    val ops = TeacherAndOpsDelegate(
        repo = repo,
        scope = viewModelScope,
        onToast = { msg -> showToast(msg) },
        onRefreshQueue = { refreshPendingSaves() }
    )

    val admin = ReportsAndAdminDelegate(
        repo = repo,
        scope = viewModelScope,
        onToast = { msg -> showToast(msg) }
    )

    init {
        repo.onAuthExpired = {
            repo.logout(keepOffline = true)
            _shellState.update { it.copy(session = null, toastMessage = BannerMessage.Err("🔒 Session संपले — कृपया पुन्हा Login करा.")) }
        }
        repo.currentSession?.let { sess ->
            onPostLoginInit(sess.role)
        }
        SgsWorkScheduler.scheduleDailyReminders(application, storage.getRemindersEnabled())
        if (storage.loadPendingSaves().isNotEmpty()) {
            SgsWorkScheduler.enqueuePendingSync(application)
        }
    }

    fun showToast(msg: BannerMessage?) {
        _shellState.update { it.copy(toastMessage = msg) }
    }

    fun refreshConnectivityAndQueue() {
        val online = repo.isOnline()
        val off = repo.currentSession?.offline == true
        _shellState.update {
            it.copy(
                isOnline = online,
                isOfflineBanner = !online || off,
                pendingSaves = storage.loadPendingSaves()
            )
        }
    }

    private fun refreshPendingSaves() {
        _shellState.update { it.copy(pendingSaves = storage.loadPendingSaves()) }
    }

    // ===== Login & Auth =====
    fun updateLoginFields(username: String? = null, password: String? = null, rememberOffline: Boolean? = null) {
        _loginState.update {
            it.copy(
                username = username ?: it.username,
                password = password ?: it.password,
                rememberOffline = rememberOffline ?: it.rememberOffline,
                loginError = null
            )
        }
    }

    fun preConnect() {
        viewModelScope.launch {
            _loginState.update { it.copy(isConnecting = true, preConnectStatus = BannerMessage.Info("⏳ शाळेशी संपर्क करत आहे...")) }
            val r = repo.preConnect()
            _loginState.update {
                if (r.isOk) {
                    val ac = r.raw.str("acYear")
                    if (ac.isNotBlank()) storage.saveAcYear(ac)
                    it.copy(isConnecting = false, preConnectStatus = BannerMessage.Ok("✅ शाळेच्या सर्व्हरशी संपर्क झाला! (${ac.ifBlank { "Ready" }})"))
                } else {
                    it.copy(isConnecting = false, preConnectStatus = BannerMessage.Err("⚠️ ${r.message.ifBlank { "सर्व्हर संपर्क अयशस्वी" }}"))
                }
            }
        }
    }

    fun attemptLogin() {
        val ls = _loginState.value
        viewModelScope.launch {
            _loginState.update { it.copy(isLoggingIn = true, loginError = null) }
            val result = repo.login(ls.username, ls.password, ls.rememberOffline)
            result.onSuccess { sess ->
                val defaultPage = AppPage.defaultFor(sess.role)
                _loginState.update { it.copy(isLoggingIn = false, password = "", loginError = null) }
                _shellState.update {
                    it.copy(
                        session = sess,
                        currentPage = defaultPage,
                        teacherTab = TeacherTab.DASH,
                        isOfflineBanner = sess.offline || !repo.isOnline(),
                        pendingSaves = storage.loadPendingSaves(),
                        pinnedQuickKeys = storage.loadPinnedQuickKeys(sess.username),
                        isInactivityLocked = false
                    )
                }
                onPostLoginInit(sess.role)
            }.onFailure { err ->
                _loginState.update { it.copy(isLoggingIn = false, loginError = err.message ?: "Login अयशस्वी") }
            }
        }
    }

    private fun onPostLoginInit(role: UserRole) {
        when (role) {
            UserRole.SUPER, UserRole.MASTER -> ops.loadDashboard()
            UserRole.TEACHER -> ops.loadTeacherWorkspace()
            UserRole.PEON -> ops.initEarlyLeave()
            else -> {}
        }
        viewModelScope.launch {
            val (sent, _) = repo.flushPendingSaves()
            if (sent > 0) {
                refreshPendingSaves()
                showToast(BannerMessage.Ok("☁️ $sent Pending Save सर्व्हरवर पाठवले!"))
            }
            repo.flushClientErrors()
        }
    }

    fun loginWithBiometric() {
        val result = repo.loginWithBiometricCredential()
        result.onSuccess { sess ->
            val defaultPage = AppPage.defaultFor(sess.role)
            _loginState.update { it.copy(isLoggingIn = false, password = "", loginError = null) }
            _shellState.update {
                it.copy(
                    session = sess,
                    currentPage = defaultPage,
                    teacherTab = TeacherTab.DASH,
                    isOfflineBanner = sess.offline || !repo.isOnline(),
                    pendingSaves = storage.loadPendingSaves(),
                    isBiometricLocked = false,
                    isInactivityLocked = false,
                    pinnedQuickKeys = storage.loadPinnedQuickKeys(sess.username)
                )
            }
            onPostLoginInit(sess.role)
        }.onFailure { err ->
            _loginState.update { it.copy(loginError = err.message ?: "बायोमेट्रिक Login अयशस्वी") }
        }
    }

    fun unlockBiometricLock() {
        _shellState.update { it.copy(isBiometricLocked = false, isInactivityLocked = false) }
    }

    fun setAutoLockMinutes(minutes: Int) {
        val clean = minutes.coerceAtLeast(0)
        storage.saveAutoLockMinutes(clean)
        if (clean == 0) storage.saveLastBackgroundMs(0L)
        _shellState.update { it.copy(autoLockMinutes = clean) }
        showToast(
            if (clean == 0) BannerMessage.Info("🔓 ऑटो-लॉक बंद केले.")
            else BannerMessage.Ok("⏱️ ऑटो-लॉक: $clean मिनिट(s) सेट केले.")
        )
    }

    fun onAppBackgrounded(nowMs: Long = System.currentTimeMillis()) {
        val st = _shellState.value
        if (st.session != null && st.autoLockMinutes > 0) {
            storage.saveLastBackgroundMs(nowMs)
        }
    }

    fun onAppForegrounded(nowMs: Long = System.currentTimeMillis()) {
        val st = _shellState.value
        val mins = st.autoLockMinutes
        if (st.session != null && mins > 0) {
            val bgMs = storage.getLastBackgroundMs()
            if (bgMs > 0L && nowMs - bgMs >= mins * 60_000L) {
                _shellState.update { it.copy(isInactivityLocked = true) }
            }
        }
        storage.saveLastBackgroundMs(0L)
    }

    fun toggleDarkTheme() {
        val next = !_shellState.value.isDarkTheme
        storage.saveIsDarkTheme(next)
        _shellState.update { it.copy(isDarkTheme = next) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        storage.saveBiometricEnabled(enabled)
        _shellState.update { it.copy(biometricEnabled = enabled) }
        val rec = storage.getValidBiometricCredential()
        _loginState.update {
            it.copy(
                biometricQuickAvailable = rec != null,
                biometricQuickLabel = rec?.let { r -> "${r.label} (${r.username})" }.orEmpty()
            )
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        storage.saveRemindersEnabled(enabled)
        _shellState.update { it.copy(remindersEnabled = enabled) }
        SgsWorkScheduler.scheduleDailyReminders(getApplication(), enabled)
    }

    fun currentAcYear(): String = storage.getAcYear()

    fun logout() {
        repo.logout(keepOffline = false)
        storage.saveLastBackgroundMs(0L)
        val rec = storage.getValidBiometricCredential()
        _loginState.update {
            it.copy(
                biometricQuickAvailable = rec != null,
                biometricQuickLabel = rec?.let { r -> "${r.label} (${r.username})" }.orEmpty()
            )
        }
        _shellState.update {
            it.copy(
                session = null,
                cacheCount = 0,
                pendingSaves = storage.loadPendingSaves(),
                certPreview = null,
                isBiometricLocked = false,
                isInactivityLocked = false,
                pinnedQuickKeys = emptyList()
            )
        }
    }

    // ===== Navigation =====
    fun navigateTo(page: AppPage, teacherTab: TeacherTab? = null) {
        val role = _shellState.value.session?.role ?: return
        val allowed = AppPage.allowedFor(role)
        val target = if (page in allowed) page else AppPage.defaultFor(role)
        _shellState.update {
            it.copy(
                currentPage = target,
                teacherTab = teacherTab ?: it.teacherTab
            )
        }
        when (target) {
            AppPage.DASHBOARD -> ops.loadDashboard()
            AppPage.TEACHER -> {
                ops.loadTeacherWorkspace()
                if (teacherTab == TeacherTab.ANA) ops.loadTeacherAnalytics()
            }
            AppPage.EARLYLEAVE -> ops.initEarlyLeave()
            AppPage.SCHOLARSHIP -> ops.initScholarship()
            AppPage.CLASSINFO, AppPage.TEACHERLIST -> ops.loadClassListAndTeachers()
            AppPage.REPORT -> admin.initReport()
            AppPage.STATS -> admin.loadStatsReport()
            AppPage.ATTANALYTICS -> {
                ops.loadClassListAndTeachers()
                admin.loadAttendanceAnalytics("", "", MarathiUtils.currentMonthIso(), 3)
            }
            AppPage.USERS -> admin.loadUsers()
            AppPage.ARCHIVE -> admin.loadArchiveYears()
            AppPage.SUPERADMIN -> admin.loadSuperAdminAll()
            AppPage.DATAQC -> admin.loadDataQcTab(admin.adminState.value.qcTab)
            else -> {}
        }
    }

    fun selectTeacherTab(tab: TeacherTab) {
        _shellState.update { it.copy(currentPage = AppPage.TEACHER, teacherTab = tab) }
        if (tab == TeacherTab.ANA) ops.loadTeacherAnalytics()
        if (tab == TeacherTab.FEES) ops.loadTeacherFees()
    }

    fun navigateHomeForRole(): Boolean {
        val role = _shellState.value.session?.role ?: return false
        val home = AppPage.defaultFor(role)
        if (_shellState.value.currentPage == AppPage.TEACHER && _shellState.value.teacherTab != TeacherTab.DASH) {
            _shellState.update { it.copy(teacherTab = TeacherTab.DASH) }
            return true
        }
        if (_shellState.value.currentPage != home) {
            navigateTo(home)
            return true
        }
        return false
    }

    // ===== Config Dialog & Pending Saves Modal =====
    fun setConfigDialogVisible(show: Boolean) {
        _shellState.update { it.copy(showConfigDialog = show, testConnectionStatus = null) }
    }

    fun updateSheetsUrl(url: String) {
        repo.setUrl(url)
        _shellState.update { it.copy(sheetsUrl = url) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _shellState.update { it.copy(testConnectionStatus = BannerMessage.Info("⏳ संपर्क तपासत आहे...")) }
            val r = repo.preConnect()
            _shellState.update {
                it.copy(
                    testConnectionStatus = if (r.isOk) {
                        BannerMessage.Ok("✅ जोडणी यशस्वी! (${r.raw.str("acYear").ifBlank { "Connected" }})")
                    } else {
                        BannerMessage.Err("❌ ${r.message}")
                    }
                )
            }
        }
    }

    fun bulkLoadAllStudents() {
        viewModelScope.launch {
            _shellState.update { it.copy(isBulkLoading = true, bulkLoadMessage = "⏳ सर्व विद्यार्थी Load होत आहेत...") }
            val res = repo.bulkLoadStudents()
            res.onSuccess { list ->
                _shellState.update {
                    it.copy(
                        isBulkLoading = false,
                        cacheCount = list.size,
                        bulkLoadMessage = "✅ ${list.size} विद्यार्थी Cache मध्ये Load झाले!"
                    )
                }
                if (_shellState.value.currentPage == AppPage.SEARCH) runLocalSearch()
            }.onFailure { err ->
                _shellState.update {
                    it.copy(isBulkLoading = false, bulkLoadMessage = "❌ ${err.message}")
                }
            }
        }
    }

    fun clearStudentCache() {
        storage.clearStudentCache()
        _shellState.update { it.copy(cacheCount = 0, bulkLoadMessage = "🗑️ Cache पुसला.") }
    }

    fun setPendingModalVisible(show: Boolean) {
        refreshPendingSaves()
        _shellState.update { it.copy(showPendingModal = show) }
    }

    fun flushPendingSaves() {
        viewModelScope.launch {
            val (sent, fail) = repo.flushPendingSaves()
            refreshPendingSaves()
            showToast(
                if (sent > 0 && fail == 0) BannerMessage.Ok("✅ सर्व $sent Pending Save पाठवले!")
                else BannerMessage.Info("☁️ पाठवले: $sent | बाकी/त्रुटी: $fail")
            )
        }
    }

    fun removePendingSave(reqId: String) {
        val next = storage.loadPendingSaves().filterNot { it.reqId == reqId }
        storage.savePendingSaves(next)
        refreshPendingSaves()
    }

    fun clearAllPendingSaves() {
        storage.savePendingSaves(emptyList())
        refreshPendingSaves()
    }

    fun exportPendingSavesJson(): String = storage.exportPendingSavesJson()

    fun logClientError(page: String, type: String, message: String, stack: String = "") {
        repo.logClientError(page, type, message, stack)
    }

    // ===== QR Scanner, Quick Actions Hub, Holidays & Birthdays =====
    fun setQrScannerVisible(show: Boolean) {
        _shellState.update { it.copy(showQrScanner = show) }
    }

    fun setQuickActionsVisible(show: Boolean) {
        _shellState.update { it.copy(showQuickActionsDialog = show) }
    }

    fun togglePinQuickKey(key: String) {
        val username = _shellState.value.session?.username ?: return
        val current = _shellState.value.pinnedQuickKeys.toMutableList()
        val pinnedNow: Boolean
        if (key in current) {
            current.remove(key)
            pinnedNow = false
        } else {
            current.add(key)
            pinnedNow = true
        }
        storage.savePinnedQuickKeys(username, current)
        _shellState.update { it.copy(pinnedQuickKeys = current) }
        showToast(BannerMessage.Info(if (pinnedNow) "⭐ जलद बारमध्ये पिन केले!" else "📌 पिन काढले."))
    }

    fun setTemplatesDialogVisible(show: Boolean) {
        _shellState.update {
            it.copy(showTemplatesDialog = show, messageTemplates = storage.loadMessageTemplates())
        }
    }

    fun saveMessageTemplate(title: String, body: String, editId: String? = null) {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.isBlank() || cleanBody.isBlank()) return
        val current = storage.loadMessageTemplates().toMutableList()
        if (!editId.isNullOrBlank()) {
            val idx = current.indexOfFirst { it.id == editId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(title = cleanTitle, body = cleanBody)
            } else {
                current.add(0, MessageTemplate(id = editId, title = cleanTitle, body = cleanBody))
            }
        } else {
            val newId = "tpl_${System.currentTimeMillis()}"
            current.add(0, MessageTemplate(id = newId, title = cleanTitle, body = cleanBody))
        }
        storage.saveMessageTemplates(current)
        _shellState.update { it.copy(messageTemplates = current) }
        showToast(BannerMessage.Ok("💬 मेसेज टेम्पलेट जतन केले: $cleanTitle"))
    }

    fun deleteMessageTemplate(id: String) {
        val next = storage.loadMessageTemplates().filterNot { it.id == id }
        storage.saveMessageTemplates(next)
        _shellState.update { it.copy(messageTemplates = next) }
    }

    fun resetDefaultMessageTemplates() {
        val defaults = storage.resetDefaultMessageTemplates()
        _shellState.update { it.copy(messageTemplates = defaults) }
        showToast(BannerMessage.Ok("🔄 मूळ (Default) टेम्पलेट्स पुनर्संचयित केले."))
    }

    private fun canManageHolidays(): Boolean {
        val role = _shellState.value.session?.role
        return role == UserRole.SUPER || role == UserRole.MASTER
    }

    fun setHolidayDialogVisible(show: Boolean) {
        _shellState.update { it.copy(showHolidayDialog = show, holidays = storage.loadHolidays()) }
    }

    fun addHoliday(dateIso: String, title: String) {
        if (!canManageHolidays()) {
            showToast(BannerMessage.Err("सुट्टी कॅलेंडर मॅनेज करण्याचे अधिकार फक्त Super Master व Master User यांनाच आहेत."))
            return
        }
        val iso = MarathiUtils.isoDateOrEmpty(dateIso).ifBlank { dateIso.trim() }
        val cleanTitle = title.trim()
        if (iso.isBlank() || cleanTitle.isBlank()) return
        val next = (storage.loadHolidays().filterNot { it.dateIso == iso } + HolidayItem(iso, cleanTitle))
            .sortedBy { it.dateIso }
        storage.saveHolidays(next)
        _shellState.update { it.copy(holidays = next) }
        showToast(BannerMessage.Ok("📅 सुट्टी जतन केली: $cleanTitle"))
    }

    fun deleteHoliday(dateIso: String) {
        if (!canManageHolidays()) {
            showToast(BannerMessage.Err("सुट्टी कॅलेंडर मॅनेज करण्याचे अधिकार फक्त Super Master व Master User यांनाच आहेत."))
            return
        }
        val next = storage.loadHolidays().filterNot { it.dateIso == dateIso }
        storage.saveHolidays(next)
        _shellState.update { it.copy(holidays = next) }
    }

    fun seedDefaultHolidays() {
        if (!canManageHolidays()) {
            showToast(BannerMessage.Err("सुट्टी कॅलेंडर मॅनेज करण्याचे अधिकार फक्त Super Master व Master User यांनाच आहेत."))
            return
        }
        val curYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val defaults = MarathiUtils.defaultPublicHolidays(curYear)
        val existingMap = storage.loadHolidays().associateBy { it.dateIso }.toMutableMap()
        defaults.forEach { h ->
            if (!existingMap.containsKey(h.dateIso)) {
                existingMap[h.dateIso] = h
            }
        }
        val merged = existingMap.values.sortedBy { it.dateIso }
        storage.saveHolidays(merged)
        _shellState.update { it.copy(holidays = merged) }
        showToast(BannerMessage.Ok("🇮🇳 ${defaults.size} प्रमुख शासकीय सुट्ट्या जोडल्या!"))
    }

    fun openBirthdaysDialog() {
        _shellState.update { it.copy(showBirthdaysDialog = true) }
        refreshQuickBirthdays()
    }

    fun closeBirthdaysDialog() {
        _shellState.update { it.copy(showBirthdaysDialog = false) }
    }

    fun refreshQuickBirthdays() {
        val today = MarathiUtils.todayIso()
        val cachedMatches = storage.loadStudentCache()?.first.orEmpty()
            .filter { MarathiUtils.isBirthdayToday(it.dob, today) }
            .map { st ->
                BirthdayStudent(
                    regNo = st.regNo,
                    studentId = st.studentId,
                    rollNo = st.rollNo,
                    fullName = st.fullName,
                    iyatta = st.iyatta,
                    tukdi = st.tukdi,
                    dob = st.dob,
                    whatsappMobile = st.whatsappMobile,
                    alternateMobile = st.alternateMobile,
                    contact = st.contact
                )
            }
        val dashBirthdays = ops.dashState.value.birthdays
        val combinedLocal = (dashBirthdays + cachedMatches).distinctBy { it.regNo.ifBlank { it.fullName } }
        _shellState.update { it.copy(quickBirthdays = combinedLocal) }

        viewModelScope.launch {
            val bdR = repo.request(mapOf("action" to "getTodayBirthdays"))
            if (bdR.isOk) {
                val remote = repo.parseBirthdays(bdR)
                val merged = (remote + cachedMatches).distinctBy { it.regNo.ifBlank { it.fullName } }
                _shellState.update { it.copy(quickBirthdays = merged) }
            }
        }
    }

    fun findStudentInCache(query: String): Student? {
        val code = MarathiUtils.parseQrLookupCode(query)
        if (code.isBlank()) return null
        val cached = storage.loadStudentCache()?.first.orEmpty()
        val results = _searchProfileState.value.results
        val pool = (results + cached).distinctBy { it.regNo }
        return pool.firstOrNull {
            it.regNo.equals(code, true) || it.studentId.equals(code, true)
        }
    }

    fun openQrInEarlyLeave(code: String) {
        val parsed = MarathiUtils.parseQrLookupCode(code)
        val stu = findStudentInCache(parsed)
        val role = _shellState.value.session?.role
        navigateTo(AppPage.EARLYLEAVE)
        if (role == UserRole.PEON) {
            ops.loadPeonEarlyLeave()
            showToast(BannerMessage.Info("🚪 QR स्कॅन: ${stu?.fullName ?: parsed} — यादीत तपासा"))
        } else if (stu != null) {
            if (role != UserRole.TEACHER) {
                ops.setEarlyLeaveFilters(classKey = "${stu.iyatta}|${stu.tukdi}")
            }
            ops.setEarlyLeaveDraft(stu.regNo, "permitted", 1)
            showToast(BannerMessage.Ok("✅ QR: ${stu.fullName} (${MarathiUtils.classToMr(stu.iyatta)} ${stu.tukdi}) निवडला"))
        } else {
            showToast(BannerMessage.Info("🚪 QR कोड: $parsed — संबंधित वर्ग निवडा"))
        }
    }

    fun openQrInSearch(code: String) {
        val parsed = MarathiUtils.parseQrLookupCode(code)
        updateSearchFilters(name = parsed)
        navigateTo(AppPage.SEARCH)
    }

    // ===== Student Entry & Photo Upload =====
    fun updateStudentSearchQuery(q: String) {
        _studentEntryState.update { it.copy(searchQuery = q) }
    }

    fun updateStudentForm(transform: (Student) -> Student) {
        _studentEntryState.update { st ->
            val updated = transform(st.form)
            val autoWords = if (updated.dob != st.form.dob && updated.dob.isNotBlank()) {
                MarathiUtils.dobToMarathiWords(updated.dob).ifBlank { updated.dobWords }
            } else updated.dobWords
            st.copy(form = updated.copy(dobWords = autoWords))
        }
    }

    fun clearStudentForm() {
        _studentEntryState.value = StudentEntryUiState(
            form = Student(acYear = storage.getAcYear(), admissionDate = MarathiUtils.todayIso())
        )
    }

    fun searchStudentForEntry() {
        val q = _studentEntryState.value.searchQuery.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _studentEntryState.update { it.copy(isLoading = true, status = BannerMessage.Info("⏳ शोधत आहे...")) }
            repo.searchStudentDirect(q).onSuccess { st ->
                if (st != null) {
                    storage.addRecentSearch(st)
                    _searchProfileState.update { it.copy(recentSearches = storage.loadRecentSearches()) }
                    _studentEntryState.update {
                        it.copy(isLoading = false, form = st, isExistingRow = st.rowIndex > 0, status = BannerMessage.Ok("✅ सापडले: ${st.fullName} (Row #${st.rowIndex})"))
                    }
                } else {
                    _studentEntryState.update { it.copy(isLoading = false, status = BannerMessage.Err("⚠️ '$q' सापडले नाही.")) }
                }
            }.onFailure { err ->
                _studentEntryState.update { it.copy(isLoading = false, status = BannerMessage.Err("❌ ${err.message}")) }
            }
        }
    }

    fun uploadStudentPhoto(uri: Uri) {
        val f = _studentEntryState.value.form
        val key = f.regNo.ifBlank { f.studentId }.ifBlank { f.fullName }
        if (key.isBlank()) {
            _studentEntryState.update { it.copy(photoStatus = BannerMessage.Err("⚠️ आधी Reg No किंवा नाव टाका.")) }
            return
        }
        viewModelScope.launch {
            _studentEntryState.update { it.copy(isUploadingPhoto = true, photoStatus = BannerMessage.Info("⏳ फोटो Compress आणि Drive Upload होत आहे...")) }
            repo.compressAndUploadPhoto(uri, key).onSuccess { url ->
                _studentEntryState.update {
                    it.copy(
                        isUploadingPhoto = false,
                        form = it.form.copy(photoUrl = url),
                        photoStatus = BannerMessage.Ok("✅ फोटो Drive ला Upload झाला!")
                    )
                }
            }.onFailure { err ->
                _studentEntryState.update {
                    it.copy(isUploadingPhoto = false, photoStatus = BannerMessage.Err("❌ ${err.message}"))
                }
            }
        }
    }

    fun testPhotoDrive() {
        viewModelScope.launch {
            _studentEntryState.update { it.copy(photoStatus = BannerMessage.Info("⏳ Drive Folder तपासत आहे...")) }
            val r = repo.request(mapOf("action" to "testPhotoFolder"))
            _studentEntryState.update {
                it.copy(
                    photoStatus = if (r.isOk) {
                        BannerMessage.Ok("✅ Drive Folder OK: ${r.raw.str("folderName").ifBlank { "Ready" }}")
                    } else {
                        BannerMessage.Err("❌ ${r.message}")
                    }
                )
            }
        }
    }

    fun saveStudent(isUpdate: Boolean) {
        val f = _studentEntryState.value.form
        if (f.fullName.isBlank()) {
            _studentEntryState.update { it.copy(status = BannerMessage.Err("⚠️ विद्यार्थ्याचे पूर्ण नाव आवश्यक आहे!")) }
            return
        }
        val aadharClean = f.aadhar.filter { it.isDigit() }
        if (aadharClean.isNotEmpty() && aadharClean.length != 12) {
            _studentEntryState.update { it.copy(status = BannerMessage.Err("⚠️ आधार कार्ड क्रमांक १२ अंकी असावा (किंवा रिकामा ठेवा).")) }
            return
        }
        viewModelScope.launch {
            _studentEntryState.update { it.copy(isLoading = true, status = BannerMessage.Info("⏳ Save होत आहे...")) }
            val action = if (isUpdate && f.rowIndex > 0) "update" else "upsert"
            val params = mutableMapOf(
                "action" to action,
                "acYear" to f.acYear.ifBlank { storage.getAcYear() },
                "regNo" to f.regNo.trim(),
                "studentId" to f.studentId.trim(),
                "bookNo" to f.bookNo.trim(),
                "aadhar" to aadharClean,
                "iyatta" to f.iyatta,
                "tukdi" to f.tukdi,
                "pen" to f.pen.trim(),
                "rollNo" to f.rollNo.trim(),
                "firstName" to f.fullName.trim(),
                "motherName" to f.motherName.trim(),
                "gender" to f.gender,
                "religion" to f.religion,
                "caste" to f.caste.trim(),
                "subcaste" to f.subcaste.trim(),
                "category" to f.category,
                "minority" to when (f.minority.trim()) {
                    "होय" -> "Yes"
                    "नाही" -> "No"
                    else -> f.minority.trim()
                },
                "dob" to f.dob.trim(),
                "dobWords" to f.dobWords.trim(),
                "nationality" to f.nationality.trim(),
                "motherTongue" to f.motherTongue,
                "birthVillage" to f.birthVillage.trim(),
                "prevSchool" to f.prevSchool.trim(),
                "admissionDate" to f.admissionDate.trim(),
                "admissionClass" to f.admissionClass,
                "address" to f.address.trim(),
                "photoUrl" to f.photoUrl.trim(),
                "whatsappMobile" to f.whatsappMobile.trim(),
                "alternateMobile" to f.alternateMobile.trim()
            )
            if (action == "update") params["rowIndex"] = f.rowIndex.toString()

            val res = repo.smartSave(action, params, "विद्यार्थी: ${f.fullName}")
            when (res) {
                is SmartSaveResult.Success -> {
                    val newRow = res.response.raw.intVal("rowIndex", f.rowIndex)
                    storage.clearStudentCache()
                    val updatedStudent = f.copy(rowIndex = newRow)
                    _studentEntryState.update {
                        it.copy(
                            isLoading = false,
                            form = updatedStudent,
                            isExistingRow = newRow > 0,
                            status = BannerMessage.Ok("✅ विद्यार्थी माहिती Save झाली! (${f.fullName})"),
                            savedDialogStudent = updatedStudent
                        )
                    }
                }
                is SmartSaveResult.Queued -> _studentEntryState.update { it.copy(isLoading = false, status = BannerMessage.Queued(res.message)) }
                is SmartSaveResult.Error -> _studentEntryState.update { it.copy(isLoading = false, status = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun dismissStudentSavedDialog() {
        _studentEntryState.update { it.copy(savedDialogStudent = null) }
    }

    // ===== Certificates: LC / Bonafide / Attendance =====
    fun updateLcSearch(q: String) { _certState.update { it.copy(lcSearchQuery = q) } }
    fun updateLcForm(fn: (LcFormData) -> LcFormData) { _certState.update { it.copy(lc = fn(it.lc)) } }
    fun updateBfSearch(q: String) { _certState.update { it.copy(bfSearchQuery = q) } }
    fun updateBfForm(fn: (BonafideFormData) -> BonafideFormData) { _certState.update { it.copy(bf = fn(it.bf)) } }
    fun updateAtSearch(q: String) { _certState.update { it.copy(atSearchQuery = q) } }
    fun updateAtForm(fn: (AttendanceCertFormData) -> AttendanceCertFormData) {
        _certState.update { st ->
            val next = fn(st.at)
            val tot = next.totalDays.toDoubleOrNull()
            val pres = next.presentDays.toDoubleOrNull()
            val autoPct = if (tot != null && pres != null && tot > 0) "${String.format(java.util.Locale.US, "%.1f", pres * 100.0 / tot)}%" else next.pct
            st.copy(at = next.copy(pct = autoPct))
        }
    }

    fun fillStudentIntoModule(student: Student, targetPage: AppPage) {
        storage.addRecentSearch(student)
        _searchProfileState.update { it.copy(recentSearches = storage.loadRecentSearches()) }
        when (targetPage) {
            AppPage.STUDENT -> _studentEntryState.update {
                it.copy(form = student, isExistingRow = student.rowIndex > 0, searchQuery = student.regNo, status = null)
            }
            AppPage.LC -> _certState.update {
                it.copy(
                    lcSearchQuery = student.regNo,
                    lc = it.lc.copy(
                        regNo = student.regNo.ifBlank { student.studentId },
                        studentId = student.studentId,
                        fullName = student.fullName,
                        motherName = student.motherName,
                        gender = student.gender,
                        dob = student.dob,
                        dobWords = student.dobWords.ifBlank { MarathiUtils.dobToMarathiWords(student.dob) },
                        iyatta = student.iyatta,
                        religion = student.religion,
                        caste = student.caste,
                        subcaste = student.subcaste,
                        nationality = student.nationality,
                        motherTongue = student.motherTongue,
                        birthPlace = student.birthVillage,
                        aadhar = student.aadhar,
                        pen = student.pen,
                        bookNo = student.bookNo,
                        prevSchool = student.prevSchool,
                        admissionDate = student.admissionDate,
                        admissionClass = student.admissionClass,
                        leaveClass = student.iyatta,
                        lcDate = MarathiUtils.todayIso(),
                        leaveDate = MarathiUtils.todayIso(),
                        lcNo = ""
                    ),
                    lcStatus = null
                )
            }
            AppPage.BONAFIDE -> _certState.update {
                it.copy(
                    bfSearchQuery = student.regNo,
                    bf = it.bf.copy(
                        regNo = student.regNo.ifBlank { student.studentId },
                        studentId = student.studentId,
                        fullName = student.fullName,
                        gender = student.gender,
                        dob = student.dob,
                        dobWords = student.dobWords.ifBlank { MarathiUtils.dobToMarathiWords(student.dob) },
                        iyatta = student.iyatta,
                        tukdi = student.tukdi,
                        birthPlace = student.birthVillage,
                        caste = student.caste,
                        religion = student.religion,
                        nationality = student.nationality,
                        acYear = student.acYear.ifBlank { storage.getAcYear() },
                        bfDate = MarathiUtils.todayIso(),
                        admissionDate = student.admissionDate,
                        admissionClass = student.admissionClass,
                        address = student.address,
                        contact = student.bestPhone,
                        pen = student.pen,
                        photoUrl = student.photoUrl,
                        certNo = ""
                    ),
                    bfStatus = null
                )
            }
            AppPage.ATTENDANCE -> _certState.update {
                it.copy(
                    atSearchQuery = student.regNo,
                    at = it.at.copy(
                        regNo = student.regNo.ifBlank { student.studentId },
                        studentId = student.studentId,
                        fullName = student.fullName,
                        gender = student.gender,
                        dob = student.dob,
                        iyatta = student.iyatta,
                        tukdi = student.tukdi,
                        acYear = student.acYear.ifBlank { storage.getAcYear() },
                        atDate = MarathiUtils.todayIso(),
                        pen = student.pen,
                        photoUrl = student.photoUrl,
                        certNo = ""
                    ),
                    atStatus = null
                )
            }
            AppPage.PROFILE -> openStudentProfile(student.regNo.ifBlank { student.studentId })
            else -> {}
        }
        navigateTo(targetPage)
    }

    fun searchForModule(query: String, targetPage: AppPage) {
        if (query.isBlank()) return
        viewModelScope.launch {
            repo.searchStudentDirect(query).onSuccess { stu ->
                if (stu != null) {
                    fillStudentIntoModule(stu, targetPage)
                    if (targetPage == AppPage.LC) {
                        val lcR = repo.request(mapOf("action" to "getLCByReg", "regNo" to stu.regNo, "studentId" to stu.studentId))
                        if (lcR.isOk && lcR.raw["data"] is JsonObject) {
                            val d = lcR.raw["data"] as JsonObject
                            val prevCount = lcR.raw.intVal("count", 1)
                            val csd = d.str("classStartDate")
                            val csdParts = csd.split("-")
                            _certState.update { st ->
                                st.copy(
                                    lc = st.lc.copy(
                                        admissionDate = d.str("admissionDate").ifBlank { st.lc.admissionDate },
                                        admissionClass = d.str("admissionClass").ifBlank { st.lc.admissionClass },
                                        leaveDate = d.str("lcLeaveDate").ifBlank { st.lc.leaveDate },
                                        leaveClass = d.str("lcLeaveClass").ifBlank { st.lc.leaveClass },
                                        classStartYear = csdParts.getOrNull(0).orEmpty().ifBlank { csd }.ifBlank { st.lc.classStartYear },
                                        classStartMonth = csdParts.getOrNull(1).orEmpty().ifBlank { st.lc.classStartMonth },
                                        lcNo = d.str("lcNo").ifBlank { st.lc.lcNo },
                                        lcDate = d.str("lcDate").ifBlank { st.lc.lcDate },
                                        conduct = d.str("conduct").ifBlank { st.lc.conduct },
                                        progress = d.str("progress").ifBlank { st.lc.progress },
                                        certReceived = d.str("stxt75").ifBlank { st.lc.certReceived },
                                        stxt76 = d.str("stxt76").ifBlank { st.lc.stxt76 },
                                        remarks = d.str("remarks").ifBlank { st.lc.remarks },
                                        medium = d.str("medium").ifBlank { st.lc.medium },
                                        nextSchool = d.str("nextSchool").ifBlank { st.lc.nextSchool },
                                        lcCount = d.str("lcCount").ifBlank { (prevCount + 1).toString() },
                                        feeStatus = d.str("feeStatus").ifBlank { st.lc.feeStatus },
                                        stxt60 = d.str("stxt60").ifBlank { st.lc.stxt60 },
                                        stxt61 = d.str("stxt61").ifBlank { st.lc.stxt61 },
                                        stxt62 = d.str("stxt62").ifBlank { st.lc.stxt62 },
                                        stxt63 = d.str("stxt63").ifBlank { st.lc.stxt63 },
                                        stxt67 = d.str("stxt67").ifBlank { st.lc.stxt67 },
                                        stxt68 = d.str("stxt68").ifBlank { st.lc.stxt68 },
                                        stxt69 = d.str("stxt69").ifBlank { st.lc.stxt69 },
                                        stxt70 = d.str("stxt70").ifBlank { st.lc.stxt70 },
                                        nationalityType = d.str("nationalityType").ifBlank { st.lc.nationalityType }
                                    ),
                                    lcStatus = BannerMessage.Info("ℹ️ मागील LC नोंद सापडली (एकूण नोंदी: $prevCount, लि.स.क्र. ${d.str("lcNo")})")
                                )
                            }
                        }
                    }
                } else {
                    val msg = BannerMessage.Err("⚠️ '$query' सापडले नाही.")
                    when (targetPage) {
                        AppPage.LC -> _certState.update { it.copy(lcStatus = msg) }
                        AppPage.BONAFIDE -> _certState.update { it.copy(bfStatus = msg) }
                        AppPage.ATTENDANCE -> _certState.update { it.copy(atStatus = msg) }
                        else -> showToast(msg)
                    }
                }
            }.onFailure { err -> showToast(BannerMessage.Err(err.message ?: "शोध अयशस्वी")) }
        }
    }

    fun saveLC() {
        val d = _certState.value.lc
        if (d.fullName.isBlank()) {
            _certState.update { it.copy(lcStatus = BannerMessage.Err("⚠️ विद्यार्थ्याचे नाव आवश्यक आहे.")) }
            return
        }
        val classStart = when {
            d.classStartYear.isNotBlank() && d.classStartMonth.isNotBlank() -> "${d.classStartYear.trim()}-${d.classStartMonth.trim()}"
            else -> d.classStartYear.trim()
        }
        viewModelScope.launch {
            _certState.update { it.copy(lcStatus = BannerMessage.Info("⏳ LC Save होत आहे...")) }
            val res = repo.smartSave(
                "upsert_lc",
                mapOf(
                    "stxt1" to d.regNo, "firstName" to d.fullName, "gender" to d.gender, "dob" to d.dob, "dobWords" to d.dobWords,
                    "iyatta" to d.iyatta, "religion" to d.religion, "caste" to d.caste, "nationality" to d.nationality,
                    "motherTongue" to d.motherTongue, "admissionDate" to d.admissionDate, "admissionClass" to d.admissionClass,
                    "lcLeaveDate" to d.leaveDate, "lcLeaveClass" to d.leaveClass, "classStartDate" to classStart,
                    "lcNo" to d.lcNo, "lcDate" to d.lcDate, "conduct" to d.conduct, "progress" to d.progress,
                    "stxt75" to d.certReceived, "stxt76" to d.stxt76,
                    "remarks" to d.remarks, "medium" to d.medium, "nextSchool" to d.nextSchool, "lcCount" to d.lcCount,
                    "feeStatus" to d.feeStatus,
                    "stxt60" to d.stxt60, "stxt61" to d.stxt61, "stxt62" to d.stxt62, "stxt63" to d.stxt63,
                    "stxt67" to d.stxt67, "stxt68" to d.stxt68.ifBlank { d.leaveDate }, "stxt69" to d.stxt69,
                    "stxt70" to d.stxt70, "nationalityType" to d.nationalityType
                ),
                "LC: ${d.fullName}"
            )
            when (res) {
                is SmartSaveResult.Success -> {
                    val serial = res.serial.ifBlank { res.response.raw.str("serial") }
                    _certState.update {
                        it.copy(
                            lc = it.lc.copy(lcNo = it.lc.lcNo.ifBlank { serial }),
                            lcStatus = BannerMessage.Ok("✅ LC Save झाला! (लि.स.क्र. $serial)")
                        )
                    }
                }
                is SmartSaveResult.Queued -> _certState.update { it.copy(lcStatus = BannerMessage.Queued(res.message)) }
                is SmartSaveResult.Error -> _certState.update { it.copy(lcStatus = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun saveBonafide() {
        val d = _certState.value.bf
        if (d.fullName.isBlank()) {
            _certState.update { it.copy(bfStatus = BannerMessage.Err("⚠️ विद्यार्थ्याचे नाव आवश्यक आहे.")) }
            return
        }
        viewModelScope.launch {
            _certState.update { it.copy(bfStatus = BannerMessage.Info("⏳ Bonafide Save होत आहे...")) }
            val res = repo.smartSave(
                "upsert_bf",
                mapOf(
                    "stxt1" to d.regNo, "firstName" to d.fullName, "dob" to d.dob, "dobWords" to d.dobWords,
                    "iyatta" to d.iyatta, "tukdi" to d.tukdi, "religion" to d.religion, "caste" to d.caste,
                    "nationality" to d.nationality, "acYear" to d.acYear, "bfDate" to d.bfDate, "purpose" to d.purpose,
                    "admissionDate" to d.admissionDate, "admissionClass" to d.admissionClass,
                    "remarks" to d.remarks, "address" to d.address, "contact" to d.contact
                ),
                "Bonafide: ${d.fullName}"
            )
            when (res) {
                is SmartSaveResult.Success -> {
                    val serial = res.serial.ifBlank { res.response.raw.str("serial") }
                    _certState.update {
                        it.copy(
                            bf = it.bf.copy(certNo = serial),
                            bfStatus = BannerMessage.Ok("✅ Bonafide Save झाले! (बोना.क्र. $serial)")
                        )
                    }
                }
                is SmartSaveResult.Queued -> _certState.update { it.copy(bfStatus = BannerMessage.Queued(res.message)) }
                is SmartSaveResult.Error -> _certState.update { it.copy(bfStatus = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun saveAttendanceCert() {
        val d = _certState.value.at
        if (d.fullName.isBlank()) {
            _certState.update { it.copy(atStatus = BannerMessage.Err("⚠️ विद्यार्थ्याचे नाव आवश्यक आहे.")) }
            return
        }
        viewModelScope.launch {
            _certState.update { it.copy(atStatus = BannerMessage.Info("⏳ उपस्थिती प्रमाणपत्र Save होत आहे...")) }
            val res = repo.smartSave(
                "upsert_at",
                mapOf(
                    "stxt1" to d.regNo, "firstName" to d.fullName, "gender" to d.gender, "dob" to d.dob,
                    "iyatta" to d.iyatta, "tukdi" to d.tukdi, "acYear" to d.acYear, "atDate" to d.atDate,
                    "purpose" to d.purpose, "totalDays" to d.totalDays, "presentDays" to d.presentDays,
                    "pct" to d.pct, "fromDate" to d.fromDate, "toDate" to d.toDate, "remarks" to d.remarks
                ),
                "Attendance Cert: ${d.fullName}"
            )
            when (res) {
                is SmartSaveResult.Success -> {
                    val serial = res.serial.ifBlank { res.response.raw.str("serial") }
                    _certState.update {
                        it.copy(
                            at = it.at.copy(certNo = serial),
                            atStatus = BannerMessage.Ok("✅ उपस्थिती प्रमाणपत्र Save झाले! (क्र. $serial)")
                        )
                    }
                }
                is SmartSaveResult.Queued -> _certState.update { it.copy(atStatus = BannerMessage.Queued(res.message)) }
                is SmartSaveResult.Error -> _certState.update { it.copy(atStatus = BannerMessage.Err("❌ ${res.message}")) }
            }
        }
    }

    fun openCertPreview(payload: CertPreviewPayload) {
        _shellState.update { it.copy(certPreview = payload) }
    }

    fun closeCertPreview() {
        _shellState.update { it.copy(certPreview = null) }
    }

    fun setTwoUpPrint(twoUp: Boolean) {
        _shellState.update { it.copy(twoUpPrint = twoUp) }
    }

    // ===== Search, Batch Print, Profile & Cert History =====
    fun updateSearchFilters(
        name: String? = null, father: String? = null, surname: String? = null,
        dob: String? = null, cls: String? = null, div: String? = null, aadhar: String? = null
    ) {
        _searchProfileState.update {
            it.copy(
                nameQuery = name ?: it.nameQuery,
                fatherQuery = father ?: it.fatherQuery,
                surnameQuery = surname ?: it.surnameQuery,
                dobQuery = dob ?: it.dobQuery,
                classFilter = cls ?: it.classFilter,
                divFilter = div ?: it.divFilter,
                aadharQuery = aadhar ?: it.aadharQuery
            )
        }
        runLocalSearch()
    }

    fun runLocalSearch() {
        val sp = _searchProfileState.value
        val cached = storage.loadStudentCache()?.first
        if (cached == null) {
            bulkLoadAllStudents()
            return
        }
        val filtered = cached.filter { s ->
            val okName = sp.nameQuery.isBlank() || s.fullName.contains(sp.nameQuery.trim(), true) || s.regNo.contains(sp.nameQuery.trim(), true)
            val okFather = sp.fatherQuery.isBlank() || s.fullName.contains(sp.fatherQuery.trim(), true)
            val okSur = sp.surnameQuery.isBlank() || s.fullName.contains(sp.surnameQuery.trim(), true)
            val okDob = sp.dobQuery.isBlank() || s.dob.contains(sp.dobQuery.trim(), true)
            val okCls = sp.classFilter.isBlank() || s.iyatta == sp.classFilter
            val okDiv = sp.divFilter.isBlank() || s.tukdi == sp.divFilter
            val okAad = sp.aadharQuery.isBlank() || s.aadhar.contains(sp.aadharQuery.trim(), true)
            okName && okFather && okSur && okDob && okCls && okDiv && okAad
        }.take(120)
        _searchProfileState.update { it.copy(results = filtered) }
    }

    fun toggleBatchSelect(regNo: String) {
        _searchProfileState.update { sp ->
            val next = sp.selectedBatchRegNos.toMutableSet()
            if (!next.add(regNo)) next.remove(regNo)
            sp.copy(selectedBatchRegNos = next)
        }
    }

    fun clearBatchSelect() {
        _searchProfileState.update { it.copy(selectedBatchRegNos = emptySet()) }
    }

    fun getSelectedBatchStudents(): List<Student> {
        val sp = _searchProfileState.value
        val cached = storage.loadStudentCache()?.first.orEmpty()
        val pool = (sp.results + cached).associateBy { it.regNo }
        return sp.selectedBatchRegNos.mapNotNull { pool[it] }
    }

    fun openStudentProfile(regNoOrQuery: String) {
        val q = regNoOrQuery.trim()
        if (q.isEmpty()) return
        _searchProfileState.update {
            it.copy(
                profileRegNo = q,
                profileLoading = true,
                profileError = null,
                profilePhotoBitmap = null,
                auditLogs = emptyList(),
                auditActionLogs = emptyList(),
                auditPromotions = emptyList(),
                yearHistory = emptyList(),
                profileFees = emptyList(),
                profileDiary = emptyList(),
                profileAbsentDays = 0,
                profileEarlyLeaveCount = 0
            )
        }
        navigateTo(AppPage.PROFILE)
        viewModelScope.launch {
            repo.searchStudentDirect(q).onSuccess { stu ->
                if (stu == null) {
                    _searchProfileState.update { it.copy(profileLoading = false, profileStudent = null, profileError = "विद्यार्थी सापडला नाही.") }
                    return@onSuccess
                }
                storage.addRecentSearch(stu)
                _searchProfileState.update { it.copy(profileLoading = false, profileStudent = stu, recentSearches = storage.loadRecentSearches()) }
                val bmp = repo.fetchPhotoBitmap(stu.regNo.ifBlank { stu.studentId })
                if (bmp != null) {
                    _searchProfileState.update { it.copy(profilePhotoBitmap = bmp) }
                }
                val role = _shellState.value.session?.role
                if (role == UserRole.SUPER || role == UserRole.MASTER) {
                    val audR = repo.request(mapOf("action" to "getStudentAudit", "regNo" to stu.regNo))
                    if (audR.isOk) {
                        val bundle = repo.parseStudentAudit(audR)
                        _searchProfileState.update { st ->
                            st.copy(
                                auditHasSheet = bundle.hasAuditSheet,
                                auditLogs = bundle.changes.map { c -> ProfileAuditItem(c.time, c.user, c.role, c.field, c.oldVal, c.newVal) },
                                auditActionLogs = bundle.logs.map { l -> ProfileActionLogItem(l.time, l.user, l.role, l.action) },
                                auditPromotions = bundle.promotions.map { p -> ProfilePromoLogItem(p.time, p.from, p.to, p.by, p.undone) }
                            )
                        }
                    }
                }
                if (_shellState.value.session?.canSeeMoney == true) {
                    val yrR = repo.request(mapOf("action" to "getFees", "regNo" to stu.regNo, "yearsOnly" to "1"))
                    val years = (yrR.raw["years"] as? JsonArray)?.mapNotNull { el ->
                        when (el) {
                            is JsonObject -> el.str("year")
                            else -> el.toString().trim('"')
                        }.takeIf { it.isNotBlank() }
                    }.orEmpty()
                    val curYear = yrR.raw.str("currentYear").ifBlank { stu.acYear.ifBlank { storage.getAcYear() } }
                    val mergedYears = (listOf(curYear) + years).filter { it.isNotBlank() }.distinct()
                    _searchProfileState.update { it.copy(profileFeeYears = mergedYears, selectedProfileFeeYear = curYear) }
                    loadProfileFeesForYear(stu.regNo, curYear)
                }
                val tr = repo.request(mapOf("action" to "getTransport", "regNo" to stu.regNo))
                if (tr.isOk) _searchProfileState.update { it.copy(profileTransport = repo.parseTransportList(tr).firstOrNull()) }
                val dyR = repo.request(mapOf("action" to "getDiary", "regNo" to stu.regNo))
                if (dyR.isOk) _searchProfileState.update { it.copy(profileDiary = repo.parseDiaryList(dyR)) }
                val activeAcYear = stu.acYear.ifBlank { storage.getAcYear() }
                val histCore = repo.request(mapOf("action" to "getStudentYearHistory", "regNo" to stu.regNo, "year" to activeAcYear, "part" to "core"))
                val histEl = repo.request(mapOf("action" to "getStudentYearHistory", "regNo" to stu.regNo, "year" to activeAcYear, "part" to "el"))
                if (histCore.isOk || histEl.isOk) {
                    val attObj = histCore.raw["attendance"] as? JsonObject
                    val elArr = histEl.raw["records"] as? JsonArray
                    _searchProfileState.update {
                        it.copy(
                            profileAbsentDays = attObj?.intVal("absentDays") ?: 0,
                            profileEarlyLeaveCount = elArr?.size ?: 0
                        )
                    }
                }
            }.onFailure { err ->
                _searchProfileState.update { it.copy(profileLoading = false, profileError = err.message) }
            }
        }
    }

    fun loadProfileFeesForYear(regNo: String, acYear: String) {
        if (regNo.isBlank()) return
        _searchProfileState.update { it.copy(selectedProfileFeeYear = acYear) }
        viewModelScope.launch {
            val fr = repo.request(mapOf("action" to "getFees", "regNo" to regNo, "acYear" to acYear))
            if (fr.isOk) {
                val list = repo.parseFeesList(fr)
                _searchProfileState.update {
                    it.copy(
                        profileFees = list,
                        profileFeeOpening = fr.raw.doubleVal("openingDue"),
                        profileFeeYearTotal = fr.raw.doubleVal("yearTotalFee", fr.raw.doubleVal("totalFeePerStudent", 1000.0)),
                        profileFeeBalance = fr.raw.doubleVal("balance"),
                        profileFeeAssumed = fr.raw["totalFeeAssumed"]?.toString()?.contains("true") == true,
                        profileFeeArchived = fr.raw["archived"]?.toString()?.contains("true") == true
                    )
                }
            } else {
                showToast(BannerMessage.Err(fr.message))
            }
        }
    }

    fun loadStudentAllYearHistory(regNo: String) {
        viewModelScope.launch {
            _searchProfileState.update { it.copy(yearHistoryLoading = true, yearHistory = emptyList()) }
            val metaR = repo.request(mapOf("action" to "getStudentYearHistory", "regNo" to regNo, "meta" to "1"))
            val years = (metaR.raw["years"] as? JsonArray)?.map { it.toString().trim('"') }.orEmpty()
            val blocks = mutableListOf<ProfileYearBlock>()
            for (y in years) {
                val coreR = repo.request(mapOf("action" to "getStudentYearHistory", "regNo" to regNo, "year" to y, "part" to "core"))
                val elR = repo.request(mapOf("action" to "getStudentYearHistory", "regNo" to regNo, "year" to y, "part" to "el"))
                if (coreR.isOk) {
                    val cls = coreR.raw["cls"] as? JsonObject
                    val att = coreR.raw["attendance"] as? JsonObject
                    val fees = coreR.raw["fees"] as? JsonObject
                    val dy = coreR.raw["diary"] as? JsonArray
                    val sch = coreR.raw["scholarship"] as? JsonArray
                    val tr = coreR.raw["transport"] as? JsonObject
                    val elRecs = elR.raw["records"] as? JsonArray
                    blocks.add(
                        ProfileYearBlock(
                            year = y,
                            archived = coreR.raw["archived"]?.toString()?.contains("true") == true,
                            iyatta = cls?.str("iyatta").orEmpty(),
                            tukdi = cls?.str("tukdi").orEmpty(),
                            roll = cls?.str("roll").orEmpty(),
                            absentDays = att?.intVal("absentDays") ?: 0,
                            markedDays = att?.str("markedDays")?.toIntOrNull(),
                            pct = att?.str("pct")?.toDoubleOrNull(),
                            feeTotal = fees?.doubleVal("total") ?: 0.0,
                            feeOpening = fees?.doubleVal("opening") ?: 0.0,
                            feePaid = fees?.doubleVal("paid") ?: 0.0,
                            feeBalance = fees?.doubleVal("balance") ?: 0.0,
                            diaryCount = dy?.size ?: 0,
                            elCount = elRecs?.size ?: 0,
                            schCount = sch?.size ?: 0,
                            transportSummary = tr?.let { "${it.str("nativeVillage")} | ${it.str("travelMode")}" } ?: "नोंद नाही",
                            detailsText = "हजेरी: ${att?.str("pct") ?: "—"}% | फी बाकी: ${MarathiUtils.fmtRupees(fees?.doubleVal("balance") ?: 0.0)}"
                        )
                    )
                }
            }
            _searchProfileState.update { it.copy(yearHistoryLoading = false, yearHistory = blocks) }
        }
    }

    fun searchCertHistory(regNo: String) {
        _searchProfileState.update { it.copy(certHistSearchQuery = regNo, certHistLoading = true) }
        viewModelScope.launch {
            val r = repo.request(mapOf("action" to "getStudentHistory", "regNo" to regNo.trim()))
            _searchProfileState.update {
                it.copy(
                    certHistLoading = false,
                    certHistory = if (r.isOk) repo.parseCertHistory(r) else emptyList()
                )
            }
        }
    }

    fun loadAllCertificatesExport() {
        _searchProfileState.update { it.copy(certHistLoading = true) }
        viewModelScope.launch {
            val r = repo.request(mapOf("action" to "getAllCertificates"))
            _searchProfileState.update {
                it.copy(
                    certHistLoading = false,
                    allCerts = if (r.isOk) repo.parseCertHistory(r) else emptyList()
                )
            }
        }
    }
}
