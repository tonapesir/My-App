package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClassDivisionPair
import com.example.domain.model.Student
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
import com.example.ui.components.MobileCallWaButtons
import com.example.ui.components.SgsCard
import com.example.ui.components.SgsDropdown
import com.example.ui.components.SgsTextField
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Emerald
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintGreen
import com.example.ui.theme.Navy2
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftRed
import com.example.ui.theme.TextCream
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.AdminAndQcUiState
import com.example.ui.viewmodel.ReportUiState
import com.example.ui.viewmodel.ReportsAndAdminDelegate
import com.example.ui.viewmodel.StatsUiState
import com.example.ui.viewmodel.TeacherAttStreakStudent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportBuilderScreen(
    role: UserRole,
    state: ReportUiState,
    admin: ReportsAndAdminDelegate
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgsCard(title = "📑 कस्टम रिपोर्ट तयार करा") {
            if (role != UserRole.TEACHER) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("इयत्ता निवडा:", color = TextMuted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { admin.setAllReportClasses(true) }) { Text("सर्व", fontSize = 11.sp, color = GoldLight) }
                        OutlinedButton(onClick = { admin.setAllReportClasses(false) }) { Text("नाही", fontSize = 11.sp, color = TextMuted) }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MarathiUtils.CLASSES.forEach { cls ->
                        FilterChip(
                            selected = cls in state.selectedClasses,
                            onClick = { admin.toggleReportClass(cls) },
                            label = { Text(MarathiUtils.classToMr(cls), fontSize = 11.5.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("तुकडी निवडा:", color = TextMuted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { admin.setAllReportDivisions(true) }) { Text("सर्व", fontSize = 11.sp, color = GoldLight) }
                        OutlinedButton(onClick = { admin.setAllReportDivisions(false) }) { Text("नाही", fontSize = 11.sp, color = TextMuted) }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MarathiUtils.DIVISIONS.forEach { div ->
                        FilterChip(
                            selected = div in state.selectedDivisions,
                            onClick = { admin.toggleReportDivision(div) },
                            label = { Text(div, fontSize = 11.5.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            SgsTextField(state.purpose, { admin.updateReportConfig(purpose = it) }, "रिपोर्ट शीर्षक / उद्देश", tag = "report_purpose_input")
            Spacer(Modifier.height(8.dp))

            Text("कॉलम व क्रम (▲/▼):", color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val sortedFields = state.fields.sortedBy { it.order }
                sortedFields.forEachIndexed { idx, f ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = f.selected,
                            onClick = { admin.toggleReportField(f.key) },
                            label = { Text(f.label, fontSize = 11.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        if (f.selected) {
                            IconButton(onClick = { admin.moveReportField(f.key, -1) }, enabled = idx > 0) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "वर", tint = GoldLight, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { admin.moveReportField(f.key, 1) }, enabled = idx < sortedFields.lastIndex) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "खाली", tint = GoldLight, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsDropdown(
                    "क्रम (Sort)",
                    state.sortField,
                    listOf("asis", "girlsfirst", "boysfirst", "name", "rollno"),
                    { admin.updateReportConfig(sortField = it) },
                    displayTransform = {
                        when (it) {
                            "girlsfirst" -> "मुली प्रथम"
                            "boysfirst" -> "मुले प्रथम"
                            "name" -> "नावानुसार"
                            "rollno" -> "रोल नं."
                            else -> "मूळ क्रम"
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                SgsDropdown(
                    "बाकी फी फिल्टर",
                    state.feeDueFilter,
                    listOf("", "nonzero", "zero"),
                    { admin.updateReportConfig(feeDueFilter = it) },
                    displayTransform = { when (it) { "nonzero" -> "बाकी असलेले"; "zero" -> "पूर्ण जमा"; else -> "सर्व" } },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Landscape A4 PDF", color = TextCream, fontSize = 13.sp)
                Switch(checked = state.landscapePdf, onCheckedChange = { admin.updateReportConfig(landscapePdf = it) })
            }
            if (!state.error.isNullOrBlank()) Text(state.error, color = SoftRed, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = admin::runReport, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.weight(1f).testTag("report_run_btn")) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("पहा", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val pdf = if (state.groupedRows.isNotEmpty()) {
                            PdfAndExportUtils.renderGroupedReportPdf(
                                context, state.purpose, state.groupedRows, state.headers,
                                "SGS_Report_${MarathiUtils.todayIso()}.pdf", state.landscapePdf
                            )
                        } else {
                            PdfAndExportUtils.renderTablePdf(
                                context, state.purpose.ifBlank { "शाळा विद्यार्थी रिपोर्ट" },
                                "एकूण नोंदी: ${state.rows.size} | दिनांक: ${MarathiUtils.todayDisplay()}",
                                state.headers, state.rows, "SGS_Report_${MarathiUtils.todayIso()}.pdf", state.landscapePdf
                            )
                        }
                        PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Report PDF Share करा")
                    },
                    enabled = state.rows.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier.weight(1f).testTag("report_pdf_btn")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("PDF")
                }
                Button(
                    onClick = {
                        val csv = PdfAndExportUtils.writeCsvFile(context, "SGS_Report_${MarathiUtils.todayIso()}.csv", state.headers, state.rows)
                        PdfAndExportUtils.shareFile(context, csv, "text/csv", "Report CSV Share")
                    },
                    enabled = state.rows.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    modifier = Modifier.weight(1f).testTag("report_csv_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("CSV")
                }
            }
        }

        if (state.loading) CircularProgressIndicator(color = Gold)

        if (state.rows.isNotEmpty()) {
            SgsCard(title = "📋 निकाल (${state.rows.size} विद्यार्थी • ${state.groupedRows.size} वर्ग)") {
                state.rows.take(80).forEach { r ->
                    Text(r.joinToString(" | "), color = TextCream, fontSize = 11.5.sp, modifier = Modifier.padding(vertical = 3.dp))
                }
                if (state.rows.size > 80) Text("... आणि आणखी ${state.rows.size - 80} नोंदी (संपूर्ण यादीसाठी PDF/CSV काढा).", color = GoldLight, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatsReportScreen(state: StatsUiState, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgsCard(title = "📊 सांख्यिकी अहवाल") {
            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.fillMaxWidth().testTag("stats_refresh_btn")) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("ताजी माहिती Load करा")
            }
            if (state.loading) CircularProgressIndicator(color = Gold, modifier = Modifier.padding(top = 8.dp))
            if (!state.error.isNullOrBlank()) Text(state.error, color = SoftRed)
        }

        if (state.classes.isNotEmpty()) {
            SgsCard(title = "✅ १. वर्गनिहाय हजेरी व जमा फी") {
                state.classes.forEach { c ->
                    val pres = c.total - c.absent
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${MarathiUtils.classToMr(c.iyatta)} ${c.tukdi}", color = GoldLight, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("हजर $pres/${c.total}", color = MintGreen, fontSize = 12.sp)
                        Spacer(Modifier.size(8.dp))
                        Text(MarathiUtils.fmtRupees(c.fees), color = TextCream, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }

            SgsCard(title = "🧬 २. प्रवर्गनिहाय विद्यार्थी (Category Matrix)") {
                state.classes.forEach { c ->
                    Text("${MarathiUtils.classToMr(c.iyatta)} ${c.tukdi}:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    val cats = MarathiUtils.CATEGORIES.mapNotNull { cat ->
                        val fc = c.femaleCat[cat] ?: 0
                        val mc = c.maleCat[cat] ?: 0
                        if (fc + mc > 0) "$cat: ${mc}मुले/${fc}मुली" else null
                    }
                    Text(cats.joinToString(", ").ifBlank { "नोंद नाही" }, color = TextCream, fontSize = 11.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
            }

            SgsCard(title = "🎂 ३. वयानुरूप वर्गनिहाय (30-09-2026 नुसार)") {
                state.classes.forEach { c ->
                    val ageLine = c.ageCounts.entries.sortedBy { it.key }.joinToString(", ") { "${it.key}व: ${it.value}" }
                    Text("${MarathiUtils.classToMr(c.iyatta)} ${c.tukdi}: ${ageLine.ifBlank { "—" }}", color = TextCream, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun AttendanceAnalyticsScreen(
    classes: List<ClassDivisionPair>,
    month: String,
    overview: List<List<String>>,
    students: List<TeacherAttStreakStudent>,
    streaks: List<TeacherAttStreakStudent>,
    summary: String,
    admin: ReportsAndAdminDelegate
) {
    val context = LocalContext.current
    var selectedClass by remember { mutableStateOf("") }
    var monthInput by remember { mutableStateOf(month) }
    var minRunInput by remember { mutableStateOf("3") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SgsCard(title = "📈 शाळा हजेरी विश्लेषण") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsDropdown(
                    label = "वर्ग निवडा",
                    selected = selectedClass,
                    options = classes.map { it.key },
                    onSelect = { selectedClass = it },
                    displayTransform = { k ->
                        val p = k.split("|")
                        if (p[0].isBlank()) "— निवडा —" else "${MarathiUtils.classToMr(p[0])} ${p.getOrNull(1).orEmpty()}"
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(monthInput, { monthInput = it }, "महिना (YYYY-MM)", modifier = Modifier.weight(1f))
                    SgsTextField(minRunInput, { minRunInput = it }, "सलग किमान दिवस", modifier = Modifier.weight(0.7f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val p = selectedClass.split("|")
                            admin.loadAttendanceAnalytics(p.getOrNull(0).orEmpty(), p.getOrNull(1).orEmpty(), monthInput, minRunInput.toIntOrNull() ?: 3)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                        modifier = Modifier.weight(1f)
                    ) { Text("विश्लेषण पहा", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = {
                            val headers = listOf("Roll", "नाव", "कार्यदिवस", "गैरहजर", "%", "सलग")
                            val rows = students.map { listOf(it.rollNo, it.fullName, it.workingDays.toString(), it.absent.toString(), it.pct?.let { p -> "$p%" } ?: "—", it.maxRun.toString()) }
                            val pdf = PdfAndExportUtils.renderTablePdf(context, "हजेरी विश्लेषण ($monthInput)", summary, headers, rows, "Att_Analytics_$monthInput.pdf")
                            PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Attendance PDF")
                        },
                        enabled = students.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.weight(1f)
                    ) { Text("PDF काढा") }
                }
            }
        }

        if (overview.isNotEmpty()) {
            SgsCard(title = "🏫 सर्व वर्ग सारांश ($monthInput)") {
                overview.forEach { row ->
                    Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.getOrNull(0).orEmpty(), color = GoldLight, fontWeight = FontWeight.Bold)
                                Text("विद्यार्थी: ${row.getOrNull(1).orEmpty()} | दिवस: ${row.getOrNull(2).orEmpty()} | ${row.getOrNull(3).orEmpty()}", color = MintGreen, fontSize = 12.sp)
                            }
                            Text(
                                "गैरहजर: ${row.getOrNull(4).orEmpty()} | शेवटची ता.: ${row.getOrNull(5).orEmpty().ifBlank { "—" }} | आज गैरहजर: ${row.getOrNull(6).orEmpty()}",
                                color = TextMuted, fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }

        if (streaks.isNotEmpty()) {
            SgsCard(title = "⚠️ सलग गैरहजर विद्यार्थी (${streaks.size})") {
                streaks.forEach { st ->
                    Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("${st.fullName} — सलग ${st.maxRun} दिवस", color = SoftRed, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                if (st.mobile.isNotBlank()) {
                                    Text("📞 ${st.mobile}", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                            MobileCallWaButtons(
                                mobile = st.mobile,
                                recipientLabel = st.fullName,
                                defaultMessage = "नमस्कार, आपला पाल्य ${st.fullName} सलग ${st.maxRun} दिवस शाळेत गैरहजर आहे. कृपया कारण कळवा. — श्री. गो. से. हायस्कूल, पाचोरा",
                                student = Student(tukdi = st.tukdi, rollNo = st.rollNo, fullName = st.fullName, whatsappMobile = st.mobile)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveScreen(
    role: UserRole,
    state: AdminAndQcUiState,
    admin: ReportsAndAdminDelegate
) {
    val context = LocalContext.current
    var cmpA by remember(state.archiveCompareYearA) { mutableStateOf(state.archiveCompareYearA) }
    var cmpB by remember(state.archiveCompareYearB) { mutableStateOf(state.archiveCompareYearB) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SgsCard(title = "📦 जुने शैक्षणिक वर्ष (Archive — फक्त पाहण्यासाठी)") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val badgeMap = state.archiveYearItems.associate {
                    it.year to buildString {
                        append(it.year)
                        if (it.current) append(" [चालू]")
                        if (it.hasFile) append(" [Backup File]")
                        else if (it.archived) append(" [Archive]")
                    }
                }
                SgsDropdown(
                    "वर्ष निवडा",
                    state.selectedArchiveYear,
                    state.archiveYears,
                    { admin.loadArchiveReport(it, state.archiveSection) },
                    displayTransform = { badgeMap[it] ?: it }
                )
                SgsDropdown(
                    "विभाग निवडा",
                    state.archiveSection,
                    listOf("fees", "transport", "att", "el", "sch"),
                    { admin.loadArchiveReport(state.selectedArchiveYear, it) },
                    displayTransform = {
                        when (it) {
                            "fees" -> "💰 फी नोंदी"
                            "transport" -> "🚌 प्रवास"
                            "att" -> "✅ हजेरी"
                            "el" -> "🚪 घरी जाणारे"
                            "sch" -> "🎓 शिष्यवृत्ती"
                            else -> it
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { admin.checkArchiveHealth(state.selectedArchiveYear) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.weight(1f)
                    ) { Text("🩺 आरोग्य", fontSize = 12.sp) }
                    if (role == UserRole.SUPER) {
                        Button(
                            onClick = { admin.backupArchiveFile(state.selectedArchiveYear) },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                            modifier = Modifier.weight(1f)
                        ) { Text("💾 Backup प्रत", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                    Button(
                        onClick = {
                            val csv = PdfAndExportUtils.writeCsvFile(context, "Archive_${state.selectedArchiveYear}_${state.archiveSection}.csv", state.archiveHeaders, state.archiveRows)
                            PdfAndExportUtils.shareFile(context, csv, "text/csv", "Archive CSV Share")
                        },
                        enabled = state.archiveRows.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("CSV", fontSize = 12.sp)
                    }
                }
                if (state.archiveHealthText.isNotBlank()) Text(state.archiveHealthText, color = MintGreen, fontSize = 12.sp)
            }
        }

        if (state.archiveYears.size >= 2) {
            SgsCard(title = "📊 दोन वर्षांची तुलना (Compare)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("वर्ष A", cmpA, state.archiveYears, { cmpA = it }, modifier = Modifier.weight(1f))
                    SgsDropdown("वर्ष B", cmpB, state.archiveYears, { cmpB = it }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { admin.compareArchiveYears(cmpA, cmpB) },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier.fillMaxWidth().testTag("archive_compare_btn")
                ) { Text("तुलना पहा") }
            }
        }

        if (state.archiveLoading) CircularProgressIndicator(color = Gold)

        SgsCard(title = "📋 Archive नोंदी (${state.archiveRows.size})") {
            if (state.archiveSummary.isNotBlank()) {
                Text(state.archiveSummary, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
            }
            if (state.archiveRows.isEmpty()) Text("नोंदी उपलब्ध नाहीत.", color = TextMuted)
            else state.archiveRows.take(80).forEach { r ->
                Text(r.joinToString(" | "), color = TextCream, fontSize = 11.5.sp, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
    }
}
