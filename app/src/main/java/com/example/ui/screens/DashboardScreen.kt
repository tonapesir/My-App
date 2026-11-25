package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AppPage
import com.example.domain.model.HolidayItem
import com.example.domain.model.Student
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
import com.example.ui.components.BannerView
import com.example.ui.components.LocalWhatsAppLauncher
import com.example.ui.components.SgsCard
import com.example.ui.components.SgsDateField
import com.example.ui.components.SgsDropdown
import com.example.ui.components.SgsTextField
import com.example.ui.components.WhatsAppLaunchRequest
import com.example.ui.components.dialPhone
import com.example.ui.theme.ColorTokens
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Emerald
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintGreen
import com.example.ui.theme.Navy
import com.example.ui.theme.Navy2
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftRed
import com.example.ui.theme.TextCream
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.DashUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    role: UserRole,
    state: DashUiState,
    onRefresh: () -> Unit,
    onNavigate: (AppPage) -> Unit,
    onDailyDateChange: (String) -> Unit,
    onLoadDailySummary: () -> Unit,
    onNoticeDraftChange: (String?, String?, String?) -> Unit,
    onSendNotice: () -> Unit,
    onSyncAcYear: (String) -> Unit,
    holidays: List<HolidayItem> = emptyList(),
    onOpenQrScanner: () -> Unit = {},
    onOpenHolidays: () -> Unit = {},
    onOpenBirthdays: () -> Unit = {}
) {
    val context = LocalContext.current
    val launchWa = LocalWhatsAppLauncher.current
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncConfirmInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.sheetCapLevel != "ok" || state.sheetCapPct >= 70.0) {
            Surface(
                color = if (state.sheetCapLevel == "crit" || state.sheetCapPct >= 90.0) ColorTokens.ErrorBg else ColorTokens.WarningBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SoftRed.copy(0.45f)),
                modifier = Modifier.fillMaxWidth().clickable {
                    if (AppPage.DATAQC in AppPage.allowedFor(role)) onNavigate(AppPage.DATAQC)
                }
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = "Sheet Capacity", tint = ColorTokens.WarningText)
                    Text(
                        "⚠️ Sheet क्षमता ${"%.1f".format(state.sheetCapPct)}% भरली आहे (${state.sheetCapLevel.uppercase()}) — Data QC मध्ये पहा.",
                        color = ColorTokens.WarningText, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // School Hero Banner + Academic Year Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, Gold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.img_school_banner),
                    contentDescription = "शाळा बॅनर",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(DeepNavy.copy(0.92f), DeepNavy.copy(0.60f))))
                )
                Column(modifier = Modifier.align(Alignment.CenterStart).padding(16.dp)) {
                    Text("श्री.गोविंदराम सेक्सरिया हायस्कूल, पाचोरा", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("UDISE - 27031201718  |  ${MarathiUtils.todayDisplay()}", color = TextCream, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (state.acYearInfo.mismatchCount > 0) ColorTokens.WarningBg else Navy2,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Gold)
                        ) {
                            Text(
                                "📅 शैक्षणिक वर्ष: ${state.acYearInfo.currentAcYear}" +
                                    if (state.acYearInfo.mismatchCount > 0) " ⚠️ ${state.acYearInfo.mismatchCount}" else "",
                                color = if (state.acYearInfo.mismatchCount > 0) ColorTokens.WarningText else GoldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        val todayIso = remember { MarathiUtils.todayIso() }
                        val todayHoliday = holidays.firstOrNull { it.dateIso == todayIso }
                        if (MarathiUtils.isHolidayOrSunday(todayIso, holidays)) {
                            Surface(
                                color = ColorTokens.WarningBg,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Gold),
                                modifier = Modifier.clickable { onOpenHolidays() }
                            ) {
                                Text(
                                    "🏖️ ${todayHoliday?.title ?: "रविवार सुट्टी"}",
                                    color = ColorTokens.WarningText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (role == UserRole.SUPER && (state.acYearInfo.canSync || state.acYearInfo.mismatchCount > 0)) {
                            OutlinedButton(
                                onClick = { syncConfirmInput = ""; showSyncDialog = true },
                                border = BorderStroke(1.dp, GoldLight),
                                modifier = Modifier.testTag("dash_sync_year_btn")
                            ) {
                                Text("वर्ष जुळवा", color = GoldLight, fontSize = 11.sp)
                            }
                        }
                    }
                }
                IconButtonSafeRefresh(onClick = onRefresh, loading = state.loading, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }

        if (state.acYearInfo.breakdown.isNotEmpty() || state.acYearInfo.sample.isNotEmpty()) {
            val matchCount = (state.acYearInfo.studentsCount - state.acYearInfo.mismatchCount).coerceAtLeast(0)
            SgsCard(title = "🗓️ शैक्षणिक वर्ष पडताळणी (${state.acYearInfo.currentAcYear})") {
                Text(
                    "एकूण: ${state.acYearInfo.studentsCount} | जुळणारे: $matchCount | न जुळणारे: ${state.acYearInfo.mismatchCount}",
                    color = if (state.acYearInfo.mismatchCount > 0) ColorTokens.WarningText else MintGreen,
                    fontSize = 12.5.sp, fontWeight = FontWeight.Bold
                )
                if (state.acYearInfo.breakdown.isNotEmpty()) {
                    Text(
                        state.acYearInfo.breakdown.entries.joinToString(" | ") { "${it.key}: ${it.value}" },
                        color = TextMuted, fontSize = 11.5.sp
                    )
                }
                if (state.acYearInfo.sample.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    state.acYearInfo.sample.take(5).forEach { s ->
                        Text("• ${s.regNo} — ${s.name} (${s.year})", color = SoftRed, fontSize = 11.sp)
                    }
                }
            }
        }

        BannerView(state.syncStatus)

        // Live Stats Cards
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3
        ) {
            StatPill("एकूण विद्यार्थी", state.stats.totalStudents.toString(), GoldLight, Modifier.weight(1f))
            StatPill("मुले", state.stats.totalBoys.toString(), MintGreen, Modifier.weight(1f))
            StatPill("मुली", state.stats.totalGirls.toString(), Gold, Modifier.weight(1f))
            StatPill("आजचे LC", state.stats.todayLC.toString(), TextCream, Modifier.weight(1f))
            StatPill("आजचे बोनाफाईड", state.stats.todayBF.toString(), TextCream, Modifier.weight(1f))
            StatPill("महिना प्रमाणपत्रे", state.stats.monthCerts.toString(), MintGreen, Modifier.weight(1f))
        }

        // Quick Actions Grid — ALL Allowed Menu Items + Smart Utilities
        SgsCard(title = "⚡ जलद कृती — सर्व मेनू (Quick Actions)") {
            val quickPages = AppPage.allowedFor(role).filter { it != AppPage.DASHBOARD }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                Surface(
                    color = Navy2,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Gold.copy(0.45f)),
                    modifier = Modifier.weight(1f).clickable { onOpenQrScanner() }.testTag("dash_quick_qr")
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 20.sp)
                        Text("QR स्कॅनर", color = GoldLight, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
                Surface(
                    color = Navy2,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Gold.copy(0.45f)),
                    modifier = Modifier.weight(1f).clickable { onOpenHolidays() }.testTag("dash_quick_holidays")
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅", fontSize = 20.sp)
                        Text("सुट्टी कॅलेंडर", color = GoldLight, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
                Surface(
                    color = Navy2,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Gold.copy(0.45f)),
                    modifier = Modifier.weight(1f).clickable { onOpenBirthdays() }.testTag("dash_quick_birthdays")
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎂", fontSize = 20.sp)
                        Text("आजचे वाढदिवस", color = GoldLight, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
                quickPages.forEach { pg ->
                    Surface(
                        color = Navy2,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Gold.copy(0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(pg) }
                            .testTag("dash_quick_${pg.key}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(pg.emoji, fontSize = 20.sp)
                            Text(pg.titleMr, color = TextCream, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Daily Summary (रोजचा सारांश)
        SgsCard(title = "📅 रोजचा सारांश (Daily Summary)") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    SgsDateField("तारीख", state.dailyDate, onDateSelected = onDailyDateChange, tag = "dash_daily_date")
                }
                Button(
                    onClick = onLoadDailySummary,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                    modifier = Modifier.height(50.dp).testTag("dash_daily_load_btn")
                ) {
                    Text("पहा", fontWeight = FontWeight.Bold)
                }
            }
            if (state.dailyLoading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Gold)
            }
            if (!state.dailyError.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(state.dailyError, color = SoftRed, fontSize = 12.sp)
            }
            state.dailySummary?.let { ds ->
                Spacer(Modifier.height(10.dp))
                if (ds.message.isNotBlank()) {
                    Text("ℹ️ ${ds.message}", color = GoldLight, fontSize = 12.5.sp)
                }
                Text(
                    "✅ उपस्थिती: ${ds.present}/${ds.total}" + (ds.pct?.let { " ($it%)" } ?: "") +
                        " | गैरहजर: ${ds.absent} | 💰 फी जमा: ${MarathiUtils.fmtRupees(ds.feesTotal)} (${ds.feesCount}) | 🚪 घरी गेले: ${ds.elGone}/${ds.elTotal}",
                    color = TextCream, fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
                if (ds.elPermitted > 0 || ds.elNotGone > 0 || ds.elOther > 0) {
                    Text("🚪 परवानगी: ${ds.elPermitted} | न गेलेले: ${ds.elNotGone} | इतर: ${ds.elOther}", color = TextMuted, fontSize = 11.5.sp)
                }
                if (ds.feesByClass.isNotEmpty()) {
                    Text("💰 वर्गनिहाय फी: " + ds.feesByClass.joinToString(", ") { "${it.first}: ₹${it.second.toInt()}" }, color = GoldLight, fontSize = 11.5.sp)
                }
                if (ds.noTeacherClasses.isNotEmpty()) {
                    Text("⚠️ शिक्षक न नेमलेले वर्ग: " + ds.noTeacherClasses.joinToString(", ") { "${MarathiUtils.classToMr(it.iyatta)} ${it.tukdi}" }, color = SoftRed, fontSize = 11.5.sp)
                }
                if (ds.changesTotal > 0 || ds.changesStudents > 0) {
                    Text("✏️ आजचे बदल: ${ds.changesTotal} नोंदी (${ds.changesStudents} विद्यार्थी)", color = TextMuted, fontSize = 11.5.sp)
                }
                if (ds.noticeTitles.isNotEmpty()) {
                    Text("📢 सूचना (${ds.noticesCount}): " + ds.noticeTitles.joinToString(" | "), color = MintGreen, fontSize = 11.5.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Daily Summary", ds.messageText))
                        },
                        modifier = Modifier.testTag("dash_daily_copy_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextCream)
                        Spacer(Modifier.size(4.dp))
                        Text("Copy", color = TextCream, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            launchWa(
                                WhatsAppLaunchRequest(
                                    mobile = "",
                                    defaultMessage = ds.messageText,
                                    recipientLabel = "रोजचा अहवाल"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                        modifier = Modifier.testTag("dash_daily_wa_btn")
                    ) {
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val headers = listOf("वर्ग", "एकूण", "हजर", "गैरहजर", "%")
                            val rows = ds.classRows.map {
                                listOf("${MarathiUtils.classToMr(it.iyatta)} ${it.tukdi}", it.total.toString(), it.present.toString(), it.absent.toString(), it.pct?.toString() ?: "—")
                            }
                            val pdf = PdfAndExportUtils.renderTablePdf(
                                context,
                                "रोजचा सारांश — ${MarathiUtils.fmtDate(ds.date)}",
                                "उपस्थिती ${ds.present}/${ds.total} | फी जमा ${MarathiUtils.fmtRupees(ds.feesTotal)}",
                                headers, rows, "Daily_Summary_${ds.date}.pdf"
                            )
                            PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Daily Summary PDF")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.testTag("dash_daily_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("PDF", fontSize = 12.sp)
                    }
                }
            }
        }

        // Pending / Done Attendance Today
        if (state.attPendingIsSunday) {
            SgsCard(title = "📅 आज रविवार / सुट्टी आहे") {
                Text("आज नियमित हजेरी अपेक्षित नाही.", color = GoldLight, fontSize = 13.sp)
            }
        } else if (state.pendingAtt.isNotEmpty() || state.doneAtt.isNotEmpty() || state.unassignedAtt.isNotEmpty()) {
            SgsCard(title = "⏳ आजची हजेरी स्थिती (पूर्ण: ${state.doneAtt.size}/${state.attPendingTotalExpected} | बाकी: ${state.pendingAtt.size})") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.pendingAtt.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Navy2, RoundedCornerShape(8.dp)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${MarathiUtils.classToMr(p.iyatta)} ${p.tukdi}", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text(p.teacher.ifBlank { "वर्ग शिक्षक नेमलेले नाहीत" }, color = TextMuted, fontSize = 12.sp)
                                if (p.mobile.isNotBlank()) {
                                    Text("📞 ${p.mobile}", color = MintGreen, fontSize = 11.5.sp)
                                }
                            }
                            if (p.mobile.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { dialPhone(context, p.mobile) }) {
                                        Icon(Icons.Default.Call, contentDescription = "कॉल ${p.teacher}", tint = MintGreen)
                                    }
                                    Button(
                                        onClick = {
                                            val clsLbl = "${MarathiUtils.classToMr(p.iyatta)} ${p.tukdi}".trim()
                                            val remMsg = "नमस्कार ${p.teacher}, कृपया $clsLbl ची आजची हजेरी ॲपवर भरावी. — श्री.गो.से.हायस्कूल, पाचोरा"
                                            launchWa(
                                                WhatsAppLaunchRequest(
                                                    mobile = p.mobile,
                                                    defaultMessage = remMsg,
                                                    recipientLabel = p.teacher.ifBlank { clsLbl },
                                                    student = Student(fullName = p.teacher.ifBlank { clsLbl }, iyatta = p.iyatta, tukdi = p.tukdi, whatsappMobile = p.mobile),
                                                    customClassLabel = clsLbl
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                                    ) { Text("💬 WA", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                    if (state.unassignedAtt.isNotEmpty()) {
                        Text(
                            "⚠️ वर्ग शिक्षक नेमलेले नाहीत: " + state.unassignedAtt.joinToString(", ") { "${MarathiUtils.classToMr(it.iyatta)} ${it.tukdi}" },
                            color = SoftRed, fontSize = 11.5.sp
                        )
                    }
                    if (state.doneAtt.isNotEmpty()) {
                        Text(
                            "✅ हजेरी पूर्ण (${state.doneAtt.size}): " + state.doneAtt.joinToString(", ") {
                                "${MarathiUtils.classToMr(it.iyatta)} ${it.tukdi}${if (it.time.isNotBlank()) " (${it.time})" else ""}"
                            },
                            color = MintGreen, fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        // Class-wise Bar Chart
        if (state.stats.classCounts.isNotEmpty()) {
            SgsCard(title = "📊 वर्गनिहाय विद्यार्थी संख्या") {
                val maxVal = (state.stats.classCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarathiUtils.CLASSES.forEach { cls ->
                        val cnt = state.stats.classCounts[cls] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(MarathiUtils.classToMr(cls), color = TextCream, fontWeight = FontWeight.Bold, modifier = Modifier.size(width = 58.dp, height = 20.dp))
                            LinearProgressIndicator(
                                progress = { cnt.toFloat() / maxVal.toFloat() },
                                color = Gold,
                                trackColor = Navy2,
                                modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp))
                            )
                            Text(" $cnt", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Religion, LC Reasons & Admissions Breakdown
        if (state.religionCounts.isNotEmpty() || state.lcReasonCounts.isNotEmpty() || state.yearAdmissions.isNotEmpty()) {
            SgsCard(title = "📈 धर्म, दाखला कारणे व प्रवेश आकडेवारी") {
                if (state.religionCounts.isNotEmpty()) {
                    Text("धर्मनिहाय:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    val maxRel = (state.religionCounts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                    state.religionCounts.take(6).forEach { (rel, cnt) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(rel, color = TextCream, fontSize = 11.5.sp, modifier = Modifier.size(width = 88.dp, height = 18.dp))
                            LinearProgressIndicator(progress = { cnt.toFloat() / maxRel }, color = MintGreen, trackColor = Navy2, modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)))
                            Text(" $cnt", color = MintGreen, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (state.lcReasonCounts.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("LC कारणे:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    Text(state.lcReasonCounts.joinToString(" | ") { "${it.first}: ${it.second}" }, color = TextCream, fontSize = 11.5.sp)
                }
                if (state.yearAdmissions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("प्रवेश वर्षनिहाय:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    Text(state.yearAdmissions.joinToString(" | ") { "${it.first}: ${it.second}" }, color = TextMuted, fontSize = 11.5.sp)
                }
            }
        }

        // Birthdays Today
        SgsCard(title = "🎂 आजचे वाढदिवस (${state.birthdays.size})") {
            if (state.birthdays.isEmpty()) {
                Text("आज कोणाचाही वाढदिवस नाही.", color = TextMuted, fontSize = 13.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.birthdays.forEach { b ->
                        val phone = b.whatsappMobile.ifBlank { b.alternateMobile }.ifBlank { b.contact }
                        val clsLabel = "${MarathiUtils.classToMr(b.iyatta)} ${b.tukdi}".trim()
                        val wishMsg = "🎂 वाढदिवसाच्या हार्दिक शुभेच्छा ${b.fullName} ($clsLabel)! तुम्हाला उज्ज्वल यश व उत्तम आरोग्य लाभो हीच सदिच्छा! — श्री.गोविंदराम सेक्सरिया हायस्कूल, पाचोरा"
                        val bStu = Student(regNo = b.regNo, iyatta = b.iyatta, tukdi = b.tukdi, rollNo = b.rollNo, fullName = b.fullName, whatsappMobile = phone)
                        Column(
                            modifier = Modifier.fillMaxWidth().background(Navy2, RoundedCornerShape(8.dp)).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Cake, contentDescription = null, tint = GoldLight)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(b.fullName, color = TextCream, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "$clsLabel • Roll ${b.rollNo}" + if (phone.isNotBlank()) " • 📱 $phone" else "",
                                        color = TextMuted,
                                        fontSize = 11.5.sp
                                    )
                                }
                                if (phone.isNotBlank()) {
                                    IconButton(onClick = { dialPhone(context, phone) }) {
                                        Icon(Icons.Default.Call, contentDescription = "कॉल ${b.fullName}", tint = MintGreen)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        launchWa(
                                            WhatsAppLaunchRequest(
                                                mobile = phone,
                                                defaultMessage = wishMsg,
                                                recipientLabel = b.fullName,
                                                student = bStu,
                                                customClassLabel = clsLabel
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (phone.isNotBlank()) "💬 शुभेच्छा" else "💬 WA निवडा", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val card = PdfAndExportUtils.createBirthdayGreetingCard(context, b.fullName, clsLabel)
                                        PdfAndExportUtils.shareFile(context, card, "image/png", "वाढदिवस शुभेच्छा कार्ड")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🎨 कार्ड Share", color = GoldLight, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Notice Board & Send Notice
        SgsCard(title = "📢 सूचना फलक / नवीन नोटीस पाठवा") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(state.noticeTitle, { onNoticeDraftChange(it, null, null) }, "नोटीस शीर्षक", tag = "dash_notice_title", enableVoice = true)
                SgsTextField(state.noticeMessage, { onNoticeDraftChange(null, it, null) }, "संदेश", singleLine = false, tag = "dash_notice_msg", enableVoice = true)
                SgsDropdown(
                    label = "लक्ष्य वर्ग",
                    selected = state.noticeTarget,
                    options = listOf("ALL") + MarathiUtils.CLASSES,
                    onSelect = { onNoticeDraftChange(null, null, it) },
                    displayTransform = { if (it == "ALL") "सर्व वर्ग" else MarathiUtils.classToMr(it) }
                )
                Button(
                    onClick = onSendNotice,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dash_send_notice_btn")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("नोटीस पाठवा", fontWeight = FontWeight.Bold)
                }
                BannerView(state.noticeStatus)
                if (state.notices.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    state.notices.take(5).forEach { n ->
                        Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${n.title} (${MarathiUtils.fmtDate(n.date)})", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(n.message, color = TextCream, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            containerColor = Navy,
            titleContentColor = GoldLight,
            textContentColor = TextCream,
            title = { Text("🗓️ सर्व विद्यार्थ्यांचे शैक्षणिक वर्ष जुळवा", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${state.acYearInfo.mismatchCount} विद्यार्थ्यांचे वर्ष '${state.acYearInfo.currentAcYear}' केले जाईल. खात्रीसाठी खाली SYNC टाईप करा.")
                    SgsTextField(syncConfirmInput, { syncConfirmInput = it }, "SYNC टाईप करा", tag = "dash_sync_confirm_input")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSyncDialog = false
                        onSyncAcYear(state.acYearInfo.currentAcYear)
                    },
                    enabled = syncConfirmInput.trim().equals("SYNC", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    modifier = Modifier.testTag("dash_sync_confirm_btn")
                ) { Text("जुळवा") }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) { Text("रद्द", color = SoftRed) }
            }
        )
    }
}

@Composable
private fun StatPill(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        color = Navy,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Navy2),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(label, color = TextMuted, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun IconButtonSafeRefresh(onClick: () -> Unit, loading: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = DeepNavy.copy(alpha = 0.75f),
        shape = RoundedCornerShape(50),
        modifier = modifier.clickable(enabled = !loading) { onClick() }.testTag("dash_refresh_btn")
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = GoldLight)
            else Icon(Icons.Default.Refresh, contentDescription = "रिफ्रेश", tint = GoldLight, modifier = Modifier.size(20.dp))
        }
    }
}
