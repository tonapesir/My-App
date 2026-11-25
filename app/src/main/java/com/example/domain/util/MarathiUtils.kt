package com.example.domain.util

import com.example.domain.model.HolidayItem
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object MarathiUtils {
    val CLASSES = listOf("5th", "6th", "7th", "8th", "9th", "10th")
    val ADMISSION_CLASSES = listOf("1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th")
    val DIVISIONS = listOf("अ", "ब", "क", "ड", "इ", "फ", "ग", "ह", "आय")
    val RELIGIONS = listOf("हिंदू", "मुस्लिम", "ख्रिचन", "बौद्ध", "जैन", "शीख", "इतर")
    val CATEGORIES = listOf("SC", "ST", "VJA", "NT B", "NT C", "NT D", "SEBC", "SBC", "OBC", "Gen")
    val MOTHER_TONGUES = listOf("मराठी", "हिंदी", "इंग्रजी", "उर्दू")
    val TRAVEL_MODES = listOf("ST", "रिक्षा", "वैयक्तिक वाहन", "सायकल", "पायी")
    val SCHOLARSHIP_STATUSES = listOf("प्रलंबित", "मंजूर", "वितरित")
    val BF_PURPOSES = listOf(
        "शिष्यवृत्ती अर्ज करण्यासाठी",
        "आधार कार्ड अद्यावत करण्यासाठी",
        "बँक खाते उघडण्यासाठी",
        "जातीचा दाखला काढण्यासाठी",
        "राष्ट्रीयत्व आणि अधिवास प्रमाणपत्र मिळवण्यासाठी",
        "एसटी महामंडळाच्या पास सुविधा मिळणेसाठी",
        "जन्म दाखला वरील माहिती अद्यावत करण्यासाठी",
        "शालेय रेकॉर्ड वरील माहिती दुरुस्ती / अद्यावत करण्यासाठी",
        "विविध स्पर्धांमध्ये सहभाग घेण्यासाठी",
        "इतर"
    )
    val AT_PURPOSES = listOf(
        "शासकीय योजनेचा लाभ घेण्यासाठी",
        "शिष्यवृत्तीसाठी अर्ज करण्यासाठी",
        "इतर"
    )

    private val MONTHS_MR = listOf(
        "", "जानेवारी", "फेब्रुवारी", "मार्च", "एप्रिल", "मे", "जून",
        "जुलै", "ऑगस्ट", "सप्टेंबर", "ऑक्टोबर", "नोव्हेंबर", "डिसेंबर"
    )

    private val DAYS_MR = listOf(
        "", "एक", "दोन", "तीन", "चार", "पाच", "सहा", "सात", "आठ", "नऊ", "दहा",
        "अकरा", "बारा", "तेरा", "चौदा", "पंधरा", "सोळा", "सतरा", "अठरा", "एकोणीस", "वीस",
        "एकवीस", "बावीस", "तेवीस", "चोवीस", "पंचवीस", "सव्वीस", "सत्तावीस", "अठ्ठावीस", "एकोणतीस", "तीस", "एकतीस"
    )

    private val NUMS_0_TO_99 = listOf(
        "शून्य", "एक", "दोन", "तीन", "चार", "पाच", "सहा", "सात", "आठ", "नऊ", "दहा",
        "अकरा", "बारा", "तेरा", "चौदा", "पंधरा", "सोळा", "सतरा", "अठरा", "एकोणीस", "वीस",
        "एकवीस", "बावीस", "तेवीस", "चोवीस", "पंचवीस", "सव्वीस", "सत्तावीस", "अठ्ठावीस", "एकोणतीस", "तीस",
        "एकतीस", "बत्तीस", "तेहतीस", "चौतीस", "पस्तीस", "छत्तीस", "सदोतीस", "अडोतीस", "एकोणचाळीस", "चाळीस",
        "एक्केचाळीस", "बेचाळीस", "त्रेचाळीस", "चव्वेचाळीस", "पंचेचाळीस", "सेहेचाळीस", "सत्तेचाळीस", "अठ्ठेचाळीस", "एकोणपन्नास", "पन्नास",
        "एक्कावन्न", "बावन्न", "त्रेपन्न", "चोपन्न", "पंचावन्न", "छप्पन्न", "सत्तावन्न", "अठ्ठावन्न", "एकोणसाठ", "साठ",
        "एकसष्ट", "बासष्ट", "त्रेसष्ट", "चौसष्ट", "पासष्ट", "सहासष्ट", "सदुसष्ट", "अडुसष्ट", "एकोणसत्तर", "सत्तर",
        "एकाहत्तर", "बहात्तर", "त्र्याहत्तर", "चौर्‍याहत्तर", "पंच्याहत्तर", "शहात्तर", "सत्त्याहत्तर", "अठ्ठ्याहत्तर", "एकोणऐंशी", "ऐंशी",
        "एक्क्याऐंशी", "ब्याऐंशी", "त्र्याऐंशी", "चौर्‍याऐंशी", "पंच्यांशी", "शहाऐंशी", "सत्त्याऐंशी", "अठ्ठ्याऐंशी", "एकोणनव्वद", "नव्वद",
        "एक्क्याण्णव", "ब्याण्णव", "त्र्याण्णव", "चौर्‍याण्णव", "पंच्याण्णव", "शहाण्णव", "सत्त्याण्णव", "अठ्ठ्याण्णव", "नव्व्याण्णव"
    )

    fun dobToMarathiWords(isoDate: String): String {
        val parts = isoDate.trim().split("-")
        if (parts.size != 3) return ""
        val y = parts[0].toIntOrNull() ?: return ""
        val m = parts[1].toIntOrNull() ?: return ""
        val d = parts[2].toIntOrNull() ?: return ""
        val dayStr = DAYS_MR.getOrNull(d) ?: d.toString()
        val monthStr = MONTHS_MR.getOrNull(m) ?: ""
        val yearStr = yearInMarathi(y)
        return "$dayStr $monthStr $yearStr".trim()
    }

    fun monthNameMr(m: Int): String = MONTHS_MR.getOrNull(m).orEmpty()

    fun yearInMarathi(y: Int): String {
        if (y < 100) return numToMarathi(y)
        if (y in 1900..1999) {
            val yy = y - 1900
            return "एकोणावीसशे" + if (yy > 0) " ${numToMarathi(yy)}" else ""
        }
        if (y < 1000) {
            val h = y / 100
            val rem = y % 100
            return "${numToMarathi(h)}शे" + if (rem > 0) " ${numToMarathi(rem)}" else ""
        }
        val th = y / 1000
        val rest = y % 1000
        val thStr = (if (th == 1) "एक" else numToMarathi(th)) + " हजार"
        if (rest == 0) return thStr
        if (rest < 100) return "$thStr ${numToMarathi(rest)}"
        val h = rest / 100
        val rem = rest % 100
        return "$thStr ${numToMarathi(h)}शे" + if (rem > 0) " ${numToMarathi(rem)}" else ""
    }

    fun numToMarathi(n: Int): String = NUMS_0_TO_99.getOrNull(n) ?: n.toString()

    fun classToMr(c: String?): String = when (c?.trim()) {
        "1st" -> "१ ली"
        "2nd" -> "२ री"
        "3rd" -> "३ री"
        "4th" -> "४ थी"
        "5th" -> "५ वी"
        "6th" -> "६ वी"
        "7th" -> "७ वी"
        "8th" -> "८ वी"
        "9th" -> "९ वी"
        "10th" -> "१० वी"
        else -> c.orEmpty()
    }

    fun classWords(c: String?): String = when (c?.trim()) {
        "5th" -> "पाचवी"
        "6th" -> "सहावी"
        "7th" -> "सातवी"
        "8th" -> "आठवी"
        "9th" -> "नववी"
        "10th" -> "दहावी"
        else -> c.orEmpty()
    }

    fun todayIso(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun currentMonthIso(): String = todayIso().substring(0, 7)

    fun todayDisplay(): String = fmtDate(todayIso())

    fun fmtDate(v: String?): String {
        val raw = v.orEmpty().trim()
        if (raw.isEmpty()) return ""
        val match = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(raw)
        return if (match != null) {
            val (y, m, d) = match.destructured
            "$d-$m-$y"
        } else {
            raw
        }
    }

    fun isoDateOrEmpty(v: String?): String {
        val raw = v.orEmpty().trim()
        if (Regex("""^\d{4}-\d{2}-\d{2}""").containsMatchIn(raw)) return raw.take(10)
        val dmY = Regex("""^(\d{2})[/-](\d{2})[/-](\d{4})$""").find(raw)
        if (dmY != null) {
            val (d, m, y) = dmY.destructured
            return "$y-$m-$d"
        }
        return ""
    }

    fun fmtRupees(amount: Double): String {
        val rounded = if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format(Locale.US, "%.2f", amount)
        return "₹$rounded"
    }

    fun computeAgeAsOf(dobIso: String, asOfIso: String = "2026-09-30"): Int? {
        return try {
            val dob = LocalDate.parse(isoDateOrEmpty(dobIso))
            val asOf = LocalDate.parse(asOfIso)
            var age = asOf.year - dob.year
            if (asOf.monthValue < dob.monthValue || (asOf.monthValue == dob.monthValue && asOf.dayOfMonth < dob.dayOfMonth)) {
                age--
            }
            age
        } catch (_: Exception) {
            null
        }
    }

    fun normalizeIndianMobile(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits"
            digits.length >= 11 -> digits
            else -> ""
        }
    }

    fun whatsappUrl(mobile: String, message: String): String {
        val num = normalizeIndianMobile(mobile)
        val enc = URLEncoder.encode(message, "UTF-8")
        return if (num.isNotEmpty()) "https://wa.me/$num?text=$enc" else "https://wa.me/?text=$enc"
    }

    fun cleanDigits10(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return if (d.length >= 10) d.takeLast(10) else d
    }

    fun parseQrLookupCode(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val regMatch = Regex("""(?:[?&])regNo=([^&\s]+)""", RegexOption.IGNORE_CASE).find(trimmed)
        if (regMatch != null) {
            return runCatching { URLDecoder.decode(regMatch.groupValues[1], "UTF-8") }.getOrDefault(regMatch.groupValues[1]).trim()
        }
        val serMatch = Regex("""(?:[?&])serial=([^&\s]+)""", RegexOption.IGNORE_CASE).find(trimmed)
        if (serMatch != null) {
            return runCatching { URLDecoder.decode(serMatch.groupValues[1], "UTF-8") }.getOrDefault(serMatch.groupValues[1]).trim()
        }
        val prefixMatch = Regex("""^(?:ID|REG|REG\s*NO|STUDENT\s*ID)\s*[:\-]\s*(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (prefixMatch != null) {
            return prefixMatch.groupValues[1].trim()
        }
        return trimmed
    }

    fun isSundayIso(dateIso: String): Boolean {
        return try {
            LocalDate.parse(isoDateOrEmpty(dateIso)).dayOfWeek == DayOfWeek.SUNDAY
        } catch (_: Exception) {
            false
        }
    }

    fun isHolidayOrSunday(dateIso: String, holidays: List<HolidayItem>): Boolean {
        val iso = isoDateOrEmpty(dateIso)
        if (iso.isBlank()) return false
        if (isSundayIso(iso)) return true
        return holidays.any { isoDateOrEmpty(it.dateIso) == iso }
    }

    fun defaultPublicHolidays(year: Int = LocalDate.now().year): List<HolidayItem> = listOf(
        HolidayItem("$year-01-26", "प्रजासत्ताक दिन"),
        HolidayItem("$year-02-19", "छत्रपती शिवाजी महाराज जयंती"),
        HolidayItem("$year-05-01", "महाराष्ट्र दिन"),
        HolidayItem("$year-08-15", "स्वातंत्र्य दिन"),
        HolidayItem("$year-10-02", "महात्मा गांधी जयंती")
    )

    fun isBirthdayToday(dob: String, todayIsoVal: String = todayIso()): Boolean {
        val iso = isoDateOrEmpty(dob)
        val today = isoDateOrEmpty(todayIsoVal)
        return iso.length >= 10 && today.length >= 10 && iso.drop(5) == today.drop(5)
    }

    fun formatMessageTemplate(
        templateBody: String,
        student: com.example.domain.model.Student,
        customClassLabel: String = ""
    ): String {
        val clsLabel = customClassLabel.ifBlank {
            listOf(classToMr(student.iyatta), student.tukdi).filter { it.isNotBlank() }.joinToString(" ")
        }
        return templateBody
            .replace("{नाव}", student.fullName)
            .replace("{name}", student.fullName)
            .replace("{वर्ग}", clsLabel.ifBlank { "-" })
            .replace("{class}", clsLabel.ifBlank { "-" })
            .replace("{रोल}", student.rollNo.ifBlank { "-" })
            .replace("{roll}", student.rollNo.ifBlank { "-" })
            .replace("{नोंद}", student.regNo.ifBlank { "-" })
            .replace("{reg}", student.regNo.ifBlank { "-" })
    }
}

