package com.bytebench.app.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BtnStyle
import com.bytebench.app.ui.CardShape
import com.bytebench.app.ui.Dropdown
import com.bytebench.app.ui.ErrorText
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.LabeledSlider
import com.bytebench.app.ui.Note
import com.bytebench.app.ui.NumberField
import com.bytebench.app.ui.Segmented
import com.bytebench.app.ui.theme.LocalSkin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** An image the user picked. */
class Picked(val uri: Uri, val name: String, val mime: String, val size: Long)

/** What a tool made from one picked image. */
class Made(val file: OutFile, val width: Int, val height: Int, val thumb: ImageBitmap, val note: String? = null, val original: Long = 0)

private fun info(context: Context, uri: Uri): Picked {
    var name = "image"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
            c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
        }
    }
    return Picked(uri, name, context.contentResolver.getType(uri) ?: "image/jpeg", size)
}

/** Decodes an image, shrinking very large photos so the phone doesn't run out of memory. */
fun decode(context: Context, uri: Uri, maxPixels: Long = 40_000_000): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (bounds.outWidth.toLong() * bounds.outHeight / (sample * sample) > maxPixels) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}

enum class Fit { Stretch, Cover, Pad }

/** FT.draw: draws [src] onto a new W×H bitmap, optionally on a solid background. */
fun draw(src: Bitmap, w: Int, h: Int, fit: Fit, bg: Int?): Bitmap {
    val W = max(1, w); val H = max(1, h)
    val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    if (bg != null) c.drawColor(bg)
    val p = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    val iw = src.width.toFloat(); val ih = src.height.toFloat()
    when (fit) {
        Fit.Cover -> {
            val s = max(W / iw, H / ih); val sw = W / s; val sh = H / s
            val l = ((iw - sw) / 2).roundToInt(); val t = ((ih - sh) / 2).roundToInt()
            c.drawBitmap(src, Rect(l, t, (l + sw).roundToInt(), (t + sh).roundToInt()), Rect(0, 0, W, H), p)
        }
        Fit.Pad -> {
            val k = min(W / iw, H / ih); val dw = iw * k; val dh = ih * k
            c.drawBitmap(src, null, RectF((W - dw) / 2, (H - dh) / 2, (W + dw) / 2, (H + dh) / 2), p)
        }
        Fit.Stretch -> c.drawBitmap(src, null, Rect(0, 0, W, H), p)
    }
    return out
}

fun encode(bmp: Bitmap, mime: String, quality: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val fmt = when (mime) {
        "image/png" -> Bitmap.CompressFormat.PNG
        "image/webp" -> if (android.os.Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.JPEG
    }
    bmp.compress(fmt, quality.coerceIn(1, 100), out)
    return out.toByteArray()
}

fun ext(mime: String) = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }

private fun thumb(b: Bitmap): ImageBitmap {
    val k = min(1f, 360f / max(b.width, b.height))
    return (if (k < 1f) Bitmap.createScaledBitmap(b, max(1, (b.width * k).roundToInt()), max(1, (b.height * k).roundToInt()), true) else b).asImageBitmap()
}

/**
 * The shared batch for the image tools (FT.imageBatch): pick images, run [process] on each
 * whenever the settings change, then save or share one or all of them.
 */
