package com.example.ui.viewmodel

import android.graphics.Bitmap
import com.example.domain.model.AcYearInfo
import com.example.domain.model.AppPage
import com.example.domain.model.ArchiveYearItem
import com.example.domain.model.AttendanceCertFormData
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.BirthdayStudent
import com.example.domain.model.BonafideFormData
import com.example.domain.model.CertificateHistoryItem
import com.example.domain.model.ClassDivisionPair
import com.example.domain.model.ClassTeacherItem
import com.example.domain.model.DashboardStats
import com.example.domain.model.DiaryEntry
import com.example.domain.model.EarlyLeaveRecord
import com.example.domain.model.FeeRecord
import com.example.domain.model.HolidayItem
import com.example.domain.model.LcFormData
import com.example.domain.model.MessageTemplate
import com.example.domain.model.NoticeItem
import com.example.domain.model.PendingSaveItem
import com.example.domain.model.ReportClassGroup
import com.example.domain.model.ReportFieldOption
import com.example.domain.model.ScholarshipRecord
import com.example.domain.model.Student
import com.example.domain.model.TeacherTab
import com.example.domain.model.TransportInfo
import com.example.domain.model.UserAccount
import com.example.domain.model.UserSession
import com.example.domain.util.MarathiUtils
import kotlinx.serialization.json.JsonObject

sealed class BannerMessage {
    data class Ok(val text: String) : BannerMessage()
    data class Err(val text: String) : BannerMessage()
    data class Info(val text: String) : BannerMessage()
    data class Queued(val text: String) : BannerMessage()
}

sealed class CertPreviewPayload {
    data class LcPreview(val data: LcFormData) : CertPreviewPayload()
    data class BonafidePreview(val data: BonafideFormData) : CertPreviewPayload()
    data class AttendancePreview(val data: AttendanceCertFormData) : CertPreviewPayload()
}

data class ShellUiState(
    val session: UserSession? = null,
    val currentPage: AppPage = AppPage.DASHBOARD,
    val teacherTab: TeacherTab = TeacherTab.DASH,
    val sheetsUrl: String = "",
    val isOnline: Boolean = true,
    val isOfflineBanner: Boolean = false,
    val staleDataNotice: String? = null,
    val pendingSaves: List<PendingSaveItem> = emptyList(),
    val showPendingModal: Boolean = false,
    val showConfigDialog: Boolean = false,
    val testConnectionStatus: BannerMessage? = null,
    val cacheCount: Int = 0,
    val isBulkLoading: Boolean = false,
    val bulkLoadMessage: String? = null,
    val certPreview: CertPreviewPayload? = null,
    val twoUpPrint: Boolean = false,
    val toastMessage: BannerMessage? = null,
    val isDarkTheme: Boolean = true,
    val biometricEnabled: Boolean = false,
    val isBiometricLocked: Boolean = false,
    val remindersEnabled: Boolean = true,
    val holidays: List<HolidayItem> = emptyList(),
    val showQrScanner: Boolean = false,
    val showQuickActionsDialog: Boolean = false,
    val showHolidayDialog: Boolean = false,
    val showBirthdaysDialog: Boolean = false,
    val quickBirthdays: List<BirthdayStudent> = emptyList(),
    val pinnedQuickKeys: List<String> = emptyList(),
    val messageTemplates: List<MessageTemplate> = emptyList(),
    val showTemplatesDialog: Boolean = false,
    val autoLockMinutes: Int = 0,
    val isInactivityLocked: Boolean = false
)

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberOffline: Boolean = true,
    val isConnecting: Boolean = false,
    val preConnectStatus: BannerMessage? = null,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val biometricQuickAvailable: Boolean = false,
    val biometricQuickLabel: String = ""
)

data class PendingAttClass(
    val iyatta: String,
    val tukdi: String,
    val teacher: String,
    val mobile: String,
    val username: String = "",
    val time: String = ""
)

