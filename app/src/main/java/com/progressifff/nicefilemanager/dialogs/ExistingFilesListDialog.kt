package com.progressifff.nicefilemanager.dialogs

import android.app.Dialog
import android.support.v4.app.DialogFragment
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import com.progressifff.nicefilemanager.R
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.progressifff.nicefilemanager.isScrollable

class ExistingFilesListDialog : DialogFragment() {

    private lateinit var existingFilesList: ListView
    private lateinit var existingFilesListDivTop: View
    private lateinit var existingFilesListDivBottom: View
    private val existingFilesListListener = OnGlobalLayoutListener {
        if (!existingFilesList.isScrollable) {
            existingFilesListDivTop.visibility = View.INVISIBLE
            existingFilesListDivBottom.visibility = View.INVISIBLE
        }
        else{
            existingFilesListDivTop.visibility = View.VISIBLE
            existingFilesListDivBottom.visibility = View.VISIBLE
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content = activity!!.layoutInflater.inflate(R.layout.existing_files_list, null)
        existingFilesList = content.findViewById(R.id.existingFilesList)
        existingFilesListDivTop = content.findViewById(R.id.existingFilesListDivTop)
        existingFilesListDivBottom = content.findViewById(R.id.existingFilesListDivBottom)
        val filesNames = arguments!!.getStringArrayList(EXISTING_FILES_NAMES_KEY)
        val adapter = ArrayAdapter(context!!, R.layout.existing_files_list_item, R.id.existingFileName, filesNames!!)
        existingFilesList.adapter = adapter
        val observer = existingFilesList.viewTreeObserver
        observer.addOnGlobalLayoutListener(existingFilesListListener)
        val dialogBuilder = AlertDialog.Builder(context!!)
        dialogBuilder.setTitle(getString(R.string.existing_files_dialog_title))
        dialogBuilder.setView(content)
        dialogBuilder.setPositiveButton(getString(android.R.string.ok), null)
        return dialogBuilder.create()
    }

    override fun onDestroy() {
        super.onDestroy()
        val observer = existingFilesList.viewTreeObserver
        observer.removeOnGlobalLayoutListener(existingFilesListListener)
    }

    companion object {
        private const val EXISTING_FILES_NAMES_KEY = "ExistingFilesNames"

        fun createInstance(filesNames: ArrayList<String>): ExistingFilesListDialog{
            val dialog = ExistingFilesListDialog()
            val arguments = Bundle()
            arguments.putStringArrayList(EXISTING_FILES_NAMES_KEY, filesNames)
            dialog.arguments = arguments
            return dialog
        }
    }
}