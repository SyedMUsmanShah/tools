package com.bytebench.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * The shape of content.json. The same file ships inside the app (assets/content.json,
 * built by scripts/export_content.php) and is served by the website at
 * /api/app/content.json once it is live, so the app can stay in sync with the admin panel.
 */

@Serializable
data class Content(
    val version: Int = 1,
    val generatedAt: String = "",
    val site: Site,
    val toolCategories: List<ToolCategory> = emptyList(),
    val featuredTools: List<String> = emptyList(),
    val tools: List<FreeTool> = emptyList(),
    val services: List<Service> = emptyList(),
    val projects: List<Project> = emptyList(),
    val posts: List<Post> = emptyList(),
    val products: List<Product> = emptyList(),
    val legal: List<LegalPage> = emptyList(),
)

@Serializable
data class Site(
    val name: String,
    val tagline: String,
    val description: String = "",
    val owner: Owner,
    val socials: List<Social> = emptyList(),
    val intro: String = "",
    val experience: List<Experience> = emptyList(),
    val skills: List<SkillGroup> = emptyList(),
    val education: String = "",
    val stats: List<Stat> = emptyList(),
    val steps: List<Step> = emptyList(),
    val booking: Booking = Booking(),
    val authorBio: String = "",
    val blogCategories: List<String> = emptyList(),
    val legalUpdated: String = "",
    val defaultSkin: String = "ink",
)

@Serializable
data class Owner(
    val name: String,
    val shortName: String,
    val role: String = "",
    val location: String = "",
    val email: String,
    val whatsapp: String,
    val whatsappDisplay: String = "",
    val whatsappMessage: String = "",
)

@Serializable
data class Social(val name: String, val url: String, val icon: String)

@Serializable
data class Experience(val role: String, val org: String, val period: String, val points: List<String> = emptyList())

@Serializable
data class SkillGroup(val group: String, val items: List<String>)

@Serializable
data class Stat(
    val label: String,
    val since: String? = null,
    val value: Int? = null,
    val count: String? = null,
    val suffix: String = "",
)

@Serializable
data class Step(val icon: String, val title: String, val text: String)

@Serializable
data class Booking(
    val timezone: String = "Asia/Karachi",
    val days: List<Int> = listOf(1, 2, 3, 4, 5),
    val from: String = "17:00",
    val to: String = "22:00",
    val minutes: Int = 15,
    @SerialName("every_minutes") val everyMinutes: Int = 30,
    @SerialName("days_ahead") val daysAhead: Int = 14,
    @SerialName("min_notice_hours") val minNoticeHours: Int = 6,
)

@Serializable
data class ToolCategory(val slug: String, val name: String)

@Serializable
data class Faq(val q: String, val a: String)

@Serializable
data class Pitch(val service: String, val title: String, val text: String)

@Serializable
data class FreeTool(
    val slug: String,
    val name: String,
    val category: String,
    val icon: String,
    val summary: String,
    val steps: List<String> = emptyList(),
    val about: String = "",
    val faqs: List<Faq> = emptyList(),
    val pitch: Pitch? = null,
    val topics: List<String> = emptyList(),
)

@Serializable
data class Service(
    val slug: String,
    val title: String,
    val icon: String = "fa-solid fa-briefcase",
    val summary: String,
    val includes: List<String> = emptyList(),
    val details: String = "",
    val priceLabel: String? = null,
    val sortOrder: Int = 0,
)

@Serializable
data class Project(
    val slug: String,
    val title: String,
    val summary: String = "",
    val role: String? = null,
    val year: String? = null,
    val tech: List<String> = emptyList(),
    val url: String? = null,
    val image: String? = null,
    val challenge: String = "",
    val approach: String = "",
    val outcome: String = "",
) {
    val isCaseStudy: Boolean get() = challenge.isNotBlank() || approach.isNotBlank() || outcome.isNotBlank()
}

@Serializable
data class Post(
    val slug: String,
    val title: String,
    val category: String? = null,
    val excerpt: String = "",
    val body: String,
    val cover: String? = null,
    val publishedAt: String = "",
    val readingMinutes: Int = 1,
)

/** A product from the website's Shop (free or paid). */
@Serializable
data class Product(
    val slug: String,
    val name: String,
    val tagline: String = "",
    val description: String = "",
    val type: String = "web",
    val pricing: String = "free",
    val priceLabel: String? = null,
    val version: String? = null,
    val cover: String? = null,
    val features: List<String> = emptyList(),
    val url: String? = null,
) {
    val isFree: Boolean get() = pricing == "free"
    val typeLabel: String get() = when (type) {
        "web" -> "Web tool"
        "download" -> "Download"
        "extension" -> "Chrome extension"
        else -> type.replaceFirstChar { it.uppercase() }
    }
}

@Serializable
data class LegalPage(val slug: String, val title: String, val body: String)
