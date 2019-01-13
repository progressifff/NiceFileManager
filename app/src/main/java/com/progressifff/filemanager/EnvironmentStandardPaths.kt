package com.progressifff.filemanager

import android.os.Environment

interface StandardPaths{
    fun getInternalStoragePath(): String
    fun getDownloadsFolderPath(): String
    fun getPicturesFolderPath(): String
    fun getMoviesFolderPath(): String
    fun getMusicFolderPath(): String
    fun getPhotosFolderPath(): String
    fun getDocumentsFolderPath(): String
}

object EnvironmentStandardPaths : StandardPaths{
    override fun getInternalStoragePath(): String = Environment.getExternalStorageDirectory().path
    override fun getDownloadsFolderPath(): String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    override fun getPicturesFolderPath(): String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path
    override fun getMoviesFolderPath(): String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path
    override fun getMusicFolderPath(): String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).path
    override fun getPhotosFolderPath(): String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path
    override fun getDocumentsFolderPath(): String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path
}