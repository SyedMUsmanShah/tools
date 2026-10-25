package com.bytebench.app.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bytebench.app.BytebenchApp
import com.bytebench.app.ui.Actions
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BtnStyle
import com.bytebench.app.ui.CardShape
import com.bytebench.app.ui.CheckRow
import com.bytebench.app.ui.CopyList
import com.bytebench.app.ui.Dropdown
import com.bytebench.app.ui.ErrorText
import com.bytebench.app.ui.Field
import com.bytebench.app.ui.FlowPills
import com.bytebench.app.ui.LabeledSlider
import com.bytebench.app.ui.Meter
import com.bytebench.app.ui.MeterState
import com.bytebench.app.ui.Note
import com.bytebench.app.ui.NumberField
import com.bytebench.app.ui.Segmented
import com.bytebench.app.ui.TextArea
import com.bytebench.app.ui.theme.LocalSkin
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private fun log2(x: Double) = ln(x) / ln(2.0)

@Composable
fun PasswordGenerator() {
    val context = LocalContext.current
    val skin = LocalSkin.current
    val data = remember { GenData.tool(context, "password-generator") }
    val words = remember { data.strings("WORDS") }
    val sets = remember { data.obj("SETS") }
    var mode by rememberSaveable { mutableStateOf("password") }
    var len by rememberSaveable { mutableFloatStateOf(16f) }
    var up by rememberSaveable { mutableStateOf(true) }
    var lo by rememberSaveable { mutableStateOf(true) }
    var num by rememberSaveable { mutableStateOf(true) }
    var sym by rememberSaveable { mutableStateOf(true) }
    var similar by rememberSaveable { mutableStateOf(false) }
    var count by rememberSaveable { mutableFloatStateOf(5f) }
    var sep by rememberSaveable { mutableStateOf("-") }
    var capitalise by rememberSaveable { mutableStateOf(true) }
    var addNum by rememberSaveable { mutableStateOf(true) }
    var seed by remember { mutableIntStateOf(0) }

    fun makePassword(): Pair<String, Double> {
        val groups = listOf("up" to up, "lo" to lo, "num" to num, "sym" to sym).filter { it.second }.map { (k, _) ->
            sets.str(k).let { s -> if (similar) s.replace(Regex("[Il1O0o]"), "") else s }
        }
        if (groups.isEmpty()) return "" to 0.0
        val all = groups.joinToString("")
        val n = len.roundToInt()
        val out = groups.map { g -> g[Ft.randInt(g.length)] }.toMutableList()   // at least one of each chosen type
        while (out.size < n) out.add(all[Ft.randInt(all.length)])
        return Ft.shuffle(out).take(n).joinToString("") to n * log2(all.length.toDouble())
    }
    fun makePhrase(): Pair<String, Double> {
        val n = count.roundToInt()
        val list = MutableList(n) { val w = Ft.pick(words); if (capitalise) w.replaceFirstChar { it.uppercaseChar() } else w }
        var bits = n * log2(words.size.toDouble())
        if (addNum) { list.add(Ft.randInt(list.size + 1), Ft.randInt(100).toString()); bits += log2(100.0) + log2(n + 1.0) }
        return list.joinToString(sep) to bits
    }
    fun gen() = if (mode == "phrase") makePhrase() else makePassword()
    fun crack(bits: Double): String {
        var sec = 2.0.pow(bits) / 2 / 1e11   // 100 billion guesses a second
        if (sec < 1) return "instantly"
        val units = listOf("seconds" to 60.0, "minutes" to 60.0, "hours" to 24.0, "days" to 365.0, "years" to 1000.0, "thousand years" to 1000.0, "million years" to 1000.0, "billion years" to 1e99)
        var i = 0
        while (i < units.size - 1 && sec >= units[i].second) { sec /= units[i].second; i++ }
        return if (sec > 1e6) "longer than the age of the universe" else "about ${Ft.num(sec.roundToInt().toDouble(), 0)} ${units[i].first}"
    }

    // regenerate whenever an option changes or "New" is pressed
    val key = listOf(mode, len.roundToInt(), up, lo, num, sym, similar, count.roundToInt(), sep, capitalise, addNum, seed)
    val (pw, bits) = remember(key) { gen() }
    val more = remember(key) { List(5) { gen().first } }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Segmented(listOf("password" to "Password", "phrase" to "Passphrase"), mode, { mode = it })
        Box(Modifier.fillMaxWidth().clip(CardShape).background(skin.board).padding(18.dp)) {
            Text(
                pw.ifEmpty { "Choose at least one character type." },
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                color = skin.ink,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton("Copy", { Actions.copy(context, pw) }, icon = "fa-solid fa-copy", style = BtnStyle.Gold)
            BbButton("New", { seed++ }, icon = "fa-solid fa-arrows-rotate", style = BtnStyle.Ghost)
        }
        if (pw.isNotEmpty()) {
            val label = when { bits < 40 -> "Weak"; bits < 60 -> "Fair"; bits < 80 -> "Strong"; else -> "Very strong" }
            Meter(
                "Strength: $label · would take ${crack(bits)} to guess", "${bits.roundToInt()} bits", (bits / 100).toFloat(),
                if (bits < 40) MeterState.Bad else if (bits < 60) MeterState.Warn else MeterState.Ok, 10.dp,
            )
        }
        if (mode == "password") {
            LabeledSlider("Length: ${len.roundToInt()} characters", len, 6f..64f, { len = it })
            FlowPills {
                CheckRow("A–Z", up) { up = it }
                CheckRow("a–z", lo) { lo = it }
                CheckRow("0–9", num) { num = it }
                CheckRow("!@#$", sym) { sym = it }
                CheckRow("Avoid look-alikes (l, 1, O, 0)", similar) { similar = it }
            }
        } else {
            LabeledSlider("Words: ${count.roundToInt()}", count, 3f..10f, { count = it }, steps = 6)
            Dropdown("Separator", listOf("-" to "Hyphen (-)", "." to "Dot (.)", "_" to "Underscore (_)", " " to "Space", "" to "None"), sep, { sep = it })
            FlowPills {
                CheckRow("Capitalise", capitalise) { capitalise = it }
                CheckRow("Add a number", addNum) { addNum = it }
            }
        }
        Text("More options", style = MaterialTheme.typography.titleMedium, color = skin.ink)
        CopyList(more)
    }
}

