package com.bytebench.app.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytebench.app.BytebenchApp
import com.bytebench.app.ui.BbButton
import com.bytebench.app.ui.BbCard
import com.bytebench.app.ui.BigResult
import com.bytebench.app.ui.BtnStyle
import com.bytebench.app.ui.Dropdown
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.Field
import com.bytebench.app.ui.Formula
import com.bytebench.app.ui.Meter
import com.bytebench.app.ui.Note
import com.bytebench.app.ui.NumberField
import com.bytebench.app.ui.Segmented
import com.bytebench.app.ui.StatGrid
import com.bytebench.app.ui.TextArea
import com.bytebench.app.ui.theme.LocalSkin
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

val CURRENCIES = listOf(
    "Rs " to "Rs (PKR)", "$" to "$ (USD)", "£" to "£ (GBP)", "€" to "€ (EUR)",
    "AED " to "AED", "SAR " to "SAR", "₹" to "₹ (INR)", "" to "No symbol",
)

@Composable
private fun rememberCurrency(): Pair<String, (String) -> Unit> {
    val prefs = BytebenchApp.instance.prefs
    var cur by remember { mutableStateOf(prefs.get("cur", "Rs ")) }
    return cur to { v: String -> cur = v; prefs.set("cur", v) }
}

private fun money(cur: String, n: Double, dp: Int): String =
    if (n.isNaN() || n.isInfinite()) "—" else (if (n < 0) "−" else "") + cur.replace(' ', ' ') + Ft.fixed(abs(n), dp)

@Composable
fun ProfitMarginCalculator() {
    val (cur, setCur) = rememberCurrency()
    var mode by rememberSaveable { mutableStateOf("price") }
    var cost by rememberSaveable { mutableStateOf("800") }
    var price by rememberSaveable { mutableStateOf("1000") }
    var target by rememberSaveable { mutableStateOf("30") }
    var qty by rememberSaveable { mutableStateOf("1") }
    val c = Ft.value(cost)
    val q = max(1.0, Ft.value(qty).takeIf { !it.isNaN() } ?: 1.0)
    fun m(n: Double) = money(cur, n, if (abs(n) >= 1000) 0 else 2)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Segmented(listOf("price" to "I know my selling price", "target" to "Find price for a target margin"), mode, { mode = it })
        Dropdown("Currency", CURRENCIES, cur, setCur)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Cost per item", cost, { cost = it }, Modifier.weight(1f))
            if (mode == "price") NumberField("Selling price", price, { price = it }, Modifier.weight(1f))
            else NumberField("Target margin", target, { target = it }, Modifier.weight(1f), suffix = "%")
        }
        NumberField("Quantity sold", qty, { qty = it })
        val t = Ft.value(target)
        if (mode == "target" && !(t < 100)) {
            Formula("A margin must be below 100%.")
            return@Column
        }
        val p = if (mode == "target") c / (1 - t / 100) else Ft.value(price)
        val profit = p - c
        val tiles = mutableListOf<Triple<String, String, Boolean>>()
        if (mode == "target") tiles += Triple("Selling price", m(p), true)
        tiles += Triple("Profit per item", m(profit), false)
        tiles += Triple("Profit margin", if (p != 0.0) Ft.num(profit / p * 100, 2) + "%" else "—", false)
        tiles += Triple("Markup", if (c != 0.0) Ft.num(profit / c * 100, 2) + "%" else "—", false)
        tiles += Triple("Total revenue", m(p * q), false)
        if (mode == "price") tiles += Triple("Total profit", m(profit * q), true)
        StatGrid(tiles)
        Formula(
            if (mode == "target") "Price = cost ÷ (1 − margin) = ${m(c)} ÷ ${Ft.num(1 - t / 100, 4)} = ${m(p)}"
            else "Margin = profit ÷ price · Markup = profit ÷ cost",
        )
    }
}

