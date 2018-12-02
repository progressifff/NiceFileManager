package com.progressifff.filemanager.views

import android.os.Parcelable
import com.progressifff.filemanager.MultiSelectMode
import com.progressifff.filemanager.models.AbstractFilesNode
import com.progressifff.filemanager.models.AbstractStorageFile

interface BaseFilesView{
    fun updateFilesListEntry(index: Int)
    fun insertFilesListEntry(index: Int)
    fun removeFilesListEntry(index: Int)
    fun update(animate: Boolean = true, resetListScrollPosition: Boolean = false)
    fun showRenameFileDialog(file: AbstractStorageFile)
    fun startActionMode(multiSelectMode: MultiSelectMode)
    fun showNoFilesMsg()
    fun showFilesList()
    fun showProgressBar()
    fun showFileDetailsDialog(file: AbstractStorageFile)
    fun showFileActionsDialog(file: AbstractStorageFile)
    fun showDeleteFilesDialog(filesCount: Int)
}

interface FilesView : BaseFilesView {
    fun restoreFilesListState(filesListSavedState: Parcelable?)
    fun setupToolBarScrollingBehavior(isEnabled: Boolean = true)
    fun showCreateFolderDialog(parentFolder: AbstractStorageFile)
    fun showSortTypeDialog(sortType: AbstractFilesNode.SortFilesType)
    fun setFilesInGridLayout()
    fun setFilesInListLayout()
    fun invalidateMenuOptions()
}

interface SearchedFilesView : BaseFilesView{
    fun hideProgressBar()
    fun resetFilesList()
    fun postResult(folder: AbstractStorageFile)
}