@Composable
fun RandomNameGenerator() {
    val context = LocalContext.current
    val data = remember { GenData.tool(context, "random-name-generator") }
    var type by rememberSaveable { mutableStateOf("english") }
    var gender by rememberSaveable { mutableStateOf("any") }
    var format by rememberSaveable { mutableStateOf("full") }
    var style by rememberSaveable { mutableStateOf("cool") }
    var ukw by rememberSaveable { mutableStateOf("") }
    var unum by rememberSaveable { mutableStateOf(true) }
    var bkw by rememberSaveable { mutableStateOf("") }
    var bstyle by rememberSaveable { mutableStateOf("mixed") }
    var seed by remember { mutableIntStateOf(0) }
    val M = remember { data.strings("M") }; val F = remember { data.strings("F") }; val L = remember { data.strings("L") }
    val U = remember { data.obj("U") }; val B = remember { data.obj("B") }

    fun cap(s: String) = if (s.isEmpty()) s else s[0].uppercaseChar() + s.substring(1).lowercase()
    fun clean(s: String) = s.replace(Regex("""[^\p{L}\p{N}]+"""), "")
    fun english(): String {
        val pool = when (gender) { "m" -> M; "f" -> F; else -> if (Ft.randInt(2) == 1) M else F }
        val first = Ft.pick(pool)
        var name = first
        if (format == "middle") { var mid: String; do { mid = Ft.pick(pool) } while (mid == first); name += " $mid" }
        if (format != "first") name += " " + Ft.pick(L)
        return name
    }
    fun username(): String {
        val s = U.obj(style)
        val a = Ft.pick(s.strings("a")); val n = Ft.pick(s.strings("n"))
        val kw = clean(ukw)
        val k = if (kw.isNotEmpty()) cap(kw) else null
        val pick = Ft.randInt(6)
        var base = if (k != null && pick < 3) listOf(k + n, a + k, k + "_" + n)[pick]
        else listOf(a + n, a + "_" + n, a.lowercase() + "." + n.lowercase(), "The$a$n", n + a[0], a + n + "X")[Ft.randInt(6)]
        if (unum) base += listOf((Ft.randInt(90) + 10).toString(), (Ft.randInt(9000) + 1000).toString(), "_" + (Ft.randInt(98) + 1), (2000 + Ft.randInt(27)).toString())[Ft.randInt(4)]
        return base
    }
    fun business(st0: String): String {
        val kw = cap(clean(bkw.trim().split(Regex("\\s+")).firstOrNull() ?: ""))
        val st = if (st0 == "mixed") listOf("modern", "classic", "playful", "any", "any")[Ft.randInt(5)] else st0
        val short = if (kw.length > 5) kw.replace(Regex("[aeiou]+$", RegexOption.IGNORE_CASE), "").take(max(4, (kw.length * 0.7).let { kotlin.math.ceil(it).toInt() })) else kw
        return when (st) {
            "modern" -> { val m = B.obj("modern"); listOf(short + Ft.pick(m.strings("suf")), Ft.pick(m.strings("pre")) + kw, short + Ft.pick(m.strings("suf")), kw + Ft.pick(listOf("Hub", "Lab", "Box", "Bit", "Base")))[Ft.randInt(4)] }
            "classic" -> { val c = B.obj("classic"); listOf(Ft.pick(c.strings("adj")) + " " + kw + " " + Ft.pick(c.strings("word")), kw + " " + Ft.pick(c.strings("word")), Ft.pick(c.strings("adj")) + " " + kw)[Ft.randInt(3)] }
            "playful" -> {
                val p = B.obj("playful"); val w1 = Ft.pick(p.strings("word")); var w2: String
                do { w2 = Ft.pick(p.strings("word")) } while (w2 == w1)
                listOf(Ft.pick(p.strings("adj")) + " " + w1 + " " + kw, "The " + Ft.pick(p.strings("adj")) + " " + kw, "$w1 & $w2 $kw", "The $kw $w1")[Ft.randInt(4)]
            }
            else -> { val y = B.obj("any"); listOf(Ft.pick(y.strings("adj")) + kw, kw + Ft.pick(y.strings("noun")), Ft.pick(y.strings("adj")) + " " + kw, kw + " " + Ft.pick(y.strings("noun")))[Ft.randInt(4)] }
        }
    }
    val needKw = type == "business" && clean(bkw).isEmpty()
    val names = remember(type, gender, format, style, ukw, unum, bkw, bstyle, seed) {
        if (needKw) emptyList() else {
            val seen = HashSet<String>(); val out = mutableListOf<String>(); var guard = 0
            while (out.size < 12 && guard++ < 400) {
                val v = when (type) { "english" -> english(); "username" -> username(); else -> business(bstyle) }
                if (seen.add(v.lowercase())) out.add(v)
            }
            out
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Segmented(listOf("english" to "English names", "username" to "Usernames", "business" to "Business names"), type, { type = it })
        when (type) {
            "english" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dropdown("Gender", listOf("any" to "Any", "m" to "Male", "f" to "Female"), gender, { gender = it }, Modifier.weight(1f))
                Dropdown("Format", listOf("full" to "First and last", "first" to "First name only", "middle" to "First, middle, last"), format, { format = it }, Modifier.weight(1.2f))
            }
            "username" -> {
                Dropdown("Style", listOf("cool" to "Cool", "gamer" to "Gamer", "pro" to "Professional", "cute" to "Cute"), style, { style = it })
                Field("Include a word (optional)", ukw, { ukw = it.take(20) }, placeholder = "e.g. your name or hobby")
                CheckRow("Add numbers", unum) { unum = it }
            }
            else -> {
                Field("Keyword", bkw, { bkw = it.take(24) }, placeholder = "What your business does — e.g. bakery, travel, tech")
                Dropdown("Style", listOf("mixed" to "Mixed", "modern" to "Modern & short", "classic" to "Classic & trustworthy", "playful" to "Fun & playful"), bstyle, { bstyle = it })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton("Generate", { seed++ }, icon = "fa-solid fa-shuffle")
            BbButton("Copy all", { Actions.copy(context, names.joinToString("\n")) }, icon = "fa-solid fa-copy", style = BtnStyle.Ghost)
        }
        if (needKw) ErrorText("Type a keyword to get business name ideas.")
        if (names.isNotEmpty()) CopyList(names)
        if (type == "business") Note("Before using a business name, search for it and check that the domain name and social handles are free.")
    }
}

@Composable
fun LoremIpsumGenerator() {
    val context = LocalContext.current
    val skin = LocalSkin.current
    val W = remember { GenData.tool(context, "lorem-ipsum-generator").strings("W") }
    var unit by rememberSaveable { mutableStateOf("p") }
    var n by rememberSaveable { mutableStateOf("3") }
    var start by rememberSaveable { mutableStateOf(true) }
    var html by rememberSaveable { mutableStateOf(false) }
    var seed by remember { mutableIntStateOf(0) }
    val START = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"

    fun words(k: Int) = List(k) { Ft.pick(W) }.toMutableList()
    fun sentence(): String {
        val w = words(7 + Ft.randInt(9))
        if (w.size > 9 && Ft.randInt(2) == 1) { val i = 3 + Ft.randInt(4); w[i] = w[i] + "," }
        return Ft.cap(w.joinToString(" ")) + "."
    }
    fun paragraph() = List(4 + Ft.randInt(4)) { sentence() }.joinToString(" ")
    val count = (n.toIntOrNull() ?: 1).coerceIn(1, 500)
    val out = remember(unit, count, start, html, seed) {
        val parts: List<String> = when (unit) {
            "w" -> {
                var w = words(count)
                if (start) w = (START.lowercase().replace(",", "").split(' ').take(count) + w).take(count).toMutableList()
                listOf(Ft.cap(w.joinToString(" ")) + ".")
            }
            "s" -> {
                val s = MutableList(count) { sentence() }
                if (start) s[0] = "$START."
                listOf(s.joinToString(" "))
            }
            else -> {
                val p = MutableList(count) { paragraph() }
                if (start) p[0] = START + ", " + p[0][0].lowercase() + p[0].substring(1)
                p
            }
        }
        parts.joinToString(if (html) "\n" else "\n\n") { if (html) "<p>$it</p>" else it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Segmented(listOf("p" to "Paragraphs", "s" to "Sentences", "w" to "Words"), unit, { u -> unit = u; n = when (u) { "p" -> "3"; "s" -> "5"; else -> "50" } })
        NumberField("How many", n, { n = it })
        FlowPills {
            CheckRow("Start with “Lorem ipsum…”", start) { start = it }
            CheckRow("Wrap in <p> tags", html) { html = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton("Generate", { seed++ }, icon = "fa-solid fa-arrows-rotate")
            BbButton("Copy", { Actions.copy(context, out) }, icon = "fa-solid fa-copy", style = BtnStyle.Gold)
        }
        val plain = out.replace(Regex("</?p>"), "")
        Note("${Ft.num(plain.split(Regex("\\s+")).count { it.isNotBlank() })} words · ${Ft.num(plain.length)} characters")
        Box(Modifier.fillMaxWidth().clip(CardShape).background(skin.board).padding(14.dp)) {
            Text(out, style = MaterialTheme.typography.bodyMedium, color = skin.ink)
        }
    }
}

/* ───────────────────────── QR codes ───────────────────────── */

private val qrColors = listOf(0xFF111827, 0xFF000000, 0xFF0F1B2D, 0xFF1D4ED8, 0xFF0D6B51, 0xFF7C2D12, 0xFF6D28D9, 0xFFB42318)
private val qrBgs = listOf(0xFFFFFFFF, 0xFFF6F7F9, 0xFFFFF7E6, 0xFFEFF6FF, 0xFFECFDF5)

private fun lum(argb: Int): Double {
    val c = listOf((argb shr 16) and 255, (argb shr 8) and 255, argb and 255).map { x ->
        val v = x / 255.0
        if (v <= .03928) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
    }
    return .2126 * c[0] + .7152 * c[1] + .0722 * c[2]
}

private fun qrMatrix(text: String, ec: String) = QRCodeWriter().encode(
    text, BarcodeFormat.QR_CODE, 0, 0,
    mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.valueOf(ec), EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 0),
)

private fun qrBitmap(text: String, ec: String, px: Int, fg: Int, bg: Int): Bitmap {
    val m = qrMatrix(text, ec)
    val n = m.width
    val quiet = 4
    val cell = px.toFloat() / (n + quiet * 2)
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    c.drawColor(bg)
    val p = android.graphics.Paint().apply { color = fg; isAntiAlias = false }
    for (y in 0 until n) for (x in 0 until n) if (m.get(x, y)) {
        c.drawRect(((x + quiet) * cell), ((y + quiet) * cell), ((x + quiet + 1) * cell + 0.5f), ((y + quiet + 1) * cell + 0.5f), p)
    }
    return bmp
}

private fun hex(argb: Int) = String.format("#%06X", argb and 0xFFFFFF)

private fun qrSvg(text: String, ec: String, fg: Int, bg: Int): String {
    val m = qrMatrix(text, ec)
    val n = m.width; val quiet = 4; val size = n + quiet * 2
    val path = StringBuilder()
    for (y in 0 until n) for (x in 0 until n) if (m.get(x, y)) path.append("M${x + quiet} ${y + quiet}h1v1h-1z")
    return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $size $size" shape-rendering="crispEdges"><rect width="100%" height="100%" fill="${hex(bg)}"/><path fill="${hex(fg)}" d="$path"/></svg>"""
}

@Composable
fun QrCodeGenerator() {
    val context = LocalContext.current
    val skin = LocalSkin.current
    val prefs = BytebenchApp.instance.prefs
    val saver = rememberFileSaver()
    var type by rememberSaveable { mutableStateOf("url") }
    var url by rememberSaveable { mutableStateOf(prefs.siteUrl.ifBlank { "https://" }) }
    var text by rememberSaveable { mutableStateOf("") }
    var wa by rememberSaveable { mutableStateOf("") }
    var wam by rememberSaveable { mutableStateOf("") }
    var ssid by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var sec by rememberSaveable { mutableStateOf("WPA") }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var em by rememberSaveable { mutableStateOf("") }
    var ems by rememberSaveable { mutableStateOf("") }
    var emb by rememberSaveable { mutableStateOf("") }
    var ph by rememberSaveable { mutableStateOf("") }
    var fg by rememberSaveable { mutableStateOf(prefs.get("qr-fg", "${0xFF111827.toInt()}").toIntOrNull() ?: 0xFF111827.toInt()) }
    var bg by rememberSaveable { mutableStateOf(prefs.get("qr-bg", "${0xFFFFFFFF.toInt()}").toIntOrNull() ?: 0xFFFFFFFF.toInt()) }
    var size by rememberSaveable { mutableStateOf(1024) }
    var ec by rememberSaveable { mutableStateOf("M") }

    fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    fun wifiEsc(s: String) = s.replace(Regex("""([\\;,:"])"""), "\\\\$1")
    val payload = when (type) {
        "url" -> url.trim().let { u -> if (u.isEmpty() || u == "https://") "" else if (Regex("^[a-z]+:", RegexOption.IGNORE_CASE).containsMatchIn(u)) u else "https://$u" }
        "text" -> text
        "whatsapp" -> {
            var d = wa.filter { it.isDigit() }.removePrefix("00")
            if (Regex("^03\\d{9}$").matches(d)) d = "92" + d.substring(1)
            if (d.isEmpty()) "" else "https://wa.me/$d" + (if (wam.isNotBlank()) "?text=" + enc(wam.trim()) else "")
        }
        "wifi" -> if (ssid.isBlank()) "" else "WIFI:T:$sec;S:${wifiEsc(ssid.trim())};" + (if (sec != "nopass") "P:${wifiEsc(pass)};" else "") + (if (hidden) "H:true;" else "") + ";"
        "email" -> if (em.isBlank()) "" else {
            val q = listOfNotNull(ems.takeIf { it.isNotBlank() }?.let { "subject=" + enc(it.trim()) }, emb.takeIf { it.isNotBlank() }?.let { "body=" + enc(it) })
            "mailto:${em.trim()}" + if (q.isNotEmpty()) "?" + q.joinToString("&") else ""
        }
        else -> ph.filter { it.isDigit() || it == '+' }.let { if (it.isEmpty()) "" else "tel:$it" }
    }
    val preview = remember(payload, ec, fg, bg) { if (payload.isEmpty()) null else runCatching { qrBitmap(payload, ec, 600, fg, bg) }.getOrNull() }
    val tooBig = payload.isNotEmpty() && preview == null
    val l1 = lum(fg); val l2 = lum(bg)
    val ratio = (max(l1, l2) + .05) / (min(l1, l2) + .05)
    val warn = when {
        tooBig -> "That’s too much information for one QR code. Shorten the text, or link to a web page instead."
        l1 > l2 -> "The code is lighter than the background — many phones can’t scan that. Use a dark code on a light background."
        ratio < 4 -> "The colours are too close together. Pick a darker code or a lighter background so phones can scan it."
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Segmented(listOf("url" to "Link", "text" to "Text", "whatsapp" to "WhatsApp", "wifi" to "Wi-Fi", "email" to "Email", "phone" to "Phone"), type, { type = it })
        when (type) {
            "url" -> Field("Website link", url, { url = it }, placeholder = "https://example.com", keyboard = androidx.compose.ui.text.input.KeyboardType.Uri)
            "text" -> TextArea("Text", text, { text = it }, minLines = 3, placeholder = "Any text — up to about 1,000 characters")
            "whatsapp" -> {
                Field("WhatsApp number (with country code)", wa, { wa = it }, placeholder = "+92 300 1234567", keyboard = androidx.compose.ui.text.input.KeyboardType.Phone)
                Field("Pre-filled message (optional)", wam, { wam = it }, placeholder = "Hi! I saw your QR code…")
            }
            "wifi" -> {
                Field("Network name (SSID)", ssid, { ssid = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Password", pass, { pass = it }, Modifier.weight(1f))
                    Dropdown("Security", listOf("WPA" to "WPA / WPA2 / WPA3", "WEP" to "WEP", "nopass" to "No password"), sec, { sec = it }, Modifier.weight(1f))
                }
                CheckRow("Hidden network", hidden) { hidden = it }
            }
            "email" -> {
                Field("Email address", em, { em = it }, keyboard = androidx.compose.ui.text.input.KeyboardType.Email)
                Field("Subject (optional)", ems, { ems = it })
                TextArea("Message (optional)", emb, { emb = it }, minLines = 2)
            }
            else -> Field("Phone number", ph, { ph = it }, placeholder = "+92 300 1234567", keyboard = androidx.compose.ui.text.input.KeyboardType.Phone)
        }
        Text("Code colour", style = MaterialTheme.typography.labelLarge, color = skin.ink)
        Swatches(qrColors.map { it.toInt() }, fg) { fg = it; prefs.set("qr-fg", it.toString()) }
        Text("Background", style = MaterialTheme.typography.labelLarge, color = skin.ink)
        Swatches(qrBgs.map { it.toInt() }, bg) { bg = it; prefs.set("qr-bg", it.toString()) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dropdown("Download size", listOf(512 to "512 px", 1024 to "1024 px", 2048 to "2048 px (print)"), size, { size = it }, Modifier.weight(1f))
            Dropdown("Error correction", listOf("L" to "Low (7%)", "M" to "Medium (15%)", "Q" to "High (25%)", "H" to "Highest (30%)"), ec, { ec = it }, Modifier.weight(1f))
        }
        ErrorText(warn)
        Box(Modifier.fillMaxWidth().clip(CardShape).background(skin.board).padding(18.dp), contentAlignment = Alignment.Center) {
            if (preview != null) Image(preview.asImageBitmap(), "QR code preview", Modifier.size(240.dp))
            else Note("Fill in the details to create your QR code.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ok = preview != null
            BbButton("Save PNG", {
                saver.save(OutFile("qr-code.png", "image/png", encode(qrBitmap(payload, ec, size, fg, bg), "image/png", 100)))
            }, icon = "fa-solid fa-download", enabled = ok, style = BtnStyle.Gold, small = true)
            BbButton("SVG", { saver.save(OutFile("qr-code.svg", "image/svg+xml", qrSvg(payload, ec, fg, bg).toByteArray())) }, enabled = ok, style = BtnStyle.Ghost, small = true)
            BbButton("Share", {
                saver.share(listOf(OutFile("qr-code.png", "image/png", encode(qrBitmap(payload, ec, size, fg, bg), "image/png", 100))))
            }, icon = "fa-solid fa-share-nodes", enabled = ok, style = BtnStyle.Ghost, small = true)
        }
        Note("Always scan a test print with your phone before printing many copies.")
    }
}

@Composable
private fun Swatches(colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    val skin = LocalSkin.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (c == selected) skin.gold else skin.rule)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .clickable { onPick(c) },
            )
        }
    }
}
