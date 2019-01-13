package com.progressifff.filemanager.presenters

import com.progressifff.filemanager.AbstractStorageFile

class RenameStorageFileDialogPresenter(private val initialStorageFileName: String, storageFile: AbstractStorageFile) : InputFileNameDialogPresenter(storageFile) {
    override fun isFileNameChanged(inputFileName: String): Boolean{
        return initialStorageFileName != inputFileName
    }
}