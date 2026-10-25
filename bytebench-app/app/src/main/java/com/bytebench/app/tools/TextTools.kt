package com.bytebench.app.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bytebench.app.BytebenchApp
import com.bytebench.app.ui.Actions
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BtnStyle
import com.bytebench.app.ui.FlowPills
import com.bytebench.app.ui.Meter
import com.bytebench.app.ui.MeterState
import com.bytebench.app.ui.Note
import com.bytebench.app.ui.Pill
import com.bytebench.app.ui.StatGrid
import com.bytebench.app.ui.TextArea
import com.bytebench.app.ui.theme.LocalSkin
import java.text.BreakIterator
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val wordRe = Regex("""[\p{L}\p{N}]+(?:['’\-][\p{L}\p{N}]+)*""")

fun countWords(t: String) = wordRe.findAll(t).count()

/** Characters as people see them (an emoji counts once), like Intl.Segmenter. */
fun graphemes(s: String): Int {
    if (s.isEmpty()) return 0
    val it = BreakIterator.getCharacterInstance()
    it.setText(s)
    var n = 0
    while (it.next() != BreakIterator.DONE) n++
    return n
}

private fun duration(sec: Double): String {
    if (sec < 1) return "0 sec"
    if (sec < 60) return "${sec.roundToInt()} sec"
    val m = floor(sec / 60).toInt()
    val s = (sec % 60).roundToInt()
    return "$m min" + if (s > 0) " $s sec" else ""
}

@Composable
fun WordCounter() {
    val context = LocalContext.current
    val prefs = BytebenchApp.instance.prefs
    val skin = LocalSkin.current
    val stop = remember { GenData.tool(context, "word-counter").strings("stop").toSet() }
    var text by remember { mutableStateOf(prefs.get("wc")) }

    val words = wordRe.findAll(text).map { it.value }.toList()
    val sentences = Regex("""[^.!?…]*[\p{L}\p{N}][^.!?…]*(?:[.!?…]+|$)""").findAll(text).count { it.value.isNotBlank() }
    val paras = text.split(Regex("\n+")).count { it.isNotBlank() }
    val counts = words.map { it.lowercase() }.filter { it.length > 2 && it !in stop && !it.all(Char::isDigit) }.groupingBy { it }.eachCount()
    val top = counts.entries.sortedByDescending { it.value }.take(12)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StatGrid(
            listOf(
                Triple("Words", Ft.num(words.size), true),
                Triple("Characters", Ft.num(graphemes(text)), false),
                Triple("Without spaces", Ft.num(graphemes(text.replace(Regex("\\s"), ""))), false),
                Triple("Sentences", Ft.num(sentences), false),
                Triple("Paragraphs", Ft.num(paras), false),
                Triple("Reading time", duration(words.size / 230.0 * 60), false),
                Triple("Speaking time", duration(words.size / 140.0 * 60), false),
            ),
        )
        TextArea("Your text", text, { text = it; prefs.set("wc", it.take(50_000)) }, minLines = 8, placeholder = "Start typing, or paste your text here…")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton("Copy text", { Actions.copy(context, text) }, icon = "fa-solid fa-copy", small = true)
            BbButton("Clear", { text = ""; prefs.set("wc", "") }, style = BtnStyle.Ghost, small = true)
        }
        Text("Most used words", style = MaterialTheme.typography.titleMedium, color = skin.ink)
        if (top.isEmpty()) Note("Your top keywords appear here as you type.")
        else FlowPills {
            top.forEach { (w, c) -> Pill("$w × $c · ${Ft.fixed(c * 100.0 / words.size, 1)}%") }
        }
    }
}

private val limits = listOf(
    "X (Twitter) post" to 280, "Instagram caption" to 2200, "Facebook ad primary text" to 125, "LinkedIn post" to 3000,
    "YouTube title" to 100, "Page title (SEO)" to 60, "Meta description (SEO)" to 160, "Google Ads headline" to 30,
)
private val gsm = Regex("""^[@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !"#¤%&'()*+,\-./0-9:;<=>?¡A-ZÄÖÑÜ§¿a-zäöñüà^{}\\\[~\]|€]*$""")

