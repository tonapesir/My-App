package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppPage
import com.example.domain.model.BirthdayStudent
import com.example.domain.model.HolidayItem
import com.example.domain.model.Student
import com.example.domain.model.TeacherTab
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
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

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionsHubDialog(
    role: UserRole,
    pinnedKeys: List<String> = emptyList(),
    onTogglePinKey: (String) -> Unit = {},
    onSelectPage: (AppPage) -> Unit,
    onSelectTeacherTab: (TeacherTab) -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenHolidayCalendar: () -> Unit,
    onOpenBirthdays: () -> Unit,
    onOpenTemplates: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val q = searchQuery.trim()
    val allowedPages = remember(role) { AppPage.allowedFor(role) }
    val filteredPages = remember(allowedPages, q) {
        if (q.isBlank()) allowedPages
        else allowedPages.filter { it.titleMr.contains(q, true) || it.key.contains(q, true) }
    }
    val filteredTabs = remember(role, q) {
        if (role != UserRole.TEACHER) emptyList()
        else if (q.isBlank()) TeacherTab.entries
        else TeacherTab.entries.filter { it.titleMr.contains(q, true) || it.key.contains(q, true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy,
        titleContentColor = GoldLight,
        textContentColor = TextCream,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("⚡ जलद कृती — सर्व मेनू", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "बंद करा", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SgsTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "🔍 मेनू शोधा (नाव किंवा टॅब)",
                    tag = "qa_search_input",
                    enableVoice = true
                )

                if (pinnedKeys.isNotEmpty() && q.isBlank()) {
                    val pinnedTabs = TeacherTab.entries.filter { "tab_${it.key}" in pinnedKeys && role == UserRole.TEACHER }
                    val pinnedPages = allowedPages.filter { "page_${it.key}" in pinnedKeys }
                    if (pinnedTabs.isNotEmpty() || pinnedPages.isNotEmpty()) {
                        Text("⭐ पिन केलेले आवडते (${pinnedTabs.size + pinnedPages.size})", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 3
                        ) {
                            pinnedTabs.forEach { tab ->
                                val key = "tab_${tab.key}"
                                QuickTile(
                                    emoji = tab.emoji,
                                    label = tab.titleMr,
                                    modifier = Modifier.weight(1f),
                                    tag = "qa_pin_$key",
                                    isPinned = true,
                                    onTogglePin = { onTogglePinKey(key) }
                                ) {
                                    onDismiss()
                                    onSelectTeacherTab(tab)
                                }
                            }
                            pinnedPages.forEach { pg ->
                                val key = "page_${pg.key}"
                                QuickTile(
                                    emoji = pg.emoji,
                                    label = pg.titleMr,
                                    modifier = Modifier.weight(1f),
                                    tag = "qa_pin_$key",
                                    isPinned = true,
                                    onTogglePin = { onTogglePinKey(key) }
                                ) {
                                    onDismiss()
                                    onSelectPage(pg)
                                }
                            }
                        }
                        HorizontalDivider(color = Gold.copy(0.25f))
                    }
                }

                // Universal Smart Utilities row
                Text("🛠️ स्मार्ट सुविधा", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickTile("📷", "QR स्कॅनर", Modifier.weight(1f), "qa_qr_scanner") {
                        onDismiss()
                        onOpenQrScanner()
                    }
                    QuickTile("📅", "सुट्ट्या", Modifier.weight(1f), "qa_holiday_cal") {
                        onDismiss()
                        onOpenHolidayCalendar()
                    }
                    QuickTile("🎂", "वाढदिवस", Modifier.weight(1f), "qa_birthdays") {
                        onDismiss()
                        onOpenBirthdays()
                    }
                    QuickTile("💬", "टेम्पलेट्स", Modifier.weight(1f), "qa_templates") {
                        onDismiss()
                        onOpenTemplates()
                    }
                }

                if (role == UserRole.TEACHER && filteredTabs.isNotEmpty()) {
                    HorizontalDivider(color = Gold.copy(0.25f))
                    Text("🏫 वर्ग शिक्षक टॅब्स (⭐ पिन करा)", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        filteredTabs.forEach { tab ->
                            val pKey = "tab_${tab.key}"
                            QuickTile(
                                emoji = tab.emoji,
                                label = tab.titleMr,
                                modifier = Modifier.weight(1f),
                                tag = "qa_tab_${tab.key}",
                                isPinned = pKey in pinnedKeys,
                                onTogglePin = { onTogglePinKey(pKey) }
                            ) {
                                onDismiss()
                                onSelectTeacherTab(tab)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Gold.copy(0.25f))
                Text("📋 सर्व मेनू आयटम्स (${filteredPages.size}) — ⭐ ने पिन करा", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                if (filteredPages.isEmpty() && filteredTabs.isEmpty()) {
                    Text("'$q' साठी कोणताही मेनू सापडला नाही.", color = TextMuted, fontSize = 12.sp)
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        filteredPages.forEach { pg ->
                            val pKey = "page_${pg.key}"
                            QuickTile(
                                emoji = pg.emoji,
                                label = pg.titleMr,
                                modifier = Modifier.weight(1f),
                                tag = "qa_page_${pg.key}",
                                isPinned = pKey in pinnedKeys,
                                onTogglePin = { onTogglePinKey(pKey) }
                            ) {
                                onDismiss()
                                onSelectPage(pg)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("बंद करा", color = GoldLight) }
        }
    )
}

@Composable
fun QuickTile(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    tag: String = "",
    isPinned: Boolean? = null,
    onTogglePin: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        color = Navy2,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isPinned == true) Gold else Gold.copy(0.3f)),
        modifier = modifier
            .then(if (tag.isNotBlank()) Modifier.testTag(tag) else Modifier)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(emoji, fontSize = 19.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    color = TextCream,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (isPinned != null && onTogglePin != null) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isPinned) "पिन काढा" else "पिन करा",
                    tint = if (isPinned) Gold else TextMuted.copy(0.75f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .clickable { onTogglePin() }
                )
            }
        }
    }
}

@Composable
fun HolidayCalendarDialog(
    holidays: List<HolidayItem>,
    canManage: Boolean = false,
    onAddHoliday: (String, String) -> Unit,
    onDeleteHoliday: (String) -> Unit,
    onSeedDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var dateIso by remember { mutableStateOf(MarathiUtils.todayIso()) }
    var title by remember { mutableStateOf("") }
    val todayIso = remember { MarathiUtils.todayIso() }
    val todayHoliday = holidays.firstOrNull { it.dateIso == todayIso }
    val isTodayOff = MarathiUtils.isHolidayOrSunday(todayIso, holidays)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy,
        titleContentColor = GoldLight,
        textContentColor = TextCream,
        title = { Text("📅 शाळा सुट्टी कॅलेंडर", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (isTodayOff) ColorTokens.WarningBg else ColorTokens.SuccessBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Gold.copy(0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = if (isTodayOff) ColorTokens.WarningText else MintGreen
                        )
                        Column {
                            Text(
                                text = when {
                                    todayHoliday != null -> "आज सुट्टी: ${todayHoliday.title}"
                                    MarathiUtils.isSundayIso(todayIso) -> "आज रविवार (सुट्टी)"
                                    else -> "आज नियमित शाळा कार्यदिवस आहे"
                                },
                                color = if (isTodayOff) ColorTokens.WarningText else MintGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Text("सुट्टीच्या दिवशी दैनंदिन हजेरी अलर्ट येत नाही.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }

                if (canManage) {
                    SgsDateField("सुट्टीचा दिनांक", dateIso, { dateIso = it }, tag = "holiday_date_input")
                    SgsTextField(title, { title = it }, "सुट्टीचे नाव (उदा. दिवाळी सुट्टी)", tag = "holiday_title_input", enableVoice = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (dateIso.isNotBlank() && title.isNotBlank()) {
                                    onAddHoliday(dateIso, title)
                                    title = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                            modifier = Modifier.weight(1f).testTag("holiday_add_btn")
                        ) { Text("➕ सुट्टी जोडा", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = onSeedDefaults,
                            modifier = Modifier.weight(1f).testTag("holiday_seed_btn")
                        ) { Text("🇮🇳 प्रमुख सुट्ट्या", color = GoldLight, fontSize = 11.5.sp) }
                    }
                } else {
                    Surface(
                        color = Navy2,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Gold.copy(0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "ℹ️ सुट्टी कॅलेंडर मॅनेज करण्याचे अधिकार फक्त Super Master व Master User यांनाच आहेत.",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider(color = Gold.copy(0.25f))
                Text("नोंदवलेल्या सुट्ट्या (${holidays.size})", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                if (holidays.isEmpty()) {
                    Text("कोणतीही सुट्टी नोंदवलेली नाही.", color = TextMuted, fontSize = 12.sp)
                } else {
                    holidays.forEach { h ->
                        Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(h.title, color = TextCream, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                    Text(MarathiUtils.fmtDate(h.dateIso), color = GoldLight, fontSize = 11.sp)
                                }
                                if (canManage) {
                                    IconButton(onClick = { onDeleteHoliday(h.dateIso) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "हटवा", tint = SoftRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("पूर्ण", color = GoldLight) }
        }
    )
}

@Composable
fun BirthdaysModalDialog(
    birthdays: List<BirthdayStudent>,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val launchWa = LocalWhatsAppLauncher.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy,
        titleContentColor = GoldLight,
        textContentColor = TextCream,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🎂 आजचे वाढदिवस (${birthdays.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                TextButton(onClick = onRefresh) { Text("🔄 ताजे करा", color = GoldLight, fontSize = 12.sp) }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (birthdays.isEmpty()) {
                    Text(
                        "आज कोणाचाही वाढदिवस सापडला नाही (किंवा Cache लोड करून पहा).",
                        color = TextMuted,
                        fontSize = 12.5.sp
                    )
                } else {
                    birthdays.forEach { b ->
                        val phone = b.whatsappMobile.ifBlank { b.alternateMobile }.ifBlank { b.contact }
                        val clsLabel = "${MarathiUtils.classToMr(b.iyatta)} ${b.tukdi}".trim()
                        val msg = "🎂 वाढदिवसाच्या हार्दिक शुभेच्छा ${b.fullName} ($clsLabel)! तुम्हाला उज्ज्वल यश व उत्तम आरोग्य लाभो हीच सदिच्छा! — श्री. गोविंदराम सेक्सरिया हायस्कूल, पाचोरा"
                        val stu = Student(regNo = b.regNo, iyatta = b.iyatta, tukdi = b.tukdi, rollNo = b.rollNo, fullName = b.fullName, whatsappMobile = phone)
                        Surface(color = Navy2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Cake, contentDescription = null, tint = GoldLight)
                                    Column(Modifier.weight(1f)) {
                                        Text(b.fullName, color = TextCream, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        Text(
                                            "$clsLabel • Roll ${b.rollNo.ifBlank { "-" }} • ${MarathiUtils.fmtDate(b.dob)}" +
                                                if (phone.isNotBlank()) " • 📱 $phone" else "",
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
                                                    defaultMessage = msg,
                                                    recipientLabel = b.fullName,
                                                    student = stu,
                                                    customClassLabel = clsLabel
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (phone.isNotBlank()) "💬 शुभेच्छा" else "💬 WA निवडा", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val card = PdfAndExportUtils.createBirthdayGreetingCard(context, b.fullName, clsLabel)
                                            PdfAndExportUtils.shareFile(context, card, "image/png", "वाढदिवस शुभेच्छा कार्ड")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("🎨 शुभेच्छा कार्ड", color = GoldLight, fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("बंद करा", color = GoldLight) }
        }
    )
}
