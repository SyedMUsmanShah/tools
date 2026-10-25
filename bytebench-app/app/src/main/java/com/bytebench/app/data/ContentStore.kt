package com.bytebench.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Holds the site content. It starts from the copy bundled in the APK (or the last copy
 * synced from the website), and [sync] refreshes it from `<site>/api/app/content.json`
 * when a website address is set in Settings.
 */
class ContentStore(private val context: Context, private val prefs: AppPrefs) {

    private val cache = File(context.filesDir, "content-synced.json")
    private val _content = MutableStateFlow(loadInitial())
    val content: StateFlow<Content> = _content.asStateFlow()

    private fun loadInitial(): Content {
        if (cache.exists()) {
            runCatching { return json.decodeFromString(Content.serializer(), cache.readText()) }
        }
        return bundled()
    }

    private fun bundled(): Content =
        context.assets.open("content.json").bufferedReader().use { json.decodeFromString(Content.serializer(), it.readText()) }

    /** Downloads the latest content from the website. Returns null on success, or a message. */
    suspend fun sync(): String? = withContext(Dispatchers.IO) {
        val base = prefs.siteUrl.trim().trimEnd('/')
        if (base.isEmpty()) return@withContext "Add your website address first."
        try {
            val conn = (URL("$base/api/app/content.json").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode != 200) return@withContext "The website answered with error ${conn.responseCode}."
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val fresh = json.decodeFromString(Content.serializer(), text)
                cache.writeText(text)
                _content.value = fresh
                prefs.lastSync = System.currentTimeMillis()
                null
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            "Couldn’t reach the website: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    /** Forgets synced content and goes back to what shipped with the app. */
    fun resetToBundled() {
        cache.delete()
        prefs.lastSync = 0
        _content.value = bundled()
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
    }
}
