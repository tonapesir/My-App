package com.example.domain.model

import kotlinx.serialization.Serializable

enum class UserRole(val key: String, val displayLabel: String) {
    SUPER("super", "Super Master User"),
    MASTER("master", "Master User"),
    DEO("deo", "DEO User"),
    CERT("cert", "Certificate User"),
    TEACHER("teacher", "वर्ग शिक्षक"),
    PEON("peon", "Peon (शिपाई)");

    companion object {
        fun fromKey(raw: String?): UserRole? {
            val k = raw.orEmpty().trim().lowercase()
            return entries.firstOrNull { it.key == k }
        }
    }
}

enum class AppPage(val key: String, val titleMr: String, val subtitle: String, val emoji: String) {
    DASHBOARD("dashboard", "मुख्यपृष्ठ", "Dashboard & Summary", "🏠"),
    TEACHER("teacher", "वर्ग शिक्षक", "Class Teacher Workspace", "🏫"),
    STUDENT("student", "विद्यार्थी माहिती", "Student Entry & Update", "👤"),
    LC("lc", "LC Issue", "शाळा सोडल्याचा दाखला", "📋"),
    BONAFIDE("bonafide", "बोनाफाईड", "Bonafide Certificate", "📄"),
    ATTENDANCE("attendance", "उपस्थिती प्रमाणपत्र", "Attendance Certificate", "✅"),
    SEARCH("search", "विद्यार्थी शोधा", "Search Students", "🔍"),
    HISTORY("history", "History / Export", "LC/BF/AT History & Export", "📁"),
    PROFILE("profile", "Student Profile", "फोटो सह संपूर्ण माहिती", "🪪"),
    USERS("users", "User Management", "Users & Password", "👥"),
    STATS("stats", "सांख्यिकी अहवाल", "Class, Fees, Category & Age", "📊"),
    MAINTENANCE("maintenance", "मेंटेनन्स", "Class/Div Change, Fees & Delete", "🔧"),
    CLASSINFO("classinfo", "वर्ग माहिती", "Class Roster & Diary", "🏫"),
    REPORT("report", "रिपोर्ट", "Custom Columns & PDF", "📑"),
    SUPERADMIN("superadmin", "Super Admin", "Fees, Promotion, Year & Backup", "🛡️"),
    ATTANALYTICS("attanalytics", "हजेरी विश्लेषण", "Monthly % & Consecutive Absent", "📈"),
    EARLYLEAVE("earlyleave", "घरी जाणारे", "Early Leave Register", "🚪"),
    SCHOLARSHIP("scholarship", "शिष्यवृत्ती", "Scholarship Register", "🎓"),
    TEACHERLIST("teacherlist", "वर्ग शिक्षक यादी", "Class Teachers Contacts", "📒"),
    ARCHIVE("archive", "Archive", "Old Years (Read-only)", "📦"),
    DATAQC("dataqc", "डेटा तपासणी", "Data QC & Year-End Wizard", "🔎"),
    HELP("help", "मदत", "वापरकर्ता-मार्गदर्शिका", "❓");

