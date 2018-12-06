package com.progressifff.filemanager

import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.progressifff.filemanager.presenters.FilesPresenter
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import com.progressifff.filemanager.dialogs.*
import com.progressifff.filemanager.models.AbstractFilesNode
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.presenters.MainPresenter
import android.util.TypedValue
import java.lang.Exception
import android.widget.LinearLayout
import com.progressifff.filemanager.views.FilesView

class FilesFragment : Fragment(), FilesView {

    private lateinit var noFilesMessage: LinearLayout
    private lateinit var filesList: RecyclerView
    private lateinit var filesListAdapter: RecyclerView.Adapter<*>
    private lateinit var filesListLinearLayoutManager: LinearLayoutManager
    private lateinit var filesListGridLayoutManager: GridLayoutManager
    private lateinit var filesListGridLayoutDecoration: RecyclerView.ItemDecoration
    private lateinit var updateViewProgressBar: ProgressBar
    private lateinit var filesListRefresher: SwipeRefreshLayout
    private lateinit var presenter: FilesPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        presenter = if(savedInstanceState == null){
            FilesPresenter()
        }
        else try{
            PresenterManager.instance.restorePresenter<FilesPresenter>(savedInstanceState)
        }
        catch (e: Exception){
            e.printStackTrace()
            FilesPresenter()
        }

        activity!!.findViewById<FloatingActionButton>(R.id.addFolderFab).setOnClickListener(presenter.onViewClickListener)
        val view = inflater.inflate(R.layout.files_fragment, container, false)

        noFilesMessage = view.findViewById(R.id.noFilesMsgView)
        updateViewProgressBar = view.findViewById(R.id.loadFilesProgressBar)

