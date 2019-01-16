package com.progressifff.nicefilemanager

import android.os.Bundle
import android.support.annotation.IdRes
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.progressifff.nicefilemanager.dialogs.PasteExistingFilesDialog
import com.progressifff.nicefilemanager.presenters.FilesTasksPresenter
import com.progressifff.nicefilemanager.views.FilesTasksView

class FilesTasks(@IdRes val viewId: Int) : FilesTasksView{
    private lateinit var presenter: FilesTasksPresenter
    private lateinit var filesTasksTitle: TextView
    private lateinit var filesTasksViewBehavior: BottomSheetBehavior<*>
    private lateinit var filesTasksList: RecyclerView
    private lateinit var filesTasksListAdapter: RecyclerView.Adapter<*>
    private lateinit var filesTasksListLayoutManager: RecyclerView.LayoutManager
    private lateinit var activity: FragmentActivity

    fun onActivityCreate(activity: FragmentActivity, savedInstanceState: Bundle?){

        this.activity = activity

        presenter = if(savedInstanceState == null){
            FilesTasksPresenter()
        }
        else
            try{
                PresenterManager.restorePresenter<FilesTasksPresenter>(savedInstanceState, PRESENTER_KEY)
            } catch (e: Exception){
                e.printStackTrace()
                FilesTasksPresenter()
            }


        filesTasksTitle = activity.findViewById(R.id.filesTasksTitle)

        updateFilesTasksTitle()

        val tasksView = activity.findViewById<LinearLayout>(viewId)

        filesTasksViewBehavior = tasksView.bottomSheetBehavior()

        if(presenter.tasksCount == 0){
            hideFilesTaskView()
        }

        filesTasksListLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        filesTasksListAdapter = FilesTasksListAdapter(presenter)

        filesTasksList = activity.findViewById(R.id.filesTasksList)

        filesTasksList.apply {
            adapter = filesTasksListAdapter
            layoutManager = filesTasksListLayoutManager
            setHasFixedSize(true)
        }

        (filesTasksList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    }

    fun onActivityStart(){
        presenter.bindView(this)
        filesTasksListAdapter.notifyDataSetChanged()
    }

    fun onActivityStop(){
        presenter.unbindView()
    }

    fun onSaveInstance(outState: Bundle){
        PresenterManager.savePresenter(presenter, outState, PRESENTER_KEY)
    }

    fun release(){
        presenter.onCancelAllTasks()
    }

    override fun showCopyExistingFilesDialog(filesNames: ArrayList<String>) {
        val dialog = PasteExistingFilesDialog.createInstance(filesNames)
        dialog.show(activity.supportFragmentManager, PasteExistingFilesDialog::class.java.name)
    }

    override fun update() {
        if(presenter.tasksCount == 0){
            hideFilesTaskView()
        }
        else{
            showFilesTasksView()
        }
        filesTasksListAdapter.notifyDataSetChanged()
    }

    override fun showFilesTasksView() {
        filesTasksViewBehavior.isHideable = false
        filesTasksViewBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun hideFilesTaskView() {
        filesTasksViewBehavior.isHideable = true
        filesTasksViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun notifyFileTaskAdded() {
        showFilesTasksView()
        val filesTasksCounts = presenter.tasksCount
        updateFilesTasksTitle()
        filesTasksListAdapter.notifyItemInserted(filesTasksCounts - 1)
    }

    override fun removeFileTask(taskIndex: Int) {
        updateFilesTasksTitle()
        filesTasksListAdapter.notifyItemRemoved(taskIndex)
        if(presenter.tasksCount == 0){
            hideFilesTaskView()
        }
    }

    override fun updateFileTask(taskIndex: Int) {
        if(filesTasksViewBehavior.isHideable &&
                filesTasksViewBehavior.state == BottomSheetBehavior.STATE_HIDDEN){
            showFilesTasksView()
        }

        if(!filesTasksList.itemAnimator!!.isRunning) {
            filesTasksListAdapter.notifyItemChanged(taskIndex)
        }
    }

    override fun showToast(messageId: Int) {
        Toast.makeText(activity, activity.getString(messageId), Toast.LENGTH_SHORT).show()
    }

    private fun updateFilesTasksTitle(){
        val tasksCount = presenter.tasksCount
        val title = if(tasksCount > 1) "$tasksCount ${activity.getString(R.string.tasks)}" else "$tasksCount ${activity.getString(R.string.task)}"
        filesTasksTitle.text = title
    }

    companion object {
        private val PRESENTER_KEY = FilesTasks::class.java.name + "PresenterId"
    }
}