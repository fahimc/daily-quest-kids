package com.dailyquestkids.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.dailyquestkids.core.model.ShareCardModel
import java.io.File
import java.time.Clock
import java.time.format.DateTimeFormatter

internal object ShareCardRenderer {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    fun render(model: ShareCardModel): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(226, 246, 255))
        drawBackground(canvas)
        drawCard(canvas, model)
        return bitmap
    }

    fun renderToCacheFile(
        context: Context,
        model: ShareCardModel,
        clock: Clock = Clock.systemUTC(),
    ): File {
        cleanupOldShareFiles(context)
        val directory = File(context.cacheDir, SHARE_DIRECTORY).apply { mkdirs() }
        val fileName = safeFileName(model, clock)
        val file = File(directory, fileName)
        val bitmap = render(model)
        try {
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
            }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    fun saveToPictures(
        context: Context,
        model: ShareCardModel,
        clock: Clock = Clock.systemUTC(),
    ): Uri {
        val fileName = safeFileName(model, clock)
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Daily Quest Kids")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        val bitmap = render(model)
        try {
            resolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "Could not open output stream for share card." }
                bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
            }
        } finally {
            bitmap.recycle()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    fun share(
        context: Context,
        model: ShareCardModel,
        clock: Clock = Clock.systemUTC(),
    ) {
        val file = renderToCacheFile(context, model, clock)
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText(model))
                putExtra(Intent.EXTRA_TITLE, "${model.brand} result")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(intent, "Share Daily Quest Kids"))
    }

    fun shareText(model: ShareCardModel): String =
        listOf(
            model.brand,
            model.utcDate,
            model.visibleResultPattern,
            "Hints used ${model.hintsUsed}",
            "Current streak ${model.currentStreak}",
            "Best streak ${model.bestStreak}",
            "Answers hidden.",
        ).joinToString(separator = "\n")

    fun cleanupOldShareFiles(context: Context) {
        File(context.cacheDir, SHARE_DIRECTORY)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "png" }
            .forEach { file -> file.delete() }
    }

    fun safeFileName(
        model: ShareCardModel,
        clock: Clock,
    ): String {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(clock.zone).format(clock.instant())
        val type =
            model.cardType.name
                .lowercase()
                .replace("_", "-")
        return "daily-quest-kids-$type-${model.utcDate}-$stamp.png"
            .replace(Regex("[^a-zA-Z0-9._-]"), "-")
    }

    private fun drawBackground(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(63, 184, 231)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), (HEIGHT * 0.42f), paint)
        paint.color = Color.rgb(80, 184, 91)
        canvas.drawRect(0f, (HEIGHT * 0.42f), WIDTH.toFloat(), HEIGHT.toFloat(), paint)
    }

    private fun drawCard(
        canvas: Canvas,
        model: ShareCardModel,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(80f, 90f, 1000f, 1260f), 52f, 52f, paint)
        paint.color = Color.rgb(5, 56, 106)
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        paint.textSize = 84f
        canvas.drawText(model.brand, WIDTH / 2f, 210f, paint)
        paint.textSize = 40f
        paint.isFakeBoldText = false
        canvas.drawText(model.utcDate, WIDTH / 2f, 282f, paint)
        val nextY = drawResultPattern(canvas, paint, model.visibleResultPattern)
        paint.color = Color.rgb(255, 183, 38)
        val badgeY = (nextY + 120f).coerceIn(880f, 970f)
        canvas.drawCircle(WIDTH / 2f, badgeY, 118f, paint)
        paint.color = Color.WHITE
        paint.textSize = 116f
        paint.isFakeBoldText = true
        canvas.drawText("★", WIDTH / 2f, badgeY + 40f, paint)
        paint.color = Color.rgb(5, 56, 106)
        paint.textSize = 38f
        canvas.drawText("Ask a grown-up before sharing outside the app.", WIDTH / 2f, 1148f, paint)
        paint.textSize = 32f
        paint.isFakeBoldText = false
        canvas.drawText("Answers and personal details are hidden.", WIDTH / 2f, 1205f, paint)
    }

    private fun drawResultPattern(
        canvas: Canvas,
        paint: Paint,
        pattern: String,
    ): Float {
        var y = 370f
        pattern
            .split("\n")
            .filter { it.isNotBlank() }
            .forEachIndexed { index, line ->
                if (line.isTilePattern()) {
                    drawTilePattern(canvas, line, y)
                    y += 72f
                } else {
                    paint.color = Color.rgb(5, 56, 106)
                    paint.textAlign = Paint.Align.CENTER
                    paint.isFakeBoldText = index == 0
                    drawCenteredTextFit(canvas, paint, line, y, if (index == 0) 54f else 42f)
                    y += if (index == 0) 66f else 54f
                }
            }
        return y
    }

    private fun drawCenteredTextFit(
        canvas: Canvas,
        paint: Paint,
        text: String,
        y: Float,
        maxTextSize: Float,
    ) {
        paint.textSize = maxTextSize
        while (paint.measureText(text) > 790f && paint.textSize > 26f) {
            paint.textSize -= 2f
        }
        canvas.drawText(text, WIDTH / 2f, y, paint)
    }

    private fun drawTilePattern(
        canvas: Canvas,
        line: String,
        centerY: Float,
    ) {
        val labels = line.split("-")
        val tileSize = 56f
        val gap = 14f
        val totalWidth = labels.size * tileSize + (labels.size - 1) * gap
        var left = WIDTH / 2f - totalWidth / 2f
        labels.forEach { label ->
            val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            tilePaint.color = label.tileColor()
            canvas.drawRoundRect(
                RectF(left, centerY - tileSize + 10f, left + tileSize, centerY + 10f),
                10f,
                10f,
                tilePaint,
            )
            left += tileSize + gap
        }
    }

    private fun String.isTilePattern(): Boolean {
        val labels = split("-")
        return labels.size == 5 && labels.all { it in TILE_LABELS }
    }

    private fun String.tileColor(): Int =
        when (this) {
            "correct" -> Color.rgb(44, 164, 86)
            "present" -> Color.rgb(242, 190, 46)
            "absent" -> Color.rgb(55, 53, 68)
            else -> Color.rgb(222, 232, 238)
        }

    private const val SHARE_DIRECTORY = "share-cards"
    private const val MIME_TYPE = "image/png"
    private const val PNG_QUALITY = 100
    private val TILE_LABELS = setOf("correct", "present", "absent", "empty")
}
