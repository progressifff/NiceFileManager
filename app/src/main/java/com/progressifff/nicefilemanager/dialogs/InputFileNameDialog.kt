package com.progressifff.nicefilemanager.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.progressifff.nicefilemanager.PresenterManager
import com.progressifff.nicefilemanager.R
import com.progressifff.nicefilemanager.presenters.InputFileNameDialogPresenter
import com.progressifff.nicefilemanager.views.InputFileNameDialogView

abstract class InputFileNameDialog : DialogFragment(), InputFileNameDialogView {

    protected lateinit var presenter: InputFileNameDialogPresenter
    protected lateinit var fileNameEditText: EditText
    protected lateinit var fileNameInputLayout: TextInputLayout

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        PresenterManager.savePresenter(presenter, outState)
    }

    override fun onStart() {
        super.onStart()
        presenter.bindView(this)
    }

    override fun onStop() {
        super.onStop()
        presenter.unbindView()
    }

    override fun showText(text: String, cursorPosition: Int) {
        fileNameEditText.setText(text)
        fileNameEditText.setSelection(cursorPosition)
    }

    override fun showError(errorType: InputFileNameDialogPresenter.IOErrorType) {
        fileNameInputLayout.isErrorEnabled = true
        when(errorType){
            InputFileNameDialogPresenter.IOErrorType.INVALID_FILE_NAME -> fileNameInputLayout.error = context!!.getString(R.string.invalid_file_name_error)
            InputFileNameDialogPresenter.IOErrorType.EMPTY_FILE_NAME -> fileNameInputLayout.error = context!!.getString(R.string.empty_file_name_error)
            InputFileNameDialogPresenter.IOErrorType.FILE_ALREADY_EXISTS -> fileNameInputLayout.error = context!!.getString(R.string.file_name_already_exists_error)
        }
    }

    override fun hideError() {
        fileNameInputLayout.isErrorEnabled = false
    }

    protected open fun buildDialog(dialogTitle: String, fileIsDirectory: Boolean, fileName: String = ""): Dialog {
        val contentLayout = activity!!.layoutInflater.inflate(R.layout.rename_create_storage_file_dialog, null)
        fileNameEditText = contentLayout.findViewById(R.id.fileNameField)
        fileNameEditText.setText(fileName)
        fileNameEditText.setSelection(fileNameEditText.text.length)
        fileNameInputLayout = contentLayout.findViewById(R.id.fileNameInputLayout)
        initInputField(fileIsDirectory)

        val dialogBuilder = AlertDialog.Builder(context!!)
        dialogBuilder.setTitle(dialogTitle)
        dialogBuilder.setView(contentLayout)
        dialogBuilder.setPositiveButton(getString(android.R.string.ok), null)
        dialogBuilder.setNegativeButton(getString(android.R.string.cancel), null)
        val dialog = dialogBuilder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                presenter.onDialogPositiveButtonClicked(fileNameEditText.text.toString())
            }
        }
        return dialog
    }

    private fun initInputField(fileIsDirectory: Boolean){
        fileNameEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter.inputTextChanged(s!!, start, count)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                presenter.beforeInputTextChanged()
            }
        })
        val hintId = if(fileIsDirectory){
            R.string.text_input_hint_folder
        }
        else R.string.text_input_hint_file
        fileNameInputLayout.hint = context!!.getString(hintId)
    }
}