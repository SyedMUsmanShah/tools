package com.bytebench.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bytebench.app.ui.theme.LocalSkin

/**
 * Renders the small Markdown subset the site's admin panel writes:
 * headings, paragraphs, bullet and numbered lists, **bold**, *italic*, `code` and links.
 * Links are handed to [onLink] so site paths like /contact open the matching screen.
 */
@Composable
fun Markdown(text: String, onLink: (String) -> Unit, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    val blocks = remember(text) { parseBlocks(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (b in blocks) {
            when (b) {
                is Block.Heading -> Text(
                    inline(b.text, onLink),
                    style = if (b.level <= 2) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    color = skin.ink,
                    modifier = Modifier.padding(top = 8.dp),
                )
                is Block.Para -> Text(inline(b.text, onLink), style = MaterialTheme.typography.bodyLarge, color = skin.ink)
                is Block.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    b.items.forEachIndexed { i, item ->
                        Row {
                            Text(
                                if (b.ordered) "${i + 1}." else "•",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (b.ordered) skin.steel else skin.gold,
                                modifier = Modifier.width(24.dp),
                            )
                            Text(inline(item, onLink), style = MaterialTheme.typography.bodyLarge, color = skin.ink)
                        }
                    }
                }
            }
        }
    }
}

private sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Para(val text: String) : Block
    data class ListBlock(val ordered: Boolean, val items: List<String>) : Block
}

private val bullet = Regex("""^\s*[-*+]\s+(.*)$""")
private val numbered = Regex("""^\s*\d+[.)]\s+(.*)$""")
private val heading = Regex("""^(#{1,6})\s+(.*)$""")

private fun parseBlocks(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val para = StringBuilder()
    var list: MutableList<String>? = null
    var ordered = false
    fun flushPara() { if (para.isNotBlank()) out += Block.Para(para.toString().trim()); para.clear() }
    fun flushList() { list?.let { if (it.isNotEmpty()) out += Block.ListBlock(ordered, it) }; list = null }
    for (raw in src.replace("\r\n", "\n").split('\n')) {
        val line = raw.trimEnd()
        val h = heading.find(line)
        val b = bullet.find(line)
        val n = numbered.find(line)
        when {
            line.isBlank() -> { flushPara(); flushList() }
            h != null -> { flushPara(); flushList(); out += Block.Heading(h.groupValues[1].length, h.groupValues[2]) }
            b != null || n != null -> {
                flushPara()
                val isNum = n != null
                if (list == null || ordered != isNum) { flushList(); list = mutableListOf(); ordered = isNum }
                list!!.add((n ?: b)!!.groupValues[1])
            }
            list != null && raw.startsWith("  ") -> list!!.add(list!!.removeAt(list!!.lastIndex) + " " + line.trim())
            else -> { flushList(); if (para.isNotEmpty()) para.append(' '); para.append(line.trim()) }
        }
    }
    flushPara(); flushList()
    return out
}

private val inlineToken = Regex("""\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`|\[([^\]]+)]\(([^)]+)\)""")

/** Turns inline Markdown into styled text with clickable links. */
@Composable
fun inline(text: String, onLink: (String) -> Unit): AnnotatedString {
    val skin = LocalSkin.current
    val linkStyle = TextLinkStyles(SpanStyle(color = skin.accent, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        var i = 0
        for (m in inlineToken.findAll(text)) {
            append(text.substring(i, m.range.first))
            val g = m.groupValues
            when {
                g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[1]) }
                g[2].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[2]) }
                g[3].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = skin.board)) { append(g[3]) }
                else -> {
                    val url = g[5]
                    withLink(LinkAnnotation.Clickable(tag = url, styles = linkStyle) { onLink(url) }) { append(g[4]) }
                }
            }
            i = m.range.last + 1
        }
        append(text.substring(i))
    }
}

/** Plain text of a Markdown string, for previews and search. */
fun stripMarkdown(s: String): String = s
    .replace(Regex("""\[([^\]]+)]\([^)]+\)"""), "$1")
    .replace(Regex("""[*_`#>]+"""), "")
    .replace(Regex("""\s+"""), " ")
    .trim()