@Composable
fun LoanEmiCalculator() {
    val skin = LocalSkin.current
    val (cur, setCur) = rememberCurrency()
    val saver = rememberFileSaver()
    var amount by rememberSaveable { mutableStateOf("2500000") }
    var rate by rememberSaveable { mutableStateOf("18") }
    var period by rememberSaveable { mutableStateOf("5") }
    var unit by rememberSaveable { mutableStateOf("y") }
    var showTable by rememberSaveable { mutableStateOf(false) }
    fun m(n: Double) = money(cur, n, if (n >= 1000) 0 else 2)

    val p = Ft.value(amount)
    val r0 = Ft.value(rate)
    val n = (Ft.value(period) * if (unit == "y") 12 else 1).roundToInt()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NumberField("Loan amount", amount, { amount = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Rate (yearly)", rate, { rate = it }, Modifier.weight(1f), suffix = "%")
            NumberField("Period", period, { period = it }, Modifier.weight(1f))
            Dropdown("Unit", listOf("y" to "years", "m" to "months"), unit, { unit = it }, Modifier.weight(0.9f))
        }
        Dropdown("Currency", CURRENCIES, cur, setCur)
        if (!(p > 0) || n <= 0 || !(r0 >= 0)) {
            BigResult("Monthly installment (EMI)", "—", "Enter the amount, rate and period.")
            return@Column
        }
        val r = r0 / 1200
        val emi = if (r != 0.0) p * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1) else p / n
        val total = emi * n
        val interest = total - p
        val sched = remember(p, r, n) {
            var bal = p
            (1..n).map { i ->
                val int = bal * r
                val pr = emi - int
                bal = max(0.0, bal - pr)
                doubleArrayOf(i.toDouble(), emi, pr, int, bal)
            }
        }
        BigResult("Monthly installment (EMI)", m(emi), "for $n months at ${Ft.num(r0)}% a year")
        StatGrid(listOf(Triple("Total interest", m(interest), false), Triple("Total payment", m(total), false)))
        Meter(
            "Principal ${Ft.num(p / total * 100, 1)}% · Interest ${Ft.num(interest / total * 100, 1)}%",
            "", (p / total).toFloat(), height = 12.dp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton(if (showTable) "Hide schedule" else "Show schedule", { showTable = !showTable }, icon = "fa-solid fa-table", style = BtnStyle.Ghost, small = true)
            BbButton("Save CSV", {
                val csv = "Month,Payment,Principal,Interest,Balance\n" + sched.joinToString("\n") { s ->
                    "${s[0].toInt()}," + (1..4).joinToString(",") { String.format(Locale.US, "%.2f", s[it]) }
                }
                saver.save(OutFile("loan-schedule.csv", "text/csv", csv.toByteArray()))
            }, icon = "fa-solid fa-download", style = BtnStyle.Ghost, small = true)
        }
        if (showTable) {
            Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                val widths = listOf(52.dp, 90.dp, 90.dp, 90.dp, 100.dp)
                val head = listOf("Month", "Payment", "Principal", "Interest", "Balance")
                Row(Modifier.background(skin.board).padding(vertical = 6.dp)) {
                    head.forEachIndexed { i, h -> Text(h, Modifier.width(widths[i]).padding(horizontal = 6.dp), style = MaterialTheme.typography.labelMedium, color = skin.steel) }
                }
                sched.forEach { s ->
                    HorizontalDivider(color = skin.rule)
                    Row(Modifier.padding(vertical = 5.dp)) {
                        s.forEachIndexed { i, v ->
                            Text(if (i == 0) v.toInt().toString() else Ft.fixed(v, 0), Modifier.width(widths[i]).padding(horizontal = 6.dp), style = MaterialTheme.typography.bodySmall, color = skin.ink)
                        }
                    }
                }
            }
        }
        Note("This is an estimate. Banks may add processing fees, insurance or taxes — confirm the final figures with your lender.")
    }
}

