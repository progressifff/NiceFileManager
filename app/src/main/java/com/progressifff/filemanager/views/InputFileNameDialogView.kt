package com.progressifff.filemanager.views

import com.progressifff.filemanager.presenters.InputFileNameDialogPresenter

interface InputFileNameDialogView {
    fun showError(errorType: InputFileNameDialogPresenter.IOErrorType)
    fun showText(text: String, cursorPosition: Int)
    fun hideError()
    fun dismiss()
    fun onSuccess()
}