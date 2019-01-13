package com.progressifff.filemanager

import android.view.Menu
import com.nhaarman.mockitokotlin2.*
import com.progressifff.filemanager.TestUtils.createMockedMenuItem
import com.progressifff.filemanager.models.StorageFile
import com.progressifff.filemanager.presenters.FilesPresenter
import com.progressifff.filemanager.presenters.MainPresenter
import com.progressifff.filemanager.views.NestedFilesView
import io.reactivex.Single
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@Suppress("UNCHECKED_CAST")
@RunWith(MockitoJUnitRunner::class)
class FilesPresenterTest {
    private lateinit var presenter: FilesPresenter
    private lateinit var view: NestedFilesView
    private lateinit var appPreferences: Preferences
    private lateinit var eventBus: RxBus
    private lateinit var filesClipboard: FilesClipboard
    private lateinit var multiSelectMode: MultiSelectMode

    @Before
    fun setup(){
        appPreferences = mock()
        `when`(appPreferences.getBoolean(Constants.SHOW_HIDDEN_FILES_KEY)).thenReturn(false)
        eventBus = spy(RxBus)
        filesClipboard = spy(FilesClipboard)
        multiSelectMode = spy()
        `when`(multiSelectMode.running).thenReturn(true)
        presenter = FilesPresenter(appPreferences, eventBus, filesClipboard, mock())
        presenter.multiSelectMode = multiSelectMode
        view = mock()
        presenter.bindView(view)
        verify(view).startActionMode(multiSelectMode)
        verify(view).update(false)
        reset(view)
        reset(eventBus)
    }

    @After
    fun release(){
        `when`(multiSelectMode.running).thenReturn(true)
        presenter.unbindView()
        verify(multiSelectMode).saveState()
    }

    @Test
    fun checkNavigateEvent(){
        val filesNode = createSpyFilesNode()
        RxBus.publish(RxEvent.NavigateEvent(filesNode))
        verify(multiSelectMode).cancel()
        verify(view).restoreFilesListState(null)
        verify(filesNode).load()
    }

    @Test
    fun checkDoubleNavigateEvent(){
        val filesNode1 = createSpyFilesNode()
        RxBus.publish(RxEvent.NavigateEvent(filesNode1))
        val filesNode2 = createSpyFilesNode()
        RxBus.publish(RxEvent.NavigateEvent(filesNode2))
        verify(filesNode1).load()
        verify(filesNode2).load()
        verify(view, times(2)).restoreFilesListState(null)
    }

    @Test
    fun checkRefreshFiles(){
        val filesNode = createSpyFilesNode()
        RxBus.publish(RxEvent.NavigateEvent(filesNode))
        verify(filesNode).load()
    }

    @Test
    fun checkFilesDisplayGridModeEvent(){
        RxBus.publish(RxEvent.FilesDisplayModeChangedEvent(MainPresenter.FilesDisplayMode.GRID))
        verify(view).setFilesInGridLayout()
    }

    @Test
    fun checkFilesDisplayListModeEvent(){
        RxBus.publish(RxEvent.FilesDisplayModeChangedEvent(MainPresenter.FilesDisplayMode.LIST))
        verify(view).setFilesInListLayout()
    }

    @Test
    fun checkFilesOrderModeEvent(){
        val filesNode = createSpyFilesNode()
        RxBus.publish(RxEvent.NavigateEvent(filesNode))
        RxBus.publish(RxEvent.FilesOrderModeChangedEvent(MainPresenter.FilesOrderMode.ASCENDING))
        verify(filesNode).sort()
    }

    @Test
    fun checkFilesSortTypeChangedEvent(){
        val filesNode = createSpyFilesNode()
        RxBus.publish(RxEvent.NavigateEvent(filesNode))
        verify(filesNode).load()
        RxBus.publish(RxEvent.FilesSortTypeChangedEvent(AbstractFilesNode.SortFilesType.MODIFICATION_TIME))
        verify(filesNode).sort()
        verify(appPreferences).saveString(Constants.SORT_TYPE_KEY, AbstractFilesNode.SortFilesType.MODIFICATION_TIME.name)
    }

    @Test
    fun checkFilesListFolderClicked(){
        `when`(multiSelectMode.running).thenReturn(false)
        val folder = TestUtils.createMockedStorageFile(true)
        `when`(folder.openAsDir()).thenReturn(Single.create<ArrayList<AbstractStorageFile>> {})
        val filesNode = createSpyFilesNode(arrayListOf(folder))
        RxBus.publish(RxEvent.NavigateEvent(filesNode))
        presenter.onFilesListEntryClicked(0, mock())
        verify(eventBus, times(2)).publish(any())
    }

    @Test
    fun checkFilesListFileClicked(){
        `when`(multiSelectMode.running).thenReturn(false)
        val file = TestUtils.createMockedStorageFile()
        val filesNode = createMockedFilesNode(arrayListOf(file))
        RxBus.publish(RxEvent.NavigateEvent(filesNode))
        presenter.onFilesListEntryClicked(0, mock())
        verify(view).showOpenFileDialog(file)
    }

    @Test
    fun checkFilesListFileInMultiSelectModeClicked(){
        val file = TestUtils.createMockedStorageFile()
        val filesNode = createMockedFilesNode(arrayListOf(file))
        RxBus.publish(RxEvent.NavigateEvent(filesNode))
        val fileIndex = 0
        presenter.onFilesListEntryClicked(fileIndex, mock())
        verify(multiSelectMode).take(file)
        verify(view).updateFilesListEntry(fileIndex)
    }

