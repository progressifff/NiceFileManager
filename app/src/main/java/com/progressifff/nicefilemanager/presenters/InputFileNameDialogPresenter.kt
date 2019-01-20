package com.progressifff.nicefilemanager.presenters

import com.progressifff.nicefilemanager.AbstractStorageFile
import com.progressifff.nicefilemanager.BasePresenter
import com.progressifff.nicefilemanager.views.InputFileNameDialogView
import java.util.regex.Pattern

open class InputFileNameDialogPresenter(file: AbstractStorageFile) : BasePresenter<AbstractStorageFile, InputFileNameDialogView>() {
    override var model = file
    private var isIOError = false
    private lateinit var currentIOErrorType: IOErrorType

    override fun updateView() {
        if(isIOError){
            view?.showError(currentIOErrorType)
        }
        else{
            view?.onSuccess()
        }
    }

    fun onDialogPositiveButtonClicked(inputFileName: String){
        if(inputFileName.isEmpty()){
            isIOError = true
            currentIOErrorType = IOErrorType.EMPTY_FILE_NAME
            updateView()
        }
        else if(!isFileNameChanged(inputFileName)){
            view?.dismiss()
        }
        else if(!isIOError){
            if(model.contains(inputFileName)){
                isIOError = true
                currentIOErrorType = IOErrorType.FILE_ALREADY_EXISTS
                updateView()
            }
            else{
                updateView()
                view?.dismiss()
            }
        }
    }

    open fun isFileNameChanged(inputFileName: String): Boolean = true

    fun beforeInputTextChanged() {
        if(!isIOError){
            view?.hideError()
        }
    }

    fun inputTextChanged(s: CharSequence, start: Int, count: Int){
        if(!isIOError){
            if(s.isNotEmpty() && !Pattern.matches("^([^\\\\/*?\"<>|]+)\$", s)){
                isIOError = true
                currentIOErrorType = IOErrorType.INVALID_FILE_NAME
                updateView()
                view?.showText(s.removeRange(start, start + count).toString(), start)
            }
            else{
                isIOError = false
                view?.hideError()
            }
        }
        else isIOError = false
    }

    enum class IOErrorType{
        FILE_ALREADY_EXISTS,
        EMPTY_FILE_NAME,
        INVALID_FILE_NAME
    }
}