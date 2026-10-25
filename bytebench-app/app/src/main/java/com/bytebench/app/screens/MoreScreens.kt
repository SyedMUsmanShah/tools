package com.bytebench.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytebench.app.BuildConfigInfo
import com.bytebench.app.BytebenchApp
import com.bytebench.app.R
import com.bytebench.app.nav.AboutR
import com.bytebench.app.nav.BookR
import com.bytebench.app.nav.ContactR
import com.bytebench.app.nav.LegalR
import com.bytebench.app.nav.PostR
import com.bytebench.app.nav.Route
import com.bytebench.app.nav.SearchR
import com.bytebench.app.nav.ServiceR
import com.bytebench.app.nav.SettingsR
import com.bytebench.app.nav.ShopR
import com.bytebench.app.nav.WorkR
import com.bytebench.app.ui.Actions
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BbCard
import com.bytebench.app.ui.BtnStyle
import com.bytebench.app.ui.CardShape
import com.bytebench.app.ui.CtaBand
import com.bytebench.app.ui.EmptyState
import com.bytebench.app.ui.Eyebrow
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.Field
import com.bytebench.app.ui.FlowPills
import com.bytebench.app.ui.IconTile
import com.bytebench.app.ui.LocalContent
import com.bytebench.app.ui.LocalNav
import com.bytebench.app.ui.Markdown
import com.bytebench.app.ui.Note
import com.bytebench.app.ui.Pill
import com.bytebench.app.ui.ScreenScaffold
import com.bytebench.app.ui.SectionHeader
import com.bytebench.app.ui.stripMarkdown
import com.bytebench.app.ui.theme.LocalSkin
import com.bytebench.app.ui.theme.Skins
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun MoreScreen() {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    val context = LocalContext.current
    val owner = content.site.owner
    ScreenScaffold("More", showBack = false, spacing = 12.dp) {
        item {
            BbCard(onClick = { nav.navigate(AboutR) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(R.drawable.owner), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape).border(2.dp, skin.gold, CircleShape),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(owner.name, style = MaterialTheme.typography.titleMedium, color = skin.ink)
                        Text(owner.role, style = MaterialTheme.typography.bodySmall, color = skin.steel)
                    }
                    FaIcon("fa-solid fa-chevron-right", size = 12.sp, color = skin.steel)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BbButton("WhatsApp", { Actions.whatsapp(context, owner) }, Modifier.weight(1f), icon = "fa-brands fa-whatsapp", style = BtnStyle.Gold)
                BbButton("Email", { Actions.email(context, owner.email, "Hello from the ${content.site.name} app") }, Modifier.weight(1f), icon = "fa-solid fa-envelope", style = BtnStyle.Ghost)
            }
        }
        item {
            MenuGroup(
                listOf(
                    Triple("fa-solid fa-user", "About me", AboutR),
                    Triple("fa-solid fa-calendar-check", "Book a free call", BookR()),
                    Triple("fa-solid fa-paper-plane", "Contact", ContactR),
                    Triple("fa-solid fa-bag-shopping", "Shop", ShopR),
                ) + content.projects.filter { it.isCaseStudy }.map { Triple("fa-solid fa-briefcase", "Case study: ${it.title}", WorkR(it.slug)) },
            )
        }
        item {
            MenuGroup(
                listOf(
                    Triple("fa-solid fa-magnifying-glass", "Search", SearchR),
                    Triple("fa-solid fa-sliders", "Settings & theme", SettingsR),
                ),
            )
        }
        item { Eyebrow("Legal", skin.steel) }
        item { MenuGroup(content.legal.map { Triple("fa-solid fa-scale-balanced", it.title, LegalR(it.slug)) }) }
        if (content.site.socials.isNotEmpty()) {
            item { Eyebrow("Follow", skin.steel) }
            item {
                FlowPills {
                    content.site.socials.forEach { s -> Pill(s.name, icon = s.icon, onClick = { Actions.open(context, s.url) }) }
                }
            }
        }
        item {
            Note(
                "${content.site.name} ${BuildConfigInfo.versionName(context)} · Made in ${owner.location.substringBefore(',')}",
                Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MenuGroup(items: List<Triple<String, String, Route>>) {
    val nav = LocalNav.current
    val skin = LocalSkin.current
    BbCard(padding = 0.dp) {
        items.forEachIndexed { i, (icon, label, route) ->
            if (i > 0) HorizontalDivider(color = skin.rule)
            Row(
                Modifier.fillMaxWidth().clickable { nav.navigate(route) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(28.dp)) { FaIcon(icon, color = skin.gold, size = 16.sp) }
                Text(label, style = MaterialTheme.typography.bodyLarge, color = skin.ink, modifier = Modifier.weight(1f))
                FaIcon("fa-solid fa-chevron-right", size = 11.sp, color = skin.steel)
            }
        }
    }
}

@Composable
fun AboutScreen() {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    val context = LocalContext.current
    val site = content.site
    ScreenScaffold("About") {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(
                    painterResource(R.drawable.owner), contentDescription = site.owner.name, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(132.dp).clip(CircleShape).border(3.dp, skin.gold, CircleShape),
                )
                Spacer(Modifier.height(14.dp))
                Text(site.owner.name, style = MaterialTheme.typography.headlineLarge, color = skin.ink)
                Text(site.owner.role, style = MaterialTheme.typography.bodyMedium, color = skin.steel, modifier = Modifier.padding(top = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    FaIcon("fa-solid fa-location-dot", size = 12.sp, color = skin.gold)
                    Spacer(Modifier.width(6.dp))
                    Text(site.owner.location, style = MaterialTheme.typography.labelMedium, color = skin.steel)
                }
            }
        }
        item { Text(site.intro, style = MaterialTheme.typography.bodyLarge, color = skin.ink) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BbButton("WhatsApp me", { Actions.whatsapp(context, site.owner) }, icon = "fa-brands fa-whatsapp")
                BbButton("Book a call", { nav.navigate(BookR()) }, style = BtnStyle.Ghost)
            }
        }
        item { SectionHeader("Experience") }
        site.experience.forEachIndexed { i, e ->
            item(key = "exp$i") {
                Row {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(22.dp)) {
                        Box(Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape).background(skin.gold))
                    }
                    BbCard(Modifier.weight(1f)) {
                        Eyebrow(e.period, skin.steel)
                        Text(e.role, style = MaterialTheme.typography.titleMedium, color = skin.ink)
                        Text(e.org, style = MaterialTheme.typography.bodyMedium, color = skin.gold)
                        e.points.forEach { p ->
                            Row { Text("•", color = skin.gold, modifier = Modifier.width(16.dp)); Text(p, style = MaterialTheme.typography.bodyMedium, color = skin.steel) }
                        }
                    }
                }
            }
        }
        item { SectionHeader("Skills") }
        site.skills.forEach { g ->
            item(key = "sk-${g.group}") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(g.group, style = MaterialTheme.typography.titleSmall, color = skin.ink)
                    FlowPills { g.items.forEach { Pill(it) } }
                }
            }
        }
        if (site.education.isNotBlank()) {
            item { SectionHeader("Education") }
            item {
                BbCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile("fa-solid fa-graduation-cap")
                        Spacer(Modifier.width(12.dp))
                        Text(site.education, style = MaterialTheme.typography.bodyMedium, color = skin.ink)
                    }
                }
            }
        }
        val work = content.projects
        if (work.isNotEmpty()) {
            item { SectionHeader("Work") }
            work.forEach { p ->
                item(key = "w-${p.slug}") {
                    BbCard(onClick = if (p.isCaseStudy) ({ nav.navigate(WorkR(p.slug)) }) else p.url?.let { u -> { Actions.open(context, u) } }) {
                        if (p.isCaseStudy) Eyebrow("Case study")
                        Text(p.title, style = MaterialTheme.typography.titleLarge, color = skin.ink)
                        Text(listOfNotNull(p.role, p.year).joinToString(", "), style = MaterialTheme.typography.labelMedium, color = skin.steel)
                        if (p.summary.isNotBlank()) Text(p.summary, style = MaterialTheme.typography.bodyMedium, color = skin.steel)
                    }
                }
            }
        }
        if (site.socials.isNotEmpty()) {
            item { SectionHeader("Find me online") }
            item { FlowPills { site.socials.forEach { s -> Pill(s.name, icon = s.icon, onClick = { Actions.open(context, s.url) }) } } }
        }
    }
}