@Composable
fun ImageBatch(
    settingsKey: Any,
    zipName: String,
    showSaving: Boolean,
    process: (Bitmap, Picked) -> Made,
    controls: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val skin = LocalSkin.current
    val saver = rememberFileSaver()
    val picked = remember { mutableStateListOf<Picked>() }
    val results = remember { mutableStateListOf<Made?>() }
    var problem by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
        uris.forEach { picked.add(info(context, it)) }
    }

    LaunchedEffect(settingsKey, picked.size) {
        if (picked.isEmpty()) { results.clear(); return@LaunchedEffect }
        busy = true
        problem = null
        val made = withContext(Dispatchers.Default) {
            picked.toList().map { p ->
                runCatching {
                    val bmp = decode(context, p.uri) ?: throw IllegalArgumentException("“${p.name}” isn’t an image this tool can open.")
                    process(bmp, p)
                }.onFailure { e -> problem = e.message }.getOrNull()
            }
        }
        results.clear(); results.addAll(made)
        busy = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, skin.rule, RoundedCornerShape(14.dp))
                .background(skin.board)
                .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FaIcon("fa-solid fa-images", size = 26.sp, color = skin.gold)
            Text(if (picked.isEmpty()) "Choose images" else "Add more images", style = MaterialTheme.typography.titleMedium, color = skin.ink)
            Text("JPG, PNG or WebP · several at once · they never leave your phone", style = MaterialTheme.typography.bodySmall, color = skin.steel)
        }
        controls()
        ErrorText(problem)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = skin.gold, trackColor = skin.board)
        val done = results.filterNotNull()
        if (done.isNotEmpty()) {
            if (showSaving) {
                val before = done.sumOf { it.original }
                val after = done.sumOf { it.file.bytes.size.toLong() }
                if (before > 0) Note("${done.size} image${if (done.size > 1) "s" else ""}: ${Ft.bytes(before)} → ${Ft.bytes(after)} (${Ft.num((1 - after.toDouble() / before) * 100, 0)}% smaller)")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BbButton(if (done.size > 1) "Save all (ZIP)" else "Save", { saver.saveAll(done.map { it.file }, zipName) }, icon = "fa-solid fa-download", style = BtnStyle.Gold, small = true)
                BbButton("Share", { saver.share(done.map { it.file }) }, icon = "fa-solid fa-share-nodes", style = BtnStyle.Ghost, small = true)
                BbButton("Clear", { picked.clear() }, style = BtnStyle.Ghost, small = true)
            }
        }
        results.forEachIndexed { i, r ->
            if (r == null) return@forEachIndexed
            Row(
                Modifier.fillMaxWidth().clip(CardShape).background(skin.board).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(r.thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.file.name, style = MaterialTheme.typography.titleSmall, color = skin.ink, maxLines = 1)
                    val sizes = if (showSaving && r.original > 0) "${Ft.bytes(r.original)} → ${Ft.bytes(r.file.bytes.size.toLong())}" else Ft.bytes(r.file.bytes.size.toLong())
                    Text("${r.width} × ${r.height} · $sizes" + (r.note?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = skin.steel)
                }
                IconButton(onClick = { saver.save(r.file) }) { FaIcon("fa-solid fa-download", size = 15.sp, color = skin.ink, contentDescription = "Save") }
                IconButton(onClick = { if (i < picked.size) picked.removeAt(i) }) { FaIcon("fa-solid fa-xmark", size = 14.sp, color = skin.steel, contentDescription = "Remove") }
            }
        }
    }
}

private fun sameType(mime: String) = if (mime.contains("png") || mime.contains("webp")) mime else "image/jpeg"

@Composable
fun ImageCompressor() {
    var quality by rememberSaveable { mutableFloatStateOf(75f) }
    var maxW by rememberSaveable { mutableStateOf(1920) }
    var fmt by rememberSaveable { mutableStateOf("same") }
    val q = quality.roundToInt()
    ImageBatch(Triple(q, maxW, fmt), "compressed-images.zip", true, process = { img, p ->
        val k = if (maxW > 0 && img.width > maxW) maxW.toFloat() / img.width else 1f
        val type = if (fmt == "same") sameType(p.mime) else fmt
        val out = draw(img, (img.width * k).roundToInt(), (img.height * k).roundToInt(), Fit.Stretch, if (type == "image/jpeg") android.graphics.Color.WHITE else null)
        val bytes = encode(out, type, q)
        if (bytes.size >= p.size && p.size > 0 && type == p.mime && k == 1f) {
            Made(OutFile(Ft.baseName(p.name) + "." + ext(type), type, bytes), img.width, img.height, thumb(out), "already well compressed", p.size)
        } else {
            Made(OutFile(Ft.baseName(p.name) + "-compressed." + ext(type), type, bytes), out.width, out.height, thumb(out), null, p.size)
        }
    }) {
        LabeledSlider("Quality: $q%", quality, 10f..100f, { quality = it })
        Note("70–80% looks the same to most eyes and saves the most.")
        Dropdown("Maximum width", listOf(0 to "Keep original size", 2560 to "2560 px", 1920 to "1920 px (full HD)", 1280 to "1280 px", 1024 to "1024 px", 800 to "800 px"), maxW, { maxW = it })
        Dropdown("Save as", listOf("same" to "Same format", "image/jpeg" to "JPG", "image/webp" to "WebP (smallest)", "image/png" to "PNG"), fmt, { fmt = it })
    }
}

private val presets = listOf(
    "" to "Custom size",
    "1080x1080" to "Instagram post — square (1080×1080)",
    "1080x1350" to "Instagram post — portrait (1080×1350)",
    "1080x1920" to "Instagram / WhatsApp story (1080×1920)",
    "1200x630" to "Facebook / LinkedIn post (1200×630)",
    "851x315" to "Facebook cover (851×315)",
    "1280x720" to "YouTube thumbnail (1280×720)",
    "2560x1440" to "YouTube banner (2560×1440)",
    "1584x396" to "LinkedIn banner (1584×396)",
    "1500x500" to "X (Twitter) header (1500×500)",
    "500x500" to "Profile picture (500×500)",
    "413x531" to "Passport photo 35×45 mm at 300 dpi (413×531)",
    "1920x1080" to "Full HD (1920×1080)",
)

