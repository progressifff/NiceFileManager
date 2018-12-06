package com.progressifff.filemanager.presenters

import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.progressifff.filemanager.*
import com.progressifff.filemanager.Constants.FILES_DISPLAY_MODE_KEY
import com.progressifff.filemanager.Constants.FILES_ORDER_MODE_KEY
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.models.FilesNode
import com.progressifff.filemanager.models.StorageFile
import com.progressifff.filemanager.views.MainView
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import kotlin.properties.Delegates

class MainPresenter : BasePresenter<NavigationManager, MainView>() {

    override var model = NavigationManager()

    val navigationDrawerListener = NavigationDrawerListener()

    val viewOnClickListener = View.OnClickListener { v ->
        when(v?.id){
            R.id.filesDisplayModeBtn -> {filesDisplayMode = if(filesDisplayMode == FilesDisplayMode.LIST) FilesDisplayMode.GRID else FilesDisplayMode.LIST}
            R.id.filesOrderModeBtn -> {filesOrderMode = if(filesOrderMode == FilesOrderMode.ASCENDING) FilesOrderMode.DESCENDING else FilesOrderMode.ASCENDING}
        }
    }

    private var filesDisplayMode: FilesDisplayMode by Delegates.observable(FilesDisplayMode.LIST){
            _, old, new ->
        if(new != old){
            saveStringToSharedPreferences(FILES_DISPLAY_MODE_KEY, new.name)
        }
        RxBus.publish(RxEvent.FilesDisplayModeChangedEvent(new))
        view?.setFilesDisplayModeButton(new)
    }

    private var filesOrderMode: FilesOrderMode by Delegates.observable(FilesOrderMode.ASCENDING){
        _, old, new ->
        if(new != old){
            saveStringToSharedPreferences(FILES_ORDER_MODE_KEY, new.name)
        }
        RxBus.publish(RxEvent.FilesOrderModeChangedEvent(new))
        view?.setFilesOrderModeButton(new)
    }

    val navigationEntriesCount: Int get() = model.navigationEntriesCount
    private lateinit var saveFilesStateEventListenerDisposable: Disposable
    private lateinit var openFolderEventListenerDisposable: Disposable

    init {
        filesDisplayMode = MainPresenter.FilesDisplayMode.fromString(getStringFromSharedPreferences(FILES_DISPLAY_MODE_KEY, MainPresenter.FilesDisplayMode.LIST.name))
        filesOrderMode = MainPresenter.FilesOrderMode.fromString(getStringFromSharedPreferences(FILES_ORDER_MODE_KEY, MainPresenter.FilesOrderMode.ASCENDING.name))
        model.add(NavigationManager.NavigationEntry("Internal storage", FilesNode(StorageFile(Environment.getExternalStorageDirectory()))))
        RxBus.publish(RxEvent.NavigateEvent(model.current().filesNode))
        model.currentDrawerMenuItemId = R.id.internal_storage
        model.currentToolBarTitle = App.get().getString(R.string.storage)
    }

    override fun bindView(@NonNull v: MainView){
        super.bindView(v)
        view!!.setToolBarTitle(model.currentToolBarTitle)
        view!!.setCheckedDrawerMenuItem(model.currentDrawerMenuItemId)
        view!!.setFilesOrderModeButton(filesOrderMode)
        view!!.setFilesDisplayModeButton(filesDisplayMode)
        view!!.updateNavigationBar()
        saveFilesStateEventListenerDisposable = RxBus.listen(RxEvent.SaveFilesStateEvent::class.java).subscribe(::onSaveFilesStateEvent)
        openFolderEventListenerDisposable = RxBus.listen(RxEvent.OpenFolderEvent::class.java).subscribe(::onOpenFolderEvent)
        if(model.current().filesNode.source.notExists){
            navigateBack()
        }
    }

    override fun unbindView() {
        super.unbindView()
        saveFilesStateEventListenerDisposable.dispose()
        openFolderEventListenerDisposable.dispose()
        RxBus.clearHistory()
    }

    private fun onOpenFolderEvent(event: RxEvent.OpenFolderEvent){
        model.add(NavigationManager.NavigationEntry(event.filesNode.source.name, event.filesNode))
        view!!.insertNavigationBarItemsRange(model.navigationEntriesCount - 1)
    }

    private fun onSaveFilesStateEvent(event: RxEvent.SaveFilesStateEvent){
        val previous = model.previous()
        if(previous != null && previous.filesNode == event.filesNode){
            previous.filesListState = event.filesListState
        }
    }

