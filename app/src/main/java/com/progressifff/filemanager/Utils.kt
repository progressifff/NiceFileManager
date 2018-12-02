package com.progressifff.filemanager

import android.content.Context
import android.content.res.Resources
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.preference.PreferenceManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ListView
import android.widget.Toast
import io.reactivex.disposables.Disposable
import java.io.InputStream
import java.io.OutputStream
import android.graphics.Point
import android.view.WindowManager




fun saveStringToSharedPreferences(key: String, value: String){
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get())
    val editor = sharedPreferences.edit()
    editor.putString(key, value)
    editor.apply()
}

fun saveBooleanToSharedPreferences(key: String, value: Boolean){
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get())
    val editor = sharedPreferences.edit()
    editor.putBoolean(key, value)
    editor.apply()
}

fun getBooleanFromSharedPreferences(key: String, defaultValue: Boolean = false): Boolean{
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get())
    return sharedPreferences.getBoolean(key, defaultValue)
}

fun getStringFromSharedPreferences(key: String, defaultValue: String = ""): String{
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get())
    return sharedPreferences.getString(key, defaultValue)!!
}

fun showToast(message: String){
    Toast.makeText(App.get(), message, Toast.LENGTH_SHORT).show()
}

fun getStringFromRes(id: Int): String{
    return App.get().getString(id)
}

fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Long.toGB(): Float = this.toFloat() / 1073741824

fun Long.toMB(): Float = this.toFloat() / 1048576

fun Long.toKB(): Float = this.toFloat() / 1024

fun InputStream.copyTo(out: OutputStream, onProgressChanged: (bytesCopied: Long) -> Unit, isCanceled: () -> Boolean): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytes = read(buffer)
    while (bytes >= 0 && !(isCanceled())) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        onProgressChanged(bytesCopied)
        bytes = read(buffer)
    }
    return bytesCopied
}

fun Menu.uncheckItems(){
    val size = this.size()
    for (i in 0 until size) {
        val item = this.getItem(i)
        if (item.hasSubMenu()) {
            item.subMenu.uncheckItems()
        } else {
            item.isChecked = false
        }
    }
}

var View.visible: Boolean
    get() {
        return visibility == View.VISIBLE
    }
    set(value) {
        visibility = if(value) View.VISIBLE else View.GONE
    }

fun View.gone(){
    this.visibility = View.GONE
}

fun disposeResource(disposable: Disposable?){
    if(disposable != null && !disposable.isDisposed){
        disposable.dispose()
    }
}

fun View.bottomSheetBehavior(): BottomSheetBehavior<*>{
    return BottomSheetBehavior.from(this)
}

val RecyclerView.isScrollable: Boolean get() = this.canScrollVertically(1) || this.canScrollVertically(-1)

val ListView.isScrollable: Boolean get() {
    if(this.lastVisiblePosition < 0){
        return false
    }
    val child = this.getChildAt(this.lastVisiblePosition)
    return child != null && this.getChildAt(this.lastVisiblePosition).bottom > this.height
}

fun calculateGridColumnsCount(): Int {
    val resources = App.get().resources
    val fileCardWidth = resources.getDimensionPixelSize(R.dimen.file_card_width)
    val filesGridItemMinSpacing = resources.getDimensionPixelSize(R.dimen.files_grid_item_min_spacing)

    val windowManger = App.get().getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val size = Point()
    windowManger.defaultDisplay.getSize(size)

    var columnsCount = size.x / fileCardWidth

    assert(columnsCount > 0)

    if(columnsCount > 1){
        val spacing = (size.x % fileCardWidth) / columnsCount / 2
        if(spacing < filesGridItemMinSpacing){
            columnsCount -= 1
        }
    }

    return columnsCount
}

fun View.runOnLayoutChanged(func: () -> Unit){
    this.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            func()
            this@runOnLayoutChanged.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
}