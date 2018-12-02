package com.progressifff.filemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StorageMediaMountReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action!!
        when (action) {
            Intent.ACTION_MEDIA_MOUNTED -> {
                val mediaUri = intent.data
                if (mediaUri != null) {
                    Log.v(StorageMediaMountReceiver::class.java.name, Intent.ACTION_MEDIA_MOUNTED + " " + mediaUri.toString())
                }
            }

            Intent.ACTION_MEDIA_UNMOUNTED -> Log.v(StorageMediaMountReceiver::class.java.name, Intent.ACTION_MEDIA_UNMOUNTED)
        }
    }
}
