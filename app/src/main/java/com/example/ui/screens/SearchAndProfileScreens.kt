package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppPage
import com.example.domain.model.Student
import com.example.domain.model.UserRole
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
import com.example.ui.components.LocalWhatsAppLauncher
import com.example.ui.components.MobileCallWaButtons
import com.example.ui.components.SgsCard
import com.example.ui.components.SgsDropdown
import com.example.ui.components.SgsTextField
import com.example.ui.components.StudentPhotoBox
import com.example.ui.components.WhatsAppLaunchRequest
import com.example.ui.components.dialPhone
import com.example.ui.theme.ColorTokens
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
import com.example.ui.viewmodel.SearchAndProfileUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchStudentsScreen(
    role: UserRole,
    state: SearchAndProfileUiState,
    verifyBaseUrl: String,
    acYear: String = "2026-27",
    selectedBatchStudents: List<Student>,
    onFilterChange: (name: String?, father: String?, surname: String?, dob: String?, cls: String?, div: String?, aadhar: String?) -> Unit,
    onBulkLoad: () -> Unit,
    onOpenStudentIn: (Student, AppPage) -> Unit,
    onToggleBatch: (String) -> Unit,
    onClearBatch: () -> Unit,
    onOpenQrScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgsCard(title = "🔍 विद्यार्थी शोधा (जलद स्थानीय शोध)") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(state.nameQuery, { onFilterChange(it, null, null, null, null, null, null) }, "नाव / Reg No", tag = "search_name_input", enableVoice = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(state.fatherQuery, { onFilterChange(null, it, null, null, null, null, null) }, "वडिलांचे नाव", modifier = Modifier.weight(1f))
                    SgsTextField(state.surnameQuery, { onFilterChange(null, null, it, null, null, null, null) }, "आडनाव", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("वर्ग", state.classFilter, listOf("") + MarathiUtils.CLASSES, { onFilterChange(null, null, null, null, it, null, null) }, modifier = Modifier.weight(1f), displayTransform = { if (it.isBlank()) "सर्व" else MarathiUtils.classToMr(it) })
                    SgsDropdown("तुकडी", state.divFilter, listOf("") + MarathiUtils.DIVISIONS, { onFilterChange(null, null, null, null, null, it, null) }, modifier = Modifier.weight(1f), displayTransform = { if (it.isBlank()) "सर्व" else it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(state.aadharQuery, { onFilterChange(null, null, null, null, null, null, it) }, "आधार क्र.", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onOpenQrScanner, modifier = Modifier.height(54.dp).testTag("search_qr_scan_btn")) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QR स्कॅन", tint = GoldLight)
                    }
                    Button(onClick = onBulkLoad, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.height(54.dp).testTag("search_bulk_load_btn")) {
                        Text("⚡ Load All", fontSize = 12.sp)
                    }
                }
            }
        }

        if (state.recentSearches.isNotEmpty()) {
            SgsCard(title = "🕒 अलीकडील शोध") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.recentSearches.forEach { st ->
                        AssistChip(
                            onClick = { onOpenStudentIn(st, AppPage.PROFILE) },
                            label = { Text("${st.fullName} (${st.regNo})", fontSize = 11.5.sp) }
                        )
                    }
                }
            }
        }

        if (state.selectedBatchRegNos.isNotEmpty()) {
            SgsCard(title = "📦 Batch निवड (${state.selectedBatchRegNos.size} विद्यार्थी)") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (selectedBatchStudents.isNotEmpty()) {
                                val pdf = PdfAndExportUtils.renderBatchBonafidePdf(context, selectedBatchStudents, verifyBaseUrl)
                                PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Batch Bonafide PDF")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.testTag("batch_bonafide_pdf_btn")
                    ) { Text("Batch Bonafide PDF") }
                    Button(
                        onClick = {
                            if (selectedBatchStudents.isNotEmpty()) {
                                val pdf = PdfAndExportUtils.renderBatchAttendancePdf(context, selectedBatchStudents, verifyBaseUrl)
                                PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Batch Attendance PDF")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.testTag("batch_attendance_pdf_btn")
                    ) { Text("Batch Attendance PDF") }
                    Button(
                        onClick = {
                            if (selectedBatchStudents.isNotEmpty()) {
                                val pdf = PdfAndExportUtils.renderStudentIdCardPdf(
                                    context = context,
                                    students = selectedBatchStudents,
                                    verifyBaseUrl = verifyBaseUrl,
                                    acYear = acYear,
                                    fileName = "Batch_ID_Cards.pdf"
                                )
                                PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Batch ID Card PDF")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                        modifier = Modifier.testTag("batch_idcard_pdf_btn")
                    ) { Text("🪪 Batch ID Card PDF", fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = {
                            if (selectedBatchStudents.isNotEmpty()) {
                                val pdf = PdfAndExportUtils.renderStudentIdCardPdf(
                                    context = context,
                                    students = selectedBatchStudents,
                                    verifyBaseUrl = verifyBaseUrl,
                                    acYear = acYear,
                                    fileName = "Batch_ID_Cards.pdf"
                                )
                                PdfAndExportUtils.printPdfFile(context, pdf, "SGS Batch ID Cards")
                            }
                        },
                        modifier = Modifier.testTag("batch_idcard_print_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Batch ID Print", modifier = Modifier.size(16.dp), tint = GoldLight)
                        Spacer(Modifier.size(4.dp))
                        Text("ID Print", color = GoldLight, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            if (selectedBatchStudents.isNotEmpty()) {
                                val pdf = PdfAndExportUtils.renderBatchBonafidePdf(context, selectedBatchStudents, verifyBaseUrl)
                                PdfAndExportUtils.printPdfFile(context, pdf, "SGS Batch Bonafide")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Batch Bonafide Print", modifier = Modifier.size(16.dp), tint = MintGreen)
                        Spacer(Modifier.size(4.dp))
                        Text("BF Print", color = MintGreen, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onClearBatch) { Text("रद्द", color = SoftRed) }
                }
            }
        }

        SgsCard(title = "📋 निकाल (${state.results.size})") {
            if (state.results.isEmpty()) {
                Text("विद्यार्थी शोधण्यासाठी वर माहिती टाका किंवा '⚡ Load All' दाबा.", color = TextMuted, fontSize = 13.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.results.forEach { st ->
                        Surface(color = Navy2, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Gold.copy(0.25f))) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = st.regNo in state.selectedBatchRegNos, onCheckedChange = { onToggleBatch(st.regNo) })
                                    Column(modifier = Modifier.weight(1f).clickable { onOpenStudentIn(st, AppPage.PROFILE) }) {
                                        Text(st.fullName, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Reg: ${st.regNo} | ${MarathiUtils.classToMr(st.iyatta)} ${st.tukdi} (Roll ${st.rollNo})" +
                                                if (st.bestPhone.isNotBlank()) " | 📞 ${st.bestPhone}" else "",
                                            color = TextCream,
                                            fontSize = 12.sp
                                        )
                                    }
                                    MobileCallWaButtons(
                                        mobile = st.bestPhone,
                                        recipientLabel = st.fullName,
                                        student = st
                                    )
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    AssistChip(onClick = { onOpenStudentIn(st, AppPage.PROFILE) }, label = { Text("🪪 Profile") })
                                    if (AppPage.STUDENT in AppPage.allowedFor(role)) AssistChip(onClick = { onOpenStudentIn(st, AppPage.STUDENT) }, label = { Text("✏️ Edit") })
                                    if (AppPage.BONAFIDE in AppPage.allowedFor(role)) AssistChip(onClick = { onOpenStudentIn(st, AppPage.BONAFIDE) }, label = { Text("📄 BF") })
                                    if (AppPage.ATTENDANCE in AppPage.allowedFor(role)) AssistChip(onClick = { onOpenStudentIn(st, AppPage.ATTENDANCE) }, label = { Text("✅ AT") })
                                    if (AppPage.LC in AppPage.allowedFor(role)) AssistChip(onClick = { onOpenStudentIn(st, AppPage.LC) }, label = { Text("📋 LC") })
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
fun StudentProfileScreen(
    role: UserRole,
    state: SearchAndProfileUiState,
    verifyBaseUrl: String = "",
    acYear: String = "2026-27",
    onSearchProfile: (String) -> Unit,
    onSelectFeeYear: (String) -> Unit,
    onLoadAllYearsHistory: (String) -> Unit
) {
    val context = LocalContext.current
    val launchWa = LocalWhatsAppLauncher.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgsCard(title = "🪪 विद्यार्थी प्रोफाइल शोधा") {
            var regInput by remember(state.profileRegNo) { mutableStateOf(state.profileRegNo) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(regInput, { regInput = it }, "Reg No / ID", tag = "profile_reg_input", modifier = Modifier.weight(1f))
                Button(onClick = { onSearchProfile(regInput) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.height(54.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "पहा")
                }
            }
        }

        if (state.profileLoading) {
            CircularProgressIndicator(color = Gold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (!state.profileError.isNullOrBlank()) {
            Text(state.profileError, color = SoftRed)
        }

        state.profileStudent?.let { st ->
            SgsCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StudentPhotoBox(st.photoUrl, sizeDp = 96, fallbackBitmap = state.profilePhotoBitmap)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(st.fullName, color = GoldLight, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Text("रजि. क्र.: ${st.regNo} | ID: ${st.studentId}", color = TextCream, fontSize = 12.5.sp)
                        Text("इयत्ता: ${MarathiUtils.classToMr(st.iyatta)} ${st.tukdi} (Roll ${st.rollNo})", color = MintGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("जन्मतारीख: ${MarathiUtils.fmtDate(st.dob)}", color = TextMuted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { dialPhone(context, st.bestPhone) },
                        enabled = st.bestPhone.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.weight(1f).testTag("profile_call_btn")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Call", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            launchWa(
                                WhatsAppLaunchRequest(
                                    mobile = st.bestPhone,
                                    defaultMessage = "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी ${st.fullName} (${MarathiUtils.classToMr(st.iyatta)} ${st.tukdi}) यांच्या पालकांसाठी संदेश:",
                                    recipientLabel = st.fullName,
                                    student = st
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                        modifier = Modifier.weight(1f).testTag("profile_wa_btn")
                    ) {
                        Text(if (st.bestPhone.isNotBlank()) "💬 WhatsApp" else "💬 WA निवडा", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val card = PdfAndExportUtils.createProfileShareCard(context, st, state.profilePhotoBitmap)
                            PdfAndExportUtils.shareFile(context, card, "image/png", "Profile Card Share करा")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                        modifier = Modifier.weight(1f).testTag("profile_share_card_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Share Card", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val pdf = PdfAndExportUtils.renderStudentProfilePdf(context, st)
                            PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Profile PDF")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("PDF", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val pdf = PdfAndExportUtils.renderStudentIdCardPdf(
                                context = context,
                                students = listOf(st),
                                photoMap = mapOf(st.regNo to state.profilePhotoBitmap),
                                verifyBaseUrl = verifyBaseUrl,
                                acYear = st.acYear.ifBlank { acYear },
                                fileName = "ID_Card_${st.regNo.ifBlank { "student" }}.pdf"
                            )
                            PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Student ID Card PDF")
                        },
                        modifier = Modifier.weight(1f).testTag("profile_id_card_pdf_btn")
                    ) {
                        Text("🪪 ID Card PDF", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val pdf = PdfAndExportUtils.renderStudentIdCardPdf(
                                context = context,
                                students = listOf(st),
                                photoMap = mapOf(st.regNo to state.profilePhotoBitmap),
                                verifyBaseUrl = verifyBaseUrl,
                                acYear = st.acYear.ifBlank { acYear },
                                fileName = "ID_Card_${st.regNo.ifBlank { "student" }}.pdf"
                            )
                            PdfAndExportUtils.printPdfFile(context, pdf, "SGS ID Card - ${st.fullName}")
                        },
                        modifier = Modifier.testTag("profile_id_card_print_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "ID Card Print", modifier = Modifier.size(16.dp), tint = GoldLight)
                    }
                    OutlinedButton(
                        onClick = {
                            val pdf = PdfAndExportUtils.renderStudentProfilePdf(context, st)
                            PdfAndExportUtils.printPdfFile(context, pdf, "SGS Profile - ${st.fullName}")
                        },
                        modifier = Modifier.testTag("profile_pdf_print_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Profile Print", modifier = Modifier.size(16.dp), tint = MintGreen)
                    }
                }
                Spacer(Modifier.height(6.dp))
                val paidForSummary = state.profileFees.firstOrNull()?.feePaid
                    ?: (state.profileFeeOpening + state.profileFeeYearTotal - state.profileFeeBalance).coerceAtLeast(0.0)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val pdf = PdfAndExportUtils.renderParentSummaryPdf(
                                context = context,
                                s = st,
                                photoBitmap = state.profilePhotoBitmap,
                                acYear = state.selectedProfileFeeYear.ifBlank { st.acYear.ifBlank { acYear } },
                                absentDays = state.profileAbsentDays,
                                earlyLeaveCount = state.profileEarlyLeaveCount,
                                feeOpening = state.profileFeeOpening,
                                feeTotal = state.profileFeeYearTotal,
                                feePaid = paidForSummary,
                                feePending = state.profileFeeBalance,
                                transport = state.profileTransport,
                                diaryEntries = state.profileDiary,
                                verifyBaseUrl = verifyBaseUrl
                            )
                            PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "पालक सारांश अहवाल PDF")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                        modifier = Modifier.weight(1f).testTag("profile_parent_summary_pdf_btn")
                    ) {
                        Text("📄 पालक सारांश अहवाल PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val pdf = PdfAndExportUtils.renderParentSummaryPdf(
                                context = context,
                                s = st,
                                photoBitmap = state.profilePhotoBitmap,
                                acYear = state.selectedProfileFeeYear.ifBlank { st.acYear.ifBlank { acYear } },
                                absentDays = state.profileAbsentDays,
                                earlyLeaveCount = state.profileEarlyLeaveCount,
                                feeOpening = state.profileFeeOpening,
                                feeTotal = state.profileFeeYearTotal,
                                feePaid = paidForSummary,
                                feePending = state.profileFeeBalance,
                                transport = state.profileTransport,
                                diaryEntries = state.profileDiary,
                                verifyBaseUrl = verifyBaseUrl
                            )
                            PdfAndExportUtils.printPdfFile(context, pdf, "Parent Summary - ${st.fullName}")
                        },
                        modifier = Modifier.testTag("profile_parent_summary_print_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "पालक सारांश प्रिंट", modifier = Modifier.size(16.dp), tint = GoldLight)
                    }
                }
            }

            SgsCard(title = "📋 संपूर्ण माहिती") {
                listOf(
                    "आईचे नाव" to st.motherName,
                    "आधार क्र." to st.aadhar,
                    "PEN" to st.pen,
                    "धर्म / जात / प्रवर्ग" to "${st.religion} / ${st.caste} / ${st.category}",
                    "जन्मस्थळ" to st.birthVillage,
                    "WhatsApp / पर्यायी" to "${st.whatsappMobile} ${st.alternateMobile}".trim(),
                    "पत्ता" to st.address
                ).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(k, color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(0.4f))
                        Text(v.ifBlank { "—" }, color = TextCream, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f))
                    }
                }
                val distinctPhones = listOfNotNull(
                    st.whatsappMobile.takeIf { MarathiUtils.cleanDigits10(it).isNotEmpty() }?.let { "WhatsApp" to it },
                    st.alternateMobile.takeIf {
                        MarathiUtils.cleanDigits10(it).isNotEmpty() &&
                            MarathiUtils.cleanDigits10(it) != MarathiUtils.cleanDigits10(st.whatsappMobile)
                    }?.let { "पर्यायी" to it }
                )
                distinctPhones.forEach { (lbl, num) ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$lbl: $num", color = MintGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        MobileCallWaButtons(mobile = num, recipientLabel = st.fullName, student = st)
                    }
                }
            }

            if (role != UserRole.DEO && role != UserRole.CERT && role != UserRole.PEON) {
                SgsCard(title = "💰 फी व प्रवास") {
                    Text("प्रवास: ${state.profileTransport?.let { "${it.nativeVillage} (${it.travelMode}) • ${it.transportContact}" } ?: "नोंद नाही"}", color = TextCream, fontSize = 12.5.sp)
                    val trContact = state.profileTransport?.transportContact.orEmpty()
                    if (MarathiUtils.cleanDigits10(trContact).isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("प्रवास संपर्क: $trContact", color = GoldLight, fontSize = 12.sp)
                            MobileCallWaButtons(
                                mobile = trContact,
                                recipientLabel = "${st.fullName} प्रवास",
                                defaultMessage = "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी ${st.fullName} प्रवास संदर्भात संपर्क:",
                                student = st
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.profileFeeYears.isNotEmpty()) {
                        SgsDropdown(
                            label = "फी शैक्षणिक वर्ष",
                            selected = state.selectedProfileFeeYear,
                            options = state.profileFeeYears,
                            onSelect = onSelectFeeYear,
                            tag = "profile_fee_year_dropdown"
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        "मागील बाकी: ${MarathiUtils.fmtRupees(state.profileFeeOpening)} | वार्षिक फी: ${MarathiUtils.fmtRupees(state.profileFeeYearTotal)} | शिल्लक: ${MarathiUtils.fmtRupees(state.profileFeeBalance)}",
                        color = MintGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                    if (state.profileFeeAssumed) {
                        Text("ℹ️ अंदाजित फी (Master setting प्रमाणे)", color = TextMuted, fontSize = 11.sp)
                    }
                    if (state.profileFeeArchived) {
                        Text("📦 ही Archived वर्षाची फी माहिती आहे.", color = ColorTokens.WarningText, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    if (state.profileFees.isEmpty()) {
                        Text("फी नोंदी नाहीत.", color = TextMuted, fontSize = 12.sp)
                    } else {
                        state.profileFees.forEach { fr ->
                            Text("${MarathiUtils.fmtDate(fr.date)}: जमा ${MarathiUtils.fmtRupees(fr.feePaid)} | बाकी ${MarathiUtils.fmtRupees(fr.pendingFee)}", color = GoldLight, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val latestFee = state.profileFees.firstOrNull()
                    val paidSum = latestFee?.feePaid ?: (state.profileFeeOpening + state.profileFeeYearTotal - state.profileFeeBalance).coerceAtLeast(0.0)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val pdf = PdfAndExportUtils.renderFeeReceiptPdf(
                                    context = context,
                                    studentName = st.fullName,
                                    regNo = st.regNo,
                                    iyatta = st.iyatta,
                                    tukdi = st.tukdi,
                                    acYear = state.selectedProfileFeeYear.ifBlank { acYear },
                                    dateIso = latestFee?.date.orEmpty().ifBlank { MarathiUtils.todayIso() },
                                    openingDue = state.profileFeeOpening,
                                    totalFee = state.profileFeeYearTotal,
                                    feePaid = paidSum,
                                    pendingFee = state.profileFeeBalance,
                                    updatedBy = latestFee?.updatedBy.orEmpty(),
                                    verifyBaseUrl = verifyBaseUrl
                                )
                                PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Fee Receipt PDF")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier.weight(1f).testTag("profile_fee_receipt_pdf_btn")
                        ) { Text("🧾 पावती PDF", fontSize = 11.sp) }
                        OutlinedButton(
                            onClick = {
                                val pdf = PdfAndExportUtils.renderFeeReceiptPdf(
                                    context = context,
                                    studentName = st.fullName,
                                    regNo = st.regNo,
                                    iyatta = st.iyatta,
                                    tukdi = st.tukdi,
                                    acYear = state.selectedProfileFeeYear.ifBlank { acYear },
                                    dateIso = latestFee?.date.orEmpty().ifBlank { MarathiUtils.todayIso() },
                                    openingDue = state.profileFeeOpening,
                                    totalFee = state.profileFeeYearTotal,
                                    feePaid = paidSum,
                                    pendingFee = state.profileFeeBalance,
                                    updatedBy = latestFee?.updatedBy.orEmpty(),
                                    verifyBaseUrl = verifyBaseUrl
                                )
                                PdfAndExportUtils.printPdfFile(context, pdf, "Fee Receipt - ${st.regNo}")
                            }
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "पावती प्रिंट", modifier = Modifier.size(16.dp), tint = GoldLight)
                        }
                        Button(
                            onClick = {
                                val yr = state.selectedProfileFeeYear.ifBlank { acYear }
                                val msg = "नमस्कार, विद्यार्थी ${st.fullName} (इयत्ता ${MarathiUtils.classToMr(st.iyatta)} ${st.tukdi}, शै. वर्ष $yr) फी तपशील: एकूण ${MarathiUtils.fmtRupees(state.profileFeeYearTotal + state.profileFeeOpening)}, जमा ${MarathiUtils.fmtRupees(paidSum)}, शिल्लक बाकी ${MarathiUtils.fmtRupees(state.profileFeeBalance)}. — श्री. गो. से. हायस्कूल, पाचोरा"
                                launchWa(
                                    WhatsAppLaunchRequest(
                                        mobile = st.bestPhone,
                                        defaultMessage = msg,
                                        recipientLabel = st.fullName,
                                        student = st
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                            modifier = Modifier.weight(1f).testTag("profile_fee_wa_btn")
                        ) { Text("💬 WA फी", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            SgsCard(title = "📚 सर्व वर्षांचा इतिहास") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onLoadAllYearsHistory(st.regNo) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.weight(1f).testTag("profile_all_years_btn")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("इतिहास Load", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val pdf = PdfAndExportUtils.renderYearHistoryPdf(context, st, state.yearHistory)
                            PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Year History PDF")
                        },
                        enabled = state.yearHistory.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.weight(1f).testTag("profile_all_years_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("History PDF", fontSize = 12.sp)
                    }
                }
                if (state.yearHistoryLoading) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(color = Gold)
                }
                state.yearHistory.forEach { blk ->
                    Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("${if (blk.archived) "📦 " else ""}${blk.year} — इयत्ता ${MarathiUtils.classToMr(blk.iyatta)} ${blk.tukdi}", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("उपस्थिती: ${blk.pct ?: "—"}% (गैरहजर ${blk.absentDays} दिवस)", color = TextCream, fontSize = 12.sp)
                            Text("फी: एकूण ${MarathiUtils.fmtRupees(blk.feeTotal)} | जमा ${MarathiUtils.fmtRupees(blk.feePaid)} | बाकी ${MarathiUtils.fmtRupees(blk.feeBalance)}", color = MintGreen, fontSize = 12.sp)
                            Text("डायरी: ${blk.diaryCount} | घरी गेलेले: ${blk.elCount} | शिष्यवृत्ती: ${blk.schCount} | प्रवास: ${blk.transportSummary}", color = TextMuted, fontSize = 11.5.sp)
                        }
                    }
                }
            }

            if (role == UserRole.SUPER || role == UserRole.MASTER) {
                var auditTab by remember { mutableIntStateOf(0) }
                SgsCard(title = "🔍 ऑडिट व इतिहास (Audit)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val pdf = PdfAndExportUtils.renderAuditLogPdf(
                                    context = context,
                                    student = st,
                                    changes = state.auditLogs,
                                    actions = state.auditActionLogs,
                                    promotions = state.auditPromotions
                                )
                                PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "Audit Log PDF")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                            modifier = Modifier.weight(1f).testTag("profile_audit_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("📄 Audit Log PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val pdf = PdfAndExportUtils.renderAuditLogPdf(
                                    context = context,
                                    student = st,
                                    changes = state.auditLogs,
                                    actions = state.auditActionLogs,
                                    promotions = state.auditPromotions
                                )
                                PdfAndExportUtils.printPdfFile(context, pdf, "Audit Log - ${st.regNo}")
                            },
                            modifier = Modifier.testTag("profile_audit_print_btn")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Audit Log Print", modifier = Modifier.size(16.dp), tint = GoldLight)
                            Spacer(Modifier.size(4.dp))
                            Text("Print", color = GoldLight, fontSize = 11.5.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!state.auditHasSheet) {
                        Text("ℹ️ Audit Log शीट अद्याप तयार नाही किंवा रिकामा आहे.", color = ColorTokens.WarningText, fontSize = 12.sp)
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = auditTab == 0, onClick = { auditTab = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 3)) {
                            Text("बदल (${state.auditLogs.size})", fontSize = 11.sp)
                        }
                        SegmentedButton(selected = auditTab == 1, onClick = { auditTab = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 3)) {
                            Text("कृती (${state.auditActionLogs.size})", fontSize = 11.sp)
                        }
                        SegmentedButton(selected = auditTab == 2, onClick = { auditTab = 2 }, shape = SegmentedButtonDefaults.itemShape(2, 3)) {
                            Text("पदोन्नती (${state.auditPromotions.size})", fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    when (auditTab) {
                        0 -> if (state.auditLogs.isEmpty()) Text("बदलाची नोंद नाही.", color = TextMuted, fontSize = 12.sp)
                        else state.auditLogs.take(25).forEach { a ->
                            Text("${a.timestamp} • ${a.user}: ${a.field} (${a.oldVal} → ${a.newVal})", color = TextCream, fontSize = 11.5.sp)
                        }
                        1 -> if (state.auditActionLogs.isEmpty()) Text("कृती नोंद नाही.", color = TextMuted, fontSize = 12.sp)
                        else state.auditActionLogs.take(25).forEach { l ->
                            Text("${l.timestamp} • ${l.user} (${l.role}): ${l.action}", color = TextCream, fontSize = 11.5.sp)
                        }
                        else -> if (state.auditPromotions.isEmpty()) Text("पदोन्नती नोंद नाही.", color = TextMuted, fontSize = 12.sp)
                        else state.auditPromotions.take(25).forEach { p ->
                            Text("${p.timestamp} • ${p.from} → ${p.to} (By: ${p.by})${if (p.undone) " [रद्द]" else ""}", color = if (p.undone) SoftRed else MintGreen, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CertHistoryScreen(
    state: SearchAndProfileUiState,
    onSearchCerts: (String) -> Unit,
    onLoadAllCerts: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgsCard(title = "📁 प्रमाणपत्र इतिहास (LC / Bonafide / Attendance)") {
            var q by remember(state.certHistSearchQuery) { mutableStateOf(state.certHistSearchQuery) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(q, { q = it }, "Reg No टाका", modifier = Modifier.weight(1f), tag = "hist_reg_input")
                Button(onClick = { onSearchCerts(q) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.height(54.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "शोधा")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onLoadAllCerts, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.weight(1f).testTag("hist_load_all_btn")) {
                    Text("सर्व नोंदी Load")
                }
                Button(
                    onClick = {
                        val rows = state.allCerts.map { listOf(it.type, it.serial, MarathiUtils.fmtDate(it.date), it.regNo, it.fullName, it.detail) }
                        val csv = PdfAndExportUtils.writeCsvFile(context, "Certificates_Export.csv", listOf("प्रकार", "क्रमांक", "दिनांक", "Reg No", "नाव", "तपशील"), rows)
                        PdfAndExportUtils.shareFile(context, csv, "text/csv", "CSV Export Share")
                    },
                    enabled = state.allCerts.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    modifier = Modifier.weight(1f).testTag("hist_export_csv_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("CSV Export")
                }
            }
        }

        if (state.certHistLoading) CircularProgressIndicator(color = Gold)

        val list = if (state.certHistory.isNotEmpty()) state.certHistory else state.allCerts
        SgsCard(title = "📋 नोंदी (${list.size})") {
            if (list.isEmpty()) {
                Text("कोणतीही प्रमाणपत्र नोंद आढळली नाही.", color = TextMuted, fontSize = 13.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    list.take(80).forEach { c ->
                        Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${c.type} #${c.serial} — ${c.fullName} (Reg: ${c.regNo})", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${MarathiUtils.fmtDate(c.date)} • ${c.detail}", color = TextCream, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
