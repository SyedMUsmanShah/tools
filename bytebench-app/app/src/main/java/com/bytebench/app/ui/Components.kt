@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.bytebench.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytebench.app.data.Content
import com.bytebench.app.nav.Route
import com.bytebench.app.ui.theme.LocalSkin

/** What every screen needs to move around the app. */
interface Navigator {
    fun navigate(route: Route)
    fun back()
    /** Opens a link from content: site paths go to screens, everything else to the browser. */
    fun openLink(url: String)
}

val LocalNav = staticCompositionLocalOf<Navigator> { error("No navigator") }
val LocalContent = staticCompositionLocalOf<Content> { error("No content") }

val CardShape = RoundedCornerShape(12.dp)
val CardShapeLg = RoundedCornerShape(18.dp)

/** A screen with a top bar and a scrolling list body. */
@Composable
fun ScreenScaffold(
    title: String,
    showBack: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
    spacing: Dp = 16.dp,
    content: LazyListScope.() -> Unit,
) {
    val skin = LocalSkin.current
    val nav = LocalNav.current
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        containerColor = skin.paper,
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { nav.back() }) { FaIcon("fa-solid fa-arrow-left", contentDescription = "Back", size = 18.sp) }
                    }
                },
                actions = actions,
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = skin.paper,
                    scrolledContainerColor = skin.surface,
                    titleContentColor = skin.ink,
                    navigationIconContentColor = skin.ink,
                    actionIconContentColor = skin.ink,
                ),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(inner)
                .imePadding(),
            contentPadding = PaddingValues(
                start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                top = inner.calculateTopPadding() + contentPadding.calculateTopPadding(),
                bottom = inner.calculateBottomPadding() + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

@Composable
fun BbCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = LocalSkin.current.surface,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = LocalSkin.current
    val base = modifier
        .fillMaxWidth()
        .clip(CardShape)
        .background(color)
        .border(1.dp, skin.rule, CardShape)
    Column(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun IconTile(icon: String, size: Dp = 44.dp, iconSize: Int = 18, bg: Color = LocalSkin.current.iconBg, fg: Color = LocalSkin.current.iconInk) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) { FaIcon(icon, size = iconSize.sp, color = fg) }
}

enum class BtnStyle { Primary, Ghost, Gold, Hero }

@Composable
fun BbButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    style: BtnStyle = BtnStyle.Primary,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val skin = LocalSkin.current
    val pad = if (small) PaddingValues(horizontal = 14.dp, vertical = 6.dp) else PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    val label: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            FaIcon(icon, size = if (small) 13.sp else 15.sp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge, maxLines = 1)
    }
    val shape = RoundedCornerShape(10.dp)
    when (style) {
        BtnStyle.Ghost -> OutlinedButton(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, contentPadding = pad,
            border = BorderStroke(1.dp, skin.rule),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = skin.ink),
            content = label,
        )
        else -> {
            val (bg, fg) = when (style) {
                BtnStyle.Gold -> skin.gold to skin.goldInk
                BtnStyle.Hero -> skin.heroBtn to skin.heroBtnInk
                else -> if (skin.dark) skin.accent to skin.accentInk else skin.ink to skin.paper
            }
            Button(
                onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, contentPadding = pad,
                colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg, disabledContainerColor = skin.board, disabledContentColor = skin.steel),
                content = label,
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    val skin = LocalSkin.current
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = skin.ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = skin.steel, modifier = Modifier.padding(top = 4.dp))
        }
        if (action != null && onAction != null) {
            Text(
                "$action →",
                style = MaterialTheme.typography.labelLarge,
                color = skin.accent,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}

@Composable
fun Eyebrow(text: String, color: Color = LocalSkin.current.gold) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun Pill(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null, icon: String? = null) {
    val skin = LocalSkin.current
    val bg = if (selected) (if (skin.dark) skin.accent else skin.ink) else skin.board
    val fg = if (selected) (if (skin.dark) skin.accentInk else skin.paper) else skin.ink
    Row(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { FaIcon(icon, size = 12.sp, color = fg); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/** Horizontally scrolling single-choice pills, like the site's segmented buttons (.ft-seg). */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) -> Pill(label, selected = value == selected, onClick = { onSelect(value) }) }
    }
}

@Composable
fun FlowPills(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun fieldColors() = LocalSkin.current.let { s ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = s.accent, unfocusedBorderColor = s.rule,
        focusedContainerColor = s.surface, unfocusedContainerColor = s.surface,
        focusedTextColor = s.ink, unfocusedTextColor = s.ink,
        focusedLabelColor = s.accent, unfocusedLabelColor = s.steel,
        cursorColor = s.accent, focusedPlaceholderColor = s.steel, unfocusedPlaceholderColor = s.steel,
    )
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    suffix: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    mono: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it, color = LocalSkin.current.steel) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        textStyle = MaterialTheme.typography.bodyLarge.let { if (mono) it.copy(fontFamily = FontFamily.Monospace) else it },
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, suffix: String? = null, placeholder: String? = null) =
    Field(label, value, { onChange(it.filter { c -> c.isDigit() || c == '.' || c == '-' || c == ',' }) }, modifier, placeholder, suffix, KeyboardType.Decimal)

@Composable
fun TextArea(label: String, value: String, onChange: (String) -> Unit, minLines: Int = 6, placeholder: String? = null, modifier: Modifier = Modifier) =
    Field(label, value, onChange, modifier, placeholder, singleLine = false, minLines = minLines)

