package com.progressifff.nicefilemanager.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.progressifff.nicefilemanager.Constants
import com.progressifff.nicefilemanager.R
import com.progressifff.nicefilemanager.RxBus
import com.progressifff.nicefilemanager.RxEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.lang.AssertionError

class DeleteFilesDialog : DialogFragment() {

    private lateinit var callback: () -> Unit

    @SuppressLint("CheckResult")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = AlertDialog.Builder(activity)
        val filesCount = arguments!!.getInt(Constants.FILES_COUNT_KEY)
        if(filesCount < 0){
            throw AssertionError("No files to delete")
        }

        if(filesCount == 1){
            dialogBuilder.setMessage(R.string.delete_file_question)
        }
        else{
            dialogBuilder.setMessage(R.string.delete_files_question)
        }

        RxBus.listen(RxEvent.DeleteFilesEvent::class.java).subscribeWith(object: Observer<RxEvent.DeleteFilesEvent> {
                lateinit var disposable: Disposable
                override fun onComplete() {}
                override fun onError(e: Throwable) {}
                override fun onSubscribe(d: Disposable) { disposable = d }
                override fun onNext(event: RxEvent.DeleteFilesEvent) {
                    callback = event.callback
                    disposable.dispose()
                    RxBus.clearHistory()
                }
            }
        )

        dialogBuilder.setPositiveButton(getString(android.R.string.yes)) { _, _ -> callback() }

        dialogBuilder.setNegativeButton(getString(android.R.string.no), null)

        return dialogBuilder.create()
    }

    companion object {
        fun createInstance(filesCount: Int): DeleteFilesDialog{
            val deleteFilesDialog = DeleteFilesDialog()
            val arguments = Bundle()
            arguments.putInt(Constants.FILES_COUNT_KEY, filesCount)
            deleteFilesDialog.arguments = arguments
            return deleteFilesDialog
        }
    }
}