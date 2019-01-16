package com.progressifff.nicefilemanager

import android.support.annotation.IdRes
import android.view.Menu
import android.view.MenuItem
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.progressifff.nicefilemanager.models.FilesNode
import com.progressifff.nicefilemanager.models.StorageFile
import com.progressifff.nicefilemanager.presenters.MainPresenter
import com.progressifff.nicefilemanager.views.MainView
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.Mockito.`when`
import org.junit.BeforeClass
import java.io.File

@RunWith(MockitoJUnitRunner.Silent::class)
class MainPresenterTest {
    private lateinit var presenter: MainPresenter
    private lateinit var view: MainView
    private lateinit var appPreferences: Preferences
    private lateinit var mStandardPaths: StandardPaths
    private val externalStoragePath = "${File.listRoots()[0]}"

    @Before
    fun setup(){
        appPreferences = mock()
        `when`(appPreferences.getString(Constants.FILES_DISPLAY_MODE_KEY, MainPresenter.FilesDisplayMode.LIST.name)).thenReturn("LIST")
        `when`(appPreferences.getString(Constants.FILES_ORDER_MODE_KEY, MainPresenter.FilesOrderMode.ASCENDING.name)).thenReturn("ASCENDING")
        `when`(appPreferences.getString(Constants.SORT_TYPE_KEY, AbstractFilesNode.SortFilesType.NAME.name)).thenReturn("NAME")
        `when`(appPreferences.getBoolean(Constants.SHOW_HIDDEN_FILES_KEY)).thenReturn(true)

        mStandardPaths = mock()
        `when`(mStandardPaths.getInternalStoragePath()).thenReturn(externalStoragePath)
        `when`(mStandardPaths.getDownloadsFolderPath()).thenReturn("${externalStoragePath}Downloads")
        `when`(mStandardPaths.getPicturesFolderPath()).thenReturn("${externalStoragePath}Pictures")
        `when`(mStandardPaths.getMoviesFolderPath()).thenReturn("${externalStoragePath}Movies")
        `when`(mStandardPaths.getMusicFolderPath()).thenReturn("${externalStoragePath}Music")
        `when`(mStandardPaths.getPhotosFolderPath()).thenReturn("${externalStoragePath}Photos")
        `when`(mStandardPaths.getDocumentsFolderPath()).thenReturn("${externalStoragePath}Documents")

        presenter = MainPresenter(appPreferences, RxBus, mStandardPaths)
        view = mock()
        presenter.bindView(view)

        verify(view).setToolBarTitle(R.string.storage)
        verify(view).setCheckedDrawerMenuItem(R.id.internal_storage)
        verify(view).setFilesOrderModeButton(MainPresenter.FilesOrderMode.ASCENDING)
        verify(view).setFilesDisplayModeButton(MainPresenter.FilesDisplayMode.LIST)
        verify(view).updateNavigationBar()
        reset(view)
    }

    @After
    fun release(){
        presenter.unbindView()
    }

    @Test
    fun checkOpenFolderEvent(){
        val filesNode = FilesNode(StorageFile(externalStoragePath, "simpleFolder"))
        RxBus.publish(RxEvent.OpenFolderEvent(filesNode))
        verify(view).insertNavigationBarItemsRange(1)
    }

    @Test
    fun checkOpenSearchedFolder(){
        val folder1 = StorageFile(externalStoragePath, "Searched1")
        val folder2 = StorageFile(folder1, "Searched2")
        val folder3 = StorageFile(folder2, "Searched3")

        presenter.onOpenSearchedFolder(folder1)
        verify(view).insertNavigationBarItemsRange(1)
        presenter.onOpenSearchedFolder(folder2)
        verify(view).insertNavigationBarItemsRange(2)
        presenter.onOpenSearchedFolder(folder3)
        verify(view).insertNavigationBarItemsRange(3)
        Assert.assertEquals(presenter.navigationEntriesCount, 4)
    }