    fun onOpenSearchedFolder(folder: AbstractStorageFile){
        fun addFolder(folder: AbstractStorageFile) {
            if(folder.parent!!.path != model.current().filesNode.source.path){
                addFolder(folder.parent!!)
            }
            model.add(NavigationManager.NavigationEntry(folder.name, FilesNode(folder)))
            view?.insertNavigationBarItemsRange(model.navigationEntriesCount - 1)
        }
        addFolder(folder)

        RxBus.publish(RxEvent.NavigateEvent(model.current().filesNode))
    }

    fun onBindNavigationBarItem(index: Int, navigationBarEntryView: NavigationEntriesAdapter.NavigationBarEntryView){
        val entry = model.get(index)
        navigationBarEntryView.setEntryName(entry.nodeName)
    }

    fun onNavigationBarItemClicked(index: Int){
        try{
            val navigationEntriesToRemoveCount = model.navigationEntriesCount - index
            val navEntry = model.navigate(index)
            RxBus.publish(RxEvent.NavigateEvent(navEntry.filesNode, navEntry.filesListState))
            view!!.removeNavigationBarItemsRange(index + 1, navigationEntriesToRemoveCount)
        }
        catch (e: Exception){ }
    }

    fun onNavigationDrawerItemSelected(menu: Menu, item: MenuItem): Boolean {
        if (menu.findItem(item.itemId).isChecked) {
            view!!.closeNavigationDrawer()
            return false
        }

        val drawerCloseHandler: (folder: AbstractStorageFile, nodeName: String) -> Unit = { folder, nodeName ->

            if(model.current().filesNode.source != folder){
                val toolBarTitle = if (item.groupId == R.id.storage_group) { App.get().getString(R.string.storage) } else { App.get().getString(R.string.media) }

                view!!.setToolBarTitle(toolBarTitle)

                view!!.setCheckedDrawerMenuItem(item.itemId)

                val filesNode = FilesNode(folder)

                RxBus.publish(RxEvent.NavigateEvent(filesNode))

                if(folder.notExists){
                    folder.createFolder()
                }

                model.reset(NavigationManager.NavigationEntry(nodeName, filesNode),
                        item.itemId,
                        toolBarTitle)

                view!!.updateNavigationBar()
            }
        }

        when (item.itemId) {
            R.id.root_storage -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    drawerCloseHandler(StorageFile("/"), "Root")
                }
            }
            R.id.internal_storage -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    drawerCloseHandler(StorageFile(Environment.getExternalStorageDirectory()), "Internal storage")
                }
            }
            R.id.downloads -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    drawerCloseHandler(StorageFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)), Environment.DIRECTORY_DOWNLOADS)
                }
            }
            R.id.pictures -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    drawerCloseHandler(StorageFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)), Environment.DIRECTORY_PICTURES)
                }
            }
            R.id.movies -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    drawerCloseHandler(StorageFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)), Environment.DIRECTORY_MOVIES)
                }
            }
            R.id.music -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    drawerCloseHandler(StorageFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)), Environment.DIRECTORY_MUSIC)
                }
            }
            R.id.dcim -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    drawerCloseHandler(StorageFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)), Environment.DIRECTORY_DCIM)
                }
            }
            R.id.documents -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    drawerCloseHandler(StorageFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)), Environment.DIRECTORY_DOCUMENTS)
                }
            }
            R.id.settings -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    view!!.openSettings()
                }
                view!!.closeNavigationDrawer()
                return false
            }
            else -> return false
        }

        view!!.closeNavigationDrawer()
        return true
    }

    fun onBackPressed(): Boolean {
        if(navigationDrawerListener.drawerIsOpened){
            view!!.closeNavigationDrawer()
            return true
        }

        return navigateBack()
    }

    private fun navigateBack(): Boolean{
        val navEntry = model.navigateBack()

        return if(navEntry != null){
            view!!.updateNavigationBar()
            view!!.setCheckedDrawerMenuItem(model.currentDrawerMenuItemId)
            view!!.setToolBarTitle(model.currentToolBarTitle)
            RxBus.publish(RxEvent.NavigateEvent(navEntry.filesNode, navEntry.filesListState))
            true
        }
        else{
            false
        }
    }

    fun getCurrentOpenedFolder(): AbstractStorageFile {
        return model.current().filesNode.source
    }

    enum class FilesDisplayMode{
        LIST,
        GRID;

        companion object {
            fun fromString(name: String): FilesDisplayMode{
                return try{
                    valueOf(name)
                } catch (e: Exception){
                    e.printStackTrace()
                    LIST
                }
            }
        }
    }

    enum class FilesOrderMode{
        ASCENDING,
        DESCENDING;

        companion object {
            fun fromString(name: String): FilesOrderMode{
                return try{
                    valueOf(name)
                } catch (e: Exception){
                    e.printStackTrace()
                    ASCENDING
                }
            }
        }
    }
}
