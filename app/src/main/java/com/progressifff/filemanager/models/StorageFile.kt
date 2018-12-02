package com.progressifff.filemanager.models

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.content.FileProvider
import android.util.Log
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
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.DateFormat

class StorageFile(var file: File) : AbstractStorageFile(), Parcelable{
    private val resettableLazyManager = ResettableLazyManager()
    override val name: String get() {return file.name}
    override val path: String get() {return file.absolutePath}
    override val exists: Boolean get() {return file.exists()}
    override val notExists: Boolean get() {return !file.exists()}
    override val isHidden: Boolean get() {return file.isHidden}
    override val isDirectory: Boolean get() {return file.isDirectory}
    override val lastModified: Long get() {return file.lastModified()}
    override val canRead: Boolean get() {return file.canRead()}
    override val canWriter: Boolean get() {return file.canWrite()}
    override val size: Long by resettableLazy(resettableLazyManager) {
        if(file.isDirectory){
            val folderSize = calculateDirSize(file.path)
            if(folderSize < 0){
                throw Exception("Failed to calculate directory size")
            }
            return@resettableLazy folderSize
        }

        return@resettableLazy file.length()
    }

    override val lastModifiedDateTime: String by resettableLazy(resettableLazyManager) {
        val lastModified = file.lastModified()
        val dateStyle = DateFormat.MEDIUM
        val timeStyle = DateFormat.SHORT
        val df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.getDefault())
        return@resettableLazy df.format(Date(lastModified))
    }

    override val extension: String get() {return file.extension}
    override val parent: AbstractStorageFile? get()  {return StorageFile(file.parentFile) }
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

    constructor(fileName: String): this(File(fileName))

    constructor(parent: StorageFile, fileName: String): this(parent.file, fileName)

    constructor(parent: File, fileName: String): this(File(parent, fileName))

    constructor(parent: String, fileName: String): this(StorageFile(parent), fileName)

    constructor(parcel: Parcel) : this(parcel.readSerializable() as File)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(file)
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

        init {
            System.loadLibrary("native-lib")
        }
    }

    private external fun calculateDirSize(dirPath: String): Long

    override fun rename(name: String) {
        val filePath = file.absolutePath.substring(0, file.absolutePath.lastIndexOf(File.separator) + 1) // + 1 for current slash
        val newFileName = filePath + name
        val newFile = File(newFileName)
        rename(newFile, true)
    }

    private fun rename(newFile: File, forceRename: Boolean = false){
        if(newFile.exists()){
            if(!forceRename){
                throw FileAlreadyExistsException(file, newFile, "The file with the same name is already exists.")
            }
            else  {
                newFile.deleteRecursively()
            }
        }
        val isRenamed = file.renameTo(newFile)
        if(!isRenamed){
            throw FileSystemException(file, newFile, "Failed to rename file.")
        }
        file = newFile
    }

    override fun copyRecursively(destPath: String, existingFileAction: ExistingFileAction){
        when(existingFileAction){
            ExistingFileAction.REWRITE -> file.copyRecursively(File(destPath, file.name), true)
            ExistingFileAction.SAVE_BOTH -> {
                val destFile = getUniqueFile(File(destPath, file.name))
                file.copyRecursively(destFile)
            }
            else -> { }
        }
    }

    override fun copyRecursivelyAsync(dest: String,
                                      onProgressChanged: (bytesCopied: Long) -> Unit, // from 0 to 100
                                      onProcessNewFile: (file: AbstractStorageFile) -> Unit,
                                      onError: (file: AbstractStorageFile, e: Exception) -> OnErrorAction,
                                      existingFileAction: ExistingFileAction): Completable {

        var destPath = dest

        var filesRelativePathBase = file.parent

        return Completable.create {singleEmitter ->

            when {
                this.notExists -> onError(this, NoSuchFieldException("The source file doesn't exist."))
                (file.path == StorageFile(destPath).path) -> onError(StorageFile(destPath), IOException("The destination folder is a subfolder of the source folder."))
                else -> {
                    try {

                        var overallBytesCopied = 0L

                        //walk throw a directory
                        file.walkTopDown().forEach { currentFile ->

                            //If disposed -> cancel copying
                            if (singleEmitter.isDisposed) {
                                return@create
                            }

                            if (!currentFile.exists()) {
                                if (onError(StorageFile(currentFile),
                                                NoSuchFileException(file = currentFile,
                                                        reason = "The source file doesn't exist.")) == OnErrorAction.TERMINATE) {
                                    return@create
                                }

                            } else {
                                //Find dest file for current file
                                val relativePath = currentFile.toRelativeString(File(filesRelativePathBase))
                                var destFile = File(destPath, relativePath)

                                //Delete dest file if exists
                                if (destFile.exists()) {

                                    when (existingFileAction) {
                                        ExistingFileAction.REWRITE -> {
                                            when {
                                                destFile.isDirectory -> destFile.deleteRecursively()
                                                destFile.path == file.path -> return@forEach
                                                else -> destFile.delete()
                                            }
                                        }

                                        ExistingFileAction.SAVE_BOTH -> {
                                            destFile = getUniqueFile(destFile)
                                            destPath +=  ("/" + destFile.name)
                                            filesRelativePathBase = file.path
                                            Log.v("AAA", destPath)
                                        }

                                        ExistingFileAction.SKIP -> return@forEach
                                    }
                                }

                                onProcessNewFile(StorageFile(currentFile))

                                //Create filesNode or copy file bytes
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
                                                        onProgressChanged(overallBytesCopied + bytesCopied)
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

    override fun deleteRecursivelyAsync(onProgressChanged: (bytesProcessed: Long) -> Unit, //from 0.0 - 1.0
                                        onProcessNewFile: (file: AbstractStorageFile) -> Unit): Completable {

        return Completable.create { completableEmitter ->
            try{
                var overallBytes = 0L
                //walk throw a directory
                file.walkBottomUp().forEach {currentFile ->

                    //If disposed -> cancel copying
                    if(completableEmitter.isDisposed){
                        return@create
                    }

                    onProcessNewFile(StorageFile(currentFile))

                    if(currentFile.exists()){
                        overallBytes += currentFile.length()
                        currentFile.delete()
                        onProgressChanged(overallBytes)
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

    override fun move(path: String, existingFileAction: ExistingFileAction){
        var destFile = File(path + File.separator + file.name)
        if(destFile.exists()){
            when(existingFileAction){
                ExistingFileAction.REWRITE -> rename(destFile, true)

                ExistingFileAction.SAVE_BOTH -> {
                    destFile = getUniqueFile(destFile)
                }

                ExistingFileAction.SKIP -> return
            }
        }
        rename(destFile)
    }

    override fun share() {
        assert(!isDirectory)

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(App.get(), BuildConfig.APPLICATION_ID, file))
            type = mimeType
        }
        App.get().startActivity(Intent.createChooser(sendIntent, App.get().getString(R.string.share_dialog_title)))
    }

    override fun openAsFile() {
        assert(!file.isDirectory)

        val mimeType = this.mimeType
        val apkMimeType = "application/vnd.android.package-archive"
        val intentAction = if(mimeType == apkMimeType) Intent.ACTION_INSTALL_PACKAGE else Intent.ACTION_VIEW
        val fileIntent = Intent(intentAction)
        fileIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        val uri = FileProvider.getUriForFile(App.get(), BuildConfig.APPLICATION_ID, file)
        fileIntent.setDataAndType(uri, mimeType)
        //Check if there is an application for this intent
        val packageManager = App.get().packageManager
        if (fileIntent.resolveActivity(packageManager) != null) {
            App.get().startActivity(fileIntent)
        }
        else{
            throw UnsupportedOperationException("There isn`t an application for opening this file")
        }
    }

    override fun openAsDir(): Single<ArrayList<AbstractStorageFile>> {
        assert(file.isDirectory)

        return Single.create<ArrayList<AbstractStorageFile>> {
            try{
                if(!file.isDirectory){
                    throw UnsupportedOperationException("File is not a directory.")
                }
                val files = file.listFiles() ?: throw IOException("An I/O error occurs.")
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
                file.walkTopDown().forEach {currentFile ->
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
        val createFileResult = file.createNewFile()
        if(!createFileResult){
            throw FileAlreadyExistsException(file, null, "Failed to create new file, because the another one with the same name is already exists.")
        }
    }

    override fun createFolder(){
        val createDirResult = file.mkdir()
        if(!createDirResult){
            throw FileAlreadyExistsException(file, null, "Failed to create new directory, because the another one with the same name is already exists.")
        }
    }

    override fun contains(fileName: String): Boolean {
        if(!file.isDirectory){
            throw Exception("File should be a directory")
        }
        return File(this.file, fileName).exists()
    }

    @RequiresApi(26)
    override fun creationDateTime(): String{
        val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        val fileCreationTime = attributes.creationTime()
        val dateStyle = DateFormat.MEDIUM
        val timeStyle = DateFormat.SHORT
        val df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.getDefault())
        return df.format(Date(fileCreationTime.toMillis()))
    }

    override fun hasChild(fileName: String): Boolean {
        if(!file.isDirectory){
            throw Exception("File is not a directory")
        }
        return File(file, fileName).exists()
    }

    override fun get(fileName: String): AbstractStorageFile {
        if(!file.isDirectory){
            throw Exception("File is not a directory")
        }
        return StorageFile(File(file, fileName))
    }

    fun notifyModified(){
        resettableLazyManager.reset()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (file != (other as StorageFile).file) return false
        return true
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }
}