    @Test
    fun checkClickNavigationBarItem(){
        val sep = File.separator
        presenter.onOpenSearchedFolder(StorageFile("$externalStoragePath${sep}one${sep}two${sep}three${sep}four"))
        presenter.onNavigationBarItemClicked(0)
        verify(view).removeNavigationBarItemsRange(1, 5)
    }

    @Test
    fun checkBindNavigationBarItem(){
        val sep = File.separator
        presenter.onOpenSearchedFolder(StorageFile("$externalStoragePath${sep}one${sep}two${sep}three${sep}four"))
        val navigationBarItem: NavigationEntriesAdapter.NavigationBarEntryView = mock()
        presenter.onBindNavigationBarItem(2, navigationBarItem)
        verify(navigationBarItem).setName("two")
    }

    @Test
    fun checkSelectNavigationDrawerSettingsItem(){
        val settingsMenuItem = createMenuItem(R.id.settings)
        val menu = createMenu(R.id.settings, settingsMenuItem)

        presenter.onNavigationDrawerItemSelected(menu, settingsMenuItem)
        presenter.navigationDrawerListener.onDrawerClosed(mock())

        verify(view).openSettings()
        verify(view).closeNavigationDrawer()
        Assert.assertTrue(presenter.navigationEntriesCount == 1)
    }

    @Test
    fun checkSelectNavigationDrawerRootStorageItem(){
        val rootStorageMenuItem = createMenuItem(R.id.root_storage, R.id.storage_group)
        val menu = createMenu(R.id.root_storage, rootStorageMenuItem)

        Assert.assertTrue(presenter.onNavigationDrawerItemSelected(menu, rootStorageMenuItem))
        presenter.navigationDrawerListener.onDrawerClosed(mock())

        verify(view).setToolBarTitle(R.string.storage)
        verify(view).updateNavigationBar()
        verify(view).closeNavigationDrawer()
        Assert.assertTrue(presenter.navigationEntriesCount == 1)
    }

    @Test
    fun checkSelectNavigationDrawerPicturesItem(){
        val picturesMenuItem = createMenuItem(R.id.pictures, R.id.media_group, true)
        val menu = createMenu(R.id.pictures, picturesMenuItem)
        presenter.onNavigationDrawerItemSelected(menu, picturesMenuItem)
        presenter.navigationDrawerListener.onDrawerClosed(mock())
        Assert.assertTrue(presenter.navigationEntriesCount == 1)
    }

    @Test
    fun checkBackPress(){
        Assert.assertFalse(presenter.onBackPressed())
    }

    @Test
    fun checkBackPressedWhenNavigationStackIsNotEmpty(){
        val filesNode1 = FilesNode(StorageFile(externalStoragePath, "simpleFolder1"))
        RxBus.publish(RxEvent.OpenFolderEvent(filesNode1))
        Assert.assertTrue(presenter.onBackPressed())
        verify(view).updateNavigationBar()
        verify(view).setCheckedDrawerMenuItem(R.id.internal_storage)
        verify(view).setToolBarTitle(R.string.storage)
    }

    @Test
    fun checkBackPressWhenDrawerOpened(){
        presenter.navigationDrawerListener.onDrawerOpened(mock())
        Assert.assertTrue(presenter.onBackPressed())
        verify(view).closeNavigationDrawer()
    }

    @Test
    fun checkGetCurrentOpenedFolder(){
        val folder = presenter.getCurrentOpenedFolder()
        Assert.assertTrue(folder.path == mStandardPaths.getInternalStoragePath())
    }

    private fun createMenuItem(@IdRes itemId: Int, @IdRes groupId: Int = -1, checked: Boolean = false): MenuItem{
        val menuItem: MenuItem = mock()
        `when`(menuItem.itemId).thenReturn(itemId)
        `when`(menuItem.groupId).thenReturn(groupId)
        `when`(menuItem.isChecked).thenReturn(checked)
        return menuItem
    }

    private fun createMenu(@IdRes menuItemId: Int, menuItem: MenuItem): Menu{
        val menu: Menu = mock()
        `when`(menu.findItem(menuItemId)).thenReturn(menuItem)
        return menu
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        }
    }
}