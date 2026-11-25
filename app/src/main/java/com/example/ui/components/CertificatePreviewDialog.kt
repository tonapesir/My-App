package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Emerald
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.Navy
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextCream
import com.example.ui.viewmodel.CertPreviewPayload

@Composable
fun CertificatePreviewDialog(
    payload: CertPreviewPayload,
    verifyBaseUrl: String,
    twoUp: Boolean,
    onToggleTwoUp: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    fun generatePdfFile() = when (payload) {
        is CertPreviewPayload.LcPreview -> PdfAndExportUtils.renderLcPdf(context, payload.data)
        is CertPreviewPayload.BonafidePreview -> PdfAndExportUtils.renderBonafidePdf(context, payload.data, verifyBaseUrl, twoUp)
        is CertPreviewPayload.AttendancePreview -> PdfAndExportUtils.renderAttendanceCertPdf(context, payload.data, verifyBaseUrl, twoUp)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Navy
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Actions
                Row(
                    modifier = Modifier.fillMaxWidth().background(DeepNavy).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (payload) {
                            is CertPreviewPayload.LcPreview -> "📋 शाळा सोडल्याचा दाखला"
                            is CertPreviewPayload.BonafidePreview -> "📄 बोनाफाईड सर्टिफिकेट"
                            is CertPreviewPayload.AttendancePreview -> "✅ उपस्थिती दाखला"
                        },
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करा", tint = TextCream)
                    }
                }

                // Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val f = generatePdfFile()
                            PdfAndExportUtils.printPdfFile(context, f, f.nameWithoutExtension)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.testTag("cert_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Print", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val f = generatePdfFile()
                            PdfAndExportUtils.shareFile(context, f, "application/pdf", "PDF Share करा")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.testTag("cert_pdf_share_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("PDF", fontSize = 12.sp)
                    }
                    if (payload !is CertPreviewPayload.LcPreview) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                            Text("A4 वर २ प्रती", color = TextCream, fontSize = 12.sp)
                            Switch(checked = twoUp, onCheckedChange = onToggleTwoUp)
                        }
                    }
                }

                // Visual Certificate Sheet
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CertificatePaperCard(payload = payload, verifyBaseUrl = verifyBaseUrl)
                }
            }
        }
    }
}

