package com.progressifff.filemanager.presenters

import android.os.Parcelable
import android.support.annotation.NonNull
import com.progressifff.filemanager.*
import com.progressifff.filemanager.AbstractStorageFile
import com.progressifff.filemanager.views.SearchedFilesView
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver

class SearchedFilesPresenter(private var rootFolder: AbstractStorageFile,
                             eventBus: RxBus,
                             filesClipboard: FilesClipboard,
                             fileImageLoader: FileImageLoader) : AbstractFilesPresenter<ArrayList<AbstractStorageFile>, SearchedFilesView>( eventBus,
                                                                                                                                            filesClipboard,
                                                                                                                                            fileImageLoader) {

    private var fileDeleteEventListenerDisposable: Disposable? = null
    private var fileRenamedEventListenerDisposable: Disposable? = null
    private var searchDisposable: Disposable? = null
    override var model = arrayListOf<AbstractStorageFile>()

    init { multiSelectMode.eventsListener = this }

    override fun getFiles(): ArrayList<AbstractStorageFile> = model

    override fun getFilesCount(): Int = model.size

    override fun getFile(index: Int): AbstractStorageFile = model[index]

    override fun onFilesListEntryClicked(index: Int, filesListState: Parcelable) {
        try{
            val file = model[index]
            if(!multiSelectMode.running){
                if(file.isDirectory){
                    view!!.openFolder(file)
                }
                else {
                    view!!.showOpenFileDialog(file)
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

    override fun bindView(@NonNull v: SearchedFilesView){
        super.bindView(v)

        //If search performed
        if(searchDisposable != null){
            view?.update()
        }

        fileDeleteEventListenerDisposable = eventBus.listen(RxEvent.FileDeletedEvent::class.java).subscribe{ event ->
            val fileIndex = model.indexOf(event.file)
            if(fileIndex >= 0){
                model.removeAt(fileIndex)
                view?.removeFilesListEntry(fileIndex)
            }
        }

        fileRenamedEventListenerDisposable = eventBus.listen(RxEvent.FileRenamedEvent::class.java).subscribe{ event ->
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

    fun searchFiles(query: String) {
        if(searchDisposable != null && searchDisposable!!.isDisposed){
            searchDisposable!!.dispose()
        }
        model.clear()
        view!!.updateFilesList()
        view!!.showProgressBar()
        searchDisposable = rootFolder.search(query).subscribeWith(object : DisposableObserver<AbstractStorageFile>(){
            override fun onComplete() {
                view?.hideProgressBar()
                view?.update()
            }

            override fun onNext(file: AbstractStorageFile) {
                model.add(file)
                view?.insertFilesListEntry(model.size - 1)
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
                model.clear()
                view!!.showToast(R.string.searching_files_error)
                view?.hideProgressBar()
                view?.update()
            }
        })
    }
}