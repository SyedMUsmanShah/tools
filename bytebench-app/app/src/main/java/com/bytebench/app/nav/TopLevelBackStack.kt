package com.bytebench.app.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * One back stack per bottom-navigation tab, flattened for NavDisplay
 * (the Navigation 3 "common UI" recipe). Home always stays at the bottom,
 * so Back from any tab returns to Home before leaving the app.
 */
class TopLevelBackStack(private val start: TopRoute = HomeR) {

    private val stacks: LinkedHashMap<TopRoute, SnapshotStateList<Route>> =
        linkedMapOf(start to mutableStateListOf<Route>(start))

    var topLevel by mutableStateOf(start)
        private set

    val backStack = mutableStateListOf<Route>(start)

    private fun rebuild() {
        backStack.clear()
        backStack.addAll(stacks.values.flatten())
    }

    /** Selects a tab. Tapping the current tab again pops it to its first screen. */
    fun selectTab(tab: TopRoute) {
        if (tab == topLevel) {
            stacks[tab]?.let { s -> while (s.size > 1) s.removeAt(s.lastIndex) }
        } else {
            val stack = stacks.remove(tab) ?: mutableStateListOf<Route>(tab)
            if (tab == start) {
                // Home is the root: bring it forward by dropping the other tabs
                stacks.clear()
            }
            stacks[tab] = stack
            topLevel = tab
        }
        rebuild()
    }

    fun navigate(route: Route) {
        if (route is TopRoute) { selectTab(route); return }
        stacks[topLevel]?.add(route)
        rebuild()
    }

    /** Returns false when there is nothing left to go back to (the app should close). */
    fun goBack(): Boolean {
        val stack = stacks[topLevel] ?: return false
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        } else {
            if (stacks.size <= 1) return false
            stacks.remove(topLevel)
            topLevel = stacks.keys.last()
        }
        rebuild()
        return true
    }
}
