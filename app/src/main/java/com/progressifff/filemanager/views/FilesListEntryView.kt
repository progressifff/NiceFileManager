package com.progressifff.filemanager.views

import android.graphics.drawable.Drawable
import android.widget.ImageView

interface FilesListEntryView {
    fun setName(name: String)
    fun setModificationDate(date: String)
    fun setSelected(isSelected: Boolean)
    fun setFileImage(image: Drawable?)
    fun getImageView(): ImageView
}