    companion object {
        private val ALLOWED: Map<UserRole, List<AppPage>> = mapOf(
            UserRole.SUPER to listOf(
                DASHBOARD, STUDENT, LC, BONAFIDE, ATTENDANCE, SEARCH, HISTORY, PROFILE,
                USERS, STATS, MAINTENANCE, CLASSINFO, REPORT, SUPERADMIN, ATTANALYTICS,
                EARLYLEAVE, SCHOLARSHIP, TEACHERLIST, ARCHIVE, DATAQC, HELP
            ),
            UserRole.MASTER to listOf(
                DASHBOARD, STUDENT, LC, BONAFIDE, ATTENDANCE, SEARCH, HISTORY, PROFILE,
                USERS, STATS, MAINTENANCE, CLASSINFO, REPORT, ATTANALYTICS,
                EARLYLEAVE, SCHOLARSHIP, TEACHERLIST, ARCHIVE, HELP
            ),
            UserRole.DEO to listOf(
                STUDENT, SEARCH, HISTORY, USERS, PROFILE, TEACHERLIST, HELP
            ),
            UserRole.CERT to listOf(
                BONAFIDE, ATTENDANCE, SEARCH, HISTORY, USERS, PROFILE, HELP
            ),
            UserRole.TEACHER to listOf(
                TEACHER, PROFILE, REPORT, EARLYLEAVE, SCHOLARSHIP, HELP
            ),
            UserRole.PEON to listOf(
                SEARCH, PROFILE, EARLYLEAVE, TEACHERLIST, HELP
            )
        )

        fun allowedFor(role: UserRole): List<AppPage> = ALLOWED[role].orEmpty()

        fun defaultFor(role: UserRole): AppPage = when (role) {
            UserRole.DEO -> STUDENT
            UserRole.CERT -> BONAFIDE
            UserRole.TEACHER -> TEACHER
            UserRole.PEON -> EARLYLEAVE
            UserRole.SUPER, UserRole.MASTER -> DASHBOARD
        }
    }
}

enum class TeacherTab(val key: String, val titleMr: String, val emoji: String) {
    DASH("dash", "डॅशबोर्ड", "📊"),
    ROSTER("roster", "विद्यार्थी माहिती", "👥"),
    ATT("att", "हजेरी", "✅"),
    DIARY("diary", "विद्यार्थी डायरी", "📔"),
    FEES("fees", "फी", "💰"),
    NOTICES("notices", "सूचना", "🔔"),
    ANA("ana", "विश्लेषण", "📈"),
    MAINT("maint", "मेंटेनन्स", "🔧")
}

@Serializable
data class UserSession(
    val username: String,
    val roleKey: String,
    val label: String,
    val assignedClass: String = "",
    val token: String = "",
    val exp: Long = 0L,
    val offline: Boolean = false
) {
    val role: UserRole get() = UserRole.fromKey(roleKey) ?: UserRole.CERT
    val iyatta: String get() = assignedClass.split("|").getOrNull(0).orEmpty().trim()
    val tukdi: String get() = assignedClass.split("|").getOrNull(1).orEmpty().trim()
    val canEditMaster: Boolean get() = role == UserRole.MASTER || role == UserRole.SUPER
    val canEditStudent: Boolean get() = canEditMaster || role == UserRole.DEO
    val canSaveCert: Boolean get() = canEditMaster || role == UserRole.CERT
    val canSeeMoney: Boolean get() = role == UserRole.SUPER || role == UserRole.MASTER || role == UserRole.TEACHER
}

@Serializable
data class OfflineCredRecord(
    val username: String,
    val role: String,
    val label: String,
    val assignedClass: String = "",
    val saltBase64: String,
    val hashBase64: String,
    val iterations: Int = 12000,
    val exp: Long
)

@Serializable
data class Student(
    val rowIndex: Int = 0,
    val acYear: String = "",
    val regNo: String = "",
    val studentId: String = "",
    val bookNo: String = "",
    val aadhar: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val pen: String = "",
    val rollNo: String = "",
    val fullName: String = "",
    val motherName: String = "",
    val gender: String = "",
    val religion: String = "",
    val caste: String = "",
    val subcaste: String = "",
    val dob: String = "",
    val dobWords: String = "",
    val nationality: String = "भारतीय",
    val motherTongue: String = "मराठी",
    val birthVillage: String = "",
    val prevSchool: String = "",
    val admissionDate: String = "",
    val admissionClass: String = "",
    val contact: String = "",
    val address: String = "",
    val photoUrl: String = "",
    val whatsappMobile: String = "",
    val alternateMobile: String = "",
    val category: String = "",
    val minority: String = ""
) {
    val bestPhone: String get() = whatsappMobile.ifBlank { alternateMobile }.ifBlank { contact }
}

