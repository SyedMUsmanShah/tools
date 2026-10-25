package com.bytebench.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytebench.app.R
import com.bytebench.app.nav.AboutR
import com.bytebench.app.nav.BlogR
import com.bytebench.app.nav.BookR
import com.bytebench.app.nav.ContactR
import com.bytebench.app.nav.SearchR
import com.bytebench.app.nav.ServicesR
import com.bytebench.app.nav.SettingsR
import com.bytebench.app.nav.ShopR
import com.bytebench.app.nav.ToolsR
import com.bytebench.app.nav.WorkR
import com.bytebench.app.ui.Actions
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BbCard
import com.bytebench.app.ui.BtnStyle
import com.bytebench.app.ui.CardShapeLg
import com.bytebench.app.ui.CtaBand
import com.bytebench.app.ui.Eyebrow
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.IconTile
import com.bytebench.app.ui.LocalContent
import com.bytebench.app.ui.LocalNav
import com.bytebench.app.ui.ScreenScaffold
import com.bytebench.app.ui.SectionHeader
import com.bytebench.app.ui.theme.LocalSkin

@Composable
fun HomeScreen() {
    val content = LocalContent.current
    val nav = LocalNav.current
    val skin = LocalSkin.current
    val context = LocalContext.current
    val site = content.site

    ScreenScaffold(
        title = site.name,
        showBack = false,
        actions = {
            IconButton(onClick = { nav.navigate(SearchR) }) { FaIcon("fa-solid fa-magnifying-glass", contentDescription = "Search") }
            IconButton(onClick = { nav.navigate(SettingsR) }) { FaIcon("fa-solid fa-sliders", contentDescription = "Settings") }
        },
    ) {
        // Hero
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(CardShapeLg)
                    .background(skin.heroBg)
                    .background(Brush.radialGradient(listOf(skin.gold.copy(alpha = 0.18f), skin.heroBg.copy(alpha = 0f)), radius = 900f))
                    .border(1.dp, skin.rule, CardShapeLg)
                    .padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(skin.green))
                        Spacer(Modifier.width(8.dp))
                        Text("Available for new projects", style = MaterialTheme.typography.labelMedium, color = skin.heroMuted)
                    }
                    Text(site.tagline, style = MaterialTheme.typography.displayMedium, color = skin.heroInk)
                    Text(
                        "I build websites, look after your customers, handle the day-to-day work that keeps a business running, and make tools that save your team hours. One person, remote, and easy to reach.",
                        style = MaterialTheme.typography.bodyLarge, color = skin.heroMuted,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BbButton("See services", { nav.navigate(ServicesR) }, style = BtnStyle.Hero)
                        BbButton("Book a free call", { nav.navigate(BookR()) }, icon = "fa-solid fa-calendar-check", style = BtnStyle.Ghost)
                    }
                }
            }
        }

        // Numbers strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                site.stats.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { s ->
                            BbCard(Modifier.weight(1f), padding = 14.dp) {
                                Text(statValue(s, content), style = MaterialTheme.typography.headlineLarge, color = skin.gold)
                                Text(s.label, style = MaterialTheme.typography.bodySmall, color = skin.steel)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // How I work
        item { SectionHeader("How I work", "Simple, clear and on your schedule — from first message to launch.") }
        site.steps.forEachIndexed { i, step ->
            item(key = "step$i") {
                BbCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(step.icon)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Step ${i + 1}")
                            Text(step.title, style = MaterialTheme.typography.titleMedium, color = skin.ink)
                        }
                    }
                    Text(step.text, style = MaterialTheme.typography.bodyMedium, color = skin.steel)
                }
            }
        }

        // Free tools
        item {
            SectionHeader(
                "Free online tools",
                "Quick, private tools for everyday work. No sign-up, and your files never leave your device.",
                "All free tools",
            ) { nav.navigate(ToolsR) }
        }
        val featured = content.featuredTools.mapNotNull { slug -> content.tools.firstOrNull { it.slug == slug } }
            .ifEmpty { content.tools.take(8) }
        featured.chunked(2).forEachIndexed { i, pair ->
            item(key = "ft$i") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { t ->
                        Box(Modifier.weight(1f)) { ToolPeg(t.name, t.icon) { nav.openLink("/free-tools/${t.slug}") } }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Shop
        if (content.products.isNotEmpty()) {
            item {
                SectionHeader("From the shop", "Templates and tools I’ve built — free ones you can use now, and paid ones you can request.", "Visit the shop") { nav.navigate(ShopR) }
            }
            content.products.take(3).forEach { p -> item(key = "p-${p.slug}") { ProductCard(p) } }
        }

        // Blog
        if (content.posts.isNotEmpty()) {
            item { SectionHeader("From the blog", null, "All posts") { nav.navigate(BlogR) } }
            content.posts.take(3).forEach { p -> item(key = "post-${p.slug}") { PostRow(p) } }
        }

        // Hi, I'm …
        item {
            BbCard(padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(R.drawable.owner), contentDescription = site.owner.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, skin.gold, CircleShape),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Hi, I’m ${site.owner.shortName}", style = MaterialTheme.typography.headlineMedium, color = skin.ink)
                        Text(site.owner.location, style = MaterialTheme.typography.bodySmall, color = skin.steel)
                    }
                }
                Text(site.intro, style = MaterialTheme.typography.bodyLarge, color = skin.ink)
                Row(
                    Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).background(skin.board).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FaIcon("fa-solid fa-gears", color = skin.gold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "My habit: if a task repeats, I build a tool or automation for it — so the time goes into the work that needs a person.",
                        style = MaterialTheme.typography.bodyMedium, color = skin.ink,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                    BbButton("Book a free 15-min call", { nav.navigate(BookR()) }, icon = "fa-solid fa-calendar-check", small = true)
                    BbButton("More about me", { nav.navigate(AboutR) }, style = BtnStyle.Ghost, small = true)
                }
            }
        }
        content.projects.firstOrNull { it.isCaseStudy }?.let { study ->
            item {
                BbCard(onClick = { nav.navigate(WorkR(study.slug)) }, color = skin.board) {
                    Eyebrow("Case study")
                    Text(study.title, style = MaterialTheme.typography.headlineSmall, color = skin.ink)
                    Text(study.summary, style = MaterialTheme.typography.bodyMedium, color = skin.steel, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Text("Read how it works →", style = MaterialTheme.typography.labelLarge, color = skin.accent)
                }
            }
        }

        item {
            CtaBand(
                title = "Have a project in mind?",
                text = "Tell me what you need — a website, support for your customers, or a tool that saves your team time. I usually reply within a day.",
                primary = "WhatsApp me",
                onPrimary = { Actions.whatsapp(context, site.owner) },
                secondary = "Send a message",
                onSecondary = { nav.navigate(ContactR) },
                icon = "fa-brands fa-whatsapp",
            )
        }
    }
}

/** A tool "tag" hanging on the pegboard, like the home page of the website. */
@Composable
private fun ToolPeg(name: String, icon: String, onClick: () -> Unit) {
    val skin = LocalSkin.current
    BbCard(onClick = onClick, color = skin.tagBg, padding = 14.dp) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(skin.steel.copy(alpha = 0.5f)))
        }
        IconTile(icon, size = 38.dp, iconSize = 15)
        Text(name, style = MaterialTheme.typography.titleSmall, color = skin.tagInk, maxLines = 2, minLines = 2)
        Text("Open →", style = MaterialTheme.typography.labelMedium, color = skin.accent, fontSize = 12.sp)
    }
}
