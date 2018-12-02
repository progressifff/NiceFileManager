package com.progressifff.filemanager.presenters

import android.os.Parcelable
import android.support.annotation.NonNull
import android.support.v4.widget.SwipeRefreshLayout
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.progressifff.filemanager.*
import com.progressifff.filemanager.Constants.SHOW_HIDDEN_FILES_KEY
import com.progressifff.filemanager.models.AbstractFilesNode
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.models.FilesNode
import com.progressifff.filemanager.views.FilesView
import io.reactivex.disposables.Disposable
import kotlin.collections.ArrayList

class FilesPresenter : AbstractFilesPresenter<AbstractFilesNode, FilesView>(),
                                                SwipeRefreshLayout.OnRefreshListener{

    override lateinit var model: AbstractFilesNode

    private val fileNodeEventsListener = object: AbstractFilesNode.EventsListener{

        override fun onStartUpdate() {
            view?.showProgressBar()
        }

        override fun onFileChanged(index: Int) {
            view?.updateFilesListEntry(index)
        }

        override fun onFileRemoved(index: Int) {
            view?.removeFilesListEntry(index)
        }

        override fun onFileCreated(index: Int) {
            view?.insertFilesListEntry(index)
        }

        override fun onUpdated() {
            view?.update(true, true)
        }

        override fun onError(msg: String) {
            showToast(msg)
            view?.showNoFilesMsg()
        }
    }

    private val clipboardListener = object: FilesClipboard.FilesClipboardListener{
        override fun onClipDataChanged() {
            view?.invalidateMenuOptions()
        }
    }

    override val multiSelectMode = MultiSelectMode(this)

    val onViewClickListener = View.OnClickListener {v->
        when(v.id){
            R.id.addFolderFab -> view!!.showCreateFolderDialog(model.source)
        }
    }

    private lateinit var navigateEventListenerDisposable: Disposable
    private lateinit var navDrawerStateListenerDisposable: Disposable
    private lateinit var displayModeEventListenerDisposable: Disposable
    private lateinit var orderModeEventListenerDisposable: Disposable
    private lateinit var sortTypeEventListenerDisposable: Disposable

    init {
        initRxEventsListeners()
    }

    override fun bindView(@NonNull v: FilesView){
        super.bindView(v)
        if(multiSelectMode.running){ view!!.startActionMode(multiSelectMode) }
        FilesClipboard.instance.filesClipboardListener = clipboardListener
        initRxEventsListeners()
        view!!.update(false)
        view!!.invalidateMenuOptions()
    }

    override fun unbindView() {
        if(multiSelectMode.running){
            multiSelectMode.saveState()
        }
        navigateEventListenerDisposable.dispose()
        navDrawerStateListenerDisposable.dispose()
        displayModeEventListenerDisposable.dispose()
        orderModeEventListenerDisposable.dispose()
        sortTypeEventListenerDisposable.dispose()
        super.unbindView()
    }

    private fun initRxEventsListeners(){

        navigateEventListenerDisposable = RxBus.listen(
                RxEvent.NavigateEvent::class.java).
                subscribe(::onNavigateEvent)

        navDrawerStateListenerDisposable = RxBus.listen(
                RxEvent.NavigationDrawerStateChangedEvent::class.java).
                subscribe(::onNavigationDrawerStateChangedEvent)

        displayModeEventListenerDisposable = RxBus.listen(
                RxEvent.FilesDisplayModeChangedEvent::class.java).
                subscribe(::onFilesDisplayModeChangedEvent)

        orderModeEventListenerDisposable = RxBus.listen(
                RxEvent.FilesOrderModeChangedEvent::class.java).
                subscribe(::onFilesOrderModeChangedEvent)

        sortTypeEventListenerDisposable = RxBus.listen(
                RxEvent.FilesSortTypeChangedEvent::class.java).
                subscribe{event -> model.sortFilesType = event.sortType}
    }

    override fun onRefresh() {
        if(::model.isInitialized){
            model.load()
        }
    }

    private fun onNavigateEvent(event: RxEvent.NavigateEvent){
        if(multiSelectMode.running) {
            multiSelectMode.cancel()
        }

        if(!::model.isInitialized){
            changeModel(event.filesNode)
            model.load()
            model.sort()
        }
        else{
            val isModelSortRequired = (event.filesNode.sortFilesType != model.sortFilesType ||
                    event.filesNode.isDescendingSort != model.isDescendingSort)

            val isModelUpdateRequired = (event.filesNode is FilesNode && model is FilesNode) &&
                                        (event.filesNode.hiddenFilesAreShown != (model as FilesNode).hiddenFilesAreShown)

            changeModel(event.filesNode)

            if(!model.isLoaded || isModelUpdateRequired){
                model.load()
            }
            else if(isModelSortRequired){
                model.sort()
            }
            else{
                view?.update(true, true)
            }
        }

        view?.restoreFilesListState(event.filesListState)
    }

    private fun onNavigationDrawerStateChangedEvent(event: RxEvent.NavigationDrawerStateChangedEvent) {
        if(event.isDragging){
            if(multiSelectMode.running) {
                multiSelectMode.hide()
            }
        }
        else{
            if(multiSelectMode.hidden) {
                view?.startActionMode(multiSelectMode)
            }
        }
    }

    private fun onFilesDisplayModeChangedEvent(event: RxEvent.FilesDisplayModeChangedEvent){
        when (event.displayMode){
            MainPresenter.FilesDisplayMode.GRID -> view?.setFilesInGridLayout()
            MainPresenter.FilesDisplayMode.LIST -> view?.setFilesInListLayout()
        }
    }

    private fun onFilesOrderModeChangedEvent(event: RxEvent.FilesOrderModeChangedEvent){
        model.isDescendingSort = event.orderMode == MainPresenter.FilesOrderMode.DESCENDING
    }

    override fun onFileListEntryClicked(index: Int, filesListState: Parcelable) {
        try{
            val file = model.get(index)

            if(!multiSelectMode.running){
                if(file.isDirectory){
                    val filesNode = FilesNode(file)
                    RxBus.publish(RxEvent.OpenFolderEvent(filesNode))
                    RxBus.publish(RxEvent.SaveFilesStateEvent(model, filesListState))
                    changeModel(filesNode)
                    model.load()
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

    override fun onFileListEntryMenuClicked(index: Int){
        view!!.showFileActionsDialog(getFile(index))
    }

    override fun onActionModeDestroyed() {
        super.onActionModeDestroyed()
        view?.setupToolBarScrollingBehavior()
    }

    override fun getFilesCount(): Int = if(::model.isInitialized) model.files.size else 0

    override fun getFile(index: Int): AbstractStorageFile = model.files.elementAt(index)

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){

            R.id.paste -> {
                if(FilesClipboard.instance.isNotEmpty){
                    when(FilesClipboard.instance.clipData!!.action){
                        FilesClipboard.ClipData.Action.COPY -> {
                            RxBus.publish(RxEvent.NewFilesTaskEvent(CopyTask(FilesClipboard.instance.clipData!!.files, model.source)))
                        }

                        FilesClipboard.ClipData.Action.CUT -> {
                            RxBus.publish(RxEvent.NewFilesTaskEvent(CutTask(FilesClipboard.instance.clipData!!.files, model.source)))
                            FilesClipboard.instance.clear()
                        }
                    }
                }
            }

            R.id.sortType -> view!!.showSortTypeDialog(model.sortFilesType)

            R.id.showHiddenFiles -> {
                item.isChecked = !item.isChecked
                (model as FilesNode).hiddenFilesAreShown = item.isChecked
                saveBooleanToSharedPreferences(SHOW_HIDDEN_FILES_KEY, item.isChecked)
            }
            else -> return false
        }
        return true
    }

    fun onPrepareOptionsMenu(menu: Menu){
        val pasteMenuItem = menu.findItem(R.id.paste)
        pasteMenuItem!!.isEnabled = FilesClipboard.instance.isNotEmpty
        menu.findItem(R.id.showHiddenFiles)?.isChecked = getBooleanFromSharedPreferences(SHOW_HIDDEN_FILES_KEY)
    }

    private fun changeModel(node: AbstractFilesNode){
        if(!::model.isInitialized){
            model = node
            model.subscribe(fileNodeEventsListener)
        }
        else{
            if(node is FilesNode &&  model is FilesNode){
                node.hiddenFilesAreShown = (model as FilesNode).hiddenFilesAreShown
            }
            node.sortFilesType = model.sortFilesType
            node.isDescendingSort = model.isDescendingSort
            model.unsubscribe(fileNodeEventsListener)
            model = node
            model.subscribe(fileNodeEventsListener)
        }
    }

    override fun getFiles(): ArrayList<AbstractStorageFile> = model.files
}