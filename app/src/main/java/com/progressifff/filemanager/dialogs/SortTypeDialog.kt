package com.progressifff.filemanager.dialogs

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.progressifff.filemanager.Constants.SORT_TYPE_KEY
import com.progressifff.filemanager.R
import com.progressifff.filemanager.RxBus
import com.progressifff.filemanager.RxEvent
import com.progressifff.filemanager.models.AbstractFilesNode

class SortTypeDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var lastCheckedItemChecker: ImageView
    private lateinit var sortByNameItemChecker: ImageView
    private lateinit var sortByTypeItemChecker: ImageView
    private lateinit var sortBySizeItemChecker: ImageView
    private lateinit var sortByModificationTimeItemChecker: ImageView

    override fun onClick(v: View?) {
        lastCheckedItemChecker.visibility = View.INVISIBLE
        dismiss()
        when(v!!.id){
            R.id.sortByNameItem -> {
                setSortTypeCheckerVisible(sortByNameItemChecker)
                RxBus.publish(RxEvent.FilesSortTypeChangedEvent(AbstractFilesNode.SortFilesType.NAME))
            }
            R.id.sortByTypeItem -> {
                setSortTypeCheckerVisible(sortByTypeItemChecker)
                RxBus.publish(RxEvent.FilesSortTypeChangedEvent(AbstractFilesNode.SortFilesType.TYPE))
            }
            R.id.sortByModificationTimeItem -> {
                setSortTypeCheckerVisible(sortByModificationTimeItemChecker)
                RxBus.publish(RxEvent.FilesSortTypeChangedEvent(AbstractFilesNode.SortFilesType.MODIFICATION_TIME))
            }
            R.id.sortBySizeItem -> {
                setSortTypeCheckerVisible(sortBySizeItemChecker)
                RxBus.publish(RxEvent.FilesSortTypeChangedEvent(AbstractFilesNode.SortFilesType.SIZE))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.sort_type_dialog, container, false)
        view!!.findViewById<LinearLayout>(R.id.sortByNameItem).setOnClickListener(this)
        view.findViewById<LinearLayout>(R.id.sortByTypeItem).setOnClickListener(this)
        view.findViewById<LinearLayout>(R.id.sortByModificationTimeItem).setOnClickListener(this)
        view.findViewById<LinearLayout>(R.id.sortBySizeItem).setOnClickListener(this)

        sortByNameItemChecker = view.findViewById(R.id.sortByNameItemChecker)
        sortByTypeItemChecker = view.findViewById(R.id.sortByTypeItemChecker)
        sortBySizeItemChecker = view.findViewById(R.id.sortBySizeItemChecker)
        sortByModificationTimeItemChecker = view.findViewById(R.id.sortByModificationTimeItemChecker)

        val sortType = AbstractFilesNode.SortFilesType.fromString(arguments!!.getString(SORT_TYPE_KEY)!!)

        when(sortType){
            AbstractFilesNode.SortFilesType.NAME -> setSortTypeCheckerVisible(sortByNameItemChecker)
            AbstractFilesNode.SortFilesType.SIZE -> setSortTypeCheckerVisible(sortBySizeItemChecker)
            AbstractFilesNode.SortFilesType.TYPE -> setSortTypeCheckerVisible(sortByTypeItemChecker)
            AbstractFilesNode.SortFilesType.MODIFICATION_TIME -> setSortTypeCheckerVisible(sortByModificationTimeItemChecker)
        }

        return view
    }

    private fun setSortTypeCheckerVisible(checker: ImageView){
        checker.visibility = View.VISIBLE
        lastCheckedItemChecker = checker
    }

    override fun onStart() {
        super.onStart()
        val bottomSheetBehavior = BottomSheetBehavior.from(view?.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    companion object {
        const val DIALOG_KEY = "SortTypeDialogKey"

        fun createInstance(sortFilesType: AbstractFilesNode.SortFilesType): SortTypeDialog{
            val dialog = SortTypeDialog()
            val arguments = Bundle()
            arguments.putString(SORT_TYPE_KEY, sortFilesType.name)
            dialog.arguments = arguments
            return dialog
        }
    }
}