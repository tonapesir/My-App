package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ClientErrorLogItem
import com.example.data.local.LocalStorage
import com.example.data.repository.SchoolRepository
import com.example.domain.model.AppPage
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
import com.example.ui.components.ConfigSettingsDialog
import com.example.ui.screens.AppShell
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.AttendanceAnalyticsScreen
import com.example.ui.screens.AttendanceCertScreen
import com.example.ui.screens.BonafideScreen
import com.example.ui.screens.CertHistoryScreen
import com.example.ui.screens.ClassInfoScreen
import com.example.ui.screens.ClassTeacherListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DataQcScreen
import com.example.ui.screens.EarlyLeaveScreen
import com.example.ui.screens.HelpManualScreen
import com.example.ui.screens.LcScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.ReportBuilderScreen
import com.example.ui.screens.ScholarshipScreen
import com.example.ui.screens.SearchStudentsScreen
import com.example.ui.screens.StatsReportScreen
import com.example.ui.screens.StudentEntryScreen
import com.example.ui.screens.StudentProfileScreen
import com.example.ui.screens.SuperAdminScreen
import com.example.ui.screens.TeacherWorkspaceScreen
import com.example.ui.screens.UserManagementScreen
import com.example.ui.theme.SGSSchoolTheme
import com.example.ui.viewmodel.BannerMessage
import com.example.ui.viewmodel.MainViewModel
import com.example.util.BiometricAuthHelper

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                LocalStorage(applicationContext).appendClientError(
                    ClientErrorLogItem(
                        v = SchoolRepository.APP_VERSION,
                        pg = "MainActivity",
                        ua = "Android/${android.os.Build.VERSION.RELEASE}",
                        t = throwable.javaClass.simpleName.take(20),
                        m = (throwable.message ?: "Uncaught exception").take(300),
                        s = throwable.stackTraceToString().take(500),
                        c = 1
                    )
                )
            } catch (_: Exception) {
            }
            prevHandler?.uncaughtException(thread, throwable)
        }
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val shell by vm.shellState.collectAsStateWithLifecycle()
            SGSSchoolTheme(darkTheme = shell.isDarkTheme) {
                SgsRootApp(vm)
            }
        }
    }
}

