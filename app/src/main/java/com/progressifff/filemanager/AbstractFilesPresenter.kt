package com.progressifff.filemanager

import android.os.Parcelable
import android.support.annotation.CallSuper
import android.view.MenuItem
import com.progressifff.filemanager.dialogs.FileActionsDialog
import com.progressifff.filemanager.views.FilesView
import com.progressifff.filemanager.views.FilesListEntryView
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference

abstract class AbstractFilesPresenter<M, V : FilesView>(protected val eventBus: RxBus,
                                                        protected val filesClipboard: FilesClipboard,
                                                        protected val fileImageLoader: FileImageLoader) : BasePresenter<M, V>(),
                                                                                                          MultiSelectMode.EventsListener{
    private lateinit var fileActionEventListenerDisposable: Disposable
    var multiSelectMode = MultiSelectMode()

    @CallSuper
    override fun bindView(v: V) {
        super.bindView(v)
        fileActionEventListenerDisposable = eventBus.listen(RxEvent.FileActionEvent::class.java).subscribe(::onFileActionEvent)
    }

    @CallSuper
    override fun unbindView() {
        super.unbindView()
        fileActionEventListenerDisposable.dispose()
        eventBus.clearHistory()
    }

    open fun onBindFileListEntryView(index: Int, filesListEntryView: FilesListEntryView){
        val file = getFile(index)
        fileImageLoader.applyFileImage(file, WeakReference(filesListEntryView.getImageView()))
        filesListEntryView.setName(file.name)
        filesListEntryView.setModificationDate(file.lastModifiedDateTime)
        filesListEntryView.setSelected(multiSelectMode.isFileSelected(file))
    }

    abstract fun getFilesCount(): Int

    protected abstract fun getFile(index: Int): AbstractStorageFile

    protected abstract fun getFiles(): ArrayList<AbstractStorageFile>

    open fun onFileListItemLongClick(index: Int) {
        if(!multiSelectMode.running) {
            view!!.startActionMode(multiSelectMode)
        }
        multiSelectMode.take(getFile(index))
        view!!.updateFilesListEntry(index)
    }

    private fun onFileActionEvent(event: RxEvent.FileActionEvent) {

        when(event.action){

            FileActionsDialog.FileAction.RENAME -> view!!.showRenameFileDialog(event.file)

            FileActionsDialog.FileAction.DELETE -> deleteFile(event.file)

            FileActionsDialog.FileAction.COPY -> filesClipboard.clipData = FilesClipboard.ClipData(FilesClipboard.ClipData.Action.COPY, arrayListOf(event.file))

            FileActionsDialog.FileAction.CUT -> filesClipboard.clipData = FilesClipboard.ClipData(FilesClipboard.ClipData.Action.CUT, arrayListOf(event.file))

            FileActionsDialog.FileAction.SHARE -> view!!.showShareDialog(event.file)

            FileActionsDialog.FileAction.DETAILS -> view!!.showFileDetailsDialog(event.file)
        }
    }

    protected open fun deleteFile(file: AbstractStorageFile) {
        deleteFiles(arrayListOf(file))
    }

    protected open fun deleteFiles(files: ArrayList<AbstractStorageFile>) {
        val taskFiles = ArrayList(files)
        eventBus.publish(RxEvent.DeleteFilesEvent {
            multiSelectMode.cancel()
            val deleteTask = DeleteTask(taskFiles)
            eventBus.publish(RxEvent.NewFilesTaskEvent(deleteTask))
        })
        view!!.showDeleteFilesDialog(filesCount = files.size)
    }

    override fun onActionModeDestroyed() {
        view?.update(false, false)
    }

    override fun onMenuActionClicked(menuItem: MenuItem) {
        when(menuItem.itemId) {

            R.id.action_delete -> {
                deleteFiles(multiSelectMode.selectedFiles)
            }

            R.id.action_copy -> {
                filesClipboard.clipData = FilesClipboard.ClipData(FilesClipboard.ClipData.Action.COPY, ArrayList(multiSelectMode.selectedFiles))
                multiSelectMode.cancel()
            }

            R.id.action_cut -> {
                filesClipboard.clipData = FilesClipboard.ClipData(FilesClipboard.ClipData.Action.CUT, ArrayList(multiSelectMode.selectedFiles))
                multiSelectMode.cancel()
            }

            R.id.action_select_all -> {
                multiSelectMode.takeAll(getFiles())
                view?.update(false)
            }
        }
    }

    abstract fun onFilesListEntryClicked(index: Int, filesListState: Parcelable)

    abstract fun onFileListEntryMenuClicked(index: Int)
}