@Composable
fun DiscountTaxCalculator() {
    val (cur, setCur) = rememberCurrency()
    var mode by rememberSaveable { mutableStateOf("discount") }
    var price by rememberSaveable { mutableStateOf("5000") }
    var d1 by rememberSaveable { mutableStateOf("20") }
    var d2 by rememberSaveable { mutableStateOf("") }
    var tax by rememberSaveable { mutableStateOf("18") }
    fun v(s: String) = Ft.value(s).takeIf { !it.isNaN() } ?: 0.0
    fun m(n: Double) = money(cur, n, 2)
    val p = v(price)
    val t = v(tax)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Segmented(listOf("discount" to "Discount", "add" to "Add tax", "remove" to "Remove tax"), mode, { mode = it })
        Dropdown("Currency", CURRENCIES, cur, setCur)
        NumberField(when (mode) { "discount" -> "Original price"; "add" -> "Price before tax"; else -> "Price including tax" }, price, { price = it })
        if (mode == "discount") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Discount", d1, { d1 = it }, Modifier.weight(1f), suffix = "%")
            NumberField("Extra discount", d2, { d2 = it }, Modifier.weight(1f), suffix = "%", placeholder = "0")
        } else NumberField("Tax rate", tax, { tax = it }, suffix = "%")
        when (mode) {
            "discount" -> {
                val a = min(100.0, v(d1)); val b = min(100.0, v(d2))
                val after = p * (1 - a / 100) * (1 - b / 100)
                BigResult("Price after discount", m(after), if (b != 0.0) "${Ft.num(a)}% off, then another ${Ft.num(b)}% off the reduced price." else null)
                StatGrid(listOf(Triple("You save", m(p - after), false), Triple("Total discount", Ft.num(if (p != 0.0) (p - after) / p * 100 else 0.0, 2) + "%", false)))
            }
            "add" -> {
                BigResult("Price including tax", m(p * (1 + t / 100)), "${Ft.num(p)} × ${Ft.num(1 + t / 100, 4)}")
                StatGrid(listOf(Triple("Tax amount", m(p * t / 100), false), Triple("Price before tax", m(p), false)))
            }
            else -> {
                val base = p / (1 + t / 100)
                BigResult("Price before tax", m(base), "${Ft.num(p)} ÷ ${Ft.num(1 + t / 100, 4)}")
                StatGrid(listOf(Triple("Tax included", m(p - base), false), Triple("Price including tax", m(p), false)))
            }
        }
        Note("The tax rate starts at 18% — change it to the rate that applies to your product, service and location.")
    }
}

/* ───────────────────────── Invoice generator ───────────────────────── */

@Serializable
data class InvItem(val d: String = "", val q: String = "1", val p: String = "")

@Serializable
data class Invoice(
    val cur: String = "Rs ",
    val biz: String = "",
    val from: String = "",
    val no: String = "INV-0001",
    val date: String = LocalDate.now().toString(),
    val due: String = LocalDate.now().plusDays(14).toString(),
    val client: String = "",
    val caddr: String = "",
    val items: List<InvItem> = listOf(InvItem("Website design and development", "1", "50000"), InvItem("Hosting setup (1 year)", "1", "8000")),
    val disc: String = "0",
    val taxLabel: String = "Tax",
    val tax: String = "0",
    val notes: String = "",
    val logo: String? = null,   // small JPEG, base64
)

private val invJson = Json { ignoreUnknownKeys = true }
private val INV_CURRENCIES = listOf("Rs " to "Rs (PKR)", "$" to "$ (USD)", "£" to "£ (GBP)", "€" to "€ (EUR)", "AED " to "AED", "SAR " to "SAR", "INR " to "INR", "CAD $" to "CAD $", "AUD $" to "AUD $")

private fun invMoney(cur: String, n: Double) = cur + String.format(Locale.US, "%,.2f", n)
private fun num(s: String) = s.replace(",", "").toDoubleOrNull() ?: 0.0
private fun niceDate(iso: String) = runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)) }.getOrDefault(iso)

private data class Totals(val sub: Double, val disc: Double, val tax: Double, val total: Double)

private fun totals(inv: Invoice): Totals {
    val sub = inv.items.sumOf { num(it.q) * num(it.p) }
    val disc = sub * min(100.0, num(inv.disc)) / 100
    val tax = (sub - disc) * num(inv.tax) / 100
    return Totals(sub, disc, tax, sub - disc + tax)
}