data class DailySummaryData(
    val date: String = "",
    val dayType: String = "",
    val message: String = "",
    val present: Int = 0,
    val total: Int = 0,
    val absent: Int = 0,
    val pct: Double? = null,
    val pendingClasses: List<PendingAttClass> = emptyList(),
    val noTeacherClasses: List<ClassDivisionPair> = emptyList(),
    val classRows: List<DailyAttClassRow> = emptyList(),
    val elTotal: Int = 0,
    val elGone: Int = 0,
    val elPermitted: Int = 0,
    val elNotGone: Int = 0,
    val elOther: Int = 0,
    val feesTotal: Double = 0.0,
    val feesCount: Int = 0,
    val feesByClass: List<Pair<String, Double>> = emptyList(),
    val noticesCount: Int = 0,
    val noticeTitles: List<String> = emptyList(),
    val changesTotal: Int = 0,
    val changesStudents: Int = 0,
    val messageText: String = "",
    val raw: JsonObject? = null
)

data class DailyAttClassRow(
    val iyatta: String,
    val tukdi: String,
    val total: Int,
    val present: Int,
    val absent: Int,
    val pct: Double?
)

data class DashUiState(
    val loading: Boolean = false,
    val stats: DashboardStats = DashboardStats(),
    val acYearInfo: AcYearInfo = AcYearInfo(),
    val yearAdmissions: List<Pair<String, Int>> = emptyList(),
    val religionCounts: List<Pair<String, Int>> = emptyList(),
    val lcReasonCounts: List<Pair<String, Int>> = emptyList(),
    val pendingAtt: List<PendingAttClass> = emptyList(),
    val doneAtt: List<PendingAttClass> = emptyList(),
    val unassignedAtt: List<ClassDivisionPair> = emptyList(),
    val attPendingIsSunday: Boolean = false,
    val attPendingTotalExpected: Int = 0,
    val sheetCapPct: Double = 0.0,
    val sheetCapLevel: String = "ok",
    val birthdays: List<BirthdayStudent> = emptyList(),
    val notices: List<NoticeItem> = emptyList(),
    val noticeTitle: String = "",
    val noticeMessage: String = "",
    val noticeTarget: String = "ALL",
    val noticeStatus: BannerMessage? = null,
    val dailyDate: String = "",
    val dailyLoading: Boolean = false,
    val dailySummary: DailySummaryData? = null,
    val dailyError: String? = null,
    val syncStatus: BannerMessage? = null
)

data class StudentEntryUiState(
    val form: Student = Student(),
    val searchQuery: String = "",
    val isExistingRow: Boolean = false,
    val isLoading: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val status: BannerMessage? = null,
    val photoStatus: BannerMessage? = null,
    val savedDialogStudent: Student? = null
)

data class CertFormsUiState(
    val lcSearchQuery: String = "",
    val lc: LcFormData = LcFormData(),
    val lcStatus: BannerMessage? = null,
    val bfSearchQuery: String = "",
    val bf: BonafideFormData = BonafideFormData(),
    val bfStatus: BannerMessage? = null,
    val atSearchQuery: String = "",
    val at: AttendanceCertFormData = AttendanceCertFormData(),
    val atStatus: BannerMessage? = null
)

data class ProfileAuditItem(
    val timestamp: String,
    val user: String,
    val role: String,
    val field: String,
    val oldVal: String,
    val newVal: String
)

data class ProfileActionLogItem(
    val timestamp: String,
    val user: String,
    val role: String,
    val action: String
)

data class ProfilePromoLogItem(
    val timestamp: String,
    val from: String,
    val to: String,
    val by: String,
    val undone: Boolean = false
)

data class ProfileYearBlock(
    val year: String,
    val archived: Boolean,
    val iyatta: String,
    val tukdi: String,
    val roll: String,
    val absentDays: Int,
    val markedDays: Int?,
    val pct: Double?,
    val feeTotal: Double,
    val feeOpening: Double,
    val feePaid: Double,
    val feeBalance: Double,
    val diaryCount: Int,
    val elCount: Int,
    val schCount: Int,
    val transportSummary: String,
    val detailsText: String
)

