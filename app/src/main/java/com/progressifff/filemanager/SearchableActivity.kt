package com.progressifff.filemanager

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.progressifff.filemanager.Constants.RESULT_SEARCHED_FOLDER_OPEN
import com.progressifff.filemanager.Constants.USE_DARK_THEME_KEY
import com.progressifff.filemanager.dialogs.DeleteFilesDialog
import com.progressifff.filemanager.dialogs.FileActionsDialog
import com.progressifff.filemanager.dialogs.FileDetailsDialog
import com.progressifff.filemanager.dialogs.RenameStorageFileDialog
import com.progressifff.filemanager.models.AbstractStorageFile
import com.progressifff.filemanager.presenters.MainPresenter
import com.progressifff.filemanager.presenters.SearchedFilesPresenter
import com.progressifff.filemanager.views.SearchedFilesView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers

class SearchableActivity : AppCompatActivity(), SearchedFilesView {
    override fun showDeleteFilesDialog(filesCount: Int) {
        val dialog = DeleteFilesDialog.createInstance(filesCount)
        dialog.show(supportFragmentManager, DeleteFilesDialog::class.java.name)
    }

    private lateinit var noFilesMessage: LinearLayout
    private lateinit var searchFilesProgressBar: ProgressBar
    private lateinit var searchView: com.progressifff.materialsearchview.SearchView
    private lateinit var searchedFilesList: RecyclerView
    private lateinit var searchedFilesListAdapter: RecyclerView.Adapter<*>
    private lateinit var presenter: SearchedFilesPresenter
    private val filesTaskView = FilesTaskView(R.id.searchedFilesTasks)
    private var query: String? = null

