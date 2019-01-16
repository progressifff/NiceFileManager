package com.progressifff.nicefilemanager

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.progressifff.nicefilemanager.presenters.FilesTasksPresenter
import com.progressifff.nicefilemanager.views.FileTaskView
import com.progressifff.nicefilemanager.views.FilesTasksView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.atomic.AtomicReference

@RunWith(MockitoJUnitRunner::class)
class FilesTasksPresenterTest {
    private val eventBus = spy<RxBus>()
    private var presenter = FilesTasksPresenter()
    private val view = mock<FilesTasksView>()
    private val filesTask = mock<AbstractTask>()

    @Before
    fun setup(){
        TestUtils.initRx()
        presenter.bindView(view)
        verify(view).update()
        `when`(filesTask.currentProcessingFile).thenReturn(AtomicReference("SimpleFile"))
        `when`(filesTask.progress).thenReturn(AtomicReference(1))
        `when`(filesTask.files).thenReturn(arrayListOf(mock()))
        eventBus.publish(RxEvent.NewFilesTaskEvent(filesTask))
        verify(view).notifyFileTaskAdded()
        eventBus.clearHistory()
    }

    @After
    fun release(){
        presenter.unbindView()
    }

    @Test
    fun checkBindFilesTaskView(){
        val filesTaskView = mock<FileTaskView>()
        presenter.onBindFileTaskView(filesTaskView, 0)
        verify(filesTaskView).setProcessedFileName("SimpleFile")
        verify(filesTaskView).setFileTaskProgress(1)
        verify(filesTaskView).updateTitle(filesTask)
    }

    @Test
    fun checkCancelTask(){
        val taskIndex = 0
        presenter.onCancelTask(taskIndex)
        verify(view).updateFileTask(taskIndex)
        verify(view).removeFileTask(taskIndex)
    }

    @Test
    fun checkCancelAllTasks(){
        presenter.onCancelAllTasks()
        verify(view).updateFileTask(0)
        verify(view).removeFileTask(0)
    }
}