@Composable
private fun SgsRootApp(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val bioSupported = remember(context) { BiometricAuthHelper.canAuthenticate(context) }

    val shell by vm.shellState.collectAsStateWithLifecycle()
    val login by vm.loginState.collectAsStateWithLifecycle()
    val studentEntry by vm.studentEntryState.collectAsStateWithLifecycle()
    val cert by vm.certState.collectAsStateWithLifecycle()
    val searchProfile by vm.searchProfileState.collectAsStateWithLifecycle()

    val dash by vm.ops.dashState.collectAsStateWithLifecycle()
    val teacher by vm.ops.teacherState.collectAsStateWithLifecycle()
    val earlyLeave by vm.ops.earlyLeaveState.collectAsStateWithLifecycle()
    val scholarship by vm.ops.scholarshipState.collectAsStateWithLifecycle()
    val maintStatus by vm.ops.maintenanceStatus.collectAsStateWithLifecycle()
    val classInfoStudents by vm.ops.classInfoStudents.collectAsStateWithLifecycle()
    val classList by vm.ops.classList.collectAsStateWithLifecycle()
    val classTeachers by vm.ops.classTeachers.collectAsStateWithLifecycle()

    val report by vm.admin.reportState.collectAsStateWithLifecycle()
    val stats by vm.admin.statsState.collectAsStateWithLifecycle()
    val attAnaMonth by vm.admin.attAnaMonth.collectAsStateWithLifecycle()
    val attAnaOverview by vm.admin.attAnaOverview.collectAsStateWithLifecycle()
    val attAnaStudents by vm.admin.attAnaStudents.collectAsStateWithLifecycle()
    val attAnaStreaks by vm.admin.attAnaStreaks.collectAsStateWithLifecycle()
    val attAnaSummary by vm.admin.attAnaSummary.collectAsStateWithLifecycle()
    val adminState by vm.admin.adminState.collectAsStateWithLifecycle()

    val promptBiometricUnlock: () -> Unit = {
        if (activity != null && bioSupported) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = "SGS हायस्कूल लॉक उघडा",
                subtitle = "अ‍ॅप वापरण्यासाठी फिंगरप्रिंट किंवा स्क्रीन लॉक पडताळा",
                onSuccess = vm::unlockBiometricLock,
                onError = { msg -> vm.showToast(BannerMessage.Err(msg)) }
            )
        } else {
            vm.unlockBiometricLock()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> vm.onAppBackgrounded()
                Lifecycle.Event.ON_START -> vm.onAppForegrounded()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(shell.session != null, shell.isBiometricLocked, shell.isInactivityLocked) {
        if (shell.session != null && (shell.isBiometricLocked || shell.isInactivityLocked) && bioSupported) {
            promptBiometricUnlock()
        }
    }

    val session = shell.session
    if (session == null) {
        LoginScreen(
            state = login,
            isDarkTheme = shell.isDarkTheme,
            biometricSupported = bioSupported,
            onToggleTheme = vm::toggleDarkTheme,
            onBiometricLogin = {
                if (activity != null && bioSupported) {
                    BiometricAuthHelper.authenticate(
                        activity = activity,
                        title = "बायोमेट्रिक Login",
                        subtitle = login.biometricQuickLabel.ifBlank { "फिंगरप्रिंटने त्वरित Login करा" },
                        onSuccess = vm::loginWithBiometric,
                        onError = { msg -> vm.showToast(BannerMessage.Err(msg)) }
                    )
                }
            },
            onUsernameChange = { vm.updateLoginFields(username = it) },
            onPasswordChange = { vm.updateLoginFields(password = it) },
            onRememberOfflineChange = { vm.updateLoginFields(rememberOffline = it) },
            onPreConnect = vm::preConnect,
            onLogin = vm::attemptLogin,
            onOpenSettings = { vm.setConfigDialogVisible(true) }
        )
        if (shell.showConfigDialog) {
            ConfigSettingsDialog(
                url = shell.sheetsUrl,
                cacheCount = shell.cacheCount,
                isBulkLoading = shell.isBulkLoading,
                bulkMessage = shell.bulkLoadMessage,
                testStatus = shell.testConnectionStatus,
                isDarkTheme = shell.isDarkTheme,
                biometricEnabled = shell.biometricEnabled,
                biometricSupported = bioSupported,
                remindersEnabled = shell.remindersEnabled,
                onToggleTheme = vm::toggleDarkTheme,
                onToggleBiometric = vm::setBiometricEnabled,
                onToggleReminders = vm::setRemindersEnabled,
                autoLockMinutes = shell.autoLockMinutes,
                onAutoLockChange = vm::setAutoLockMinutes,
                onUrlChange = vm::updateSheetsUrl,
                onTest = vm::testConnection,
                onBulkLoad = vm::bulkLoadAllStudents,
                onClearCache = vm::clearStudentCache,
                onDismiss = { vm.setConfigDialogVisible(false) }
            )
        }
        return
    }

    val role = session.role
    val canEditStudents = role == UserRole.SUPER || role == UserRole.MASTER || role == UserRole.DEO
    val canSaveCerts = role == UserRole.SUPER || role == UserRole.MASTER || role == UserRole.CERT

    AppShell(
        shell = shell,
        onSelectPage = { vm.navigateTo(it) },
        onSelectTeacherTab = vm::selectTeacherTab,
        onNavigateBack = vm::navigateHomeForRole,
        onOpenConfig = { vm.setConfigDialogVisible(true) },
        onCloseConfig = { vm.setConfigDialogVisible(false) },
        onUrlChange = vm::updateSheetsUrl,
        onTestConnection = vm::testConnection,
        onBulkLoad = vm::bulkLoadAllStudents,
        onClearCache = vm::clearStudentCache,
        onOpenPendingModal = { vm.setPendingModalVisible(true) },
        onClosePendingModal = { vm.setPendingModalVisible(false) },
        onFlushPending = vm::flushPendingSaves,
        onExportPendingJson = {
            val json = vm.exportPendingSavesJson()
            val file = PdfAndExportUtils.writeTextFile(
                context,
                "SGS_Pending_Saves_${MarathiUtils.todayIso()}.json",
                json
            )
            PdfAndExportUtils.shareFile(context, file, "application/json", "Pending Saves JSON Export")
        },
        onRemovePending = vm::removePendingSave,
        onClearAllPending = vm::clearAllPendingSaves,
        onDismissToast = { vm.showToast(null) },
        onToggleTwoUp = vm::setTwoUpPrint,
        onCloseCertPreview = vm::closeCertPreview,
        onRefreshNetwork = vm::refreshConnectivityAndQueue,
        onToggleTheme = vm::toggleDarkTheme,
        biometricSupported = bioSupported,
        onToggleBiometric = vm::setBiometricEnabled,
        onToggleReminders = vm::setRemindersEnabled,
        onPromptBiometricUnlock = promptBiometricUnlock,
        onOpenQrScanner = { vm.setQrScannerVisible(true) },
        onDismissQrScanner = { vm.setQrScannerVisible(false) },
        onOpenQuickActions = { vm.setQuickActionsVisible(true) },
        onDismissQuickActions = { vm.setQuickActionsVisible(false) },
        onOpenHolidayDialog = { vm.setHolidayDialogVisible(true) },
        onDismissHolidayDialog = { vm.setHolidayDialogVisible(false) },
        onAddHoliday = vm::addHoliday,
        onDeleteHoliday = vm::deleteHoliday,
        onSeedDefaultHolidays = vm::seedDefaultHolidays,
        onOpenBirthdaysDialog = vm::openBirthdaysDialog,
        onDismissBirthdaysDialog = vm::closeBirthdaysDialog,
        onRefreshQuickBirthdays = vm::refreshQuickBirthdays,
        findStudentInCache = vm::findStudentInCache,
        onQrOpenProfile = vm::openStudentProfile,
        onQrOpenEarlyLeave = vm::openQrInEarlyLeave,
        onQrOpenSearch = vm::openQrInSearch,
        onTogglePinQuickKey = vm::togglePinQuickKey,
        onAutoLockChange = vm::setAutoLockMinutes,
        onOpenTemplatesDialog = { vm.setTemplatesDialogVisible(true) },
        onDismissTemplatesDialog = { vm.setTemplatesDialogVisible(false) },
        onSaveMessageTemplate = vm::saveMessageTemplate,
        onDeleteMessageTemplate = vm::deleteMessageTemplate,
        onResetDefaultMessageTemplates = vm::resetDefaultMessageTemplates,
        onLogout = vm::logout
    ) { page ->
        when (page) {
            AppPage.DASHBOARD -> DashboardScreen(
                role = role,
                state = dash,
                onRefresh = vm.ops::loadDashboard,
                onNavigate = { vm.navigateTo(it) },
                onDailyDateChange = vm.ops::updateDailyDate,
                onLoadDailySummary = { vm.ops.loadDailySummary() },
                onNoticeDraftChange = vm.ops::updateNoticeDraft,
                onSendNotice = vm.ops::sendNotice,
                onSyncAcYear = vm.ops::syncStudentsAcYear,
                holidays = shell.holidays,
                onOpenQrScanner = { vm.setQrScannerVisible(true) },
                onOpenHolidays = { vm.setHolidayDialogVisible(true) },
                onOpenBirthdays = vm::openBirthdaysDialog
            )

            AppPage.TEACHER -> TeacherWorkspaceScreen(
                session = session,
                activeTab = shell.teacherTab,
                state = teacher,
                ops = vm.ops,
                verifyBaseUrl = shell.sheetsUrl,
                acYear = vm.currentAcYear(),
                onSelectTab = vm::selectTeacherTab,
                onOpenStudentProfile = vm::openStudentProfile,
                holidays = shell.holidays,
                onOpenPage = { vm.navigateTo(it) },
                onOpenQrScanner = { vm.setQrScannerVisible(true) },
                onOpenHolidays = { vm.setHolidayDialogVisible(true) },
                templates = shell.messageTemplates,
                onOpenManageTemplates = { vm.setTemplatesDialogVisible(true) }
            )

            AppPage.STUDENT -> StudentEntryScreen(
                state = studentEntry,
                canEdit = canEditStudents,
                onSearchQueryChange = vm::updateStudentSearchQuery,
                onSearch = vm::searchStudentForEntry,
                onUpdateForm = vm::updateStudentForm,
                onUploadPhoto = vm::uploadStudentPhoto,
                onTestPhotoDrive = vm::testPhotoDrive,
                onSaveNew = { vm.saveStudent(isUpdate = false) },
                onUpdateExisting = { vm.saveStudent(isUpdate = true) },
                onClearForm = vm::clearStudentForm,
                onDismissSavedDialog = vm::dismissStudentSavedDialog,
                onQuickOpenModule = { st, targetPage ->
                    vm.dismissStudentSavedDialog()
                    vm.fillStudentIntoModule(st, targetPage)
                }
            )

            AppPage.LC -> LcScreen(
                state = cert,
                canEdit = role == UserRole.SUPER || role == UserRole.MASTER,
                onSearchChange = vm::updateLcSearch,
                onSearch = { vm.searchForModule(cert.lcSearchQuery, AppPage.LC) },
                onUpdateLc = vm::updateLcForm,
                onSaveLc = vm::saveLC,
                onPreview = vm::openCertPreview
            )

            AppPage.BONAFIDE -> BonafideScreen(
                state = cert,
                canSave = canSaveCerts,
                onSearchChange = vm::updateBfSearch,
                onSearch = { vm.searchForModule(cert.bfSearchQuery, AppPage.BONAFIDE) },
                onUpdateBf = vm::updateBfForm,
                onSaveBf = vm::saveBonafide,
                onPreview = vm::openCertPreview
            )

            AppPage.ATTENDANCE -> AttendanceCertScreen(
                state = cert,
                canSave = canSaveCerts,
                onSearchChange = vm::updateAtSearch,
                onSearch = { vm.searchForModule(cert.atSearchQuery, AppPage.ATTENDANCE) },
                onUpdateAt = vm::updateAtForm,
                onSaveAt = vm::saveAttendanceCert,
                onPreview = vm::openCertPreview
            )

            AppPage.SEARCH -> SearchStudentsScreen(
                role = role,
                state = searchProfile,
                verifyBaseUrl = shell.sheetsUrl,
                acYear = vm.currentAcYear(),
                selectedBatchStudents = vm.getSelectedBatchStudents(),
                onFilterChange = vm::updateSearchFilters,
                onBulkLoad = vm::bulkLoadAllStudents,
                onOpenStudentIn = vm::fillStudentIntoModule,
                onToggleBatch = vm::toggleBatchSelect,
                onClearBatch = vm::clearBatchSelect,
                onOpenQrScanner = { vm.setQrScannerVisible(true) }
            )

            AppPage.HISTORY -> CertHistoryScreen(
                state = searchProfile,
                onSearchCerts = vm::searchCertHistory,
                onLoadAllCerts = vm::loadAllCertificatesExport
            )

            AppPage.PROFILE -> StudentProfileScreen(
                role = role,
                state = searchProfile,
                verifyBaseUrl = shell.sheetsUrl,
                acYear = vm.currentAcYear(),
                onSearchProfile = vm::openStudentProfile,
                onSelectFeeYear = { year ->
                    val reg = searchProfile.profileStudent?.regNo ?: searchProfile.profileRegNo
                    vm.loadProfileFeesForYear(reg, year)
                },
                onLoadAllYearsHistory = vm::loadStudentAllYearHistory
            )

            AppPage.USERS -> UserManagementScreen(
                role = role,
                state = adminState,
                admin = vm.admin
            )

            AppPage.STATS -> StatsReportScreen(
                state = stats,
                onRefresh = vm.admin::loadStatsReport
            )

            AppPage.MAINTENANCE -> MaintenanceScreen(
                role = role,
                status = maintStatus,
                ops = vm.ops
            )

            AppPage.CLASSINFO -> ClassInfoScreen(
                classes = classList,
                students = classInfoStudents,
                verifyBaseUrl = shell.sheetsUrl,
                acYear = vm.currentAcYear(),
                templates = shell.messageTemplates,
                onOpenManageTemplates = { vm.setTemplatesDialogVisible(true) },
                onLoadClass = vm.ops::loadClassInfoRoster,
                onOpenProfile = vm::openStudentProfile
            )

            AppPage.REPORT -> ReportBuilderScreen(
                role = role,
                state = report,
                admin = vm.admin
            )

            AppPage.SUPERADMIN -> SuperAdminScreen(
                state = adminState,
                admin = vm.admin,
                templates = shell.messageTemplates
            )

            AppPage.ATTANALYTICS -> AttendanceAnalyticsScreen(
                classes = classList,
                month = attAnaMonth,
                overview = attAnaOverview,
                students = attAnaStudents,
                streaks = attAnaStreaks,
                summary = attAnaSummary,
                admin = vm.admin
            )

            AppPage.EARLYLEAVE -> EarlyLeaveScreen(
                role = role,
                state = earlyLeave,
                ops = vm.ops,
                templates = shell.messageTemplates,
                onOpenProfile = vm::openStudentProfile,
                onOpenQrScanner = { vm.setQrScannerVisible(true) }
            )

            AppPage.SCHOLARSHIP -> ScholarshipScreen(
                role = role,
                state = scholarship,
                ops = vm.ops
            )

            AppPage.TEACHERLIST -> ClassTeacherListScreen(
                teachers = classTeachers
            )

            AppPage.ARCHIVE -> ArchiveScreen(
                role = role,
                state = adminState,
                admin = vm.admin
            )

            AppPage.DATAQC -> DataQcScreen(
                state = adminState,
                admin = vm.admin,
                onNavigate = { vm.navigateTo(it) },
                onOpenProfile = vm::openStudentProfile
            )

            AppPage.HELP -> HelpManualScreen(role = role)
        }
    }
}
