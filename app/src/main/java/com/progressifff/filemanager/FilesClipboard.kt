package com.progressifff.filemanager

import com.progressifff.filemanager.models.AbstractStorageFile
import kotlin.properties.Delegates

class FilesClipboard private constructor(){
    var filesClipboardListener: FilesClipboardListener? = null

    var clipData by Delegates.observable<ClipData?>(null) {
        _, _, _ ->
        filesClipboardListener?.onClipDataChanged()
    }

    val isEmpty: Boolean get() = clipData == null
    val isNotEmpty: Boolean get() = !isEmpty
    fun clear(){ clipData = null }

    private object Holder { val FILES_CLIPBOARD = FilesClipboard() }

    companion object {
         val instance: FilesClipboard by lazy {
             return@lazy Holder.FILES_CLIPBOARD
         }
    }

    data class ClipData(val action: Action, val files: ArrayList<AbstractStorageFile>, val sourceFilesList: List<AbstractStorageFile>? = null){
        enum class Action{ COPY, CUT }
    }

    interface FilesClipboardListener{
        fun onClipDataChanged()
    }
}