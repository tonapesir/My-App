package com.example.domain.util

import com.example.domain.model.ClassDivisionPair
import com.example.domain.model.Student
import java.text.Collator
import java.util.Locale

object ReportUtils {

    fun compareNatural(a: String, b: String): Int {
        val na = a.trim().toLongOrNull()
        val nb = b.trim().toLongOrNull()
        if (na != null && nb != null) return na.compareTo(nb)
        return a.trim().compareTo(b.trim(), ignoreCase = true)
    }

    private fun isFemale(gender: String): Boolean {
        val g = gender.trim().lowercase()
        return g == "female" || g == "मुलगी" || g == "girl" || g == "स्त्री"
    }

    private fun isMale(gender: String): Boolean {
        val g = gender.trim().lowercase()
        return g == "male" || g == "मुलगा" || g == "boy" || g == "पुरुष"
    }

    fun sortReportStudents(list: List<Student>, order: String): List<Student> {
        val byReg = Comparator<Student> { a, b -> compareNatural(a.regNo, b.regNo) }
        return when (order) {
            "rollno" -> list.sortedWith { a, b ->
                val ra = a.rollNo.trim().toDoubleOrNull()
                val rb = b.rollNo.trim().toDoubleOrNull()
                when {
                    ra == null && rb == null -> compareNatural(a.regNo, b.regNo)
                    ra == null -> 1
                    rb == null -> -1
                    else -> {
                        val cmp = ra.compareTo(rb)
                        if (cmp != 0) cmp else compareNatural(a.regNo, b.regNo)
                    }
                }
            }
            "name" -> {
                val collator = Collator.getInstance(Locale.forLanguageTag("mr-IN"))
                list.sortedWith { a, b ->
                    val c = collator.compare(a.fullName.trim(), b.fullName.trim())
                    if (c != 0) c else compareNatural(a.regNo, b.regNo)
                }
            }
            "girlsfirst", "boysfirst" -> {
                val girls = list.filter { isFemale(it.gender) }.sortedWith(byReg)
                val boys = list.filter { isMale(it.gender) }.sortedWith(byReg)
                val others = list.filter { !isFemale(it.gender) && !isMale(it.gender) }.sortedWith(byReg)
                if (order == "girlsfirst") girls + boys + others else boys + girls + others
            }
            else -> list.sortedWith(byReg)
        }
    }

    fun groupReportStudents(
        rows: List<Student>,
        sortOrder: String
    ): List<Pair<ClassDivisionPair, List<Student>>> {
        val grouped = rows.groupBy { ClassDivisionPair(it.iyatta.trim(), it.tukdi.trim()) }
        val sortedKeys = grouped.keys.sortedWith { a, b ->
            val ca = MarathiUtils.CLASSES.indexOf(a.iyatta).let { if (it == -1) 99 else it }
            val cb = MarathiUtils.CLASSES.indexOf(b.iyatta).let { if (it == -1) 99 else it }
            if (ca != cb) return@sortedWith ca - cb
            val da = MarathiUtils.DIVISIONS.indexOf(a.tukdi).let { if (it == -1) 99 else it }
            val db = MarathiUtils.DIVISIONS.indexOf(b.tukdi).let { if (it == -1) 99 else it }
            if (da != db) da - db else a.tukdi.compareTo(b.tukdi)
        }
        return sortedKeys.map { key ->
            key to sortReportStudents(grouped[key].orEmpty(), sortOrder)
        }
    }
}
