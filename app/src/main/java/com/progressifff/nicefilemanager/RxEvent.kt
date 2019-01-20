package com.progressifff.nicefilemanager

import android.os.Parcelable
import com.progressifff.nicefilemanager.dialogs.FileActionsDialog
import com.progressifff.nicefilemanager.presenters.MainPresenter

class RxEvent {

    data class NavigateEvent(val filesNode: AbstractFilesNode, val filesListState: Parcelable? = null, var processed: () -> Unit = {})

    data class SaveFilesStateEvent(val filesNode: AbstractFilesNode, val filesListState: Parcelable?)

    data class OpenFolderEvent(val filesNode: AbstractFilesNode)

    data class NewFilesTaskEvent(val task: AbstractTask)

    data class FilesDisplayModeChangedEvent(val displayMode: MainPresenter.FilesDisplayMode)

    data class FilesOrderModeChangedEvent(val orderMode: MainPresenter.FilesOrderMode)

    data class NavigationDrawerStateChangedEvent(val isDragging: Boolean)

    data class FilesSortTypeChangedEvent(val sortType: AbstractFilesNode.SortFilesType)

    data class PasteExistingFilesEvent(val callback: (existingFileAction: AbstractStorageFile.ExistingFileAction) -> Unit)

    data class FileActionEvent(val file: AbstractStorageFile, val action: FileActionsDialog.FileAction)

    data class DeleteFilesEvent(val callback: () -> Unit)

    data class FileDeletedEvent(val file: AbstractStorageFile)

    data class FileRenamedEvent(val file: AbstractStorageFile)
}