    private val searchViewListener = object : com.progressifff.materialsearchview.SearchView.SearchViewListener{
        override fun onBackButtonPressed() {
            RxBus.clearHistory()
            finish()
        }
        override fun onVisibilityChanged(visible: Boolean) { }
        override fun onQueryTextChange(query: String) { }

        override fun onSuggestionsVisibilityChanged(visible: Boolean) {
            if(visible){
               updateSuggestions()
            }
        }

        override fun onQueryTextSubmit(query: String): Boolean {
            if(query.isEmpty()) {
                return false
            }
            presenter.searchFiles(query)
            saveSuggestion(query)
            return true
        }

        override fun onSuggestionSelected(suggestion: String) { presenter.searchFiles(suggestion) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        if(preferences.getBoolean(USE_DARK_THEME_KEY, false)){
            setTheme(R.style.AppThemeDark)
        }
        else{
            setTheme(R.style.AppThemeLight)
        }

        RxBus.clearHistory()

        setContentView(R.layout.searchable_activity)

        //Find noFilesMessage
        noFilesMessage = findViewById(R.id.noFilesFoundTextView)

        searchFilesProgressBar = findViewById(R.id.searchFilesProgressBar)
        searchView = findViewById(R.id.searchView)
        searchView.searchViewListener = searchViewListener

        val folder = intent.getParcelableExtra<AbstractStorageFile>(SEARCH_ROOT_FOLDER_KEY)

        presenter = if(savedInstanceState == null){
            query = intent.getStringExtra(SEARCH_QUERY)

            searchView.setQueryText(query!!)
            //Save search suggestions
            saveSuggestion(query!!)

            updateSuggestions()

            //Create presenter
            SearchedFilesPresenter(folder)
        }
        else try{
            PresenterManager.instance.restorePresenter<SearchedFilesPresenter>(savedInstanceState)
        }
        catch (e: Exception){
            e.printStackTrace()
            SearchedFilesPresenter(folder)
        }

        setupSearchResultList()

        filesTaskView.onActivityCreate(this, savedInstanceState)

    }

    override fun onStart() {
        super.onStart()
        filesTaskView.onActivityStart()
        presenter.bindView(this)

        if(query != null){
            presenter.searchFiles(query!!)
            query = null
        }
    }

    override fun onStop() {
        super.onStop()
        filesTaskView.onActivityStop()
        presenter.unbindView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        PresenterManager.instance.savePresenter(presenter, outState)
        filesTaskView.onSaveInstance(outState)
    }

    private fun setupSearchResultList() {
        searchedFilesListAdapter = FilesListAdapter(presenter)
        searchedFilesList = findViewById<RecyclerView>(R.id.searchResultList).apply{
            setHasFixedSize(true)
            adapter = searchedFilesListAdapter
        }

        val filesDisplayMode = MainPresenter.FilesDisplayMode.fromString(getStringFromSharedPreferences(Constants.FILES_DISPLAY_MODE_KEY, MainPresenter.FilesDisplayMode.LIST.name))

        when(filesDisplayMode){
            MainPresenter.FilesDisplayMode.GRID -> {
                val gridLayoutManager = GridLayoutManager(this, 1)
                searchedFilesList.layoutManager = gridLayoutManager
                searchedFilesList.addItemDecoration(RecyclerViewGridLayoutDecoration())
                searchedFilesList.runOnLayoutChanged {
                    gridLayoutManager.spanCount = calculateGridColumnsCount()
                    searchedFilesListAdapter.notifyDataSetChanged()
                }
            }
            MainPresenter.FilesDisplayMode.LIST -> {
                searchedFilesList.layoutManager = LinearLayoutManager(this)
            }
        }
    }

    override fun showFileActionsDialog(file: AbstractStorageFile) {
        FileActionsDialog.createInstance(file).show(supportFragmentManager, FileActionsDialog::class.java.name)
    }

    private fun saveSuggestion(suggestion: String){
        val saveSuggestionThread = Thread {
            val suggestionDao = SearchSuggestions.instance.suggestionDao()
            suggestionDao.insert(Suggestion(suggestion))
            suggestionDao.truncateHistory()
        }
        saveSuggestionThread.start()
        saveSuggestionThread.join()
    }

    private fun updateSuggestions(){
        val suggestionDao = SearchSuggestions.instance.suggestionDao()
        suggestionDao.getAll().subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(object: DisposableSingleObserver<List<String>>(){
                    override fun onSuccess(suggestions: List<String>) {
                        searchView.suggestionsData = ArrayList(suggestions)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }
                })
    }

    override fun resetFilesList() {
        searchedFilesListAdapter.notifyDataSetChanged()
    }

    override fun updateFilesListEntry(index: Int) {
        searchedFilesListAdapter.notifyItemChanged(index)
    }

    override fun insertFilesListEntry(index: Int) {
        searchedFilesListAdapter.notifyItemInserted(index)
    }

    override fun removeFilesListEntry(index: Int) {
        searchedFilesList.itemAnimator!!.endAnimations()
        searchedFilesListAdapter.notifyItemRemoved(index)
        if(presenter.getFilesCount() == 0){
            showNoFilesMsg()
        }
    }

    override fun update(animate: Boolean, resetListScrollPosition: Boolean) {
        if(presenter.getFilesCount() == 0){
            showNoFilesMsg()
        }
        else if(!searchedFilesList.visible){
            showFilesList()
        }
        searchedFilesListAdapter.notifyDataSetChanged()
    }

    override fun showFilesList(){
        if(!searchedFilesList.visible) {
            noFilesMessage.visible = false
            searchedFilesList.visible = true
        }
    }

    override fun showNoFilesMsg() {
        if(!noFilesMessage.visible){
            noFilesMessage.visible = true
            searchedFilesList.visible = false
        }
    }

    override fun showProgressBar() {
        if(!searchFilesProgressBar.visible) {
            searchFilesProgressBar.visible = true
        }
    }

    override fun hideProgressBar() {
        if(searchFilesProgressBar.visible){
            searchFilesProgressBar.visible = false
        }
    }

    override fun onBackPressed() {
        if(!searchView.onBackPressed()){
            RxBus.clearHistory()
            super.onBackPressed()
        }
    }

    override fun postResult(folder: AbstractStorageFile) {
        val resultIntent = Intent()
        resultIntent.putExtra(RESULT_FOLDER, folder)
        this.setResult(RESULT_SEARCHED_FOLDER_OPEN, resultIntent)
        finish()
    }

    override fun showFileDetailsDialog(file: AbstractStorageFile) {
        FileDetailsDialog.createInstance(file).show(supportFragmentManager, FileDetailsDialog::class.java.name)
    }

    override fun startActionMode(multiSelectMode: MultiSelectMode) {
        startSupportActionMode(multiSelectMode)
    }

    override fun showRenameFileDialog(file: AbstractStorageFile) {
        val renameDialog = RenameStorageFileDialog.createInstance(file)
        renameDialog.show(supportFragmentManager, RenameStorageFileDialog::class.java.name)
    }

    companion object {
        const val SEARCH_ROOT_FOLDER_KEY = "SearchRootFolder"
        const val SEARCH_QUERY = "SearchQuery"
        const val RESULT_FOLDER = "ResultFolder"
    }
}
