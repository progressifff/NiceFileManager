package com.progressifff.nicefilemanager.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import com.progressifff.nicefilemanager.PresenterManager
import com.progressifff.nicefilemanager.R
import com.progressifff.nicefilemanager.AbstractStorageFile
import com.progressifff.nicefilemanager.presenters.InputFileNameDialogPresenter

class CreateFolderDialog : InputFileNameDialog() {

    private lateinit var parentFolder: AbstractStorageFile

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        parentFolder = arguments!!.getParcelable(PARENT_STORAGE_FILE_KEY)!!

        presenter = if(savedInstanceState == null){
            InputFileNameDialogPresenter(parentFolder)
        }
        else try{
            PresenterManager.restorePresenter<InputFileNameDialogPresenter>(savedInstanceState)
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
            Toast.makeText(context, context!!.getString(R.string.create_folder_error), Toast.LENGTH_SHORT).show()
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