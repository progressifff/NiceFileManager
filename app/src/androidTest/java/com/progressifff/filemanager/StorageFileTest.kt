package com.progressifff.filemanager

import android.os.Environment
import android.support.test.InstrumentationRegistry
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import com.progressifff.filemanager.models.StorageFile
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageFileTest {

    @Rule
    @JvmField
    val grantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!!

    private lateinit var root: StorageFile

    @Before
    fun init(){
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.newApplication(instrumentation.context.classLoader, App::class.java.name, InstrumentationRegistry.getContext())
        instrumentation.callApplicationOnCreate(app)
        root = StorageFile(Environment.getExternalStorageDirectory(), "FileManagerTests")
        root.createFolder()
    }

    @After
    fun release(){
        val completable = root.deleteRecursivelyAsync()
        completable.subscribeOn(AndroidSchedulers.mainThread()).blockingAwait()
    }

    @Test
    fun checkRenameFile(){
        val file = StorageFile(root, "1")
        file.createNew()
        file.rename("2")
        assert(file.exists)
    }

    @Test
    fun checkOpenDir(){
        val dir = StorageFile(root, "Dir")
        dir.createFolder()
        val filesCount = 100
        for(i in 0 until filesCount) {
            val newFile = StorageFile(dir, i.toString())
            newFile.createFolder()
        }
        val files = dir.openAsDir().blockingGet()
        Assert.assertEquals(files.size, filesCount)
    }

    @Test
    fun checkCopyFiles(){
        val dest = StorageFile(root, "CopyDest")
        dest.createFolder()
        var copyTask: Completable? = null
        var filesSize = 0L
        for(i in 0 until 100){
            val newFile = StorageFile(root, i.toString())
            newFile.createFolder()
            filesSize += newFile.size

            val task = newFile.copyRecursivelyAsync(dest.path)
            task.subscribeOn(AndroidSchedulers.mainThread())
            copyTask = if(copyTask == null){
                task
            } else{
                copyTask.concatWith(task)
            }
        }
        copyTask!!.blockingAwait()

        val getFilesTask = dest.openAsDir().subscribeOn(AndroidSchedulers.mainThread())

        val files = getFilesTask.blockingGet()

        for(file in files){
            filesSize -= file.size
        }
        Assert.assertEquals(filesSize, 0L)
    }

    @Test
    fun checkDeleteFiles(){
        val filesFolder = StorageFile(root, "Files")
        filesFolder.createFolder()
        for(i in 0 until 100){
            val newFile = StorageFile(filesFolder, i.toString())
            newFile.createNew()
        }
        filesFolder.deleteRecursivelyAsync().blockingAwait()
        assert(filesFolder.exists)
    }

    @Test
    fun checkMoveFiles() {
        val filesFolder = StorageFile(root, "FilesSrc")
        filesFolder.createFolder()
        val dest = StorageFile(root, "Dest")
        dest.createFolder()
        for(i in 0 until 100){
            val newFile = StorageFile(filesFolder, i.toString())
            newFile.createFolder()
        }
        val filesSize = filesFolder.size
        filesFolder.move(dest.path)
        assert(filesFolder.exists)
        val actualActualSize = filesFolder.size
        Assert.assertEquals(filesSize, actualActualSize)
    }

    @Test
    fun checkSearchFiles(){
        for(i in 0 until 100){
            val newFile = StorageFile(root, i.toString())
            newFile.createNew()
        }
        try{
            root.search("0").blockingFirst()
        }
        catch (e: Exception){assert(false)}
        try{
            root.search("53").blockingFirst()
        }
        catch (e: Exception){assert(false)}
        try{
            root.search("99").blockingFirst()
        }
        catch (e: Exception){assert(false)}
    }
}