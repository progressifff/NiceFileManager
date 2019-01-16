package com.progressifff.nicefilemanager.models

import android.os.FileObserver
import android.util.Log
import com.progressifff.nicefilemanager.AbstractFilesNode
import com.progressifff.nicefilemanager.AbstractStorageFile
import com.progressifff.nicefilemanager.App
import kotlin.properties.Delegates

class FilesNode(source: AbstractStorageFile) : AbstractFilesNode(source){

    var includeHiddenFiles: Boolean by Delegates.observable(false){
        _, old, new ->
        if(new != old && files.isNotEmpty()){
            load()
        }
    }

    private val eventsObserver: FileObserver

    init {
        eventsObserver = object : FileObserver(source.path, FileObserver.ALL_EVENTS) {
            override fun onEvent(event: Int, path: String?) {
                if(path == null){ return }
                synchronized(fileEventObservers){
                    val filePath = source.path + AbstractStorageFile.SEPARATOR + path
                    val observers = fileEventObservers[source.path]
                    if(observers != null) {
                        for(observer in observers){
                            observer.onEvent(event, StorageFile(filePath))
                        }
                    }
                }
            }
        }

        startEventWatching()
    }

    override fun check(file: AbstractStorageFile): Boolean = !(!includeHiddenFiles && file.isHidden)

    override fun release(){
        synchronized(fileEventObservers){
            if(fileEventObservers.containsKey(folder.path)){
                val observers = fileEventObservers[folder.path]
                if(observers != null){
                    observers.remove(this)
                    if(observers.isEmpty()){
                        eventsObserver.stopWatching()
                    }
                }
            }
        }
    }

    private fun startEventWatching(){
        synchronized(fileEventObservers) {
            if(!fileEventObservers.containsKey(folder.path)) {
                fileEventObservers[folder.path] = HashSet()
            }
            val observers = fileEventObservers[folder.path]
            if(!observers!!.contains(this)) {
                observers.add(this)
            }
        }
        eventsObserver.startWatching()
    }

    private fun onEvent(event: Int, file: StorageFile){

        when (event and FileObserver.ALL_EVENTS) {

            FileObserver.DELETE -> {
                App.get().handler.post{
                    val fileIndex = files.indexOf(file)
                    if (fileIndex >= 0) {
                        files.removeAt(fileIndex)
                        for(observer in eventsListeners){
                            observer.onFileRemoved(fileIndex)
                        }
                    }
                }
            }

            FileObserver.CREATE ->{
                App.get().handler.post {
                    val index = insert(file)
                    for(observer in eventsListeners){
                        observer.onFileCreated(index)
                    }
                }
            }

            FileObserver.MODIFY -> {
                App.get().handler.post {
                    (files.find { f -> f.path == file.path } as? StorageFile)?.notifyModified()
                }
            }

            FileObserver.DELETE_SELF -> {Log.v("DELETE_SELF", "DELETE_SELF")}

            FileObserver.MOVE_SELF -> {Log.v("MOVE_SELF", " MOVE_SELF")}

            FileObserver.ATTRIB -> { }

            FileObserver.MOVED_FROM -> {
                App.get().handler.post{

                    var fileIndex = -1
                    for((i, f) in files.withIndex()){
                        if(f.name == file.name) {
                            fileIndex = i
                            break
                        }
                    }
                    if (fileIndex >= 0) {
                        files.removeAt(fileIndex)
                        for(observer in eventsListeners){
                            observer.onFileRemoved(fileIndex)
                        }
                    }
                }
            }

            FileObserver.MOVED_TO -> {
                App.get().handler.post {
                    val removeFileIndex = files.indexOf(file)
                    if(removeFileIndex >= 0) {
                        files.removeAt(removeFileIndex)
                    }
                    val insertedFileIndex = insert(file)

                    for(observer in eventsListeners){
                        if(removeFileIndex == insertedFileIndex){
                            observer.onFileChanged(removeFileIndex)
                        }
                        else {
                            if(removeFileIndex >= 0) {
                                observer.onFileRemoved(removeFileIndex)
                            }
                            observer.onFileCreated(insertedFileIndex)
                        }
                    }
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilesNode

        if (eventsObserver != other.eventsObserver) return false

        return true
    }

    override fun hashCode(): Int {
        return eventsObserver.hashCode()
    }

    companion object {
        private val fileEventObservers = HashMap<String, HashSet<FilesNode>>()
    }
}