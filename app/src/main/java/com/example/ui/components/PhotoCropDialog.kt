package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.Emerald
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.Navy
import com.example.ui.theme.SoftRed
import com.example.ui.theme.TextCream
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PhotoCropDialog(
    sourceUri: Uri,
    onCropped: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var loadedBmp by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var rotationDeg by remember(sourceUri) { mutableIntStateOf(0) }
    var scale by remember(sourceUri) { mutableFloatStateOf(1f) }
    var offsetX by remember(sourceUri) { mutableFloatStateOf(0f) }
    var offsetY by remember(sourceUri) { mutableFloatStateOf(0f) }
    var isPassportRatio by remember(sourceUri) { mutableStateOf(true) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val targetAspect = if (isPassportRatio) 0.75f else 1f

    LaunchedEffect(sourceUri) {
        loadedBmp = withContext(Dispatchers.IO) {
            runCatching {
                val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(sourceUri)?.use {
                    BitmapFactory.decodeStream(it, null, optsBounds)
                }
                var sample = 1
                while (optsBounds.outWidth / sample > 1200 || optsBounds.outHeight / sample > 1200) {
                    sample *= 2
                }
                val raw = context.contentResolver.openInputStream(sourceUri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                } ?: return@runCatching null

                val exifRot = context.contentResolver.openInputStream(sourceUri)?.use { ins ->
                    when (ExifInterface(ins).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f

                if (exifRot != 0f) {
                    val m = Matrix().apply { postRotate(exifRot) }
                    Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                } else raw
            }.getOrNull()
        }
    }

    val displayBmp = remember(loadedBmp, rotationDeg) {
        val b = loadedBmp ?: return@remember null
        if (rotationDeg % 360 == 0) b else {
            val m = Matrix().apply { postRotate(rotationDeg.toFloat()) }
            Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Navy,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("✂️ पासपोर्ट फोटो Crop व Rotate", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("फोटो झूम/ड्रॅग करा, फिरवा व पासपोर्ट चौकोनात बसवा.", color = TextMuted, fontSize = 11.5.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isPassportRatio,
                        onClick = { isPassportRatio = true; offsetX = 0f; offsetY = 0f },
                        label = { Text("🪪 पासपोर्ट (3:4)", fontSize = 11.5.sp) },
                        modifier = Modifier.testTag("photo_crop_aspect_passport")
                    )
                    FilterChip(
                        selected = !isPassportRatio,
                        onClick = { isPassportRatio = false; offsetX = 0f; offsetY = 0f },
                        label = { Text("⬜ चौरस (1:1)", fontSize = 11.5.sp) },
                        modifier = Modifier.testTag("photo_crop_aspect_square")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isPassportRatio) 0.78f else 1f)
                        .align(Alignment.CenterHorizontally)
                        .aspectRatio(targetAspect)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .onSizeChanged { boxSize = it }
                        .pointerInput(displayBmp) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (displayBmp == null) {
                        CircularProgressIndicator(color = Gold)
                    } else {
                        val imgBmp = remember(displayBmp) { displayBmp.asImageBitmap() }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val baseScale = max(w / displayBmp.width.toFloat(), h / displayBmp.height.toFloat())
                            val drawW = displayBmp.width * baseScale * scale
                            val drawH = displayBmp.height * baseScale * scale
                            val left = (w - drawW) / 2f + offsetX
                            val top = (h - drawH) / 2f + offsetY
                            drawImage(
                                image = imgBmp,
                                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt())
                            )
                            val stroke = Stroke(width = 2f)
                            drawRect(Color.White.copy(alpha = 0.75f), style = stroke)
                            drawLine(Color.White.copy(alpha = 0.25f), Offset(w / 3f, 0f), Offset(w / 3f, h), 1f)
                            drawLine(Color.White.copy(alpha = 0.25f), Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), 1f)
                            drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, h / 3f), Offset(w, h / 3f), 1f)
                            drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), 1f)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("झूम", color = TextCream, fontSize = 12.sp)
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..4f,
                        colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { rotationDeg = (rotationDeg + 270) % 360; offsetX = 0f; offsetY = 0f },
                        modifier = Modifier.weight(1f).testTag("photo_crop_rotate_left_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "डावीकडे ९०°", tint = GoldLight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(3.dp))
                        Text("-90°", color = GoldLight, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { rotationDeg = (rotationDeg + 90) % 360; offsetX = 0f; offsetY = 0f },
                        modifier = Modifier.weight(1f).testTag("photo_crop_rotate_right_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "उजवीकडे ९०°", tint = GoldLight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(3.dp))
                        Text("+90°", color = GoldLight, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { rotationDeg = 0; scale = 1f; offsetX = 0f; offsetY = 0f },
                        modifier = Modifier.weight(1f).testTag("photo_crop_reset_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "मूळ स्थिती", tint = TextCream, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(3.dp))
                        Text("Reset", color = TextCream, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = SoftRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("रद्द", color = SoftRed)
                    }
                    Button(
                        onClick = {
                            val bmp = displayBmp ?: return@Button
                            val outW = if (isPassportRatio) 360 else 400
                            val outH = if (isPassportRatio) 480 else 400
                            val boxW = boxSize.width.coerceAtLeast(outW).toFloat()
                            val boxH = boxSize.height.coerceAtLeast(outH).toFloat()
                            val outBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(outBmp)
                            canvas.drawColor(android.graphics.Color.WHITE)

                            val baseScale = max(boxW / bmp.width.toFloat(), boxH / bmp.height.toFloat())
                            val drawW = bmp.width * baseScale * scale
                            val drawH = bmp.height * baseScale * scale
                            val left = (boxW - drawW) / 2f + offsetX
                            val top = (boxH - drawH) / 2f + offsetY
                            val ratioX = outW / boxW
                            val ratioY = outH / boxH

                            val matrix = Matrix().apply {
                                postScale((drawW / bmp.width) * ratioX, (drawH / bmp.height) * ratioY)
                                postTranslate(left * ratioX, top * ratioY)
                            }
                            canvas.drawBitmap(bmp, matrix, Paint(Paint.FILTER_BITMAP_FLAG))

                            val file = File(context.cacheDir, "cropped_student_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(file).use { outBmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                            onCropped(Uri.fromFile(file))
                        },
                        enabled = displayBmp != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        modifier = Modifier.weight(1.4f).testTag("photo_crop_confirm_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Crop व Upload", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
