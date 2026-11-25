package com.example

import com.example.domain.model.AppPage
import com.example.domain.model.Student
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import com.example.domain.util.ReportUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SgsParityTest {

    private fun sampleStudent(
        regNo: String,
        name: String,
        gender: String,
        iyatta: String = "5th",
        tukdi: String = "अ",
        rollNo: String = ""
    ) = Student(
        regNo = regNo,
        fullName = name,
        gender = gender,
        iyatta = iyatta,
        tukdi = tukdi,
        rollNo = rollNo
    )

    @Test
    fun reportSorting_allFiveModesWorkAccurately() {
        val list = listOf(
            sampleStudent("10", "सागर पाटील", "मुलगा", rollNo = "10"),
            sampleStudent("2", "अंजली देशमुख", "मुलगी", rollNo = "2"),
            sampleStudent("5", "किरण जोशी", "मुलगा", rollNo = "1"),
            sampleStudent("3", "मीरा कुलकर्णी", "मुलगी", rollNo = "")
        )

        val asis = ReportUtils.sortReportStudents(list, "asis")
        assertEquals(listOf("2", "3", "5", "10"), asis.map { it.regNo })

        val rollSorted = ReportUtils.sortReportStudents(list, "rollno")
        assertEquals(listOf("5", "2", "10", "3"), rollSorted.map { it.regNo })

        val girlsFirst = ReportUtils.sortReportStudents(list, "girlsfirst")
        assertEquals(listOf("2", "3", "5", "10"), girlsFirst.map { it.regNo })

        val boysFirst = ReportUtils.sortReportStudents(list, "boysfirst")
        assertEquals(listOf("5", "10", "2", "3"), boysFirst.map { it.regNo })

        val nameSorted = ReportUtils.sortReportStudents(list, "name")
        assertEquals(4, nameSorted.size)
        assertTrue(nameSorted.first().fullName.startsWith("अंजली"))
    }

    @Test
    fun groupReportStudents_ordersByClassThenDivision() {
        val list = listOf(
            sampleStudent("1", "A", "मुलगा", iyatta = "10th", tukdi = "ब"),
            sampleStudent("2", "B", "मुलगी", iyatta = "5th", tukdi = "ब"),
            sampleStudent("3", "C", "मुलगी", iyatta = "5th", tukdi = "अ")
        )
        val grouped = ReportUtils.groupReportStudents(list, "asis")
        assertEquals(3, grouped.size)
        assertEquals("5th", grouped[0].first.iyatta)
        assertEquals("अ", grouped[0].first.tukdi)
        assertEquals("5th", grouped[1].first.iyatta)
        assertEquals("ब", grouped[1].first.tukdi)
        assertEquals("10th", grouped[2].first.iyatta)
    }

    @Test
    fun rolePermissions_coverAllV1965Roles() {
        val superPages = AppPage.allowedFor(UserRole.SUPER)
        assertTrue(AppPage.USERS in superPages)
        assertTrue(AppPage.SUPERADMIN in superPages)
        assertTrue(AppPage.DATAQC in superPages)
        assertTrue(AppPage.ARCHIVE in superPages)

        val masterPages = AppPage.allowedFor(UserRole.MASTER)
        assertTrue(AppPage.USERS in masterPages)
        assertFalse(AppPage.SUPERADMIN in masterPages)
        assertFalse(AppPage.DATAQC in masterPages)
        assertTrue(AppPage.ARCHIVE in masterPages)

        val deoPages = AppPage.allowedFor(UserRole.DEO)
        assertEquals(AppPage.STUDENT, AppPage.defaultFor(UserRole.DEO))
        assertTrue(AppPage.STUDENT in deoPages)
        assertFalse(AppPage.LC in deoPages)

        val certPages = AppPage.allowedFor(UserRole.CERT)
        assertEquals(AppPage.BONAFIDE, AppPage.defaultFor(UserRole.CERT))
        assertFalse(AppPage.LC in certPages)
        assertTrue(AppPage.BONAFIDE in certPages)
        assertTrue(AppPage.ATTENDANCE in certPages)

        val peonPages = AppPage.allowedFor(UserRole.PEON)
        assertEquals(AppPage.EARLYLEAVE, AppPage.defaultFor(UserRole.PEON))
        assertTrue(AppPage.EARLYLEAVE in peonPages)
        assertFalse(AppPage.DASHBOARD in peonPages)
    }

    @Test
    fun sessionMoneyPermission_matchesRoleRules() {
        val superSess = UserSession("user_s", "super", "Super")
        val masterSess = UserSession("user_1", "master", "Master")
        val teacherSess = UserSession("t1", "teacher", "Teacher")
        val deoSess = UserSession("user_2", "deo", "DEO")
        val certSess = UserSession("user_3", "cert", "Cert")
        val peonSess = UserSession("p1", "peon", "Peon")

        assertTrue(superSess.canSeeMoney)
        assertTrue(masterSess.canSeeMoney)
        assertTrue(teacherSess.canSeeMoney)
        assertFalse(deoSess.canSeeMoney)
        assertFalse(certSess.canSeeMoney)
        assertFalse(peonSess.canSeeMoney)
    }

    @Test
    fun computeAgeAsOf_calculatesExactYears() {
        assertEquals(16, com.example.domain.util.MarathiUtils.computeAgeAsOf("2010-08-15", "2026-09-30"))
        assertEquals(15, com.example.domain.util.MarathiUtils.computeAgeAsOf("2010-10-01", "2026-09-30"))
        assertEquals(null, com.example.domain.util.MarathiUtils.computeAgeAsOf("invalid"))
    }
}
