package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.compositionLocalOf
import com.example.domain.model.Student
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.domain.model.PendingSaveItem
import com.example.domain.util.MarathiUtils
import com.example.ui.theme.ColorTokens
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Emerald
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintGreen
import com.example.ui.theme.Navy
import com.example.ui.theme.Navy2
import com.example.ui.theme.Peach
import com.example.ui.theme.PeachBorder
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SoftRed
import com.example.ui.theme.TextCream
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.BannerMessage
import java.time.Instant
import java.time.ZoneOffset

@Composable
fun SgsCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy),
        border = BorderStroke(1.dp, Navy2),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Gold.copy(alpha = 0.25f)
                )
            }
            content()
        }
    }
}

@Composable
fun SgsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    tag: String = "",
    placeholder: String = "",
    readOnly: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enableVoice: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotBlank()) {
                val next = if (value.isBlank()) spoken else "${value.trimEnd()} $spoken"
                onValueChange(next)
            }
        }
    }

    val effectiveTrailing: (@Composable () -> Unit)? = when {
        trailingIcon != null -> trailingIcon
        enableVoice && !readOnly && enabled -> ({
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "mr-IN")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "mr-IN")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "मराठीत बोला ($label)...")
                    }
                    runCatching { voiceLauncher.launch(intent) }.onFailure {
                        Toast.makeText(context, "या डिव्हाइसवर व्हॉइस टायपिंग उपलब्ध नाही.", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.Mic, contentDescription = "मराठी व्हॉइस टायपिंग", tint = RoyalBlue)
            }
        })
        else -> null
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1) },
        placeholder = if (placeholder.isNotBlank()) ({ Text(placeholder, color = Color(0xFF8A7562)) }) else null,
        readOnly = readOnly,
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = effectiveTrailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Peach,
            unfocusedContainerColor = Peach,
            disabledContainerColor = Peach.copy(alpha = 0.72f),
            focusedTextColor = DeepNavy,
            unfocusedTextColor = DeepNavy,
            disabledTextColor = Color(0xFF4A3B31),
            focusedBorderColor = Gold,
            unfocusedBorderColor = PeachBorder,
            focusedLabelColor = GoldLight,
            unfocusedLabelColor = TextMuted,
            cursorColor = DeepNavy
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (tag.isNotBlank()) Modifier.testTag(tag) else Modifier)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgsDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "",
    enabled: Boolean = true,
    displayTransform: (String) -> String = { it }
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayTransform(selected),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label, maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Peach,
                unfocusedContainerColor = Peach,
                disabledContainerColor = Peach.copy(alpha = 0.7f),
                focusedTextColor = DeepNavy,
                unfocusedTextColor = DeepNavy,
                focusedBorderColor = Gold,
                unfocusedBorderColor = PeachBorder,
                focusedLabelColor = GoldLight,
                unfocusedLabelColor = TextMuted
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .then(if (tag.isNotBlank()) Modifier.testTag(tag) else Modifier)
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            containerColor = Peach
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayTransform(option), color = DeepNavy) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgsDateField(
    label: String,
    isoDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    tag: String = ""
) {
    var openPicker by remember { mutableStateOf(false) }
    SgsTextField(
        value = MarathiUtils.fmtDate(isoDate).ifBlank { isoDate },
        onValueChange = { raw ->
            val iso = MarathiUtils.isoDateOrEmpty(raw)
            if (iso.isNotBlank()) onDateSelected(iso)
        },
        label = label,
        placeholder = "DD-MM-YYYY",
        readOnly = true,
        tag = tag,
        trailingIcon = {
            IconButton(onClick = { openPicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "तारीख निवडा", tint = Navy)
            }
        },
        modifier = modifier.clickable { openPicker = true }
    )

    if (openPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { openPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = state.selectedDateMillis
                    if (ms != null) {
                        val local = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(local.toString())
                    }
                    openPicker = false
                }) { Text("निवडा") }
            },
            dismissButton = {
                TextButton(onClick = { openPicker = false }) { Text("रद्द करा") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun BannerView(banner: BannerMessage?, modifier: Modifier = Modifier) {
    if (banner == null) return
    val (bg, fg, icon) = when (banner) {
        is BannerMessage.Ok -> Triple(ColorTokens.SuccessBg, MintGreen, Icons.Default.CheckCircle)
        is BannerMessage.Err -> Triple(ColorTokens.ErrorBg, SoftRed, Icons.Default.Error)
        is BannerMessage.Queued -> Triple(ColorTokens.WarningBg, ColorTokens.WarningText, Icons.Default.CloudQueue)
        is BannerMessage.Info -> Triple(Navy2, SkyBlue, Icons.Default.Info)
    }
    val msg = when (banner) {
        is BannerMessage.Ok -> banner.text
        is BannerMessage.Err -> banner.text
        is BannerMessage.Queued -> banner.text
        is BannerMessage.Info -> banner.text
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Text(msg, color = fg, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun StudentPhotoBox(
    photoUrl: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 88,
    fallbackBitmap: Bitmap? = null
) {
    val resolvedUrl = remember(photoUrl) { resolveDriveImageUrl(photoUrl) }
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Navy2)
            .border(1.5.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            fallbackBitmap != null -> {
                Image(
                    bitmap = fallbackBitmap.asImageBitmap(),
                    contentDescription = "विद्यार्थी फोटो",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            resolvedUrl.isBlank() -> {
                Icon(Icons.Default.Person, contentDescription = "फोटो नाही", tint = TextMuted, modifier = Modifier.size((sizeDp / 2).dp))
            }
            else -> {
                SubcomposeAsyncImage(
                    model = resolvedUrl,
                    contentDescription = "विद्यार्थी फोटो",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    error = {
                        Icon(Icons.Default.Person, contentDescription = "फोटो लोड नाही", tint = TextMuted)
                    }
                )
            }
        }
    }
}

fun resolveDriveImageUrl(raw: String): String {
    val t = raw.trim()
    if (t.isBlank()) return ""
    val idMatch = Regex("""[?&]id=([A-Za-z0-9_-]+)|/d/([A-Za-z0-9_-]+)""").find(t)
    val fileId = idMatch?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }
    return if (fileId != null) "https://drive.google.com/uc?export=view&id=$fileId" else t
}

@Composable
fun PendingSavesModal(
    items: List<PendingSaveItem>,
    onRetryAll: () -> Unit,
    onExportJson: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy,
        titleContentColor = GoldLight,
        textContentColor = TextCream,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("⏳ Pending Saves (${items.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "बंद करा", tint = TextMuted) }
            }
        },
        text = {
            if (items.isEmpty()) {
                Text("सर्व Pending नोंदी पाठवल्या आहेत.", color = MintGreen)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(items, key = { it.reqId }) { item ->
                        Surface(
                            color = Navy2,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (item.failed) SoftRed.copy(0.4f) else Gold.copy(0.3f))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.summary.ifBlank { item.action }, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextCream)
                                    Text("Action: ${item.action} | By: ${item.by}", fontSize = 11.sp, color = TextMuted)
                                    if (item.lastError.isNotBlank()) Text(item.lastError, fontSize = 11.sp, color = SoftRed)
                                }
                                IconButton(onClick = { onRemoveItem(item.reqId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "काढा", tint = SoftRed)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (items.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExportJson,
                        modifier = Modifier.testTag("pending_export_json_button")
                    ) { Text("📤 JSON", color = GoldLight) }
                    Button(
                        onClick = onRetryAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.testTag("pending_retry_all_button")
                    ) {
                        Text("🔄 आत्ता पाठवा")
                    }
                }
            }
        },
        dismissButton = {
            if (items.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("pending_clear_all_button")
                ) { Text("सर्व पुसा", color = SoftRed) }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigSettingsDialog(
    url: String,
    cacheCount: Int,
    isBulkLoading: Boolean,
    bulkMessage: String?,
    testStatus: BannerMessage?,
    onUrlChange: (String) -> Unit,
    onTest: () -> Unit,
    onBulkLoad: () -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    biometricEnabled: Boolean = false,
    biometricSupported: Boolean = false,
    onToggleBiometric: (Boolean) -> Unit = {},
    remindersEnabled: Boolean = true,
    onToggleReminders: (Boolean) -> Unit = {},
    autoLockMinutes: Int = 0,
    onAutoLockChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleReminders(granted)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy,
        titleContentColor = GoldLight,
        textContentColor = TextCream,
        title = { Text("⚙️ सेटिंग्ज व Cache", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SgsTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = "Apps Script Web App URL",
                    tag = "config_url_input",
                    singleLine = false
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onTest,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.weight(1f).testTag("test_connection_button")
                    ) { Text("🔧 Test") }
                    Button(
                        onClick = onBulkLoad,
                        enabled = !isBulkLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                        modifier = Modifier.weight(1f).testTag("bulk_load_button")
                    ) { Text("⚡ सर्व Load") }
                }
                BannerView(testStatus)
                if (!bulkMessage.isNullOrBlank()) {
                    Text(bulkMessage, color = MintGreen, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Cache विद्यार्थी: $cacheCount", color = TextMuted, fontSize = 13.sp)
                    TextButton(onClick = onClearCache) { Text("🗑️ Cache पुसा", color = SoftRed) }
                }

                HorizontalDivider(color = Gold.copy(alpha = 0.25f))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(if (isDarkTheme) "🌙 डार्क थीम (Dark Mode)" else "☀️ लाईट थीम (Light Mode)", color = TextCream, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("अ‍ॅपचा रंग मोड बदला", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = !isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepNavy, checkedTrackColor = Gold),
                        modifier = Modifier.testTag("settings_theme_switch")
                    )
                }

                if (biometricSupported) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("👆 फिंगरप्रिंट / बायोमेट्रिक लॉक", color = TextCream, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("अ‍ॅप उघडताना व जलद Login साठी", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = onToggleBiometric,
                            colors = SwitchDefaults.colors(checkedThumbColor = DeepNavy, checkedTrackColor = Gold),
                            modifier = Modifier.testTag("settings_biometric_switch")
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("🔔 दैनंदिन हजेरी व वाढदिवस अलर्ट", color = TextCream, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("बाकी हजेरी, वाढदिवस व ऑटो-सिंक सूचना", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { want ->
                            if (want && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onToggleReminders(want)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepNavy, checkedTrackColor = Gold),
                        modifier = Modifier.testTag("settings_reminders_switch")
                    )
                }

                HorizontalDivider(color = Gold.copy(alpha = 0.25f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⏱️ अ‍ॅप निष्क्रियता ऑटो-लॉक (Auto-Lock)", color = TextCream, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("पार्श्वभूमीवर (Background) गेल्यास ठरलेल्या वेळेनंतर अ‍ॅप आपोआप लॉक होईल.", color = TextMuted, fontSize = 11.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            0 to "बंद",
                            1 to "१ मिनिट",
                            2 to "२ मिनिटे",
                            5 to "५ मिनिटे",
                            15 to "१५ मिनिटे"
                        ).forEach { (mins, label) ->
                            FilterChip(
                                selected = autoLockMinutes == mins,
                                onClick = { onAutoLockChange(mins) },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("settings_autolock_$mins")
                            )
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

fun openWhatsApp(context: Context, mobile: String, message: String) {
    val url = MarathiUtils.whatsappUrl(mobile, message)
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}

data class WhatsAppLaunchRequest(
    val mobile: String,
    val defaultMessage: String = "",
    val recipientLabel: String = "",
    val student: Student? = null,
    val customClassLabel: String = ""
)

val LocalWhatsAppLauncher = compositionLocalOf<(WhatsAppLaunchRequest) -> Unit> {
    { _ -> }
}

@Composable
fun MobileCallWaButtons(
    mobile: String,
    recipientLabel: String = "",
    defaultMessage: String = "",
    student: Student? = null,
    customClassLabel: String = "",
    allowWaWhenEmpty: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launchWa = LocalWhatsAppLauncher.current
    val hasPhone = MarathiUtils.cleanDigits10(mobile).isNotEmpty()
    if (!hasPhone && !allowWaWhenEmpty) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasPhone) {
            IconButton(
                onClick = { dialPhone(context, mobile) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = "कॉल $recipientLabel", tint = MintGreen, modifier = Modifier.size(20.dp))
            }
        }
        Button(
            onClick = {
                launchWa(
                    WhatsAppLaunchRequest(
                        mobile = mobile,
                        defaultMessage = defaultMessage,
                        recipientLabel = recipientLabel,
                        student = student,
                        customClassLabel = customClassLabel
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
            modifier = Modifier.height(36.dp)
        ) {
            Text(if (hasPhone) "💬 WA" else "💬 निवडा", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun dialPhone(context: Context, mobile: String) {
    val digits = MarathiUtils.cleanDigits10(mobile)
    if (digits.isEmpty()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$digits")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}

fun sendSms(context: Context, mobile: String, body: String) {
    val digits = MarathiUtils.cleanDigits10(mobile)
    if (digits.isEmpty()) return
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:+91$digits")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