data class SearchAndProfileUiState(
    val nameQuery: String = "",
    val fatherQuery: String = "",
    val surnameQuery: String = "",
    val dobQuery: String = "",
    val classFilter: String = "",
    val divFilter: String = "",
    val aadharQuery: String = "",
    val results: List<Student> = emptyList(),
    val recentSearches: List<Student> = emptyList(),
    val selectedBatchRegNos: Set<String> = emptySet(),
    val profileRegNo: String = "",
    val profileStudent: Student? = null,
    val profilePhotoBitmap: Bitmap? = null,
    val profileLoading: Boolean = false,
    val profileError: String? = null,
    val auditHasSheet: Boolean = true,
    val auditLogs: List<ProfileAuditItem> = emptyList(),
    val auditActionLogs: List<ProfileActionLogItem> = emptyList(),
    val auditPromotions: List<ProfilePromoLogItem> = emptyList(),
    val yearHistory: List<ProfileYearBlock> = emptyList(),
    val yearHistoryLoading: Boolean = false,
    val profileFeeYears: List<String> = emptyList(),
    val selectedProfileFeeYear: String = "",
    val profileFeeOpening: Double = 0.0,
    val profileFeeYearTotal: Double = 0.0,
    val profileFeeBalance: Double = 0.0,
    val profileFeeAssumed: Boolean = false,
    val profileFeeArchived: Boolean = false,
    val profileFees: List<FeeRecord> = emptyList(),
    val profileTransport: TransportInfo? = null,
    val profileDiary: List<DiaryEntry> = emptyList(),
    val profileAbsentDays: Int = 0,
    val profileEarlyLeaveCount: Int = 0,
    val certHistSearchQuery: String = "",
    val certHistory: List<CertificateHistoryItem> = emptyList(),
    val allCerts: List<CertificateHistoryItem> = emptyList(),
    val certHistLoading: Boolean = false
)

data class TeacherAttStreakStudent(
    val rollNo: String,
    val fullName: String,
    val tukdi: String,
    val workingDays: Int,
    val absent: Int,
    val pct: Double?,
    val maxRun: Int,
    val mobile: String,
    val from: String,
    val to: String
)

data class TeacherUiState(
    val loading: Boolean = false,
    val roster: List<Student> = emptyList(),
    val rosterSearch: String = "",
    val attMarkedToday: Boolean = false,
    val todayAbsentCount: Int = 0,
    val classFeesTotal: Double = 0.0,
    val attDate: String = "",
    val absentReasons: Map<String, String> = emptyMap(),
    val attSavedList: List<AttendanceRecord> = emptyList(),
    val attStatus: BannerMessage? = null,
    val reportFrom: String = "",
    val reportTo: String = "",
    val reportRecords: List<AttendanceRecord> = emptyList(),
    val selectedDiaryRegNo: String = "",
    val diaryEntries: List<DiaryEntry> = emptyList(),
    val diaryFull: Boolean = false,
    val diaryArchive: Boolean = false,
    val transportInfo: TransportInfo = TransportInfo(),
    val transportStatus: BannerMessage? = null,
    val diaryType: String = "positive",
    val diaryRemark: String = "",
    val diaryStatus: BannerMessage? = null,
    val feesList: List<FeeRecord> = emptyList(),
    val totalFeePerStudent: Double = 1000.0,
    val selectedFeeRegNo: String = "",
    val studentFeeHistory: List<FeeRecord> = emptyList(),
    val feeAmountInput: String = "",
    val feeStatus: BannerMessage? = null,
    val notices: List<NoticeItem> = emptyList(),
    val anaMonth: String = "",
    val anaMinRun: Int = 3,
    val anaWorkingDays: Int = 0,
    val anaAvgPct: Double? = null,
    val anaStudents: List<TeacherAttStreakStudent> = emptyList(),
    val anaStreaks: List<TeacherAttStreakStudent> = emptyList(),
    val anaLoading: Boolean = false,
    val maintSearchRegNo: String = "",
    val maintStudent: Student? = null,
    val maintNewDiv: String = "अ",
    val maintStatus: BannerMessage? = null
)

data class EarlyLeaveUiState(
    val loading: Boolean = false,
    val date: String = "",
    val selectedClassKey: String = "ALL|",
    val classes: List<ClassDivisionPair> = emptyList(),
    val roster: List<Student> = emptyList(),
    val absentRegNos: Set<String> = emptySet(),
    val records: List<EarlyLeaveRecord> = emptyList(),
    val draftChanges: Map<String, Pair<String, Int>> = emptyMap(),
    val filter: String = "all",
    val peonRecords: List<EarlyLeaveRecord> = emptyList(),
    val archived: Boolean = false,
    val stale: Boolean = false,
    val status: BannerMessage? = null
)

