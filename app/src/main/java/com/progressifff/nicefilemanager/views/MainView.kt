package com.progressifff.nicefilemanager.views

import android.support.annotation.StringRes
import com.progressifff.nicefilemanager.presenters.MainPresenter

interface MainView {
    fun closeNavigationDrawer()
    fun expandAppBar()
    fun setFilesDisplayModeButton(filesDisplayMode: MainPresenter.FilesDisplayMode)
    fun setFilesOrderModeButton(filesOrderMode: MainPresenter.FilesOrderMode)
    fun setCheckedDrawerMenuItem(menuItemId: Int)
    fun openSettings()
    fun setToolBarTitle(@StringRes title: Int)
    fun insertNavigationBarItemsRange(index: Int, count: Int = 1)
    fun removeNavigationBarItemsRange(index: Int, count: Int = 1)
    fun updateNavigationBar()
}