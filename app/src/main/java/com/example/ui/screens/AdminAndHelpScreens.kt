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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.domain.model.MessageTemplate
import com.example.domain.model.Student
import com.example.domain.model.UserAccount
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
import com.example.ui.components.BannerView
import com.example.ui.components.LocalWhatsAppLauncher
import com.example.ui.components.MobileCallWaButtons
import com.example.ui.components.SgsCard
import com.example.ui.components.SgsDropdown
import com.example.ui.components.SgsTextField
import com.example.ui.components.WhatsAppLaunchRequest
import com.example.ui.components.dialPhone
import com.example.ui.components.openWhatsApp
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
import com.example.ui.viewmodel.AdminAndQcUiState
import com.example.ui.viewmodel.ReportsAndAdminDelegate

@Composable
fun UserManagementScreen(
    role: UserRole,
    state: AdminAndQcUiState,
    admin: ReportsAndAdminDelegate
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var uName by remember { mutableStateOf("") }
    var uPass by remember { mutableStateOf("") }
    var uRole by remember { mutableStateOf("teacher") }
    var uLabel by remember { mutableStateOf("") }
    var uClass by remember { mutableStateOf("") }
    var uMobile by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BannerView(state.userStatus)

        SgsCard(title = "🔐 माझा Password बदला") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(oldPass, { oldPass = it }, "सध्याचा Password")
                SgsTextField(newPass, { newPass = it }, "नवीन Password")
                Button(onClick = { admin.changeMyPassword(oldPass, newPass) }, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.fillMaxWidth()) {
                    Text("Password बदला")
                }
            }
        }

        if (role == UserRole.SUPER) {
            SgsCard(title = "➕ नवीन User जोडा / बदला") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(uName, { uName = it }, "Username")
                    SgsTextField(uPass, { uPass = it }, "Password")
                    SgsDropdown("भूमिका (Role)", uRole, listOf("master", "deo", "cert", "teacher", "peon"), { uRole = it })
                    SgsTextField(uLabel, { uLabel = it }, "नाव / पद (Label)")
                    SgsTextField(uClass, { uClass = it }, "वर्ग (उदा. 7th|अ)")
                    SgsTextField(uMobile, { uMobile = it }, "मोबाईल", keyboardType = KeyboardType.Phone)
                    Button(
                        onClick = {
                            admin.saveUser(UserAccount(0, uName, uPass, uRole, uLabel, uClass, uMobile))
                            uName = ""; uPass = ""; uLabel = ""; uClass = ""; uMobile = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.fillMaxWidth().testTag("users_save_btn")
                    ) { Text("User Save करा", fontWeight = FontWeight.Bold) }
                }
            }

            SgsCard(title = "👥 सर्व Users (${state.users.size})") {
                if (state.usersLoading) CircularProgressIndicator(color = Gold)
                state.users.forEach { u ->
                    Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("${u.username} (${u.role}) — ${u.label}", color = GoldLight, fontWeight = FontWeight.Bold)
                                Text("वर्ग: ${u.assignedClass.ifBlank { "सर्व" }} | Mob: ${u.mobile.ifBlank { "—" }}", color = TextMuted, fontSize = 12.sp)
                            }
                            MobileCallWaButtons(
                                mobile = u.mobile,
                                recipientLabel = u.label.ifBlank { u.username },
                                student = Student(fullName = u.label.ifBlank { u.username }, whatsappMobile = u.mobile),
                                customClassLabel = u.assignedClass
                            )
                            if (u.username.isNotBlank() && u.role != "super") {
                                IconButton(onClick = { admin.deleteUser(u.username) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SoftRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperAdminScreen(
    state: AdminAndQcUiState,
    admin: ReportsAndAdminDelegate,
    templates: List<MessageTemplate> = emptyList()
) {
    val context = LocalContext.current
    val launchWa = LocalWhatsAppLauncher.current
    var promoYear by remember { mutableStateOf(state.newAcYearInput) }
    var promoConfirm by remember { mutableStateOf("") }
    var newYearStart by remember { mutableStateOf("2027-28") }
    var newYearConfirm by remember { mutableStateOf("") }
    var archYear by remember { mutableStateOf("2026-27") }
    var archConfirm by remember { mutableStateOf("") }
    var passOutClass by remember { mutableStateOf("10th") }
    var passOutLabel by remember { mutableStateOf("2026-27") }
    var passOutConfirm by remember { mutableStateOf("") }

    var bkSchedule by remember(state.backupSchedule) { mutableStateOf(state.backupSchedule.ifBlank { "off" }) }
    var bkExtraIds by remember(state.backupExtraIds) { mutableStateOf(state.backupExtraIds) }
    var bkAlertEmail by remember(state.backupAlertEmail) { mutableStateOf(state.backupAlertEmail) }
    var rebuildFeeYear by remember { mutableStateOf("2026-27") }
    var renumberClasses by remember { mutableStateOf(MarathiUtils.CLASSES.toSet()) }
    var renumberOrder by remember { mutableStateOf("ASC") }
    var confirmActionType by remember { mutableStateOf<String?>(null) }
    var confirmActionText by remember { mutableStateOf("") }
    var showSaFeeQueue by remember { mutableStateOf(false) }
    var saFeeQueueIdx by remember { mutableIntStateOf(0) }
    var saFeeTplId by remember { mutableStateOf("__default__") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BannerView(state.superStatus)

        SgsCard(title = "🔐 सुरक्षा स्थिती") {
            state.securityLines.forEach { line -> Text(line, color = TextCream, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        }

        SgsCard(title = "💰 शाळा-व्यापी फी थकबाकी व सारांश") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = admin::loadPendingFeesAll, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.weight(1f).testTag("sa_load_fees_btn")) {
                    Text("थकबाकी Load", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
                Button(
                    onClick = {
                        val rows = state.pendingFees.mapIndexed { i, x -> listOf((i + 1).toString(), x.fullName, "${x.iyatta} ${x.tukdi}", x.rollNo, x.paid.toString(), x.pending.toString(), x.mobile) }
                        val pdf = PdfAndExportUtils.renderTablePdf(context, "शाळा फी थकबाकी यादी", state.pendingFeesSummary, listOf("क्र.", "नाव", "वर्ग", "Roll", "जमा", "बाकी", "मोबाईल"), rows, "Pending_Fees.pdf")
                        PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Pending Fees PDF")
                    },
                    enabled = state.pendingFees.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier.weight(0.6f)
                ) { Text("PDF", fontSize = 11.5.sp) }
                OutlinedButton(
                    onClick = {
                        val rows = state.pendingFees.mapIndexed { i, x -> listOf((i + 1).toString(), x.fullName, "${x.iyatta} ${x.tukdi}", x.rollNo, x.paid.toString(), x.pending.toString(), x.mobile) }
                        val pdf = PdfAndExportUtils.renderTablePdf(context, "शाळा फी थकबाकी यादी", state.pendingFeesSummary, listOf("क्र.", "नाव", "वर्ग", "Roll", "जमा", "बाकी", "मोबाईल"), rows, "Pending_Fees.pdf")
                        PdfAndExportUtils.printPdfFile(context, pdf, "SGS Pending Fees")
                    },
                    enabled = state.pendingFees.isNotEmpty(),
                    modifier = Modifier.testTag("sa_pending_fees_print_btn")
                ) {
                    Icon(Icons.Default.Print, contentDescription = "थकबाकी प्रिंट", modifier = Modifier.size(16.dp), tint = GoldLight)
                }
                Button(
                    onClick = {
                        val rows = state.pendingFees.mapIndexed { i, x -> listOf((i + 1).toString(), x.fullName, "${x.iyatta} ${x.tukdi}", x.rollNo, x.paid.toString(), x.pending.toString(), x.mobile) }
                        val csv = PdfAndExportUtils.writeCsvFile(context, "Pending_Fees.csv", listOf("क्र.", "नाव", "वर्ग", "Roll", "जमा", "बाकी", "मोबाईल"), rows)
                        PdfAndExportUtils.shareFile(context, csv, "text/csv", "Pending Fees CSV")
                    },
                    enabled = state.pendingFees.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    modifier = Modifier.weight(0.6f)
                ) { Text("CSV", fontSize = 11.5.sp) }
            }
            if (state.pendingFeesSummary.isNotBlank()) Text(state.pendingFeesSummary, color = GoldLight, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            if (state.pendingFees.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        saFeeQueueIdx = 0
                        showSaFeeQueue = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                    modifier = Modifier.fillMaxWidth().testTag("sa_fee_bulk_wa_btn")
                ) {
                    Text("📢 थकबाकी WhatsApp रांग सुरू करा (${state.pendingFees.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            state.pendingFees.take(25).forEach { x ->
                val xStu = Student(regNo = x.regNo, iyatta = x.iyatta, tukdi = x.tukdi, rollNo = x.rollNo, fullName = x.fullName, whatsappMobile = x.mobile)
                val xMsg = "नमस्कार, आपला पाल्य ${x.fullName} याची शालेय फी ₹${x.pending.toInt()} बाकी आहे. कृपया लवकरात लवकर भरावी. — श्री.गो.से.हायस्कूल, पाचोरा"
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("${x.fullName} (${x.iyatta} ${x.tukdi}) — बाकी ₹${x.pending.toInt()}", color = TextCream, fontSize = 12.sp)
                        if (x.mobile.isNotBlank()) {
                            Text("📞 ${x.mobile}", color = MintGreen, fontSize = 11.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (x.mobile.isNotBlank()) {
                            IconButton(onClick = { dialPhone(context, x.mobile) }) {
                                Icon(Icons.Default.Call, contentDescription = "कॉल ${x.fullName}", tint = MintGreen)
                            }
                        }
                        Button(
                            onClick = {
                                launchWa(
                                    WhatsAppLaunchRequest(
                                        mobile = x.mobile,
                                        defaultMessage = xMsg,
                                        recipientLabel = x.fullName,
                                        student = xStu
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                        ) { Text(if (x.mobile.isNotBlank()) "आठवण" else "WA निवडा", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SgsTextField(rebuildFeeYear, { rebuildFeeYear = it }, "फी सारांश वर्ष", modifier = Modifier.weight(1f))
                Button(onClick = { admin.rebuildFeesSummary(rebuildFeeYear) }, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.height(54.dp)) {
                    Text("फी सारांश बांधा", fontSize = 11.5.sp)
                }
            }
        }

        SgsCard(title = "🎓 वर्ग-बढती (Promotion) व Undo") {
            state.promoClasses.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("${MarathiUtils.classToMr(c.iyatta)} (${c.count})", color = TextCream, modifier = Modifier.weight(0.9f))
                    SgsDropdown(
                        label = "पुढील वर्ग",
                        selected = c.selectedTarget,
                        options = listOf("") + MarathiUtils.CLASSES,
                        onSelect = { admin.setPromotionTarget(c.iyatta, it) },
                        displayTransform = { if (it.isBlank()) "— बढती नाही —" else MarathiUtils.classToMr(it) },
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            SgsTextField(promoYear, { promoYear = it }, "नवीन शैक्षणिक वर्ष (उदा. 2027-28)")
            Spacer(Modifier.height(6.dp))
            SgsTextField(promoConfirm, { promoConfirm = it }, "खात्रीसाठी PROMOTE टाइप करा")
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { if (promoConfirm.trim() == "PROMOTE") admin.runPromotion(promoYear) },
                colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                modifier = Modifier.fillMaxWidth()
            ) { Text("🎓 वर्ग-बढती चालवा", fontWeight = FontWeight.Bold) }

            if (state.promoBatches.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("↩️ अलीकडील बढती Undo करा:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                state.promoBatches.forEach { b ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${b.time} (${b.count} विद्यार्थी)", color = TextMuted, fontSize = 12.sp)
                        OutlinedButton(onClick = { admin.undoPromotion(b.batchId) }) { Text("↩️ Undo", color = GoldLight, fontSize = 11.sp) }
                    }
                }
            }
        }

        SgsCard(title = "🔢 रोल नं. क्रमवारी (Renumber Rolls)") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MarathiUtils.CLASSES.forEach { cls ->
                    FilterChip(
                        selected = cls in renumberClasses,
                        onClick = {
                            renumberClasses = if (cls in renumberClasses) renumberClasses - cls else renumberClasses + cls
                        },
                        label = { Text(MarathiUtils.classToMr(cls), fontSize = 11.sp) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            SgsDropdown(
                "क्रम", renumberOrder, listOf("ASC", "GIRLS_FIRST", "BOYS_FIRST"), { renumberOrder = it },
                displayTransform = { when (it) { "GIRLS_FIRST" -> "मुली प्रथम"; "BOYS_FIRST" -> "मुले प्रथम"; else -> "नावाप्रमाणे (A-Z)" } }
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { confirmActionType = "RENUMBER"; confirmActionText = "" },
                enabled = renumberClasses.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                modifier = Modifier.fillMaxWidth()
            ) { Text("रोल नं. अद्ययावत करा") }
        }

        SgsCard(title = "🧹 पास-आऊट Archive व नवीन शैक्षणिक वर्ष") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsDropdown("वर्ग निवडा", passOutClass, MarathiUtils.CLASSES, { passOutClass = it })
                SgsTextField(passOutLabel, { passOutLabel = it }, "Archive नाव (उदा. 2026-27)")
                SgsTextField(passOutConfirm, { passOutConfirm = it }, "ARCHIVE टाइप करा")
                Button(
                    onClick = {
                        if (passOutConfirm.trim() == "ARCHIVE") {
                            admin.runSuperAction("archivePassedOut", mapOf("iyatta" to passOutClass, "label" to passOutLabel, "confirm" to "ARCHIVE"), "Pass-out Archive पूर्ण!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📦 Pass-out Archive करा") }

                if (state.archiveBatches.isNotEmpty()) {
                    Text("↩️ Pass-out पुनर्संचयित करा:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    state.archiveBatches.forEach { b ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${MarathiUtils.classToMr(b.iyatta)} (${b.count}) — ${b.sheet}", color = TextMuted, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { admin.restorePassedOut(b.batchId) }) { Text("↩️ Restore", color = MintGreen, fontSize = 11.sp) }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                SgsTextField(newYearStart, { newYearStart = it }, "नवे वर्ष (उदा. 2027-28)")
                SgsTextField(newYearConfirm, { newYearConfirm = it }, "NEWYEAR टाइप करा")
                Button(
                    onClick = {
                        if (newYearConfirm.trim() == "NEWYEAR") {
                            admin.runSuperAction("startNewYear", mapOf("newYear" to newYearStart, "carry" to "1", "confirm" to "NEWYEAR"), "नवे वर्ष सुरू!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗓️ नवे शैक्षणिक वर्ष सुरू करा") }

                Spacer(Modifier.height(6.dp))
                SgsTextField(archYear, { archYear = it }, "Archive वर्ष (उदा. 2026-27)")
                SgsTextField(archConfirm, { archConfirm = it }, "ARCHIVEYEAR टाइप करा")
                Button(
                    onClick = {
                        if (archConfirm.trim() == "ARCHIVEYEAR") {
                            admin.runSuperAction("archiveYear", mapOf("year" to archYear, "confirm" to "ARCHIVEYEAR"), "वर्ष Archive पूर्ण!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📦 जुने वर्ष Archive फाइलमध्ये हलवा") }
            }
        }

        SgsCard(title = "💾 Backup, फोटो Recall व Sheet/Log टूल्स") {
            Text(state.backupInfo, color = TextCream, fontSize = 12.sp)
            Text(state.restoreDrillInfo, color = MintGreen, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { admin.runSuperAction("runBackup", emptyMap(), "Backup पूर्ण!") }, colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.weight(1f)) {
                    Text("💾 आत्ता Backup", fontSize = 12.sp)
                }
                Button(onClick = admin::runRestoreDrill, enabled = !state.restoreDrillRunning, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.weight(1f)) {
                    Text(if (state.restoreDrillRunning) "तपासत आहे..." else "🩺 Restore Drill", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            SgsDropdown("स्वयंचलित Backup वेळापत्रक", bkSchedule, listOf("off", "daily", "weekly"), { bkSchedule = it })
            Spacer(Modifier.height(6.dp))
            SgsTextField(bkExtraIds, { bkExtraIds = it }, "इतर File IDs (स्वल्पविरामाने)")
            Spacer(Modifier.height(6.dp))
            SgsTextField(bkAlertEmail, { bkAlertEmail = it }, "इशारा Email")
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { admin.setBackupSchedule(bkSchedule, bkExtraIds, bkAlertEmail) }, modifier = Modifier.fillMaxWidth()) {
                Text("वेळापत्रक Save करा", color = GoldLight)
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { admin.runPhotoRecall(true) }, enabled = !state.photoRecallRunning, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.weight(1f)) {
                    Text("📷 Photo Recall (तपासा)", fontSize = 11.5.sp)
                }
                Button(onClick = { admin.runPhotoRecall(false) }, enabled = !state.photoRecallRunning, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.weight(1f)) {
                    Text("📷 Photo जोडा", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (state.photoRecallRunning) {
                Spacer(Modifier.height(6.dp))
                Button(onClick = admin::stopPhotoRecall, colors = ButtonDefaults.buttonColors(containerColor = Crimson), modifier = Modifier.fillMaxWidth()) {
                    Text("⏹ Photo Recall थांबवा")
                }
            }
            if (state.photoRecallReport.isNotBlank()) Text(state.photoRecallReport, color = GoldLight, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { confirmActionType = "SHRINK"; confirmActionText = "" }, modifier = Modifier.weight(1f)) {
                    Text("🧹 Shrink Sheets", color = GoldLight, fontSize = 11.sp)
                }
                OutlinedButton(onClick = { confirmActionType = "LOG"; confirmActionText = "" }, modifier = Modifier.weight(1f)) {
                    Text("📜 Log स्थलांतर", color = TextCream, fontSize = 11.sp)
                }
                OutlinedButton(onClick = { admin.archiveLogNow(90) }, modifier = Modifier.weight(1f)) {
                    Text("📦 90d Log", color = MintGreen, fontSize = 11.sp)
                }
            }
            if (state.sheetSizes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Sheet आकार: " + state.sheetSizes.joinToString(", ") { "${it.first}(${it.second})" }, color = TextMuted, fontSize = 11.sp)
            }
        }
    }

    confirmActionType?.let { keyword ->
        AlertDialog(
            onDismissRequest = { confirmActionType = null },
            containerColor = Navy,
            titleContentColor = GoldLight,
            textContentColor = TextCream,
            title = { Text("⚠️ पुष्टीकरण ($keyword)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ही प्रक्रिया चालवण्यासाठी खाली '$keyword' टाईप करा:")
                    SgsTextField(confirmActionText, { confirmActionText = it }, keyword)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmActionType = null
                        when (keyword) {
                            "SHRINK" -> admin.shrinkSheets()
                            "RENUMBER" -> admin.renumberRolls(renumberClasses.toList(), renumberOrder)
                            "LOG" -> admin.migrateLogs()
                        }
                    },
                    enabled = confirmActionText.trim().equals(keyword, ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) { Text("चालवा") }
            },
            dismissButton = {
                TextButton(onClick = { confirmActionType = null }) { Text("रद्द", color = SoftRed) }
            }
        )
    }

    if (showSaFeeQueue) {
        val cur = state.pendingFees.getOrNull(saFeeQueueIdx)
        if (cur == null) {
            showSaFeeQueue = false
        } else {
            val defaultMsg = "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. आपला पाल्य ${cur.fullName} (${MarathiUtils.classToMr(cur.iyatta)} ${cur.tukdi}) यांची शालेय फी ₹${cur.pending.toInt()} शिल्लक बाकी आहे. कृपया लवकरात लवकर जमा करावी."
            val selTpl = templates.firstOrNull { it.id == saFeeTplId }
            val curStu = Student(regNo = cur.regNo, iyatta = cur.iyatta, tukdi = cur.tukdi, rollNo = cur.rollNo, fullName = cur.fullName, whatsappMobile = cur.mobile)
            val msg = if (selTpl == null) defaultMsg else MarathiUtils.formatMessageTemplate(selTpl.body, curStu)
            AlertDialog(
                onDismissRequest = { showSaFeeQueue = false },
                containerColor = Navy,
                title = { Text("📢 थकबाकी WhatsApp (${saFeeQueueIdx + 1} / ${state.pendingFees.size})", color = GoldLight, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${cur.fullName} (${cur.iyatta} ${cur.tukdi}) — बाकी ₹${cur.pending.toInt()}", color = SoftRed, fontWeight = FontWeight.Bold)
                        Text(
                            if (cur.mobile.isNotBlank()) "📱 मोबाईल: ${cur.mobile}" else "⚠️ मोबाईल नोंद नाही — WhatsApp मध्ये Contact निवडा",
                            color = if (cur.mobile.isNotBlank()) MintGreen else GoldLight,
                            fontSize = 12.sp
                        )
                        if (templates.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(selected = saFeeTplId == "__default__", onClick = { saFeeTplId = "__default__" }, label = { Text("✨ मूळ संदेश", fontSize = 10.5.sp) })
                                templates.forEach { t ->
                                    FilterChip(selected = saFeeTplId == t.id, onClick = { saFeeTplId = t.id }, label = { Text(t.title, fontSize = 10.5.sp) })
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
                            openWhatsApp(context, cur.mobile, msg)
                            if (saFeeQueueIdx + 1 < state.pendingFees.size) saFeeQueueIdx++ else showSaFeeQueue = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy)
                    ) { Text(if (saFeeQueueIdx + 1 < state.pendingFees.size) "💬 पाठवा व पुढे" else "💬 पाठवा व पूर्ण") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (saFeeQueueIdx + 1 < state.pendingFees.size) {
                            TextButton(onClick = { saFeeQueueIdx++ }) { Text("पुढे (Skip)", color = GoldLight) }
                        }
                        TextButton(onClick = { showSaFeeQueue = false }) { Text("बंद करा", color = TextMuted) }
                    }
                }
            )
        }
    }
}

@Composable
fun HelpManualScreen(role: UserRole) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SgsCard(title = "❓ वापरकर्ता मार्गदर्शिका — V19.65 (${role.displayLabel})") {
            val sections = buildList {
                when (role) {
                    UserRole.TEACHER -> {
                        add("📊 वर्ग शिक्षक डॅशबोर्ड" to "तुमच्या वर्गातील मुले/मुली, प्रवर्ग, वयोगट, आजची हजेरी स्थिती आणि वर्ग फी जमा येथे दिसते.")
                        add("✅ हजेरी" to "फक्त अनुपस्थित विद्यार्थ्यांवर टिक करा व कारण लिहा. पालकांना थेट Call, SMS किंवा WhatsApp पाठवा.")
                        add("📔 विद्यार्थी डायरी व प्रवास" to "प्रत्येक विद्यार्थ्याचे मूळ गाव, प्रवास साधन व डायरी नोंदी (संपूर्ण व जुने Archive फिल्टरसह) पहा.")
                        add("💰 फी" to "टप्प्याटप्प्याने फी जमा नोंद करा; एकूण जमा व बाकी रक्कम आपोआप मोजली जाते.")
                    }
                    UserRole.DEO -> {
                        add("👤 विद्यार्थी माहिती" to "नवीन विद्यार्थी नोंदवा किंवा Reg No/आधार ने शोधून Update करा. आधार क्रमांक १२ अंकी तपासा.")
                        add("📷 फोटो Upload" to "फोटो आपोआप <10 KB मध्ये Compress होऊन Google Drive ला Save होतो.")
                    }
                    UserRole.CERT -> {
                        add("📄 बोनाफाईड व ✅ उपस्थिती प्रमाणपत्र" to "Reg No ने विद्यार्थी भरा, भाषा (मराठी/English) निवडा आणि 'Save & Issue' दाबून QR कोड असलेली PDF/Print काढा. Search मधून Batch PDF काढा.")
                    }
                    UserRole.PEON -> {
                        add("🚪 घरी जाणारे (मार्क-आउट)" to "आजच्या परवानगी असलेल्या विद्यार्थ्यांच्या यादीत '✅ गेला' किंवा '❌ गेला नाही' अशी नोंद करा.")
                    }
                    UserRole.MASTER, UserRole.SUPER -> {
                        add("🏠 मुख्यपृष्ठ व रोजचा सारांश" to "दररोजची उपस्थिती, बाकी/पूर्ण वर्ग, वाढदिवस, नोटीस, Sheet क्षमता इशारा आणि रोजचा सारांश WhatsApp/PDF द्वारे पाठवा.")
                        add("📋 LC / बोनाफाईड / रिपोर्ट" to "संपूर्ण ३४-फील्ड LC, Batch Bonafide/Attendance PDF आणि कॉलम क्रमवारीसह कस्टम रिपोर्ट Portrait/Landscape PDF व CSV मध्ये काढा.")
                        add("📦 Archive व तुलना" to "मागील वर्षांची फी, प्रवास, हजेरी, शिष्यवृत्ती व दोन वर्षांची तुलना (Compare) पहा.")
                        if (role == UserRole.SUPER) {
                            add("🛡️ Super Admin व 🔎 डेटा तपासणी" to "थकबाकी, वर्ग-बढती (Undo सह), रोल नं. क्रमवारी, Backup वेळापत्रक, Restore Drill, Live Photo Recall, Sheet क्षमता, Client त्रुटी आणि ९-पायऱ्यांचा वर्ष-अखेर मार्गदर्शक वापरा.")
                        }
                    }
                }
                add("📴 ऑफलाइन लॉगिन व Pending Saves" to "३ दिवस ऑफलाइन लॉगिन सुविधा (५ वेळा चुकीचा पासवर्ड टाकल्यास ५ मिनिटे लॉक). इंटरनेट नसताना नोंदी फोनमध्ये सुरक्षित राहतात आणि JSON Export देखील उपलब्ध आहे.")
            }

            sections.forEach { (title, body) ->
                Surface(color = Navy2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(title, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(body, color = TextCream, fontSize = 12.5.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}
