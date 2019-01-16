package com.progressifff.nicefilemanager.presenters

import com.progressifff.nicefilemanager.AbstractStorageFile

class RenameStorageFileDialogPresenter(private val initialStorageFileName: String, storageFile: AbstractStorageFile) : InputFileNameDialogPresenter(storageFile) {
    override fun isFileNameChanged(inputFileName: String): Boolean{
        return initialStorageFileName != inputFileName
    }
}