package com.bytebench.app.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytebench.app.nav.ServiceR
import com.bytebench.app.tools.ToolUi
import com.bytebench.app.ui.Actions
import com.bytebench.app.ui.BbCard
import com.bytebench.app.ui.CtaBand
import com.bytebench.app.ui.EmptyState
import com.bytebench.app.ui.Eyebrow
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.Field
import com.bytebench.app.ui.IconTile
import com.bytebench.app.ui.LocalContent
import com.bytebench.app.ui.LocalNav
import com.bytebench.app.ui.ScreenScaffold
import com.bytebench.app.ui.SectionHeader
import com.bytebench.app.ui.Segmented
import com.bytebench.app.ui.theme.LocalSkin

@Composable
fun ToolsScreen() {
    val content = LocalContent.current
    val skin = LocalSkin.current
    var query by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf("all") }
    val q = query.trim().lowercase()
    val list = content.tools.filter { t ->
        (cat == "all" || t.category == cat) &&
            (q.isEmpty() || t.name.lowercase().contains(q) || t.summary.lowercase().contains(q) || t.topics.any { it.contains(q) })
    }
    ScreenScaffold("Free online tools", showBack = false, spacing = 12.dp) {
        item {
            Text(
                "Quick, private tools for everyday work. They run on your phone — nothing is uploaded, and they work offline.",
                style = MaterialTheme.typography.bodyMedium, color = skin.steel,
            )
        }
        item { Field("Search ${content.tools.size} tools", query, { query = it }, placeholder = "e.g. PDF, invoice, password") }
        item {
            Segmented(
                listOf("all" to "All") + content.toolCategories.map { it.slug to it.name },
                cat, { cat = it },
            )
        }
        if (list.isEmpty()) {
            item { EmptyState("fa-solid fa-magnifying-glass", "No tools found", "Try another word, or pick “All”.") }
        }
        list.forEach { t -> item(key = t.slug) { ToolCard(t, categoryName(content, t.category)) } }
    }
}

@Composable
fun ToolScreen(slug: String) {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    val context = LocalContext.current
    val tool = content.tools.firstOrNull { it.slug == slug }
    if (tool == null) {
        ScreenScaffold("Tool") { item { EmptyState("fa-solid fa-circle-question", "Tool not found", "This tool isn’t available in this version of the app.") } }
        return
    }
    ScreenScaffold(
        tool.name,
        actions = {
            IconButton(onClick = {
                Actions.shareText(context, "${tool.name} — ${tool.summary}", tool.name)
            }) { FaIcon("fa-solid fa-share-nodes", contentDescription = "Share") }
        },
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(tool.icon, size = 52.dp, iconSize = 20)
                Spacer(Modifier.width(14.dp))
                Column {
                    categoryName(content, tool.category)?.let { Eyebrow(it) }
                    Text(tool.name, style = MaterialTheme.typography.headlineLarge, color = skin.ink)
                }
            }
        }
        item { Text(tool.summary, style = MaterialTheme.typography.bodyLarge, color = skin.steel) }
        item {
            BbCard(padding = 16.dp) { ToolUi(slug) }
        }
        if (tool.steps.isNotEmpty()) {
            item { SectionHeader("How to use it") }
            item {
                BbCard {
                    tool.steps.forEachIndexed { i, s ->
                        Row {
                            Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = skin.gold, modifier = Modifier.width(26.dp))
                            Text(s, style = MaterialTheme.typography.bodyMedium, color = skin.ink)
                        }
                    }
                }
            }
        }
        if (tool.about.isNotBlank()) {
            item { SectionHeader("About this tool") }
            item { Text(tool.about, style = MaterialTheme.typography.bodyLarge, color = skin.ink) }
        }
        if (tool.faqs.isNotEmpty()) {
            item { SectionHeader("Questions") }
            item {
                BbCard(padding = 0.dp) {
                    tool.faqs.forEachIndexed { i, f ->
                        if (i > 0) HorizontalDivider(color = skin.rule)
                        FaqRow(f.q, f.a)
                    }
                }
            }
        }
        tool.pitch?.let { p ->
            item {
                CtaBand(
                    title = p.title, text = p.text,
                    primary = "See the service", onPrimary = { nav.navigate(ServiceR(p.service)) },
                    secondary = "Ask on WhatsApp",
                    onSecondary = { Actions.whatsapp(context, content.site.owner, "Hi ${content.site.owner.shortName}, I was using your ${tool.name} and have a question.") },
                    icon = "fa-solid fa-handshake",
                )
            }
        }
        val related = content.posts.filter { post ->
            tool.topics.any { topic ->
                post.title.lowercase().contains(topic) || post.excerpt.lowercase().contains(topic) || (post.category ?: "").lowercase().contains(topic)
            }
        }.take(3)
        if (related.isNotEmpty()) {
            item { SectionHeader("Related reading") }
            related.forEach { p -> item(key = "rel-${p.slug}") { PostRow(p) } }
        }
        val more = content.tools.filter { it.category == tool.category && it.slug != tool.slug }.take(4)
        if (more.isNotEmpty()) {
            item { SectionHeader("More ${categoryName(content, tool.category)?.lowercase() ?: ""} tools") }
            more.forEach { t -> item(key = "more-${t.slug}") { ToolCard(t, null, compact = true) } }
        }
    }
}

@Composable
private fun FaqRow(q: String, a: String) {
    val skin = LocalSkin.current
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { open = !open }
            .padding(16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(q, style = MaterialTheme.typography.titleSmall, color = skin.ink, modifier = Modifier.weight(1f))
            FaIcon(if (open) "fa-solid fa-minus" else "fa-solid fa-plus", size = 12.sp, color = skin.steel)
        }
        if (open) Text(a, style = MaterialTheme.typography.bodyMedium, color = skin.steel)
    }
}
