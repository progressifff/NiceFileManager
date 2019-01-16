package com.progressifff.nicefilemanager

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.constraint.ConstraintLayout
import android.support.design.widget.*
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import com.progressifff.nicefilemanager.Constants.RESULT_SEARCHED_FOLDER_OPEN
import com.progressifff.nicefilemanager.Constants.SEARCH_ACTIVITY_REQUEST_CODE
import com.progressifff.nicefilemanager.Constants.SETTINGS_ACTIVITY_REQUEST_CODE
import com.progressifff.nicefilemanager.Constants.RESULT_THEME_CHANGED
import com.progressifff.nicefilemanager.Constants.USE_DARK_THEME_KEY
import com.progressifff.nicefilemanager.presenters.MainPresenter
import com.progressifff.nicefilemanager.views.MainView
import com.progressifff.materialsearchview.SearchView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import android.support.design.widget.Snackbar
import android.widget.TextView

class MainActivity : AppCompatActivity(), MainView {

    private lateinit var presenter: MainPresenter
    lateinit var toolBar: Toolbar
    lateinit var appBarLayout: AppBarLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var navigationBar: ConstraintLayout
    private lateinit var filesDisplayModeButton: ImageButton
    private lateinit var filesOrderModeButton: ImageButton
    private lateinit var navigationBarList: RecyclerView
    private lateinit var navigationBarListAdapter: RecyclerView.Adapter<*>
    private lateinit var navigationBarListLayoutManager: RecyclerView.LayoutManager
    private lateinit var searchView: SearchView
    private val filesTaskView = FilesTasks(R.id.filesTasks)
    private lateinit var quitAppBar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        if(AppPreferences.getBoolean(USE_DARK_THEME_KEY, false)){
            setTheme(R.style.AppThemeDark)
        }
        else{
            setTheme(R.style.AppThemeLight)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = Color.TRANSPARENT

        //Create presenter
        if(savedInstanceState == null){
            presenter = MainPresenter(AppPreferences, RxBus, EnvironmentStandardPaths)
            setupFilesFragment()
        }
        else{
            presenter = try{
                PresenterManager.restorePresenter(savedInstanceState)
            } catch (e: Exception){
                e.printStackTrace()
                MainPresenter(AppPreferences, RxBus, EnvironmentStandardPaths)
            }
        }

        //Setup tool bar
        appBarLayout = findViewById(R.id.appBarLayout)
        toolBar = findViewById(R.id.mainActivityToolBar)
        setSupportActionBar(toolBar)

        //Setup search view
        searchView = findViewById(R.id.searchView)
        setSearchSuggestions()

        //Setup navigation drawerToggle
        setupNavigationDrawer()

        //Setup navigation bar
        setupNavigationBar()

        filesTaskView.onActivityCreate(this, savedInstanceState)
    }