@Composable
fun InvoiceGenerator() {
    val context = LocalContext.current
    val prefs = BytebenchApp.instance.prefs
    val skin = LocalSkin.current
    val saver = rememberFileSaver()
    var inv by remember {
        mutableStateOf(runCatching { invJson.decodeFromString(Invoice.serializer(), prefs.get("invoice")) }.getOrNull() ?: Invoice())
    }
    fun update(f: (Invoice) -> Invoice) { inv = f(inv); prefs.set("invoice", invJson.encodeToString(Invoice.serializer(), inv)) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val bmp = runCatching { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }.getOrNull()
            if (bmp != null) {
                val h = min(160, bmp.height)
                val w = (bmp.width * h / bmp.height.toFloat()).roundToInt().coerceAtLeast(1)
                val small = Bitmap.createScaledBitmap(bmp, w, h, true)
                val out = ByteArrayOutputStream()
                // white background so transparent logos look right in the PDF
                val flat = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { Canvas(it).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(small, 0f, 0f, null) } }
                flat.compress(Bitmap.CompressFormat.JPEG, 90, out)
                update { it.copy(logo = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)) }
            }
        }
    }
    val t = totals(inv)
    fun m(n: Double) = invMoney(inv.cur, n)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Dropdown("Currency", INV_CURRENCIES, inv.cur, { v -> update { it.copy(cur = v) } })
        Text("From", style = MaterialTheme.typography.titleSmall, color = skin.ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val logo = inv.logo?.let { runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull() }?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (logo != null) {
                Image(logo.asImageBitmap(), contentDescription = "Logo", modifier = Modifier.height(44.dp))
                BbButton("Remove logo", { update { it.copy(logo = null) } }, style = BtnStyle.Ghost, small = true)
            } else {
                BbButton("Add logo", { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, icon = "fa-solid fa-image", style = BtnStyle.Ghost, small = true)
            }
        }
        Field("Your business name", inv.biz, { v -> update { it.copy(biz = v) } })
        TextArea("Address, phone, email", inv.from, { v -> update { it.copy(from = v) } }, minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Invoice no.", inv.no, { v -> update { it.copy(no = v) } }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Date", runCatching { LocalDate.parse(inv.date) }.getOrDefault(LocalDate.now()), { d -> update { it.copy(date = d.toString()) } }, Modifier.weight(1f))
            DateField("Due date", runCatching { LocalDate.parse(inv.due) }.getOrDefault(LocalDate.now()), { d -> update { it.copy(due = d.toString()) } }, Modifier.weight(1f))
        }
        Text("Bill to", style = MaterialTheme.typography.titleSmall, color = skin.ink)
        Field("Client name", inv.client, { v -> update { it.copy(client = v) } })
        TextArea("Client address, email", inv.caddr, { v -> update { it.copy(caddr = v) } }, minLines = 2)
        Text("Items", style = MaterialTheme.typography.titleSmall, color = skin.ink)
        inv.items.forEachIndexed { i, item ->
            BbCard(padding = 12.dp, color = skin.board) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Field("Description", item.d, { v -> update { s -> s.copy(items = s.items.toMutableList().also { it[i] = item.copy(d = v) }) } }, Modifier.weight(1f), singleLine = false)
                    IconButton(onClick = {
                        update { s -> s.copy(items = s.items.toMutableList().also { it.removeAt(i) }.ifEmpty { listOf(InvItem()) }) }
                    }) { FaIcon("fa-solid fa-xmark", size = 14.sp, color = skin.steel, contentDescription = "Remove item") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    NumberField("Qty", item.q, { v -> update { s -> s.copy(items = s.items.toMutableList().also { it[i] = item.copy(q = v) }) } }, Modifier.weight(0.7f))
                    NumberField("Price", item.p, { v -> update { s -> s.copy(items = s.items.toMutableList().also { it[i] = item.copy(p = v) }) } }, Modifier.weight(1f), placeholder = "0.00")
                    Text(m(num(item.q) * num(item.p)), style = MaterialTheme.typography.titleSmall, color = skin.ink, modifier = Modifier.weight(1f))
                }
            }
        }
        BbButton("Add item", { update { it.copy(items = it.items + InvItem()) } }, icon = "fa-solid fa-plus", style = BtnStyle.Ghost, small = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Discount", inv.disc, { v -> update { it.copy(disc = v) } }, Modifier.weight(1f), suffix = "%")
            Field("Tax name", inv.taxLabel, { v -> update { it.copy(taxLabel = v) } }, Modifier.weight(1f))
            NumberField("Tax", inv.tax, { v -> update { it.copy(tax = v) } }, Modifier.weight(0.8f), suffix = "%")
        }
        TextArea("Notes & payment details", inv.notes, { v -> update { it.copy(notes = v) } }, minLines = 3, placeholder = "Bank name, account title, IBAN\nJazzCash / Easypaisa number\nThank you for your business!")
        BbCard(color = skin.board) {
            TotalLine("Subtotal", m(t.sub))
            TotalLine("Discount", "−" + m(t.disc))
            TotalLine(inv.taxLabel.ifBlank { "Tax" }, m(t.tax))
            HorizontalDivider(color = skin.rule)
            TotalLine("Total", m(t.total), bold = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BbButton("Save PDF", { saver.save(OutFile(fileName(inv), "application/pdf", invoicePdf(inv))) }, icon = "fa-solid fa-download", style = BtnStyle.Gold)
            BbButton("Share", { saver.share(listOf(OutFile(fileName(inv), "application/pdf", invoicePdf(inv)))) }, icon = "fa-solid fa-share-nodes", style = BtnStyle.Ghost)
        }
        BbButton("New invoice", {
            update { old ->
                val next = Regex("(\\d+)$").find(old.no)?.let { mt -> old.no.dropLast(mt.value.length) + (mt.value.toLong() + 1).toString().padStart(mt.value.length, '0') } ?: old.no
                Invoice(cur = old.cur, biz = old.biz, from = old.from, notes = old.notes, taxLabel = old.taxLabel, tax = old.tax, disc = old.disc, logo = old.logo, no = next, items = listOf(InvItem()))
            }
        }, icon = "fa-solid fa-file-circle-plus", style = BtnStyle.Ghost, small = true)
        Note("Your business details, logo and notes are remembered on this phone only. The PDF is created on your device.")
    }
}