data class ScholarshipUiState(
    val loading: Boolean = false,
    val selectedClassKey: String = "ALL|",
    val classes: List<ClassDivisionPair> = emptyList(),
    val roster: List<Student> = emptyList(),
    val records: List<ScholarshipRecord> = emptyList(),
    val filterStatus: String = "ALL",
    val editId: String = "",
    val formRegNo: String = "",
    val formName: String = "",
    val formScheme: String = "",
    val formAmount: String = "",
    val formStatus: String = "प्रलंबित",
    val formRemark: String = "",
    val statusBanner: BannerMessage? = null
)

data class ReportUiState(
    val classes: List<ClassDivisionPair> = emptyList(),
    val selectedClasses: Set<String> = MarathiUtils.CLASSES.toSet(),
    val selectedDivisions: Set<String> = MarathiUtils.DIVISIONS.toSet(),
    val fields: List<ReportFieldOption> = defaultReportFields(),
    val sortField: String = "asis",
    val purpose: String = "विद्यार्थी यादी",
    val feePaidFilter: String = "",
    val feeDueFilter: String = "",
    val landscapePdf: Boolean = false,
    val loading: Boolean = false,
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val groupedRows: List<ReportClassGroup> = emptyList(),
    val error: String? = null
) {
    companion object {
        fun defaultReportFields(): List<ReportFieldOption> = listOf(
            ReportFieldOption("regNo", "नोंद क्र. (Reg No)", true, 1),
            ReportFieldOption("rollNo", "रोल नं.", true, 2),
            ReportFieldOption("fullName", "पूर्ण नाव", true, 3),
            ReportFieldOption("iyatta", "इयत्ता", true, 4),
            ReportFieldOption("tukdi", "तुकडी", true, 5),
            ReportFieldOption("gender", "लिंग", false, 6),
            ReportFieldOption("dob", "जन्मतारीख", false, 7),
            ReportFieldOption("category", "प्रवर्ग", false, 8),
            ReportFieldOption("caste", "जात", false, 9),
            ReportFieldOption("aadhar", "आधार क्र.", false, 10),
            ReportFieldOption("studentId", "Student ID", false, 11),
            ReportFieldOption("pen", "PEN", false, 12),
            ReportFieldOption("whatsappMobile", "WhatsApp नं.", true, 13),
            ReportFieldOption("alternateMobile", "पर्यायी नं.", false, 14),
            ReportFieldOption("address", "पत्ता", false, 15),
            ReportFieldOption("motherName", "आईचे नाव", false, 16),
            ReportFieldOption("feePaid", "जमा फी (Paid)", false, 17),
            ReportFieldOption("feeDue", "बाकी फी (Pending)", false, 18)
        )
    }
}

data class StatsClassRow(
    val iyatta: String,
    val tukdi: String,
    val total: Int,
    val absent: Int,
    val fees: Double,
    val femaleCat: Map<String, Int>,
    val maleCat: Map<String, Int>,
    val ageCounts: Map<Int, Int>
)

data class StatsUiState(
    val loading: Boolean = false,
    val classes: List<StatsClassRow> = emptyList(),
    val statusText: String = "",
    val error: String? = null
)

data class PendingFeeRow(
    val fullName: String,
    val iyatta: String,
    val tukdi: String,
    val rollNo: String,
    val regNo: String,
    val paid: Double,
    val opening: Double,
    val pending: Double,
    val mobile: String,
    val lastPaidDate: String
)

data class PromotionClassRow(
    val iyatta: String,
    val count: Int,
    val suggestedNext: String,
    val selectedTarget: String
)

data class PromoBatchItem(val batchId: String, val time: String, val count: Int, val by: String)
data class ArchiveBatchItem(val batchId: String, val iyatta: String, val count: Int, val sheet: String)

data class YearEndStepState(
    val n: Int,
    val title: String,
    val what: String,
    val undoInfo: String,
    val status: String,
    val by: String,
    val reason: String,
    val infoSummary: String
)

