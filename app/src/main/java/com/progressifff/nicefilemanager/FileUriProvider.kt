package com.progressifff.nicefilemanager

import android.net.Uri
import android.support.v4.content.FileProvider
import com.progressifff.nicefilemanager.models.StorageFile
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