@Composable
fun WorkScreen(slug: String) {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    val p = content.projects.firstOrNull { it.slug == slug }
    if (p == null) {
        ScreenScaffold("Case study") { item { EmptyState("fa-solid fa-circle-question", "Not found", "This case study isn’t available.") } }
        return
    }
    ScreenScaffold("Case study") {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Case study")
                Text(p.title, style = MaterialTheme.typography.displaySmall, color = skin.ink)
                Text(p.summary, style = MaterialTheme.typography.bodyLarge, color = skin.steel)
            }
        }
        item {
            BbCard(color = skin.board) {
                p.role?.let { Fact("Role", it) }
                p.year?.let { Fact("When", it) }
                if (p.tech.isNotEmpty()) {
                    Text("Tools & skills", style = MaterialTheme.typography.labelMedium, color = skin.steel)
                    FlowPills { p.tech.forEach { Pill(it) } }
                }
            }
        }
        listOf("The challenge" to p.challenge, "How I approached it" to p.approach, "The result" to p.outcome).forEach { (h, body) ->
            if (body.isNotBlank()) {
                item(key = h) { SectionHeader(h) }
                item(key = "$h-body") { Markdown(body, onLink = nav::openLink) }
            }
        }
        item {
            CtaBand(
                "Want the same for your business?", "If your team repeats the same task every day, there’s usually a tool that can do it in a minute.",
                "Book a free call", { nav.navigate(BookR("Tools & automation")) }, "Custom tools", { nav.navigate(ServiceR("custom-tools-automation")) },
            )
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    val skin = LocalSkin.current
    Row {
        Text(label, style = MaterialTheme.typography.labelMedium, color = skin.steel, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = skin.ink)
    }
}

