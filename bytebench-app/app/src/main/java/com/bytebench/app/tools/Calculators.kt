package com.bytebench.app.tools

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytebench.app.BytebenchApp
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BigResult
import com.bytebench.app.ui.BtnStyle
import com.bytebench.app.ui.CardShape
import com.bytebench.app.ui.Dropdown
import com.bytebench.app.ui.ErrorText
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.Field
import com.bytebench.app.ui.Formula
import com.bytebench.app.ui.Note
import com.bytebench.app.ui.NumberField
import com.bytebench.app.ui.Segmented
import com.bytebench.app.ui.StatGrid
import com.bytebench.app.ui.theme.LocalSkin
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/** A tappable field that opens the system date picker. */
@Composable
fun DateField(label: String, date: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    PickerBox(label, date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)), "fa-solid fa-calendar", modifier) {
        DatePickerDialog(context, { _, y, m, d -> onChange(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }
}

@Composable
fun TimeField(label: String, time: LocalTime, onChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    PickerBox(label, time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)), "fa-solid fa-clock", modifier) {
        TimePickerDialog(context, { _, h, m -> onChange(LocalTime.of(h, m)) }, time.hour, time.minute, false).show()
    }
}

@Composable
private fun PickerBox(label: String, value: String, icon: String, modifier: Modifier, onClick: () -> Unit) {
    val skin = LocalSkin.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(skin.surface)
            .border(1.dp, skin.rule, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = skin.steel)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyLarge, color = skin.ink, modifier = Modifier.weight(1f))
            FaIcon(icon, size = 13.sp, color = skin.steel)
        }
    }
}

private fun plural(n: Long, w: String) = "$n $w" + if (n == 1L) "" else "s"

@Composable
fun AgeCalculator() {
    val prefs = BytebenchApp.instance.prefs
    var dob by remember { mutableStateOf(runCatching { LocalDate.parse(prefs.get("dob", "2000-01-01")) }.getOrDefault(LocalDate.of(2000, 1, 1))) }
    var on by remember { mutableStateOf(LocalDate.now()) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DateField("Date of birth", dob, { dob = it; prefs.set("dob", it.toString()) }, Modifier.weight(1f))
            DateField("Age on", on, { on = it }, Modifier.weight(1f))
        }
        if (dob.isAfter(on)) {
            ErrorText("The date of birth is after the “age on” date. Check both dates.")
            return@Column
        }
        var y = (on.year - dob.year).toLong()
        var m = (on.monthValue - dob.monthValue).toLong()
        var d = (on.dayOfMonth - dob.dayOfMonth).toLong()
        if (d < 0) { m--; d += on.minusMonths(1).lengthOfMonth() }
        if (m < 0) { y--; m += 12 }
        val days = ChronoUnit.DAYS.between(dob, on)
        fun bday(year: Int): LocalDate =
            if (dob.monthValue == 2 && dob.dayOfMonth == 29 && !java.time.Year.isLeap(year.toLong())) LocalDate.of(year, 3, 1)
            else LocalDate.of(year, dob.monthValue, dob.dayOfMonth)
        var next = bday(on.year)
        if (next.isBefore(on)) next = bday(on.year + 1)
        val left = ChronoUnit.DAYS.between(on, next)
        BigResult(
            "Your age",
            "${plural(y, "year")}, ${plural(m, "month")}, ${plural(d, "day")}",
            "Born on a ${dob.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}.",
        )
        StatGrid(
            listOf(
                Triple("Total months", Ft.num((y * 12 + m).toDouble(), 0), false),
                Triple("Total weeks", Ft.num((days / 7).toDouble(), 0), false),
                Triple("Total days", Ft.num(days.toDouble(), 0), false),
                Triple("Total hours", Ft.num((days * 24).toDouble(), 0), false),
                Triple("Next birthday in", if (left == 0L) "Today! 🎉" else plural(left, "day"), true),
            ),
        )
    }
}

@Composable
private fun PcRow(question: @Composable () -> Unit, answer: String, formula: String, color: Color = LocalSkin.current.ink) {
    val skin = LocalSkin.current
    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(skin.board).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        question()
        Text(answer, style = MaterialTheme.typography.headlineMedium, color = color)
        Formula(formula)
    }
}