@Composable
private fun CertificatePaperCard(payload: CertPreviewPayload, verifyBaseUrl: String) {
    val isSaved = when (payload) {
        is CertPreviewPayload.LcPreview -> payload.data.lcNo.isNotBlank()
        is CertPreviewPayload.BonafidePreview -> payload.data.certNo.isNotBlank()
        is CertPreviewPayload.AttendancePreview -> payload.data.certNo.isNotBlank()
    }
    val qrUrl = remember(payload, verifyBaseUrl) {
        when (payload) {
            is CertPreviewPayload.BonafidePreview -> if (payload.data.certNo.isNotBlank()) {
                "$verifyBaseUrl?action=verify&type=BF&serial=${java.net.URLEncoder.encode(payload.data.certNo, "UTF-8")}"
            } else ""
            is CertPreviewPayload.AttendancePreview -> if (payload.data.certNo.isNotBlank()) {
                "$verifyBaseUrl?action=verify&type=AT&serial=${java.net.URLEncoder.encode(payload.data.certNo, "UTF-8")}"
            } else ""
            else -> ""
        }
    }
    val qrBmp = remember(qrUrl) { if (qrUrl.isNotBlank()) PdfAndExportUtils.generateQrBitmap(qrUrl, 160) else null }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(4.dp),
        border = if (payload is CertPreviewPayload.LcPreview) BorderStroke(2.dp, Navy) else BorderStroke(1.dp, Color.LightGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            if (!isSaved && payload !is CertPreviewPayload.LcPreview) {
                Text(
                    "DRAFT",
                    color = Color(0x22B40000),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Center).rotate(-25f)
                )
            }
            if (qrBmp != null) {
                Image(
                    bitmap = qrBmp.asImageBitmap(),
                    contentDescription = "QR Verification",
                    modifier = Modifier.size(64.dp).align(Alignment.TopEnd).border(0.5.dp, Color.Gray)
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val isEn = when (payload) {
                    is CertPreviewPayload.BonafidePreview -> payload.data.lang == "en"
                    is CertPreviewPayload.AttendancePreview -> payload.data.lang == "en"
                    else -> false
                }
                Text(
                    if (isEn) "Pachora Taluka Sahakari Shikshan Sanstha, Run by" else "पाचोरा तालुका सहकारी शिक्षण संस्था संचलित,",
                    color = Color.DarkGray, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (isEn) "SHRI GOVINDRAM SEKSARIA HIGH SCHOOL, PACHORA" else "श्री.गोविंदराम सेक्सरिया हायस्कूल, पाचोरा",
                    color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Text("UDISE - 27031201718", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                HorizontalDivider(color = Color.Black)

                when (payload) {
                    is CertPreviewPayload.BonafidePreview -> {
                        val d = payload.data
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${if (isEn) "Reg. No." else "रजि. क्र."} ${d.regNo}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${if (isEn) "Bonafide No." else "बोना. क्र."} ${d.certNo.ifBlank { "DRAFT" }}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            if (isEn) "BONAFIDE CERTIFICATE" else "बोनाफाईड सर्टिफिकेट",
                            color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                        val iyMr = MarathiUtils.classToMr(d.iyatta)
                        val isGirl = d.gender.equals("Female", true)
                        val txt = if (isEn) {
                            "This is to certify that ${if (isGirl) "Miss" else "Mr."} ${d.fullName} is a bonafide student studying in Std. $iyMr, Div ${d.tukdi.ifBlank { "_" }} (${d.acYear}). DOB: ${MarathiUtils.fmtDate(d.dob)} (${d.dobWords}). Caste: ${d.religion} ${d.caste}. Birthplace: ${d.birthPlace}."
                        } else {
                            "${if (isGirl) "कुमारी" else "कुमार"} ${d.fullName} हे आमच्या शाळेत शैक्षणिक वर्ष ${d.acYear} मध्ये इयत्ता $iyMr तुकडी ${d.tukdi.ifBlank { "_" }} मध्ये शिकत आहेत. दफ्तरी नोंदीप्रमाणे जन्म तारीख ${MarathiUtils.fmtDate(d.dob)} (${d.dobWords}), जात ${d.religion} ${d.caste}, जन्मस्थळ ${d.birthPlace}."
                        }
                        Text(txt, color = Color.Black, fontSize = 12.sp, lineHeight = 19.sp)
                        if (d.purpose.isNotBlank()) Text("${if (isEn) "Purpose" else "उद्देश"}: ${d.purpose}", color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    is CertPreviewPayload.AttendancePreview -> {
                        val d = payload.data
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${if (isEn) "Reg. No." else "रजि. क्र."} ${d.regNo}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${if (isEn) "Att. No." else "उप. क्र."} ${d.certNo.ifBlank { "DRAFT" }}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            if (isEn) "ATTENDANCE CERTIFICATE" else "उपस्थिती दाखला",
                            color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                        val iyMr = MarathiUtils.classToMr(d.iyatta)
                        val txt = if (isEn) {
                            "This is to certify that ${d.fullName}, Std. $iyMr ${d.tukdi} (${d.acYear}), was present for ${d.presentDays} out of ${d.totalDays} working days (${d.pct}) from ${MarathiUtils.fmtDate(d.fromDate)} to ${MarathiUtils.fmtDate(d.toDate)}."
                        } else {
                            "${d.fullName} (इयत्ता $iyMr ${d.tukdi}, ${d.acYear}) यांची ${MarathiUtils.fmtDate(d.fromDate)} ते ${MarathiUtils.fmtDate(d.toDate)} कालावधीत एकूण ${d.totalDays} दिवसांपैकी ${d.presentDays} दिवस (${d.pct}) उपस्थिती होती."
                        }
                        Text(txt, color = Color.Black, fontSize = 12.sp, lineHeight = 19.sp)
                    }
                    is CertPreviewPayload.LcPreview -> {
                        val d = payload.data
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("लि.स.क्र.: ${d.lcNo.ifBlank { "DRAFT" }}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("जनरल रजि. क्र.: ${d.regNo}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("शाळा सोडल्याचा दाखला (${if ((d.lcCount.toIntOrNull() ?: 1) > 1) "दुय्यमप्रत" else "मुळप्रत"})", color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        listOf(
                            "संपूर्ण नाव" to d.fullName,
                            "आईचे नाव" to d.motherName,
                            "आधार / Student ID / PEN" to "${d.aadhar} | ${d.studentId} | ${d.pen}",
                            "धर्म / जात" to "${d.religion} ${d.caste} ${d.subcaste}",
                            "जन्म दिनांक" to "${MarathiUtils.fmtDate(d.dob)} (${d.dobWords})",
                            "पूर्वीची शाळा" to d.prevSchool,
                            "प्रवेश दिनांक व वर्ग" to "${MarathiUtils.fmtDate(d.admissionDate)} (${MarathiUtils.classToMr(d.admissionClass)})",
                            "सोडल्याची दिनांक व इयत्ता" to "${MarathiUtils.fmtDate(d.leaveDate)} (${MarathiUtils.classToMr(d.leaveClass)})",
                            "प्रगती / वर्तणूक" to "${d.progress} / ${d.conduct}",
                            "कारण" to d.remarks,
                            "माध्यम / पुढील शाळा" to listOf(d.medium, d.nextSchool).filter { it.isNotBlank() }.joinToString(" | "),
                            "प्रमाणपत्र / इतर" to listOf(d.certReceived, d.stxt76, d.feeStatus).filter { it.isNotBlank() }.joinToString(" | ")
                        ).filter { it.second.isNotBlank() || it.first == "कारण" }.forEach { (k, v) ->
                            Row(Modifier.fillMaxWidth()) {
                                Text("$k:", color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.42f))
                                Text(v.ifBlank { "—" }, color = Color.Black, fontSize = 11.sp, modifier = Modifier.weight(0.58f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isEn) "Place – Pachora\nDate – ${MarathiUtils.todayDisplay()}" else "स्थळ – पाचोरा\nदिनांक – ${MarathiUtils.todayDisplay()}", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(if (isEn) "Headmaster / Principal" else "मुख्याध्यापक / स्वाक्षरी", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
