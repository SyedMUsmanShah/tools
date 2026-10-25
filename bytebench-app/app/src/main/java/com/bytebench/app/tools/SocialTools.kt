package com.bytebench.app.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bytebench.app.ui.Actions
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BtnStyle
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
import com.bytebench.app.ui.Segmented
import com.bytebench.app.ui.TextArea
import com.bytebench.app.ui.theme.LocalSkin
import java.time.Year
import kotlin.math.max
import kotlin.math.roundToInt

private val irregular = mapOf(
    "make" to "made", "build" to "built", "grow" to "grew", "get" to "got", "learn" to "learned", "buy" to "bought",
    "sell" to "sold", "write" to "wrote", "find" to "found", "run" to "ran", "begin" to "began", "lose" to "lost",
    "win" to "won", "take" to "took", "teach" to "taught", "go" to "went", "do" to "did",
)

/** "start a channel" → "started a channel" (good enough for most topics). */
private fun past(t: String): String {
    val w = t.split(' ').toMutableList()
    val v = w[0].lowercase()
    irregular[v]?.let { w[0] = it; return w.joinToString(" ") }
    if (Regex("^[a-z]+$").matches(v) && w.size > 1) {
        w[0] = when {
            v.endsWith("e") -> v + "d"
            Regex("[^aeiou]y$").containsMatchIn(v) -> v.dropLast(1) + "ied"
            Regex("^[^aeiou]*[aeiou][^aeiouwxy]$").matches(v) -> v + v.last() + "ed"
            else -> v + "ed"
        }
    }
    return w.joinToString(" ")
}

@Composable
fun YoutubeTitleGenerator() {
    val context = LocalContext.current
    val skin = LocalSkin.current
    val data = remember { GenData.tool(context, "youtube-title-generator") }
    val T = remember { data.obj("T") }
    val verbs = remember { data.strings("VERBS").toSet() }
    var topic by rememberSaveable { mutableStateOf("start a YouTube channel") }
    var aud by rememberSaveable { mutableStateOf("") }
    var style by rememberSaveable { mutableStateOf("mixed") }
    var seed by remember { mutableIntStateOf(0) }
    val raw = topic.trim().replace(Regex("[.!?]+$"), "")
    val titles = remember(raw, aud, style, seed) {
        if (raw.isEmpty()) return@remember emptyList()
        val kind = if (raw.split(Regex("\\s+"))[0].lowercase() in verbs) "V" else "N"
        val pool = if (style == "mixed") T.keys.flatMap { T.obj(it).strings(kind) } else T.obj(style).strings(kind)
        val reviewSet = T.obj("review").strings("N") + T.obj("review").strings("V")
        val title = Ft.titleCase(raw, true)
        val plain = Ft.titleCase(raw.replace(Regex("\\s+review$", RegexOption.IGNORE_CASE), ""), true)
        val audience = Ft.titleCase(aud.trim().ifEmpty { "Beginners" }, true)
        val year = Year.now().value.toString()
        val out = LinkedHashSet<String>()
        Ft.shuffle(pool).forEach { tpl ->
            if (out.size >= 10) return@forEach
            out += tpl.replace("{T}", if (tpl in reviewSet) plain else title)
                .replace("{Tp}", Ft.titleCase(past(raw), true))
                .replace("{A}", audience).replace("{Y}", year)
                .replace("{N}", Ft.pick(listOf(5, 7, 9, 10, 12)).toString())
        }
        out.toList()
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Field("Video topic", topic, { topic = it.take(80) }, placeholder = "e.g. learn Excel, iPhone 17 review, cook biryani")
        Field("Who is it for? (optional)", aud, { aud = it.take(40) }, placeholder = "e.g. beginners, students")
        Segmented(listOf("mixed" to "Mixed", "howto" to "How-to", "list" to "List", "curious" to "Curiosity", "beginner" to "Beginner", "story" to "Story", "review" to "Review"), style, { style = it })
        BbButton("Generate titles", { seed++ }, icon = "fa-solid fa-wand-magic-sparkles")
        if (raw.isEmpty()) ErrorText("Type your video topic first.")
        Note("Green = under 60 characters, so it won’t be cut off on phones.")
        if (titles.isNotEmpty()) CopyList(titles) { _, t ->
            val n = graphemes(t)
            val c = if (n <= 60) skin.green else if (n <= 100) skin.gold else skin.danger
            Text("$n characters" + if (n > 100) " — too long for YouTube" else "", style = MaterialTheme.typography.bodySmall, color = c)
        }
    }
}