@Composable
fun ImageResizer() {
    var mode by rememberSaveable { mutableStateOf("size") }
    var preset by rememberSaveable { mutableStateOf("") }
    var w by rememberSaveable { mutableStateOf("1080") }
    var h by rememberSaveable { mutableStateOf("") }
    var fit by rememberSaveable { mutableStateOf("contain") }
    var bgWhite by rememberSaveable { mutableStateOf(true) }
    var fmt by rememberSaveable { mutableStateOf("same") }
    var pct by rememberSaveable { mutableFloatStateOf(50f) }
    ImageBatch(listOf(mode, w, h, fit, bgWhite, fmt, pct.roundToInt()), "resized-images.zip", false, process = { img, p ->
        val iw = img.width.toDouble(); val ih = img.height.toDouble()
        var tw: Double; var th: Double; var how = Fit.Stretch
        var pad = false
        if (mode == "percent") { tw = iw * pct.roundToInt() / 100; th = ih * pct.roundToInt() / 100 }
        else {
            val ww = Ft.value(w); val hh = Ft.value(h)
            if (!(ww > 0) && !(hh > 0)) error("Enter a width or a height.")
            when {
                !(hh > 0) -> { tw = ww; th = ih * ww / iw }
                !(ww > 0) -> { th = hh; tw = iw * hh / ih }
                fit == "contain" -> { val k = min(ww / iw, hh / ih); tw = iw * k; th = ih * k }
                else -> { tw = ww; th = hh; how = when (fit) { "cover" -> Fit.Cover; "pad" -> Fit.Pad.also { pad = true }; else -> Fit.Stretch } }
            }
        }
        if (tw * th > 50e6) error("That size is too large to create on a phone. Try a smaller size.")
        val type = if (fmt == "same") sameType(p.mime) else fmt
        val bg = if (pad) (if (bgWhite) android.graphics.Color.WHITE else android.graphics.Color.BLACK) else if (type == "image/jpeg") android.graphics.Color.WHITE else null
        val out = draw(img, tw.roundToInt(), th.roundToInt(), how, bg)
        Made(OutFile("${Ft.baseName(p.name)}-${out.width}x${out.height}.${ext(type)}", type, encode(out, type, 92)), out.width, out.height, thumb(out))
    }) {
        Segmented(listOf("size" to "By size", "percent" to "By percentage"), mode, { mode = it })
        if (mode == "size") {
            Dropdown("Preset", presets, preset, { v ->
                preset = v
                if (v.isNotEmpty()) { val (a, b) = v.split('x'); w = a; h = b; fit = "cover" }
            })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Width (px)", w, { w = it; preset = "" }, Modifier.weight(1f))
                NumberField("Height (px)", h, { h = it; preset = "" }, Modifier.weight(1f), placeholder = "auto")
            }
            Dropdown(
                "How to fit",
                listOf("contain" to "Keep proportions (fit inside)", "cover" to "Crop to fill exact size", "pad" to "Fit with background (exact size)", "stretch" to "Stretch to exact size"),
                fit, { fit = it },
            )
            if (fit == "pad") Segmented(listOf(true to "White background", false to "Black background"), bgWhite, { bgWhite = it })
            Dropdown("Save as", listOf("same" to "Same format", "image/jpeg" to "JPG", "image/png" to "PNG", "image/webp" to "WebP"), fmt, { fmt = it })
        } else {
            LabeledSlider("Scale: ${pct.roundToInt()}%", pct, 5f..200f, { pct = it })
        }
    }
}

@Composable
fun JpgToPng() {
    var to by rememberSaveable { mutableStateOf("image/png") }
    var quality by rememberSaveable { mutableFloatStateOf(92f) }
    val q = quality.roundToInt()
    ImageBatch(to to q, "converted-images.zip", false, process = { img, p ->
        val out = draw(img, img.width, img.height, Fit.Stretch, if (to == "image/jpeg") android.graphics.Color.WHITE else null)
        Made(OutFile(Ft.baseName(p.name) + "." + ext(to), to, encode(out, to, q)), out.width, out.height, thumb(out), "saved as ${ext(to).uppercase()}")
    }) {
        Segmented(listOf("image/png" to "PNG", "image/jpeg" to "JPG", "image/webp" to "WebP"), to, { to = it })
        if (to != "image/png") LabeledSlider("JPG / WebP quality: $q%", quality, 40f..100f, { quality = it })
        Note("Transparent areas become white when converting to JPG, because JPG can’t store transparency.")
    }
}

