package com.example

import com.example.domain.model.AppPage
import com.example.domain.model.Student
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarathiUtilsTest {
    @Test
    fun dobToMarathiWords_formatsCorrectly() {
        assertEquals("पंधरा ऑगस्ट दोन हजार दहा", MarathiUtils.dobToMarathiWords("2010-08-15"))
        assertEquals("एक जानेवारी दोन हजार", MarathiUtils.dobToMarathiWords("2000-01-01"))
        assertEquals("दहा मे एकोणावीसशे नव्व्याण्णव", MarathiUtils.dobToMarathiWords("1999-05-10"))
    }

    @Test
    fun classToMr_mapsAllStandards() {
        assertEquals("५ वी", MarathiUtils.classToMr("5th"))
        assertEquals("१० वी", MarathiUtils.classToMr("10th"))
    }

    @Test
    fun fmtDate_and_isoDateOrEmpty_roundTrip() {
        assertEquals("15-08-2010", MarathiUtils.fmtDate("2010-08-15"))
        assertEquals("2010-08-15", MarathiUtils.isoDateOrEmpty("15/08/2010"))
        assertEquals("2010-08-15", MarathiUtils.isoDateOrEmpty("15-08-2010"))
    }

    @Test
    fun normalizeIndianMobile_handles10DigitsAndSpaces() {
        assertEquals("919876543210", MarathiUtils.normalizeIndianMobile("98765 43210"))
        assertEquals("919876543210", MarathiUtils.normalizeIndianMobile("+91-9876543210"))
        assertEquals("", MarathiUtils.normalizeIndianMobile("12345"))
    }

    @Test
    fun rolePermissions_enforceSuperMasterTeacherSeparation() {
        assertTrue(AppPage.DATAQC in AppPage.allowedFor(UserRole.SUPER))
        assertFalse(AppPage.DATAQC in AppPage.allowedFor(UserRole.MASTER))
        assertFalse(AppPage.SUPERADMIN in AppPage.allowedFor(UserRole.MASTER))
        assertTrue(AppPage.TEACHER in AppPage.allowedFor(UserRole.TEACHER))
        assertEquals(AppPage.EARLYLEAVE, AppPage.defaultFor(UserRole.PEON))
    }

    @Test
    fun parseQrLookupCode_extractsRegNoFromUrlOrRaw() {
        assertEquals("1234", MarathiUtils.parseQrLookupCode("https://script.google.com/macros/s/abc/exec?action=verify&type=ID&regNo=1234"))
        assertEquals("SGS-505", MarathiUtils.parseQrLookupCode("ID: SGS-505"))
        assertEquals("987", MarathiUtils.parseQrLookupCode("987"))
    }

    @Test
    fun holidaysAndBirthdays_evaluateCorrectly() {
        val holidays = MarathiUtils.defaultPublicHolidays(2026)
        assertTrue(MarathiUtils.isHolidayOrSunday("2026-08-15", holidays))
        assertTrue(MarathiUtils.isSundayIso("2026-08-16"))
        assertFalse(MarathiUtils.isHolidayOrSunday("2026-08-17", holidays))

        assertTrue(MarathiUtils.isBirthdayToday("2012-11-24", "2026-11-24"))
        assertTrue(MarathiUtils.isBirthdayToday("24-11-2012", "2026-11-24"))
        assertFalse(MarathiUtils.isBirthdayToday("2012-11-25", "2026-11-24"))
    }

    @Test
    fun formatMessageTemplate_replacesPlaceholdersAndFallbacks() {
        val student = Student(
            regNo = "1402",
            iyatta = "8th",
            tukdi = "ब",
            rollNo = "12",
            fullName = "आरव पाटील"
        )
        val formatted = MarathiUtils.formatMessageTemplate(
            "प्रिय पालक, {नाव} (वर्ग: {वर्ग}, हजेरी क्र.: {रोल}, नोंद क्र.: {नोंद})",
            student
        )
        assertEquals("प्रिय पालक, आरव पाटील (वर्ग: ८ वी ब, हजेरी क्र.: 12, नोंद क्र.: 1402)", formatted)

        val emptyFallbackStudent = Student(fullName = "मीरा देशमुख")
        val fallbackFormatted = MarathiUtils.formatMessageTemplate(
            "{name} | {class} | {roll} | {reg}",
            emptyFallbackStudent,
            customClassLabel = "९ वी अ"
        )
        assertEquals("मीरा देशमुख | ९ वी अ | - | -", fallbackFormatted)
    }
}
