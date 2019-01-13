package com.progressifff.filemanager.models

import android.os.Parcel
import android.os.Parcelable
import android.webkit.MimeTypeMap
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.lang.UnsupportedOperationException
import java.util.*
import io.reactivex.Observable
import io.reactivex.Completable
import android.support.annotation.RequiresApi
import com.progressifff.filemanager.*
import java.lang.AssertionError
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.DateFormat

class StorageFile(source: File) : AbstractStorageFile(), Parcelable{
    var source = source
        private set
    private val resettableLazyManager = ResettableLazyManager()
    override val name: String get() {return source.name}
    override val path: String get() {return source.absolutePath}
    override val exists: Boolean get() {return source.exists()}
    override val notExists: Boolean get() {return !source.exists()}
    override val isHidden: Boolean get() {return source.isHidden}
    override val isDirectory: Boolean get() {return source.isDirectory}
    override val lastModified: Long get() {return source.lastModified()}
    override val canRead: Boolean get() {return source.canRead()}
    override val canWrite: Boolean get() {return source.canWrite()}
    override val size: Long by resettableLazy(resettableLazyManager) {
        if(source.isDirectory){
            val folderSize = calculateDirSize(source.path)
            if(folderSize < 0){
                throw Exception("Failed to calculate directory size")
            }
            return@resettableLazy folderSize
        }

        return@resettableLazy source.length()
    }

