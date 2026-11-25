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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MessageTemplate
import com.example.domain.model.Student
import com.example.domain.util.MarathiUtils
import com.example.ui.theme.ColorTokens
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Emerald
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintGreen
import com.example.ui.theme.Navy
import com.example.ui.theme.Navy2
import com.example.ui.theme.SoftRed
import com.example.ui.theme.TextCream
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WhatsAppGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageTemplatesDialog(
    templates: List<MessageTemplate>,
    onSaveTemplate: (title: String, body: String, editId: String?) -> Unit,
    onDeleteTemplate: (id: String) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var editId by remember { mutableStateOf<String?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy,
        titleContentColor = GoldLight,
        textContentColor = TextCream,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("💬 सानुकूल मेसेज टेम्पलेट्स", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (editId == null) "➕ नवीन टेम्पलेट तयार करा" else "✏️ टेम्पलेट संपादन",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
                SgsTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = "टेम्पलेट नाव (उदा. पालक सभा)",
                    tag = "tpl_title_input",
                    enableVoice = true
                )
                SgsTextField(
                    value = bodyInput,
                    onValueChange = { bodyInput = it },
                    label = "मेसेज मजकूर ({नाव}, {वर्ग}, {रोल}, {नोंद})",
                    tag = "tpl_body_input",
                    singleLine = false,
                    enableVoice = true
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("{नाव}", "{वर्ग}", "{रोल}", "{नोंद}").forEach { ph ->
                        AssistChip(
                            onClick = { bodyInput = if (bodyInput.isBlank()) ph else "$bodyInput $ph" },
                            label = { Text("+ $ph", fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (titleInput.isNotBlank() && bodyInput.isNotBlank()) {
                                onSaveTemplate(titleInput, bodyInput, editId)
                                editId = null
                                titleInput = ""
                                bodyInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.weight(1f).testTag("tpl_save_btn")
                    ) {
                        Text(if (editId == null) "जतन करा" else "अद्ययावत करा", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (editId != null) {
                        OutlinedButton(
                            onClick = {
                                editId = null
                                titleInput = ""
                                bodyInput = ""
                            }
                        ) { Text("रद्द", color = SoftRed, fontSize = 12.sp) }
                    }
                    OutlinedButton(onClick = onResetDefaults) {
                        Text("🔄 मूळ", color = GoldLight, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Gold.copy(0.25f))
                Text("जतन केलेले टेम्पलेट्स (${templates.size})", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)

                if (templates.isEmpty()) {
                    Text("कोणतेही टेम्पलेट नाही. '🔄 मूळ' दाबून तयार टेम्पलेट्स मिळवा.", color = TextMuted, fontSize = 12.sp)
                } else {
                    templates.forEach { tpl ->
                        Surface(
                            color = Navy2,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Gold.copy(0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(tpl.title, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            editId = tpl.id
                                            titleInput = tpl.title
                                            bodyInput = tpl.body
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "संपादित करा", tint = GoldLight, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteTemplate(tpl.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "हटवा", tint = SoftRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(tpl.body, color = TextCream, fontSize = 11.5.sp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassBroadcastQueueDialog(
    title: String = "📢 वर्ग मेसेज प्रसारण (WhatsApp / SMS)",
    students: List<Student>,
    templates: List<MessageTemplate>,
    customClassLabel: String = "",
    onOpenManageTemplates: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTplId by remember(templates) { mutableStateOf(templates.firstOrNull()?.id.orEmpty()) }
    var draftBody by remember(templates) {
        mutableStateOf(
            templates.firstOrNull()?.body
                ?: "नमस्कार, श्री. गो. से. हायस्कूल, पाचोरा. विद्यार्थी {नाव} ({वर्ग}, Roll {रोल}) यांच्या पालकांसाठी महत्त्वाची सूचना."
        )
    }
    var queueStarted by remember { mutableStateOf(false) }
    var queueIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy,
        titleContentColor = GoldLight,
        textContentColor = TextCream,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (queueStarted) "📤 प्रसारण (${queueIndex + 1} / ${students.size})" else title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "बंद करा", tint = TextMuted)
                }
            }
        },
        text = {
            if (students.isEmpty()) {
                Text("यादीत विद्यार्थी नाहीत.", color = TextMuted)
            } else if (!queueStarted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("टेम्पलेट निवडा (${students.size} विद्यार्थी):", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        TextButton(onClick = onOpenManageTemplates) {
                            Text("✏️ संपादन", color = GoldLight, fontSize = 11.5.sp)
                        }
                    }
                    if (templates.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            templates.forEach { tpl ->
                                FilterChip(
                                    selected = selectedTplId == tpl.id,
                                    onClick = {
                                        selectedTplId = tpl.id
                                        draftBody = tpl.body
                                    },
                                    label = { Text(tpl.title, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    SgsTextField(
                        value = draftBody,
                        onValueChange = { draftBody = it },
                        label = "संदेश मजकूर ({नाव}, {वर्ग}, {रोल}, {नोंद})",
                        singleLine = false,
                        enableVoice = true,
                        tag = "broadcast_msg_input"
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("{नाव}", "{वर्ग}", "{रोल}", "{नोंद}").forEach { ph ->
                            AssistChip(
                                onClick = { draftBody = if (draftBody.isBlank()) ph else "$draftBody $ph" },
                                label = { Text("+ $ph", fontSize = 11.sp) }
                            )
                        }
                    }
                    val samplePreview = MarathiUtils.formatMessageTemplate(draftBody, students.first(), customClassLabel)
                    Surface(color = Navy2, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Gold.copy(0.25f)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("नमुना पूर्वावलोकन (${students.first().fullName}):", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(samplePreview, color = TextCream, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                val stu = students.getOrNull(queueIndex)
                if (stu == null) {
                    Text("✅ सर्व संदेश पाठवून पूर्ण झाले.", color = MintGreen)
                } else {
                    val formatted = MarathiUtils.formatMessageTemplate(draftBody, stu, customClassLabel)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Roll ${stu.rollNo.ifBlank { "-" }}. ${stu.fullName}", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Text(
                            if (stu.bestPhone.isNotBlank()) "📱 मोबाईल: ${stu.bestPhone}" else "⚠️ मोबाईल नोंद नाही — WhatsApp मध्ये Contact निवडा",
                            color = if (stu.bestPhone.isNotBlank()) MintGreen else ColorTokens.WarningText,
                            fontSize = 12.sp
                        )
                        Surface(color = Navy2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(formatted, color = TextCream, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                        if (stu.bestPhone.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { dialPhone(context, stu.bestPhone) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = MintGreen)
                                    Spacer(Modifier.size(4.dp))
                                    Text("📞 कॉल", color = MintGreen, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        sendSms(context, stu.bestPhone, formatted)
                                        if (queueIndex + 1 < students.size) queueIndex++ else onDismiss()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldLight)
                                    Spacer(Modifier.size(4.dp))
                                    Text("✉️ SMS व पुढे", color = GoldLight, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (students.isNotEmpty()) {
                if (!queueStarted) {
                    Button(
                        onClick = {
                            if (draftBody.isNotBlank()) {
                                queueIndex = 0
                                queueStarted = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                        modifier = Modifier.testTag("broadcast_start_queue_btn")
                    ) {
                        Text("🚀 क्रमवार रांग सुरू करा (${students.size})", fontWeight = FontWeight.Bold)
                    }
                } else {
                    val stu = students.getOrNull(queueIndex)
                    if (stu != null) {
                        val formatted = MarathiUtils.formatMessageTemplate(draftBody, stu, customClassLabel)
                        Button(
                            onClick = {
                                openWhatsApp(context, stu.bestPhone, formatted)
                                if (queueIndex + 1 < students.size) queueIndex++ else onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                            modifier = Modifier.testTag("broadcast_send_wa_next_btn")
                        ) {
                            Text(if (queueIndex + 1 < students.size) "💬 WA व पुढे" else "💬 WA व पूर्ण", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (queueStarted && students.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (queueIndex > 0) {
                        TextButton(onClick = { queueIndex-- }) { Text("मागे", color = TextCream) }
                    }
                    if (queueIndex + 1 < students.size) {
                        TextButton(onClick = { queueIndex++ }) { Text("पुढे (Skip)", color = GoldLight) }
                    }
                    TextButton(onClick = { queueStarted = false }) { Text("बदल", color = TextMuted) }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("बंद करा", color = TextMuted) }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppComposeWithTemplateDialog(
    request: WhatsAppLaunchRequest,
    templates: List<MessageTemplate>,
    onOpenManageTemplates: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val effectiveStudent = remember(request) {
        request.student ?: Student(
            fullName = request.recipientLabel.ifBlank { "विद्यार्थी" },
            whatsappMobile = request.mobile
        )
    }
    var selectedChipKey by remember(request) {
        mutableStateOf(if (request.defaultMessage.isNotBlank()) "__default__" else templates.firstOrNull()?.id ?: "__blank__")
    }
    var messageBody by remember(request) {
        mutableStateOf(
            if (request.defaultMessage.isNotBlank()) request.defaultMessage
            else templates.firstOrNull()?.let {
                MarathiUtils.formatMessageTemplate(it.body, effectiveStudent, request.customClassLabel)
            }.orEmpty()
        )
    }
    val hasPhone = MarathiUtils.cleanDigits10(request.mobile).isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy,
        titleContentColor = GoldLight,
        textContentColor = TextCream,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (request.recipientLabel.isNotBlank()) "💬 WhatsApp — ${request.recipientLabel}" else "💬 WhatsApp संदेश",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "बंद करा", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (hasPhone) "📱 ${request.mobile}" else "⚠️ मोबाईल नोंद नाही — WhatsApp मध्ये Contact निवडा",
                        color = if (hasPhone) MintGreen else ColorTokens.WarningText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasPhone) {
                        OutlinedButton(onClick = { dialPhone(context, request.mobile) }) {
                            Icon(Icons.Default.Call, contentDescription = "कॉल", tint = MintGreen, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("कॉल", color = MintGreen, fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("टेम्पलेट निवडा:", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    TextButton(onClick = onOpenManageTemplates) {
                        Text("✏️ टेम्पलेट्स", color = GoldLight, fontSize = 11.5.sp)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (request.defaultMessage.isNotBlank()) {
                        FilterChip(
                            selected = selectedChipKey == "__default__",
                            onClick = {
                                selectedChipKey = "__default__"
                                messageBody = request.defaultMessage
                            },
                            label = { Text("✨ मूळ संदेश", fontSize = 11.sp) }
                        )
                    }
                    templates.forEach { tpl ->
                        FilterChip(
                            selected = selectedChipKey == tpl.id,
                            onClick = {
                                selectedChipKey = tpl.id
                                messageBody = MarathiUtils.formatMessageTemplate(tpl.body, effectiveStudent, request.customClassLabel)
                            },
                            label = { Text(tpl.title, fontSize = 11.sp) }
                        )
                    }
                    FilterChip(
                        selected = selectedChipKey == "__blank__",
                        onClick = {
                            selectedChipKey = "__blank__"
                            messageBody = ""
                        },
                        label = { Text("📝 रिकामा", fontSize = 11.sp) }
                    )
                }

                SgsTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    label = "WhatsApp संदेश (बदल करता येईल)",
                    singleLine = false,
                    enableVoice = true,
                    tag = "wa_template_msg_input"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openWhatsApp(context, request.mobile, messageBody)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = DeepNavy),
                modifier = Modifier.testTag("wa_template_send_btn")
            ) {
                Text("💬 WhatsApp पाठवा", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasPhone) {
                    OutlinedButton(
                        onClick = {
                            sendSms(context, request.mobile, messageBody)
                            onDismiss()
                        }
                    ) { Text("✉️ SMS", color = GoldLight, fontSize = 11.5.sp) }
                }
                TextButton(onClick = onDismiss) { Text("बंद करा", color = TextMuted) }
            }
        }
    )
}