@Composable
fun PercentageCalculator() {
    val skin = LocalSkin.current
    var a1 by rememberSaveable { mutableStateOf("15") }; var a2 by rememberSaveable { mutableStateOf("2000") }
    var b1 by rememberSaveable { mutableStateOf("45") }; var b2 by rememberSaveable { mutableStateOf("60") }
    var c1 by rememberSaveable { mutableStateOf("80") }; var c2 by rememberSaveable { mutableStateOf("100") }
    var d1 by rememberSaveable { mutableStateOf("1500") }; var d2 by rememberSaveable { mutableStateOf("10") }
    val v = Ft::value
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        run {
            val x = v(a1); val y = v(a2); val ok = !x.isNaN() && !y.isNaN()
            PcRow({
                Text("What is X% of Y?", style = MaterialTheme.typography.titleSmall, color = skin.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Percent", a1, { a1 = it }, Modifier.weight(1f), suffix = "%")
                    NumberField("of", a2, { a2 = it }, Modifier.weight(1f))
                }
            }, if (ok) Ft.num(x * y / 100, 4) else "—", if (ok) "${Ft.num(y)} × ${Ft.num(x)} ÷ 100" else "")
        }
        run {
            val x = v(b1); val y = v(b2); val ok = !x.isNaN() && !y.isNaN() && y != 0.0
            PcRow({
                Text("X is what percent of Y?", style = MaterialTheme.typography.titleSmall, color = skin.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Part", b1, { b1 = it }, Modifier.weight(1f))
                    NumberField("Whole", b2, { b2 = it }, Modifier.weight(1f))
                }
            }, if (ok) Ft.num(x / y * 100, 4) + "%" else "—", if (ok) "${Ft.num(x)} ÷ ${Ft.num(y)} × 100" else if (y == 0.0) "Can’t divide by zero" else "")
        }
        run {
            val x = v(c1); val y = v(c2); val ok = !x.isNaN() && !y.isNaN() && x != 0.0
            val ch = if (ok) (y - x) / abs(x) * 100 else 0.0
            PcRow({
                Text("Percentage change", style = MaterialTheme.typography.titleSmall, color = skin.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("From", c1, { c1 = it }, Modifier.weight(1f))
                    NumberField("To", c2, { c2 = it }, Modifier.weight(1f))
                }
            },
                if (ok) (if (ch > 0) "+" else "") + Ft.num(ch, 4) + "% " + (if (ch > 0) "increase" else if (ch < 0) "decrease" else "change") else "—",
                if (ok) "(${Ft.num(y)} − ${Ft.num(x)}) ÷ ${Ft.num(abs(x))} × 100" else if (x == 0.0) "The starting value can’t be zero" else "",
                if (ch > 0) skin.green else if (ch < 0) skin.danger else skin.ink,
            )
        }
        run {
            val x = v(d1); val y = v(d2); val ok = !x.isNaN() && !y.isNaN()
            PcRow({
                Text("Increase / decrease a number by X%", style = MaterialTheme.typography.titleSmall, color = skin.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Number", d1, { d1 = it }, Modifier.weight(1f))
                    NumberField("By", d2, { d2 = it }, Modifier.weight(1f), suffix = "%")
                }
            },
                if (ok) Ft.num(x * (1 + y / 100), 4) + "  /  " + Ft.num(x * (1 - y / 100), 4) else "—",
                if (ok) "Increased  /  decreased by ${Ft.num(x * y / 100, 4)}" else "",
            )
        }
    }
}

