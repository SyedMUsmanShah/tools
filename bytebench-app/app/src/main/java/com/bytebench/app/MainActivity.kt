package com.bytebench.app

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.bytebench.app.nav.AboutR
import com.bytebench.app.nav.BlogCategoryR
import com.bytebench.app.nav.BlogR
import com.bytebench.app.nav.BookR
import com.bytebench.app.nav.ContactR
import com.bytebench.app.nav.HomeR
import com.bytebench.app.nav.LegalR
import com.bytebench.app.nav.MoreR
import com.bytebench.app.nav.PostR
import com.bytebench.app.nav.ProductR
import com.bytebench.app.nav.Route
import com.bytebench.app.nav.SearchR
import com.bytebench.app.nav.ServiceR
import com.bytebench.app.nav.ServicesR
import com.bytebench.app.nav.SettingsR
import com.bytebench.app.nav.ShopR
import com.bytebench.app.nav.TOP_ROUTES
import com.bytebench.app.nav.ToolR
import com.bytebench.app.nav.ToolsR
import com.bytebench.app.nav.TopLevelBackStack
import com.bytebench.app.nav.WorkR
import com.bytebench.app.nav.routeForPath
import com.bytebench.app.screens.AboutScreen
import com.bytebench.app.screens.BlogScreen
import com.bytebench.app.screens.BookScreen
import com.bytebench.app.screens.ContactScreen
import com.bytebench.app.screens.HomeScreen
import com.bytebench.app.screens.LegalScreen
import com.bytebench.app.screens.MoreScreen
import com.bytebench.app.screens.PostScreen
import com.bytebench.app.screens.ProductScreen
import com.bytebench.app.screens.SearchScreen
import com.bytebench.app.screens.ServiceScreen
import com.bytebench.app.screens.ServicesScreen
import com.bytebench.app.screens.SettingsScreen
import com.bytebench.app.screens.ShopScreen
import com.bytebench.app.screens.ToolScreen
import com.bytebench.app.screens.ToolsScreen
import com.bytebench.app.screens.WorkScreen
import com.bytebench.app.ui.Actions
import com.bytebench.app.ui.FaIcon
import com.bytebench.app.ui.LocalContent
import com.bytebench.app.ui.LocalNav
import com.bytebench.app.ui.Navigator
import com.bytebench.app.ui.theme.BytebenchTheme
import com.bytebench.app.ui.theme.LocalSkin
import com.bytebench.app.ui.theme.resolveSkin

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Let the bottom bar colour reach the bottom edge (edge-to-edge skill)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false

        val app = application as BytebenchApp
        val startTool = intent?.getStringExtra(EXTRA_TOOL)

        setContent {
            val content by app.store.content.collectAsStateWithLifecycle()
            val skin = resolveSkin(app.prefs.skin)
            val context = LocalContext.current
            val stack = remember {
                TopLevelBackStack().also { s -> if (startTool != null) { s.selectTab(ToolsR); s.navigate(ToolR(startTool)) } }
            }
            val nav = remember(stack) {
                object : Navigator {
                    override fun navigate(route: Route) = stack.navigate(route)
                    override fun back() { if (!stack.goBack()) finish() }
                    override fun openLink(url: String) {
                        val site = Uri.parse(app.prefs.siteUrl.ifBlank { "https://invalid.local" })
                        val target = Uri.parse(url)
                        val route = when {
                            url.startsWith("/") -> routeForPath(url)
                            target.host != null && target.host == site.host -> routeForPath(target.path ?: "/")
                            else -> null
                        }
                        if (route != null) stack.navigate(route) else Actions.open(context, url)
                    }
                }
            }

            BytebenchTheme(skin) {
                CompositionLocalProvider(LocalNav provides nav, LocalContent provides content) {
                    BackHandler { nav.back() }
                    Scaffold(
                        containerColor = skin.paper,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            NavigationBar(containerColor = skin.surface, contentColor = skin.ink) {
                                TOP_ROUTES.forEach { tab ->
                                    NavigationBarItem(
                                        selected = stack.topLevel == tab,
                                        onClick = { stack.selectTab(tab) },
                                        icon = { FaIcon(tab.icon, size = 18.sp) },
                                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = LocalSkin.current.goldInk,
                                            indicatorColor = LocalSkin.current.gold,
                                            selectedTextColor = LocalSkin.current.ink,
                                            unselectedIconColor = LocalSkin.current.steel,
                                            unselectedTextColor = LocalSkin.current.steel,
                                        ),
                                    )
                                }
                            }
                        },
                    ) { inner ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(inner)
                                .consumeWindowInsets(inner),
                        ) {
                            NavDisplay(
                                backStack = stack.backStack,
                                onBack = { nav.back() },
                                entryProvider = entryProvider {
                                    entry<HomeR> { HomeScreen() }
                                    entry<ToolsR> { ToolsScreen() }
                                    entry<BlogR> { BlogScreen(null) }
                                    entry<ServicesR> { ServicesScreen() }
                                    entry<MoreR> { MoreScreen() }
                                    entry<ToolR> { ToolScreen(it.slug) }
                                    entry<PostR> { PostScreen(it.slug) }
                                    entry<BlogCategoryR> { BlogScreen(it.category) }
                                    entry<ServiceR> { ServiceScreen(it.slug) }
                                    entry<WorkR> { WorkScreen(it.slug) }
                                    entry<ProductR> { ProductScreen(it.slug) }
                                    entry<LegalR> { LegalScreen(it.slug) }
                                    entry<AboutR> { AboutScreen() }
                                    entry<ShopR> { ShopScreen() }
                                    entry<ContactR> { ContactScreen() }
                                    entry<BookR> { BookScreen(it.topic) }
                                    entry<SearchR> { SearchScreen() }
                                    entry<SettingsR> { SettingsScreen() }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** Opens straight into one tool (used by the launcher shortcuts). */
        const val EXTRA_TOOL = "tool"
    }
}
