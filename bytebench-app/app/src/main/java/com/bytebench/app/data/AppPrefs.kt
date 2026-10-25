package com.bytebench.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Small settings and the "remember on this device" values the website keeps in
 * localStorage (FT.store) — last currency, saved date of birth, invoice details…
 */
class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences("bytebench", Context.MODE_PRIVATE)

    /** "auto", "porcelain", "ink" or "emerald". Observable so the theme updates at once. */
    var skin by mutableStateOf(sp.getString("skin", "auto") ?: "auto")
        private set

    fun updateSkin(value: String) {
        skin = value
        sp.edit().putString("skin", value).apply()
    }

    var siteUrl: String
        get() = sp.getString("site_url", "") ?: ""
        set(v) = sp.edit().putString("site_url", v).apply()

    var lastSync: Long
        get() = sp.getLong("last_sync", 0)
        set(v) = sp.edit().putLong("last_sync", v).apply()

    fun get(key: String, default: String = ""): String = sp.getString("ft_$key", default) ?: default
    fun set(key: String, value: String) = sp.edit().putString("ft_$key", value).apply()
}
