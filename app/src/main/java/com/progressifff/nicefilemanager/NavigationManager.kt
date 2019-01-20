package com.progressifff.nicefilemanager

import android.os.Parcelable
import android.support.annotation.IdRes
import android.support.annotation.StringRes
import java.util.*

class NavigationManager {
    private var navigationEntries = mutableListOf<NavigationEntry>()
    private var navigationStack = Stack<NavigationStackEntry>()
    private var statesStack = Stack<NavigationManagerState>()
    @IdRes var currentDrawerMenuItemId: Int = 0
    @StringRes var currentToolBarTitle: Int = 0
    val navigationEntriesCount: Int get() {return navigationEntries.size}

    fun reset(startEntry: NavigationEntry,
              @IdRes drawerMenuItemId: Int,
              @StringRes toolBarTitle: Int){

        if(navigationEntries.isNotEmpty() || navigationStack.isNotEmpty()){
            statesStack.push(NavigationManagerState(currentDrawerMenuItemId, currentToolBarTitle, navigationEntries, navigationStack))
        }
        navigationStack = Stack()
        navigationEntries = arrayListOf(startEntry)
        currentDrawerMenuItemId = drawerMenuItemId
        currentToolBarTitle = toolBarTitle
    }

    fun navigate(navigationEntryIndex: Int): NavigationEntry {
        if(navigationEntryIndex < 0 || navigationEntryIndex >= navigationEntries.size - 1){
            throw IllegalArgumentException("Wrong index")
        }

        val subEntries = navigationEntries.subList(navigationEntryIndex + 1, navigationEntries.size)
        navigationStack.push(NavigationStackEntry(navigationEntryIndex, ArrayList(subEntries)))
        subEntries.clear()

        return navigationEntries.last()
    }

    fun navigateBack(): NavigationEntry? {
        val result = if (navigationStack.isNotEmpty() && ((navigationEntries.size - 1) == navigationStack.last().index)){
            val stackEntry = navigationStack.pop()
            navigationEntries.addAll(stackEntry.entries)
            navigationEntries.last()

        } else {
            if (navigationEntries.size <= 1) {
                return if(statesStack.isNotEmpty()){
                    val lastState = statesStack.pop()
                    navigationEntries.last().filesNode.release()
                    navigationEntries = lastState.navigationEntries
                    navigationStack = lastState.navigationEntriesStack
                    currentDrawerMenuItemId = lastState.drawerMenuItemId
                    currentToolBarTitle = lastState.toolBarTitle
                    navigationEntries.last()
                } else {
                    null
                }
            }

            val removed = navigationEntries.removeAt(navigationEntries.size - 1)
            removed.filesNode.release()
            navigationEntries.last()
        }
        if(result.filesNode.folder.notExists){
            return navigateBack()
        }
        return result
    }

    fun add(navigationEntry: NavigationEntry){
        navigationEntries.add(navigationEntry)
    }

    fun previous(): NavigationEntry? {
        return when {
            navigationStack.isNotEmpty() -> navigationStack.first().entries.last()
            navigationEntries.size > 1 -> navigationEntries[navigationEntries.size - 2]
            statesStack.isNotEmpty() -> statesStack.first().navigationEntries.last()
            else -> null
        }
    }

    fun get(index: Int): NavigationEntry = navigationEntries.elementAt(index)

    fun current(): NavigationEntry = navigationEntries.last()

    data class NavigationManagerState(@IdRes     val drawerMenuItemId: Int,
                                      @StringRes val toolBarTitle: Int,
                                                 val navigationEntries: MutableList<NavigationEntry>,
                                                 val navigationEntriesStack: Stack<NavigationStackEntry>)

    data class NavigationEntry(val nodeName: String,
                               var filesNode: AbstractFilesNode,
                               var filesListState: Parcelable? = null)

    data class NavigationStackEntry(val index: Int,
                                    val entries: MutableList<NavigationEntry>)
}