    @Test
    fun checkFileListEntryMenuClicked() {
        val file = TestUtils.createMockedStorageFile()
        val filesNode = createMockedFilesNode(arrayListOf(file))
        RxBus.publish(RxEvent.NavigateEvent(filesNode))
        presenter.onFileListEntryMenuClicked(0)
        verify(view).showFileActionsDialog(file)
    }

    @Test
    fun checkActionModeDestroyedEvent(){
        presenter.onActionModeDestroyed()
        verify(view).setupToolBarScrollingBehavior()
    }

    @Test
    fun checkGetFilesCount(){
        val files = arrayListOf<AbstractStorageFile>(mock(), mock(), mock(), mock(), mock())
        val filesNode = createMockedFilesNode(files)
        RxBus.publish(RxEvent.NavigateEvent(filesNode))
        Assert.assertTrue(presenter.getFilesCount() == files.size)
    }

    @Test
    fun checkPasteCopiedFiles(){
        setupNotEmptyClipboard(FilesClipboard.ClipData.Action.COPY)
        RxBus.publish(RxEvent.NavigateEvent(createMockedFilesNode()))
        presenter.onOptionsItemSelected(createMockedMenuItem(R.id.paste))
        verify(eventBus).publish(any<RxEvent.NewFilesTaskEvent>())
    }

    @Test
    fun checkPasteCutFiles(){
        setupNotEmptyClipboard(FilesClipboard.ClipData.Action.CUT)
        RxBus.publish(RxEvent.NavigateEvent(createMockedFilesNode()))
        presenter.onOptionsItemSelected(createMockedMenuItem(R.id.paste))
        verify(eventBus).publish(any<RxEvent.NewFilesTaskEvent>())
        verify(filesClipboard).clear()
    }

    @Test
    fun checkPressSortTypeMenuItem(){
        RxBus.publish(RxEvent.NavigateEvent(createSpyFilesNode()))
        RxBus.publish(RxEvent.FilesSortTypeChangedEvent(AbstractFilesNode.SortFilesType.SIZE))
        val menuItem = createMockedMenuItem(R.id.sortType)
        presenter.onOptionsItemSelected(menuItem)
        verify(view).showSortTypeDialog(AbstractFilesNode.SortFilesType.SIZE)
    }

    @Test
    fun checkPressShowHiddenFilesMenuItem(){
        RxBus.publish(RxEvent.NavigateEvent(createSpyFilesNode()))
        val menuItem = createMockedMenuItem(R.id.showHiddenFiles)
        presenter.onOptionsItemSelected(menuItem)
        verify(appPreferences).saveBoolean(Constants.SHOW_HIDDEN_FILES_KEY, true)
    }

    @Test
    fun checkPrepareOptionsMenuEvent(){
        setupNotEmptyClipboard()
        val menu: Menu = mock()
        val pasteMenuItem = createMockedMenuItem(R.id.paste)
        `when`(menu.findItem(R.id.paste)).thenReturn(pasteMenuItem)
        val showHiddenFilesMenuItem = createMockedMenuItem(R.id.showHiddenFiles)
        `when`(menu.findItem(R.id.showHiddenFiles)).thenReturn(showHiddenFilesMenuItem)
        presenter.onPrepareOptionsMenu(menu)
        verify(pasteMenuItem).isEnabled = true
        verify(showHiddenFilesMenuItem).isChecked = false
    }

    @Test
    fun checkNavigationDrawerStateDraggingEvent(){
        RxBus.publish(RxEvent.NavigationDrawerStateChangedEvent(true))
        verify(multiSelectMode).hide()
    }

    @Test
    fun checkNavigationDrawerStateIdleEvent(){
        `when`(multiSelectMode.hidden).thenReturn(true)
        RxBus.publish(RxEvent.NavigationDrawerStateChangedEvent(false))
        verify(view).startActionMode(multiSelectMode)
    }

    private fun createSpyFilesNode(files: ArrayList<AbstractStorageFile> = arrayListOf(mock()), source: StorageFile = mock()): AbstractFilesNode{
        val filesNode: AbstractFilesNode = spy(AbstractFilesNode.create(source))
        Mockito.doNothing().`when`(filesNode).load()
        Mockito.doNothing().`when`(filesNode).sort()
        Mockito.doNothing().`when`(filesNode).subscribe(any())
        Mockito.doReturn(files.elementAt(0)).`when`(filesNode).get(0)
        return filesNode
    }

    private fun createMockedFilesNode(files: ArrayList<AbstractStorageFile> = arrayListOf(mock()), source: AbstractStorageFile = mock()): AbstractFilesNode{
        val filesNode: AbstractFilesNode = mock()
        `when`(filesNode.folder).thenReturn(source)
        Mockito.doNothing().`when`(filesNode).load()
        `when`(filesNode.files).thenReturn(files)
        `when`(filesNode.get(0)).thenReturn(files.elementAt(0))
        return filesNode
    }

    private fun setupNotEmptyClipboard(action: FilesClipboard.ClipData.Action = FilesClipboard.ClipData.Action.COPY){
        Mockito.doReturn(true).`when`(filesClipboard).isNotEmpty
        Mockito.doReturn(FilesClipboard.ClipData(action, arrayListOf())).`when`(filesClipboard).clipData
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            TestUtils.initRx()
        }
    }
}