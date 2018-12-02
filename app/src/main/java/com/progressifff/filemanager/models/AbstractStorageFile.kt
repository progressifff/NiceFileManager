package com.progressifff.filemanager.models

import android.os.Parcelable
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*

abstract class AbstractStorageFile: Parcelable {
    abstract val name: String
    abstract val path: String
    abstract val exists: Boolean
    abstract val notExists: Boolean
    abstract val isHidden: Boolean
    abstract val isDirectory: Boolean
    abstract val lastModified: Long
    abstract val canRead: Boolean
    abstract val canWriter: Boolean
    abstract val size: Long
    abstract val extension: String
    abstract val parent: AbstractStorageFile?
    abstract val mimeType: String
    abstract val lastModifiedDateTime: String

    abstract fun rename(name: String)
    abstract fun copyRecursively(destPath: String, existingFileAction: ExistingFileAction = ExistingFileAction.REWRITE)
    abstract fun copyRecursivelyAsync(dest: String,
                                      onProgressChanged: (bytesCopied: Long) -> Unit,
                                      onProcessNewFile: (file: AbstractStorageFile) -> Unit,
                                      onError: (file: AbstractStorageFile, e: Exception) -> OnErrorAction = { _, exception -> throw exception },
                                      existingFileAction: ExistingFileAction = ExistingFileAction.REWRITE): Completable

    abstract fun deleteRecursivelyAsync(onProgressChanged: (bytesProcessed: Long) -> Unit,
                                        onProcessNewFile: (file: AbstractStorageFile) -> Unit): Completable
    abstract fun move(path: String, existingFileAction: ExistingFileAction)
    abstract fun share()
    abstract fun openAsFile()
    abstract fun openAsDir(): Single<ArrayList<AbstractStorageFile>>
    abstract fun createNew()
    abstract fun createFolder()
    abstract fun creationDateTime(): String
    abstract fun hasChild(fileName: String): Boolean
    abstract fun get(fileName: String): AbstractStorageFile
    abstract fun contains(fileName: String): Boolean
    abstract fun search(fileName: String): io.reactivex.Observable<AbstractStorageFile>

    companion object {
        const val SEPARATOR = "/"
    }

    enum class ExistingFileAction{
        REWRITE,
        SAVE_BOTH,
        SKIP
    }
}