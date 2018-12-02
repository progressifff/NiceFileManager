package com.progressifff.filemanager.views

interface FileTaskView {
    fun setTaskTitle(title: String)
    fun setProcessedFileName(fileName: String)
    fun setFileTaskProgress(taskProgress: Int)
}