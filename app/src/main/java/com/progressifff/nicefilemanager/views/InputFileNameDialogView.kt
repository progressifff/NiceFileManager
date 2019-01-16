package com.progressifff.nicefilemanager.views

import com.progressifff.nicefilemanager.presenters.InputFileNameDialogPresenter

interface InputFileNameDialogView {
    fun showError(errorType: InputFileNameDialogPresenter.IOErrorType)
    fun showText(text: String, cursorPosition: Int)
    fun hideError()
    fun dismiss()
    fun onSuccess()
}