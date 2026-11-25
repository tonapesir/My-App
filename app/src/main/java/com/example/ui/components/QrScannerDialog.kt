package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.Student
import com.example.domain.util.MarathiUtils
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Emerald
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintGreen
import com.example.ui.theme.Navy
import com.example.ui.theme.Navy2
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextCream
import com.example.ui.theme.TextMuted
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun QrScannerDialog(
    canOpenEarlyLeave: Boolean,
    findCachedStudent: (String) -> Student?,
    onOpenProfile: (String) -> Unit,
    onOpenEarlyLeave: (String) -> Unit,
    onOpenSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPerm = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPerm) permLauncher.launch(Manifest.permission.CAMERA)
    }

    var scannedRaw by remember { mutableStateOf<String?>(null) }
    var scannedCode by remember { mutableStateOf("") }
    var torchOn by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    DisposableEffect(hasCameraPerm, lifecycleOwner) {
        if (hasCameraPerm) {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            val executor = ContextCompat.getMainExecutor(context)

            cameraController.setImageAnalysisAnalyzer(
                executor,
                MlKitAnalyzer(
                    listOf(scanner),
                    CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    executor
                ) { result ->
                    if (scannedRaw != null) return@MlKitAnalyzer
                    val codes = result?.getValue(scanner).orEmpty()
                    val firstRaw = codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                    if (!firstRaw.isNullOrBlank()) {
                        scannedRaw = firstRaw
                        scannedCode = MarathiUtils.parseQrLookupCode(firstRaw)
                    }
                }
            )
            runCatching { cameraController.bindToLifecycle(lifecycleOwner) }

            onDispose {
                runCatching {
                    cameraController.clearImageAnalysisAnalyzer()
                    cameraController.unbind()
                    scanner.close()
                }
            }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(torchOn, hasCameraPerm) {
        if (hasCameraPerm) {
            runCatching { cameraController.enableTorch(torchOn) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            shape = RoundedCornerShape(18.dp),
            color = Navy
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepNavy)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = GoldLight)
                        Column {
                            Text("ID कार्ड QR स्कॅनर", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("विद्यार्थी ओळखपत्र / दाखला QR स्कॅन करा", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasCameraPerm) {
                            IconButton(onClick = { torchOn = !torchOn }) {
                                Icon(
                                    if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "फ्लॅश लाइट",
                                    tint = if (torchOn) Gold else TextCream
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "बंद करा", tint = TextCream)
                        }
                    }
                }

                // Camera Preview Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPerm) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    controller = cameraController
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Scan Frame Box
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .border(3.dp, if (scannedCode.isNotBlank()) MintGreen else Gold, RoundedCornerShape(18.dp))
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text("📷 QR स्कॅन करण्यासाठी कॅमेरा परवानगी आवश्यक आहे.", color = TextCream, textAlign = TextAlign.Center)
                            Button(
                                onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy)
                            ) {
                                Text("परवानगी द्या", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Bottom Result / Action Panel
                Surface(
                    color = Navy2,
                    border = BorderStroke(1.dp, Gold.copy(0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (scannedCode.isNotBlank()) {
                            val matched = remember(scannedCode) { findCachedStudent(scannedCode) }
                            Text("✅ स्कॅन झाले: $scannedCode", color = MintGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (matched != null) {
                                Text(
                                    "👤 ${matched.fullName} (${MarathiUtils.classToMr(matched.iyatta)} ${matched.tukdi}, Roll ${matched.rollNo})",
                                    color = GoldLight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenProfile(scannedCode)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                                    modifier = Modifier.weight(1f).testTag("qr_open_profile_btn")
                                ) {
                                    Text("🪪 प्रोफाइल", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (canOpenEarlyLeave) {
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onOpenEarlyLeave(scannedCode)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                        modifier = Modifier.weight(1f).testTag("qr_open_early_leave_btn")
                                    ) {
                                        Text("🚪 Early Leave", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenSearch(scannedCode)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🔍 शोध मध्ये पहा", color = TextCream, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        scannedRaw = null
                                        scannedCode = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🔄 पुन्हा स्कॅन", color = GoldLight, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text("QR कोड कॅमेराच्या फ्रेममध्ये धरा किंवा खाली Reg No लिहा:", color = TextMuted, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                SgsTextField(
                                    value = manualInput,
                                    onValueChange = { manualInput = it },
                                    label = "Reg No / ID",
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (manualInput.isNotBlank()) {
                                            scannedCode = MarathiUtils.parseQrLookupCode(manualInput)
                                            scannedRaw = manualInput
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                                    modifier = Modifier.height(54.dp)
                                ) { Text("उघडा", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}
