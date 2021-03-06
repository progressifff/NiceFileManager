package com.progressifff.nicefilemanager

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.content.pm.PackageManager
import android.support.v7.preference.PreferenceScreen
import android.support.v7.widget.Toolbar
import com.progressifff.nicefilemanager.Constants.RESULT_THEME_CHANGED_CODE
import com.progressifff.nicefilemanager.Constants.USE_DARK_THEME_KEY

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        if(AppPreferences.getBoolean(USE_DARK_THEME_KEY)){
            setTheme(R.style.AppThemeDark)
        }
        else{
            setTheme(R.style.AppThemeLight)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)

        toolbar.setNavigationOnClickListener{
            setActivityResult()
            finish()
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsContent, SettingsFragment())
                .commit()
    }

    override fun onBackPressed() {
        setActivityResult()
        super.onBackPressed()
    }

    private fun setActivityResult(){
        if(intent.getBooleanExtra(USE_DARK_THEME_KEY, false) != AppPreferences.getBoolean(USE_DARK_THEME_KEY)) {
            setResult(RESULT_THEME_CHANGED_CODE)
        }
        else{
            setResult(Activity.RESULT_OK)
        }
    }
}

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val useDarkThemePref = findPreference(USE_DARK_THEME_KEY)
        useDarkThemePref.setOnPreferenceChangeListener { _, _ ->
            activity!!.recreate()
            return@setOnPreferenceChangeListener true
        }

        val appVersionPref = findPreference("AppVersion")

        try {
            val packageInfo = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0)
            appVersionPref.summary = packageInfo.versionName

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun setPreferenceScreen(preferenceScreen: PreferenceScreen?) {
        super.setPreferenceScreen(preferenceScreen)
        if (preferenceScreen != null) {
            val count = preferenceScreen.preferenceCount
            for (i in 0 until count) {
                preferenceScreen.getPreference(i)!!.isIconSpaceReserved = false
            }
        }
    }
}