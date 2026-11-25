package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.domain.model.ClassDivisionPair
import com.example.domain.model.ClassTeacherItem
import com.example.domain.model.MessageTemplate
import com.example.domain.model.Student
import com.example.domain.model.UserRole
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
import com.example.ui.theme.Crimson
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
import com.example.ui.viewmodel.BannerMessage
import com.example.ui.viewmodel.EarlyLeaveUiState
import com.example.ui.viewmodel.ScholarshipUiState
import com.example.ui.viewmodel.TeacherAndOpsDelegate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EarlyLeaveScreen(
    role: UserRole,
    state: EarlyLeaveUiState,
    ops: TeacherAndOpsDelegate,
    templates: List<MessageTemplate> = emptyList(),
    onOpenProfile: (String) -> Unit,
    onOpenQrScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    val launchWa = LocalWhatsAppLauncher.current
    var showPeonBulkWa by remember { mutableStateOf(false) }
    var peonBulkIdx by remember { mutableIntStateOf(0) }
    var peonBulkTplId by remember { mutableStateOf("__default__") }
    var showStaffBulkWa by remember { mutableStateOf(false) }
    var staffBulkIdx by remember { mutableIntStateOf(0) }
    var staffBulkTplId by remember { mutableStateOf("__default__") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (role == UserRole.PEON) {
            val peonTargets = remember(state.peonRecords) {
                state.peonRecords.filter { it.outStatus == "gone" }.ifEmpty { state.peonRecords }
            }
            SgsCard(title = "🚪 आजची परवानगी यादी (शिपाई मार्क-आउट)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = ops::loadPeonEarlyLeave, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("यादी ताजी करा", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onOpenQrScanner, modifier = Modifier.testTag("peon_qr_scan_btn")) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QR स्कॅन", modifier = Modifier.size(16.dp), tint = GoldLight)
                        Spacer(Modifier.size(4.dp))
                        Text("QR स्कॅन", color = GoldLight, fontSize = 12.sp)
                    }
                }
                if (peonTargets.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { peonBulkIdx = 0; showPeonBulkWa = true },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                        modifier = Modifier.fillMaxWidth().testTag("peon_el_bulk_wa_btn")
                    ) {
                        Text(
                            if (state.peonRecords.any { it.outStatus == "gone" }) "📢 घरी गेलेल्यांना WhatsApp रांग (${peonTargets.size})"
                            else "📢 सर्व परवानगी WhatsApp रांग (${peonTargets.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (state.peonRecords.isEmpty()) Text("आज कोणताही परवानगी विद्यार्थी नाही.", color = TextMuted)
                else state.peonRecords.forEach { rec ->
                    val statusMsgPart = when (rec.outStatus) {
                        "gone" -> "शाळेतून घरी गेला/गेली आहे"
                        "notgone" -> "अजून शाळेतून बाहेर गेला/गेली नाही"
                        else -> "घरी जाण्याची परवानगी दिली आहे"
                    }
                    val waMsg = "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. आपला पाल्य ${rec.fullName} (${MarathiUtils.classToMr(rec.iyatta)} ${rec.tukdi}) यास दिनांक ${MarathiUtils.fmtDate(rec.date)} रोजी तासिका क्र. ${rec.period} नंतर $statusMsgPart."
                    Surface(color = Navy2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${rec.fullName} — ${MarathiUtils.classToMr(rec.iyatta)} ${rec.tukdi} (तासिका ${rec.period})", color = GoldLight, fontWeight = FontWeight.Bold)
                            Text("स्थिती: ${if (rec.outStatus == "gone") "✅ गेला" else if (rec.outStatus == "notgone") "❌ गेला नाही" else "⏳ बाकी"}", color = TextCream, fontSize = 12.5.sp)
                            val recStu = Student(regNo = rec.regNo, iyatta = rec.iyatta, tukdi = rec.tukdi, fullName = rec.fullName, whatsappMobile = rec.bestPhone)
                            if (rec.bestPhone.isNotBlank()) {
                                Text("📞 मोबाईल: ${rec.bestPhone}", color = MintGreen, fontSize = 11.5.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { ops.markPeonOut(rec, "gone") }, colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.weight(1f)) { Text("✅ गेला") }
                                Button(onClick = { ops.markPeonOut(rec, "notgone") }, colors = ButtonDefaults.buttonColors(containerColor = Crimson), modifier = Modifier.weight(1f)) { Text("❌ गेला नाही") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (rec.bestPhone.isNotBlank()) {
                                    OutlinedButton(onClick = { dialPhone(context, rec.bestPhone) }, modifier = Modifier.weight(1f)) {
                                        Text("📞 कॉल", color = TextCream, fontSize = 11.5.sp)
                                    }
                                }
                                Button(
                                    onClick = {
                                        launchWa(
                                            WhatsAppLaunchRequest(
                                                mobile = rec.bestPhone,
                                                defaultMessage = waMsg,
                                                recipientLabel = rec.fullName,
                                                student = recStu
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (rec.bestPhone.isNotBlank()) "💬 WhatsApp पालक" else "💬 WhatsApp निवडा", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (showPeonBulkWa) {
                val current = peonTargets.getOrNull(peonBulkIdx)
                if (current == null) {
                    showPeonBulkWa = false
                } else {
                    val statusMsgPart = when (current.outStatus) {
                        "gone" -> "शाळेतून घरी गेला/गेली आहे"
                        "notgone" -> "अजून शाळेतून बाहेर गेला/गेली नाही"
                        else -> "घरी जाण्याची परवानगी दिली आहे"
                    }
                    val defaultMsg = "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. आपला पाल्य ${current.fullName} (${MarathiUtils.classToMr(current.iyatta)} ${current.tukdi}) यास दिनांक ${MarathiUtils.fmtDate(current.date)} रोजी तासिका क्र. ${current.period} नंतर $statusMsgPart."
                    val selectedTpl = templates.firstOrNull { it.id == peonBulkTplId }
                    val curStu = Student(regNo = current.regNo, iyatta = current.iyatta, tukdi = current.tukdi, fullName = current.fullName, whatsappMobile = current.bestPhone)
                    val msg = if (selectedTpl == null) defaultMsg else MarathiUtils.formatMessageTemplate(selectedTpl.body, curStu)
                    AlertDialog(
                        onDismissRequest = { showPeonBulkWa = false },
                        containerColor = Navy,
                        title = { Text("📢 घरी गेलेले WhatsApp (${peonBulkIdx + 1} / ${peonTargets.size})", color = GoldLight, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${current.fullName} (${MarathiUtils.classToMr(current.iyatta)} ${current.tukdi})", color = TextCream, fontWeight = FontWeight.Bold)
                                Text(
                                    if (current.bestPhone.isNotBlank()) "📱 मोबाईल: ${current.bestPhone}" else "⚠️ मोबाईल नोंद नाही — WhatsApp मध्ये Contact निवडा",
                                    color = if (current.bestPhone.isNotBlank()) MintGreen else GoldLight,
                                    fontSize = 12.sp
                                )
                                if (templates.isNotEmpty()) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilterChip(selected = peonBulkTplId == "__default__", onClick = { peonBulkTplId = "__default__" }, label = { Text("✨ मूळ संदेश", fontSize = 10.5.sp) })
                                        templates.forEach { t ->
                                            FilterChip(selected = peonBulkTplId == t.id, onClick = { peonBulkTplId = t.id }, label = { Text(t.title, fontSize = 10.5.sp) })
                                        }
                                    }
                                }
                                Surface(color = Navy2, shape = RoundedCornerShape(8.dp)) {
                                    Text(msg, color = TextCream, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                                }
                                if (current.bestPhone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            sendSms(context, current.bestPhone, msg)
                                            if (peonBulkIdx + 1 < peonTargets.size) peonBulkIdx++ else showPeonBulkWa = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldLight)
                                        Spacer(Modifier.size(4.dp))
                                        Text("✉️ SMS व पुढे", color = GoldLight, fontSize = 12.sp)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    openWhatsApp(context, current.bestPhone, msg)
                                    if (peonBulkIdx + 1 < peonTargets.size) peonBulkIdx++ else showPeonBulkWa = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                            ) { Text(if (peonBulkIdx + 1 < peonTargets.size) "💬 पाठवा व पुढे" else "💬 पाठवा व पूर्ण") }
                        },
                        dismissButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (peonBulkIdx + 1 < peonTargets.size) {
                                    TextButton(onClick = { peonBulkIdx++ }) { Text("पुढे (Skip)", color = GoldLight) }
                                }
                                TextButton(onClick = { showPeonBulkWa = false }) { Text("बंद करा", color = TextMuted) }
                            }
                        }
                    )
                }
            }
            return
        }

        SgsCard(title = "🚪 घरी जाणारे विद्यार्थी नोंदवही") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsDateField("दिनांक", state.date, { ops.setEarlyLeaveFilters(date = it) }, tag = "el_date")
                if (role != UserRole.TEACHER) {
                    val classKeys = listOf("ALL|") + state.classes.map { it.key }
                    SgsDropdown(
                        label = "वर्ग निवडा",
                        selected = state.selectedClassKey,
                        options = classKeys,
                        onSelect = { ops.setEarlyLeaveFilters(classKey = it) },
                        displayTransform = { k ->
                            if (k == "ALL|") "📋 सर्व वर्ग (फक्त यादी)" else {
                                val p = k.split("|")
                                "${MarathiUtils.classToMr(p[0])} ${p.getOrNull(1).orEmpty()}"
                            }
                        }
                    )
                }
                OutlinedButton(
                    onClick = onOpenQrScanner,
                    modifier = Modifier.fillMaxWidth().testTag("el_qr_scan_btn")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "ID Card QR स्कॅन", modifier = Modifier.size(16.dp), tint = GoldLight)
                    Spacer(Modifier.size(6.dp))
                    Text("📷 ID Card QR स्कॅन करून विद्यार्थी निवडा", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                BannerView(state.status)
            }
        }

        if (state.roster.isNotEmpty() && !state.archived) {
            SgsCard(title = "📝 वर्ग नोंद-तक्ता (तासिका १–९)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.roster.forEach { st ->
                        val isAbs = st.regNo.lowercase() in state.absentRegNos
                        val saved = state.records.firstOrNull { it.regNo == st.regNo }
                        val draft = state.draftChanges[st.regNo]
                        val curType = draft?.first ?: saved?.type.orEmpty()
                        val curPeriod = draft?.second ?: saved?.period ?: 1

                        Surface(color = Navy2, shape = RoundedCornerShape(8.dp)) {
                            Column(Modifier.padding(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${st.rollNo}. ${st.fullName}", color = GoldLight, fontWeight = FontWeight.Bold)
                                        if (st.bestPhone.isNotBlank()) {
                                            Text("📞 ${st.bestPhone}", color = TextMuted, fontSize = 11.sp)
                                        }
                                    }
                                    MobileCallWaButtons(mobile = st.bestPhone, recipientLabel = st.fullName, student = st)
                                    TextButton(onClick = { onOpenProfile(st.regNo) }) { Text("🪪", fontSize = 16.sp) }
                                }
                                if (isAbs) {
                                    Text("🔴 गैरहजर — नोंद करता येत नाही", color = SoftRed, fontSize = 12.sp)
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SgsDropdown(
                                            label = "प्रकार",
                                            selected = curType,
                                            options = listOf("", "permitted", "unpermitted"),
                                            onSelect = { ops.setEarlyLeaveDraft(st.regNo, it, curPeriod) },
                                            displayTransform = {
                                                when (it) {
                                                    "permitted" -> "✅ परवानगी"
                                                    "unpermitted" -> "⚠️ विनापरवानगी"
                                                    else -> "— नोंद नाही —"
                                                }
                                            },
                                            modifier = Modifier.weight(1.3f)
                                        )
                                        SgsDropdown(
                                            label = "तासिका",
                                            selected = curPeriod.toString(),
                                            options = (1..9).map { it.toString() },
                                            onSelect = { ops.setEarlyLeaveDraft(st.regNo, curType.ifBlank { "permitted" }, it.toIntOrNull() ?: 1) },
                                            modifier = Modifier.weight(0.7f)
                                        )
                                    }
                                    if (curType == "permitted" || curType == "unpermitted") {
                                        val rowMsg = "नमस्कार, आपला पाल्य ${st.fullName} दिनांक ${MarathiUtils.fmtDate(state.date)} रोजी तासिका क्र. $curPeriod नंतर शाळेतून ${if (curType == "permitted") "परवानगी घेऊन" else "विनापरवानगी"} घरी गेला/गेली आहे. — श्री. गो. से. हायस्कूल, पाचोरा"
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                            if (st.bestPhone.isNotBlank()) {
                                                OutlinedButton(onClick = { dialPhone(context, st.bestPhone) }) {
                                                    Text("📞 कॉल", color = TextCream, fontSize = 11.sp)
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    launchWa(
                                                        WhatsAppLaunchRequest(
                                                            mobile = st.bestPhone,
                                                            defaultMessage = rowMsg,
                                                            recipientLabel = st.fullName,
                                                            student = st
                                                        )
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                                            ) {
                                                Text(if (st.bestPhone.isNotBlank()) "💬 WhatsApp पालक" else "💬 WhatsApp निवडा", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = ops::saveEarlyLeave,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("el_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("नोंदी Save करा", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        SgsCard(title = "📋 आजची यादी (${state.records.size})") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all" to "सर्व", "permitted" to "✅ परवानगी", "unpermitted" to "⚠️ विनापरवानगी").forEach { (k, lbl) ->
                    FilterChip(selected = state.filter == k, onClick = { ops.setEarlyLeaveFilters(filter = k) }, label = { Text(lbl) })
                }
            }
            Spacer(Modifier.height(8.dp))
            val list = state.records.filter { state.filter == "all" || it.type == state.filter }
            if (list.isNotEmpty()) {
                Button(
                    onClick = { staffBulkIdx = 0; showStaffBulkWa = true },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                    modifier = Modifier.fillMaxWidth().testTag("el_bulk_wa_btn")
                ) {
                    Text("📢 सर्व घरी गेलेल्यांना WhatsApp रांग (${list.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (list.isEmpty()) Text("कोणतीही नोंद नाही.", color = TextMuted)
            else list.forEach { rec ->
                val recStu = Student(regNo = rec.regNo, iyatta = rec.iyatta, tukdi = rec.tukdi, fullName = rec.fullName, whatsappMobile = rec.bestPhone)
                Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${rec.fullName} (${MarathiUtils.classToMr(rec.iyatta)} ${rec.tukdi}) — तासिका ${rec.period}", color = GoldLight, fontWeight = FontWeight.Bold)
                        Text(
                            (if (rec.type == "permitted") "✅ परवानगी (${rec.outStatus.ifBlank { "बाकी" }})" else "⚠️ विनापरवानगी") +
                                if (rec.bestPhone.isNotBlank()) " • 📞 ${rec.bestPhone}" else "",
                            color = if (rec.type == "permitted") MintGreen else SoftRed,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            if (rec.bestPhone.isNotBlank()) {
                                OutlinedButton(onClick = { dialPhone(context, rec.bestPhone) }) { Text("📞 कॉल", color = TextCream, fontSize = 11.sp) }
                            }
                            Button(
                                onClick = {
                                    val msg = "नमस्कार, आपला पाल्य ${rec.fullName} दिनांक ${MarathiUtils.fmtDate(rec.date)} रोजी तासिका क्र. ${rec.period} नंतर शाळेतून ${if (rec.type == "permitted") "परवानगी घेऊन" else "विनापरवानगी"} घरी गेला/गेली आहे. — श्री. गो. से. हायस्कूल, पाचोरा"
                                    launchWa(
                                        WhatsAppLaunchRequest(
                                            mobile = rec.bestPhone,
                                            defaultMessage = msg,
                                            recipientLabel = rec.fullName,
                                            student = recStu
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                            ) { Text(if (rec.bestPhone.isNotBlank()) "💬 WhatsApp पालक" else "💬 WhatsApp निवडा", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }

    if (showStaffBulkWa) {
        val list = state.records.filter { state.filter == "all" || it.type == state.filter }
        val current = list.getOrNull(staffBulkIdx)
        if (current == null) {
            showStaffBulkWa = false
        } else {
            val defaultMsg = "नमस्कार, आपला पाल्य ${current.fullName} (${MarathiUtils.classToMr(current.iyatta)} ${current.tukdi}) दिनांक ${MarathiUtils.fmtDate(current.date)} रोजी तासिका क्र. ${current.period} नंतर शाळेतून ${if (current.type == "permitted") "परवानगी घेऊन" else "विनापरवानगी"} घरी गेला/गेली आहे. — श्री. गो. से. हायस्कूल, पाचोरा"
            val selectedTpl = templates.firstOrNull { it.id == staffBulkTplId }
            val curStu = Student(regNo = current.regNo, iyatta = current.iyatta, tukdi = current.tukdi, fullName = current.fullName, whatsappMobile = current.bestPhone)
            val msg = if (selectedTpl == null) defaultMsg else MarathiUtils.formatMessageTemplate(selectedTpl.body, curStu)
            AlertDialog(
                onDismissRequest = { showStaffBulkWa = false },
                containerColor = Navy,
                title = { Text("📢 घरी गेलेले WhatsApp (${staffBulkIdx + 1} / ${list.size})", color = GoldLight, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${current.fullName} — तासिका ${current.period}", color = TextCream, fontWeight = FontWeight.Bold)
                        Text(
                            if (current.bestPhone.isNotBlank()) "📱 मोबाईल: ${current.bestPhone}" else "⚠️ मोबाईल नोंद नाही — WhatsApp मध्ये Contact निवडा",
                            color = if (current.bestPhone.isNotBlank()) MintGreen else GoldLight,
                            fontSize = 12.sp
                        )
                        if (templates.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(selected = staffBulkTplId == "__default__", onClick = { staffBulkTplId = "__default__" }, label = { Text("✨ मूळ संदेश", fontSize = 10.5.sp) })
                                templates.forEach { t ->
                                    FilterChip(selected = staffBulkTplId == t.id, onClick = { staffBulkTplId = t.id }, label = { Text(t.title, fontSize = 10.5.sp) })
                                }
                            }
                        }
                        Surface(color = Navy2, shape = RoundedCornerShape(8.dp)) {
                            Text(msg, color = TextCream, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                        if (current.bestPhone.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    sendSms(context, current.bestPhone, msg)
                                    if (staffBulkIdx + 1 < list.size) staffBulkIdx++ else showStaffBulkWa = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldLight)
                                Spacer(Modifier.size(4.dp))
                                Text("✉️ SMS व पुढे", color = GoldLight, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openWhatsApp(context, current.bestPhone, msg)
                            if (staffBulkIdx + 1 < list.size) staffBulkIdx++ else showStaffBulkWa = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                    ) { Text(if (staffBulkIdx + 1 < list.size) "💬 पाठवा व पुढे" else "💬 पाठवा व पूर्ण") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (staffBulkIdx + 1 < list.size) {
                            TextButton(onClick = { staffBulkIdx++ }) { Text("पुढे (Skip)", color = GoldLight) }
                        }
                        TextButton(onClick = { showStaffBulkWa = false }) { Text("बंद करा", color = TextMuted) }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScholarshipScreen(
    role: UserRole,
    state: ScholarshipUiState,
    ops: TeacherAndOpsDelegate
) {
    val canDelete = role == UserRole.SUPER || role == UserRole.MASTER
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgsCard(title = "🎓 शिष्यवृत्ती नोंदवही") {
            if (role != UserRole.TEACHER) {
                val classKeys = listOf("ALL|") + state.classes.map { it.key }
                SgsDropdown(
                    label = "वर्ग निवडा",
                    selected = state.selectedClassKey,
                    options = classKeys,
                    onSelect = ops::setScholarshipClass,
                    displayTransform = { k ->
                        if (k == "ALL|") "📋 सर्व वर्ग" else {
                            val p = k.split("|")
                            "${MarathiUtils.classToMr(p[0])} ${p.getOrNull(1).orEmpty()}"
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            // Summary & Filter Chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("ALL") + MarathiUtils.SCHOLARSHIP_STATUSES).forEach { st ->
                    val cnt = if (st == "ALL") state.records.size else state.records.count { it.status == st }
                    FilterChip(
                        selected = state.filterStatus == st,
                        onClick = { ops.setScholarshipFilter(st) },
                        label = { Text("${if (st == "ALL") "सर्व" else st} ($cnt)") }
                    )
                }
            }
        }

        if (state.selectedClassKey != "ALL|" && state.roster.isNotEmpty()) {
            SgsCard(title = if (state.editId.isBlank()) "➕ नवीन शिष्यवृत्ती नोंद" else "✏️ नोंद अद्ययावत करा") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val regOpts = state.roster.map { it.regNo }
                    val labels = state.roster.associate { it.regNo to "${it.rollNo} - ${it.fullName}" }
                    SgsDropdown("विद्यार्थी", state.formRegNo, regOpts, { ops.updateScholarshipForm(regNo = it) }, displayTransform = { labels[it] ?: "— निवडा —" })
                    SgsTextField(state.formName, { ops.updateScholarshipForm(name = it) }, "शिष्यवृत्तीचे नाव*", tag = "sch_name_input")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SgsTextField(state.formScheme, { ops.updateScholarshipForm(scheme = it) }, "योजना", modifier = Modifier.weight(1f))
                        SgsTextField(state.formAmount, { ops.updateScholarshipForm(amount = it) }, "रक्कम (₹)", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    }
                    SgsDropdown("स्थिती", state.formStatus, MarathiUtils.SCHOLARSHIP_STATUSES, { ops.updateScholarshipForm(status = it) })
                    SgsTextField(state.formRemark, { ops.updateScholarshipForm(remark = it) }, "शेरा", enableVoice = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ops::saveScholarship, colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.weight(1f).testTag("sch_save_btn")) {
                            Text("Save करा", fontWeight = FontWeight.Bold)
                        }
                        if (state.editId.isNotBlank()) {
                            OutlinedButton(onClick = ops::resetScholarshipForm, modifier = Modifier.weight(1f)) { Text("रद्द", color = SoftRed) }
                        }
                    }
                    BannerView(state.statusBanner)
                }
            }
        }

        val visible = state.records.filter { state.filterStatus == "ALL" || it.status == state.filterStatus }
        SgsCard(title = "📋 यादी (${visible.size})") {
            if (visible.isEmpty()) Text("कोणतीही शिष्यवृत्ती नोंद नाही.", color = TextMuted)
            else visible.forEach { r ->
                Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${r.fullName} (${MarathiUtils.classToMr(r.iyatta)} ${r.tukdi})", color = GoldLight, fontWeight = FontWeight.Bold)
                            Text("${r.name} • ${MarathiUtils.fmtRupees(r.amount)} • ${r.status}", color = TextCream, fontSize = 12.5.sp)
                            if (r.remark.isNotBlank()) Text(r.remark, color = TextMuted, fontSize = 11.5.sp)
                        }
                        IconButton(onClick = {
                            ops.updateScholarshipForm(r.id, r.regNo, r.name, r.scheme, r.amount.toString(), r.status, r.remark)
                        }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GoldLight) }
                        if (canDelete) {
                            IconButton(onClick = { ops.deleteScholarship(r.id) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SoftRed) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassInfoScreen(
    classes: List<ClassDivisionPair>,
    students: List<Student>,
    verifyBaseUrl: String = "",
    acYear: String = "2026-27",
    templates: List<MessageTemplate> = emptyList(),
    onOpenManageTemplates: () -> Unit = {},
    onLoadClass: (String, String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf("") }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SgsCard(title = "🏫 वर्ग माहिती") {
            val keys = classes.map { it.key }
            SgsDropdown(
                label = "वर्ग निवडा",
                selected = selected,
                options = keys,
                onSelect = { k ->
                    selected = k
                    val p = k.split("|")
                    onLoadClass(p[0], p.getOrNull(1).orEmpty())
                },
                displayTransform = { k ->
                    val p = k.split("|")
                    if (p[0].isBlank()) "— निवडा —" else "${MarathiUtils.classToMr(p[0])} ${p.getOrNull(1).orEmpty()}"
                }
            )
            if (students.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val safeClass = selected.replace("|", "_").ifBlank { "class" }
                            val pdf = PdfAndExportUtils.renderStudentIdCardPdf(
                                context = context,
                                students = students,
                                verifyBaseUrl = verifyBaseUrl,
                                acYear = acYear,
                                fileName = "SGS_ID_Cards_$safeClass.pdf"
                            )
                            PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "वर्ग ID Card PDF")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.weight(1f).testTag("class_id_card_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("🪪 वर्ग ID Card PDF (${students.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val safeClass = selected.replace("|", "_").ifBlank { "class" }
                            val pdf = PdfAndExportUtils.renderStudentIdCardPdf(
                                context = context,
                                students = students,
                                verifyBaseUrl = verifyBaseUrl,
                                acYear = acYear,
                                fileName = "SGS_ID_Cards_$safeClass.pdf"
                            )
                            PdfAndExportUtils.printPdfFile(context, pdf, "SGS Class ID Cards")
                        },
                        modifier = Modifier.testTag("class_id_card_print_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "वर्ग ID Print", modifier = Modifier.size(16.dp), tint = GoldLight)
                        Spacer(Modifier.size(4.dp))
                        Text("Print", color = GoldLight, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showBroadcastDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                    modifier = Modifier.fillMaxWidth().testTag("class_broadcast_btn")
                ) {
                    Text("📢 वर्ग मेसेज प्रसारण — WhatsApp / SMS (${students.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        SgsCard(title = "👥 विद्यार्थी (${students.size})") {
            students.forEach { st ->
                Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${st.rollNo}. ${st.fullName}", color = GoldLight, fontWeight = FontWeight.Bold)
                            Text("Reg: ${st.regNo} | ${st.bestPhone.ifBlank { "—" }}", color = TextMuted, fontSize = 12.sp)
                        }
                        MobileCallWaButtons(mobile = st.bestPhone, recipientLabel = st.fullName, student = st)
                        Spacer(Modifier.size(4.dp))
                        OutlinedButton(onClick = { onOpenProfile(st.regNo) }) { Text("Profile", color = TextCream, fontSize = 11.sp) }
                    }
                }
            }
        }
    }

    if (showBroadcastDialog) {
        val parts = selected.split("|")
        val clsLabel = if (parts.getOrNull(0).isNullOrBlank()) "" else "${MarathiUtils.classToMr(parts[0])} ${parts.getOrNull(1).orEmpty()}".trim()
        ClassBroadcastQueueDialog(
            title = "📢 वर्ग मेसेज प्रसारण ($clsLabel)",
            students = students,
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
fun ClassTeacherListScreen(teachers: List<ClassTeacherItem>) {
    val context = LocalContext.current
    val launchWa = LocalWhatsAppLauncher.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SgsCard(title = "📒 वर्ग शिक्षक संपर्क यादी (${teachers.size})") {
            if (teachers.isEmpty()) Text("माहिती उपलब्ध नाही.", color = TextMuted)
            else teachers.forEach { t ->
                val tName = t.nameMr.ifBlank { t.nameEn }
                val clsLbl = "${t.classMr} ${t.div}".trim()
                Surface(color = Navy2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("$clsLbl — $tName", color = GoldLight, fontWeight = FontWeight.Bold)
                            Text("मोबाईल: ${t.mobile.ifBlank { "—" }}", color = TextCream, fontSize = 12.sp)
                        }
                        if (t.mobile.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { dialPhone(context, t.mobile) }) { Icon(Icons.Default.Call, contentDescription = "कॉल", tint = MintGreen) }
                                Button(
                                    onClick = {
                                        launchWa(
                                            WhatsAppLaunchRequest(
                                                mobile = t.mobile,
                                                defaultMessage = "नमस्कार $tName सर/मॅडम ($clsLbl), ",
                                                recipientLabel = tName,
                                                student = Student(fullName = tName, whatsappMobile = t.mobile),
                                                customClassLabel = clsLbl
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                                ) {
                                    Text("WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenanceScreen(
    role: UserRole,
    status: BannerMessage?,
    ops: TeacherAndOpsDelegate
) {
    var regInput by remember { mutableStateOf("") }
    var newClass by remember { mutableStateOf("5th") }
    var newDiv by remember { mutableStateOf("अ") }
    var feeInput by remember { mutableStateOf("1000") }
    var delReg by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BannerView(status)

        SgsCard(title = "🔄 वर्ग / तुकडी बदल") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(regInput, { regInput = it }, "Reg No टाका", tag = "mnt_reg_input")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("नवीन इयत्ता", newClass, MarathiUtils.CLASSES, { newClass = it }, modifier = Modifier.weight(1f), displayTransform = { MarathiUtils.classToMr(it) })
                    SgsDropdown("नवीन तुकडी", newDiv, MarathiUtils.DIVISIONS, { newDiv = it }, modifier = Modifier.weight(1f))
                }
                Button(onClick = { ops.updateClassDivision(regInput, newClass, newDiv) }, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.fillMaxWidth().testTag("mnt_update_class_btn")) {
                    Text("वर्ग/तुकडी अद्ययावत करा")
                }
            }
        }

        SgsCard(title = "💰 वार्षिक एकूण फी सेटिंग (प्रति विद्यार्थी)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SgsTextField(feeInput, { feeInput = it }, "रक्कम (₹)", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), tag = "mnt_fee_input")
                Button(onClick = { ops.saveTotalFeeSetting(feeInput) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.height(54.dp).testTag("mnt_save_fee_btn")) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (role == UserRole.SUPER || role == UserRole.MASTER) {
            SgsCard(title = "🗑️ विद्यार्थी कायमस्वरूपी Delete करा") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚠️ Delete करण्यापूर्वी Deleted_Students Sheet मध्ये Backup जतन केला जातो.", color = SoftRed, fontSize = 12.sp)
                    SgsTextField(delReg, { delReg = it }, "Delete करायचा Reg No", tag = "mnt_delete_reg_input")
                    Button(
                        onClick = { if (delReg.isNotBlank()) confirmDelete = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                        modifier = Modifier.fillMaxWidth().testTag("mnt_delete_btn")
                    ) { Text("विद्यार्थी Delete करा", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Navy,
            title = { Text("⚠️ खात्री करा", color = SoftRed) },
            text = { Text("Reg No '$delReg' हा विद्यार्थी कायमचा Delete करायचा का?", color = TextCream) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        ops.deleteStudentPermanent(delReg)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson)
                ) { Text("हो, Delete करा") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("रद्द", color = TextCream) } }
        )
    }
}
