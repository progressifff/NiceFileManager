package com.progressifff.filemanager.views

import com.progressifff.filemanager.presenters.MainPresenter

interface MainView {
    fun closeNavigationDrawer()
    fun expandAppBar()
    fun setFilesDisplayModeButton(filesDisplayMode: MainPresenter.FilesDisplayMode)
    fun setFilesOrderModeButton(filesOrderMode: MainPresenter.FilesOrderMode)
    fun setCheckedDrawerMenuItem(menuItemId: Int)
    fun openSettings()
    fun setToolBarTitle(title: String)
    fun insertNavigationBarItemsRange(index: Int, count: Int = 1)
    fun removeNavigationBarItemsRange(index: Int, count: Int = 1)
    fun updateNavigationBar()
}