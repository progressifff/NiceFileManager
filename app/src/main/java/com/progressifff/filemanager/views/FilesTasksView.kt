package com.progressifff.filemanager.views

import android.support.annotation.StringRes

interface FilesTasksView {
    fun showCopyExistingFilesDialog(filesNames: ArrayList<String>)
    fun showFilesTasksView()
    fun hideFilesTaskView()
    fun notifyFileTaskAdded()
    fun removeFileTask(taskIndex: Int)
    fun updateFileTask(taskIndex: Int)
    fun update()
    fun showToast(@StringRes messageId: Int)
}