@Serializable
data class LcFormData(
    val regNo: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val motherName: String = "",
    val gender: String = "",
    val dob: String = "",
    val dobWords: String = "",
    val iyatta: String = "",
    val religion: String = "",
    val caste: String = "",
    val subcaste: String = "",
    val nationality: String = "भारतीय",
    val motherTongue: String = "मराठी",
    val birthPlace: String = "",
    val aadhar: String = "",
    val pen: String = "",
    val bookNo: String = "",
    val prevSchool: String = "",
    val admissionDate: String = "",
    val admissionClass: String = "",
    val leaveDate: String = "",
    val leaveClass: String = "",
    val classStartMonth: String = "",
    val classStartYear: String = "",
    val lcNo: String = "",
    val lcDate: String = "",
    val conduct: String = "चांगला",
    val progress: String = "चांगली",
    val certReceived: String = "",
    val stxt76: String = "",
    val remarks: String = "",
    val medium: String = "",
    val nextSchool: String = "",
    val lcCount: String = "1",
    val feeStatus: String = "",
    val stxt60: String = "",
    val stxt61: String = "",
    val stxt62: String = "",
    val stxt63: String = "",
    val stxt67: String = "",
    val stxt68: String = "",
    val stxt69: String = "",
    val stxt70: String = "",
    val nationalityType: String = "",
    val rowIndex: Int = 0
)

@Serializable
data class BonafideFormData(
    val regNo: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val gender: String = "",
    val dob: String = "",
    val dobWords: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val birthPlace: String = "",
    val caste: String = "",
    val religion: String = "",
    val nationality: String = "भारतीय",
    val acYear: String = "2026-27",
    val bfDate: String = "",
    val purpose: String = "",
    val admissionDate: String = "",
    val admissionClass: String = "",
    val remarks: String = "",
    val address: String = "",
    val contact: String = "",
    val certNo: String = "",
    val pen: String = "",
    val photoUrl: String = "",
    val lang: String = "mr"
)

@Serializable
data class AttendanceCertFormData(
    val regNo: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val gender: String = "",
    val dob: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val acYear: String = "2026-27",
    val atDate: String = "",
    val purpose: String = "",
    val totalDays: String = "",
    val presentDays: String = "",
    val pct: String = "",
    val fromDate: String = "",
    val toDate: String = "",
    val remarks: String = "",
    val certNo: String = "",
    val pen: String = "",
    val photoUrl: String = "",
    val lang: String = "mr"
)

@Serializable
data class DashboardStats(
    val totalStudents: Int = 0,
    val totalGirls: Int = 0,
    val totalBoys: Int = 0,
    val todayLC: Int = 0,
    val todayBF: Int = 0,
    val monthCerts: Int = 0,
    val classCounts: Map<String, Int> = emptyMap()
)

@Serializable
data class ClassDivisionPair(
    val iyatta: String = "",
    val tukdi: String = ""
) {
    val key: String get() = "$iyatta|$tukdi"
}

@Serializable
data class AttendanceRecord(
    val date: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val regNo: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val reason: String = "",
    val markedBy: String = ""
)

@Serializable
data class NoticeItem(
    val timestamp: String = "",
    val date: String = "",
    val title: String = "",
    val message: String = "",
    val postedBy: String = "",
    val targetClass: String = ""
)

@Serializable
data class DiaryEntry(
    val timestamp: String = "",
    val date: String = "",
    val regNo: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val type: String = "",
    val remark: String = "",
    val enteredBy: String = ""
)

@Serializable
data class TransportInfo(
    val regNo: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val nativeVillage: String = "",
    val travelMode: String = "",
    val transportContact: String = "",
    val updatedBy: String = "",
    val sourceYear: String = "",
    val archived: Boolean = false
)

