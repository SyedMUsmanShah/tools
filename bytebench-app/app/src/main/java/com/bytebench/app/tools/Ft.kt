package com.bytebench.app.tools

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.SecureRandom
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Kotlin versions of the website's shared tool helpers (public/js/tools.js, "FT"). */
object Ft {
    private val rng = SecureRandom()

    /** Like FT.num: grouping, up to [max] decimals. */
    fun num(n: Double, max: Int = 2): String {
        if (n.isNaN() || n.isInfinite()) return "—"
        val f = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US)).apply { maximumFractionDigits = max; minimumFractionDigits = 0 }
        val s = f.format(n)
        return if (s == "-0") "0" else s
    }

    fun num(n: Int): String = num(n.toDouble(), 0)

    /** Like FT.fixed: exactly [dp] decimals with grouping. */
    fun fixed(n: Double, dp: Int): String {
        if (n.isNaN() || n.isInfinite()) return "—"
        val f = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US)).apply { maximumFractionDigits = dp; minimumFractionDigits = dp }
        return f.format(n)
    }

    /** Parses what the user typed; commas are ignored. NaN when empty or invalid. */
    fun value(s: String): Double = s.replace(",", "").trim().toDoubleOrNull() ?: Double.NaN

    fun bytes(b: Long): String {
        var v = b.toDouble()
        val u = listOf("B", "KB", "MB", "GB")
        var i = 0
        while (v >= 1024 && i < u.size - 1) { v /= 1024; i++ }
        return if (i == 0) "$b B" else String.format(Locale.US, if (v < 10) "%.2f %s" else "%.1f %s", v, u[i])
    }

    fun randInt(max: Int): Int = if (max <= 0) 0 else rng.nextInt(max)
    fun <T> pick(list: List<T>): T = list[randInt(list.size)]
    fun <T> shuffle(list: List<T>): List<T> {
        val a = list.toMutableList()
        for (i in a.size - 1 downTo 1) {
            val j = randInt(i + 1)
            val t = a[i]; a[i] = a[j]; a[j] = t
        }
        return a
    }

    fun cap(s: String): String = s.trim().let { if (it.isEmpty()) it else it[0].uppercaseChar() + it.substring(1) }

    private val small = Regex("^(a|an|and|as|at|but|by|for|in|nor|of|on|or|per|the|to|vs?\\.?|via|with)$", RegexOption.IGNORE_CASE)
    private val wordRe = Regex("[A-Za-zÀ-ɏ][^\\s\\-–—/]*")
    private val sentenceEnd = Regex("[:.!?]\\s*$")

    /** FT.titleCase: small words stay lowercase unless they start the title or a sentence. */
    fun titleCase(s: String, keepBrands: Boolean = false): String = wordRe.replace(s) { m ->
        val word = m.value
        val idx = m.range.first
        if (keepBrands && ((word.any { it.isLowerCase() } && word.drop(1).any { it.isUpperCase() }) || Regex("^[A-Z0-9]{2,4}$").matches(word))) {
            return@replace word
        }
        val w = word.lowercase()
        val atStart = idx == 0 || sentenceEnd.containsMatchIn(s.substring(0, idx))
        if (!atStart && small.matches(w)) w else w.replaceFirstChar { it.uppercaseChar() }
    }

    /** "my-photo (1).jpg" → "my-photo 1", like FT.baseName. */
    fun baseName(name: String): String =
        name.replace(Regex("\\.[^.]+$"), "").replace(Regex("[^\\w\\- ]+"), "").trim().ifEmpty { "image" }
}

/** The word lists and templates pulled out of the website's tool scripts (assets/generators.json). */
object GenData {
    private var root: JsonObject? = null

    fun load(context: Context): JsonObject {
        root?.let { return it }
        val text = context.assets.open("generators.json").bufferedReader().use { it.readText() }
        return Json.parseToJsonElement(text).jsonObject.also { root = it }
    }

    fun tool(context: Context, slug: String): JsonObject = load(context)[slug]?.jsonObject ?: JsonObject(emptyMap())
}

fun JsonElement.strings(): List<String> = (this as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
fun JsonObject.strings(key: String): List<String> = this[key]?.strings() ?: emptyList()
fun JsonObject.obj(key: String): JsonObject = this[key]?.jsonObject ?: JsonObject(emptyMap())
fun JsonObject.arr(key: String): JsonArray = this[key]?.jsonArray ?: JsonArray(emptyList())
fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.content ?: ""