@Composable
fun BmiCalculator() {
    val prefs = BytebenchApp.instance.prefs
    val skin = LocalSkin.current
    var unit by remember { mutableStateOf(prefs.get("bmi-unit", "metric")) }
    var kg by rememberSaveable { mutableStateOf("70") }; var cm by rememberSaveable { mutableStateOf("170") }
    var lb by rememberSaveable { mutableStateOf("154") }; var ft by rememberSaveable { mutableStateOf("5") }; var inch by rememberSaveable { mutableStateOf("7") }
    val imp = unit == "imperial"
    val w = if (imp) Ft.value(lb) * 0.45359237 else Ft.value(kg)
    val h = if (imp) ((Ft.value(ft).takeIf { !it.isNaN() } ?: 0.0) * 12 + (Ft.value(inch).takeIf { !it.isNaN() } ?: 0.0)) * 0.0254 else Ft.value(cm) / 100
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Segmented(listOf("metric" to "Metric (kg, cm)", "imperial" to "Imperial (lb, ft, in)"), unit, { unit = it; prefs.set("bmi-unit", it) })
        if (imp) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Weight", lb, { lb = it }, Modifier.weight(1f), suffix = "lb")
            NumberField("Feet", ft, { ft = it }, Modifier.weight(1f), suffix = "ft")
            NumberField("Inches", inch, { inch = it }, Modifier.weight(1f), suffix = "in")
        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Weight", kg, { kg = it }, Modifier.weight(1f), suffix = "kg")
            NumberField("Height", cm, { cm = it }, Modifier.weight(1f), suffix = "cm")
        }
        if (!(w > 0) || !(h > 0.5) || h > 2.8) {
            BigResult("Your BMI", "—", "Enter your weight and height.")
        } else {
            val bmi = w / (h * h)
            val label = when { bmi < 18.5 -> "Underweight"; bmi < 25 -> "Healthy weight"; bmi < 30 -> "Overweight"; else -> "Obese" }
            val lo = 18.5 * h * h; val hi = 24.9 * h * h
            val range = if (imp) "${Ft.num(lo / 0.45359237, 0)}–${Ft.num(hi / 0.45359237, 0)} lb" else "${Ft.num(lo, 1)}–${Ft.num(hi, 1)} kg"
            BigResult("Your BMI", String.format(Locale.US, "%.1f", bmi), "$label. A healthy weight for your height is about $range.")
            BmiScale(bmi)
        }
        Note("BMI is a screening number for adults only. It doesn’t measure body fat and doesn’t suit children, pregnant women or very muscular people. This is not medical advice — please speak to a doctor about your health.")
    }
}

@Composable
private fun BmiScale(bmi: Double) {
    val skin = LocalSkin.current
    val bands = listOf(18.5f to Color(0xFF6AA6E8), 6.5f to Color(0xFF3FB97A), 5f to Color(0xFFE7B53C), 10f to Color(0xFFE46A5A))
    Column {
        Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.CenterStart) {
            Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(99.dp))) {
                bands.forEach { (w, c) -> Box(Modifier.weight(w).fillMaxHeight().background(c)) }
            }
            val frac = (bmi / 40).coerceIn(0.0, 1.0).toFloat()
            Row(Modifier.fillMaxWidth()) {
                if (frac > 0f) Spacer(Modifier.weight(frac))
                Box(Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(3.dp)).background(skin.ink))
                if (frac < 1f) Spacer(Modifier.weight(1f - frac))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            listOf("Underweight\n< 18.5" to 18.5f, "Healthy\n18.5–24.9" to 6.5f, "Over\n25–29.9" to 5f, "Obese\n30+" to 10f).forEach { (t, w) ->
                Text(t, style = MaterialTheme.typography.bodySmall, color = skin.steel, fontSize = 11.sp, modifier = Modifier.weight(w))
            }
        }
    }
}

private data class UnitDef(val code: String, val name: String, val factor: Double)

