package com.bytebench.app.nav

/** Navigation keys. Each tab keeps its own back stack (see [TopLevelBackStack]). */
sealed interface Route

sealed interface TopRoute : Route {
    val label: String
    val icon: String
}

data object HomeR : TopRoute { override val label = "Home"; override val icon = "fa-solid fa-house" }
data object ToolsR : TopRoute { override val label = "Free tools"; override val icon = "fa-solid fa-toolbox" }
data object BlogR : TopRoute { override val label = "Blog"; override val icon = "fa-solid fa-newspaper" }
data object ServicesR : TopRoute { override val label = "Services"; override val icon = "fa-solid fa-briefcase" }
data object MoreR : TopRoute { override val label = "More"; override val icon = "fa-solid fa-bars" }

val TOP_ROUTES: List<TopRoute> = listOf(HomeR, ToolsR, BlogR, ServicesR, MoreR)

data class ToolR(val slug: String) : Route
data class PostR(val slug: String) : Route
data class BlogCategoryR(val category: String) : Route
data class ServiceR(val slug: String) : Route
data class WorkR(val slug: String) : Route
data class ProductR(val slug: String) : Route
data class LegalR(val slug: String) : Route
data object AboutR : Route
data object ShopR : Route
data object ContactR : Route
data class BookR(val topic: String = "") : Route
data object SearchR : Route
data object SettingsR : Route

/** Maps a website path (as used in links inside posts and services) to a screen. */
fun routeForPath(path: String): Route? {
    val p = path.substringBefore('?').substringBefore('#').trim('/')
    val parts = p.split('/').filter { it.isNotEmpty() }
    return when (parts.getOrNull(0)) {
        null -> HomeR
        "free-tools" -> parts.getOrNull(1)?.let { ToolR(it) } ?: ToolsR
        "blog" -> when {
            parts.getOrNull(1) == "category" && parts.size > 2 -> BlogCategoryR(parts[2])
            parts.size > 1 -> PostR(parts[1])
            else -> BlogR
        }
        "services" -> parts.getOrNull(1)?.let { ServiceR(it) } ?: ServicesR
        "work" -> parts.getOrNull(1)?.let { WorkR(it) }
        "shop", "tools" -> parts.getOrNull(1)?.let { ProductR(it) } ?: ShopR
        "about" -> AboutR
        "contact" -> ContactR
        "book-a-call" -> BookR()
        "search" -> SearchR
        "privacy-policy", "terms", "disclaimer", "cookie-policy", "refund-policy" -> LegalR(parts[0])
        else -> null
    }
}
