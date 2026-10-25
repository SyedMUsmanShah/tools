package com.bytebench.app.ui

import android.content.Context
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.bytebench.app.R
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Font Awesome icons, exactly as the website names them ("fa-solid fa-file-word",
 * "fa-brands fa-whatsapp"), drawn from the site's own icon fonts.
 */
object FontAwesome {
    private val solidFont = FontFamily(Font(R.font.fa_solid))
    private val brandsFont = FontFamily(Font(R.font.fa_brands))
    private var solid: Map<String, Int> = emptyMap()
    private var brands: Map<String, Int> = emptyMap()

    fun load(context: Context) {
        if (solid.isNotEmpty()) return
        val root = Json.parseToJsonElement(context.assets.open("fa_icons.json").bufferedReader().use { it.readText() }).jsonObject
        solid = root.getValue("solid").jsonObject.mapValues { it.value.jsonPrimitive.int }
        brands = root.getValue("brands").jsonObject.mapValues { it.value.jsonPrimitive.int }
    }

    /** Returns the glyph and font for a class list such as "fa-brands fa-whatsapp". */
    fun resolve(classes: String): Pair<String, FontFamily> {
        val parts = classes.split(' ').filter { it.startsWith("fa-") && it != "fa-solid" && it != "fa-brands" && it != "fa-regular" }
        val name = parts.lastOrNull()?.removePrefix("fa-") ?: "circle"
        val isBrand = classes.contains("fa-brands")
        val code = (if (isBrand) brands[name] else solid[name]) ?: solid[name] ?: brands[name] ?: solid["circle"] ?: 0xF111
        val font = if (isBrand || (solid[name] == null && brands[name] != null)) brandsFont else solidFont
        return String(Character.toChars(code)) to font
    }
}

@Composable
fun FaIcon(
    icon: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 18.sp,
    color: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    FontAwesome.load(LocalContext.current)
    val (glyph, font) = FontAwesome.resolve(if (icon.startsWith("fa-")) icon else "fa-solid fa-$icon")
    Text(
        text = glyph,
        modifier = modifier.clearAndSetSemantics { if (contentDescription != null) this.contentDescription = contentDescription },
        style = TextStyle(fontFamily = font, fontSize = size, lineHeight = size, color = color, textAlign = TextAlign.Center),
    )
}
