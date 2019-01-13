package com.progressifff.filemanager

import android.app.Application
import android.os.Handler
import android.os.Looper

class App : Application() {

    val handler = Handler(Looper.getMainLooper())

    companion object {
        private lateinit var instance: App
        fun get(): App {return instance}

        init {
            System.loadLibrary("native-lib")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}