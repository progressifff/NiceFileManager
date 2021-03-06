package com.progressifff.nicefilemanager

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import com.progressifff.nicefilemanager.Constants.RESULT_FOLDER_KEY
import com.progressifff.nicefilemanager.Constants.RESULT_SEARCHED_FOLDER_OPEN_CODE
import com.progressifff.nicefilemanager.Constants.SEARCH_QUERY_KEY
import com.progressifff.nicefilemanager.Constants.SEARCH_ROOT_FOLDER_KEY
import com.progressifff.nicefilemanager.Constants.USE_DARK_THEME_KEY
import com.progressifff.nicefilemanager.dialogs.DeleteFilesDialog
import com.progressifff.nicefilemanager.dialogs.FileActionsDialog
import com.progressifff.nicefilemanager.dialogs.FileDetailsDialog
import com.progressifff.nicefilemanager.dialogs.RenameStorageFileDialog
import com.progressifff.nicefilemanager.presenters.MainPresenter
import com.progressifff.nicefilemanager.presenters.SearchedFilesPresenter
import com.progressifff.nicefilemanager.views.SearchedFilesView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers

class SearchableActivity : AppCompatActivity(), SearchedFilesView {
    private lateinit var noFilesMessage: LinearLayout
    private lateinit var searchFilesProgressBar: ProgressBar
    private lateinit var searchView: com.progressifff.materialsearchview.SearchView
    private lateinit var searchedFilesList: RecyclerView
    private lateinit var searchedFilesListAdapter: RecyclerView.Adapter<*>
    private lateinit var presenter: SearchedFilesPresenter
    private val filesTaskView = FilesTasks(R.id.searchedFilesTasks)
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
        if(preferences.getBoolean(USE_DARK_THEME_KEY, false)) setTheme(R.style.AppThemeDark) else setTheme(R.style.AppThemeLight)
        setContentView(R.layout.searchable_activity)
        RxBus.clearHistory()
        //Find noFilesMessage
        noFilesMessage = findViewById(R.id.noFilesFoundView)
        searchFilesProgressBar = findViewById(R.id.searchFilesProgressBar)
        searchView = findViewById(R.id.searchView)
        searchView.searchViewListener = searchViewListener

        val folder = intent.getParcelableExtra<AbstractStorageFile>(SEARCH_ROOT_FOLDER_KEY)
        presenter = if(savedInstanceState == null){
            query = intent.getStringExtra(SEARCH_QUERY_KEY)
            searchView.setQueryText(query!!)
            //Save search suggestions
            saveSuggestion(query!!)
            updateSuggestions()
            //Create presenter
            SearchedFilesPresenter(folder, RxBus, FilesClipboard, FileImageManager)
        }
        else try{
            PresenterManager.restorePresenter<SearchedFilesPresenter>(savedInstanceState)
        }
        catch (e: Exception){
            e.printStackTrace()
            SearchedFilesPresenter(folder, RxBus, FilesClipboard, FileImageManager)
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
        PresenterManager.savePresenter(presenter, outState)
        filesTaskView.onSaveInstance(outState)
    }

    private fun setupSearchResultList() {
        searchedFilesListAdapter = FilesListAdapter(presenter)
        searchedFilesList = findViewById<RecyclerView>(R.id.searchResultList).apply{
            setHasFixedSize(true)
            adapter = searchedFilesListAdapter
        }

        val filesDisplayMode = MainPresenter.FilesDisplayMode.fromString(AppPreferences.getString(Constants.FILES_DISPLAY_MODE_KEY, MainPresenter.FilesDisplayMode.LIST.name))

        when(filesDisplayMode){
            MainPresenter.FilesDisplayMode.GRID -> {
                searchedFilesList.layoutManager = GridLayoutManager(this, Utils.calculateGridColumnsCount())
                searchedFilesList.addItemDecoration(RecyclerViewGridLayoutDecoration())
            }
            MainPresenter.FilesDisplayMode.LIST -> searchedFilesList.layoutManager = LinearLayoutManager(this)
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

    override fun updateFilesList() {
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
        else {
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

    override fun openFolder(folder: AbstractStorageFile) {
        val resultIntent = Intent()
        resultIntent.putExtra(RESULT_FOLDER_KEY, folder)
        this.setResult(RESULT_SEARCHED_FOLDER_OPEN_CODE, resultIntent)
        finish()
    }

    override fun showFileDetailsDialog(file: AbstractStorageFile) {
        FileDetailsDialog.createInstance(file).show(supportFragmentManager, FileDetailsDialog::class.java.name)
    }

    override fun startActionMode(multiSelectMode: MultiSelectMode) {
        startSupportActionMode(multiSelectMode)
    }

    override fun showRenameFileDialog(file: AbstractStorageFile) {
        RenameStorageFileDialog.createInstance(file).show(supportFragmentManager, RenameStorageFileDialog::class.java.name)
    }

    override fun showShareDialog(file: AbstractStorageFile) {
        Utils.showShareFileDialog(this, file)
    }

    override fun showOpenFileDialog(file: AbstractStorageFile) {
        Utils.showOpenFileDialog(this, file)
    }

    override fun showToast(messageId: Int) {
        Toast.makeText(this, getString(messageId), Toast.LENGTH_SHORT).show()
    }

    override fun showDeleteFilesDialog(filesCount: Int) {
        DeleteFilesDialog.createInstance(filesCount).show(supportFragmentManager, DeleteFilesDialog::class.java.name)
    }
}
