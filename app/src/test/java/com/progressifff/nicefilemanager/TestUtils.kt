package com.progressifff.nicefilemanager

import android.support.annotation.IdRes
import android.view.MenuItem
import com.nhaarman.mockitokotlin2.mock
import com.progressifff.nicefilemanager.models.StorageFile
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.plugins.RxJavaPlugins
import org.mockito.Mockito
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

object TestUtils {
    fun createMockedStorageFile(isDirectory: Boolean = false): AbstractStorageFile{
        val file = mock<StorageFile>()
        Mockito.`when`(file.isDirectory).thenReturn(isDirectory)
        return file
    }

    fun createMockedMenuItem(@IdRes id: Int): MenuItem {
        val menuItem: MenuItem = mock()
        Mockito.`when`(menuItem.itemId).thenReturn(id)
        return menuItem
    }

    fun initRx(){
        val immediate = object : Scheduler() {
            override fun scheduleDirect(run: Runnable, delay: Long, unit: TimeUnit): Disposable {
                return super.scheduleDirect(run, 0, unit)
            }

            override fun createWorker(): Worker {
                return ExecutorScheduler.ExecutorWorker(Executor { it.run() })
            }
        }

        RxJavaPlugins.setInitIoSchedulerHandler { immediate }
        RxJavaPlugins.setInitComputationSchedulerHandler { immediate }
        RxJavaPlugins.setInitNewThreadSchedulerHandler { immediate }
        RxJavaPlugins.setInitSingleSchedulerHandler { immediate }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { immediate }
    }

    private fun <T> any(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    private fun <T> uninitialized(): T = null as T
}