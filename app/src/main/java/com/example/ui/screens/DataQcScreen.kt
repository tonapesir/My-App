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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppPage
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
import com.example.ui.components.BannerView
import com.example.ui.components.SgsCard
import com.example.ui.components.SgsTextField
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
import com.example.ui.viewmodel.AdminAndQcUiState
import com.example.ui.viewmodel.ReportsAndAdminDelegate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DataQcScreen(
    state: AdminAndQcUiState,
    admin: ReportsAndAdminDelegate,
    onNavigate: (AppPage) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val context = LocalContext.current
    var skipStepNum by remember { mutableIntStateOf(-1) }
    var skipReason by remember { mutableStateOf("") }
    var usageDaysInput by remember(state.usageDays) { mutableStateOf(state.usageDays.toString()) }
    var usageTeachersInput by remember(state.usageTeachers) { mutableStateOf(state.usageTeachers.toString()) }

    val tabs = listOf(
        "q" to "🔎 गुणवत्ता",
        "y" to "📋 वर्ष-अखेर",
        "g" to "🎓 मार्गदर्शक (९)",
        "c" to "📊 क्षमता",
        "e" to "🐞 त्रुटी",
        "u" to "📈 वापर"
    )
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == state.qcTab }.coerceAtLeast(0),
            containerColor = Navy,
            contentColor = GoldLight
        ) {
            tabs.forEach { (k, lbl) ->
                Tab(
                    selected = state.qcTab == k,
                    onClick = { admin.loadDataQcTab(k) },
                    text = { Text(lbl, fontSize = 12.5.sp) },
                    modifier = Modifier.testTag("qc_tab_$k")
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.qcLoading) CircularProgressIndicator(color = Gold)

            when (state.qcTab) {
                "q" -> {
                    SgsCard(title = "🔎 डेटा गुणवत्ता तपासणी") {
                        Text(
                            "एकूण विद्यार्थी: ${state.qcTotalStudents} | त्रुटी असलेले: ${state.qcStudentsWithIssues} | वय-मर्यादा फरक: ±${state.qcAgeTol} वर्षे",
                            color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.qcSummaryCounts.forEach { (type, cnt) ->
                                FilterChip(
                                    selected = state.qcDetailType == type,
                                    onClick = { admin.loadQcDetail(type) },
                                    label = { Text("$type: $cnt", fontSize = 11.5.sp) }
                                )
                            }
                        }
                        if (state.qcClassSummaryRows.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("वर्गनिहाय त्रुटी सारांश:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            state.qcClassSummaryRows.forEach { r ->
                                Text(
                                    "${r.getOrNull(0)}: एकूण ${r.getOrNull(1)}, त्रुटी ${r.getOrNull(2)} (दुबार:${r.getOrNull(3)} DOB:${r.getOrNull(4)} WA:${r.getOrNull(5)} फोटो:${r.getOrNull(6)})",
                                    color = TextCream, fontSize = 11.5.sp, modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                        if (state.qcDetailRows.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("तपशील (${state.qcDetailType}: ${state.qcDetailTotal})", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                OutlinedButton(onClick = {
                                    val csv = PdfAndExportUtils.writeCsvFile(
                                        context, "QC_${state.qcDetailType}.csv",
                                        listOf("Reg No", "नाव", "वर्ग", "तपशील"), state.qcDetailRows
                                    )
                                    PdfAndExportUtils.shareFile(context, csv, "text/csv", "QC CSV")
                                }) { Text("CSV", fontSize = 11.sp, color = MintGreen) }
                            }
                            state.qcDetailRows.forEach { r ->
                                val regNo = r.getOrNull(0).orEmpty()
                                Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${r.getOrNull(0)} | ${r.getOrNull(1)} (${r.getOrNull(2)}) — ${r.getOrNull(3)}", color = TextCream, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                        if (regNo.isNotBlank()) {
                                            TextButton(onClick = { onOpenProfile(regNo) }) { Text("🪪", fontSize = 14.sp) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "y" -> {
                    SgsCard(title = "📋 वर्ष-अखेर पूर्वतपासणी") {
                        if (state.yearEndSummaryHeader.isNotBlank()) {
                            Text(state.yearEndSummaryHeader, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Spacer(Modifier.height(6.dp))
                        }
                        state.yearEndWarnings.forEach { w ->
                            Text("⚠️ $w", color = SoftRed, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        state.yearEndChecks.forEach { (lbl, status) ->
                            Text("• $lbl: $status", color = MintGreen, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 3.dp))
                        }
                        if (state.yearEndClassRows.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("वर्गनिहाय स्थिती:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            state.yearEndClassRows.forEach { r ->
                                Text(r.joinToString(" | "), color = TextCream, fontSize = 11.5.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                    if (state.yearEndDuesRows.isNotEmpty()) {
                        SgsCard(title = "💰 थकबाकी असलेले विद्यार्थी (${state.yearEndDuesRows.size})") {
                            state.yearEndDuesRows.take(30).forEach { r ->
                                Text("${r.getOrNull(0)} | ${r.getOrNull(1)} (${r.getOrNull(2)}) — ₹${r.getOrNull(3)}", color = TextCream, fontSize = 11.5.sp)
                            }
                        }
                    }
                    if (state.yearEndLcRows.isNotEmpty()) {
                        SgsCard(title = "📋 शेवटचा वर्ग LC स्थिती (${state.yearEndLcRows.size})") {
                            state.yearEndLcRows.take(30).forEach { r ->
                                Text("${r.getOrNull(0)} | ${r.getOrNull(1)} (${r.getOrNull(2)}) — ${r.getOrNull(4)}", color = TextCream, fontSize = 11.5.sp)
                            }
                        }
                    }
                }

                "g" -> {
                    SgsCard(title = "🎓 वर्ष-अखेर ९-पायऱ्या मार्गदर्शक (${state.wizardClosingYear} → ${state.wizardNewYear})") {
                        BannerView(state.wizardStatus)
                        if (state.wizardComplete) {
                            Text("🎉 वर्ष-अखेर प्रक्रिया पूर्ण झाली आहे!", color = MintGreen, fontWeight = FontWeight.Bold)
                        }
                        if (state.wizardPromoPreview.isNotBlank()) {
                            Text("पदोन्नती आराखडा (${state.wizardPromoTotal} विद्यार्थी): ${state.wizardPromoPreview}", color = TextMuted, fontSize = 11.5.sp)
                            Spacer(Modifier.height(6.dp))
                        }
                        state.wizardSteps.forEach { st ->
                            val statusColor = when (st.status) {
                                "done", "auto" -> MintGreen
                                "active" -> GoldLight
                                "skipped" -> ColorTokens.WarningText
                                else -> TextMuted
                            }
                            Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("पायरी ${st.n}: ${st.title} [${st.status.uppercase()}]", color = statusColor, fontWeight = FontWeight.Bold)
                                    Text(st.what, color = TextCream, fontSize = 12.sp)
                                    if (st.infoSummary.isNotBlank()) {
                                        Text(st.infoSummary, color = GoldLight, fontSize = 11.5.sp)
                                    }
                                    if (st.reason.isNotBlank()) {
                                        Text("कारण: ${st.reason} (${st.by})", color = TextMuted, fontSize = 11.sp)
                                    }
                                    if (st.status == "active" || st.status == "locked") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (st.n == 6 && st.status == "active") {
                                                Button(onClick = admin::runWizardPromotion, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy)) {
                                                    Text("🚀 बढती चालवा", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (st.status == "active") {
                                                Button(onClick = { admin.setWizardStepStatus(st.n, "done") }, colors = ButtonDefaults.buttonColors(containerColor = Emerald)) {
                                                    Text("✅ पूर्ण", fontSize = 11.sp)
                                                }
                                                OutlinedButton(onClick = { skipStepNum = st.n; skipReason = "" }) {
                                                    Text("⏭️ वगळा", color = TextCream, fontSize = 11.sp)
                                                }
                                            }
                                            if (st.n in listOf(4, 5, 6, 7, 8)) {
                                                OutlinedButton(onClick = { onNavigate(if (st.n == 4) AppPage.LC else AppPage.SUPERADMIN) }) {
                                                    Text("उघडा", color = GoldLight, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (state.wizardVerifyRows.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "अंतिम पडताळणी: ${state.wizardVerifySummary} ${if (state.wizardVerifyAllOk) "✅ सर्व ठीक" else "⚠️ तपासा"}",
                                color = if (state.wizardVerifyAllOk) MintGreen else SoftRed, fontWeight = FontWeight.Bold, fontSize = 12.sp
                            )
                            if (state.wizardVerifyPassedOut.isNotBlank()) {
                                Text(state.wizardVerifyPassedOut, color = TextCream, fontSize = 11.5.sp)
                            }
                            state.wizardVerifyRows.forEach { r ->
                                Text("${r.getOrNull(0)}: आधी ${r.getOrNull(1)} | अपेक्षित ${r.getOrNull(2)} | वास्तविक ${r.getOrNull(3)} ${r.getOrNull(4)}", color = TextCream, fontSize = 11.5.sp)
                            }
                        }
                    }
                }

                "c" -> {
                    SgsCard(title = "📊 Sheet क्षमता") {
                        Text(state.sheetCapSummary, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (state.sheetCapPct / 100.0).toFloat().coerceIn(0f, 1f) },
                            color = if (state.sheetCapLevel == "crit") SoftRed else Gold,
                            trackColor = Navy2,
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("प्रमुख Sheets:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        state.sheetCapRows.forEach { r ->
                            Text("${r.getOrNull(0)}: ${r.getOrNull(1)} cells", color = TextCream, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        if (state.sheetCapArchivableRows.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Archive करण्यायोग्य वर्षे:", color = MintGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            state.sheetCapArchivableRows.forEach { r ->
                                Text("${r.getOrNull(0)}: ${r.getOrNull(1)} cells", color = TextMuted, fontSize = 11.5.sp)
                            }
                        }
                    }
                }

                "e" -> {
                    SgsCard(title = "🐞 Client त्रुटी (शेवटचे ९० दिवस)") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(state.clientErrSummary.ifBlank { "त्रुटी यादी" }, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Button(
                                onClick = {
                                    val csv = PdfAndExportUtils.writeCsvFile(
                                        context, "Client_Errors_${MarathiUtils.todayIso()}.csv",
                                        listOf("प्रकार", "संदेश", "संख्या", "Users", "शेवटची वेळ", "आवृत्ती", "पान", "Stack"),
                                        state.clientErrorCsvRows
                                    )
                                    PdfAndExportUtils.shareFile(context, csv, "text/csv", "Client Errors CSV")
                                },
                                enabled = state.clientErrorCsvRows.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                                modifier = Modifier.testTag("qc_errors_csv_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("CSV", fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (state.clientErrorRows.isEmpty()) Text("✅ कोणतीही त्रुटी नाही.", color = MintGreen)
                        else state.clientErrorRows.forEach { r -> Text(r.joinToString(" | "), color = SoftRed, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                    }
                }

                "u" -> {
                    SgsCard(title = "📈 वापर व ताण-अंदाज") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SgsTextField(usageDaysInput, { usageDaysInput = it }, "दिवस", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                            SgsTextField(usageTeachersInput, { usageTeachersInput = it }, "शिक्षक संख्या", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    admin.loadDataQcTab("u", usageDaysInput.toIntOrNull() ?: 7, usageTeachersInput.toIntOrNull() ?: 100)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                                modifier = Modifier.height(54.dp)
                            ) { Text("पहा", fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(state.usageSummaryText, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        if (state.usageScenarioRows.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("ताण-अंदाज (Stress Scenarios):", color = MintGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            state.usageScenarioRows.forEach { r ->
                                Text("• विंडो ${r.getOrNull(0)}: वापर ${r.getOrNull(1)}, कमाल प्रतीक्षा ${r.getOrNull(2)} [${r.getOrNull(3)}]", color = TextCream, fontSize = 11.5.sp)
                            }
                        }
                        if (state.usageTips.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            state.usageTips.forEach { tip -> Text("💡 $tip", color = TextMuted, fontSize = 11.5.sp) }
                        }
                        Spacer(Modifier.height(6.dp))
                        state.usageRows.forEach { r -> Text(r.joinToString(" | "), color = TextCream, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                    }
                }
            }
        }
    }

    if (skipStepNum > 0) {
        AlertDialog(
            onDismissRequest = { skipStepNum = -1 },
            containerColor = Navy,
            titleContentColor = GoldLight,
            textContentColor = TextCream,
            title = { Text("⏭️ पायरी $skipStepNum वगळा", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("वगळण्याचे कारण लिहा (किमान ५ अक्षरे):")
                    SgsTextField(skipReason, { skipReason = it }, "कारण")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = skipStepNum
                        val r = skipReason.trim()
                        skipStepNum = -1
                        admin.setWizardStepStatus(s, "skipped", r)
                    },
                    enabled = skipReason.trim().length >= 5,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) { Text("वगळा") }
            },
            dismissButton = {
                TextButton(onClick = { skipStepNum = -1 }) { Text("रद्द", color = SoftRed) }
            }
        )
    }
}
