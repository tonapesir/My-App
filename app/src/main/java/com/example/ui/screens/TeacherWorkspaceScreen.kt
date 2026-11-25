package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppPage
import com.example.domain.model.HolidayItem
import com.example.domain.model.MessageTemplate
import com.example.domain.model.Student
import com.example.domain.model.TeacherTab
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
import com.example.ui.components.BannerView
import com.example.ui.components.ClassBroadcastQueueDialog
import com.example.ui.components.LocalWhatsAppLauncher
import com.example.ui.components.MobileCallWaButtons
import com.example.ui.components.SgsCard
import com.example.ui.components.SgsDateField
import com.example.ui.components.SgsDropdown
import com.example.ui.components.SgsTextField
import com.example.ui.components.WhatsAppLaunchRequest
import com.example.ui.components.dialPhone
import com.example.ui.components.openWhatsApp
import com.example.ui.components.sendSms
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
import com.example.ui.viewmodel.TeacherAndOpsDelegate
import com.example.ui.viewmodel.TeacherUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherWorkspaceScreen(
    session: UserSession,
    activeTab: TeacherTab,
    state: TeacherUiState,
    ops: TeacherAndOpsDelegate,
    verifyBaseUrl: String = "",
    acYear: String = "2026-27",
    onSelectTab: (TeacherTab) -> Unit,
    onOpenStudentProfile: (String) -> Unit,
    holidays: List<HolidayItem> = emptyList(),
    onOpenPage: (AppPage) -> Unit = {},
    onOpenQrScanner: () -> Unit = {},
    onOpenHolidays: () -> Unit = {},
    templates: List<MessageTemplate> = emptyList(),
    onOpenManageTemplates: () -> Unit = {}
) {
    val context = LocalContext.current
    val launchWa = LocalWhatsAppLauncher.current
    var showBulkWaQueue by remember { mutableStateOf(false) }
    var bulkWaIndex by remember { mutableIntStateOf(0) }
    var bulkWaTplId by remember { mutableStateOf("__default__") }
    var feeFilter by remember { mutableStateOf("all") }
    var showFeeBulkWaQueue by remember { mutableStateOf(false) }
    var feeBulkIndex by remember { mutableIntStateOf(0) }
    var feeBulkTplId by remember { mutableStateOf("__default__") }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = TeacherTab.entries.indexOf(activeTab),
            containerColor = Navy,
            contentColor = GoldLight,
            edgePadding = 8.dp
        ) {
            TeacherTab.entries.forEach { tab ->
                Tab(
                    selected = tab == activeTab,
                    onClick = { onSelectTab(tab) },
                    text = { Text("${tab.emoji} ${tab.titleMr}", fontSize = 12.5.sp, fontWeight = if (tab == activeTab) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tch_tab_${tab.key}")
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                TeacherTab.DASH -> {
                    val todayIso = remember { MarathiUtils.todayIso() }
                    val todayHoliday = holidays.firstOrNull { it.dateIso == todayIso }
                    if (MarathiUtils.isHolidayOrSunday(todayIso, holidays)) {
                        Surface(
                            color = ColorTokens.WarningBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Gold.copy(0.5f)),
                            modifier = Modifier.fillMaxWidth().clickable { onOpenHolidays() }
                        ) {
                            Text(
                                "🏖️ आज सुट्टी: ${todayHoliday?.title ?: "रविवार"} — हजेरी अलर्ट बंद आहेत.",
                                color = ColorTokens.WarningText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    SgsCard(title = "🏫 माझा वर्ग: ${MarathiUtils.classToMr(session.iyatta)} ${session.tukdi}") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TchStatBox("एकूण विद्यार्थी", state.roster.size.toString(), GoldLight, Modifier.weight(1f))
                            TchStatBox("आजची हजेरी", if (state.attMarkedToday) "✅ भरली" else "⏳ बाकी", if (state.attMarkedToday) MintGreen else SoftRed, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TchStatBox("आज गैरहजर", state.todayAbsentCount.toString(), SoftRed, Modifier.weight(1f))
                            TchStatBox("वर्ग जमा फी", MarathiUtils.fmtRupees(state.classFeesTotal), MintGreen, Modifier.weight(1f))
                        }
                        if (state.roster.isNotEmpty()) {
                            val boys = state.roster.count { !it.gender.equals("Female", true) && !it.gender.contains("मुलगी") }
                            val girls = state.roster.count { it.gender.equals("Female", true) || it.gender.contains("मुलगी") }
                            val catStr = state.roster.groupBy { it.category.ifBlank { "इतर" } }
                                .entries.sortedByDescending { it.value.size }
                                .joinToString(" | ") { "${it.key}: ${it.value.size}" }
                            val ageStr = state.roster.mapNotNull { stu ->
                                MarathiUtils.computeAgeAsOf(stu.dob, "2026-09-30")
                            }.groupingBy { it }.eachCount().entries.sortedBy { it.key }
                                .joinToString(", ") { "${it.key}व: ${it.value}" }
                            Spacer(Modifier.height(8.dp))
                            Text("👦 मुले: $boys  |  👧 मुली: $girls", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text("प्रवर्ग: $catStr", color = TextCream, fontSize = 11.5.sp)
                            if (ageStr.isNotBlank()) {
                                Text("वयोगट (30-09-2026): $ageStr", color = TextMuted, fontSize = 11.5.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { ops.loadTeacherWorkspace(true) }, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("ताजी माहिती Load करा")
                        }
                        if (state.roster.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showBroadcastDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                                modifier = Modifier.fillMaxWidth().testTag("tch_dash_broadcast_btn")
                            ) {
                                Text("📢 वर्ग मेसेज प्रसारण (WhatsApp / SMS)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }

                    // All Menu + Tabs Quick Actions for Teacher
                    SgsCard(title = "⚡ जलद कृती — सर्व मेनू व टॅब्स") {
                        val extraPages = AppPage.allowedFor(UserRole.TEACHER).filter { it != AppPage.TEACHER }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 3
                        ) {
                            TeacherTab.entries.filter { it != TeacherTab.DASH }.forEach { tab ->
                                Surface(
                                    color = Navy2,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Gold.copy(0.3f)),
                                    modifier = Modifier.weight(1f).clickable { onSelectTab(tab) }.testTag("tch_quick_${tab.key}")
                                ) {
                                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(tab.emoji, fontSize = 19.sp)
                                        Text(tab.titleMr, color = TextCream, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    }
                                }
                            }
                            extraPages.forEach { pg ->
                                Surface(
                                    color = Navy2,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Gold.copy(0.3f)),
                                    modifier = Modifier.weight(1f).clickable { onOpenPage(pg) }.testTag("tch_quick_${pg.key}")
                                ) {
                                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(pg.emoji, fontSize = 19.sp)
                                        Text(pg.titleMr, color = TextCream, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    }
                                }
                            }
                            Surface(
                                color = Navy2,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Gold.copy(0.45f)),
                                modifier = Modifier.weight(1f).clickable { onOpenQrScanner() }.testTag("tch_quick_qr")
                            ) {
                                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📷", fontSize = 19.sp)
                                    Text("QR स्कॅन", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                            Surface(
                                color = Navy2,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Gold.copy(0.45f)),
                                modifier = Modifier.weight(1f).clickable { onOpenHolidays() }.testTag("tch_quick_holidays")
                            ) {
                                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📅", fontSize = 19.sp)
                                    Text("सुट्ट्या", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                        }
                    }

                    // Local Class Birthdays Card
                    val classBirthdays = remember(state.roster, todayIso) {
                        state.roster.filter { MarathiUtils.isBirthdayToday(it.dob, todayIso) }
                    }
                    SgsCard(title = "🎂 आज वर्गातील वाढदिवस (${classBirthdays.size})") {
                        if (classBirthdays.isEmpty()) {
                            Text("आज वर्गात कोणाचाही वाढदिवस नाही.", color = TextMuted, fontSize = 12.5.sp)
                        } else {
                            val clsLabel = "${MarathiUtils.classToMr(session.iyatta)} ${session.tukdi}".trim()
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                classBirthdays.forEach { st ->
                                    val msg = "🎂 वाढदिवसाच्या हार्दिक शुभेच्छा ${st.fullName} ($clsLabel)! तुम्हाला उज्ज्वल यश लाभो हीच सदिच्छा! — ${session.label}"
                                    Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Default.Cake, contentDescription = null, tint = GoldLight)
                                                Column(Modifier.weight(1f)) {
                                                    Text("Roll ${st.rollNo} — ${st.fullName}", color = TextCream, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    if (st.bestPhone.isNotBlank()) {
                                                        Text("📞 ${st.bestPhone}", color = TextMuted, fontSize = 11.sp)
                                                    }
                                                }
                                                if (st.bestPhone.isNotBlank()) {
                                                    IconButton(onClick = { dialPhone(context, st.bestPhone) }) {
                                                        Icon(Icons.Default.Call, contentDescription = "कॉल ${st.fullName}", tint = MintGreen)
                                                    }
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Button(
                                                    onClick = {
                                                        launchWa(
                                                            WhatsAppLaunchRequest(
                                                                mobile = st.bestPhone,
                                                                defaultMessage = msg,
                                                                recipientLabel = st.fullName,
                                                                student = st,
                                                                customClassLabel = clsLabel
                                                            )
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text(if (st.bestPhone.isNotBlank()) "💬 शुभेच्छा" else "💬 WA निवडा", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                OutlinedButton(
                                                    onClick = {
                                                        val card = PdfAndExportUtils.createBirthdayGreetingCard(context, st.fullName, clsLabel)
                                                        PdfAndExportUtils.shareFile(context, card, "image/png", "वाढदिवस शुभेच्छा कार्ड")
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("🎨 कार्ड", color = GoldLight, fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SgsCard(title = "🔔 अलीकडील सूचना") {
                        if (state.notices.isEmpty()) Text("कोणतीही सूचना नाही.", color = TextMuted)
                        else state.notices.take(5).forEach { n ->
                            Text("• ${n.title} (${MarathiUtils.fmtDate(n.date)}): ${n.message}", color = TextCream, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }

                TeacherTab.ROSTER -> {
                    SgsCard(title = "👥 वर्गातील विद्यार्थी (${state.roster.size})") {
                        if (state.roster.isNotEmpty()) {
                            Button(
                                onClick = { showBroadcastDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                                modifier = Modifier.fillMaxWidth().testTag("tch_roster_broadcast_btn")
                            ) {
                                Text("📢 सर्व वर्गास मेसेज प्रसारण (${state.roster.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        SgsTextField(state.rosterSearch, ops::updateTeacherRosterSearch, "नाव किंवा Roll No ने शोधा", tag = "tch_roster_search")
                        Spacer(Modifier.height(8.dp))
                        val filtered = state.roster.filter {
                            state.rosterSearch.isBlank() || it.fullName.contains(state.rosterSearch, true) || it.rollNo.contains(state.rosterSearch, true)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filtered.forEach { st ->
                                Surface(
                                    color = Navy2,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().clickable { onOpenStudentProfile(st.regNo) }
                                ) {
                                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Roll ${st.rollNo} — ${st.fullName}", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                            Text("Reg: ${st.regNo} • Mob: ${st.bestPhone.ifBlank { "—" }}", color = TextMuted, fontSize = 11.5.sp)
                                        }
                                        MobileCallWaButtons(
                                            mobile = st.bestPhone,
                                            recipientLabel = st.fullName,
                                            student = st
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TeacherTab.ATT -> {
                    SgsCard(title = "✅ दैनंदिन हजेरी (फक्त गैरहजर विद्यार्थ्यांवर टिक करा)") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                SgsDateField("हजेरी दिनांक", state.attDate, onDateSelected = ops::loadTeacherAttendance, tag = "tch_att_date")
                            }
                            Button(
                                onClick = ops::saveTeacherAttendance,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                                modifier = Modifier.height(52.dp).testTag("tch_att_save_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Save")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        BannerView(state.attStatus)
                        Spacer(Modifier.height(8.dp))
                        val absentStudents = state.roster.filter { state.absentReasons.containsKey(it.regNo) }
                        Text("गैरहजर निवडलेले: ${state.absentReasons.size} / ${state.roster.size}", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (absentStudents.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    bulkWaIndex = 0
                                    showBulkWaQueue = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                                modifier = Modifier.fillMaxWidth().testTag("tch_att_bulk_wa_btn")
                            ) {
                                Text("📢 सर्व गैरहजर पालकांना WhatsApp संदेश (${absentStudents.size})", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.roster.forEach { st ->
                                val isAbs = state.absentReasons.containsKey(st.regNo)
                                Surface(color = if (isAbs) SoftRed.copy(0.12f) else Navy2, shape = RoundedCornerShape(8.dp)) {
                                    Column(Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isAbs,
                                                onCheckedChange = { chk -> ops.toggleTeacherAbsent(st.regNo, chk) },
                                                colors = CheckboxDefaults.colors(checkedColor = SoftRed, checkmarkColor = DeepNavy)
                                            )
                                            Text("Roll ${st.rollNo} — ${st.fullName}", color = TextCream, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                        }
                                        if (isAbs) {
                                            SgsTextField(
                                                value = state.absentReasons[st.regNo].orEmpty(),
                                                onValueChange = { ops.updateTeacherAbsentReason(st.regNo, it) },
                                                label = "गैरहजेरीचे कारण (ऐच्छिक)",
                                                enableVoice = true
                                            )
                                            val phone = st.bestPhone
                                            val msg = "नमस्कार, आपला पाल्य ${st.fullName} आज दिनांक ${MarathiUtils.fmtDate(state.attDate)} रोजी शाळेत अनुपस्थित आहे. कृपया कारण कळवा. - ${session.label} (श्री. गो. से. हायस्कूल, पाचोरा)"
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                                if (phone.isNotBlank()) {
                                                    OutlinedButton(onClick = { dialPhone(context, phone) }) {
                                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextCream)
                                                        Spacer(Modifier.size(3.dp))
                                                        Text("Call", fontSize = 11.sp, color = TextCream)
                                                    }
                                                    OutlinedButton(onClick = { sendSms(context, phone, msg) }) {
                                                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldLight)
                                                        Spacer(Modifier.size(3.dp))
                                                        Text("SMS", fontSize = 11.sp, color = GoldLight)
                                                    }
                                                }
                                                Button(
                                                    onClick = {
                                                        launchWa(
                                                            WhatsAppLaunchRequest(
                                                                mobile = phone,
                                                                defaultMessage = msg,
                                                                recipientLabel = st.fullName,
                                                                student = st
                                                            )
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                                                ) {
                                                    Text(if (phone.isNotBlank()) "WhatsApp" else "WA निवडा", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SgsCard(title = "📅 तारीख ते तारीख हजेरी रिपोर्ट") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SgsDateField("पासून", state.reportFrom, { ops.loadTeacherAttRangeReport(it, state.reportTo) }, modifier = Modifier.weight(1f))
                            SgsDateField("पर्यंत", state.reportTo, { ops.loadTeacherAttRangeReport(state.reportFrom, it) }, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        if (state.reportRecords.isEmpty()) Text("या कालावधीत अनुपस्थित नोंदी नाहीत.", color = TextMuted, fontSize = 12.sp)
                        else state.reportRecords.forEach { r ->
                            Text("${MarathiUtils.fmtDate(r.date)} — ${r.fullName} (${r.reason.ifBlank { "कारण नाही" }})", color = TextCream, fontSize = 12.sp)
                        }
                    }
                }

                TeacherTab.DIARY -> {
                    SgsCard(title = "📔 विद्यार्थी डायरी व प्रवास (Updown)") {
                        val options = state.roster.map { it.regNo }
                        val labels = state.roster.associate { it.regNo to "${it.rollNo} - ${it.fullName}" }
                        SgsDropdown(
                            label = "विद्यार्थी निवडा",
                            selected = state.selectedDiaryRegNo,
                            options = options,
                            onSelect = ops::selectTeacherDiaryStudent,
                            displayTransform = { labels[it] ?: "— निवडा —" },
                            tag = "tch_diary_student_sel"
                        )
                        if (state.selectedDiaryRegNo.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text("🚌 प्रवास व मूळ गाव", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                            SgsTextField(state.transportInfo.nativeVillage, { ops.updateTransportDraft(village = it) }, "मूळ गाव")
                            Spacer(Modifier.height(6.dp))
                            SgsDropdown("प्रवास साधन", state.transportInfo.travelMode, MarathiUtils.TRAVEL_MODES, { ops.updateTransportDraft(mode = it) })
                            Spacer(Modifier.height(6.dp))
                            SgsTextField(state.transportInfo.transportContact, { ops.updateTransportDraft(contact = it) }, "प्रवास संपर्क क्र.", keyboardType = KeyboardType.Phone)
                            if (MarathiUtils.cleanDigits10(state.transportInfo.transportContact).isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                val dStu = state.roster.firstOrNull { it.regNo == state.selectedDiaryRegNo }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("प्रवास संपर्क: ${state.transportInfo.transportContact}", color = MintGreen, fontSize = 12.sp)
                                    MobileCallWaButtons(
                                        mobile = state.transportInfo.transportContact,
                                        recipientLabel = "${dStu?.fullName.orEmpty()} प्रवास".trim(),
                                        student = dStu
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Button(onClick = ops::saveTeacherTransport, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.fillMaxWidth()) {
                                Text("प्रवास माहिती Save करा")
                            }
                            BannerView(state.transportStatus)

                            Spacer(Modifier.height(12.dp))
                            Text("📝 नवीन डायरी नोंद", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                            SgsDropdown("प्रकार", state.diaryType, listOf("positive", "warning", "scholarship", "personal"), { ops.updateDiaryDraft(type = it) })
                            Spacer(Modifier.height(6.dp))
                            SgsTextField(state.diaryRemark, { ops.updateDiaryDraft(remark = it) }, "तपशील / शेरा", singleLine = false, enableVoice = true)
                            Spacer(Modifier.height(6.dp))
                            Button(onClick = ops::saveTeacherDiary, colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.fillMaxWidth()) {
                                Text("डायरी नोंद जोडा")
                            }
                            BannerView(state.diaryStatus)

                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = state.diaryFull,
                                        onCheckedChange = { ops.setDiaryQueryOptions(it, state.diaryArchive) },
                                        colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = DeepNavy)
                                    )
                                    Text("संपूर्ण नोंदी", color = TextCream, fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = state.diaryArchive,
                                        onCheckedChange = { ops.setDiaryQueryOptions(state.diaryFull, it) },
                                        colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = DeepNavy)
                                    )
                                    Text("जुने Archive", color = TextCream, fontSize = 12.sp)
                                }
                            }
                            Text("📜 नोंदी (${state.diaryEntries.size})", color = GoldLight, fontWeight = FontWeight.Bold)
                            state.diaryEntries.forEach { d ->
                                Text("• [${d.type}] ${MarathiUtils.fmtDate(d.date)}: ${d.remark}", color = TextCream, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }

                TeacherTab.FEES -> {
                    SgsCard(title = "💰 वर्ग फी नोंद (एकूण फी: ${MarathiUtils.fmtRupees(state.totalFeePerStudent)})") {
                        val options = state.roster.map { it.regNo }
                        val labels = state.roster.associate { it.regNo to "${it.rollNo} - ${it.fullName}" }
                        SgsDropdown(
                            label = "विद्यार्थी निवडा",
                            selected = state.selectedFeeRegNo,
                            options = options,
                            onSelect = ops::selectTeacherFeeStudent,
                            displayTransform = { labels[it] ?: "— निवडा —" },
                            tag = "tch_fee_student_sel"
                        )
                        if (state.selectedFeeRegNo.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            val selStudent = state.roster.firstOrNull { it.regNo == state.selectedFeeRegNo }
                            val latest = state.feesList.firstOrNull { it.regNo == state.selectedFeeRegNo }
                            val paid = latest?.feePaid ?: 0.0
                            val opening = latest?.openingDue ?: 0.0
                            val pending = (opening + state.totalFeePerStudent - paid).coerceAtLeast(0.0)
                            Text("जमा: ${MarathiUtils.fmtRupees(paid)}  |  बाकी: ${MarathiUtils.fmtRupees(pending)}", color = if (pending > 0) SoftRed else MintGreen, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                SgsTextField(state.feeAmountInput, ops::updateFeeAmountInput, "जमा रक्कम (₹)", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), tag = "tch_fee_amount")
                                Button(onClick = ops::saveTeacherFee, colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.height(54.dp).testTag("tch_fee_save_btn")) {
                                    Text("जमा करा")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        val pdf = PdfAndExportUtils.renderFeeReceiptPdf(
                                            context = context,
                                            studentName = selStudent?.fullName ?: state.selectedFeeRegNo,
                                            regNo = state.selectedFeeRegNo,
                                            iyatta = session.iyatta,
                                            tukdi = session.tukdi,
                                            acYear = acYear,
                                            dateIso = latest?.date?.ifBlank { MarathiUtils.todayIso() } ?: MarathiUtils.todayIso(),
                                            openingDue = opening,
                                            totalFee = state.totalFeePerStudent,
                                            feePaid = paid,
                                            pendingFee = pending,
                                            updatedBy = latest?.updatedBy?.ifBlank { session.label } ?: session.label,
                                            verifyBaseUrl = verifyBaseUrl
                                        )
                                        PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "फी पावती PDF")
                                    },
                                    modifier = Modifier.weight(1f).testTag("tch_fee_receipt_pdf_btn")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp), tint = GoldLight)
                                    Spacer(Modifier.size(3.dp))
                                    Text("पावती PDF", color = GoldLight, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val pdf = PdfAndExportUtils.renderFeeReceiptPdf(
                                            context = context,
                                            studentName = selStudent?.fullName ?: state.selectedFeeRegNo,
                                            regNo = state.selectedFeeRegNo,
                                            iyatta = session.iyatta,
                                            tukdi = session.tukdi,
                                            acYear = acYear,
                                            dateIso = latest?.date?.ifBlank { MarathiUtils.todayIso() } ?: MarathiUtils.todayIso(),
                                            openingDue = opening,
                                            totalFee = state.totalFeePerStudent,
                                            feePaid = paid,
                                            pendingFee = pending,
                                            updatedBy = latest?.updatedBy?.ifBlank { session.label } ?: session.label,
                                            verifyBaseUrl = verifyBaseUrl
                                        )
                                        PdfAndExportUtils.printPdfFile(context, pdf, "SGS Fee Receipt - ${state.selectedFeeRegNo}")
                                    },
                                    modifier = Modifier.testTag("tch_fee_receipt_print_btn")
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "पावती प्रिंट", modifier = Modifier.size(16.dp), tint = GoldLight)
                                }
                                if (selStudent?.bestPhone.orEmpty().isNotBlank()) {
                                    IconButton(onClick = { dialPhone(context, selStudent?.bestPhone.orEmpty()) }) {
                                        Icon(Icons.Default.Call, contentDescription = "कॉल ${selStudent?.fullName.orEmpty()}", tint = MintGreen)
                                    }
                                }
                                Button(
                                    onClick = {
                                        val sName = selStudent?.fullName ?: state.selectedFeeRegNo
                                        val msg = if (pending > 0.0) {
                                            "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी $sName (रजि. क्र. ${state.selectedFeeRegNo}) यांची शैक्षणिक वर्ष $acYear ची जमा फी ${MarathiUtils.fmtRupees(paid)} असून शिल्लक बाकी ${MarathiUtils.fmtRupees(pending)} आहे. कृपया लवकरात लवकर जमा करावी."
                                        } else {
                                            "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी $sName (रजि. क्र. ${state.selectedFeeRegNo}) यांची शैक्षणिक वर्ष $acYear ची संपूर्ण फी (${MarathiUtils.fmtRupees(paid)}) जमा झाली आहे. धन्यवाद!"
                                        }
                                        launchWa(
                                            WhatsAppLaunchRequest(
                                                mobile = selStudent?.bestPhone.orEmpty(),
                                                defaultMessage = msg,
                                                recipientLabel = sName,
                                                student = selStudent
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                                    modifier = Modifier.weight(1f).testTag("tch_fee_wa_btn")
                                ) {
                                    Text("💬 WA फी", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            BannerView(state.feeStatus)
                            if (state.studentFeeHistory.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("जमा इतिहास:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                state.studentFeeHistory.forEach { f ->
                                    Text("• ${MarathiUtils.fmtDate(f.date)}: एकूण जमा ${MarathiUtils.fmtRupees(f.feePaid)} (${f.updatedBy})", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    val rosterFees = state.roster.map { st ->
                        val latest = state.feesList.firstOrNull { it.regNo == st.regNo }
                        val paid = latest?.feePaid ?: 0.0
                        val pending = ((latest?.openingDue ?: 0.0) + state.totalFeePerStudent - paid).coerceAtLeast(0.0)
                        Triple(st, paid, pending)
                    }
                    val pendingRows = rosterFees.filter { it.third > 0.0 }
                    val paidRows = rosterFees.filter { it.third <= 0.0 }
                    val totalClassPending = pendingRows.sumOf { it.third }
                    val visibleRows = when (feeFilter) {
                        "pending" -> pendingRows
                        "paid" -> paidRows
                        else -> rosterFees
                    }

                    SgsCard(title = "📊 वर्गनिहाय फी यादी (बाकी: ${MarathiUtils.fmtRupees(totalClassPending)})") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = feeFilter == "all", onClick = { feeFilter = "all" }, label = { Text("सर्व (${rosterFees.size})", fontSize = 11.sp) })
                            FilterChip(selected = feeFilter == "pending", onClick = { feeFilter = "pending" }, label = { Text("⚠️ थकबाकी (${pendingRows.size})", fontSize = 11.sp) })
                            FilterChip(selected = feeFilter == "paid", onClick = { feeFilter = "paid" }, label = { Text("✅ पूर्ण (${paidRows.size})", fontSize = 11.sp) })
                        }
                        if (pendingRows.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    feeBulkIndex = 0
                                    showFeeBulkWaQueue = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                                modifier = Modifier.fillMaxWidth().testTag("tch_fee_bulk_wa_btn")
                            ) {
                                Text("📢 सर्व थकबाकीदार पालकांना WhatsApp रांग (${pendingRows.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (visibleRows.isEmpty()) {
                            Text("या गटात विद्यार्थी नाहीत.", color = TextMuted, fontSize = 12.sp)
                        } else {
                            visibleRows.forEach { (st, paid, pending) ->
                                val feeMsg = if (pending > 0.0) {
                                    "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी ${st.fullName} (इयत्ता ${MarathiUtils.classToMr(session.iyatta)} ${session.tukdi}) यांची शैक्षणिक वर्ष $acYear ची फी ${MarathiUtils.fmtRupees(pending)} शिल्लक बाकी आहे. कृपया लवकरात लवकर जमा करावी."
                                } else {
                                    "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी ${st.fullName} (${MarathiUtils.classToMr(session.iyatta)} ${session.tukdi}) यांची शैक्षणिक वर्ष $acYear ची फी पूर्ण जमा आहे. धन्यवाद!"
                                }
                                Row(
                                    Modifier.fillMaxWidth().clickable { ops.selectTeacherFeeStudent(st.regNo) }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${st.rollNo}. ${st.fullName}", color = TextCream, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "जमा ${MarathiUtils.fmtRupees(paid)} | बाकी ${MarathiUtils.fmtRupees(pending)}" +
                                                if (st.bestPhone.isNotBlank()) " • 📞 ${st.bestPhone}" else "",
                                            color = if (pending > 0) SoftRed else MintGreen,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    MobileCallWaButtons(
                                        mobile = st.bestPhone,
                                        recipientLabel = st.fullName,
                                        defaultMessage = feeMsg,
                                        student = st,
                                        allowWaWhenEmpty = pending > 0.0
                                    )
                                }
                            }
                        }
                    }
                }

                TeacherTab.NOTICES -> {
                    SgsCard(title = "🔔 शाळेच्या सूचना (${state.notices.size})") {
                        if (state.notices.isEmpty()) Text("अजून सूचना नाहीत.", color = TextMuted)
                        else state.notices.forEach { n ->
                            Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(10.dp)) {
                                    Text("${n.title} (${MarathiUtils.fmtDate(n.date)})", color = GoldLight, fontWeight = FontWeight.Bold)
                                    Text(n.message, color = TextCream, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                }

                TeacherTab.ANA -> {
                    SgsCard(title = "📈 मासिक हजेरी विश्लेषण") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            SgsTextField(state.anaMonth, { ops.loadTeacherAnalytics(month = it) }, "महिना (YYYY-MM)", modifier = Modifier.weight(1f))
                            Button(onClick = { ops.loadTeacherAnalytics() }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.height(54.dp)) {
                                Text("पहा")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (state.anaLoading) CircularProgressIndicator(color = Gold)
                        else {
                            Text("कार्यदिवस: ${state.anaWorkingDays} | सरासरी: ${state.anaAvgPct ?: "—"}% | सलग गैरहजर: ${state.anaStreaks.size}", color = GoldLight, fontWeight = FontWeight.Bold)
                            if (state.anaStudents.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val headers = listOf("Roll", "नाव", "कार्यदिवस", "गैरहजर", "%", "सलग")
                                            val rows = state.anaStudents.map { listOf(it.rollNo, it.fullName, it.workingDays.toString(), it.absent.toString(), it.pct?.let { p -> "$p%" } ?: "—", it.maxRun.toString()) }
                                            val pdf = PdfAndExportUtils.renderTablePdf(context, "मासिक हजेरी विश्लेषण (${state.anaMonth})", "${MarathiUtils.classToMr(session.iyatta)} ${session.tukdi}", headers, rows, "Teacher_Analytics_${state.anaMonth}.pdf")
                                            PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Analytics PDF")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("PDF काढा")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val headers = listOf("Roll", "नाव", "कार्यदिवस", "गैरहजर", "%", "सलग")
                                            val rows = state.anaStudents.map { listOf(it.rollNo, it.fullName, it.workingDays.toString(), it.absent.toString(), it.pct?.let { p -> "$p%" } ?: "—", it.maxRun.toString()) }
                                            val pdf = PdfAndExportUtils.renderTablePdf(context, "मासिक हजेरी विश्लेषण (${state.anaMonth})", "${MarathiUtils.classToMr(session.iyatta)} ${session.tukdi}", headers, rows, "Teacher_Analytics_${state.anaMonth}.pdf")
                                            PdfAndExportUtils.printPdfFile(context, pdf, "SGS Attendance Analytics")
                                        }
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "प्रिंट", modifier = Modifier.size(16.dp), tint = GoldLight)
                                        Spacer(Modifier.size(4.dp))
                                        Text("प्रिंट", color = GoldLight)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            state.anaStreaks.forEach { st ->
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
                                            defaultMessage = "नमस्कार, आपला पाल्य ${st.fullName} सलग ${st.maxRun} दिवस गैरहजर आहे. कृपया कारण कळवा.",
                                            student = Student(iyatta = session.iyatta, tukdi = st.tukdi.ifBlank { session.tukdi }, rollNo = st.rollNo, fullName = st.fullName, whatsappMobile = st.mobile)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TeacherTab.MAINT -> {
                    SgsCard(title = "🔧 तुकडी बदल (फक्त तुमच्या वर्गातील)") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            SgsTextField(state.maintSearchRegNo, ops::updateTeacherMaintQuery, "Reg No / ID", modifier = Modifier.weight(1f))
                            Button(onClick = ops::searchTeacherMaintStudent, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.height(54.dp)) {
                                Icon(Icons.Default.Search, contentDescription = "शोधा")
                            }
                        }
                        state.maintStudent?.let { stu ->
                            Spacer(Modifier.height(10.dp))
                            Text("विद्यार्थी: ${stu.fullName} (सध्याची तुकडी: ${stu.tukdi})", color = GoldLight, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            SgsDropdown("नवीन तुकडी", state.maintNewDiv, MarathiUtils.DIVISIONS, ops::updateTeacherMaintDiv)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = ops::saveTeacherDivisionChange, colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.fillMaxWidth()) {
                                Text("तुकडी अद्ययावत करा")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        BannerView(state.maintStatus)
                    }
                }
            }
        }
    }

    if (showBulkWaQueue) {
        val absentList = state.roster.filter { state.absentReasons.containsKey(it.regNo) }
        val currentStu = absentList.getOrNull(bulkWaIndex)
        if (currentStu == null) {
            showBulkWaQueue = false
        } else {
            val reason = state.absentReasons[currentStu.regNo].orEmpty()
            val defaultMsg = buildString {
                append("नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. आपला पाल्य ${currentStu.fullName} (Roll ${currentStu.rollNo}) आज दिनांक ${MarathiUtils.fmtDate(state.attDate)} रोजी शाळेत अनुपस्थित आहे.")
                if (reason.isNotBlank()) append(" कारण: $reason.")
                append(" कृपया कारण कळवा. - ${session.label}")
            }
            val selTpl = templates.firstOrNull { it.id == bulkWaTplId }
            val msg = if (selTpl == null) defaultMsg else MarathiUtils.formatMessageTemplate(selTpl.body, currentStu)
            AlertDialog(
                onDismissRequest = { showBulkWaQueue = false },
                containerColor = Navy,
                title = {
                    Text(
                        "📢 गैरहजर WhatsApp (${bulkWaIndex + 1} / ${absentList.size})",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${currentStu.rollNo}. ${currentStu.fullName}", color = TextCream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (currentStu.bestPhone.isNotBlank()) "📱 मोबाईल: ${currentStu.bestPhone}" else "⚠️ मोबाईल नोंद नाही — WhatsApp मध्ये Contact निवडावा लागेल",
                            color = if (currentStu.bestPhone.isNotBlank()) MintGreen else GoldLight,
                            fontSize = 12.sp
                        )
                        if (templates.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(selected = bulkWaTplId == "__default__", onClick = { bulkWaTplId = "__default__" }, label = { Text("✨ मूळ संदेश", fontSize = 10.5.sp) })
                                templates.forEach { t ->
                                    FilterChip(selected = bulkWaTplId == t.id, onClick = { bulkWaTplId = t.id }, label = { Text(t.title, fontSize = 10.5.sp) })
                                }
                            }
                        }
                        Surface(color = Navy2, shape = RoundedCornerShape(8.dp)) {
                            Text(msg, color = TextCream, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openWhatsApp(context, currentStu.bestPhone, msg)
                            if (bulkWaIndex + 1 < absentList.size) bulkWaIndex++ else showBulkWaQueue = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                    ) { Text(if (bulkWaIndex + 1 < absentList.size) "💬 पाठवा व पुढे" else "💬 पाठवा व पूर्ण") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (bulkWaIndex + 1 < absentList.size) {
                            TextButton(onClick = { bulkWaIndex++ }) { Text("पुढे (Skip)", color = GoldLight) }
                        }
                        TextButton(onClick = { showBulkWaQueue = false }) { Text("बंद करा", color = TextMuted) }
                    }
                }
            )
        }
    }

    if (showFeeBulkWaQueue) {
        val pendingList = state.roster.mapNotNull { st ->
            val latest = state.feesList.firstOrNull { it.regNo == st.regNo }
            val paid = latest?.feePaid ?: 0.0
            val pending = ((latest?.openingDue ?: 0.0) + state.totalFeePerStudent - paid).coerceAtLeast(0.0)
            if (pending > 0.0) Triple(st, paid, pending) else null
        }
        val current = pendingList.getOrNull(feeBulkIndex)
        if (current == null) {
            showFeeBulkWaQueue = false
        } else {
            val (stu, paid, pending) = current
            val defaultMsg = "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी ${stu.fullName} (Roll ${stu.rollNo}, ${MarathiUtils.classToMr(session.iyatta)} ${session.tukdi}) यांची शैक्षणिक वर्ष $acYear ची जमा फी ${MarathiUtils.fmtRupees(paid)} असून शिल्लक बाकी ${MarathiUtils.fmtRupees(pending)} आहे. कृपया लवकरात लवकर जमा करावी."
            val selTpl = templates.firstOrNull { it.id == feeBulkTplId }
            val msg = if (selTpl == null) defaultMsg else MarathiUtils.formatMessageTemplate(selTpl.body, stu)
            AlertDialog(
                onDismissRequest = { showFeeBulkWaQueue = false },
                containerColor = Navy,
                title = {
                    Text("📢 थकबाकी WhatsApp (${feeBulkIndex + 1} / ${pendingList.size})", color = GoldLight, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${stu.rollNo}. ${stu.fullName} — बाकी: ${MarathiUtils.fmtRupees(pending)}", color = SoftRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (stu.bestPhone.isNotBlank()) "📱 मोबाईल: ${stu.bestPhone}" else "⚠️ मोबाईल नोंद नाही — WhatsApp मध्ये Contact निवडावा लागेल",
                            color = if (stu.bestPhone.isNotBlank()) MintGreen else GoldLight,
                            fontSize = 12.sp
                        )
                        if (templates.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(selected = feeBulkTplId == "__default__", onClick = { feeBulkTplId = "__default__" }, label = { Text("✨ मूळ संदेश", fontSize = 10.5.sp) })
                                templates.forEach { t ->
                                    FilterChip(selected = feeBulkTplId == t.id, onClick = { feeBulkTplId = t.id }, label = { Text(t.title, fontSize = 10.5.sp) })
                                }
                            }
                        }
                        Surface(color = Navy2, shape = RoundedCornerShape(8.dp)) {
                            Text(msg, color = TextCream, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openWhatsApp(context, stu.bestPhone, msg)
                            if (feeBulkIndex + 1 < pendingList.size) feeBulkIndex++ else showFeeBulkWaQueue = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                    ) { Text(if (feeBulkIndex + 1 < pendingList.size) "💬 पाठवा व पुढे" else "💬 पाठवा व पूर्ण") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (feeBulkIndex + 1 < pendingList.size) {
                            TextButton(onClick = { feeBulkIndex++ }) { Text("पुढे (Skip)", color = GoldLight) }
                        }
                        TextButton(onClick = { showFeeBulkWaQueue = false }) { Text("बंद करा", color = TextMuted) }
                    }
                }
            )
        }
    }

    if (showBroadcastDialog) {
        val clsLabel = "${MarathiUtils.classToMr(session.iyatta)} ${session.tukdi}".trim()
        ClassBroadcastQueueDialog(
            title = "📢 वर्ग मेसेज प्रसारण ($clsLabel)",
            students = state.roster,
            templates = templates,
            customClassLabel = clsLabel,
            onOpenManageTemplates = {
                showBroadcastDialog = false
                onOpenManageTemplates()
            },
            onDismiss = { showBroadcastDialog = false }
        )
    }
}

@Composable
private fun TchStatBox(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(color = Navy2, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Gold.copy(0.25f)), modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(label, color = TextMuted, fontSize = 11.sp)
        }
    }
}
