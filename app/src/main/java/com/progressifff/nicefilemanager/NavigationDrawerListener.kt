package com.progressifff.nicefilemanager

import android.support.design.widget.SwipeDismissBehavior
import android.support.v4.widget.DrawerLayout
import android.view.View

class NavigationDrawerListener : DrawerLayout.DrawerListener {

    var drawerIsOpened = false
                    private set

    private var onCloseHandler : (() -> Unit)? = null

    override fun onDrawerStateChanged(newState: Int) {
        if(newState == SwipeDismissBehavior.STATE_DRAGGING && !drawerIsOpened){
            RxBus.publish(RxEvent.NavigationDrawerStateChangedEvent(true))
        }
        else if(newState == SwipeDismissBehavior.STATE_IDLE && !drawerIsOpened){
            RxBus.publish(RxEvent.NavigationDrawerStateChangedEvent(false))
        }
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) { }

    override fun onDrawerClosed(drawerView: View) {
        drawerIsOpened = false
        onCloseHandler?.invoke()
        onCloseHandler = null
    }

    override fun onDrawerOpened(drawerView: View) {
        drawerIsOpened = true
    }

    fun setDrawerCloseHandler(onCloseHandler : () -> Unit){
        this.onCloseHandler = onCloseHandler
    }
}