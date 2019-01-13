package com.progressifff.filemanager

import android.net.Uri
import android.support.v4.content.FileProvider
import com.progressifff.filemanager.AbstractStorageFile
import com.progressifff.filemanager.models.StorageFile
import java.lang.UnsupportedOperationException

interface IFileUriProvider{
    fun getUri(file: AbstractStorageFile): Uri
}

object FileUriProvider : IFileUriProvider{
    override fun getUri(file: AbstractStorageFile): Uri {
        if(file is StorageFile){
            return FileProvider.getUriForFile(App.get(), BuildConfig.APPLICATION_ID, file.source)
        }
        throw UnsupportedOperationException("Failed to retrieve file uri")
    }
}