/** A select box: a read-only field that opens a menu. */
@Composable
fun <T> Dropdown(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(skin.surface)
                .border(1.dp, skin.rule, RoundedCornerShape(10.dp))
                .clickable { open = true }
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = skin.steel)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    options.firstOrNull { it.first == selected }?.second ?: "",
                    style = MaterialTheme.typography.bodyLarge, color = skin.ink,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                FaIcon("fa-solid fa-chevron-down", size = 12.sp, color = skin.steel)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = skin.surface) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, color = skin.ink, fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { open = false; onSelect(value) },
                )
            }
        }
    }
}

@Composable
fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val skin = LocalSkin.current
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable { onChange(!checked) }.padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, onCheckedChange = onChange, colors = CheckboxDefaults.colors(checkedColor = skin.accent, checkmarkColor = skin.accentInk, uncheckedColor = skin.steel))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = skin.ink)
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, steps: Int = 0) {
    val skin = LocalSkin.current
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = skin.ink)
        Slider(
            value = value, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = skin.gold, activeTrackColor = skin.gold, inactiveTrackColor = skin.board),
        )
    }
}

/** A small statistic tile (.ft-stat). [highlight] gives it the gold border. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val skin = LocalSkin.current
    Column(
        modifier
            .clip(CardShape)
            .background(if (highlight) skin.gold.copy(alpha = if (skin.dark) 0.16f else 0.14f) else skin.board)
            .then(if (highlight) Modifier.border(1.dp, skin.gold.copy(alpha = 0.6f), CardShape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = skin.steel)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = skin.ink, maxLines = 2)
    }
}

/** Stat tiles in a two-column grid. */
@Composable
fun StatGrid(stats: List<Triple<String, String, Boolean>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (l, v, h) -> StatTile(l, v, Modifier.weight(1f), h) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** The big answer box (.ft-big). */
@Composable
fun BigResult(label: String, value: String, sub: String? = null, valueColor: Color = LocalSkin.current.ink) {
    val skin = LocalSkin.current
    Column(
        Modifier.fillMaxWidth().clip(CardShapeLg).background(skin.board).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = skin.steel)
        Text(value, style = MaterialTheme.typography.displaySmall, color = valueColor)
        if (!sub.isNullOrBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = skin.steel)
    }
}

/** A progress meter (.ft-meter): label on the left, count on the right, a bar below. */
@Composable
fun Meter(label: String, right: String, fraction: Float, state: MeterState = MeterState.Ok, height: Dp = 8.dp) {
    val skin = LocalSkin.current
    val color = when (state) { MeterState.Ok -> skin.green; MeterState.Warn -> skin.gold; MeterState.Bad -> skin.danger }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodySmall, color = skin.ink, modifier = Modifier.weight(1f))
            Text(right, style = MaterialTheme.typography.bodySmall, color = skin.steel)
        }
        Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(99.dp)).background(skin.board)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(RoundedCornerShape(99.dp)).background(color))
        }
    }
}

enum class MeterState { Ok, Warn, Bad }

@Composable
fun Note(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = LocalSkin.current.steel, modifier = modifier)

@Composable
fun ErrorText(text: String?) {
    if (!text.isNullOrBlank()) Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalSkin.current.danger)
}

@Composable
fun Formula(text: String) {
    if (text.isNotBlank()) {
        val skin = LocalSkin.current
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = skin.steel,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(skin.board).padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** A list of generated results, each with its own Copy button (.ft-list). */
@Composable
fun CopyList(items: List<String>, trailing: (@Composable (Int, String) -> Unit)? = null) {
    val skin = LocalSkin.current
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().clip(CardShape).border(1.dp, skin.rule, CardShape).background(skin.surface)) {
        items.forEachIndexed { i, t ->
            if (i > 0) HorizontalDivider(color = skin.rule)
            Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t, style = MaterialTheme.typography.bodyMedium, color = skin.ink)
                    trailing?.invoke(i, t)
                }
                IconButton(onClick = { Actions.copy(context, t) }) { FaIcon("fa-solid fa-copy", size = 15.sp, color = skin.steel, contentDescription = "Copy") }
            }
        }
    }
}

/** The "Need this done for you?" box under each tool, and other call-to-action bands. */
@Composable
fun CtaBand(title: String, text: String, primary: String, onPrimary: () -> Unit, secondary: String? = null, onSecondary: (() -> Unit)? = null, icon: String? = null) {
    val skin = LocalSkin.current
    Surface(color = skin.bandBg, shape = CardShapeLg, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) IconTile(icon, bg = skin.gold, fg = skin.goldInk)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = skin.bandInk)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = skin.bandMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                BbButton(primary, onPrimary, style = BtnStyle.Gold)
                if (secondary != null && onSecondary != null) {
                    OutlinedButton(
                        onClick = onSecondary,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, skin.bandMuted.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = skin.bandInk),
                    ) { Text(secondary, style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
    }
}

/** A screen-wide placeholder for empty lists. */
@Composable
fun EmptyState(icon: String, title: String, text: String, action: (@Composable () -> Unit)? = null) {
    val skin = LocalSkin.current
    Column(
        Modifier.fillMaxWidth().clip(CardShapeLg).background(skin.board).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconTile(icon, size = 56.dp, iconSize = 22)
        Text(title, style = MaterialTheme.typography.titleLarge, color = skin.ink)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = skin.steel)
        action?.invoke()
    }
}