@Composable
fun InstagramCaptionGenerator() {
    val context = LocalContext.current
    val data = remember { GenData.tool(context, "instagram-caption-generator") }
    var topic by rememberSaveable { mutableStateOf("sunset at the beach") }
    var tone by rememberSaveable { mutableStateOf("fun") }
    var emoji by rememberSaveable { mutableStateOf(true) }
    var question by rememberSaveable { mutableStateOf(true) }
    var tags by rememberSaveable { mutableStateOf(false) }
    var seed by remember { mutableIntStateOf(0) }
    val t = topic.trim().replace(Regex("[.!?]+$"), "")

    fun hashtags(s: String): String {
        val skip = setOf("the", "and", "for", "with", "my", "our", "new", "at", "in", "of")
        val w = s.lowercase().replace(Regex("""[^\p{L}\p{N}\s]"""), "").split(Regex("\\s+")).filter { it.length > 2 && it !in skip }
        val out = w.take(3).map { "#$it" }.toMutableList()
        if (w.size > 1) out.add(0, "#" + w.take(2).joinToString(""))
        return (out + listOf("#instagood", "#photooftheday")).take(6).joinToString(" ")
    }
    val captions = remember(t, tone, emoji, question, tags, seed) {
        if (t.isEmpty()) return@remember emptyList()
        val lower = t[0].lowercase() + t.substring(1)
        val E = data.obj("E").strings(tone)
        val Q = data.obj("Q").strings(tone)
        Ft.shuffle(data.obj("C").strings(tone)).take(6).map { tpl ->
            var s = tpl.replace("{t}", lower).replace("{T}", Ft.cap(t))
                .replace("{e}", if (emoji) Ft.pick(E) + (if (Ft.randInt(2) == 1) Ft.pick(E) else "") else "")
                .replace(Regex("""\s+([.!?,])"""), "$1").replace(Regex("\\s{2,}"), " ").trim()
            if (question) s += "\n\n" + Ft.pick(Q)
            if (tags) s += "\n\n" + hashtags(t)
            s
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Field("What is your post about?", topic, { topic = it.take(80) }, placeholder = "e.g. weekend in Hunza, new menu item, my first marathon")
        Segmented(listOf("fun" to "Fun", "inspire" to "Inspiring", "pro" to "Professional", "short" to "Short", "promo" to "Promotional"), tone, { tone = it })
        FlowPills {
            CheckRow("Emojis", emoji) { emoji = it }
            CheckRow("End with a question", question) { question = it }
            CheckRow("Add hashtags", tags) { tags = it }
        }
        BbButton("Generate captions", { seed++ }, icon = "fa-solid fa-wand-magic-sparkles")
        if (t.isEmpty()) ErrorText("Tell us what your post is about first.")
        if (captions.isNotEmpty()) CopyList(captions)
    }
}

private class Tag(val text: String) { var on by mutableStateOf(true) }

@Composable
fun HashtagGenerator() {
    val context = LocalContext.current
    val skin = LocalSkin.current
    val data = remember { GenData.tool(context, "hashtag-generator") }
    val bank = remember { data.obj("BANK") }
    val alias = remember { data.obj("ALIAS") }
    val cities = remember { data.strings("CITIES").toSet() }
    val defaults = remember { data.obj("DEF") }
    val pakistan = setOf("lahore", "karachi", "islamabad", "rawalpindi", "peshawar", "multan", "faisalabad", "quetta", "hunza", "skardu", "murree", "sialkot")
    var kw by rememberSaveable { mutableStateOf("coffee, morning, Lahore") }
    var pf by rememberSaveable { mutableStateOf("instagram") }
    var n by rememberSaveable { mutableFloatStateOf(20f) }
    var seed by remember { mutableIntStateOf(0) }
    fun norm(s: String) = s.lowercase().replace(Regex("""[^\p{L}\p{N}]+"""), "")

    val raw = kw.split(Regex("[,\\n#]+")).map { it.trim() }.filter { it.isNotEmpty() }
    val kws = raw.map(::norm).filter { it.isNotEmpty() }
    val tags = remember(kw, pf, n.roundToInt(), seed) {
        val list = mutableStateListOf<Tag>()
        if (kws.isEmpty()) return@remember list
        val main = kws.toMutableList()
        if (kws.size > 1) main.add(kws[0] + kws[1])
        raw.forEach { r -> if (r.split(Regex("\\s+")).size > 1) main.add(norm(r)) }
        val related = mutableListOf<String>()
        val extra = mutableListOf<String>()
        kws.forEach { k ->
            var cat: String? = if (bank.containsKey(k)) k else alias.str(k).ifEmpty { null }
            if (k in cities) {
                related += listOf(k + "diaries", k + "food", "explore$k", k + "city")
                if (k in pakistan) cat = cat ?: "pakistan"
            }
            if (cat != null && bank.containsKey(cat)) related += Ft.shuffle(bank.strings(cat)).take(6)
            Ft.shuffle(data.obj("SUF").strings(pf)).take(3).forEach { extra += k + it }
        }
        val seen = HashSet<String>()
        (main + related + extra + Ft.shuffle(data.obj("PF").strings(pf)))
            .filter { it.isNotEmpty() && it.length <= 30 && seen.add(it) }
            .take(n.roundToInt())
            .forEach { list += Tag("#$it") }
        list
    }
    val selected = tags.filter { it.on }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Field("Keywords", kw, { kw = it.take(120) }, placeholder = "e.g. coffee, Lahore, small business")
        Segmented(listOf("instagram" to "Instagram", "tiktok" to "TikTok", "x" to "X", "linkedin" to "LinkedIn"), pf, { p ->
            pf = p
            defaults[p]?.let { d -> n = d.toString().toFloatOrNull() ?: n }
        })
        LabeledSlider("How many: ${n.roundToInt()}", n, 3f..30f, { n = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton("Generate", { seed++ }, icon = "fa-solid fa-hashtag")
            BbButton("Copy selected", { Actions.copy(context, selected.joinToString(" ") { it.text }) }, icon = "fa-solid fa-copy", style = BtnStyle.Gold)
        }
        if (kws.isEmpty()) ErrorText("Type at least one keyword.")
        if (tags.isNotEmpty()) {
            Note("${selected.size} selected — tap a hashtag to leave it out.")
            FlowPills {
                tags.forEach { tag ->
                    Text(
                        tag.text,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (tag.on) skin.ink else skin.steel,
                        textDecoration = if (tag.on) null else TextDecoration.LineThrough,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (tag.on) skin.gold.copy(alpha = 0.18f) else skin.board)
                            .clickable { tag.on = !tag.on }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
        Note("Use a mix of broad and specific tags for the best reach.")
    }
}

private fun fit160(s0: String): String {
    val s = s0.replace(Regex("\\s+"), " ").replace(Regex("""\s+([.,!?])"""), "$1").trim()
    if (s.length <= 160) return s
    val cut = s.take(157)
    val i = max(cut.lastIndexOf(". "), cut.lastIndexOf(" — "))
    if (i > 110) return cut.take(i + 1).trim()
    return cut.take(cut.lastIndexOf(' ')).replace(Regex("[,;:—-]+$"), "") + "…"
}

@Composable
fun MetaDescriptionGenerator() {
    val context = LocalContext.current
    val skin = LocalSkin.current
    var title by rememberSaveable { mutableStateOf("Free Online Word Counter") }
    var kw by rememberSaveable { mutableStateOf("word counter") }
    var benefit by rememberSaveable { mutableStateOf("count words, characters and reading time instantly") }
    var cta by rememberSaveable { mutableStateOf("Try it free") }
    var brand by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var edit by rememberSaveable { mutableStateOf("") }
    var seed by remember { mutableIntStateOf(0) }

    val list = remember(title, kw, benefit, cta, brand, seed) {
        val k = kw.trim().ifEmpty { title.trim() }
        val K = Ft.cap(k)
        val ben = benefit.trim().replace(Regex("[.]+$"), "")
        val b = if (ben.isNotEmpty()) ben[0].lowercase() + ben.substring(1) else "get exactly what you need"
        val br = brand.trim()
        val by = if (br.isNotEmpty()) " with $br" else ""
        val at = if (br.isNotEmpty()) " at $br" else ""
        val bl = b.replace(Regex("^(how to|to|helps you)\\s+", RegexOption.IGNORE_CASE), "")
        val B2 = Ft.cap(b)
        // [opening, closing] — filler phrases are added between them until the description is long enough
        val tpls = listOf(
            "$K: $b." to "$cta.",
            "Looking for $k? $B2$by." to "$cta.",
            "$B2 — $k done right." to "$cta.",
            "Learn how to $bl$at." to "$cta.",
            (if (br.isNotEmpty()) "$br helps you " else "We help you ") + "$bl. $K you can rely on." to "$cta.",
            "The easiest way to $bl. $K made simple" + (if (br.isNotEmpty()) " by $br" else "") + "." to "$cta.",
        )
        // neutral phrases only — no claims (like “free”) that might not be true for your page
        val fill = listOf("Simple, clear and easy to follow.", "See how it works and what you get.", "Find the details and next steps here.", "Everything you need in one place.")
        tpls.mapIndexed { i, (open, close) ->
            val parts = mutableListOf(open)
            var j = i
            while ((parts.joinToString(" ") + " " + close).length < 125 && j < i + fill.size) {
                val f = fill[j % fill.size]; j++
                if ((parts.joinToString(" ") + " " + f + " " + close).length <= 160) parts += f
            }
            fit160(parts.joinToString(" ") + " " + close)
        }.distinct()
    }
    LaunchedEffect(list) { if (edit.isEmpty() && list.isNotEmpty()) edit = list[0] }
    val d = edit.trim()
    val len = d.length

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Field("Page title", title, { title = it.take(90) })
        Field("Main keyword", kw, { kw = it.take(60) })
        Field("What does the page offer? (short phrase)", benefit, { benefit = it.take(110) })
        Dropdown("Call to action", listOf("Try it free", "Learn more", "Get started today", "Shop now", "Read the guide", "Book now", "Get a free quote", "Contact us").map { it to it }, cta, { cta = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Brand (optional)", brand, { brand = it.take(40) }, Modifier.weight(1f), placeholder = "e.g. Bytebench")
            Field("Page address", url, { url = it.take(100) }, Modifier.weight(1f), placeholder = "example.com/page")
        }
        BbButton("Generate descriptions", { edit = ""; seed++ }, icon = "fa-solid fa-wand-magic-sparkles")
        CopyList(list) { _, t ->
            val n = t.length
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$n characters", style = MaterialTheme.typography.bodySmall, color = if (n in 120..160) skin.green else skin.gold)
                Text("Use this", style = MaterialTheme.typography.labelMedium, color = skin.accent, modifier = Modifier.clickable { edit = t })
            }
        }
        TextArea("Your description", edit, { edit = it.take(320) }, minLines = 3)
        Meter(
            when { len == 0 -> "Aim for 120–160 characters"; len > 160 -> "Too long — Google will cut it off with “…”"; len < 120 -> "A bit short — add a benefit or detail"; else -> "Great length" },
            "$len / 160", len / 160f,
            if (len > 160) MeterState.Bad else if (len < 120) MeterState.Warn else MeterState.Ok,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton("Copy description", { Actions.copy(context, d) }, icon = "fa-solid fa-copy", style = BtnStyle.Gold, small = true)
            BbButton("Copy HTML tag", { Actions.copy(context, "<meta name=\"description\" content=\"" + d.replace("\"", "&quot;") + "\">") }, icon = "fa-solid fa-code", style = BtnStyle.Ghost, small = true)
        }
        Text("Google preview", style = MaterialTheme.typography.labelLarge, color = skin.ink)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(url.ifBlank { "example.com/your-page" }.replace(Regex("^https?://"), "").replace("/", " › "), style = MaterialTheme.typography.bodySmall, color = Color(0xFF202124))
            val t = title.ifBlank { "Your page title" }
            Text(t.take(62) + if (t.length > 62) "…" else "", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1A0DAB))
            Text(
                if (len > 160) d.take(157).replace(Regex("\\s+\\S*$"), "") + " …" else d.ifEmpty { "Your description will appear here." },
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF4D5156),
            )
        }
    }
}
