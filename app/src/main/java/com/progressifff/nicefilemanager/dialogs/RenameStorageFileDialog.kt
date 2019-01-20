package com.progressifff.nicefilemanager.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import com.progressifff.nicefilemanager.*
import com.progressifff.nicefilemanager.AbstractStorageFile
import com.progressifff.nicefilemanager.presenters.RenameStorageFileDialogPresenter
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
            PresenterManager.restorePresenter<RenameStorageFileDialogPresenter>(savedInstanceState)
        }
        catch (e: Exception){
            e.printStackTrace()
            RenameStorageFileDialogPresenter(file.name, file.parent!!)
        }
        return buildDialog(context!!.getString(R.string.rename_dialog_title), file.isDirectory, file.name)
    }

    override fun onSuccess() {
        try{
            file.rename(fileNameEditText.text.toString())
            RxBus.publish(RxEvent.FileRenamedEvent(file))
        }
        catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(context, context!!.getString(R.string.rename_file_error), Toast.LENGTH_SHORT).show()
        }
        dismiss()
    }

    companion object {
        private const val FILE_TO_RENAME_KEY = "FileToRename"

        fun createInstance(storageFile: AbstractStorageFile): RenameStorageFileDialog {
            val dialog = RenameStorageFileDialog()
            val arguments = Bundle()
            arguments.putParcelable(FILE_TO_RENAME_KEY, storageFile)
            dialog.arguments = arguments
            return dialog
        }
    }
}