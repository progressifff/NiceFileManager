package com.progressifff.filemanager.presenters

import android.os.Parcelable
import android.support.annotation.NonNull
import com.progressifff.filemanager.*
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.views.SearchedFilesView
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver

class SearchedFilesPresenter(var rootFolder: AbstractStorageFile) : AbstractFilesPresenter<ArrayList<AbstractStorageFile>, SearchedFilesView>() {

    private var fileDeleteEventListenerDisposable: Disposable? = null
    private var fileRenamedEventListenerDisposable: Disposable? = null
    override val multiSelectMode = MultiSelectMode(this)
    override var model = arrayListOf<AbstractStorageFile>()
    private lateinit var searchDisposable: Disposable

    override fun getFiles(): ArrayList<AbstractStorageFile> = model

    override fun getFilesCount(): Int = model.size

    override fun getFile(index: Int): AbstractStorageFile = model[index]

    override fun onFileListEntryClicked(index: Int, filesListState: Parcelable) {

        try{
            val file = model[index]

            if(!multiSelectMode.running){
                if(file.isDirectory){
                    view!!.postResult(file)
                }
                else {
                    try{
                        file.openAsFile()
                    }
                    catch (e: Exception){
                        showToast(App.get().getString(R.string.open_file_error))
                    }
                }
            }
            else{
                multiSelectMode.take(file)
                view!!.updateFilesListEntry(index)
            }
        }
        catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onFileListEntryMenuClicked(index: Int) {
        view!!.showFileActionsDialog(getFile(index))
    }

    override fun updateView() {
        view?.update(true, false)
    }

    override fun bindView(@NonNull v: SearchedFilesView){
        super.bindView(v)
        updateView()

        fileDeleteEventListenerDisposable = RxBus.listen(RxEvent.FileDeletedEvent::class.java).subscribe{ event ->
            val fileIndex = model.indexOf(event.file)
            if(fileIndex >= 0){
                model.removeAt(fileIndex)
                view?.removeFilesListEntry(fileIndex)
            }
        }

        fileRenamedEventListenerDisposable = RxBus.listen(RxEvent.FileRenamedEvent::class.java).subscribe{ event ->
            val fileIndex = model.indexOf(event.file)
            if(fileIndex >= 0){
                view?.updateFilesListEntry(fileIndex)
            }
        }
    }

    override fun unbindView() {
        super.unbindView()
        fileDeleteEventListenerDisposable?.dispose()
        fileRenamedEventListenerDisposable?.dispose()
    }

    override fun deleteFiles(files: ArrayList<AbstractStorageFile>) {
        val taskFiles = ArrayList(files)
        RxBus.publish(RxEvent.DeleteFilesEvent {
            multiSelectMode.cancel()
            val deleteTask = DeleteTask(taskFiles)
            RxBus.publish(RxEvent.NewFilesTaskEvent(deleteTask))
        })
        view!!.showDeleteFilesDialog(filesCount = files.size)
    }

    fun searchFiles(query: String) {
        if(::searchDisposable.isInitialized && searchDisposable.isDisposed){
            searchDisposable.dispose()
        }
        view!!.showFilesList()
        model.clear()
        view!!.resetFilesList()

        view!!.showProgressBar()

        val filesSource = rootFolder.search(query)
        searchDisposable = filesSource.subscribeWith(object : DisposableObserver<AbstractStorageFile>(){
            override fun onComplete() {
                view?.hideProgressBar()
                updateView()
            }

            override fun onNext(file: AbstractStorageFile) {
                model.add(file)
                view?.insertFilesListEntry(model.size - 1)
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
                model.clear()
                showToast("An error occurs searching in ${rootFolder.name}")
                updateView()
            }
        })
    }
}