@Composable
fun ShopScreen() {
    val content = LocalContent.current
    val nav = LocalNav.current
    var filter by rememberSaveable { mutableStateOf("all") }
    val list = content.products.filter { filter == "all" || it.pricing == filter }
    ScreenScaffold("Shop") {
        item {
            Text(
                "Templates and tools I’ve built — free ones you can use now, and paid ones you can request.",
                style = MaterialTheme.typography.bodyMedium, color = LocalSkin.current.steel,
            )
        }
        if (content.products.isEmpty()) {
            item {
                EmptyState(
                    "fa-solid fa-bag-shopping", "Products are on the way",
                    "The shop opens soon. Meanwhile, every free tool works right here in the app.",
                ) { BbButton("Browse free tools", { nav.openLink("/free-tools") }, small = true) }
            }
        } else {
            item { com.bytebench.app.ui.Segmented(listOf("all" to "All", "free" to "Free", "paid" to "Paid"), filter, { filter = it }) }
            list.forEach { p -> item(key = p.slug) { ProductCard(p) } }
        }
    }
}

@Composable
fun ProductScreen(slug: String) {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    val context = LocalContext.current
    val p = content.products.firstOrNull { it.slug == slug }
    if (p == null) {
        ScreenScaffold("Shop") { item { EmptyState("fa-solid fa-circle-question", "Product not found", "It may have been removed from the shop.") } }
        return
    }
    ScreenScaffold(p.name) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PriceTag(p); Text(p.typeLabel, style = MaterialTheme.typography.labelMedium, color = skin.steel) }
                Text(p.name, style = MaterialTheme.typography.displaySmall, color = skin.ink)
                if (p.tagline.isNotBlank()) Text(p.tagline, style = MaterialTheme.typography.bodyLarge, color = skin.steel)
                p.version?.let { Text("Version $it", style = MaterialTheme.typography.labelMedium, color = skin.steel) }
            }
        }
        mediaUrl(p.cover)?.let { url ->
            item {
                coil3.compose.AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(200.dp).clip(CardShape))
            }
        }
        item {
            val url = p.url?.let { mediaUrl(it) }
            if (p.isFree && url != null) {
                BbButton(if (p.type == "download") "Download" else "Open", { Actions.open(context, url) }, icon = "fa-solid fa-arrow-up-right-from-square", style = BtnStyle.Gold)
            } else {
                BbButton("Request to buy on WhatsApp", {
                    Actions.whatsapp(context, content.site.owner, "Hi ${content.site.owner.shortName}, I’d like to buy “${p.name}”${p.priceLabel?.let { " ($it)" } ?: ""}.")
                }, icon = "fa-brands fa-whatsapp", style = BtnStyle.Gold)
            }
        }
        if (p.features.isNotEmpty()) {
            item { SectionHeader("Features") }
            item {
                BbCard {
                    p.features.forEach { f ->
                        Row { FaIcon("fa-solid fa-check", color = skin.green, modifier = Modifier.width(26.dp)); Text(f, style = MaterialTheme.typography.bodyMedium, color = skin.ink) }
                    }
                }
            }
        }
        if (p.description.isNotBlank()) item { Markdown(p.description, onLink = nav::openLink) }
    }
}

@Composable
fun LegalScreen(slug: String) {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    val page = content.legal.firstOrNull { it.slug == slug }
    ScreenScaffold(page?.title ?: "Legal") {
        if (page == null) {
            item { EmptyState("fa-solid fa-circle-question", "Page not found", "") }
        } else {
            item { Text(page.title, style = MaterialTheme.typography.displaySmall, color = skin.ink) }
            if (content.site.legalUpdated.isNotBlank()) item { Note("Last updated ${niceDate(content.site.legalUpdated)}") }
            item { Markdown(page.body, onLink = nav::openLink) }
        }
    }
}

