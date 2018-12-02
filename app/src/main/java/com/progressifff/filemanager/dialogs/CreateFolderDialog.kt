package com.progressifff.filemanager.dialogs

import android.app.Dialog
import android.os.Bundle
import com.progressifff.filemanager.PresenterManager
import com.progressifff.filemanager.R
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.presenters.InputFileNameDialogPresenter
import com.progressifff.filemanager.showToast

class CreateFolderDialog : InputFileNameDialog() {

    private lateinit var parentFolder: AbstractStorageFile

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        parentFolder = arguments!!.getParcelable(PARENT_STORAGE_FILE_KEY)!!

        presenter = if(savedInstanceState == null){
            InputFileNameDialogPresenter(parentFolder)
        }
        else try{
            PresenterManager.instance.restorePresenter<InputFileNameDialogPresenter>(savedInstanceState)
        } catch (e: Exception){
            e.printStackTrace()
            InputFileNameDialogPresenter(parentFolder)
        }

        return buildDialog(getString(R.string.create_storage_file_dialog_title), true)
    }

    override fun onSuccess() {

        val newStorageFile = parentFolder.get(fileNameEditText.text.toString())

        try{
            newStorageFile.createFolder()
        }
        catch (e: Exception){
            e.printStackTrace()
            showToast(getString(R.string.create_folder_error))
        }
    }

    companion object {
        private const val PARENT_STORAGE_FILE_KEY = "parentStorageFileKey"

        fun createInstance(parentFolder: AbstractStorageFile): CreateFolderDialog{
            val dialog = CreateFolderDialog()
            val arguments = Bundle()
            arguments.putParcelable(PARENT_STORAGE_FILE_KEY, parentFolder)
            dialog.arguments = arguments
            return dialog
        }
    }
}