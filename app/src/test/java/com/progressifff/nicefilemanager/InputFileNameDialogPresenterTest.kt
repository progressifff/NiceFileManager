package com.progressifff.nicefilemanager

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.progressifff.nicefilemanager.presenters.InputFileNameDialogPresenter
import com.progressifff.nicefilemanager.views.InputFileNameDialogView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class InputFileNameDialogPresenterTest {
    private lateinit var presenter: InputFileNameDialogPresenter
    private val view = mock<InputFileNameDialogView>()
    private var file = mock<AbstractStorageFile>()

    @Before
    fun setup(){
        presenter = InputFileNameDialogPresenter(file)
        presenter.bindView(view)
    }

    @After
    fun release(){
        presenter.unbindView()
    }

    @Test
    fun checkInputEmptyFileName(){
        presenter.onDialogPositiveButtonClicked("")
        verify(view).showError(InputFileNameDialogPresenter.IOErrorType.EMPTY_FILE_NAME)
    }

    @Test
    fun checkInputExistingFileName(){
        `when`(file.contains("SimpleFileName")).thenReturn(true)
        presenter.onDialogPositiveButtonClicked("SimpleFileName")
        verify(view).showError(InputFileNameDialogPresenter.IOErrorType.FILE_ALREADY_EXISTS)
    }

    @Test
    fun checkInputValidFileName(){
        presenter.onDialogPositiveButtonClicked("ValidFileName")
        verify(view).onSuccess()
        verify(view).dismiss()
    }

    @Test
    fun checkBeforeInputTextChanged(){
        presenter.beforeInputTextChanged()
        verify(view).hideError()
    }

    @Test
    fun checkInputInvalidFileName(){
        val fileName = "123?321"
        presenter.inputTextChanged(fileName, 3, 1)
        verify(view).showError(InputFileNameDialogPresenter.IOErrorType.INVALID_FILE_NAME)
        verify(view).showText("123321", 3)
    }
}