        filesListRefresher = view.findViewById(R.id.filesListRefresher)
        val typedValue = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.layoutRefresherColor, typedValue, true)
        filesListRefresher.setProgressBackgroundColorSchemeColor(typedValue.data)
        activity!!.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        filesListRefresher.setColorSchemeColors(typedValue.data)
        filesListRefresher.setOnRefreshListener(presenter)
        initFilesList(view, savedInstanceState)
        return view
    }

    private fun initFilesList(root: View, savedInstanceState: Bundle?) {
        filesList = root.findViewById(R.id.filesList)
        filesListAdapter = FilesListAdapter(presenter)
        filesListLinearLayoutManager = LinearLayoutManager(context)
        filesListGridLayoutManager = GridLayoutManager(context, calculateGridColumnsCount())
        filesListGridLayoutDecoration = RecyclerViewGridLayoutDecoration()

        filesList.apply{
            setHasFixedSize(true)
            setItemViewCacheSize(40)
            adapter = filesListAdapter
        }

        filesList.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                filesListRefresher.isEnabled = (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0
            }
        })

        if(savedInstanceState != null){
            val filesDisplayMode = MainPresenter.FilesDisplayMode.fromString(getStringFromSharedPreferences(Constants.FILES_DISPLAY_MODE_KEY, MainPresenter.FilesDisplayMode.LIST.name))
            when(filesDisplayMode){
                MainPresenter.FilesDisplayMode.LIST -> filesList.layoutManager = filesListLinearLayoutManager
                MainPresenter.FilesDisplayMode.GRID -> {
                    filesList.layoutManager = filesListGridLayoutManager
                    filesList.addItemDecoration(filesListGridLayoutDecoration)
                }
            }
        }
        filesList.runOnLayoutChanged { setupToolBarScrollingBehavior(filesList.isScrollable) }
    }

    override fun onStart() {
        super.onStart()
        presenter.bindView(this)
    }

    override fun onStop(){
        super.onStop()
        presenter.unbindView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.files_fragment_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        presenter.onPrepareOptionsMenu(menu!!)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return presenter.onOptionsItemSelected(item!!)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        PresenterManager.instance.savePresenter(presenter, outState)
    }

    override fun showDeleteFilesDialog(filesCount: Int) {
        val dialog = DeleteFilesDialog.createInstance(filesCount)
        dialog.show(childFragmentManager, DeleteFilesDialog::class.java.name)
    }

    override fun setFilesInListLayout() {
        val firstVisibleItemPosition = (filesList.layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition()
        filesList.layoutManager = filesListLinearLayoutManager
        filesList.removeItemDecoration(filesListGridLayoutDecoration)
        filesList.adapter = filesListAdapter
        filesList.runOnLayoutChanged {
            setupToolBarScrollingBehavior(filesList.isScrollable)
            filesList.adapter = filesListAdapter
            if(firstVisibleItemPosition != null) filesList.layoutManager!!.scrollToPosition(firstVisibleItemPosition)
        }
    }

    override fun setFilesInGridLayout() {
        val firstVisibleItemPosition = (filesList.layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition()

        filesListGridLayoutManager.spanCount = calculateGridColumnsCount()
        filesList.layoutManager = filesListGridLayoutManager

        filesList.adapter = filesListAdapter
        filesList.runOnLayoutChanged {
            setupToolBarScrollingBehavior(filesList.isScrollable)
            filesList.addItemDecoration(filesListGridLayoutDecoration)
            filesList.adapter = filesListAdapter

            if(firstVisibleItemPosition != null) filesList.layoutManager!!.scrollToPosition(firstVisibleItemPosition)
        }
    }

    override fun setupToolBarScrollingBehavior(isEnabled: Boolean){
        if(activity != null){
            val toolBarLayoutParams = (activity as MainActivity).toolBar.layoutParams as AppBarLayout.LayoutParams
            toolBarLayoutParams.scrollFlags =  if(isEnabled){
                AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            }
            else{ 0 } //disable scrolling behavior
            (activity as MainActivity).appBarLayout.requestLayout()
        }
    }

    override fun restoreFilesListState(filesListSavedState: Parcelable?) {
        if(filesListSavedState != null) {
            filesList.layoutManager!!.onRestoreInstanceState(filesListSavedState)
        }
    }

    override fun showNoFilesMsg() {
        updateViewProgressBar.visible = false
        filesList.visible = false
        noFilesMessage.visible = true
        filesListRefresher.isEnabled = true
        setupToolBarScrollingBehavior(false)
    }

    override fun showProgressBar() {
        noFilesMessage.visible = false
        filesList.visible = false
        updateViewProgressBar.visible = true
    }

    override fun showFilesList() {
        noFilesMessage.visible = false
        updateViewProgressBar.visible = false
        filesList.visible = true
    }

    override fun update(animate: Boolean, resetListScrollPosition: Boolean) {

        if(resetListScrollPosition){
            filesList.scrollToPosition(0)
        }

        if(presenter.getFilesCount() == 0){
            showNoFilesMsg()
        }
        else{
            showFilesList()
            if(animate){
                val animationController = AnimationUtils.loadLayoutAnimation(context, R.anim.files_list_layout_animation)
                filesList.layoutAnimation = animationController
                filesList.scheduleLayoutAnimation()
            }
        }

        filesListAdapter.notifyDataSetChanged()

        filesList.runOnLayoutChanged { setupToolBarScrollingBehavior(filesList.isScrollable) }

        if(filesListRefresher.isRefreshing){
            App.get().handler.postDelayed({ filesListRefresher.isRefreshing = false }, 100)
        }

    }

    override fun showFileDetailsDialog(file: AbstractStorageFile) {
        FileDetailsDialog.createInstance(file).show(childFragmentManager, FileDetailsDialog::class.java.name)
    }

    override fun invalidateMenuOptions() {
        activity?.invalidateOptionsMenu()
    }

    override fun showCreateFolderDialog(parentFolder: AbstractStorageFile) {
        val createStorageFileDialog = CreateFolderDialog.createInstance(parentFolder)
        createStorageFileDialog.show(childFragmentManager, RenameStorageFileDialog::class.java.name)
    }

    override fun startActionMode(multiSelectMode: MultiSelectMode) {
        val mainActivity = activity!! as AppCompatActivity
        mainActivity.startSupportActionMode(multiSelectMode)
        setupToolBarScrollingBehavior(false)
    }

    override fun showSortTypeDialog(sortType: AbstractFilesNode.SortFilesType) {
        SortTypeDialog.createInstance(sortType).show(childFragmentManager, SortTypeDialog.DIALOG_KEY)
    }

    override fun showRenameFileDialog(file: AbstractStorageFile) {
        val renameDialog = RenameStorageFileDialog.createInstance(file)
        renameDialog.show(childFragmentManager, RenameStorageFileDialog::class.java.name)
    }

    override fun showFileActionsDialog(file: AbstractStorageFile) {
        FileActionsDialog.createInstance(file).show(childFragmentManager, FileActionsDialog::class.java.name)
    }

    override fun updateFilesListEntry(index: Int) = filesListAdapter.notifyItemChanged(index)

    override fun insertFilesListEntry(index: Int) {
        if(noFilesMessage.visible){
            showFilesList()
        }
        filesList.itemAnimator!!.endAnimations()
        filesListAdapter.notifyItemInserted(index)
        filesList.runOnLayoutChanged { setupToolBarScrollingBehavior(filesList.isScrollable) }
    }

    override fun removeFilesListEntry(index: Int) {
        filesList.itemAnimator!!.endAnimations()
        filesListAdapter.notifyItemRemoved(index)
        if(presenter.getFilesCount() == 0){
            showNoFilesMsg()
        }
        else{
            filesList.runOnLayoutChanged { setupToolBarScrollingBehavior(filesList.isScrollable) }
        }
    }
}