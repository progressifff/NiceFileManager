package com.progressifff.nicefilemanager

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.support.v4.content.ContextCompat
import android.util.LruCache
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import android.provider.MediaStore
import android.widget.ImageView
import io.reactivex.disposables.Disposable
import java.lang.Exception
import java.util.concurrent.Executors

interface FileImageLoader {
    fun applyFileImage(file: AbstractStorageFile, fileListEntryView: WeakReference<ImageView>)
    fun discard()
}

object FileImageManager : FileImageLoader, ComponentCallbacks2 {
    private var cache: LruCache<AbstractStorageFile, Drawable>
    private val executor = Executors.newFixedThreadPool(4)!!
    private val pooledScheduler = Schedulers.from(executor)
    private val loadImageDisposables = arrayListOf<Disposable>()
    init {
        val activityManager = App.get().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appMemoryLimit = activityManager.memoryClass * 1024 //kb
        val cacheMaxSize = appMemoryLimit / 8 // 1 / 8th of total app memory limit
        cache = LruCache(cacheMaxSize)
    }

    private fun loadFileDynamicImage(file: AbstractStorageFile): Single<Drawable>{
        return Single.create<Drawable> {
            try {
                val fileMimeType = file.mimeType

                when {
                    fileMimeType == "application/vnd.android.package-archive" -> {
                        val packageManager = App.get().packageManager
                        val packageArchiveInfo = packageManager?.getPackageArchiveInfo(file.path, 0)
                        val applicationInfo = packageArchiveInfo?.applicationInfo
                        applicationInfo?.sourceDir = file.path
                        applicationInfo?.publicSourceDir = file.path
                        it.onSuccess(applicationInfo?.loadIcon(packageManager)!!)
                    }

                    fileMimeType.startsWith("image") -> {
                        val options = BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeFile(file.path, options)
                        options.inSampleSize = calculateBitmapSampleSize(options, 30.toPx(), 30.toPx())
                        options.inJustDecodeBounds = false
                        it.onSuccess(BitmapDrawable(App.get().resources, BitmapFactory.decodeFile(file.path, options)))
                    }

                    fileMimeType.startsWith("video") -> {
                        it.onSuccess(BitmapDrawable(App.get().resources, ThumbnailUtils.createVideoThumbnail(file.path, MediaStore.Video.Thumbnails.MINI_KIND)))
                    }
                }
            }
            catch (e: Exception){
                it.onError(e)
            }
        }
        .subscribeOn(pooledScheduler)
        .observeOn(AndroidSchedulers.mainThread())
    }

    private fun calculateBitmapSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    override fun applyFileImage(file: AbstractStorageFile, fileListEntryView: WeakReference<ImageView>){
        if(file.isDirectory){
            fileListEntryView.get()?.setImageDrawable(ContextCompat.getDrawable(App.get(), R.drawable.ic_folder))
        }
        else {
            val fileMimeType = file.mimeType

            when {
                fileMimeType.startsWith("audio") -> fileListEntryView.get()?.setImageDrawable(ContextCompat.getDrawable(App.get(), R.drawable.ic_music_note))

                fileMimeType == "text/plain" -> fileListEntryView.get()?.setImageDrawable(ContextCompat.getDrawable(App.get(), R.drawable.ic_txt))

                fileMimeType == "application/pdf" -> fileListEntryView.get()?.setImageDrawable(ContextCompat.getDrawable(App.get(), R.drawable.ic_pdf))

                (fileMimeType == "application/zip" || fileMimeType == "application/rar") -> fileListEntryView.get()?.setImageDrawable(ContextCompat.getDrawable(App.get(), R.drawable.ic_zip_blue))

                (fileMimeType == "application/msword" ||
                        fileMimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document") -> fileListEntryView.get()?.setImageDrawable(ContextCompat.getDrawable(App.get(), R.drawable.ic_doc))

                (fileMimeType == "application/vnd.ms-excel" ||
                        fileMimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") -> fileListEntryView.get()?.setImageDrawable(ContextCompat.getDrawable(App.get(), R.drawable.ic_xls))

                (fileMimeType == "application/vnd.ms-powerpoint" ||
                        fileMimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation") -> fileListEntryView.get()?.setImageDrawable(ContextCompat.getDrawable(App.get(), R.drawable.ic_ppt))
                else -> {
                    val image = cache.get(file)
                    if (image != null) {
                        fileListEntryView.get()?.setImageDrawable(image)
                    } else {
                        fileListEntryView.get()?.setImageDrawable(ContextCompat.getDrawable(App.get(), R.drawable.ic_file))
                        fileListEntryView.get()?.tag = file
                        val disposable = loadFileDynamicImage(file).subscribeWith(object : DisposableSingleObserver<Drawable>() {
                            override fun onSuccess(image: Drawable) {
                                loadImageDisposables.remove(this)
                                cache.put(file, image)
                                if (fileListEntryView.get()?.tag == file) {
                                    fileListEntryView.get()?.setImageDrawable(image)
                                }
                            }
                            override fun onError(e: Throwable) {
                                loadImageDisposables.remove(this)
                                e.printStackTrace()
                            }
                        })
                        loadImageDisposables.add(disposable)
                    }
                }
            }
        }
    }

    override fun discard(){
        for(disposable in loadImageDisposables){
            disposable.dispose()
        }
        loadImageDisposables.clear()
    }

    override fun onLowMemory() {}
    override fun onConfigurationChanged(newConfig: Configuration?) {}
    override fun onTrimMemory(level: Int) {
        if (level >= TRIM_MEMORY_MODERATE) {
            cache.evictAll()
        }
        else if (level >= TRIM_MEMORY_BACKGROUND) {
            cache.trimToSize(cache.size() / 2)
        }
    }
}