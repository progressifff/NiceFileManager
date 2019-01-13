package com.progressifff.filemanager.views

import com.progressifff.filemanager.AbstractTask

interface FileTaskView {
    fun updateTitle(task: AbstractTask)
    fun setProcessedFileName(fileName: String)
    fun setFileTaskProgress(taskProgress: Int)
}