package com.progressifff.nicefilemanager.views

import com.progressifff.nicefilemanager.AbstractTask

interface FileTaskView {
    fun updateTitle(task: AbstractTask)
    fun setProcessedFileName(fileName: String)
    fun setFileTaskProgress(taskProgress: Int)
}