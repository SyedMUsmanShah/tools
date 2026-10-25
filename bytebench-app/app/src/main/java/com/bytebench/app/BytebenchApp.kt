package com.bytebench.app

import android.app.Application
import com.bytebench.app.data.AppPrefs
import com.bytebench.app.data.ContentStore

class BytebenchApp : Application() {
    val prefs by lazy { AppPrefs(this) }
    val store by lazy { ContentStore(this, prefs) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: BytebenchApp
            private set
    }
}
