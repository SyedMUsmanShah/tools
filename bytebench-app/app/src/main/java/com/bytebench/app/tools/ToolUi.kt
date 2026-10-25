package com.bytebench.app.tools

import androidx.compose.runtime.Composable
import com.bytebench.app.ui.Note

/** Shows the interactive part of a free tool, by its website slug. */
@Composable
fun ToolUi(slug: String) {
    when (slug) {
        "word-counter" -> WordCounter()
        "character-counter" -> CharacterCounter()
        "case-converter" -> CaseConverter()
        "age-calculator" -> AgeCalculator()
        "percentage-calculator" -> PercentageCalculator()
        "bmi-calculator" -> BmiCalculator()
        "unit-converter" -> UnitConverter()
        "time-zone-converter" -> TimeZoneConverter()
        "profit-margin-calculator" -> ProfitMarginCalculator()
        "loan-emi-calculator" -> LoanEmiCalculator()
        "discount-and-sales-tax-calculator" -> DiscountTaxCalculator()
        "invoice-generator" -> InvoiceGenerator()
        "image-compressor" -> ImageCompressor()
        "image-resizer" -> ImageResizer()
        "jpg-to-png" -> JpgToPng()
        "pdf-to-jpg" -> PdfToJpg()
        "password-generator" -> PasswordGenerator()
        "qr-code-generator" -> QrCodeGenerator()
        "random-name-generator" -> RandomNameGenerator()
        "lorem-ipsum-generator" -> LoremIpsumGenerator()
        "youtube-title-generator" -> YoutubeTitleGenerator()
        "instagram-caption-generator" -> InstagramCaptionGenerator()
        "hashtag-generator" -> HashtagGenerator()
        "meta-description-generator" -> MetaDescriptionGenerator()
        else -> Note("This tool isn’t in this version of the app yet — update the app to get it.")
    }
}
