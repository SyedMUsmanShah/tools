package com.bytebench.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.bytebench.app.BytebenchApp
import com.bytebench.app.R
import com.bytebench.app.nav.BookR
import com.bytebench.app.nav.ContactR
import com.bytebench.app.ui.Actions
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BbCard
import com.bytebench.app.ui.BtnStyle
import com.bytebench.app.ui.CardShape
import com.bytebench.app.ui.CtaBand
import com.bytebench.app.ui.EmptyState
import com.bytebench.app.ui.Eyebrow
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.IconTile
import com.bytebench.app.ui.LocalContent
import com.bytebench.app.ui.LocalNav
import com.bytebench.app.ui.Markdown
import com.bytebench.app.ui.ScreenScaffold
import com.bytebench.app.ui.SectionHeader
import com.bytebench.app.ui.Segmented
import com.bytebench.app.ui.theme.LocalSkin

/** Turns a site-relative image path into a full address once the website is set. */
fun mediaUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http")) return path
    val base = BytebenchApp.instance.prefs.siteUrl.trim().trimEnd('/')
    return if (base.isEmpty()) null else "$base/" + path.trimStart('/')
}

@Composable
fun BlogScreen(category: String?) {
    val content = LocalContent.current
    val skin = LocalSkin.current
    val cats = content.posts.mapNotNull { it.category }.distinct()
    var selected by rememberSaveable { mutableStateOf(category?.let { c -> cats.firstOrNull { slugOf(it) == c || it == c } } ?: "") }
    val posts = content.posts.filter { selected.isEmpty() || it.category == selected }
    ScreenScaffold(if (category != null) selected.ifEmpty { "Blog" } else "Blog", showBack = category != null) {
        item {
            Text(
                "Business, tech, news and whatever I’m learning — practical guides on sales and web development, and updates on what I’m building.",
                style = MaterialTheme.typography.bodyMedium, color = skin.steel,
            )
        }
        if (cats.size > 1) {
            item { Segmented(listOf("" to "All") + cats.map { it to it }, selected, { selected = it }) }
        }
        if (posts.isEmpty()) {
            item { EmptyState("fa-solid fa-newspaper", "No posts yet", "New posts will appear here. Turn on sync in Settings once the website is live.") }
        }
        posts.forEach { p -> item(key = p.slug) { PostRow(p) } }
    }
}

private fun slugOf(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

@Composable
fun PostScreen(slug: String) {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    val context = LocalContext.current
    val post = content.posts.firstOrNull { it.slug == slug }
    if (post == null) {
        ScreenScaffold("Blog") { item { EmptyState("fa-solid fa-circle-question", "Post not found", "It may have been removed.") } }
        return
    }
    ScreenScaffold(
        post.category ?: "Blog",
        actions = {
            IconButton(onClick = { Actions.shareText(context, post.title + "\n\n" + post.excerpt, post.title) }) {
                FaIcon("fa-solid fa-share-nodes", contentDescription = "Share")
            }
        },
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                post.category?.let { Eyebrow(it) }
                Text(post.title, style = MaterialTheme.typography.displaySmall, color = skin.ink)
                Text("${niceDate(post.publishedAt)} · ${post.readingMinutes} min read", style = MaterialTheme.typography.labelMedium, color = skin.steel)
            }
        }
        mediaUrl(post.cover)?.let { url ->
            item {
                AsyncImage(
                    url, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(CardShape),
                )
            }
        }
        item { Markdown(post.body, onLink = nav::openLink) }
        // Tools that match this post's topic
        val tools = content.tools.filter { t ->
            t.topics.any { post.title.lowercase().contains(it) || post.excerpt.lowercase().contains(it) || (post.category ?: "").lowercase().contains(it) }
        }.take(2)
        if (tools.isNotEmpty()) {
            item { SectionHeader("Try a free tool") }
            tools.forEach { t -> item(key = "t-${t.slug}") { ToolCard(t, null, compact = true) } }
        }
        item {
            BbCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(R.drawable.owner), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, skin.gold, CircleShape),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(content.site.owner.name, style = MaterialTheme.typography.titleMedium, color = skin.ink)
                }
                Text(content.site.authorBio, style = MaterialTheme.typography.bodyMedium, color = skin.steel)
            }
        }
        item {
            CtaBand(
                "Need help with this?", "If your business needs a website, support or a tool like the ones I write about, let’s talk.",
                "Get in touch", { nav.navigate(ContactR) }, "Book a call", { nav.navigate(BookR()) },
            )
        }
    }
}

@Composable
fun ServicesScreen() {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    ScreenScaffold("Services", showBack = false) {
        item {
            Text(
                "Websites, customer and business support, sales operations and custom tools — handled by one person you can actually reach.",
                style = MaterialTheme.typography.bodyLarge, color = skin.steel,
            )
        }
        content.services.sortedBy { it.sortOrder }.forEach { s -> item(key = s.slug) { ServiceCard(s) } }
        item {
            CtaBand(
                "Not sure what you need?", "Book a free 15-minute call. We’ll talk about your goals and I’ll tell you honestly whether I can help.",
                "Book a free call", { nav.navigate(BookR()) }, "Send a message", { nav.navigate(ContactR) },
                icon = "fa-solid fa-calendar-check",
            )
        }
    }
}

@Composable
fun ServiceScreen(slug: String) {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    val context = LocalContext.current
    val s = content.services.firstOrNull { it.slug == slug }
    if (s == null) {
        ScreenScaffold("Service") { item { EmptyState("fa-solid fa-circle-question", "Service not found", "It may have been renamed.") } }
        return
    }
    ScreenScaffold(s.title) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(s.icon, size = 56.dp, iconSize = 22)
                Spacer(Modifier.width(14.dp))
                Column {
                    Eyebrow("Service")
                    Text(s.title, style = MaterialTheme.typography.headlineLarge, color = skin.ink)
                }
            }
        }
        item { Text(s.summary, style = MaterialTheme.typography.bodyLarge, color = skin.steel) }
        if (!s.priceLabel.isNullOrBlank()) item { Text(s.priceLabel, style = MaterialTheme.typography.titleMedium, color = skin.gold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BbButton("Get a quote", {
                    Actions.whatsapp(context, content.site.owner, "Hi ${content.site.owner.shortName}, I’d like a quote for ${s.title}.")
                }, icon = "fa-brands fa-whatsapp")
                BbButton("Book a call", { nav.navigate(BookR(s.title)) }, style = BtnStyle.Ghost)
            }
        }
        if (s.includes.isNotEmpty()) {
            item { SectionHeader("What’s included") }
            item {
                BbCard {
                    s.includes.forEach { inc ->
                        Row(verticalAlignment = Alignment.Top) {
                            FaIcon("fa-solid fa-check", color = skin.green, modifier = Modifier.width(26.dp))
                            Text(inc, style = MaterialTheme.typography.bodyMedium, color = skin.ink)
                        }
                    }
                }
            }
        }
        if (s.details.isNotBlank()) item { Markdown(s.details, onLink = nav::openLink) }
        // Free tools that pitch this service
        val tools = content.tools.filter { it.pitch?.service == s.slug }.take(3)
        if (tools.isNotEmpty()) {
            item { SectionHeader("Free tools you might like") }
            tools.forEach { t -> item(key = "st-${t.slug}") { ToolCard(t, null, compact = true) } }
        }
        val others = content.services.filter { it.slug != s.slug }
        if (others.isNotEmpty()) {
            item { SectionHeader("Other services") }
            others.forEach { o -> item(key = "o-${o.slug}") { ServiceCard(o) } }
        }
    }
}
