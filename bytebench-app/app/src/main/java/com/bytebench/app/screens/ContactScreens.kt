package com.bytebench.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bytebench.app.data.Booking
import com.bytebench.app.ui.Actions
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BbCard
import com.bytebench.app.ui.BtnStyle
import com.bytebench.app.ui.Dropdown
import com.bytebench.app.ui.ErrorText
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.Field
import com.bytebench.app.ui.FlowPills
import com.bytebench.app.ui.IconTile
import com.bytebench.app.ui.LocalContent
import com.bytebench.app.ui.Note
import com.bytebench.app.ui.Pill
import com.bytebench.app.ui.ScreenScaffold
import com.bytebench.app.ui.SectionHeader
import com.bytebench.app.ui.Segmented
import com.bytebench.app.ui.TextArea
import com.bytebench.app.ui.theme.LocalSkin
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val emailRe = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

@Composable
fun ContactScreen() {
    val content = LocalContent.current
    val skin = LocalSkin.current
    val context = LocalContext.current
    val owner = content.site.owner
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        error = when {
            name.isBlank() -> "Please tell me your name."
            message.trim().length < 10 -> "Please write a little more in your message."
            email.isNotBlank() && !emailRe.matches(email.trim()) -> "That email address doesn’t look right."
            else -> null
        }
        return error == null
    }
    fun body() = buildString {
        append(message.trim())
        append("\n\n— ").append(name.trim())
        if (email.isNotBlank()) append(" (").append(email.trim()).append(")")
    }

    ScreenScaffold("Contact") {
        item {
            Text("Let’s talk", style = MaterialTheme.typography.displaySmall, color = skin.ink)
        }
        item {
            Text(
                "Tell me what you need — a website, support for your customers, or a tool that saves your team time. I usually reply within a day.",
                style = MaterialTheme.typography.bodyLarge, color = skin.steel,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ContactLine("fa-brands fa-whatsapp", "WhatsApp", owner.whatsappDisplay) { Actions.whatsapp(context, owner) }
                ContactLine("fa-solid fa-envelope", "Email", owner.email) { Actions.email(context, owner.email) }
                ContactLine("fa-solid fa-location-dot", "Based in", owner.location, null)
            }
        }
        item { SectionHeader("Send a message") }
        item {
            BbCard {
                Field("Your name", name, { name = it })
                Field("Your email (optional)", email, { email = it }, keyboard = KeyboardType.Email)
                Field("Subject", subject, { subject = it }, placeholder = "e.g. New website for my shop")
                TextArea("Message", message, { message = it }, minLines = 5)
                ErrorText(error)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BbButton("Send on WhatsApp", {
                        if (validate()) Actions.whatsapp(context, owner, (if (subject.isNotBlank()) "*${subject.trim()}*\n\n" else "") + body())
                    }, icon = "fa-brands fa-whatsapp", style = BtnStyle.Gold)
                    BbButton("Email", {
                        if (validate()) Actions.email(context, owner.email, subject.ifBlank { "Message from the ${content.site.name} app" }, body())
                    }, icon = "fa-solid fa-paper-plane", style = BtnStyle.Ghost)
                }
                Note("Your message opens in WhatsApp or your email app, so you can check it before sending.")
            }
        }
    }
}

@Composable
private fun ContactLine(icon: String, label: String, value: String, onClick: (() -> Unit)?) {
    val skin = LocalSkin.current
    BbCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, size = 40.dp, iconSize = 16)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = skin.steel)
                Text(value, style = MaterialTheme.typography.titleMedium, color = skin.ink)
            }
            if (onClick != null) FaIcon("fa-solid fa-arrow-up-right-from-square", color = skin.steel)
        }
    }
}

/** Free call slots, worked out the same way as the website (app/Support/Booking.php). */
fun bookingSlots(b: Booking, now: Instant = Instant.now()): List<ZonedDateTime> {
    val zone = runCatching { ZoneId.of(b.timezone) }.getOrDefault(ZoneId.of("Asia/Karachi"))
    val earliest = now.plusSeconds(b.minNoticeHours * 3600L)
    val from = LocalTime.parse(b.from)
    val to = LocalTime.parse(b.to)
    val out = mutableListOf<ZonedDateTime>()
    val today = now.atZone(zone).toLocalDate()
    for (i in 0..b.daysAhead) {
        val day = today.plusDays(i.toLong())
        if (day.dayOfWeek.value !in b.days) continue
        var t = day.atTime(from).atZone(zone)
        var end = day.atTime(to).atZone(zone)
        if (!end.isAfter(t)) end = end.plusDays(1)   // hours that run past midnight
        while (!t.plusMinutes(b.minutes.toLong()).isAfter(end)) {
            if (t.toInstant().isAfter(earliest)) out += t
            t = t.plusMinutes(b.everyMinutes.toLong())
        }
    }
    return out
}

