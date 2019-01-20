package com.progressifff.nicefilemanager.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.progressifff.nicefilemanager.R
import com.progressifff.nicefilemanager.AbstractStorageFile
import com.progressifff.nicefilemanager.RxBus
import com.progressifff.nicefilemanager.RxEvent
import io.reactivex.disposables.Disposable
import java.util.*

class PasteExistingFilesDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var filesNames: ArrayList<String>
    private lateinit var pasteExistingFilesCallback: (existingFileAction: AbstractStorageFile.ExistingFileAction) -> Unit

    @SuppressLint("CheckResult")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.paste_existing_files_dialog, container, false)
        filesNames = arguments!!.getStringArrayList(EXISTING_FILES_NAMES_KEY)!!
        RxBus.listen(RxEvent.PasteExistingFilesEvent::class.java).subscribeWith(object: io.reactivex.Observer<RxEvent.PasteExistingFilesEvent>{
            lateinit var disposable: Disposable
            override fun onComplete() { }
            override fun onError(e: Throwable) { }
            override fun onSubscribe(d: Disposable) { disposable = d }
            override fun onNext(event: RxEvent.PasteExistingFilesEvent) {
                disposable.dispose()
                pasteExistingFilesCallback = event.callback
                RxBus.clearHistory()
            }
        })
        view!!.findViewById<TextView>(R.id.saveBothFilesBtn).setOnClickListener(this)
        view.findViewById<TextView>(R.id.skipFilesBtn).setOnClickListener(this)
        view.findViewById<TextView>(R.id.rewriteFilesBtn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.showExistingFilesBtn).setOnClickListener(this)
        return view
    }

    override fun onClick(v: View?) {
        when(v?.id){

            R.id.saveBothFilesBtn -> {
                pasteExistingFilesCallback(AbstractStorageFile.ExistingFileAction.SAVE_BOTH)
            }

            R.id.skipFilesBtn -> {
                pasteExistingFilesCallback(AbstractStorageFile.ExistingFileAction.SKIP)
            }

            R.id.rewriteFilesBtn -> {
                pasteExistingFilesCallback(AbstractStorageFile.ExistingFileAction.REWRITE)
            }

            R.id.showExistingFilesBtn -> {
                val existingFilesListDialog = ExistingFilesListDialog.createInstance(filesNames)
                existingFilesListDialog.show(fragmentManager, ExistingFilesListDialog::class.java.name)
                return
            }
        }
        dismiss()
    }

    companion object {
        private const val EXISTING_FILES_NAMES_KEY = "ExistingFilesNames"

        fun createInstance(filesNames: ArrayList<String>): PasteExistingFilesDialog{
            val dialog = PasteExistingFilesDialog()
            val arguments = Bundle()
            arguments.putStringArrayList(EXISTING_FILES_NAMES_KEY, filesNames)
            dialog.arguments = arguments
            return dialog
        }
    }

    override fun onStart() {
        super.onStart()
        val bottomSheetBehavior = BottomSheetBehavior.from(view?.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}