@Composable
fun UnitConverter() {
    val context = LocalContext.current
    val skin = LocalSkin.current
    val data = remember { GenData.tool(context, "unit-converter") }
    val cats = listOf("length" to "Length", "weight" to "Weight", "temperature" to "Temperature", "area" to "Area", "volume" to "Volume", "speed" to "Speed", "data" to "Data size")
    var cat by rememberSaveable { mutableStateOf("length") }
    val defs = data.obj("U").obj(cat)
    val units = remember(cat) {
        defs.arr("units").map { u ->
            val a = u.jsonArray
            UnitDef(a[0].jsonPrimitive.content, a[1].jsonPrimitive.content, a.getOrNull(2)?.jsonPrimitive?.content?.toDoubleOrNull() ?: 1.0)
        }
    }
    var from by rememberSaveable(cat) { mutableStateOf(data.obj("defaults").str(cat).ifEmpty { units.firstOrNull()?.code ?: "" }) }
    var value by rememberSaveable { mutableStateOf("1") }
    val v = Ft.value(value)
    val src = units.firstOrNull { it.code == from } ?: units.first()

    fun temp(x: Double, f: String, t: String): Double {
        val c = when (f) { "C" -> x; "F" -> (x - 32) * 5 / 9; else -> x - 273.15 }
        return when (t) { "C" -> c; "F" -> c * 9 / 5 + 32; else -> c + 273.15 }
    }
    fun fmt(n: Double): String {
        if (n.isNaN() || n.isInfinite()) return "—"
        val a = abs(n)
        if (a != 0.0 && (a < 1e-6 || a >= 1e15)) return String.format(Locale.US, "%.4e", n).replace("e", " × 10^")
        val p = String.format(Locale.US, "%.10g", n).toDouble()
        return Ft.num(p, if (a < 1) 8 else 4)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Segmented(cats, cat, { cat = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Value", value, { value = it }, Modifier.weight(1f))
            Dropdown("From", units.map { it.code to it.name }, from, { from = it }, Modifier.weight(1.3f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            units.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { u ->
                        val r = if (cat == "temperature") temp(v, from, u.code) else v * src.factor / u.factor
                        val isFrom = u.code == from
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(CardShape)
                                .background(if (isFrom) skin.gold.copy(alpha = 0.16f) else skin.board)
                                .clickable {
                                    val num = fmt(r).replace(",", "")
                                    if (!v.isNaN() && !num.contains("×")) num.toDoubleOrNull()?.let { value = Ft.num(it, 8).replace(",", "") }
                                    from = u.code
                                }
                                .padding(12.dp),
                        ) {
                            Text(if (v.isNaN()) "—" else fmt(r), style = MaterialTheme.typography.titleMedium, color = skin.ink, fontWeight = FontWeight.Bold)
                            Text(u.name, style = MaterialTheme.typography.bodySmall, color = skin.steel)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        val note = defs.str("note")
        if (note.isNotBlank()) Note(note)
        Note("Tap any result to convert from that unit.")
    }
}

@Composable
fun TimeZoneConverter() {
    val context = LocalContext.current
    val prefs = BytebenchApp.instance.prefs
    val skin = LocalSkin.current
    val aliases = remember {
        GenData.tool(context, "time-zone-converter").obj("aliases").entries.associate { it.key to it.value.jsonPrimitive.content }
    }
    val labelOf = remember { aliases.entries.groupBy({ it.value }, { it.key }).mapValues { it.value.first() } }
    val allZones = remember { (ZoneId.getAvailableZoneIds().filter { it.contains('/') && !it.startsWith("Etc") && !it.startsWith("SystemV") } + "UTC").sorted() }
    fun city(z: String) = labelOf[z] ?: z.substringAfterLast('/').replace('_', ' ')
    fun resolve(text: String): String? {
        val t = text.trim()
        if (t.isEmpty()) return null
        aliases[t]?.let { return it }
        val low = t.lowercase()
        allZones.firstOrNull { it.lowercase() == low }?.let { return it }
        aliases.keys.firstOrNull { it.lowercase().contains(low) }?.let { return aliases[it] }
        return allZones.firstOrNull { city(it).lowercase() == low || it.lowercase().contains("/" + low.replace(' ', '_')) }
    }

    val here = ZoneId.systemDefault().id
    var srcText by remember { mutableStateOf(prefs.get("tz-from", labelOf[here] ?: here)) }
    val src = resolve(srcText)
    val nowSrc = LocalDateTime.now(ZoneId.of(src ?: here))
    var date by remember { mutableStateOf(nowSrc.toLocalDate()) }
    var time by remember { mutableStateOf(nowSrc.toLocalTime().withSecond(0).withNano(0)) }
    val saved = prefs.get("tz-list", "").split('|').filter { it.isNotBlank() }
    val list = remember { mutableStateListOf<String>().apply { addAll(saved.ifEmpty { listOf("Asia/Karachi", "America/New_York", "America/Los_Angeles", "Europe/London", "Asia/Dubai") }) } }
    var add by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    fun persist() = prefs.set("tz-list", list.joinToString("|"))

    fun offStr(sec: Int): String {
        val m = sec / 60
        val s = if (m < 0) "−" else "+"
        val a = abs(m)
        return "UTC$s${a / 60}" + if (a % 60 != 0) ":" + (a % 60).toString().padStart(2, '0') else ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Date", date, { date = it }, Modifier.weight(1f))
            TimeField("Time", time, { time = it }, Modifier.weight(1f))
        }
        Field("In time zone", srcText, { srcText = it; if (resolve(it) != null) prefs.set("tz-from", it) }, placeholder = "City or zone, e.g. Lahore")
        ZoneSuggestions(srcText, aliases.keys.toList(), allZones, ::city) { srcText = it; prefs.set("tz-from", it) }
        BbButton("Use current time", {
            val n = LocalDateTime.now(ZoneId.of(src ?: here)); date = n.toLocalDate(); time = n.toLocalTime().withSecond(0).withNano(0)
        }, icon = "fa-solid fa-clock", style = BtnStyle.Ghost, small = true)
        if (src == null) {
            ErrorText("We couldn’t find that time zone. Try a big city nearby.")
            return@Column
        }
        val instant = LocalDateTime.of(date, time).atZone(ZoneId.of(src)).toInstant()
        val srcOff = ZoneId.of(src).rules.getOffset(instant).totalSeconds
        Note("${city(src)} is ${offStr(srcOff)} on this date.")
        val all = listOf(src) + list.filter { it != src }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            all.forEachIndexed { i, z ->
                val zone = ZoneId.of(z)
                val t = instant.atZone(zone)
                val off = zone.rules.getOffset(instant).totalSeconds
                val diff = (off - srcOff) / 3600.0
                val rel = if (i == 0) "Your input" else if (diff == 0.0) "Same time" else (if (diff > 0) "+" else "−") + Ft.num(abs(diff)) + " h"
                Row(
                    Modifier.fillMaxWidth().clip(CardShape).background(if (i == 0) skin.gold.copy(alpha = 0.14f) else skin.board).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(city(z), style = MaterialTheme.typography.titleSmall, color = skin.ink)
                        Text("${offStr(off)} · $rel", style = MaterialTheme.typography.bodySmall, color = skin.steel)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(t.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)), style = MaterialTheme.typography.titleLarge, color = skin.ink)
                        Text(t.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)), style = MaterialTheme.typography.bodySmall, color = skin.steel)
                    }
                    if (i == 0) Spacer(Modifier.width(48.dp)) else IconButton(onClick = { list.remove(z); persist() }) {
                        FaIcon("fa-solid fa-xmark", size = 14.sp, color = skin.steel, contentDescription = "Remove ${city(z)}")
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Add a city or time zone", add, { add = it }, Modifier.weight(1f), placeholder = "e.g. Toronto, Sydney, Riyadh")
            BbButton("Add", {
                val z = resolve(add)
                if (z == null) err = "We couldn’t find “$add”. Try a big city nearby."
                else { err = null; if (z !in list) list.add(z); persist(); add = "" }
            }, small = true)
        }
        ZoneSuggestions(add, aliases.keys.toList(), allZones, ::city) { pick ->
            resolve(pick)?.let { z -> if (z !in list) list.add(z); persist() }
            add = ""
        }
        ErrorText(err)
        Note("Daylight saving time is applied automatically for the date you choose.")
    }
}

/** Up to five matching places under a time-zone field. */
@Composable
private fun ZoneSuggestions(text: String, aliases: List<String>, zones: List<String>, city: (String) -> String, onPick: (String) -> Unit) {
    val t = text.trim().lowercase()
    if (t.length < 2) return
    val hits = (aliases.filter { it.lowercase().contains(t) } + zones.filter { city(it).lowercase().contains(t) || it.lowercase().contains(t) })
        .distinct().filter { it.lowercase() != t }.take(5)
    if (hits.isEmpty()) return
    com.bytebench.app.ui.FlowPills { hits.forEach { h -> com.bytebench.app.ui.Pill(if (h.contains('/')) "${city(h)} · $h" else h, onClick = { onPick(h) }) } }
}
