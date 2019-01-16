package com.progressifff.nicefilemanager.views

import android.os.Parcelable
import android.support.annotation.StringRes
import com.progressifff.nicefilemanager.MultiSelectMode
import com.progressifff.nicefilemanager.AbstractFilesNode
import com.progressifff.nicefilemanager.AbstractStorageFile

interface FilesView{
    fun updateFilesListEntry(index: Int)
    fun insertFilesListEntry(index: Int)
    fun removeFilesListEntry(index: Int)
    fun update(animate: Boolean = true, resetListScrollPosition: Boolean = false)
    fun startActionMode(multiSelectMode: MultiSelectMode)
    fun showNoFilesMsg()
    fun showFilesList()
    fun showProgressBar()
    fun showRenameFileDialog(file: AbstractStorageFile)
    fun showFileDetailsDialog(file: AbstractStorageFile)
    fun showFileActionsDialog(file: AbstractStorageFile)
    fun showDeleteFilesDialog(filesCount: Int)
    fun showToast(@StringRes messageId: Int)
    fun showShareDialog(file: AbstractStorageFile)
    fun showOpenFileDialog(file: AbstractStorageFile)
}

interface NestedFilesView : FilesView {
    fun restoreFilesListState(filesListSavedState: Parcelable?)
    fun setupToolBarScrollingBehavior(isEnabled: Boolean = true)
    fun showCreateFolderDialog(parentFolder: AbstractStorageFile)
    fun showSortTypeDialog(sortType: AbstractFilesNode.SortFilesType)
    fun setFilesInGridLayout()
    fun setFilesInListLayout()
    fun invalidateMenu()
}

interface SearchedFilesView : FilesView{
    fun hideProgressBar()
    fun updateFilesList()
    fun openFolder(folder: AbstractStorageFile)
}
