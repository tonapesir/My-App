package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.domain.model.AppPage
import com.example.domain.model.Student
import com.example.domain.util.MarathiUtils
import com.example.domain.util.PdfAndExportUtils
import com.example.ui.components.BannerView
import com.example.ui.components.MobileCallWaButtons
import com.example.ui.components.PhotoCropDialog
import com.example.ui.components.SgsCard
import com.example.ui.components.SgsDateField
import com.example.ui.components.SgsDropdown
import com.example.ui.components.SgsTextField
import com.example.ui.components.StudentPhotoBox
import com.example.ui.theme.TextMuted
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Emerald
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftRed
import com.example.ui.theme.TextCream
import com.example.ui.viewmodel.StudentEntryUiState

@Composable
fun StudentEntryScreen(
    state: StudentEntryUiState,
    canEdit: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onUpdateForm: ((Student) -> Student) -> Unit,
    onUploadPhoto: (Uri) -> Unit,
    onTestPhotoDrive: () -> Unit,
    onSaveNew: () -> Unit,
    onUpdateExisting: () -> Unit,
    onClearForm: () -> Unit,
    onDismissSavedDialog: () -> Unit,
    onQuickOpenModule: (Student, AppPage) -> Unit
) {
    val context = LocalContext.current
    val f = state.form
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingCropUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val cap = tempCameraUri
        if (success && cap != null) pendingCropUri = cap
    }

    fun launchCameraCapture() {
        val file = java.io.File(context.cacheDir, "cam_capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        tempCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCameraCapture()
        else Toast.makeText(context, "फोटो काढण्यासाठी कॅमेरा परवानगी आवश्यक आहे.", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search Bar
        SgsCard(title = "🔍 विद्यार्थी शोधा (Reg No / Student ID / आधार)") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SgsTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = "नोंद क्र. / ID / आधार",
                    tag = "student_search_input",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                    modifier = Modifier.height(54.dp).testTag("student_search_btn")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "शोधा")
                }
            }
            if (state.isExistingRow) {
                Spacer(Modifier.height(6.dp))
                Text("📌 Row #${f.rowIndex} उघडली आहे — खालील माहिती तपासून Update करू शकता.", color = GoldLight, fontSize = 12.sp)
            }
        }

        // Photo Upload Card
        SgsCard(title = "📷 विद्यार्थी फोटो (कॅमेरा / गॅलरी + Crop <10 KB)") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudentPhotoBox(f.photoUrl, sizeDp = 92)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    launchCameraCapture()
                                } else {
                                    cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            enabled = canEdit && !state.isUploadingPhoto,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                            modifier = Modifier.weight(1f).testTag("student_camera_photo_btn")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("कॅमेरा", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = canEdit && !state.isUploadingPhoto,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier.weight(1f).testTag("student_pick_photo_btn")
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("गॅलरी", fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = onTestPhotoDrive,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth().testTag("test_drive_photo_btn")
                    ) {
                        Text(if (state.isUploadingPhoto) "⏳ Upload होत आहे..." else "🔧 Drive Folder तपासा", color = TextCream, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            BannerView(state.photoStatus)
        }

        // Basic & School Details
        SgsCard(title = "🏫 प्रवेश व वर्ग तपशील") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(f.acYear, { v -> onUpdateForm { it.copy(acYear = v) } }, "शैक्षणिक वर्ष", modifier = Modifier.weight(1f), tag = "s_ac_year")
                    SgsTextField(f.bookNo, { v -> onUpdateForm { it.copy(bookNo = v) } }, "बुक क्र.", modifier = Modifier.weight(1f), tag = "s_book_no")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(f.regNo, { v -> onUpdateForm { it.copy(regNo = v) } }, "जनरल रजि. क्र.*", modifier = Modifier.weight(1f), tag = "s_reg_no")
                    SgsTextField(f.studentId, { v -> onUpdateForm { it.copy(studentId = v) } }, "स्टुडंट आयडी (SARAL)", modifier = Modifier.weight(1f), tag = "s_student_id")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("इयत्ता*", f.iyatta, MarathiUtils.CLASSES, { v -> onUpdateForm { it.copy(iyatta = v) } }, modifier = Modifier.weight(1f), tag = "s_iyatta", displayTransform = { MarathiUtils.classToMr(it) })
                    SgsDropdown("तुकडी", f.tukdi, MarathiUtils.DIVISIONS, { v -> onUpdateForm { it.copy(tukdi = v) } }, modifier = Modifier.weight(1f), tag = "s_tukdi")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(f.rollNo, { v -> onUpdateForm { it.copy(rollNo = v) } }, "हजेरी क्र. (Roll No)", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), tag = "s_roll_no")
                    SgsTextField(f.pen, { v -> onUpdateForm { it.copy(pen = v) } }, "PEN नंबर", modifier = Modifier.weight(1f), tag = "s_pen")
                }
                SgsTextField(
                    value = f.aadhar,
                    onValueChange = { v -> onUpdateForm { it.copy(aadhar = v.filter { ch -> ch.isDigit() }.take(12)) } },
                    label = "आधार कार्ड क्र. (१२ अंकी)",
                    keyboardType = KeyboardType.Number,
                    tag = "s_aadhar"
                )
            }
        }

        // Personal Details
        SgsCard(title = "👤 वैयक्तिक माहिती") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SgsTextField(f.fullName, { v -> onUpdateForm { it.copy(fullName = v) } }, "विद्यार्थ्याचे संपूर्ण नाव*", tag = "s_full_name", enableVoice = true)
                SgsTextField(f.motherName, { v -> onUpdateForm { it.copy(motherName = v) } }, "आईचे नाव", tag = "s_mother_name", enableVoice = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("लिंग", f.gender, listOf("Male", "Female"), { v -> onUpdateForm { it.copy(gender = v) } }, modifier = Modifier.weight(1f), tag = "s_gender")
                    SgsDropdown("प्रवर्ग (Category)", f.category, MarathiUtils.CATEGORIES, { v -> onUpdateForm { it.copy(category = v) } }, modifier = Modifier.weight(1f), tag = "s_category")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDropdown("धर्म", f.religion, MarathiUtils.RELIGIONS, { v -> onUpdateForm { it.copy(religion = v) } }, modifier = Modifier.weight(1f), tag = "s_religion")
                    SgsTextField(f.caste, { v -> onUpdateForm { it.copy(caste = v) } }, "जात", modifier = Modifier.weight(1f), tag = "s_caste")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(f.subcaste, { v -> onUpdateForm { it.copy(subcaste = v) } }, "पोटजात", modifier = Modifier.weight(1f))
                    val normMin = when (f.minority.trim()) {
                        "होय", "Yes" -> "Yes"
                        else -> "No"
                    }
                    SgsDropdown(
                        label = "अल्पसंख्यांक",
                        selected = normMin,
                        options = listOf("No", "Yes"),
                        onSelect = { v -> onUpdateForm { it.copy(minority = v) } },
                        modifier = Modifier.weight(1f),
                        displayTransform = { if (it == "Yes") "होय" else "नाही" }
                    )
                }
                SgsDateField("जन्मतारीख", f.dob, { v -> onUpdateForm { it.copy(dob = v) } }, tag = "s_dob")
                SgsTextField(f.dobWords, { v -> onUpdateForm { it.copy(dobWords = v) } }, "जन्मतारीख (अक्षरी)", tag = "s_dob_words")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(f.nationality, { v -> onUpdateForm { it.copy(nationality = v) } }, "राष्ट्रीयत्व", modifier = Modifier.weight(1f))
                    SgsDropdown("मातृभाषा", f.motherTongue, MarathiUtils.MOTHER_TONGUES, { v -> onUpdateForm { it.copy(motherTongue = v) } }, modifier = Modifier.weight(1f))
                }
                SgsTextField(f.birthVillage, { v -> onUpdateForm { it.copy(birthVillage = v) } }, "जन्मस्थळ (गाव/शहर)", enableVoice = true)
                SgsTextField(f.prevSchool, { v -> onUpdateForm { it.copy(prevSchool = v) } }, "या पूर्वीची शाळा", enableVoice = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsDateField("प्रवेश दिनांक", f.admissionDate, { v -> onUpdateForm { it.copy(admissionDate = v) } }, modifier = Modifier.weight(1f))
                    SgsDropdown("प्रवेश इयत्ता", f.admissionClass, MarathiUtils.ADMISSION_CLASSES, { v -> onUpdateForm { it.copy(admissionClass = v) } }, modifier = Modifier.weight(1f), displayTransform = { MarathiUtils.classToMr(it) })
                }
            }
        }

        // Contact Details
        SgsCard(title = "📞 संपर्क व पत्ता") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SgsTextField(f.whatsappMobile, { v -> onUpdateForm { it.copy(whatsappMobile = v.filter { c -> c.isDigit() }.take(10)) } }, "WhatsApp मोबाईल", keyboardType = KeyboardType.Phone, modifier = Modifier.weight(1f), tag = "s_whatsapp")
                    SgsTextField(f.alternateMobile, { v -> onUpdateForm { it.copy(alternateMobile = v.filter { c -> c.isDigit() }.take(10)) } }, "पर्यायी मोबाईल", keyboardType = KeyboardType.Phone, modifier = Modifier.weight(1f), tag = "s_alt_mobile")
                }
                if (MarathiUtils.cleanDigits10(f.whatsappMobile).isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("WhatsApp नं.: ${f.whatsappMobile}", color = TextMuted, fontSize = 12.sp)
                        MobileCallWaButtons(
                            mobile = f.whatsappMobile,
                            recipientLabel = f.fullName.ifBlank { "विद्यार्थी" },
                            student = f
                        )
                    }
                }
                if (MarathiUtils.cleanDigits10(f.alternateMobile).isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("पर्यायी नं.: ${f.alternateMobile}", color = TextMuted, fontSize = 12.sp)
                        MobileCallWaButtons(
                            mobile = f.alternateMobile,
                            recipientLabel = f.fullName.ifBlank { "विद्यार्थी" },
                            student = f
                        )
                    }
                }
                SgsTextField(f.address, { v -> onUpdateForm { it.copy(address = v) } }, "संपूर्ण पत्ता", singleLine = false, tag = "s_address", enableVoice = true)
            }
        }

        BannerView(state.status)

        // Action Buttons
        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSaveNew,
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("student_save_new_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Save New", fontWeight = FontWeight.Bold)
                }
                if (state.isExistingRow) {
                    Button(
                        onClick = onUpdateExisting,
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                        modifier = Modifier.weight(1f).height(50.dp).testTag("student_update_btn")
                    ) {
                        Text("Update Row", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val pdf = PdfAndExportUtils.renderStudentProfilePdf(context, f)
                    PdfAndExportUtils.shareFile(context, pdf, "application/pdf", "विद्यार्थी माहिती PDF")
                },
                modifier = Modifier.weight(1f).height(48.dp).testTag("student_pdf_btn")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("PDF काढा", color = GoldLight)
            }
            OutlinedButton(
                onClick = onClearForm,
                modifier = Modifier.weight(1f).height(48.dp).testTag("student_clear_btn")
            ) {
                Text("फॉर्म रिकामा करा", color = SoftRed)
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    state.savedDialogStudent?.let { savedStu ->
        AlertDialog(
            onDismissRequest = onDismissSavedDialog,
            containerColor = com.example.ui.theme.Navy,
            titleContentColor = GoldLight,
            textContentColor = TextCream,
            title = { Text("✅ नोंद यशस्वी!", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${savedStu.fullName} (Reg: ${savedStu.regNo}) यांची माहिती सुरक्षित Save झाली आहे.")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onQuickOpenModule(savedStu, AppPage.PROFILE) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier.weight(1f).testTag("saved_dialog_profile_btn")
                        ) { Text("प्रोफाइल", fontSize = 11.5.sp) }
                        Button(
                            onClick = { onQuickOpenModule(savedStu, AppPage.BONAFIDE) },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                            modifier = Modifier.weight(1f).testTag("saved_dialog_bf_btn")
                        ) { Text("बोनाफाईड", fontSize = 11.5.sp) }
                        Button(
                            onClick = { onQuickOpenModule(savedStu, AppPage.LC) },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                            modifier = Modifier.weight(1f).testTag("saved_dialog_lc_btn")
                        ) { Text("LC", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissSavedDialog()
                        onClearForm()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    modifier = Modifier.testTag("saved_dialog_new_btn")
                ) { Text("पुन्हा नवीन विद्यार्थी") }
            },
            dismissButton = {
                TextButton(onClick = onDismissSavedDialog) { Text("बंद करा", color = GoldLight) }
            }
        )
    }

    pendingCropUri?.let { cropUri ->
        PhotoCropDialog(
            sourceUri = cropUri,
            onCropped = { cropped ->
                pendingCropUri = null
                onUploadPhoto(cropped)
            },
            onDismiss = { pendingCropUri = null }
        )
    }
}
