package com.progressifff.nicefilemanager

import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import java.lang.ref.WeakReference

class MultiSelectMode(var eventsListener: EventsListener? = null) : ActionMode.Callback {

    var action = WeakReference<ActionMode>(null)
    val selectedFiles = arrayListOf<AbstractStorageFile>()
    var running = false
        private set
    var hidden: Boolean = false
        private set
    var isStateSaved: Boolean = false
    private set
    private val selectedCount: Int get() {return selectedFiles.size}

    private fun update(){
        if(selectedCount <= 0){
            cancel()
        }
        else{
            action.get()?.title = "$selectedCount"
            action.get()?.invalidate()
        }
    }

    fun cancel(){
        action.get()?.finish()
    }

    fun hide(){
        hidden = true
        cancel()
    }

    fun saveState(){
        isStateSaved = true
    }

    fun take(file: AbstractStorageFile){
        if(selectedFiles.contains(file)){
            selectedFiles.remove(file)
        }
        else{
            selectedFiles.add(file)
        }
        update()
    }

    fun takeAll(files: ArrayList<AbstractStorageFile>){
        selectedFiles.clear()
        selectedFiles.addAll(files)
        update()
    }

    fun isFileSelected(file: AbstractStorageFile): Boolean{
        return selectedFiles.contains(file)
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        eventsListener?.onMenuActionClicked(item!!)
        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return if(mode != null){
            hidden = false
            isStateSaved = false
            action = WeakReference(mode)
            action.get()?.menuInflater?.inflate(R.menu.multi_select_mode_menu, menu)
            action.get()?.title = "$selectedCount"
            running = true
            true
        }
        else false
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean { return false }

    override fun onDestroyActionMode(mode: ActionMode?) {
        action.clear()
        if(!hidden && !isStateSaved) {
            running = false
            selectedFiles.clear()
            eventsListener?.onActionModeDestroyed()
        }
    }

    interface EventsListener {
        fun onActionModeDestroyed()
        fun onMenuActionClicked(menuItem: MenuItem)
    }
}