    private fun setupNavigationDrawer(){
        drawerLayout = findViewById(R.id.main_activity_drawer_layout)
        val drawerToggle = ActionBarDrawerToggle(
                this, drawerLayout, toolBar,
                R.string.main_activity_nav_drawer_open,
                R.string.main_activity_nav_drawer_close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerLayout.addDrawerListener(presenter.navigationDrawerListener)
        drawerToggle.syncState()
        navigationView = findViewById(R.id.main_activity_nav_view)
        navigationView.setNavigationItemSelectedListener {menuItem ->
            return@setNavigationItemSelectedListener presenter.onNavigationDrawerItemSelected(navigationView.menu, menuItem)
        }
    }

    private fun setupNavigationBar(){
        navigationBarListLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        navigationBarListAdapter = NavigationEntriesAdapter(presenter)
        navigationBar = findViewById(R.id.navigationBar)
        navigationBarList = findViewById(R.id.navigationBarList)
        navigationBarList.apply {
            adapter = navigationBarListAdapter
            layoutManager = navigationBarListLayoutManager
            setHasFixedSize(true)
        }
        filesDisplayModeButton = findViewById(R.id.filesDisplayModeBtn)
        filesDisplayModeButton.setOnClickListener(presenter.viewOnClickListener)
        filesOrderModeButton = findViewById(R.id.filesOrderModeBtn)
        filesOrderModeButton.setOnClickListener(presenter.viewOnClickListener)
    }

    override fun onStart() {
        super.onStart()
        presenter.bindView(this)
        filesTaskView.onActivityStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.unbindView()
        filesTaskView.onActivityStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        filesTaskView.onSaveInstance(outState)
        PresenterManager.savePresenter(presenter, outState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        val searchMenuItem = menu!!.findItem(R.id.action_search)
        searchView.setMenuItem(searchMenuItem)
        searchView.searchMenuItemIndex = 1
        searchView.searchViewListener = object : SearchView.SearchViewListener{

            override fun onVisibilityChanged(visible: Boolean) {
                if(visible){
                    setSearchSuggestions()
                }
            }

            override fun onQueryTextChange(query: String) {}

            override fun onQueryTextSubmit(query: String): Boolean {
                if(query.isNotEmpty()) {
                    startSearchableActivity(query)
                }
                return query.isNotEmpty()
            }

            override fun onSuggestionSelected(suggestion: String) {
                startSearchableActivity(suggestion)
            }
        }
        return true
    }

    private fun setSearchSuggestions(){
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

    private fun startSearchableActivity(query: String){
        val startActivityIntent = Intent(this@MainActivity, SearchableActivity::class.java)
        startActivityIntent.putExtra(SearchableActivity.SEARCH_ROOT_FOLDER_KEY, presenter.getCurrentOpenedFolder())
        startActivityIntent.putExtra(SearchableActivity.SEARCH_QUERY, query)
        startActivityForResult(startActivityIntent, SEARCH_ACTIVITY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            SEARCH_ACTIVITY_REQUEST_CODE -> {
                if(resultCode == RESULT_SEARCHED_FOLDER_OPEN){
                    presenter.onOpenSearchedFolder(data?.getParcelableExtra(SearchableActivity.RESULT_FOLDER)!!)
                }
            }
            SETTINGS_ACTIVITY_REQUEST_CODE ->{
                if(resultCode == RESULT_THEME_CHANGED){
                    recreate()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            R.id.action_search -> {
                super.onSearchRequested()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        if(!searchView.onBackPressed() && !presenter.onBackPressed()) {
            val finalize: () -> Unit = {
                filesTaskView.release()
                super.onBackPressed()
            }
            if(!::quitAppBar.isInitialized){
                quitAppBar = Snackbar.make(findViewById(R.id.mainActivityContent), getString(R.string.quit_application_msg), Snackbar.LENGTH_SHORT)
                quitAppBar.view.findViewById<TextView>(android.support.design.R.id.snackbar_text).setTextColor(Color.WHITE)
                quitAppBar.setAction(getString(android.R.string.yes)) { finalize() }
            }
            if(!quitAppBar.isShown){
                quitAppBar.show()
            }
            else{
                finalize()
            }
        }
    }

    override fun setToolBarTitle(@StringRes title: Int) {
        supportActionBar!!.title = getString(title)
    }

    override fun updateNavigationBar() {
        navigationBarListAdapter.notifyDataSetChanged()
        navigationBarList.smoothScrollToPosition(navigationBarListAdapter.itemCount - 1)
    }

    override fun insertNavigationBarItemsRange(index: Int, count: Int) {
        navigationBarListAdapter.notifyItemRangeInserted(index, count)
        navigationBarList.smoothScrollToPosition(navigationBarListAdapter.itemCount - 1)
    }

    override fun removeNavigationBarItemsRange(index: Int, count: Int) {
        navigationBarListAdapter.notifyItemRangeRemoved(index, count)
        navigationBarList.smoothScrollToPosition(navigationBarListAdapter.itemCount - 1)
    }

    override fun setCheckedDrawerMenuItem(menuItemId: Int) {
        navigationView.setCheckedItem(menuItemId)
        navigationView.menu.findItem(menuItemId).isChecked = true
    }

    override fun setFilesDisplayModeButton(filesDisplayMode: MainPresenter.FilesDisplayMode) {
        when(filesDisplayMode){
            MainPresenter.FilesDisplayMode.LIST -> {filesDisplayModeButton.setImageResource(R.drawable.ic_list)}
            MainPresenter.FilesDisplayMode.GRID -> {filesDisplayModeButton.setImageResource(R.drawable.ic_grid_on)}
        }
    }

    override fun setFilesOrderModeButton(filesOrderMode: MainPresenter.FilesOrderMode) {
        when(filesOrderMode){
            MainPresenter.FilesOrderMode.DESCENDING -> {filesOrderModeButton.setImageResource(R.drawable.ic_arrow_downward)}
            MainPresenter.FilesOrderMode.ASCENDING -> {filesOrderModeButton.setImageResource(R.drawable.ic_arrow_upward)}
        }
    }

    private fun setupFilesFragment() {
        val fragment = FilesFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.contentContainer, fragment, fragment.hashCode().toString())
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        fragmentTransaction.commit()
    }

    override fun openSettings() {
        val startSettingsIntent = Intent(this, SettingsActivity::class.java)
        startSettingsIntent.putExtra(USE_DARK_THEME_KEY, AppPreferences.getBoolean(USE_DARK_THEME_KEY))
        startActivityForResult(startSettingsIntent, SETTINGS_ACTIVITY_REQUEST_CODE)
    }

    override fun closeNavigationDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun expandAppBar(){
        appBarLayout.setExpanded(true, true)
    }
}
