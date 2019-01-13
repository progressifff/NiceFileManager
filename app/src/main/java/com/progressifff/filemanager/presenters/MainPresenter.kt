package com.progressifff.filemanager.presenters

import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.progressifff.filemanager.*
import com.progressifff.filemanager.Constants.FILES_DISPLAY_MODE_KEY
import com.progressifff.filemanager.Constants.FILES_ORDER_MODE_KEY
import com.progressifff.filemanager.Constants.SHOW_HIDDEN_FILES_KEY
import com.progressifff.filemanager.Constants.SORT_TYPE_KEY
import com.progressifff.filemanager.AbstractFilesNode
import com.progressifff.filemanager.AbstractStorageFile
import com.progressifff.filemanager.models.FilesNode
import com.progressifff.filemanager.models.StorageFile
import com.progressifff.filemanager.views.MainView
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import kotlin.properties.Delegates

class MainPresenter(private var appPreferences: Preferences, private var standardPaths: StandardPaths) : BasePresenter<NavigationManager, MainView>() {
    override var model = NavigationManager()
    val navigationDrawerListener = NavigationDrawerListener()
    val viewOnClickListener = View.OnClickListener { v ->
        when(v?.id){
            R.id.filesDisplayModeBtn -> {filesDisplayMode = if(filesDisplayMode == FilesDisplayMode.LIST) FilesDisplayMode.GRID else FilesDisplayMode.LIST}
            R.id.filesOrderModeBtn -> {filesOrderMode = if(filesOrderMode == FilesOrderMode.ASCENDING) FilesOrderMode.DESCENDING else FilesOrderMode.ASCENDING}
        }
    }

    var filesDisplayMode: FilesDisplayMode by Delegates.observable(FilesDisplayMode.LIST){
            _, old, new ->
        if(new != old){
            appPreferences.saveString(FILES_DISPLAY_MODE_KEY, new.name)
        }
        RxBus.publish(RxEvent.FilesDisplayModeChangedEvent(new))
        view?.setFilesDisplayModeButton(new)
    }

    var filesOrderMode: FilesOrderMode by Delegates.observable(FilesOrderMode.ASCENDING){
        _, old, new ->
        if(new != old){
            appPreferences.saveString(FILES_ORDER_MODE_KEY, new.name)
        }
        RxBus.publish(RxEvent.FilesOrderModeChangedEvent(new))
        view?.setFilesOrderModeButton(new)
    }

    val navigationEntriesCount: Int get() = model.navigationEntriesCount
    private lateinit var saveFilesStateEventListenerDisposable: Disposable
    private lateinit var openFolderEventListenerDisposable: Disposable

    init {
        val initialFilesNode = FilesNode(StorageFile(standardPaths.getInternalStoragePath()))
        initialFilesNode.includeHiddenFiles = appPreferences.getBoolean(SHOW_HIDDEN_FILES_KEY)
        initialFilesNode.sortFilesType = AbstractFilesNode.SortFilesType.fromString(
                appPreferences.getString(SORT_TYPE_KEY, AbstractFilesNode.SortFilesType.NAME.name))
        model.add(NavigationManager.NavigationEntry("Internal storage", initialFilesNode))
        filesDisplayMode = FilesDisplayMode.fromString(appPreferences.getString(Constants.FILES_DISPLAY_MODE_KEY, FilesDisplayMode.LIST.name))
        filesOrderMode = FilesOrderMode.fromString(appPreferences.getString(Constants.FILES_ORDER_MODE_KEY, MainPresenter.FilesOrderMode.ASCENDING.name))
        RxBus.publish(RxEvent.NavigateEvent(model.current().filesNode))
        model.currentDrawerMenuItemId = R.id.internal_storage
        model.currentToolBarTitle = R.string.storage
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
        if(model.current().filesNode.folder.notExists){
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
        model.add(NavigationManager.NavigationEntry(event.filesNode.folder.name, event.filesNode))
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
            if(folder.parent!!.path != model.current().filesNode.folder.path){
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
        navigationBarEntryView.setName(entry.nodeName)
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

        val navigate: (nodeFolder: AbstractStorageFile, nodeName: String) -> Unit = { nodeFolder, nodeName ->

            if(model.current().filesNode.folder != nodeFolder){
                val toolBarTitle = if (item.groupId == R.id.storage_group) { R.string.storage } else { R.string.media }
                view!!.setToolBarTitle(toolBarTitle)
                view!!.setCheckedDrawerMenuItem(item.itemId)
                val filesNode = FilesNode(nodeFolder)
                RxBus.publish(RxEvent.NavigateEvent(filesNode))
                if(nodeFolder.notExists){
                    nodeFolder.createFolder()
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
                    navigate(StorageFile("/"), "Root")
                }
            }
            R.id.internal_storage -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    navigate(StorageFile(standardPaths.getInternalStoragePath()), "Internal storage")
                }
            }
            R.id.downloads -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    navigate(StorageFile(standardPaths.getDownloadsFolderPath()), "Download")
                }
            }
            R.id.pictures -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    navigate(StorageFile(standardPaths.getPicturesFolderPath()), "Pictures")
                }
            }
            R.id.movies -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    navigate(StorageFile(standardPaths.getMoviesFolderPath()), "Movies")
                }
            }
            R.id.music -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    navigate(StorageFile(standardPaths.getMusicFolderPath()), "Music")
                }
            }
            R.id.dcim -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    navigate(StorageFile(standardPaths.getPhotosFolderPath()), "DCIM")
                }
            }
            R.id.documents -> {
                navigationDrawerListener.setDrawerCloseHandler {
                    navigate(StorageFile(standardPaths.getDocumentsFolderPath()), "Documents")
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

    fun getCurrentOpenedFolder(): AbstractStorageFile = model.current().filesNode.folder

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