@Composable
private fun TotalLine(label: String, value: String, bold: Boolean = false) {
    val skin = LocalSkin.current
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = skin.steel, modifier = Modifier.weight(1f))
        Text(value, style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium, color = skin.ink, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun fileName(inv: Invoice) = (inv.no.ifBlank { "invoice" }).replace(Regex("[^\\w\\-]+"), "-") + ".pdf"

/** Draws the invoice on A4 pages, matching the layout of the website's PDF. */
fun invoicePdf(inv: Invoice): ByteArray {
    val W = 595f; val H = 842f; val M = 48f
    val ink = android.graphics.Color.rgb(23, 32, 43)
    val muted = android.graphics.Color.rgb(91, 102, 118)
    val gold = android.graphics.Color.rgb(201, 165, 92)
    val line = android.graphics.Color.rgb(227, 231, 238)
    val doc = PdfDocument()
    var pageNo = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), pageNo).create())
    var c = page.canvas
    fun paint(size: Float, color: Int = ink, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; textAlign = align
    }
    fun block(text: String, x: Float, y: Float, width: Float, p: TextPaint): Float {
        if (text.isBlank()) return 0f
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, p, width.toInt()).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        c.save(); c.translate(x, y); layout.draw(c); c.restore()
        return layout.height.toFloat()
    }
    val t = totals(inv)
    fun m(n: Double) = invMoney(inv.cur, n)

    var y = M
    // logo
    inv.logo?.let { b64 ->
        runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val lh = 46f
            val lw = min(170f, bmp.width * lh / bmp.height)
            c.drawBitmap(bmp, null, RectF(M, y, M + lw, y + lw * bmp.height / bmp.width), null)
            y += 58f
        }
    }
    // title + meta on the right
    c.drawText("INVOICE", W - M, M + 22f, paint(26f, ink, true, Paint.Align.RIGHT))
    var my = M + 46f
    listOf("Invoice no." to inv.no, "Date" to niceDate(inv.date), "Due date" to niceDate(inv.due)).forEach { (k, v) ->
        if (v.isBlank()) return@forEach
        c.drawText(k, W - M - 110f, my, paint(9.5f, muted, align = Paint.Align.RIGHT))
        c.drawText(v, W - M, my, paint(9.5f, ink, true, Paint.Align.RIGHT))
        my += 15f
    }
    // from
    if (inv.biz.isNotBlank()) { c.drawText(inv.biz, M, y + 14f, paint(14f, ink, true)); y += 22f }
    y += block(inv.from, M, y, 260f, paint(9.5f, muted)) + 6f
    y = max(y, my) + 22f
    // bill to
    c.drawText("BILL TO", M, y, paint(8.5f, gold, true)); y += 16f
    if (inv.client.isNotBlank()) { c.drawText(inv.client, M, y, paint(12f, ink, true)); y += 6f }
    y += block(inv.caddr, M, y + 4f, 300f, paint(9.5f, muted)) + 18f
    // items
    val cQ = 360f; val cP = 450f; val cA = W - M
    fun head() {
        val bg = Paint().apply { color = ink }
        c.drawRect(M, y, W - M, y + 24f, bg)
        val hp = paint(9f, android.graphics.Color.WHITE, true)
        c.drawText("Description", M + 10f, y + 15.5f, hp)
        c.drawText("Qty", cQ, y + 15.5f, paint(9f, android.graphics.Color.WHITE, true, Paint.Align.RIGHT))
        c.drawText("Price", cP, y + 15.5f, paint(9f, android.graphics.Color.WHITE, true, Paint.Align.RIGHT))
        c.drawText("Amount", cA - 10f, y + 15.5f, paint(9f, android.graphics.Color.WHITE, true, Paint.Align.RIGHT))
        y += 24f
    }
    head()
    val linePaint = Paint().apply { color = line; strokeWidth = 0.8f }
    inv.items.filter { it.d.isNotBlank() || num(it.p) != 0.0 }.forEach { item ->
        val p = paint(9.5f)
        val layout = StaticLayout.Builder.obtain(item.d.ifBlank { "-" }, 0, item.d.ifBlank { "-" }.length, p, (cQ - M - 60f).toInt()).build()
        val h = layout.height + 14f
        if (y + h > H - 140f) {
            doc.finishPage(page); pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), pageNo).create()); c = page.canvas
            y = M; head()
        }
        c.save(); c.translate(M + 10f, y + 7f); layout.draw(c); c.restore()
        c.drawText(Ft.num(num(item.q), 2), cQ, y + 18f, paint(9.5f, align = Paint.Align.RIGHT))
        c.drawText(m(num(item.p)), cP, y + 18f, paint(9.5f, align = Paint.Align.RIGHT))
        c.drawText(m(num(item.q) * num(item.p)), cA - 10f, y + 18f, paint(9.5f, align = Paint.Align.RIGHT))
        y += h
        c.drawLine(M, y, W - M, y, linePaint)
    }
    // totals
    y += 16f
    fun total(label: String, value: String, big: Boolean = false) {
        c.drawText(label, cP - 40f, y, paint(if (big) 12f else 9.5f, if (big) ink else muted, big, Paint.Align.RIGHT))
        c.drawText(value, cA - 10f, y, paint(if (big) 12f else 9.5f, ink, big, Paint.Align.RIGHT))
        y += if (big) 22f else 16f
    }
    total("Subtotal", m(t.sub))
    if (t.disc > 0) total("Discount (${Ft.num(num(inv.disc))}%)", "-" + m(t.disc))
    if (t.tax > 0) total("${inv.taxLabel.ifBlank { "Tax" }} (${Ft.num(num(inv.tax))}%)", m(t.tax))
    c.drawLine(cP - 120f, y - 8f, W - M, y - 8f, Paint().apply { color = gold; strokeWidth = 1.5f })
    y += 6f
    total("Total", m(t.total), big = true)
    // notes
    if (inv.notes.isNotBlank()) {
        y += 10f
        c.drawText("NOTES & PAYMENT DETAILS", M, y, paint(8.5f, gold, true)); y += 8f
        block(inv.notes, M, y, W - 2 * M, paint(9.5f, muted))
    }
    doc.finishPage(page)
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
}
