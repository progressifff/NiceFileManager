package com.progressifff.nicefilemanager.presenters

import android.os.Parcelable
import android.support.annotation.NonNull
import android.support.v4.widget.SwipeRefreshLayout
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.progressifff.nicefilemanager.*
import com.progressifff.nicefilemanager.Constants.SHOW_HIDDEN_FILES_KEY
import com.progressifff.nicefilemanager.AbstractFilesNode
import com.progressifff.nicefilemanager.AbstractStorageFile
import com.progressifff.nicefilemanager.models.FilesNode
import com.progressifff.nicefilemanager.views.NestedFilesView
import io.reactivex.disposables.Disposable
import kotlin.collections.ArrayList

class FilesPresenter(private val appPreferences: Preferences,
                     eventBus: RxBus,
                     filesClipboard: FilesClipboard,
                     fileImageLoader: FileImageLoader) : AbstractFilesPresenter<AbstractFilesNode, NestedFilesView>(eventBus, filesClipboard, fileImageLoader),
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
            view?.update(true)
        }

        override fun onError(messageId: Int) {
            view?.showToast(messageId)
            view?.showNoFilesMsg()
        }
    }

    private val clipboardListener = object: FilesClipboard.FilesClipboardListener{
        override fun onClipDataChanged() {
            view?.invalidateMenu()
        }
    }

    val viewClickListener = View.OnClickListener { v->
        when(v.id){
            R.id.addFolderFab -> view!!.showCreateFolderDialog(model.folder)
        }
    }

    private lateinit var navigateEventListenerDisposable: Disposable
    private lateinit var navDrawerStateListenerDisposable: Disposable
    private lateinit var displayModeEventListenerDisposable: Disposable
    private lateinit var orderModeEventListenerDisposable: Disposable
    private lateinit var sortTypeEventListenerDisposable: Disposable

    init { multiSelectMode.eventsListener = this }

    override fun bindView(@NonNull v: NestedFilesView){
        super.bindView(v)
        if(multiSelectMode.running){ view!!.startActionMode(multiSelectMode) }
        filesClipboard.filesClipboardListener = clipboardListener

        navigateEventListenerDisposable = eventBus.listen(
                RxEvent.NavigateEvent::class.java).
                subscribe(::onNavigateEvent)

        navDrawerStateListenerDisposable = eventBus.listen(
                RxEvent.NavigationDrawerStateChangedEvent::class.java).
                subscribe(::onNavigationDrawerStateChangedEvent)

        displayModeEventListenerDisposable = eventBus.listen(
                RxEvent.FilesDisplayModeChangedEvent::class.java).
                subscribe(::onFilesDisplayModeChangedEvent)

        orderModeEventListenerDisposable = eventBus.listen(
                RxEvent.FilesOrderModeChangedEvent::class.java).
                subscribe(::onFilesOrderModeChangedEvent)

        sortTypeEventListenerDisposable = eventBus.listen(
                RxEvent.FilesSortTypeChangedEvent::class.java).
                subscribe(::filesSortTypeChangedEvent)

        view!!.update(false)
    }

    override fun unbindView() {
        if(multiSelectMode.running){ multiSelectMode.saveState() }
        navigateEventListenerDisposable.dispose()
        navDrawerStateListenerDisposable.dispose()
        displayModeEventListenerDisposable.dispose()
        orderModeEventListenerDisposable.dispose()
        sortTypeEventListenerDisposable.dispose()
        super.unbindView()
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
        }
        else{
            val isModelSortRequired = (event.filesNode.sortFilesType != model.sortFilesType ||
                    event.filesNode.isDescendingSort != model.isDescendingSort)

            val isModelUpdateRequired = (event.filesNode is FilesNode && model is FilesNode) &&
                                        (event.filesNode.includeHiddenFiles != (model as FilesNode).includeHiddenFiles)

            changeModel(event.filesNode)

            if(!model.isLoaded || isModelUpdateRequired){
                model.load()
            }
            else if(isModelSortRequired){
                model.sort()
            }
            else{
                view?.update()
            }
        }

        view?.restoreFilesListState(event.filesListState)
        event.processed()
    }

    private fun onNavigationDrawerStateChangedEvent(event: RxEvent.NavigationDrawerStateChangedEvent) {
        if(event.isDragging){
            if(multiSelectMode.running) {
                multiSelectMode.hide()
            }
        }
        else if(multiSelectMode.hidden) {
            view?.startActionMode(multiSelectMode)
        }
    }

    private fun onFilesDisplayModeChangedEvent(event: RxEvent.FilesDisplayModeChangedEvent){
        when (event.displayMode){
            MainPresenter.FilesDisplayMode.GRID -> view?.setFilesInGridLayout()
            MainPresenter.FilesDisplayMode.LIST -> view?.setFilesInListLayout()
        }
    }

    private fun filesSortTypeChangedEvent(event: RxEvent.FilesSortTypeChangedEvent){
        model.sortFilesType = event.sortType
        appPreferences.saveString(Constants.SORT_TYPE_KEY, event.sortType.name)
    }

    private fun onFilesOrderModeChangedEvent(event: RxEvent.FilesOrderModeChangedEvent){
        model.isDescendingSort = event.orderMode == MainPresenter.FilesOrderMode.DESCENDING
    }

    override fun onFilesListEntryClicked(index: Int, filesListState: Parcelable) {
        try{
            val file = model.get(index)

            if(!multiSelectMode.running){
                if(file.isDirectory){
                    val filesNode = AbstractFilesNode.create(file)
                    eventBus.publish(RxEvent.OpenFolderEvent(filesNode))
                    eventBus.publish(RxEvent.SaveFilesStateEvent(model, filesListState))
                    changeModel(filesNode)
                    model.load()
                }
                else {
                    try{
                        view!!.showOpenFileDialog(file)
                    }
                    catch (e: Exception){
                        view!!.showToast(R.string.open_file_error)
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

    override fun getFile(index: Int): AbstractStorageFile = model.get(index)

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.paste -> {
                if(filesClipboard.isNotEmpty){
                    when(filesClipboard.clipData!!.action){
                        FilesClipboard.ClipData.Action.COPY -> {
                            eventBus.publish(RxEvent.NewFilesTaskEvent(CopyTask(filesClipboard.clipData!!.files, model.folder)))
                        }

                        FilesClipboard.ClipData.Action.CUT -> {
                            eventBus.publish(RxEvent.NewFilesTaskEvent(CutTask(filesClipboard.clipData!!.files, model.folder)))
                            filesClipboard.clear()
                        }
                    }
                }
            }

            R.id.sortType -> view!!.showSortTypeDialog(model.sortFilesType)

            R.id.showHiddenFiles -> {
                val includeHiddenFiles = !item.isChecked
                item.isChecked = includeHiddenFiles
                (model as? FilesNode)?.includeHiddenFiles = includeHiddenFiles
                appPreferences.saveBoolean(SHOW_HIDDEN_FILES_KEY, includeHiddenFiles)
            }
            else -> return false
        }
        return true
    }

    fun onPrepareOptionsMenu(menu: Menu){
        val pasteMenuItem = menu.findItem(R.id.paste)
        pasteMenuItem!!.isEnabled = filesClipboard.isNotEmpty
        val showHiddenFilesMenuItem = menu.findItem(R.id.showHiddenFiles)
        showHiddenFilesMenuItem!!.isChecked = appPreferences.getBoolean(SHOW_HIDDEN_FILES_KEY)
    }

    private fun changeModel(node: AbstractFilesNode){
        fileImageLoader.release()
        if(!::model.isInitialized){
            model = node
            model.subscribe(fileNodeEventsListener)
        }
        else{
            if(node is FilesNode &&  model is FilesNode){
                node.includeHiddenFiles = (model as FilesNode).includeHiddenFiles
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