@Composable
fun PdfToJpg() {
    val context = LocalContext.current
    val skin = LocalSkin.current
    val saver = rememberFileSaver()
    var pdf by remember { mutableStateOf<Picked?>(null) }
    var scale by rememberSaveable { mutableStateOf(1.5f) }
    var type by rememberSaveable { mutableStateOf("image/jpeg") }
    val pages = remember { mutableStateListOf<Made>() }
    var progress by remember { mutableFloatStateOf(0f) }
    var msg by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pdf = info(context, uri)
    }

    LaunchedEffect(pdf, scale, type) {
        val p = pdf ?: return@LaunchedEffect
        pages.clear(); error = null; progress = 0.02f; msg = "Opening ${p.name}…"
        runCatching {
            withContext(Dispatchers.IO) {
                // PdfRenderer needs a seekable file, so copy the picked PDF into the cache first
                val tmp = File(context.cacheDir, "input.pdf")
                context.contentResolver.openInputStream(p.uri)!!.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { r ->
                        val n = r.pageCount
                        for (i in 0 until n) {
                            r.openPage(i).use { page ->
                                // PDF points are 1/72 inch; the website renders at 1.5× / 2.5× / 4× CSS pixels (96 dpi)
                                var k = scale * 96f / 72f
                                val area = page.width * k * page.height * k
                                if (area > 16e6) k *= sqrt(16e6 / area).toFloat()
                                val bmp = Bitmap.createBitmap(max(1, (page.width * k).roundToInt()), max(1, (page.height * k).roundToInt()), Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val name = Ft.baseName(p.name) + "-page-" + (i + 1).toString().padStart(n.toString().length, '0') + "." + ext(type)
                                val made = Made(OutFile(name, type, encode(bmp, type, 90)), bmp.width, bmp.height, thumb(bmp))
                                withContext(Dispatchers.Main) {
                                    pages.add(made); progress = (i + 1f) / n; msg = "Converting page ${i + 1} of $n…"
                                }
                            }
                        }
                        withContext(Dispatchers.Main) { msg = "$n page${if (n > 1) "s" else ""} converted. Tap a page to save it." }
                    }
                }
            }
        }.onFailure { e ->
            msg = null
            error = if (e is SecurityException) "This PDF is password-protected. Remove the password first (for example by printing it to a new PDF), then try again."
            else "This PDF couldn’t be opened. It may be damaged, or not a PDF."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, skin.rule, RoundedCornerShape(14.dp))
                .background(skin.board)
                .clickable { picker.launch(arrayOf("application/pdf")) }
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FaIcon("fa-solid fa-file-pdf", size = 26.sp, color = skin.gold)
            Text(pdf?.name ?: "Choose a PDF", style = MaterialTheme.typography.titleMedium, color = skin.ink, maxLines = 1)
            Text("One PDF at a time · it never leaves your phone", style = MaterialTheme.typography.bodySmall, color = skin.steel)
        }
        Text("Image quality", style = MaterialTheme.typography.labelLarge, color = skin.ink)
        Segmented(listOf(1.5f to "Standard", 2.5f to "High", 4f to "Print"), scale, { scale = it })
        Text("Image format", style = MaterialTheme.typography.labelLarge, color = skin.ink)
        Segmented(listOf("image/jpeg" to "JPG", "image/png" to "PNG"), type, { type = it })
        ErrorText(error)
        msg?.let {
            Note(it)
            LinearProgressIndicator({ progress }, Modifier.fillMaxWidth(), color = skin.gold, trackColor = skin.board)
        }
        if (pages.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BbButton("Save all (ZIP)", { saver.saveAll(pages.map { it.file }, Ft.baseName(pdf?.name ?: "pdf") + "-images.zip") }, icon = "fa-solid fa-file-zipper", style = BtnStyle.Gold, small = true)
                BbButton("Share", { saver.share(pages.map { it.file }) }, icon = "fa-solid fa-share-nodes", style = BtnStyle.Ghost, small = true)
            }
            pages.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { _, pg ->
                        Column(
                            Modifier.weight(1f).clip(CardShape).background(skin.board).clickable { saver.save(pg.file) }.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Image(pg.thumb, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().aspectRatio(pg.width.toFloat() / pg.height).clip(RoundedCornerShape(6.dp)))
                            Text("Page ${pages.indexOf(pg) + 1} · ${Ft.bytes(pg.file.bytes.size.toLong())}", style = MaterialTheme.typography.bodySmall, color = skin.steel)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
