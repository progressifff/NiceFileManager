package com.progressifff.filemanager

import com.nhaarman.mockitokotlin2.*
import com.progressifff.filemanager.presenters.SearchedFilesPresenter
import com.progressifff.filemanager.views.SearchedFilesView
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.lang.Exception

@RunWith(MockitoJUnitRunner::class)
class SearchedFilesPresenterTest {
    private lateinit var presenter: SearchedFilesPresenter
    private lateinit var view: SearchedFilesView
    private lateinit var multiSelectMode: MultiSelectMode
    private lateinit var eventBus: RxBus
    private lateinit var filesClipboard: FilesClipboard
    private lateinit var rootFolder: AbstractStorageFile
    private val searchedFiles = arrayListOf<AbstractStorageFile>(mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock())

    @Before
    fun setup(){
        eventBus = spy(RxBus)
        filesClipboard = spy(FilesClipboard)
        rootFolder = mock()
        `when`(rootFolder.search(ArgumentMatchers.anyString())).thenReturn(Observable.fromIterable(searchedFiles))
        multiSelectMode = mock()
        presenter = SearchedFilesPresenter(rootFolder, eventBus, filesClipboard, mock())
        presenter.multiSelectMode = multiSelectMode
        view = mock()
        presenter.bindView(view)
        reset(view)
    }

    @After
    fun release(){
        presenter.unbindView()
    }

    @Test
    fun checkSearchFiles(){
        presenter.searchFiles("")
        verify(view).updateFilesList()
        verify(view).showProgressBar()
        verify(view, times(searchedFiles.size)).insertFilesListEntry(ArgumentMatchers.anyInt())
        verify(view).hideProgressBar()
        verify(view).update()
    }

    @Test
    fun checkSearchFilesError(){
        `when`(rootFolder.search(ArgumentMatchers.anyString())).thenReturn(Observable.create<AbstractStorageFile>{ it.onError(Exception("Some error")) } )
        presenter.searchFiles("")
        verify(view).updateFilesList()
        verify(view).showProgressBar()
        verify(view).showToast(R.string.searching_files_error)
        verify(view).hideProgressBar()
        verify(view).update()
    }

    @Test
    fun checkFileListFolderClicked(){
        val folder = TestUtils.createMockedStorageFile(true)
        `when`(rootFolder.search(ArgumentMatchers.anyString())).thenReturn(Observable.just(folder) )
        presenter.searchFiles("")
        presenter.onFilesListEntryClicked(0, mock())
        verify(view).openFolder(folder)
    }

    @Test
    fun checkFileListFileClicked(){
        val file = TestUtils.createMockedStorageFile()
        `when`(rootFolder.search(ArgumentMatchers.anyString())).thenReturn(Observable.just(file))
        presenter.searchFiles("")
        presenter.onFilesListEntryClicked(0, mock())
        verify(view).showOpenFileDialog(file)
    }

    @Test
    fun checkFileListEntryClickedInMultiSelectMode(){
        `when`(multiSelectMode.running).thenReturn(true)
        presenter.searchFiles("")
        val selectedFileIndex = 0
        presenter.onFilesListEntryClicked(selectedFileIndex, mock())
        verify(multiSelectMode).take(searchedFiles[selectedFileIndex])
        verify(view).updateFilesListEntry(selectedFileIndex)
    }

    @Test
    fun checkFilesListMenuClicked(){
        presenter.searchFiles("")
        val fileIndex = 0
        presenter.onFileListEntryMenuClicked(fileIndex)
        verify(view).showFileActionsDialog(searchedFiles[fileIndex])
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            TestUtils.initRx()
        }
    }
}