    override val lastModifiedDateTime: String by resettableLazy(resettableLazyManager) {
        val lastModified = source.lastModified()
        val dateStyle = DateFormat.MEDIUM
        val timeStyle = DateFormat.SHORT
        val df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.getDefault())
        return@resettableLazy df.format(Date(lastModified))
    }

    override val extension: String get() {return source.extension}
    override val parent: AbstractStorageFile? get()  {return StorageFile(source.parentFile) }
    override val mimeType: String get() {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return try{
            mimeTypeMap.getMimeTypeFromExtension(extension)!!
        }
        catch (e: Exception){
            when (extension) {
                "7z" -> "application/x-7z-compressed"
                "rar" -> "application/x-rar-compressed"
                else -> "text/plain"
            }
        }
    }

    constructor(filePath: String): this(File(filePath))

    constructor(parent: StorageFile, fileName: String): this(parent.source, fileName)

    constructor(parent: File, fileName: String): this(File(parent, fileName))

    constructor(parent: String, fileName: String): this(StorageFile(parent), fileName)

    constructor(parcel: Parcel) : this(parcel.readSerializable() as File)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(source)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<StorageFile> {
        override fun createFromParcel(parcel: Parcel): StorageFile {
            return StorageFile(parcel)
        }

        override fun newArray(size: Int): Array<StorageFile?> {
            return arrayOfNulls(size)
        }
    }

    private external fun calculateDirSize(dirPath: String): Long

    override fun rename(name: String) {
        val filePath = source.absolutePath.substring(0, source.absolutePath.lastIndexOf(File.separator) + 1) // + 1 for current slash
        val newFilePath = filePath + name
        val newFile = File(newFilePath)
        rename(newFile, true)
    }

    private fun rename(newFile: File, forceRename: Boolean = false){
        if(newFile.exists()){
            if(!forceRename){
                throw FileAlreadyExistsException(source, newFile, "File with the same name is already exists.")
            }
            else  {
                newFile.deleteRecursively()
            }
        }
        val isRenamed = source.renameTo(newFile)
        if(!isRenamed){
            throw FileSystemException(source, newFile, "Failed to rename source.")
        }
        source = newFile
    }

    override fun copyRecursivelyAsync(dest: String,
                                      onProgressChanged: ((bytesCopied: Long) -> Unit)?, // from 0 to 100
                                      onProcessNewFile: ((file: AbstractStorageFile) -> Unit)?,
                                      onError: ((file: AbstractStorageFile, messageId: Int) -> OnErrorAction)?,
                                      existingFileAction: ExistingFileAction): Completable {

        var destPath = dest

        var filesRelativePathBase = source.parent

        return Completable.create {singleEmitter ->

            when {
                this.notExists -> onError?.invoke(this, R.string.file_not_exist)
                (source.path == StorageFile(destPath).path) -> onError?.invoke(StorageFile(destPath), R.string.destination_folder_is_subfolder_of_source)
                else -> {
                    try {

                        var overallBytesCopied = 0L

                        //walk throw a directory
                        source.walkTopDown().forEach { currentFile ->

                            //If disposed -> cancel copying
                            if (singleEmitter.isDisposed) {
                                return@create
                            }

                            if (!currentFile.exists()) {
                                if (onError?.invoke(StorageFile(currentFile), R.string.copying_file_is_not_exist) == OnErrorAction.TERMINATE) {
                                    return@create
                                }

                            } else {
                                //Find dest source for current source
                                val relativePath = currentFile.toRelativeString(File(filesRelativePathBase))
                                var destFile = File(destPath, relativePath)

                                //Delete dest source if exists
                                if (destFile.exists()) {

                                    when (existingFileAction) {
                                        ExistingFileAction.REWRITE -> {
                                            when {
                                                destFile.isDirectory -> destFile.deleteRecursively()
                                                destFile.path == source.path -> return@forEach
                                                else -> destFile.delete()
                                            }
                                        }

                                        ExistingFileAction.SAVE_BOTH -> {
                                            destFile = getUniqueFile(destFile)
                                            destPath +=  (File.separator + destFile.name)
                                            filesRelativePathBase = source.path
                                        }

                                        ExistingFileAction.SKIP -> return@forEach
                                    }
                                }

                                onProcessNewFile?.invoke(StorageFile(currentFile))

                                //Create filesNode or copy source bytes
                                if (currentFile.isDirectory) {
                                    destFile.mkdirs()

                                } else {

                                    destFile.parentFile.mkdirs()

                                    currentFile.inputStream().use { input ->
                                        destFile.outputStream().use { output ->
                                            var start = System.currentTimeMillis()
                                            input.copyTo(output, { bytesCopied ->
                                                run {
                                                    val current = System.currentTimeMillis()
                                                    if((current - start) > 100) {
                                                        start = current
                                                        onProgressChanged?.invoke(overallBytesCopied + bytesCopied)
                                                    }
                                                }
                                            }) {
                                                run {

                                                    if (singleEmitter.isDisposed) {
                                                        destFile.deleteRecursively()
                                                    }
                                                    return@run singleEmitter.isDisposed || !destFile.exists()
                                                }
                                            }
                                        }
                                    }
                                    overallBytesCopied += destFile.length()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        singleEmitter.onError(e)
                    }
                }
            }
            singleEmitter.onComplete()
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
    }

    private fun getUniqueFile(file: File): File {
        val fileName = file.name
        val extensionIndex = file.name.lastIndexOf(".")
        val fileExtension = if(extensionIndex != -1) fileName.substring(extensionIndex) else ""
        var i = 0

        var resultFile: File
        do{
            resultFile = File(file.parent, file.nameWithoutExtension + "_Copy($i)" + fileExtension)
            i++
        }while(resultFile.exists())

        return resultFile
    }

    override fun deleteRecursivelyAsync(onProgressChanged: ((bytesProcessed: Long) -> Unit)?, //from 0.0 - 1.0
                                        onProcessNewFile: ((file: AbstractStorageFile) -> Unit)?): Completable {

        return Completable.create { completableEmitter ->
            try{
                var overallBytes = 0L
                //walk throw a directory
                source.walkBottomUp().forEach { currentFile ->

                    //If disposed -> cancel copying
                    if(completableEmitter.isDisposed){
                        return@create
                    }

                    onProcessNewFile?.invoke(StorageFile(currentFile))

                    if(currentFile.exists()){
                        overallBytes += currentFile.length()
                        currentFile.delete()
                        onProgressChanged?.invoke(overallBytes)
                    }
                }
                completableEmitter.onComplete()
            }
            catch (e: IOException){
                completableEmitter.onError(e)
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
    }

    override fun move(path: String, existingFileAction: ExistingFileAction) {
        var destFile = File(path + File.separator + source.name)
        if (destFile.exists()) {
            when (existingFileAction) {
                ExistingFileAction.REWRITE -> rename(destFile, true)

                ExistingFileAction.SAVE_BOTH -> {
                    destFile = getUniqueFile(destFile)
                }

                ExistingFileAction.SKIP -> return
            }
        }
        rename(destFile)
    }

    override fun openAsDir(): Single<ArrayList<AbstractStorageFile>> {
        if(!source.isDirectory){
            throw AssertionError("File is not a directory")
        }

        return Single.create<ArrayList<AbstractStorageFile>> {
            try{
                if(!source.isDirectory){
                    throw UnsupportedOperationException("File is not a directory.")
                }
                val files = source.listFiles() ?: throw IOException("An I/O error occurs.")
                val result = ArrayList<AbstractStorageFile>()
                for (entry in files) {
                    if(it.isDisposed) return@create
                    result.add(StorageFile(entry))
                }

                if(!it.isDisposed){
                    it.onSuccess(result)
                }
            }
            catch (e: Exception){
                if(!it.isDisposed) {
                    it.onError(e)
                }
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
    }

    override fun search(fileName: String): Observable<AbstractStorageFile>{
        return Observable.create<AbstractStorageFile>{ it ->
            try{
                source.walkTopDown().forEach { currentFile ->
                    if(currentFile.name.contains(fileName, true)){
                        it.onNext(StorageFile(currentFile))
                    }
                }
                it.onComplete()
            }
            catch (e: Exception){
                it.onError(e)
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
    }

    override fun createNew(){
        val createFileResult = source.createNewFile()
        if(!createFileResult){
            throw FileAlreadyExistsException(source, null, "Failed to create new source, because the another one with the same name is already exists.")
        }
    }

    override fun createFolder(){
        val createDirResult = source.mkdir()
        if(!createDirResult){
            throw FileAlreadyExistsException(source, null, "Failed to create new directory, because the another one with the same name is already exists.")
        }
    }

    override fun contains(fileName: String): Boolean {
        if(!source.isDirectory){
            throw Exception("File is not a directory")
        }
        return File(this.source, fileName).exists()
    }

    @RequiresApi(26)
    override fun creationDateTime(): String{
        val attributes = Files.readAttributes(source.toPath(), BasicFileAttributes::class.java)
        val fileCreationTime = attributes.creationTime()
        val dateStyle = DateFormat.MEDIUM
        val timeStyle = DateFormat.SHORT
        val df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.getDefault())
        return df.format(Date(fileCreationTime.toMillis()))
    }

    override fun get(fileName: String): AbstractStorageFile {
        if(!source.isDirectory){
            throw Exception("File is not a directory")
        }
        return StorageFile(File(source, fileName))
    }

    fun notifyModified(){
        resettableLazyManager.reset()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (source != (other as StorageFile).source) return false
        return true
    }

    override fun hashCode(): Int {
        return source.hashCode()
    }
}