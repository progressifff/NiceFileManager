package com.progressifff.filemanager.presenters

import com.progressifff.filemanager.*
import com.progressifff.filemanager.AbstractStorageFile
import com.progressifff.filemanager.views.FileTaskView
import com.progressifff.filemanager.views.FilesTasksView
import io.reactivex.disposables.Disposable

class FilesTasksPresenter : BasePresenter<FilesTasksManager, FilesTasksView>(){

    override var model = FilesTasksManager

    val tasksCount: Int get() = model.tasksCount

    private lateinit var newFilesTaskEventListenerDisposable: Disposable

    private val filesTasksStatusListener = object: FilesTasksManager.EventsListener {

        override fun onNewTask() {
            view?.notifyFileTaskAdded()
        }

        override fun onTaskUpdated(taskIndex: Int, completed: Boolean) {
            view?.updateFileTask(taskIndex)
            if(completed){
                view?.removeFileTask(taskIndex)
            }
        }

        override fun onError(messageId: Int) {
            view?.showToast(messageId)
        }

        override fun onProcessingExistingFiles(callback: (existingFileAction: AbstractStorageFile.ExistingFileAction) -> Unit, existingFilesNames: ArrayList<String>) {
            RxBus.publish(RxEvent.PasteExistingFilesEvent(callback))
            view!!.showCopyExistingFilesDialog(existingFilesNames)
        }
    }

    override fun bindView(v: FilesTasksView) {
        super.bindView(v)
        model.eventsListener = filesTasksStatusListener
        newFilesTaskEventListenerDisposable = RxBus.listen(RxEvent.NewFilesTaskEvent::class.java).subscribe(::onNewFilesTaskEvent)
        view?.update()
    }

    override fun unbindView() {
        super.unbindView()
        if(model.eventsListener == filesTasksStatusListener) {
            model.eventsListener = null
        }
        newFilesTaskEventListenerDisposable.dispose()
    }

    private fun onNewFilesTaskEvent(event: RxEvent.NewFilesTaskEvent){
        model.add(event.task)
    }

    fun onBindFileTaskView(fileTaskView: FileTaskView, index: Int){
        val task = model.get(index)
        fileTaskView.setProcessedFileName(task.currentProcessingFile.get())
        fileTaskView.setFileTaskProgress(task.progress.get())
        fileTaskView.updateTitle(task)
    }

    fun onCancelTask(index: Int){
        model.removeTask(index)
    }

    fun onCancelAllTasks(){
        for(index in 0..(model.tasksCount - 1)){
            model.removeTask(index)
        }
    }
}