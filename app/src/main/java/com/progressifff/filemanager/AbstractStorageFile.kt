package com.progressifff.filemanager

import android.content.Intent
import android.os.Parcelable
import android.support.annotation.StringRes
import com.progressifff.filemanager.IFileUriProvider
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
    abstract val canWrite: Boolean
    abstract val size: Long
    abstract val extension: String
    abstract val parent: AbstractStorageFile?
    abstract val mimeType: String
    abstract val lastModifiedDateTime: String

    abstract fun rename(name: String)
    abstract fun copyRecursivelyAsync(dest: String,
                                      onProgressChanged: ((bytesCopied: Long) -> Unit)? = null,
                                      onProcessNewFile: ((file: AbstractStorageFile) -> Unit)? = null,
                                      onError: ((file: AbstractStorageFile, messageId: Int) -> OnErrorAction)? = null,
                                      existingFileAction: ExistingFileAction = ExistingFileAction.REWRITE): Completable

    abstract fun deleteRecursivelyAsync(onProgressChanged: ((bytesProcessed: Long) -> Unit)? = null,
                                        onProcessNewFile: ((file: AbstractStorageFile) -> Unit)? = null): Completable
    abstract fun move(path: String, existingFileAction: ExistingFileAction = ExistingFileAction.REWRITE)
    abstract fun openAsDir(): Single<ArrayList<AbstractStorageFile>>
    abstract fun createNew()
    abstract fun createFolder()
    abstract fun creationDateTime(): String
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