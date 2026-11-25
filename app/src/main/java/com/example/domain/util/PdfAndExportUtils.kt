package com.example.domain.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.model.AttendanceCertFormData
import com.example.domain.model.BonafideFormData
import com.example.domain.model.DiaryEntry
import com.example.domain.model.LcFormData
import com.example.domain.model.ReportClassGroup
import com.example.domain.model.Student
import com.example.domain.model.TransportInfo
import com.example.ui.viewmodel.ProfileActionLogItem
import com.example.ui.viewmodel.ProfileAuditItem
import com.example.ui.viewmodel.ProfilePromoLogItem
import com.example.ui.viewmodel.ProfileYearBlock
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfAndExportUtils {

    @Volatile
    private var cachedRegularTypeface: Typeface? = null

    @Volatile
    private var cachedBoldTypeface: Typeface? = null

    private fun getPdfTypeface(context: Context, bold: Boolean = false): Typeface {
        val reg = cachedRegularTypeface ?: synchronized(this) {
            cachedRegularTypeface ?: run {
                val assetFont = runCatching {
                    val list = context.assets.list("fonts")?.toSet().orEmpty()
                    when {
                        "kokila.ttf" in list -> Typeface.createFromAsset(context.assets, "fonts/kokila.ttf")
                        "Kokila.ttf" in list -> Typeface.createFromAsset(context.assets, "fonts/Kokila.ttf")
                        else -> null
                    }
                }.getOrNull()
                assetFont
                    ?: ResourcesCompat.getFont(context, R.font.tiro_devanagari_marathi)
                    ?: ResourcesCompat.getFont(context, R.font.noto_serif_devanagari)
                    ?: Typeface.SERIF
            }.also { cachedRegularTypeface = it }
        }
        if (!bold) return reg
        return cachedBoldTypeface ?: synchronized(this) {
            cachedBoldTypeface ?: run {
                val boldAsset = runCatching {
                    val list = context.assets.list("fonts")?.toSet().orEmpty()
                    when {
                        "kokilab.ttf" in list -> Typeface.createFromAsset(context.assets, "fonts/kokilab.ttf")
                        "Kokilab.ttf" in list -> Typeface.createFromAsset(context.assets, "fonts/Kokilab.ttf")
                        else -> null
                    }
                }.getOrNull()
                boldAsset ?: Typeface.create(reg, Typeface.BOLD)
            }.also { cachedBoldTypeface = it }
        }
    }

    private fun makeTextPaint(
        context: Context,
        colorInt: Int,
        size: Float,
        bold: Boolean = false
    ): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        textSize = size
        typeface = getPdfTypeface(context, bold)
        isFakeBoldText = bold
    }

    private fun exportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateQrBitmap(content: String, sizePx: Int = 240): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        pm.print(jobName, adapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
    }

    private fun drawStaticText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(align)
            .setLineSpacing(2f, 1.15f)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }

    fun renderBonafidePdf(
        context: Context,
        data: BonafideFormData,
        verifyBaseUrl: String,
        twoUp: Boolean = false
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas
        c.drawColor(Color.WHITE)

        val qrUrl = if (data.certNo.isNotBlank()) {
            "$verifyBaseUrl?action=verify&type=BF&serial=${java.net.URLEncoder.encode(data.certNo, "UTF-8")}"
        } else ""
        val qrBmp = if (qrUrl.isNotBlank()) generateQrBitmap(qrUrl, 130) else null

        drawBonafideHalf(context, c, data, qrBmp, offsetY = 24f)
        if (twoUp) {
            val dashPaint = Paint().apply {
                color = Color.GRAY
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
            }
            c.drawLine(24f, 421f, 571f, 421f, dashPaint)
            drawBonafideHalf(context, c, data, qrBmp, offsetY = 442f)
        }
        doc.finishPage(page)

        val safeReg = data.regNo.ifBlank { "student" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val outFile = File(exportDir(context), "Bonafide_${safeReg}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun renderBatchBonafidePdf(
        context: Context,
        students: List<Student>,
        verifyBaseUrl: String
    ): File {
        val doc = PdfDocument()
        val dashPaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }
        val chunks = students.chunked(2)
        chunks.forEachIndexed { idx, pair ->
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, idx + 1).create()
            val page = doc.startPage(pageInfo)
            val c = page.canvas
            c.drawColor(Color.WHITE)
            pair.forEachIndexed { pos, s ->
                val bf = BonafideFormData(
                    regNo = s.regNo,
                    studentId = s.studentId,
                    fullName = s.fullName,
                    gender = s.gender,
                    dob = s.dob,
                    dobWords = s.dobWords.ifBlank { MarathiUtils.dobToMarathiWords(s.dob) },
                    iyatta = s.iyatta,
                    tukdi = s.tukdi,
                    birthPlace = s.birthVillage,
                    caste = s.caste,
                    religion = s.religion,
                    nationality = s.nationality,
                    acYear = s.acYear,
                    bfDate = MarathiUtils.todayIso(),
                    pen = s.pen
                )
                drawBonafideHalf(context, c, bf, null, offsetY = if (pos == 0) 24f else 442f)
                if (pos == 0 && pair.size > 1) {
                    c.drawLine(24f, 421f, 571f, 421f, dashPaint)
                }
            }
            doc.finishPage(page)
        }
        val outFile = File(exportDir(context), "Batch_Bonafide_${students.size}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun renderBatchAttendancePdf(
        context: Context,
        students: List<Student>,
        verifyBaseUrl: String
    ): File {
        val doc = PdfDocument()
        val dashPaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }
        val chunks = students.chunked(2)
        chunks.forEachIndexed { idx, pair ->
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, idx + 1).create()
            val page = doc.startPage(pageInfo)
            val c = page.canvas
            c.drawColor(Color.WHITE)
            pair.forEachIndexed { pos, s ->
                val at = AttendanceCertFormData(
                    regNo = s.regNo,
                    studentId = s.studentId,
                    fullName = s.fullName,
                    gender = s.gender,
                    dob = s.dob,
                    iyatta = s.iyatta,
                    tukdi = s.tukdi,
                    acYear = s.acYear,
                    atDate = MarathiUtils.todayIso(),
                    pen = s.pen
                )
                drawAttendanceHalf(context, c, at, null, offsetY = if (pos == 0) 24f else 442f)
                if (pos == 0 && pair.size > 1) {
                    c.drawLine(24f, 421f, 571f, 421f, dashPaint)
                }
            }
            doc.finishPage(page)
        }
        val outFile = File(exportDir(context), "Batch_Attendance_${students.size}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    private fun drawBonafideHalf(context: Context, c: Canvas, d: BonafideFormData, qrBmp: Bitmap?, offsetY: Float) {
        val isEn = d.lang.equals("en", ignoreCase = true)
        val isSaved = d.certNo.isNotBlank()

        if (!isSaved) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(26, 180, 0, 0)
                textSize = 72f
                typeface = getPdfTypeface(context, bold = true)
                textAlign = Paint.Align.CENTER
            }
            c.save()
            c.rotate(-25f, 297f, offsetY + 190f)
            c.drawText("DRAFT", 297f, offsetY + 205f, wmPaint)
            c.restore()
        }

        if (qrBmp != null) {
            c.drawBitmap(qrBmp, null, RectF(500f, offsetY + 6f, 560f, offsetY + 66f), null)
        }

        val smallPaint = makeTextPaint(context, Color.DKGRAY, 9.5f)
        val titlePaint = makeTextPaint(context, Color.BLACK, 16f, bold = true)
        val bodyPaint = makeTextPaint(context, Color.BLACK, 11.5f)
        val boldPaint = makeTextPaint(context, Color.BLACK, 11.5f, bold = true)

        var y = offsetY + 10f
        y = drawStaticText(
            c,
            if (isEn) "Pachora Taluka Sahakari Shikshan Sanstha, Run by" else "पाचोरा तालुका सहकारी शिक्षण संस्था संचलित,",
            smallPaint, 36f, y, 455, Layout.Alignment.ALIGN_CENTER
        )
        y = drawStaticText(
            c,
            if (isEn) "SHRI GOVINDRAM SEKSARIA HIGH SCHOOL, PACHORA" else "श्री.गोविंदराम सेक्सरिया हायस्कूल, पाचोरा",
            titlePaint, 36f, y + 2f, 455, Layout.Alignment.ALIGN_CENTER
        )
        y = drawStaticText(c, "UDISE - 27031201718", boldPaint, 36f, y + 2f, 455, Layout.Alignment.ALIGN_CENTER)

        val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
        c.drawLine(36f, y + 6f, 559f, y + 6f, linePaint)
        y += 14f

        val dateTxt = MarathiUtils.fmtDate(d.bfDate).ifBlank { MarathiUtils.todayDisplay() }
        val leftMeta = buildString {
            append(if (isEn) "Reg. No. ${d.regNo}" else "रजि. क्र. ${d.regNo}")
            if (d.pen.isNotBlank()) append("\nPEN : ${d.pen}")
        }
        val rightMeta = buildString {
            append(if (isEn) "Date : $dateTxt" else "दिनांक : $dateTxt")
            if (isSaved) append(if (isEn) "\nBonafide No. ${d.certNo}" else "\nबोनाफाईड क्र. ${d.certNo}")
        }
        drawStaticText(c, leftMeta, bodyPaint, 36f, y, 250)
        val yRight = drawStaticText(c, rightMeta, bodyPaint, 310f, y, 249, Layout.Alignment.ALIGN_OPPOSITE)
        y = maxOf(y + 28f, yRight + 6f)

        val certHead = if (isEn) "BONAFIDE CERTIFICATE" else "बोनाफाईड सर्टिफिकेट"
        y = drawStaticText(c, certHead, titlePaint.apply { textSize = 13f }, 36f, y + 2f, 523, Layout.Alignment.ALIGN_CENTER) + 8f

        val isGirl = d.gender.equals("Female", ignoreCase = true)
        val iyMr = MarathiUtils.classToMr(d.iyatta)
        val dobFmt = MarathiUtils.fmtDate(d.dob)
        val casteStr = listOf(d.religion, d.caste).filter { it.isNotBlank() }.joinToString(" ")

        val p1: String
        val p2: String
        val p3: String
        if (isEn) {
            val tEn = if (isGirl) "Miss" else "Mr."
            val pr = if (isGirl) "she" else "he"
            val poss = if (isGirl) "her" else "his"
            val possCap = if (isGirl) "Her" else "His"
            p1 = "This is to certify that $tEn ${d.fullName} is a bonafide student of this school and is studying in Std. $iyMr, Division ${d.tukdi.ifBlank { "_" }} during the academic year ${d.acYear.ifBlank { "___" }}."
            p2 = "As per school records, $pr belongs to ${casteStr.ifBlank { "___" }} community, and $poss date of birth is ${dobFmt.ifBlank { "___" }} (${d.dobWords.ifBlank { "___" }}). Place of birth as per records is ${d.birthPlace.ifBlank { "___" }}."
            p3 = "$possCap conduct during the period of study has been good. This certificate is issued based on school records and on the applicant's request."
        } else {
            val tMr = if (isGirl) "कुमारी" else "कुमार"
            val ti = if (isGirl) "ती" else "तो"
            val tya = if (isGirl) "तिची" else "त्याची"
            val tyas = if (isGirl) "तिची" else "त्याची"
            p1 = "$tMr ${d.fullName} यांना सदर बोनाफाईड सर्टिफिकेट देण्यात येते की, आमच्या शाळेतील विद्यार्थी नामे ${d.fullName} $ti विद्यार्थी शैक्षणिक वर्ष सन - ${d.acYear.ifBlank { "___" }} मध्ये इयत्ता - $iyMr तुकडी - ${d.tukdi.ifBlank { "_" }} मध्ये शिकत आहे."
            p2 = "शाळेतील दफ्तरी नोंदीप्रमाणे $tya जात ${casteStr.ifBlank { "___" }} असून $tya जन्म तारीख ${dobFmt.ifBlank { "___" }} म्हणजेच अक्षरी ${d.dobWords.ifBlank { "___" }} अशी आहे. तसेच जन्मठिकाण ${d.birthPlace.ifBlank { "___" }} हे आहे."
            p3 = "सदर विद्यार्थ्याच्या शाळेतील कालावधीमध्ये $tyas वर्तणूक चांगली होती. सदर दाखला शालेय दफ्तरावरून व अर्जदाराच्या विनंतीनुसार देण्यात आला आहे."
        }

        y = drawStaticText(c, p1, bodyPaint, 36f, y, 523) + 6f
        y = drawStaticText(c, p2, bodyPaint, 36f, y, 523) + 6f
        y = drawStaticText(c, p3, bodyPaint, 36f, y, 523) + 8f

        if (d.purpose.isNotBlank()) {
            val lbl = if (isEn) "Purpose : ${d.purpose}" else "उद्देश : ${d.purpose}"
            y = drawStaticText(c, lbl, boldPaint, 36f, y, 523) + 4f
        }
        if (d.remarks.isNotBlank()) {
            val lbl = if (isEn) "Remarks : ${d.remarks}" else "शेरा : ${d.remarks}"
            y = drawStaticText(c, lbl, bodyPaint, 36f, y, 523) + 4f
        }

        val footY = maxOf(y + 18f, offsetY + 325f)
        drawStaticText(
            c,
            if (isEn) "Place – Pachora\nDate – ${MarathiUtils.todayDisplay()}" else "स्थळ – पाचोरा\nदिनांक – ${MarathiUtils.todayDisplay()}",
            boldPaint, 36f, footY, 200
        )
        c.drawLine(390f, footY + 12f, 555f, footY + 12f, linePaint)
        drawStaticText(
            c,
            if (isEn) "Headmaster/Principal" else "मुख्याध्यापक/प्राचार्य",
            smallPaint, 390f, footY + 15f, 165, Layout.Alignment.ALIGN_CENTER
        )
    }

    fun renderAttendanceCertPdf(
        context: Context,
        data: AttendanceCertFormData,
        verifyBaseUrl: String,
        twoUp: Boolean = false
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas
        c.drawColor(Color.WHITE)

        val qrUrl = if (data.certNo.isNotBlank()) {
            "$verifyBaseUrl?action=verify&type=AT&serial=${java.net.URLEncoder.encode(data.certNo, "UTF-8")}"
        } else ""
        val qrBmp = if (qrUrl.isNotBlank()) generateQrBitmap(qrUrl, 130) else null

        drawAttendanceHalf(context, c, data, qrBmp, offsetY = 24f)
        if (twoUp) {
            val dashPaint = Paint().apply {
                color = Color.GRAY
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
            }
            c.drawLine(24f, 421f, 571f, 421f, dashPaint)
            drawAttendanceHalf(context, c, data, qrBmp, offsetY = 442f)
        }
        doc.finishPage(page)

        val safeReg = data.regNo.ifBlank { "student" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val outFile = File(exportDir(context), "AttendanceCert_${safeReg}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    private fun drawAttendanceHalf(context: Context, c: Canvas, d: AttendanceCertFormData, qrBmp: Bitmap?, offsetY: Float) {
        val isEn = d.lang.equals("en", ignoreCase = true)
        val isSaved = d.certNo.isNotBlank()

        if (!isSaved) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(26, 180, 0, 0)
                textSize = 72f
                typeface = getPdfTypeface(context, bold = true)
                textAlign = Paint.Align.CENTER
            }
            c.save()
            c.rotate(-25f, 297f, offsetY + 190f)
            c.drawText("DRAFT", 297f, offsetY + 205f, wmPaint)
            c.restore()
        }
        if (qrBmp != null) {
            c.drawBitmap(qrBmp, null, RectF(500f, offsetY + 6f, 560f, offsetY + 66f), null)
        }

        val smallPaint = makeTextPaint(context, Color.DKGRAY, 9.5f)
        val titlePaint = makeTextPaint(context, Color.BLACK, 16f, bold = true)
        val bodyPaint = makeTextPaint(context, Color.BLACK, 11.5f)
        val boldPaint = makeTextPaint(context, Color.BLACK, 11.5f, bold = true)

        var y = offsetY + 10f
        y = drawStaticText(
            c,
            if (isEn) "Pachora Taluka Sahakari Shikshan Sanstha, Run by" else "पाचोरा तालुका सहकारी शिक्षण संस्था संचलित,",
            smallPaint, 36f, y, 455, Layout.Alignment.ALIGN_CENTER
        )
        y = drawStaticText(
            c,
            if (isEn) "SHRI GOVINDRAM SEKSARIA HIGH SCHOOL, PACHORA" else "श्री.गोविंदराम सेक्सरिया हायस्कूल, पाचोरा",
            titlePaint, 36f, y + 2f, 455, Layout.Alignment.ALIGN_CENTER
        )
        y = drawStaticText(c, "UDISE - 27031201718", boldPaint, 36f, y + 2f, 455, Layout.Alignment.ALIGN_CENTER)

        val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
        c.drawLine(36f, y + 6f, 559f, y + 6f, linePaint)
        y += 14f

        val dateTxt = MarathiUtils.fmtDate(d.atDate).ifBlank { MarathiUtils.todayDisplay() }
        val leftMeta = buildString {
            append(if (isEn) "Reg. No. ${d.regNo}" else "रजि. क्र. ${d.regNo}")
            if (d.pen.isNotBlank()) append("\nPEN : ${d.pen}")
        }
        val rightMeta = buildString {
            append(if (isEn) "Date : $dateTxt" else "दिनांक : $dateTxt")
            if (isSaved) append(if (isEn) "\nAttendance No. ${d.certNo}" else "\nउपस्थिती क्र. ${d.certNo}")
        }
        drawStaticText(c, leftMeta, bodyPaint, 36f, y, 250)
        val yRight = drawStaticText(c, rightMeta, bodyPaint, 310f, y, 249, Layout.Alignment.ALIGN_OPPOSITE)
        y = maxOf(y + 28f, yRight + 6f)

        val certHead = if (isEn) "ATTENDANCE CERTIFICATE" else "उपस्थिती दाखला"
        y = drawStaticText(c, certHead, titlePaint.apply { textSize = 13f }, 36f, y + 2f, 523, Layout.Alignment.ALIGN_CENTER) + 8f

        val isGirl = d.gender.equals("Female", ignoreCase = true)
        val iyMr = MarathiUtils.classToMr(d.iyatta)
        val fromFmt = MarathiUtils.fmtDate(d.fromDate)
        val toFmt = MarathiUtils.fmtDate(d.toDate)

        val p1: String
        val p2: String
        if (isEn) {
            val tEn = if (isGirl) "Miss" else "Mr."
            val pr = if (isGirl) "she" else "he"
            p1 = "This is to certify that $tEn ${d.fullName}, a student of this school, is studying in Std. $iyMr, Division ${d.tukdi.ifBlank { "_" }} during the academic year ${d.acYear.ifBlank { "___" }}."
            p2 = buildString {
                if (fromFmt.isNotBlank() && toFmt.isNotBlank()) append("During the period from $fromFmt to $toFmt, ")
                if (d.totalDays.isNotBlank()) append("out of ${d.totalDays} total working days, ")
                if (d.presentDays.isNotBlank()) append("$pr was present for ${d.presentDays} days. ")
                if (d.pct.isNotBlank()) append("Overall attendance was ${d.pct}. ")
                append("This certificate is issued at the request of the parent/guardian.")
            }
        } else {
            val tMr = if (isGirl) "कुमारी" else "कुमार"
            val ti = if (isGirl) "ती" else "तो"
            val tya = if (isGirl) "तिची" else "त्याची"
            p1 = "$tMr ${d.fullName} यांना सदर उपस्थिती दाखला देण्यात येतो की, आमच्या शाळेतील विद्यार्थी नामे ${d.fullName} $ti विद्यार्थी शैक्षणिक वर्ष सन - ${d.acYear.ifBlank { "___" }} मध्ये इयत्ता - $iyMr तुकडी - ${d.tukdi.ifBlank { "_" }} मध्ये शिकत आहे."
            p2 = buildString {
                if (fromFmt.isNotBlank() && toFmt.isNotBlank()) append("कालावधी $fromFmt ते $toFmt मध्ये ")
                if (d.totalDays.isNotBlank()) append("एकूण कामकाजाचे दिवस ${d.totalDays} पैकी ")
                if (d.presentDays.isNotBlank()) append("$tya उपस्थित दिवस ${d.presentDays} होते. ")
                if (d.pct.isNotBlank()) append("तरी $tya उपस्थिती ही एकूण ${d.pct} होती. ")
                append("सदर दाखला हा पालकांच्या विनंती नुसार देण्यात आला आहे.")
            }
        }

        y = drawStaticText(c, p1, bodyPaint, 36f, y, 523) + 8f
        y = drawStaticText(c, p2, bodyPaint, 36f, y, 523) + 10f

        if (d.purpose.isNotBlank()) {
            val lbl = if (isEn) "Purpose : ${d.purpose}" else "उद्देश : ${d.purpose}"
            y = drawStaticText(c, lbl, boldPaint, 36f, y, 523) + 4f
        }
        if (d.remarks.isNotBlank()) {
            val lbl = if (isEn) "Remarks : ${d.remarks}" else "शेरा : ${d.remarks}"
            y = drawStaticText(c, lbl, bodyPaint, 36f, y, 523) + 4f
        }

        val footY = maxOf(y + 18f, offsetY + 325f)
        drawStaticText(
            c,
            if (isEn) "Place – Pachora" else "स्थळ – पाचोरा",
            boldPaint, 36f, footY, 200
        )
        c.drawLine(390f, footY + 12f, 555f, footY + 12f, linePaint)
        drawStaticText(
            c,
            if (isEn) "Headmaster/Principal" else "मुख्याध्यापक/प्राचार्य",
            smallPaint, 390f, footY + 15f, 165, Layout.Alignment.ALIGN_CENTER
        )
    }

    fun renderLcPdf(context: Context, d: LcFormData): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas
        c.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.rgb(26, 42, 74)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        c.drawRect(22f, 22f, 573f, 820f, borderPaint)
        borderPaint.strokeWidth = 0.7f
        c.drawRect(26f, 26f, 569f, 816f, borderPaint)

        val smallPaint = makeTextPaint(context, Color.DKGRAY, 8.5f)
        val headPaint = makeTextPaint(context, Color.BLACK, 15f, bold = true)
        val boldPaint = makeTextPaint(context, Color.BLACK, 10.5f, bold = true)
        val bodyPaint = makeTextPaint(context, Color.BLACK, 10.5f)

        var y = 34f
        y = drawStaticText(c, "पाचोरा तालुका सहकारी शिक्षण संस्था संचलित,", smallPaint, 34f, y, 527, Layout.Alignment.ALIGN_CENTER)
        y = drawStaticText(c, "श्री.गोविंदराम सेक्सरिया हायस्कूल, पाचोरा जि.जळगाव", headPaint, 34f, y + 1f, 527, Layout.Alignment.ALIGN_CENTER)
        y = drawStaticText(
            c,
            "शाळा मान्यता क्र. GAC/4/Recogn/(B.E.) 27/29 Nov.1960  |  युडायस: 27031201718  |  संलग्नता: एस.15.13.001\nई-मेल: sgspachora@gmail.com  |  माध्यम: मराठी  |  फोन: (02596) 245481",
            smallPaint, 34f, y + 3f, 527, Layout.Alignment.ALIGN_CENTER
        )
        y += 6f
        val lcType = if ((d.lcCount.toIntOrNull() ?: 1) > 1) "दुय्यमप्रत" else "मुळप्रत"
        drawStaticText(c, "लि.स.क्र. ${d.lcNo}", boldPaint, 36f, y, 250)
        drawStaticText(c, "जनरल रजि. क्र. ${d.regNo}", boldPaint, 310f, y, 249, Layout.Alignment.ALIGN_OPPOSITE)
        y += 18f

        val boxPaint = Paint().apply { color = Color.rgb(26, 42, 74); style = Paint.Style.STROKE; strokeWidth = 1.4f }
        c.drawRect(195f, y, 400f, y + 24f, boxPaint)
        drawStaticText(c, "शाळा सोडल्याचा दाखला ($lcType)", boldPaint, 195f, y + 5f, 205, Layout.Alignment.ALIGN_CENTER)
        y += 32f

        val leaveRaw = d.stxt68.ifBlank { d.leaveDate }
        val leaveParts = leaveRaw.split("-")
        val lYear = leaveParts.getOrNull(0).orEmpty()
        val lMonthMr = MarathiUtils.monthNameMr(leaveParts.getOrNull(1)?.toIntOrNull() ?: 0)
        val leaveFromTxt = listOf(lMonthMr, lYear).filter { it.isNotBlank() }.joinToString(" ")

        val rows = listOf(
            "स्टुडंट आय.डी. / PEN" to "${d.studentId.ifBlank { d.regNo }}  |  PEN: ${d.pen}",
            "आधार कार्ड क्र. (UID)" to d.aadhar,
            "विद्यार्थ्याचे संपूर्ण नांव" to d.fullName,
            "आईचे नांव" to d.motherName,
            "राष्ट्रीयत्व व मातृभाषा" to "${d.nationality.ifBlank { "भारतीय" }}  |  मातृभाषा: ${d.motherTongue.ifBlank { "मराठी" }}",
            "धर्म, जात - पोटजात" to listOf(d.religion, d.caste, d.subcaste).filter { it.isNotBlank() }.joinToString(" "),
            "जन्मस्थळ" to d.birthPlace,
            "इ.सनाप्रमाणे जन्म दिनांक" to "${MarathiUtils.fmtDate(d.dob)}\n(अक्षरी: ${d.dobWords})",
            "या पूर्वीची शाळा" to d.prevSchool,
            "प्रवेश दिनांक व इयत्ता" to "${MarathiUtils.fmtDate(d.admissionDate)}  |  इयत्ता: ${MarathiUtils.classToMr(d.admissionClass)}",
            "प्रगती व वर्तणूक" to "प्रगती: ${d.progress}  |  वर्तणूक: ${d.conduct}",
            "शाळा सोडल्याची दिनांक" to MarathiUtils.fmtDate(d.leaveDate),
            "शिकत असलेली इयत्ता व केव्हापासून" to "इयत्ता ${MarathiUtils.classToMr(d.leaveClass)} त $leaveFromTxt पासून (${MarathiUtils.classWords(d.leaveClass)})",
            "शाळा सोडल्याचे कारण" to d.remarks,
            "शेरा" to "सदर दाखल्यात कोणतीही खाडा-खोड नाही."
        )

        val rowPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.6f }
        for ((lbl, valTxt) in rows) {
            val h1 = drawStaticText(c, lbl, boldPaint, 36f, y + 4f, 185)
            val h2 = drawStaticText(c, valTxt.ifBlank { "—" }, bodyPaint, 232f, y + 4f, 325)
            val nextY = maxOf(h1, h2) + 5f
            c.drawLine(34f, nextY, 561f, nextY, rowPaint)
            y = nextY + 2f
            if (y > 725f) break
        }

        y = maxOf(y + 8f, 725f)
        drawStaticText(
            c,
            "दाखला देण्यात येतो की, वरील माहिती शाळेतील जनरल रजि. बुक नं. ${d.bookNo.ifBlank { "___" }} वरील रजि. क्र. ${d.regNo.ifBlank { "___" }} प्रमाणे आहे.",
            smallPaint, 36f, y, 523
        )
        val footY = 770f
        drawStaticText(c, "दिनांक - ${MarathiUtils.todayDisplay()}\nस्थळ – पाचोरा", boldPaint, 36f, footY, 170)
        c.drawLine(245f, footY + 12f, 360f, footY + 12f, Paint().apply { color = Color.BLACK; strokeWidth = 1f })
        drawStaticText(c, "लेखनिक", smallPaint, 245f, footY + 16f, 115, Layout.Alignment.ALIGN_CENTER)
        c.drawLine(415f, footY + 12f, 550f, footY + 12f, Paint().apply { color = Color.BLACK; strokeWidth = 1f })
        drawStaticText(c, "मुख्याध्यापक / स्वाक्षरी व शिक्का", smallPaint, 415f, footY + 16f, 135, Layout.Alignment.ALIGN_CENTER)

        doc.finishPage(page)
        val safeReg = d.regNo.ifBlank { "lc" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val outFile = File(exportDir(context), "LC_${safeReg}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun renderStudentProfilePdf(context: Context, s: Student): File {
        val headers = listOf("तपशील", "माहिती")
        val rows = listOf(
            listOf("नोंद क्र. (Reg No)", s.regNo),
            listOf("Student ID / PEN", "${s.studentId} / ${s.pen}"),
            listOf("संपूर्ण नाव", s.fullName),
            listOf("आईचे नाव", s.motherName),
            listOf("इयत्ता व तुकडी", "${MarathiUtils.classToMr(s.iyatta)} ${s.tukdi} (Roll: ${s.rollNo})"),
            listOf("शैक्षणिक वर्ष", s.acYear),
            listOf("लिंग", s.gender),
            listOf("जन्मतारीख", "${MarathiUtils.fmtDate(s.dob)} (${s.dobWords})"),
            listOf("आधार क्र.", s.aadhar),
            listOf("धर्म / जात / प्रवर्ग", "${s.religion} / ${s.caste} / ${s.category}"),
            listOf("जन्मस्थळ", s.birthVillage),
            listOf("पूर्वीची शाळा", s.prevSchool),
            listOf("प्रवेश दिनांक / वर्ग", "${MarathiUtils.fmtDate(s.admissionDate)} / ${s.admissionClass}"),
            listOf("WhatsApp / मोबाईल", "${s.whatsappMobile} ${s.alternateMobile}".trim()),
            listOf("पत्ता", s.address)
        )
        return renderTablePdf(
            context = context,
            title = "विद्यार्थी माहिती पत्रक — ${s.fullName}",
            subtitle = "रजि. क्र. ${s.regNo} | इयत्ता ${MarathiUtils.classToMr(s.iyatta)} ${s.tukdi} | दिनांक: ${MarathiUtils.todayDisplay()}",
            headers = headers,
            rows = rows,
            fileName = "Student_${s.regNo.ifBlank { "profile" }}.pdf",
            landscape = false
        )
    }

    fun renderTablePdf(
        context: Context,
        title: String,
        subtitle: String,
        headers: List<String>,
        rows: List<List<String>>,
        fileName: String,
        landscape: Boolean = false
    ): File {
        val doc = PdfDocument()
        val pageW = if (landscape) 842 else 595
        val pageH = if (landscape) 595 else 842
        val margin = 28f
        val usableW = pageW - 2 * margin
        val colCount = headers.size.coerceAtLeast(1)
        val colW = usableW / colCount

        val schoolPaint = makeTextPaint(context, Color.rgb(26, 42, 74), 14f, bold = true)
        val titlePaint = makeTextPaint(context, Color.BLACK, 11.5f, bold = true)
        val subPaint = makeTextPaint(context, Color.DKGRAY, 8.5f)
        val thPaint = makeTextPaint(context, Color.WHITE, 8.5f, bold = true)
        val tdPaint = makeTextPaint(context, Color.BLACK, 8.2f)
        val thBg = Paint().apply { color = Color.rgb(26, 42, 74) }
        val altBg = Paint().apply { color = Color.rgb(245, 247, 250) }
        val borderP = Paint().apply { color = Color.rgb(190, 195, 205); style = Paint.Style.STROKE; strokeWidth = 0.6f }

        var pageNum = 1
        var rowIdx = 0
        do {
            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
            val page = doc.startPage(pageInfo)
            val c = page.canvas
            c.drawColor(Color.WHITE)

            var y = margin
            y = drawStaticText(c, "श्री.गोविंदराम सेक्सरिया हायस्कूल, पाचोरा", schoolPaint, margin, y, usableW.toInt(), Layout.Alignment.ALIGN_CENTER)
            y = drawStaticText(c, title, titlePaint, margin, y + 2f, usableW.toInt(), Layout.Alignment.ALIGN_CENTER)
            y = drawStaticText(c, "$subtitle  |  पान $pageNum", subPaint, margin, y + 2f, usableW.toInt(), Layout.Alignment.ALIGN_CENTER) + 8f

            // Header row
            val hH = 22f
            c.drawRect(margin, y, margin + usableW, y + hH, thBg)
            headers.forEachIndexed { idx, h ->
                drawStaticText(c, h, thPaint, margin + idx * colW + 3f, y + 5f, (colW - 6f).toInt())
            }
            y += hH

            while (rowIdx < rows.size && y < pageH - margin - 38f) {
                val r = rows[rowIdx]
                var maxH = 18f
                val layouts = r.take(colCount).map { cell ->
                    StaticLayout.Builder.obtain(cell, 0, cell.length, tdPaint, (colW - 6f).toInt().coerceAtLeast(12))
                        .setLineSpacing(1f, 1.05f)
                        .build()
                }
                layouts.forEach { if (it.height + 8f > maxH) maxH = it.height + 8f }
                if (y + maxH > pageH - margin - 20f) break

                if (rowIdx % 2 == 1) {
                    c.drawRect(margin, y, margin + usableW, y + maxH, altBg)
                }
                c.drawRect(margin, y, margin + usableW, y + maxH, borderP)
                layouts.forEachIndexed { idx, lay ->
                    c.save()
                    c.translate(margin + idx * colW + 3f, y + 4f)
                    lay.draw(c)
                    c.restore()
                    if (idx > 0) {
                        c.drawLine(margin + idx * colW, y, margin + idx * colW, y + maxH, borderP)
                    }
                }
                y += maxH
                rowIdx++
            }
            c.drawLine(margin, pageH - margin - 14f, margin + usableW, pageH - margin - 14f, borderP)
            drawStaticText(
                c,
                "Shri. Govindrao Sakseria High School, Pachora | Software Developed by Tonape sir (SGS Highschool, Pachora)",
                subPaint,
                margin,
                pageH - margin - 11f,
                usableW.toInt(),
                Layout.Alignment.ALIGN_CENTER
            )
            doc.finishPage(page)
            pageNum++
        } while (rowIdx < rows.size)

        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(exportDir(context), safeName)
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun renderGroupedReportPdf(
        context: Context,
        purpose: String,
        groups: List<ReportClassGroup>,
        headers: List<String>,
        fileName: String,
        landscape: Boolean = false
    ): File {
        val doc = PdfDocument()
        val pageW = if (landscape) 842 else 595
        val pageH = if (landscape) 595 else 842
        val margin = 28f
        val usableW = pageW - 2 * margin
        val colCount = headers.size.coerceAtLeast(1)
        val colW = usableW / colCount

        val schoolPaint = makeTextPaint(context, Color.rgb(26, 42, 74), 14f, bold = true)
        val titlePaint = makeTextPaint(context, Color.BLACK, 11.5f, bold = true)
        val groupPaint = makeTextPaint(context, Color.rgb(122, 74, 26), 10.5f, bold = true)
        val subPaint = makeTextPaint(context, Color.DKGRAY, 8.2f)
        val thPaint = makeTextPaint(context, Color.WHITE, 8.5f, bold = true)
        val tdPaint = makeTextPaint(context, Color.BLACK, 8.2f)
        val thBg = Paint().apply { color = Color.rgb(26, 42, 74) }
        val altBg = Paint().apply { color = Color.rgb(245, 247, 250) }
        val borderP = Paint().apply { color = Color.rgb(190, 195, 205); style = Paint.Style.STROKE; strokeWidth = 0.6f }

        val today = MarathiUtils.todayDisplay()
        var globalPageNum = 1

        groups.forEach { grp ->
            val groupLabel = "वर्ग: ${MarathiUtils.classToMr(grp.iyatta).ifBlank { "—" }}" +
                if (grp.tukdi.isNotBlank()) " — तुकडी: ${grp.tukdi}" else ""
            var rowIdx = 0
            var groupPageNum = 1
            do {
                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, globalPageNum).create()
                val page = doc.startPage(pageInfo)
                val c = page.canvas
                c.drawColor(Color.WHITE)

                var y = margin
                y = drawStaticText(c, "श्री. गो. से. हायस्कूल, पाचोरा", schoolPaint, margin, y, usableW.toInt(), Layout.Alignment.ALIGN_CENTER)
                y = drawStaticText(c, purpose.ifBlank { "विद्यार्थी यादी" }, titlePaint, margin, y + 2f, usableW.toInt(), Layout.Alignment.ALIGN_CENTER)
                y = drawStaticText(c, groupLabel, groupPaint, margin, y + 2f, usableW.toInt(), Layout.Alignment.ALIGN_CENTER)
                y = drawStaticText(
                    c,
                    "दिनांक: $today  |  पान $groupPageNum (याच वर्गाचे)  |  या वर्गात एकूण: ${grp.rows.size}",
                    subPaint, margin, y + 2f, usableW.toInt(), Layout.Alignment.ALIGN_CENTER
                ) + 6f

                val hH = 22f
                c.drawRect(margin, y, margin + usableW, y + hH, thBg)
                headers.forEachIndexed { idx, h ->
                    drawStaticText(c, h, thPaint, margin + idx * colW + 3f, y + 5f, (colW - 6f).toInt())
                }
                y += hH

                while (rowIdx < grp.rows.size && y < pageH - margin - 38f) {
                    val r = grp.rows[rowIdx]
                    var maxH = 18f
                    val layouts = r.take(colCount).map { cell ->
                        StaticLayout.Builder.obtain(cell, 0, cell.length, tdPaint, (colW - 6f).toInt().coerceAtLeast(12))
                            .setLineSpacing(1f, 1.05f)
                            .build()
                    }
                    layouts.forEach { if (it.height + 8f > maxH) maxH = it.height + 8f }
                    if (y + maxH > pageH - margin - 20f) break

                    if (rowIdx % 2 == 1) c.drawRect(margin, y, margin + usableW, y + maxH, altBg)
                    c.drawRect(margin, y, margin + usableW, y + maxH, borderP)
                    layouts.forEachIndexed { idx, lay ->
                        c.save()
                        c.translate(margin + idx * colW + 3f, y + 4f)
                        lay.draw(c)
                        c.restore()
                        if (idx > 0) c.drawLine(margin + idx * colW, y, margin + idx * colW, y + maxH, borderP)
                    }
                    y += maxH
                    rowIdx++
                }

                c.drawLine(margin, pageH - margin - 14f, margin + usableW, pageH - margin - 14f, borderP)
                drawStaticText(
                    c,
                    "Shri. Govindrao Sakseria High School, Pachora | Software Developed by Tonape sir (SGS Highschool, Pachora)",
                    subPaint, margin, pageH - margin - 11f, usableW.toInt(), Layout.Alignment.ALIGN_CENTER
                )
                doc.finishPage(page)
                globalPageNum++
                groupPageNum++
            } while (rowIdx < grp.rows.size)
        }

        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(exportDir(context), safeName)
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun renderYearHistoryPdf(context: Context, s: Student, blocks: List<ProfileYearBlock>): File {
        val headers = listOf("शै. वर्ष", "इयत्ता/तुकडी", "हजेरी", "जमा फी", "बाकी फी", "डायरी/EL/शिष्य.", "प्रवास")
        val rows = blocks.map { b ->
            listOf(
                b.year + if (b.archived) " (Arch)" else "",
                "${MarathiUtils.classToMr(b.iyatta)} ${b.tukdi} (${b.roll.ifBlank { "-" }})".trim(),
                b.pct?.let { "$it% (${b.absentDays} गै.)" } ?: "${b.absentDays} गै.",
                MarathiUtils.fmtRupees(b.feePaid),
                MarathiUtils.fmtRupees(b.feeBalance),
                "${b.diaryCount}/${b.elCount}/${b.schCount}",
                b.transportSummary
            )
        }
        return renderTablePdf(
            context = context,
            title = "सर्व वर्षांचा विद्यार्थी इतिहास — ${s.fullName}",
            subtitle = "Reg No: ${s.regNo} | Student ID: ${s.studentId} | दिनांक: ${MarathiUtils.todayDisplay()}",
            headers = headers,
            rows = rows,
            fileName = "History_${s.regNo.ifBlank { "student" }}.pdf",
            landscape = true
        )
    }

    fun createProfileShareCard(context: Context, s: Student, photoBitmap: Bitmap? = null): File {
        val w = 720
        val h = 440
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(26, 42, 74))

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(212, 144, 42) }
        c.drawRect(0f, 0f, w.toFloat(), 68f, Paint().apply { color = Color.rgb(13, 27, 53) })
        c.drawRect(0f, 68f, w.toFloat(), 72f, goldPaint)

        val hPaint = makeTextPaint(context, Color.rgb(232, 176, 90), 23f, bold = true)
        drawStaticText(c, "श्री.गोविंदराम सेक्सरिया हायस्कूल, पाचोरा", hPaint, 20f, 20f, w - 40, Layout.Alignment.ALIGN_CENTER)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(253, 240, 230) }
        c.drawRoundRect(RectF(24f, 92f, w - 24f, h - 24f), 16f, 16f, cardPaint)

        val photoRect = Rect(44, 114, 184, 274)
        if (photoBitmap != null) {
            c.drawBitmap(photoBitmap, null, photoRect, null)
        } else {
            c.drawRect(photoRect, Paint().apply { color = Color.rgb(220, 200, 180) })
            drawStaticText(c, "फोटो", makeTextPaint(context, Color.DKGRAY, 20f), 44f, 180f, 140, Layout.Alignment.ALIGN_CENTER)
        }

        val namePaint = makeTextPaint(context, Color.rgb(26, 42, 74), 22f, bold = true)
        val infoPaint = makeTextPaint(context, Color.rgb(40, 25, 15), 16.5f)

        var y = 116f
        y = drawStaticText(c, s.fullName, namePaint, 208f, y, w - 245) + 8f
        val info = buildString {
            appendLine("रजि. क्र. : ${s.regNo}   |   इयत्ता : ${MarathiUtils.classToMr(s.iyatta)} ${s.tukdi} (Roll ${s.rollNo})")
            appendLine("आईचे नाव : ${s.motherName.ifBlank { "—" }}")
            appendLine("जन्मतारीख : ${MarathiUtils.fmtDate(s.dob).ifBlank { "—" }}")
            appendLine("मोबाईल : ${s.bestPhone.ifBlank { "—" }}")
            appendLine("पत्ता : ${s.address.ifBlank { "—" }}")
        }
        drawStaticText(c, info, infoPaint, 208f, y, w - 245)

        val outFile = File(exportDir(context), "Profile_${s.regNo.ifBlank { "card" }}.png")
        FileOutputStream(outFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        return outFile
    }

    fun renderStudentIdCardPdf(
        context: Context,
        students: List<Student>,
        photoMap: Map<String, Bitmap?> = emptyMap(),
        verifyBaseUrl: String,
        acYear: String,
        fileName: String = "SGS_ID_Cards.pdf"
    ): File {
        val doc = PdfDocument()
        val emblem = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.ic_school_emblem) }.getOrNull()
        val pageW = 595
        val pageH = 842
        val marginX = 32f
        val marginY = 30f
        val gapX = 21f
        val gapY = 20f
        val cardW = (pageW - 2 * marginX - gapX) / 2f // ~255pt
        val cardH = (pageH - 2 * marginY - 3 * gapY) / 4f // ~180pt

        val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(26, 42, 74) }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(212, 144, 42) }
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(253, 248, 240) }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 42, 74); style = Paint.Style.STROKE; strokeWidth = 1.4f
        }
        val cutPaint = Paint().apply {
            color = Color.LTGRAY; strokeWidth = 0.7f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        val schoolPaint = makeTextPaint(context, Color.rgb(255, 224, 150), 9.2f, bold = true)
        val subHdrPaint = makeTextPaint(context, Color.WHITE, 6.8f)
        val namePaint = makeTextPaint(context, Color.rgb(18, 30, 62), 9.2f, bold = true)
        val bodyPaint = makeTextPaint(context, Color.rgb(35, 35, 35), 7.4f)
        val signPaint = makeTextPaint(context, Color.rgb(26, 42, 74), 6.8f, bold = true)
        val idMutedPaint = makeTextPaint(context, Color.DKGRAY, 6.5f)
        val idPhotoFallbackPaint = makeTextPaint(context, Color.DKGRAY, 8f)

        val chunks = students.chunked(8).ifEmpty { listOf(emptyList()) }
        chunks.forEachIndexed { pageIdx, batch ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIdx + 1).create())
            val c = page.canvas
            c.drawColor(Color.WHITE)

            batch.forEachIndexed { idx, s ->
                val col = idx % 2
                val row = idx / 2
                val x = marginX + col * (cardW + gapX)
                val y = marginY + row * (cardH + gapY)

                val rect = RectF(x, y, x + cardW, y + cardH)
                c.drawRoundRect(rect, 8f, 8f, cardBgPaint)
                c.drawRoundRect(rect, 8f, 8f, borderPaint)

                // Header strip
                c.drawRect(x + 1f, y + 1f, x + cardW - 1f, y + 36f, navyPaint)
                c.drawRect(x + 1f, y + 36f, x + cardW - 1f, y + 38.5f, goldPaint)
                if (emblem != null) {
                    c.drawBitmap(emblem, null, RectF(x + 6f, y + 4f, x + 34f, y + 32f), null)
                }
                val hdrX = x + (if (emblem != null) 38f else 8f)
                drawStaticText(c, "श्री.गो.से. हायस्कूल, पाचोरा", schoolPaint, hdrX, y + 5f, (cardW - 44f).toInt())
                drawStaticText(
                    c,
                    "विद्यार्थी ओळखपत्र (ID Card) • शै. वर्ष ${s.acYear.ifBlank { acYear }}",
                    subHdrPaint, hdrX, y + 20f, (cardW - 44f).toInt()
                )

                // Student photo
                val photoRect = RectF(x + 8f, y + 46f, x + 68f, y + 118f)
                val bmp = photoMap[s.regNo]
                if (bmp != null) {
                    c.drawBitmap(bmp, null, photoRect, null)
                } else {
                    c.drawRect(photoRect, Paint().apply { color = Color.rgb(225, 229, 238) })
                    drawStaticText(c, "फोटो", idPhotoFallbackPaint, photoRect.left, photoRect.top + 30f, 60, Layout.Alignment.ALIGN_CENTER)
                }
                c.drawRect(photoRect, Paint().apply { color = Color.rgb(180, 140, 70); style = Paint.Style.STROKE; strokeWidth = 1f })

                // Info column
                val infoX = x + 75f
                val infoW = (cardW - 82f).toInt()
                var ty = drawStaticText(c, s.fullName.ifBlank { "—" }, namePaint, infoX, y + 45f, infoW) + 3f
                val infoLines = buildString {
                    appendLine("इयत्ता: ${MarathiUtils.classToMr(s.iyatta)} ${s.tukdi}  |  Roll: ${s.rollNo.ifBlank { "-" }}")
                    appendLine("रजि. क्र.: ${s.regNo.ifBlank { "-" }}  |  ID: ${s.studentId.ifBlank { "-" }}")
                    appendLine("जन्म ता.: ${MarathiUtils.fmtDate(s.dob).ifBlank { "—" }}")
                    appendLine("मोबाईल: ${s.bestPhone.ifBlank { "—" }}")
                    if (s.address.isNotBlank()) append("पत्ता: ${s.address.take(36)}")
                }
                drawStaticText(c, infoLines.trim(), bodyPaint, infoX, ty, infoW)

                // Footer QR + Principal Signature
                val qrUrl = "${verifyBaseUrl}?action=verify&type=ID&regNo=${java.net.URLEncoder.encode(s.regNo, "UTF-8")}"
                val qrBmp = generateQrBitmap(qrUrl, 76)
                if (qrBmp != null) {
                    c.drawBitmap(qrBmp, null, RectF(x + cardW - 44f, y + cardH - 46f, x + cardW - 8f, y + cardH - 10f), null)
                }
                drawStaticText(c, "UDISE: 27031201718", idMutedPaint, x + 8f, y + cardH - 18f, 110)
                drawStaticText(c, "मुख्याध्यापक सही", signPaint, x + cardW - 115f, y + cardH - 18f, 68, Layout.Alignment.ALIGN_OPPOSITE)

                // Cut guide line
                c.drawLine(x - 6f, y + cardH + gapY / 2f, x + cardW + 6f, y + cardH + gapY / 2f, cutPaint)
            }
            doc.finishPage(page)
        }

        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(exportDir(context), safeName)
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun renderFeeReceiptPdf(
        context: Context,
        studentName: String,
        regNo: String,
        iyatta: String,
        tukdi: String,
        acYear: String,
        dateIso: String,
        openingDue: Double,
        totalFee: Double,
        feePaid: Double,
        pendingFee: Double,
        updatedBy: String,
        verifyBaseUrl: String
    ): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas
        c.drawColor(Color.WHITE)

        val margin = 32f
        val usableW = 595f - 2 * margin
        val borderP = Paint().apply { color = Color.rgb(26, 42, 74); style = Paint.Style.STROKE; strokeWidth = 2f }
        val innerP = Paint().apply { color = Color.rgb(212, 144, 42); style = Paint.Style.STROKE; strokeWidth = 1f }
        c.drawRect(margin, margin, 595f - margin, 420f, borderP)
        c.drawRect(margin + 5f, margin + 5f, 595f - margin - 5f, 415f, innerP)

        val emblem = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.ic_school_emblem) }.getOrNull()
        if (emblem != null) {
            c.drawBitmap(emblem, null, RectF(margin + 14f, margin + 14f, margin + 60f, margin + 60f), null)
        }

        val hPaint = makeTextPaint(context, Color.rgb(26, 42, 74), 15f, bold = true)
        val subPaint = makeTextPaint(context, Color.DKGRAY, 9.5f)
        val badgePaint = makeTextPaint(context, Color.WHITE, 11.5f, bold = true)
        val labelPaint = makeTextPaint(context, Color.rgb(30, 30, 30), 10.5f)
        val boldPaint = makeTextPaint(context, Color.BLACK, 11f, bold = true)

        var y = margin + 16f
        y = drawStaticText(c, "पाचोरा तालुका सहकारी शिक्षण संस्था संचलित,", subPaint, margin + 68f, y, (usableW - 84f).toInt(), Layout.Alignment.ALIGN_CENTER)
        y = drawStaticText(c, "श्री. गोविंदराम सेक्सरिया हायस्कूल, पाचोरा", hPaint, margin + 68f, y + 2f, (usableW - 84f).toInt(), Layout.Alignment.ALIGN_CENTER)
        drawStaticText(c, "UDISE: 27031201718  |  शैक्षणिक वर्ष: $acYear", subPaint, margin + 68f, y + 2f, (usableW - 84f).toInt(), Layout.Alignment.ALIGN_CENTER)

        val bandY = margin + 72f
        c.drawRect(margin + 14f, bandY, 595f - margin - 14f, bandY + 24f, Paint().apply { color = Color.rgb(26, 42, 74) })
        drawStaticText(c, "फी भरणा पावती (FEE RECEIPT)", badgePaint, margin + 14f, bandY + 5f, (usableW - 28f).toInt(), Layout.Alignment.ALIGN_CENTER)

        var cy = bandY + 34f
        cy = drawStaticText(c, "विद्यार्थ्याचे नाव: $studentName", boldPaint, margin + 18f, cy, 340)
        drawStaticText(c, "दिनांक: ${MarathiUtils.fmtDate(dateIso).ifBlank { MarathiUtils.todayDisplay() }}", boldPaint, margin + 360f, bandY + 34f, 155, Layout.Alignment.ALIGN_OPPOSITE)
        cy = drawStaticText(c, "जनरल रजि. क्र.: $regNo   |   इयत्ता: ${MarathiUtils.classToMr(iyatta)} $tukdi", labelPaint, margin + 18f, cy + 4f, (usableW - 36f).toInt()) + 10f

        val rowH = 24f
        val rows = listOf(
            "मागील बाकी (Opening Due)" to MarathiUtils.fmtRupees(openingDue),
            "शैक्षणिक वर्ष एकूण फी" to MarathiUtils.fmtRupees(totalFee),
            "एकूण जमा फी (Paid)" to MarathiUtils.fmtRupees(feePaid),
            "शिल्लक बाकी (Pending Balance)" to MarathiUtils.fmtRupees(pendingFee)
        )
        val tblBorder = Paint().apply { color = Color.rgb(180, 188, 200); style = Paint.Style.STROKE; strokeWidth = 0.8f }
        rows.forEachIndexed { idx, (k, v) ->
            if (idx % 2 == 0) c.drawRect(margin + 18f, cy, 595f - margin - 18f, cy + rowH, Paint().apply { color = Color.rgb(245, 247, 252) })
            c.drawRect(margin + 18f, cy, 595f - margin - 18f, cy + rowH, tblBorder)
            drawStaticText(c, k, if (idx >= 2) boldPaint else labelPaint, margin + 26f, cy + 5f, 320)
            drawStaticText(c, v, boldPaint, margin + 350f, cy + 5f, 150, Layout.Alignment.ALIGN_OPPOSITE)
            cy += rowH
        }

        val qrUrl = "$verifyBaseUrl?action=verify&type=FEE&regNo=${java.net.URLEncoder.encode(regNo, "UTF-8")}"
        generateQrBitmap(qrUrl, 90)?.let { qr ->
            c.drawBitmap(qr, null, RectF(margin + 18f, 340f, margin + 84f, 406f), null)
        }
        drawStaticText(c, "नोंद घेणारे: ${updatedBy.ifBlank { "कार्यालय / वर्ग शिक्षक" }}", subPaint, margin + 96f, 365f, 240)
        drawStaticText(c, "स्वीकारणारा सही / शिक्का", boldPaint, 595f - margin - 190f, 382f, 170, Layout.Alignment.ALIGN_OPPOSITE)

        doc.finishPage(page)
        val safeReg = regNo.ifBlank { "student" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val outFile = File(exportDir(context), "Fee_Receipt_$safeReg.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun writeCsvFile(
        context: Context,
        fileName: String,
        headers: List<String>,
        rows: List<List<String>>
    ): File {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(exportDir(context), safeName)
        file.outputStream().use { os ->
            // UTF-8 BOM for proper Marathi Devanagari rendering in Excel
            os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val sb = StringBuilder()
            fun esc(v: String): String = "\"${v.replace("\"", "\"\"")}\""
            sb.appendLine(headers.joinToString(",") { esc(it) })
            rows.forEach { r ->
                sb.appendLine(r.joinToString(",") { esc(it) })
            }
            os.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
        return file
    }

    fun writeTextFile(context: Context, fileName: String, content: String): File {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(exportDir(context), safeName)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    fun renderParentSummaryPdf(
        context: Context,
        s: Student,
        photoBitmap: Bitmap?,
        acYear: String,
        absentDays: Int,
        earlyLeaveCount: Int,
        feeOpening: Double,
        feeTotal: Double,
        feePaid: Double,
        feePending: Double,
        transport: TransportInfo?,
        diaryEntries: List<DiaryEntry>,
        verifyBaseUrl: String
    ): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas
        c.drawColor(Color.WHITE)

        val margin = 28f
        val usableW = 595f - 2 * margin
        val navyBorder = Paint().apply { color = Color.rgb(26, 42, 74); style = Paint.Style.STROKE; strokeWidth = 2f }
        val goldBorder = Paint().apply { color = Color.rgb(212, 144, 42); style = Paint.Style.STROKE; strokeWidth = 1f }
        c.drawRect(margin, margin, 595f - margin, 842f - margin, navyBorder)
        c.drawRect(margin + 4f, margin + 4f, 595f - margin - 4f, 842f - margin - 4f, goldBorder)

        val emblem = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.ic_school_emblem) }.getOrNull()
        if (emblem != null) {
            c.drawBitmap(emblem, null, RectF(margin + 12f, margin + 12f, margin + 56f, margin + 56f), null)
        }

        val hPaint = makeTextPaint(context, Color.rgb(26, 42, 74), 14.5f, bold = true)
        val subPaint = makeTextPaint(context, Color.DKGRAY, 9f)
        val bannerTextPaint = makeTextPaint(context, Color.WHITE, 11f, bold = true)
        val secHeadPaint = makeTextPaint(context, Color.rgb(26, 42, 74), 11f, bold = true)
        val boldPaint = makeTextPaint(context, Color.BLACK, 10f, bold = true)
        val bodyPaint = makeTextPaint(context, Color.rgb(30, 30, 30), 9.8f)

        var y = margin + 12f
        y = drawStaticText(c, "पाचोरा तालुका सहकारी शिक्षण संस्था संचलित,", subPaint, margin + 62f, y, (usableW - 76f).toInt(), Layout.Alignment.ALIGN_CENTER)
        y = drawStaticText(c, "श्री. गोविंदराम सेक्सरिया हायस्कूल, पाचोरा", hPaint, margin + 62f, y + 1f, (usableW - 76f).toInt(), Layout.Alignment.ALIGN_CENTER)
        drawStaticText(c, "UDISE: 27031201718  |  शैक्षणिक वर्ष: $acYear  |  दिनांक: ${MarathiUtils.todayDisplay()}", subPaint, margin + 62f, y + 2f, (usableW - 76f).toInt(), Layout.Alignment.ALIGN_CENTER)

        val bandY = margin + 66f
        c.drawRect(margin + 12f, bandY, 595f - margin - 12f, bandY + 22f, Paint().apply { color = Color.rgb(26, 42, 74) })
        drawStaticText(c, "विद्यार्थी प्रगती व पालक सारांश अहवाल (PARENT SUMMARY REPORT)", bannerTextPaint, margin + 12f, bandY + 4f, (usableW - 24f).toInt(), Layout.Alignment.ALIGN_CENTER)

        // Student Info Card + Photo
        var curY = bandY + 32f
        val photoRect = RectF(margin + 14f, curY, margin + 84f, curY + 86f)
        if (photoBitmap != null) {
            c.drawBitmap(photoBitmap, null, photoRect, null)
        } else {
            c.drawRect(photoRect, Paint().apply { color = Color.rgb(235, 238, 244) })
            drawStaticText(c, "फोटो", subPaint, photoRect.left, photoRect.top + 35f, 70, Layout.Alignment.ALIGN_CENTER)
        }
        c.drawRect(photoRect, goldBorder)

        val infoX = margin + 96f
        val infoW = (usableW - 110f).toInt()
        var iy = drawStaticText(c, s.fullName.ifBlank { "—" }, hPaint.apply { textSize = 13f }, infoX, curY, infoW) + 3f
        val stuLines = buildString {
            appendLine("रजि. क्र.: ${s.regNo}  |  Student ID: ${s.studentId.ifBlank { "-" }}  |  इयत्ता: ${MarathiUtils.classToMr(s.iyatta)} ${s.tukdi} (Roll ${s.rollNo})")
            appendLine("आईचे नाव: ${s.motherName.ifBlank { "—" }}  |  जन्मतारीख: ${MarathiUtils.fmtDate(s.dob).ifBlank { "—" }}")
            appendLine("मोबाईल: ${s.bestPhone.ifBlank { "—" }}")
            append("पत्ता: ${s.address.ifBlank { "—" }}")
        }
        iy = drawStaticText(c, stuLines, bodyPaint, infoX, iy, infoW)
        curY = maxOf(curY + 94f, iy + 10f)

        val tblBorder = Paint().apply { color = Color.rgb(185, 192, 204); style = Paint.Style.STROKE; strokeWidth = 0.8f }
        val rowBg = Paint().apply { color = Color.rgb(245, 247, 252) }

        // Section 1: Attendance & Early Leave + Transport
        c.drawRect(margin + 12f, curY, 595f - margin - 12f, curY + 20f, rowBg)
        drawStaticText(c, "१. उपस्थिती, प्रवास व शिस्त सारांश", secHeadPaint, margin + 18f, curY + 3f, (usableW - 36f).toInt())
        curY += 24f
        val attSummary = listOf(
            "अनुपस्थित (गैरहजर) दिवस" to "$absentDays दिवस",
            "लवकर घरी गेलेल्या नोंदी (Early Leave)" to "$earlyLeaveCount वेळा",
            "प्रवास साधन / मूळ गाव" to (transport?.let { "${it.travelMode.ifBlank { "—" }} (${it.nativeVillage.ifBlank { "—" }}) • ${it.transportContact.ifBlank { "—" }}" } ?: "नोंद नाही")
        )
        attSummary.forEach { (k, v) ->
            c.drawRect(margin + 12f, curY, 595f - margin - 12f, curY + 20f, tblBorder)
            drawStaticText(c, k, bodyPaint, margin + 18f, curY + 4f, 240)
            drawStaticText(c, v, boldPaint, margin + 260f, curY + 4f, (usableW - 278f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)
            curY += 20f
        }
        curY += 10f

        // Section 2: Fee Summary
        c.drawRect(margin + 12f, curY, 595f - margin - 12f, curY + 20f, rowBg)
        drawStaticText(c, "२. शैक्षणिक फी तपशील ($acYear)", secHeadPaint, margin + 18f, curY + 3f, (usableW - 36f).toInt())
        curY += 24f
        val feeRows = listOf(
            "मागील थकबाकी (Opening Due)" to MarathiUtils.fmtRupees(feeOpening),
            "चालू वार्षिक एकूण फी" to MarathiUtils.fmtRupees(feeTotal),
            "एकूण जमा फी (Paid)" to MarathiUtils.fmtRupees(feePaid),
            "शिल्लक बाकी फी (Pending Balance)" to MarathiUtils.fmtRupees(feePending)
        )
        feeRows.forEachIndexed { idx, (k, v) ->
            if (idx == 3) c.drawRect(margin + 12f, curY, 595f - margin - 12f, curY + 20f, rowBg)
            c.drawRect(margin + 12f, curY, 595f - margin - 12f, curY + 20f, tblBorder)
            drawStaticText(c, k, if (idx >= 2) boldPaint else bodyPaint, margin + 18f, curY + 4f, 260)
            drawStaticText(c, v, boldPaint, margin + 280f, curY + 4f, (usableW - 298f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)
            curY += 20f
        }
        curY += 10f

        // Section 3: Teacher Diary Entries
        c.drawRect(margin + 12f, curY, 595f - margin - 12f, curY + 20f, rowBg)
        drawStaticText(c, "३. शिक्षक डायरी व वर्तणूक नोंदी (${diaryEntries.size})", secHeadPaint, margin + 18f, curY + 3f, (usableW - 36f).toInt())
        curY += 24f
        if (diaryEntries.isEmpty()) {
            drawStaticText(c, "• या शैक्षणिक वर्षात कोणतीही विशेष डायरी नोंद नाही.", subPaint, margin + 18f, curY, (usableW - 36f).toInt())
            curY += 18f
        } else {
            diaryEntries.take(8).forEach { d ->
                if (curY > 710f) return@forEach
                val typeLabel = when (d.type.lowercase()) {
                    "positive" -> "✅ कौतुक"
                    "warning" -> "⚠️ सूचना"
                    "scholarship" -> "🎓 शिष्यवृत्ती"
                    else -> "📝 नोंद"
                }
                val h = drawStaticText(
                    c,
                    "• [${MarathiUtils.fmtDate(d.date)}] $typeLabel: ${d.remark}",
                    bodyPaint,
                    margin + 18f,
                    curY,
                    (usableW - 36f).toInt()
                )
                curY = h + 3f
            }
        }

        // Footer with QR & Signatures
        val qrUrl = "$verifyBaseUrl?action=verify&type=PROFILE&regNo=${java.net.URLEncoder.encode(s.regNo, "UTF-8")}"
        generateQrBitmap(qrUrl, 90)?.let { qr ->
            c.drawBitmap(qr, null, RectF(margin + 14f, 742f, margin + 74f, 802f), null)
        }
        drawStaticText(c, "पालकांनी कृपया वरील प्रगती, उपस्थिती व फी तपशीलाची नोंद घ्यावी.", subPaint, margin + 84f, 765f, 220)
        drawStaticText(c, "वर्ग शिक्षक स्वाक्षरी", boldPaint, 320f, 782f, 110, Layout.Alignment.ALIGN_CENTER)
        drawStaticText(c, "मुख्याध्यापक स्वाक्षरी", boldPaint, 445f, 782f, 115, Layout.Alignment.ALIGN_CENTER)

        doc.finishPage(page)
        val safeReg = s.regNo.ifBlank { "student" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val outFile = File(exportDir(context), "Parent_Summary_$safeReg.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun createBirthdayGreetingCard(
        context: Context,
        studentName: String,
        classLabel: String,
        photoBitmap: Bitmap? = null
    ): File {
        val w = 800
        val h = 480
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(20, 35, 70))

        // Festive Gold border & inner cream card
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(212, 144, 42) }
        val goldLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 224, 150) }
        c.drawRect(12f, 12f, w - 12f, h - 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(232, 176, 90); style = Paint.Style.STROKE; strokeWidth = 4f
        })
        c.drawRoundRect(RectF(28f, 92f, w - 28f, h - 28f), 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 245, 232)
        })

        // Confetti dots
        val dotColors = listOf(Color.rgb(232, 176, 90), Color.rgb(74, 222, 128), Color.rgb(125, 211, 252), Color.rgb(248, 113, 113))
        for (i in 0 until 18) {
            val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColors[i % dotColors.size] }
            val cx = 48f + (i * 41f) % (w - 96f)
            val cy = if (i % 2 == 0) 114f + (i * 7f) % 28f else h - 50f - (i * 5f) % 22f
            c.drawCircle(cx, cy, 5f, dp)
        }

        val emblem = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.ic_school_emblem) }.getOrNull()
        if (emblem != null) {
            c.drawBitmap(emblem, null, RectF(34f, 20f, 88f, 74f), null)
        }

        val hdrPaint = makeTextPaint(context, Color.rgb(255, 224, 150), 23f, bold = true)
        drawStaticText(c, "श्री. गोविंदराम सेक्सरिया हायस्कूल, पाचोरा", hdrPaint, 98f, 32f, w - 130, Layout.Alignment.ALIGN_CENTER)

        val wishTitle = makeTextPaint(context, Color.rgb(180, 90, 20), 30f, bold = true)
        val namePaint = makeTextPaint(context, Color.rgb(20, 35, 70), 27f, bold = true)
        val bodyPaint = makeTextPaint(context, Color.rgb(55, 42, 30), 18.5f)
        val signPaint = makeTextPaint(context, Color.rgb(20, 35, 70), 16f, bold = true)

        val textStartX = if (photoBitmap != null) 216f else 56f
        val textWidth = if (photoBitmap != null) w - 260 else w - 112

        if (photoBitmap != null) {
            val pr = RectF(52f, 138f, 192f, 304f)
            c.drawBitmap(photoBitmap, null, pr, null)
            c.drawRect(pr, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(212, 144, 42); style = Paint.Style.STROKE; strokeWidth = 4f })
        }

        var y = 132f
        y = drawStaticText(c, "🎂 वाढदिवसाच्या हार्दिक शुभेच्छा! 🎉", wishTitle, textStartX, y, textWidth, Layout.Alignment.ALIGN_CENTER) + 12f
        y = drawStaticText(c, studentName, namePaint, textStartX, y, textWidth, Layout.Alignment.ALIGN_CENTER) + 6f
        if (classLabel.isNotBlank()) {
            y = drawStaticText(c, "इयत्ता: $classLabel", bodyPaint, textStartX, y, textWidth, Layout.Alignment.ALIGN_CENTER) + 14f
        }
        y = drawStaticText(
            c,
            "तुम्हाला उत्तम आरोग्य, उज्ज्वल यश व आनंददायी शालेय आयुष्य लाभो, हीच आमच्या विद्यालयातर्फे सदिच्छा!",
            bodyPaint, textStartX, y, textWidth, Layout.Alignment.ALIGN_CENTER
        ) + 22f
        drawStaticText(c, "— मुख्याध्यापक, वर्गशिक्षक व सर्व कर्मचारी वृंद", signPaint, textStartX, y, textWidth, Layout.Alignment.ALIGN_CENTER)

        val safeName = studentName.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "student" }
        val outFile = File(exportDir(context), "Birthday_${safeName}.png")
        FileOutputStream(outFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        return outFile
    }

    fun renderAuditLogPdf(
        context: Context,
        student: Student,
        changes: List<ProfileAuditItem>,
        actions: List<ProfileActionLogItem>,
        promotions: List<ProfilePromoLogItem>
    ): File {
        val headers = listOf("प्रकार", "दिनांक / वेळ", "वापरकर्ता (Role)", "तपशील")
        val rows = buildList {
            changes.forEach { c ->
                add(
                    listOf(
                        "बदल",
                        c.timestamp,
                        "${c.user} (${c.role})",
                        "${c.field}: ${c.oldVal.ifBlank { "-" }} → ${c.newVal.ifBlank { "-" }}"
                    )
                )
            }
            actions.forEach { a ->
                add(
                    listOf(
                        "कृती",
                        a.timestamp,
                        "${a.user} (${a.role})",
                        a.action
                    )
                )
            }
            promotions.forEach { p ->
                add(
                    listOf(
                        "पदोन्नती",
                        p.timestamp,
                        p.by,
                        "${p.from} → ${p.to}" + if (p.undone) " [रद्द]" else ""
                    )
                )
            }
            if (isEmpty()) {
                add(listOf("—", MarathiUtils.todayDisplay(), "—", "कोणतीही ऑडिट नोंद आढळली नाही."))
            }
        }
        return renderTablePdf(
            context = context,
            title = "विद्यार्थी ऑडिट व बदल इतिहास (Audit Log) — ${student.fullName}",
            subtitle = "रजि. क्र. ${student.regNo} | इयत्ता ${MarathiUtils.classToMr(student.iyatta)} ${student.tukdi} | बदल: ${changes.size}, कृती: ${actions.size}, पदोन्नती: ${promotions.size}",
            headers = headers,
            rows = rows,
            fileName = "AuditLog_${student.regNo.ifBlank { "student" }}.pdf",
            landscape = true
        )
    }
}

