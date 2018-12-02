package com.progressifff.filemanager.presenters

import com.progressifff.filemanager.*
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.views.FileTaskView
import com.progressifff.filemanager.views.IFilesTasksView
import io.reactivex.disposables.Disposable

class FilesTasksPresenter : BasePresenter<FilesTasksManager, IFilesTasksView>(){

    override var model = FilesTasksManager.instance

    val tasksCount: Int get() = model.tasksCount

    private lateinit var newFilesTaskEventListenerDisposable: Disposable

    private val filesTasksStatusListener = object: FilesTasksManager.FileTaskStatusListener {

        override fun onNewTask() {
            view?.notifyFileTaskAdded()
        }

        override fun onTaskUpdated(taskIndex: Int, completed: Boolean) {
            view?.updateFileTask(taskIndex)
            if(completed){
                view?.removeFileTask(taskIndex)
            }
        }

        override fun onProcessingExistingFiles(callback: (existingFileAction: AbstractStorageFile.ExistingFileAction) -> Unit, existingFilesNames: ArrayList<String>) {
            RxBus.publish(RxEvent.PasteExistingFilesEvent(callback))
            view!!.showCopyExistingFilesDialog(existingFilesNames)
        }
    }

    override fun bindView(v: IFilesTasksView) {
        super.bindView(v)
        model.fileTaskStatusListener = filesTasksStatusListener
        newFilesTaskEventListenerDisposable = RxBus.listen(RxEvent.NewFilesTaskEvent::class.java).subscribe(::onNewFilesTaskEvent)
        updateView()
    }

    override fun unbindView() {
        super.unbindView()
        if(model.fileTaskStatusListener == filesTasksStatusListener) {
            model.fileTaskStatusListener = null
        }
        newFilesTaskEventListenerDisposable.dispose()
    }

    override fun updateView() {
        super.updateView()
        view?.update()
    }

    private fun onNewFilesTaskEvent(event: RxEvent.NewFilesTaskEvent){
        model.add(event.task)
    }

    fun onBindFileTaskView(fileTaskView: FileTaskView, index: Int){
        val task = model.get(index)
        fileTaskView.setProcessedFileName(task.currentProcessingFile.get())
        fileTaskView.setFileTaskProgress(task.progress.get())

        var titleToken = "${task.processingFilesCount} ${if(task.files.count() == 1) getStringFromRes(R.string.file) else getStringFromRes(R.string.files)} "
        titleToken += if(task is CopyCutTask) {
            "${getStringFromRes(R.string.from)} ${task.files.first().parent!!.name} ${getStringFromRes(R.string.to)} ${task.destFolder.name}."
        }
        else{
            "in ${task.files.first().parent!!.name}."
        }

        when (task) {
            is CopyTask -> fileTaskView.setTaskTitle("${getStringFromRes(R.string.copying_task)} $titleToken")
            is CutTask -> fileTaskView.setTaskTitle("${getStringFromRes(R.string.cut_task)} $titleToken")
            else -> fileTaskView.setTaskTitle("${getStringFromRes(R.string.deleting_task)} $titleToken")
        }
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