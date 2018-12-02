package com.progressifff.filemanager.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.progressifff.filemanager.R
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.models.StorageFile
import com.progressifff.filemanager.toGB
import com.progressifff.filemanager.toKB
import com.progressifff.filemanager.toMB
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference

class FileDetailsDialog : DialogFragment() {

    private var calculateFileSizeDisposable = WeakReference<Disposable>(null)

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val file = arguments!!.getParcelable<StorageFile>(FILE_KEY)

        val content = activity!!.layoutInflater.inflate(R.layout.file_details_dialog, null)
        content.findViewById<TextView>(R.id.filePathText).text = file!!.path

        if(Build.VERSION.SDK_INT >= 26){
            content.findViewById<TextView>(R.id.fileCreationTimeLabel).visibility = View.VISIBLE
            val fileCreationTimeText = content.findViewById<TextView>(R.id.fileCreationTimeText)
            fileCreationTimeText.visibility = View.VISIBLE
            fileCreationTimeText.text = file.creationDateTime()
        }

        content.findViewById<TextView>(R.id.fileModifiedTimeText).text = file.lastModifiedDateTime

        calculateFileSizeDisposable = WeakReference(Single.fromCallable<Long> { return@fromCallable file.size }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io()).subscribeWith(object : DisposableSingleObserver<Long>(){
            override fun onError(e: Throwable) { Toast.makeText(context, getString(R.string.calculate_file_size_error), Toast.LENGTH_SHORT).show() }

            override fun onSuccess(bytes: Long) {
                content.findViewById<ProgressBar>(R.id.fileSizeCalculationProgressBar).visibility = View.GONE
                val fileSizeText = content.findViewById<TextView>(R.id.fileSizeText)
                fileSizeText.visibility = View.VISIBLE

                val bytesInGb = 1073741824
                val bytesInMb = 1048576
                val bytesInKb = 1024

                when {
                    bytes > bytesInGb -> {
                        fileSizeText.text = String.format("%.2f GB", bytes.toGB())
                    }
                    bytes > bytesInMb -> {
                        fileSizeText.text = String.format("%.2f MB", bytes.toMB())
                    }
                    bytes > bytesInKb -> {
                        fileSizeText.text = String.format("%.2f KB", bytes.toKB())
                    }
                    else -> {
                        fileSizeText.text = String.format("%d B", bytes)
                    }
                }
            }
        }))

        val dialogBuilder = AlertDialog.Builder(context!!)
        dialogBuilder.setTitle(R.string.file_details_dialog_title)
        dialogBuilder.setView(content)
        dialogBuilder.setPositiveButton(getString(android.R.string.ok), null)
        return dialogBuilder.create()
    }

    override fun onDestroy() {
        super.onDestroy()
        calculateFileSizeDisposable.get()?.dispose()
    }

    companion object {
        private const val FILE_KEY = "FileKey"

        fun createInstance(file: AbstractStorageFile): FileDetailsDialog{
            val dialog = FileDetailsDialog()
            val parameters = Bundle()
            parameters.putParcelable(FILE_KEY, file)
            dialog.arguments = parameters
            return dialog
        }
    }
}