package com.progressifff.filemanager.presenters

import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.views.InputFileNameDialogView
import java.util.regex.Pattern

open class InputFileNameDialogPresenter(override var model: AbstractStorageFile) : BasePresenter<AbstractStorageFile, InputFileNameDialogView>() {

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
        else if(!isIOError){
            if(isFileNameChanged(inputFileName) && model.hasChild(inputFileName)){
                isIOError = true
                currentIOErrorType = IOErrorType.FILE_ALREADY_EXISTS
                updateView()
                return
            }
            if(isFileNameChanged(inputFileName)){
                updateView()
            }
            view?.dismiss()
        }
    }

    open fun isFileNameChanged(inputFileName: String): Boolean {return true}

    fun beforeInputTextChanged() {
        if(!isIOError){
            view?.hideError()
        }
    }

    fun inputTextChanged(s: CharSequence, start: Int, count: Int){
        if(!isIOError){
            if(s.isNotEmpty() && !Pattern.matches(FILE_NAME_REGEX, s)){
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

    companion object {
        private const val FILE_NAME_REGEX = "^([^\\\\/*?\"<>|]+)\$"
    }

    enum class IOErrorType{
        FILE_ALREADY_EXISTS,
        EMPTY_FILE_NAME,
        INVALID_FILE_NAME
    }
}