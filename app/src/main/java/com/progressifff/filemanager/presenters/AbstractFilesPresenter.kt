package com.progressifff.filemanager.presenters

import android.os.Parcelable
import android.support.annotation.CallSuper
import android.view.MenuItem
import com.progressifff.filemanager.*
import com.progressifff.filemanager.dialogs.FileActionsDialog
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.views.BaseFilesView
import com.progressifff.filemanager.views.FilesListEntryView
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference

abstract class AbstractFilesPresenter<M, V : BaseFilesView> :
        BasePresenter<M, V>() {

    protected abstract val multiSelectMode: MultiSelectMode
    private lateinit var fileActionEventListenerDisposable: Disposable

    @CallSuper
    override fun bindView(v: V) {
        super.bindView(v)
        fileActionEventListenerDisposable = RxBus.listen(RxEvent.FileActionEvent::class.java).subscribe(::onFileActionEvent)
    }

    @CallSuper
    override fun unbindView() {
        super.unbindView()
        fileActionEventListenerDisposable.dispose()
        RxBus.clearHistory()
    }

    open fun onBindFileListEntryView(index: Int, filesListEntryView: FilesListEntryView){
        val file = getFile(index)
        FileDrawableLoader.instance.applyFileImage(file, WeakReference(filesListEntryView.getImageView()))
        filesListEntryView.setName(file.name)
        filesListEntryView.setModificationDate(file.lastModifiedDateTime)
        filesListEntryView.setSelected(multiSelectMode.isFileSelected(file))
    }

    abstract fun getFilesCount(): Int

    protected abstract fun getFile(index: Int): AbstractStorageFile

    protected abstract fun getFiles(): ArrayList<AbstractStorageFile>

    open fun onFileListItemLongClick(index: Int): Boolean {
        if(!multiSelectMode.running) {
            view!!.startActionMode(multiSelectMode)
        }
        multiSelectMode.take(getFile(index))
        view!!.updateFilesListEntry(index)
        return true
    }

    open fun onSelectedFilesActionClicked(action: MenuItem){

        when(action.itemId) {

            R.id.action_delete -> {
                deleteFiles(multiSelectMode.selectedFiles)
            }

            R.id.action_copy -> {
                FilesClipboard.instance.clipData = FilesClipboard.ClipData(FilesClipboard.ClipData.Action.COPY, ArrayList(multiSelectMode.selectedFiles))
                multiSelectMode.cancel()
            }

            R.id.action_cut -> {
                FilesClipboard.instance.clipData = FilesClipboard.ClipData(FilesClipboard.ClipData.Action.CUT, ArrayList(multiSelectMode.selectedFiles))
                multiSelectMode.cancel()
            }

            R.id.action_select_all -> {
                multiSelectMode.takeAll(getFiles())
                view?.update(false, false)
            }
        }
    }

    private fun onFileActionEvent(event: RxEvent.FileActionEvent) {

        when(event.action){

            FileActionsDialog.FileAction.RENAME -> view!!.showRenameFileDialog(event.file)

            FileActionsDialog.FileAction.DELETE -> deleteFile(event.file)

            FileActionsDialog.FileAction.COPY -> FilesClipboard.instance.clipData = FilesClipboard.ClipData(FilesClipboard.ClipData.Action.COPY, arrayListOf(event.file))

            FileActionsDialog.FileAction.CUT -> FilesClipboard.instance.clipData = FilesClipboard.ClipData(FilesClipboard.ClipData.Action.CUT, arrayListOf(event.file))

            FileActionsDialog.FileAction.SHARE -> event.file.share()

            FileActionsDialog.FileAction.DETAILS -> view!!.showFileDetailsDialog(event.file)
        }
    }

    open fun onActionModeDestroyed(){
        view?.update(false, false)
    }

    open fun deleteFile(file: AbstractStorageFile) {
        deleteFiles(arrayListOf(file))
    }

    open fun deleteFiles(files: ArrayList<AbstractStorageFile>) {
        val taskFiles = ArrayList(files)
        RxBus.publish(RxEvent.DeleteFilesEvent {
            multiSelectMode.cancel()

            App.get().handler.postDelayed({
                val deleteTask = DeleteTask(taskFiles)
                RxBus.publish(RxEvent.NewFilesTaskEvent(deleteTask))
            }, 100)
        })
        view!!.showDeleteFilesDialog(filesCount = files.size)
    }

    abstract fun onFileListEntryClicked(index: Int, filesListState: Parcelable)

    abstract fun onFileListEntryMenuClicked(index: Int)
}