@Composable
fun BookScreen(topic: String) {
    val content = LocalContent.current
    val skin = LocalSkin.current
    val context = LocalContext.current
    val owner = content.site.owner
    val b = content.site.booking
    val local = remember { ZoneId.systemDefault() }
    val ownerZone = remember(b.timezone) { runCatching { ZoneId.of(b.timezone) }.getOrDefault(ZoneId.of("Asia/Karachi")) }
    val slots = remember(b) { bookingSlots(b).map { it.withZoneSameInstant(local) } }
    val days = slots.groupBy { it.toLocalDate() }
    val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    var day by rememberSaveable { mutableStateOf(days.keys.firstOrNull()?.toString() ?: "") }
    var slot by rememberSaveable { mutableStateOf<Long?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var whatsapp by rememberSaveable { mutableStateOf("") }
    val topics = listOf("Website development", "Customer support", "Business support", "Sales & operations", "Tools & automation", "Something else")
    var subject by rememberSaveable { mutableStateOf(topics.firstOrNull { topic.isNotBlank() && it.startsWith(topic.take(6), ignoreCase = true) } ?: topics[0]) }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun request(): String? {
        val s = slots.firstOrNull { it.toEpochSecond() == slot }
        error = when {
            s == null -> "Pick a time for the call."
            name.isBlank() -> "Please tell me your name."
            email.isBlank() || !emailRe.matches(email.trim()) -> "Please add a valid email so I can send the invite."
            else -> null
        }
        if (error != null || s == null) return null
        val mine = s.withZoneSameInstant(ownerZone)
        return buildString {
            append("Hi ${owner.shortName}, I’d like to book a free ${b.minutes}-minute call.\n\n")
            append("Time: ${s.format(dayFmt)}, ${s.format(timeFmt)} (my time, ${local.id})\n")
            append("Your time: ${mine.format(dayFmt)}, ${mine.format(timeFmt)} (${ownerZone.id})\n")
            append("Topic: $subject\n")
            append("Name: ${name.trim()}\nEmail: ${email.trim()}\n")
            if (whatsapp.isNotBlank()) append("WhatsApp: ${whatsapp.trim()}\n")
            if (notes.isNotBlank()) append("\nNotes: ${notes.trim()}\n")
        }
    }

    ScreenScaffold("Book a free call") {
        item { Text("Book a free ${b.minutes}-minute call", style = MaterialTheme.typography.displaySmall, color = skin.ink) }
        item {
            Text(
                "Pick a time that suits you. We’ll talk about your goals, and I’ll tell you honestly whether I can help. Times are shown in your time zone (${local.id}).",
                style = MaterialTheme.typography.bodyLarge, color = skin.steel,
            )
        }
        if (slots.isEmpty()) {
            item { Note("No free times right now — send a message instead and we’ll find a time.") }
        } else {
            item { SectionHeader("1. Choose a day") }
            item {
                Segmented(days.keys.map { it.toString() to it.format(dayFmt) }, day, { day = it; slot = null })
            }
            item { SectionHeader("2. Choose a time") }
            item {
                FlowPills {
                    days.entries.firstOrNull { it.key.toString() == day }?.value?.forEach { t ->
                        Pill(t.format(timeFmt), selected = slot == t.toEpochSecond(), onClick = { slot = t.toEpochSecond() })
                    }
                }
            }
        }
        item { SectionHeader("3. Your details") }
        item {
            BbCard {
                Field("Your name", name, { name = it })
                Field("Email", email, { email = it }, keyboard = KeyboardType.Email)
                Field("WhatsApp number (optional)", whatsapp, { whatsapp = it }, keyboard = KeyboardType.Phone, placeholder = "+92 300 1234567")
                Dropdown("What’s it about?", topics.map { it to it }, subject, { subject = it })
                TextArea("Anything I should know? (optional)", notes, { notes = it }, minLines = 3)
                ErrorText(error)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BbButton("Request on WhatsApp", { request()?.let { Actions.whatsapp(context, owner, it) } }, icon = "fa-brands fa-whatsapp", style = BtnStyle.Gold)
                    BbButton("Email", { request()?.let { Actions.email(context, owner.email, "Call request: $subject", it) } }, style = BtnStyle.Ghost)
                }
                Note("I’ll confirm the time and send a call link. Calls are on weekdays, ${b.from}–${b.to} Pakistan time.")
            }
        }
    }
}
