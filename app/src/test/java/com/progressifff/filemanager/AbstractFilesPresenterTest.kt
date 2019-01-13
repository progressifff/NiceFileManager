package com.progressifff.filemanager

import android.os.Parcelable
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.progressifff.filemanager.dialogs.FileActionsDialog
import com.progressifff.filemanager.views.FilesListEntryView
import com.progressifff.filemanager.views.FilesView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AbstractFilesPresenterTest {
    private val eventBus = spy<RxBus>()
    private val filesClipboard = spy<FilesClipboard>()
    private val fileDrawableLoader = spy<FileImageLoader>()
    private val multiSelectMode = mock<MultiSelectMode>()
    private lateinit var presenter: AbstractFilesPresenter<ArrayList<AbstractStorageFile>, FilesView>
    private val view = mock<FilesView>()
    private val file = mock<AbstractStorageFile>()

    @Before
    fun setup(){
        TestUtils.initRx()

        `when`(file.name).thenReturn("SimpleFile")
        `when`(file.lastModifiedDateTime).thenReturn("25 Nov 201 14:35")

        presenter = object : AbstractFilesPresenter<ArrayList<AbstractStorageFile>, FilesView>(eventBus,
                filesClipboard,
                fileDrawableLoader) {
            override var model = arrayListOf(file)
            override fun getFilesCount(): Int = model.size
            public override fun getFile(index: Int): AbstractStorageFile = model[index]
            override fun getFiles(): ArrayList<AbstractStorageFile> = model
            override fun onFilesListEntryClicked(index: Int, filesListState: Parcelable) { }
            override fun onFileListEntryMenuClicked(index: Int) { }
        }

        presenter.multiSelectMode = multiSelectMode

        presenter.bindView(view)
    }

    @After
    fun release(){
        presenter.unbindView()
    }

    @Test
    fun checkBindFileListEntryView(){
        val filesListEntryView = mock<FilesListEntryView>()
        `when`(filesListEntryView.getImageView()).thenReturn(mock())
        presenter.onBindFileListEntryView(0, filesListEntryView)
        verify(filesListEntryView).setName("SimpleFile")
        verify(filesListEntryView).setModificationDate("25 Nov 201 14:35")
        verify(filesListEntryView).setSelected(false)
    }

    @Test
    fun checkFileListItemLongClick(){
        `when`(multiSelectMode.running).thenReturn(false)
        val index = 0
        presenter.onFileListItemLongClick(index)
        verify(view).startActionMode(multiSelectMode)
        verify(multiSelectMode).take(file)
        verify(view).updateFilesListEntry(index)
    }

    @Test
    fun checkRenameFileAction(){
        eventBus.publish(RxEvent.FileActionEvent(file, FileActionsDialog.FileAction.RENAME))
        verify(view).showRenameFileDialog(file)
    }

    @Test
    fun checkDeleteFileAction(){
        eventBus.publish(RxEvent.FileActionEvent(file, FileActionsDialog.FileAction.DELETE))
        verify(eventBus).publish(any<RxEvent.DeleteFilesEvent>())
        verify(view).showDeleteFilesDialog(1)
    }

    @Test
    fun checkCopyFileAction(){
        eventBus.publish(RxEvent.FileActionEvent(file, FileActionsDialog.FileAction.COPY))
        verify(filesClipboard).clipData = any()
    }

    @Test
    fun checkCutFileAction(){
        eventBus.publish(RxEvent.FileActionEvent(file, FileActionsDialog.FileAction.CUT))
        verify(filesClipboard).clipData = any()
    }

    @Test
    fun checkShareFileAction(){
        eventBus.publish(RxEvent.FileActionEvent(file, FileActionsDialog.FileAction.SHARE))
        verify(view).showShareDialog(file)
    }

    @Test
    fun checkFileDetailsAction(){
        eventBus.publish(RxEvent.FileActionEvent(file, FileActionsDialog.FileAction.DETAILS))
        verify(view).showFileDetailsDialog(file)
    }

    @Test
    fun checkMenuDeleteActionClicked(){
        `when`(multiSelectMode.selectedFiles).thenReturn(arrayListOf(file))
        val menuItem = TestUtils.createMockedMenuItem(R.id.action_delete)
        presenter.onMenuActionClicked(menuItem)
        verify(view).showDeleteFilesDialog(1)
    }

    @Test
    fun checkMenuCopyActionClicked(){
        `when`(multiSelectMode.selectedFiles).thenReturn(arrayListOf(file))
        val menuItem = TestUtils.createMockedMenuItem(R.id.action_copy)
        presenter.onMenuActionClicked(menuItem)
        verify(filesClipboard).clipData = any()
        verify(multiSelectMode).cancel()
    }

    @Test
    fun checkMenuCutActionClicked(){
        `when`(multiSelectMode.selectedFiles).thenReturn(arrayListOf(file))
        val menuItem = TestUtils.createMockedMenuItem(R.id.action_cut)
        presenter.onMenuActionClicked(menuItem)
        verify(filesClipboard).clipData = any()
        verify(multiSelectMode).cancel()
    }

    @Test
    fun checkMenuSelectAllActionClicked(){
        val menuItem = TestUtils.createMockedMenuItem(R.id.action_select_all)
        presenter.onMenuActionClicked(menuItem)
        verify(multiSelectMode).takeAll(any())
        verify(view).update(false)
    }
}