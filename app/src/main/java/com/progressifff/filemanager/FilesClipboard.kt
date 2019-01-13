package com.progressifff.filemanager

import kotlin.properties.Delegates

object FilesClipboard {
    var filesClipboardListener: FilesClipboardListener? = null

    var clipData by Delegates.observable<ClipData?>(null) {
        _, _, _ ->
        filesClipboardListener?.onClipDataChanged()
    }

    val isEmpty: Boolean get() = clipData == null
    val isNotEmpty: Boolean get() = !isEmpty
    fun clear(){ clipData = null }

    data class ClipData(val action: Action, val files: ArrayList<AbstractStorageFile>, val sourceFilesList: List<AbstractStorageFile>? = null){
        enum class Action{ COPY, CUT }
    }

    interface FilesClipboardListener{
        fun onClipDataChanged()
    }
}