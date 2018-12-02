package com.progressifff.filemanager.dialogs

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.progressifff.filemanager.*
import com.progressifff.filemanager.models.AbstractStorageFile
import java.lang.ref.WeakReference

class FileActionsDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var file: AbstractStorageFile

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.file_actions_dialog, container, false)

        file = arguments!!.getParcelable(FILE_KEY) as AbstractStorageFile
        view.findViewById<TextView>(R.id.fileNameField).text = file.name

        FileDrawableLoader.instance.applyFileImage(file, WeakReference(view.findViewById(R.id.fileImage)))

        if(!file.isDirectory){
            val shareFileBtn = view.findViewById<LinearLayout>(R.id.shareFileBtn)
            shareFileBtn.visibility = View.VISIBLE
            shareFileBtn.setOnClickListener(this)
        }

        view.findViewById<LinearLayout>(R.id.renameFileBtn).setOnClickListener(this)
        view.findViewById<LinearLayout>(R.id.deleteFileBtn).setOnClickListener(this)
        view.findViewById<LinearLayout>(R.id.copyFileBtn).setOnClickListener(this)
        view.findViewById<LinearLayout>(R.id.cutFileBtn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.fileDetailsBtn).setOnClickListener(this)
        return view
    }

    override fun onStart() {
        super.onStart()
        val bottomSheetBehavior = BottomSheetBehavior.from(view?.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onClick(v: View?) {
        dismiss()
        when(v!!.id){
            R.id.renameFileBtn -> RxBus.publish(RxEvent.FileActionEvent(file, FileAction.RENAME))
            R.id.deleteFileBtn -> RxBus.publish(RxEvent.FileActionEvent(file, FileAction.DELETE))
            R.id.copyFileBtn -> RxBus.publish(RxEvent.FileActionEvent(file, FileAction.COPY))
            R.id.cutFileBtn -> RxBus.publish(RxEvent.FileActionEvent(file, FileAction.CUT))
            R.id.fileDetailsBtn -> RxBus.publish(RxEvent.FileActionEvent(file, FileAction.DETAILS))
            R.id.shareFileBtn -> RxBus.publish(RxEvent.FileActionEvent(file, FileAction.SHARE))
        }
    }

    companion object {
        private const val FILE_KEY = "FileKey"

        fun createInstance(storageFile: AbstractStorageFile): FileActionsDialog{
            val dialog = FileActionsDialog()
            val arguments = Bundle()
            arguments.putParcelable(FILE_KEY, storageFile)
            dialog.arguments = arguments
            return dialog
        }
    }

    enum class FileAction{
        RENAME,
        DELETE,
        COPY,
        CUT,
        SHARE,
        DETAILS
    }
}