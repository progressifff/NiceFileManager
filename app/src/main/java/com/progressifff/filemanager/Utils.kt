package com.progressifff.filemanager

import android.content.Context
import android.content.Intent
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
import java.lang.AssertionError

object Utils{
    fun showOpenFileDialog(context: Context, file: AbstractStorageFile){
        if(file.isDirectory){
            Toast.makeText(context, context.getString(R.string.open_file_error), Toast.LENGTH_SHORT).show()
        }
        val mimeType = file.mimeType
        val apkMimeType = "application/vnd.android.package-archive"
        val intentAction = if(mimeType == apkMimeType) Intent.ACTION_INSTALL_PACKAGE else Intent.ACTION_VIEW
        val intent = Intent(intentAction)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        val uri = FileUriProvider.getUri(file)
        intent.setDataAndType(uri, mimeType)

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
        else{
            Toast.makeText(context, context.getString(R.string.no_application_to_open_file), Toast.LENGTH_SHORT).show()
        }
    }

    fun showShareFileDialog(context: Context, file: AbstractStorageFile){
        assert(!file.isDirectory)
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, FileUriProvider.getUri(file))
            type = file.mimeType
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_dialog_title)))
    }

    fun calculateGridColumnsCount(): Int {
        val resources = App.get().resources
        val fileCardWidth = resources.getDimensionPixelSize(R.dimen.file_card_width)
        val filesGridItemMinSpacing = resources.getDimensionPixelSize(R.dimen.files_grid_item_min_spacing)

        val windowManger = App.get().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = Point()
        windowManger.defaultDisplay.getSize(size)

        var columnsCount = size.x / fileCardWidth

        if((columnsCount < 1)) {
            throw AssertionError("Null columns count")
        }

        if(columnsCount > 1){
            val spacing = (size.x % fileCardWidth) / columnsCount / 2
            if(spacing < filesGridItemMinSpacing){
                columnsCount -= 1
            }
        }

        return columnsCount
    }
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

fun View.runOnLayoutChanged(func: () -> Unit){
    this.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            func()
            this@runOnLayoutChanged.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
}