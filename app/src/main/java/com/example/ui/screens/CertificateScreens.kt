package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.AttendanceCertFormData
import com.example.domain.model.BonafideFormData
import com.example.domain.model.LcFormData
import com.example.domain.util.MarathiUtils
import com.example.ui.components.BannerView
import com.example.ui.components.SgsCard
import com.example.ui.components.SgsDateField
import com.example.ui.components.SgsDropdown
import com.example.ui.components.SgsTextField
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Emerald
import com.example.ui.theme.Gold
import com.example.ui.theme.RoyalBlue
import com.example.ui.viewmodel.CertFormsUiState
import com.example.ui.viewmodel.CertPreviewPayload

@Composable
fun LcScreen(
    state: CertFormsUiState,
    canEdit: Boolean,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onUpdateLc: ((LcFormData) -> LcFormData) -> Unit,
    onSaveLc: () -> Unit,
    onPreview: (CertPreviewPayload) -> Unit
) {
    val d = state.lc
    var showExtraLcFields by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgsCard(title = "🔍 विद्यार्थी निवडा (LC साठी)") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(state.lcSearchQuery, onSearchChange, "Reg No / ID", modifier = Modifier.weight(1f), tag = "lc_search_input")
                Button(onClick = onSearch, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.height(54.dp).testTag("lc_search_btn")) {
                    Icon(Icons.Default.Search, contentDescription = "शोधा")
                }
            }
        }

        SgsCard(title = "📋 शाळा सोडल्याचा दाखला तपशील") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.regNo, { v -> onUpdateLc { it.copy(regNo = v) } }, "जनरल रजि. क्र.", modifier = Modifier.weight(1f))
                    SgsTextField(d.lcNo, { v -> onUpdateLc { it.copy(lcNo = v) } }, "लि.स.क्र. (आपोआप)", readOnly = true, modifier = Modifier.weight(1f), tag = "lc_serial_no")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.studentId, { v -> onUpdateLc { it.copy(studentId = v) } }, "Student ID", modifier = Modifier.weight(1f))
                    SgsTextField(d.bookNo, { v -> onUpdateLc { it.copy(bookNo = v) } }, "बुक क्र.", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.aadhar, { v -> onUpdateLc { it.copy(aadhar = v) } }, "आधार क्र.", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    SgsTextField(d.pen, { v -> onUpdateLc { it.copy(pen = v) } }, "PEN नंबर", modifier = Modifier.weight(1f))
                }
                SgsTextField(d.fullName, { v -> onUpdateLc { it.copy(fullName = v) } }, "संपूर्ण नाव*", tag = "lc_full_name")
                SgsTextField(d.motherName, { v -> onUpdateLc { it.copy(motherName = v) } }, "आईचे नाव")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("राष्ट्रीयत्व", d.nationality, listOf("भारतीय", "Indian"), { v -> onUpdateLc { it.copy(nationality = v) } }, modifier = Modifier.weight(1f))
                    SgsDropdown("प्रकार (जन्माने)", d.nationalityType, listOf("जन्माने", "अधिवासाने"), { v -> onUpdateLc { it.copy(nationalityType = v) } }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("धर्म", d.religion, MarathiUtils.RELIGIONS, { v -> onUpdateLc { it.copy(religion = v) } }, modifier = Modifier.weight(1f))
                    SgsTextField(d.caste, { v -> onUpdateLc { it.copy(caste = v) } }, "जात", modifier = Modifier.weight(1f))
                    SgsTextField(d.subcaste, { v -> onUpdateLc { it.copy(subcaste = v) } }, "पोटजात", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.birthPlace, { v -> onUpdateLc { it.copy(birthPlace = v) } }, "जन्मस्थळ", modifier = Modifier.weight(1f))
                    SgsTextField(d.prevSchool, { v -> onUpdateLc { it.copy(prevSchool = v) } }, "पूर्वीची शाळा", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDateField("जन्मतारीख", d.dob, { v -> onUpdateLc { it.copy(dob = v) } }, modifier = Modifier.weight(1f))
                    SgsDateField("दाखला दिनांक", d.lcDate, { v -> onUpdateLc { it.copy(lcDate = v) } }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDateField("प्रवेश दिनांक", d.admissionDate, { v -> onUpdateLc { it.copy(admissionDate = v) } }, modifier = Modifier.weight(1f))
                    SgsDropdown("प्रवेश इयत्ता", d.admissionClass, MarathiUtils.ADMISSION_CLASSES, { v -> onUpdateLc { it.copy(admissionClass = v) } }, modifier = Modifier.weight(1f), displayTransform = { MarathiUtils.classToMr(it) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.classStartYear, { v -> onUpdateLc { it.copy(classStartYear = v) } }, "वर्ग प्रारंभ वर्ष", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    SgsTextField(d.classStartMonth, { v -> onUpdateLc { it.copy(classStartMonth = v) } }, "महिना (01-12)", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("सोडताना इयत्ता", d.leaveClass, MarathiUtils.CLASSES, { v -> onUpdateLc { it.copy(leaveClass = v) } }, modifier = Modifier.weight(1f), displayTransform = { MarathiUtils.classToMr(it) })
                    SgsDateField("शाळा सोडल्याची दिनांक", d.leaveDate, { v -> onUpdateLc { it.copy(leaveDate = v) } }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.progress, { v -> onUpdateLc { it.copy(progress = v) } }, "अभ्यासातील प्रगती", modifier = Modifier.weight(1f))
                    SgsTextField(d.conduct, { v -> onUpdateLc { it.copy(conduct = v) } }, "वर्तणूक", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("माध्यम", d.medium, listOf("मराठी", "सेमी इंग्रजी", "इंग्रजी"), { v -> onUpdateLc { it.copy(medium = v) } }, modifier = Modifier.weight(1f))
                    SgsTextField(d.feeStatus, { v -> onUpdateLc { it.copy(feeStatus = v) } }, "फी स्थिती", modifier = Modifier.weight(1f))
                }
                SgsTextField(d.remarks, { v -> onUpdateLc { it.copy(remarks = v) } }, "शाळा सोडल्याचे कारण", tag = "lc_reason", enableVoice = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.nextSchool, { v -> onUpdateLc { it.copy(nextSchool = v) } }, "पुढील शाळा", modifier = Modifier.weight(1f), enableVoice = true)
                    SgsDropdown("प्रत प्रकार", d.lcCount, listOf("1", "2"), { v -> onUpdateLc { it.copy(lcCount = v) } }, modifier = Modifier.weight(1f), displayTransform = { if (it == "2") "दुय्यमप्रत" else "मुळप्रत" })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.certReceived, { v -> onUpdateLc { it.copy(certReceived = v) } }, "प्रमाणपत्र प्राप्त (stxt75)", modifier = Modifier.weight(1f))
                    SgsTextField(d.stxt76, { v -> onUpdateLc { it.copy(stxt76 = v) } }, "इतर नोंद (stxt76)", modifier = Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = { showExtraLcFields = !showExtraLcFields },
                    modifier = Modifier.fillMaxWidth().testTag("lc_toggle_extra_btn")
                ) {
                    Text(if (showExtraLcFields) "▲ इतर विषय / Stxt60..70 लपवा" else "▼ इतर विषय / Stxt60..70 पहा", color = Gold)
                }

                if (showExtraLcFields) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SgsTextField(d.stxt60, { v -> onUpdateLc { it.copy(stxt60 = v) } }, "Stxt60", modifier = Modifier.weight(1f))
                        SgsTextField(d.stxt61, { v -> onUpdateLc { it.copy(stxt61 = v) } }, "Stxt61", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SgsTextField(d.stxt62, { v -> onUpdateLc { it.copy(stxt62 = v) } }, "Stxt62", modifier = Modifier.weight(1f))
                        SgsTextField(d.stxt63, { v -> onUpdateLc { it.copy(stxt63 = v) } }, "Stxt63", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SgsTextField(d.stxt67, { v -> onUpdateLc { it.copy(stxt67 = v) } }, "Stxt67", modifier = Modifier.weight(1f))
                        SgsDateField("Stxt68 (दिनांक)", d.stxt68, { v -> onUpdateLc { it.copy(stxt68 = v) } }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SgsTextField(d.stxt69, { v -> onUpdateLc { it.copy(stxt69 = v) } }, "Stxt69", modifier = Modifier.weight(1f))
                        SgsTextField(d.stxt70, { v -> onUpdateLc { it.copy(stxt70 = v) } }, "Stxt70", modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        BannerView(state.lcStatus)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (canEdit) {
                Button(onClick = onSaveLc, colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.weight(1f).height(50.dp).testTag("lc_save_btn")) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Save LC", fontWeight = FontWeight.Bold)
                }
            }
            Button(onClick = { onPreview(CertPreviewPayload.LcPreview(d)) }, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.weight(1f).height(50.dp).testTag("lc_preview_btn")) {
                Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("Preview / PDF", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BonafideScreen(
    state: CertFormsUiState,
    canSave: Boolean,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onUpdateBf: ((BonafideFormData) -> BonafideFormData) -> Unit,
    onSaveBf: () -> Unit,
    onPreview: (CertPreviewPayload) -> Unit
) {
    val d = state.bf
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgsCard(title = "🔍 विद्यार्थी शोधा (बोनाफाईड)") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(state.bfSearchQuery, onSearchChange, "Reg No / ID", modifier = Modifier.weight(1f), tag = "bf_search_input")
                Button(onClick = onSearch, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.height(54.dp).testTag("bf_search_btn")) {
                    Icon(Icons.Default.Search, contentDescription = "शोधा")
                }
            }
        }

        SgsCard(title = "📄 बोनाफाईड तपशील") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = d.lang == "mr", onClick = { onUpdateBf { it.copy(lang = "mr") } }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                        Text("मराठी")
                    }
                    SegmentedButton(selected = d.lang == "en", onClick = { onUpdateBf { it.copy(lang = "en") } }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                        Text("English")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.regNo, { v -> onUpdateBf { it.copy(regNo = v) } }, "रजि. क्र.", modifier = Modifier.weight(1f))
                    SgsTextField(d.certNo, {}, "बोना. क्र.", readOnly = true, modifier = Modifier.weight(1f))
                }
                SgsTextField(d.fullName, { v -> onUpdateBf { it.copy(fullName = v) } }, "विद्यार्थ्याचे नाव*", tag = "bf_full_name")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("इयत्ता", d.iyatta, MarathiUtils.CLASSES, { v -> onUpdateBf { it.copy(iyatta = v) } }, modifier = Modifier.weight(1f), displayTransform = { MarathiUtils.classToMr(it) })
                    SgsDropdown("तुकडी", d.tukdi, MarathiUtils.DIVISIONS, { v -> onUpdateBf { it.copy(tukdi = v) } }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.acYear, { v -> onUpdateBf { it.copy(acYear = v) } }, "शैक्षणिक वर्ष", modifier = Modifier.weight(1f))
                    SgsDateField("तारीख", d.bfDate, { v -> onUpdateBf { it.copy(bfDate = v) } }, modifier = Modifier.weight(1f))
                }
                SgsDropdown("उद्देश (Purpose)", d.purpose, MarathiUtils.BF_PURPOSES, { v -> onUpdateBf { it.copy(purpose = if (v == "इतर") "" else v) } }, tag = "bf_purpose_sel")
                SgsTextField(d.purpose, { v -> onUpdateBf { it.copy(purpose = v) } }, "उद्देश (लिहा किंवा निवडा)", tag = "bf_purpose_txt", enableVoice = true)
                SgsTextField(d.remarks, { v -> onUpdateBf { it.copy(remarks = v) } }, "शेरा", enableVoice = true)
            }
        }

        BannerView(state.bfStatus)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (canSave) {
                Button(onClick = onSaveBf, colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.weight(1f).height(50.dp).testTag("bf_save_btn")) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Save & Issue", fontWeight = FontWeight.Bold)
                }
            }
            Button(onClick = { onPreview(CertPreviewPayload.BonafidePreview(d)) }, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.weight(1f).height(50.dp).testTag("bf_preview_btn")) {
                Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("Preview / Print", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AttendanceCertScreen(
    state: CertFormsUiState,
    canSave: Boolean,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onUpdateAt: ((AttendanceCertFormData) -> AttendanceCertFormData) -> Unit,
    onSaveAt: () -> Unit,
    onPreview: (CertPreviewPayload) -> Unit
) {
    val d = state.at
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SgsCard(title = "🔍 विद्यार्थी शोधा (उपस्थिती प्रमाणपत्र)") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(state.atSearchQuery, onSearchChange, "Reg No / ID", modifier = Modifier.weight(1f), tag = "at_search_input")
                Button(onClick = onSearch, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy), modifier = Modifier.height(54.dp).testTag("at_search_btn")) {
                    Icon(Icons.Default.Search, contentDescription = "शोधा")
                }
            }
        }

        SgsCard(title = "✅ उपस्थिती प्रमाणपत्र तपशील") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = d.lang == "mr", onClick = { onUpdateAt { it.copy(lang = "mr") } }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                        Text("मराठी")
                    }
                    SegmentedButton(selected = d.lang == "en", onClick = { onUpdateAt { it.copy(lang = "en") } }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                        Text("English")
                    }
                }
                SgsTextField(d.fullName, { v -> onUpdateAt { it.copy(fullName = v) } }, "विद्यार्थ्याचे नाव*", tag = "at_full_name")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDateField("पासून (From)", d.fromDate, { v -> onUpdateAt { it.copy(fromDate = v) } }, modifier = Modifier.weight(1f))
                    SgsDateField("पर्यंत (To)", d.toDate, { v -> onUpdateAt { it.copy(toDate = v) } }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(d.totalDays, { v -> onUpdateAt { it.copy(totalDays = v) } }, "एकूण दिवस", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), tag = "at_total_days")
                    SgsTextField(d.presentDays, { v -> onUpdateAt { it.copy(presentDays = v) } }, "उपस्थित दिवस", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), tag = "at_present_days")
                    SgsTextField(d.pct, {}, "टक्केवारी", readOnly = true, modifier = Modifier.weight(0.9f))
                }
                SgsDropdown("उद्देश", d.purpose, MarathiUtils.AT_PURPOSES, { v -> onUpdateAt { it.copy(purpose = if (v == "इतर") "" else v) } })
                SgsTextField(d.purpose, { v -> onUpdateAt { it.copy(purpose = v) } }, "उद्देश (लिहा किंवा निवडा)", enableVoice = true)
            }
        }

        BannerView(state.atStatus)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (canSave) {
                Button(onClick = onSaveAt, colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.weight(1f).height(50.dp).testTag("at_save_btn")) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Save & Issue", fontWeight = FontWeight.Bold)
                }
            }
            Button(onClick = { onPreview(CertPreviewPayload.AttendancePreview(d)) }, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue), modifier = Modifier.weight(1f).height(50.dp).testTag("at_preview_btn")) {
                Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("Preview / Print", fontWeight = FontWeight.Bold)
            }
        }
    }
}