data class AdminAndQcUiState(
    val users: List<UserAccount> = emptyList(),
    val usersLoading: Boolean = false,
    val userStatus: BannerMessage? = null,
    val classTeachers: List<ClassTeacherItem> = emptyList(),
    val pendingFeesTotalSetting: Double = 1000.0,
    val pendingFees: List<PendingFeeRow> = emptyList(),
    val pendingFeesSummary: String = "",
    val promoClasses: List<PromotionClassRow> = emptyList(),
    val promoBatches: List<PromoBatchItem> = emptyList(),
    val archiveBatches: List<ArchiveBatchItem> = emptyList(),
    val newAcYearInput: String = "2027-28",
    val securityLines: List<String> = emptyList(),
    val backupInfo: String = "",
    val backupSchedule: String = "off",
    val backupExtraIds: String = "",
    val backupAlertEmail: String = "",
    val restoreDrillInfo: String = "",
    val restoreDrillRunning: Boolean = false,
    val sheetSizes: List<Pair<String, Int>> = emptyList(),
    val superStatus: BannerMessage? = null,
    val photoRecallRunning: Boolean = false,
    val photoRecallStopRequested: Boolean = false,
    val photoRecallReport: String = "",
    val archiveYearItems: List<ArchiveYearItem> = emptyList(),
    val archiveYears: List<String> = emptyList(),
    val selectedArchiveYear: String = "",
    val archiveSection: String = "fees",
    val archiveSummary: String = "",
    val archiveHeaders: List<String> = emptyList(),
    val archiveRows: List<List<String>> = emptyList(),
    val archiveCompareYearA: String = "",
    val archiveCompareYearB: String = "",
    val archiveHealthText: String = "",
    val archiveLoading: Boolean = false,
    val qcTab: String = "q",
    val qcLoading: Boolean = false,
    val qcTotalStudents: Int = 0,
    val qcStudentsWithIssues: Int = 0,
    val qcAgeTol: Int = 3,
    val qcSummaryCounts: Map<String, Int> = emptyMap(),
    val qcClassSummaryRows: List<List<String>> = emptyList(),
    val qcDetailType: String = "",
    val qcDetailTotal: Int = 0,
    val qcDetailRows: List<List<String>> = emptyList(),
    val yearEndSummaryHeader: String = "",
    val yearEndWarnings: List<String> = emptyList(),
    val yearEndChecks: List<Pair<String, String>> = emptyList(),
    val yearEndClassHeaders: List<String> = emptyList(),
    val yearEndClassRows: List<List<String>> = emptyList(),
    val yearEndDuesRows: List<List<String>> = emptyList(),
    val yearEndLcRows: List<List<String>> = emptyList(),
    val sheetCapSummary: String = "",
    val sheetCapPct: Double = 0.0,
    val sheetCapLevel: String = "ok",
    val sheetCapRows: List<List<String>> = emptyList(),
    val sheetCapArchivableRows: List<List<String>> = emptyList(),
    val clientErrSummary: String = "",
    val clientErrorRows: List<List<String>> = emptyList(),
    val clientErrorCsvRows: List<List<String>> = emptyList(),
    val usageSummaryText: String = "",
    val usageDays: Int = 7,
    val usageTeachers: Int = 100,
    val usageRows: List<List<String>> = emptyList(),
    val usageScenarioRows: List<List<String>> = emptyList(),
    val usageTips: List<String> = emptyList(),
    val wizardClosingYear: String = "",
    val wizardNewYear: String = "",
    val wizardCurrentYear: String = "",
    val wizardLastClass: String = "",
    val wizardComplete: Boolean = false,
    val wizardSteps: List<YearEndStepState> = emptyList(),
    val wizardVerifySummary: String = "",
    val wizardVerifyRows: List<List<String>> = emptyList(),
    val wizardVerifyPassedOut: String = "",
    val wizardVerifyNotNewYear: Int = 0,
    val wizardVerifyAllOk: Boolean = false,
    val wizardPromoPreview: String = "",
    val wizardPromoMap: Map<String, String> = emptyMap(),
    val wizardPromoTotal: Int = 0,
    val wizardStatus: BannerMessage? = null
)
