package com.progressifff.filemanager.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import com.progressifff.filemanager.*
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.presenters.RenameStorageFileDialogPresenter
import java.lang.Exception

class RenameStorageFileDialog : InputFileNameDialog() {
    private lateinit var file: AbstractStorageFile

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        file = arguments!!.getParcelable(FILE_TO_RENAME_KEY)!!

        presenter = if(savedInstanceState == null){
            RenameStorageFileDialogPresenter(file.name, file.parent!!)
        }
        else try{
            PresenterManager.instance.restorePresenter<RenameStorageFileDialogPresenter>(savedInstanceState)
        }
        catch (e: Exception){
            e.printStackTrace()
            RenameStorageFileDialogPresenter(file.name, file.parent!!)
        }

        return buildDialog(getStringFromRes(R.string.rename_dialog_title), file.isDirectory, file.name)
    }

    override fun onSuccess() {
        try{
            file.rename(fileNameEditText.text.toString())
            RxBus.publish(RxEvent.FileRenamedEvent(file))
        }
        catch (e: Exception){
            e.printStackTrace()
            showToast(getStringFromRes(R.string.rename_files_error))
        }
        dismiss()
    }

    companion object {
        private const val FILE_TO_RENAME_KEY = "FileToRenameKey"

        fun createInstance(storageFile: AbstractStorageFile): RenameStorageFileDialog {
            val dialog = RenameStorageFileDialog()
            val arguments = Bundle()
            arguments.putParcelable(FILE_TO_RENAME_KEY, storageFile)
            dialog.arguments = arguments
            return dialog
        }
    }
}