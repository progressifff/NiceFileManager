package com.progressifff.filemanager.views

interface IFilesTasksView {
    fun showCopyExistingFilesDialog(filesNames: ArrayList<String>)
    fun showFilesTasksView()
    fun hideFilesTaskView()
    fun notifyFileTaskAdded()
    fun removeFileTask(taskIndex: Int)
    fun updateFileTask(taskIndex: Int)
    fun update()
}