@Composable
fun SearchScreen() {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    var q by rememberSaveable { mutableStateOf("") }
    val term = q.trim().lowercase()
    fun hit(vararg s: String?) = term.length >= 2 && s.any { it != null && it.lowercase().contains(term) }
    val tools = content.tools.filter { hit(it.name, it.summary, it.about, *it.topics.toTypedArray()) }
    val posts = content.posts.filter { hit(it.title, it.excerpt, stripMarkdown(it.body), it.category) }
    val services = content.services.filter { hit(it.title, it.summary, it.details, *it.includes.toTypedArray()) }
    ScreenScaffold("Search", spacing = 12.dp) {
        item { Field("Search tools, posts and services", q, { q = it }, placeholder = "Type at least 2 letters") }
        if (term.length >= 2 && tools.isEmpty() && posts.isEmpty() && services.isEmpty()) {
            item { EmptyState("fa-solid fa-magnifying-glass", "Nothing found", "Try a shorter or different word.") }
        }
        if (tools.isNotEmpty()) {
            item { Eyebrow("Free tools", skin.steel) }
            tools.forEach { t -> item(key = "t-${t.slug}") { ToolCard(t, categoryName(content, t.category), compact = true) } }
        }
        if (services.isNotEmpty()) {
            item { Eyebrow("Services", skin.steel) }
            services.forEach { s -> item(key = "s-${s.slug}") { ServiceCard(s) } }
        }
        if (posts.isNotEmpty()) {
            item { Eyebrow("Blog", skin.steel) }
            posts.forEach { p -> item(key = "p-${p.slug}") { BbCard(onClick = { nav.navigate(PostR(p.slug)) }) { Text(p.title, style = MaterialTheme.typography.titleMedium, color = skin.ink) } } }
        }
    }
}

@Composable
fun SettingsScreen() {
    val app = BytebenchApp.instance
    val skin = LocalSkin.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(app.prefs.siteUrl) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    ScreenScaffold("Settings") {
        item { SectionHeader("Theme", "The same three looks as the website.") }
        item {
            BbCard(padding = 0.dp) {
                (listOf(Triple("auto", "Automatic", "Porcelain by day, Ink & Brass at night")) +
                    Skins.map { Triple(it.key, it.label, null) }).forEachIndexed { i, (key, label, sub) ->
                    if (i > 0) HorizontalDivider(color = skin.rule)
                    val on = app.prefs.skin == key
                    Row(
                        Modifier.fillMaxWidth().clickable { app.prefs.updateSkin(key) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val sw = Skins.firstOrNull { it.key == key }
                        Row(Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, skin.rule, RoundedCornerShape(6.dp))) {
                            if (sw != null) {
                                listOf(sw.paper, sw.ink, sw.gold).forEach { c -> Box(Modifier.size(width = 14.dp, height = 28.dp).background(c)) }
                            } else {
                                listOf(Skins[0].paper, Skins[1].paper, Skins[1].gold).forEach { c -> Box(Modifier.size(width = 14.dp, height = 28.dp).background(c)) }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.titleSmall, color = skin.ink)
                            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = skin.steel)
                        }
                        if (on) FaIcon("fa-solid fa-circle-check", color = skin.gold)
                    }
                }
            }
        }
        item { SectionHeader("Website sync", "When your website is live, the app can load new posts, services and products from it.") }
        item {
            BbCard {
                Field("Website address", url, { url = it }, placeholder = "https://yourdomain.com", keyboard = KeyboardType.Uri)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BbButton(if (busy) "Syncing…" else "Save & sync now", {
                        app.prefs.siteUrl = url.trim()
                        busy = true
                        scope.launch {
                            val err = app.store.sync()
                            busy = false
                            status = err ?: "Up to date."
                        }
                    }, enabled = !busy && url.isNotBlank(), small = true, icon = "fa-solid fa-arrows-rotate")
                    BbButton("Use built-in content", {
                        app.store.resetToBundled()
                        status = "Using the content that came with the app."
                    }, style = BtnStyle.Ghost, small = true)
                }
                status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = skin.steel) }
                val last = app.prefs.lastSync
                Note(
                    if (last > 0) "Last synced ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(last))}."
                    else "Not synced yet — the app is showing the content that shipped with it.",
                )
            }
        }
        item { SectionHeader("About the app") }
        item {
            BbCard {
                Text("All free tools run on your phone. Nothing you type or choose is uploaded.", style = MaterialTheme.typography.bodyMedium, color = skin.ink)
                Note("Version ${BuildConfigInfo.versionName(context)}. Icons by Font Awesome (CC BY 4.0). Fonts: Hanken Grotesk, Newsreader, Manrope and Source Serif 4 (SIL Open Font License).")
            }
        }
        item {
            CtaBand(
                "Found a bug or have an idea?", "Tell me what would make the app more useful.",
                "Send feedback", { Actions.email(context, app.store.content.value.site.owner.email, "Bytebench app feedback") },
            )
        }
    }
}