@Composable
fun CharacterCounter() {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    val n = graphemes(text)
    val isGsm = gsm.matches(text)
    val len = if (isGsm) text.length + text.count { it in "^{}\\[~]|€" } else text.length
    val per = if (isGsm) (if (len > 160) 153 else 160) else (if (len > 70) 67 else 70)
    val sms = if (text.isEmpty()) "0" else "${ceil(len.toDouble() / per).toInt()}" + if (isGsm) "" else " (Unicode)"

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StatGrid(
            listOf(
                Triple("Characters", Ft.num(n), true),
                Triple("Without spaces", Ft.num(graphemes(text.replace(Regex("\\s"), ""))), false),
                Triple("Words", Ft.num(countWords(text)), false),
                Triple("Lines", Ft.num(if (text.isEmpty()) 0 else text.split('\n').size), false),
                Triple("SMS messages", sms, false),
            ),
        )
        TextArea("Your text", text, { text = it }, minLines = 5, placeholder = "Type or paste your text…")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton("Copy text", { Actions.copy(context, text) }, icon = "fa-solid fa-copy", small = true)
            BbButton("Clear", { text = "" }, style = BtnStyle.Ghost, small = true)
        }
        Text("Will it fit?", style = MaterialTheme.typography.titleMedium, color = LocalSkin.current.ink)
        limits.forEach { (label, max) ->
            val left = max - n
            val pct = n.toFloat() / max
            Meter(
                label,
                if (left >= 0) "${Ft.num(n)} / ${Ft.num(max)}" else "${Ft.num(-left)} over",
                pct,
                if (left < 0) MeterState.Bad else if (pct > 0.9f) MeterState.Warn else MeterState.Ok,
            )
        }
        Note("Emojis are counted as one character each, the way you see them. X (Twitter) counts most emojis as two, so leave a little room there.")
    }
}

private fun splitWords(s: String): List<String> = s
    .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
    .replace(Regex("([A-Z])([A-Z][a-z])"), "$1 $2")
    .split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotEmpty() }

private fun perLine(s: String, f: (List<String>) -> String) = s.split('\n').joinToString("\n") { f(splitWords(it)) }

private val cases: List<Pair<String, (String) -> String>> = listOf(
    "UPPERCASE" to { s -> s.uppercase() },
    "lowercase" to { s -> s.lowercase() },
    "Title Case" to { s -> Ft.titleCase(s) },
    "Sentence case" to { s ->
        Regex("""(^\s*|[.!?…]\s+|\n\s*)(\p{L})""").replace(s.lowercase()) { it.groupValues[1] + it.groupValues[2].uppercase() }
            .replace(Regex("""\bi\b"""), "I")
            .replace(Regex("""\bi'(m|ve|ll|d)\b"""), "I'$1")
            .replace(Regex("""\bi’(m|ve|ll|d)\b"""), "I’$1")
    },
    "Capitalized Case" to { s -> Regex("""(^|[\s\-–—/(“"'])(\p{L})""").replace(s.lowercase()) { it.groupValues[1] + it.groupValues[2].uppercase() } },
    "aLtErNaTiNg" to { s ->
        var i = 0
        Regex("""\p{L}""").replace(s) { m -> if (i++ % 2 == 1) m.value.uppercase() else m.value.lowercase() }
    },
    "iNVERSE" to { s -> Regex("""\p{L}""").replace(s) { m -> val u = m.value.uppercase(); if (m.value == u) m.value.lowercase() else u } },
    "camelCase" to { s -> perLine(s) { w -> w.mapIndexed { i, x -> val l = x.lowercase(); if (i > 0) l.replaceFirstChar { it.uppercaseChar() } else l }.joinToString("") } },
    "snake_case" to { s -> perLine(s) { w -> w.joinToString("_").lowercase() } },
    "kebab-case" to { s -> perLine(s) { w -> w.joinToString("-").lowercase() } },
)

@Composable
fun CaseConverter() {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextArea("Your text", text, { text = it }, minLines = 6, placeholder = "Paste text here, then choose a case below…")
        FlowPills {
            cases.forEach { (label, f) ->
                Pill(label, onClick = {
                    if (text.isNotEmpty()) {
                        val next = f(text)
                        if (next != text) { history.add(text); if (history.size > 30) history.removeAt(0); text = next }
                    }
                })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton("Copy", { Actions.copy(context, text) }, icon = "fa-solid fa-copy", style = BtnStyle.Gold, small = true)
            BbButton("Undo", { if (history.isNotEmpty()) text = history.removeAt(history.lastIndex) }, icon = "fa-solid fa-rotate-left", style = BtnStyle.Ghost, small = true, enabled = history.isNotEmpty())
            BbButton("Clear", { if (text.isNotEmpty()) history.add(text); text = "" }, style = BtnStyle.Ghost, small = true)
        }
        if (text.isNotEmpty()) Note("${Ft.num(Regex("""[\p{L}\p{N}]+""").findAll(text).count())} words · ${Ft.num(text.codePointCount(0, text.length))} characters")
    }
}