@Serializable
data class FeeRecord(
    val date: String = "",
    val regNo: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val acYear: String = "",
    val totalFee: Double = 0.0,
    val feePaid: Double = 0.0,
    val openingDue: Double = 0.0,
    val pendingFee: Double = 0.0,
    val updatedBy: String = ""
)

@Serializable
data class EarlyLeaveRecord(
    val date: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val regNo: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val rollNo: String = "",
    val type: String = "permitted", // "permitted" | "unpermitted"
    val period: Int = 1,
    val markedBy: String = "",
    val outStatus: String = "", // "" | "gone" | "notgone"
    val outBy: String = "",
    val outAt: String = "",
    val whatsappMobile: String = "",
    val alternateMobile: String = "",
    val contact: String = ""
) {
    val bestPhone: String get() = whatsappMobile.ifBlank { alternateMobile }.ifBlank { contact }
}

@Serializable
data class ScholarshipRecord(
    val id: String = "",
    val timestamp: String = "",
    val acYear: String = "",
    val regNo: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val name: String = "",
    val scheme: String = "",
    val amount: Double = 0.0,
    val status: String = "प्रलंबित",
    val remark: String = "",
    val enteredBy: String = ""
)

@Serializable
data class ClassTeacherItem(
    val classKey: String = "",
    val classMr: String = "",
    val div: String = "",
    val nameEn: String = "",
    val nameMr: String = "",
    val mobile: String = "",
    val shortName: String = ""
)

@Serializable
data class CertificateHistoryItem(
    val type: String = "",
    val serial: String = "",
    val date: String = "",
    val regNo: String = "",
    val fullName: String = "",
    val detail: String = ""
)

@Serializable
data class UserAccount(
    val rowIndex: Int = 0,
    val username: String = "",
    val password: String = "",
    val role: String = "cert",
    val label: String = "",
    val assignedClass: String = "",
    val mobile: String = ""
)

@Serializable
data class PendingSaveItem(
    val reqId: String,
    val action: String,
    val params: Map<String, String>,
    val queuedAt: Long,
    val by: String,
    val summary: String,
    val failed: Boolean = false,
    val lastError: String = ""
)

@Serializable
data class BirthdayStudent(
    val regNo: String = "",
    val studentId: String = "",
    val rollNo: String = "",
    val fullName: String = "",
    val iyatta: String = "",
    val tukdi: String = "",
    val dob: String = "",
    val age: Int? = null,
    val whatsappMobile: String = "",
    val alternateMobile: String = "",
    val contact: String = ""
)

@Serializable
data class AcYearSample(
    val regNo: String = "",
    val name: String = "",
    val year: String = ""
)

@Serializable
data class AcYearInfo(
    val currentAcYear: String = "2026-27",
    val dateYear: String = "",
    val fromProperty: Boolean = false,
    val aheadOfDate: Boolean = false,
    val behindDate: Boolean = false,
    val mismatchCount: Int = 0,
    val futureCount: Int = 0,
    val promoPendingCount: Int = 0,
    val canSync: Boolean = false,
    val breakdown: Map<String, Int> = emptyMap(),
    val studentsCount: Int = 0,
    val sample: List<AcYearSample> = emptyList()
)

@Serializable
data class ArchiveYearItem(
    val year: String = "",
    val current: Boolean = false,
    val archived: Boolean = false,
    val hasFile: Boolean = false
)

@Serializable
data class ReportFieldOption(
    val key: String,
    val label: String,
    val selected: Boolean = false,
    val order: Int = 1
)

@Serializable
data class ReportClassGroup(
    val iyatta: String = "",
    val tukdi: String = "",
    val rows: List<List<String>> = emptyList()
)

@Serializable
data class HolidayItem(
    val dateIso: String,
    val title: String
)

data class QrScanResult(
    val rawText: String,
    val regNoOrId: String,
    val type: String = ""
)

@Serializable
data class MessageTemplate(
    val id: String,
    val title: String,
    val body: String,
    val isDefault: Boolean = false
)


