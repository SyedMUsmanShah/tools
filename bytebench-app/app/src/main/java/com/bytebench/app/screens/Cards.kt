package com.bytebench.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytebench.app.data.Content
import com.bytebench.app.data.FreeTool
import com.bytebench.app.data.Post
import com.bytebench.app.data.Product
import com.bytebench.app.data.Service
import com.bytebench.app.data.Stat
import com.bytebench.app.nav.PostR
import com.bytebench.app.nav.ProductR
import com.bytebench.app.nav.ServiceR
import com.bytebench.app.nav.ToolR
import com.bytebench.app.ui.BbCard
import com.bytebench.app.ui.Eyebrow
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.IconTile
import com.bytebench.app.ui.LocalNav
import com.bytebench.app.ui.theme.LocalSkin
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun ToolCard(tool: FreeTool, categoryName: String?, compact: Boolean = false) {
    val nav = LocalNav.current
    val skin = LocalSkin.current
    BbCard(onClick = { nav.navigate(ToolR(tool.slug)) }, padding = if (compact) 14.dp else 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(tool.icon, size = if (compact) 38.dp else 44.dp, iconSize = if (compact) 15 else 18)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (categoryName != null && !compact) Eyebrow(categoryName, skin.steel)
                Text(tool.name, style = MaterialTheme.typography.titleMedium, color = skin.ink, maxLines = 2)
            }
            FaIcon("fa-solid fa-chevron-right", size = 12.sp, color = skin.steel)
        }
        if (!compact) {
            Text(tool.summary, style = MaterialTheme.typography.bodyMedium, color = skin.steel, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PostRow(post: Post) {
    val nav = LocalNav.current
    val skin = LocalSkin.current
    BbCard(onClick = { nav.navigate(PostR(post.slug)) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (post.category != null) Eyebrow(post.category)
            Text("${niceDate(post.publishedAt)} · ${post.readingMinutes} min read", style = MaterialTheme.typography.labelMedium, color = skin.steel)
        }
        Text(post.title, style = MaterialTheme.typography.headlineSmall, color = skin.ink)
        if (post.excerpt.isNotBlank()) Text(post.excerpt, style = MaterialTheme.typography.bodyMedium, color = skin.steel, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ServiceCard(service: Service) {
    val nav = LocalNav.current
    val skin = LocalSkin.current
    BbCard(onClick = { nav.navigate(ServiceR(service.slug)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(service.icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(service.title, style = MaterialTheme.typography.titleLarge, color = skin.ink)
                if (!service.priceLabel.isNullOrBlank()) Text(service.priceLabel, style = MaterialTheme.typography.labelMedium, color = skin.gold)
            }
        }
        Text(service.summary, style = MaterialTheme.typography.bodyMedium, color = skin.steel)
        Text("Learn more →", style = MaterialTheme.typography.labelLarge, color = skin.accent)
    }
}

@Composable
fun ProductCard(p: Product) {
    val nav = LocalNav.current
    val skin = LocalSkin.current
    BbCard(onClick = { nav.navigate(ProductR(p.slug)) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriceTag(p)
            Text(p.typeLabel, style = MaterialTheme.typography.labelMedium, color = skin.steel, modifier = Modifier.padding(top = 3.dp))
        }
        Text(p.name, style = MaterialTheme.typography.titleLarge, color = skin.ink)
        if (p.tagline.isNotBlank()) Text(p.tagline, style = MaterialTheme.typography.bodyMedium, color = skin.steel)
    }
}

@Composable
fun PriceTag(p: Product) {
    val skin = LocalSkin.current
    val free = p.isFree
    Text(
        if (free) "Free" else (p.priceLabel ?: "Paid"),
        style = MaterialTheme.typography.labelMedium,
        color = if (free) skin.paper else skin.goldInk,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (free) skin.green else skin.gold)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** The home page numbers strip: "since" values count whole years, "count" reads live data. */
fun statValue(s: Stat, content: Content): String {
    val n = when {
        s.since != null -> runCatching {
            val start = YearMonth.parse(s.since).atDay(1)
            maxOf(1, ChronoUnit.YEARS.between(start, LocalDate.now()).toInt())
        }.getOrDefault(1)
        s.count == "free_tools" -> content.tools.size
        else -> s.value ?: 0
    }
    return if (s.since != null) "$n+${s.suffix}" else "$n${s.suffix}"
}

fun niceDate(iso: String): String = runCatching {
    LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
}.getOrDefault(iso)

fun categoryName(content: Content, slug: String): String? = content.toolCategories.firstOrNull { it.slug == slug }?.name
