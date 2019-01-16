package com.progressifff.nicefilemanager

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity

class PermissionActivity : AppCompatActivity() {
    private lateinit var permissionsRationaleFragment: PermissionsRationaleFragment
    private lateinit var permissionsInstructionFragment: PermissionsInstructionFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        if(AppPreferences.getBoolean(Constants.USE_DARK_THEME_KEY, false)){
            setTheme(R.style.AppThemeDark)
        }
        else{
            setTheme(R.style.AppThemeLight)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        setSupportActionBar(findViewById(R.id.toolbar))
        permissionsRationaleFragment = PermissionsRationaleFragment()
        permissionsInstructionFragment = PermissionsInstructionFragment()

        if (!checkRequestRationale()) {
            requestStoragePermissions(this, Constants.FILE_SYSTEM_PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkRequestRationale(): Boolean{
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.permissionFragmentContainer, permissionsRationaleFragment)
            fragmentTransaction.commit()
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {

            Constants.FILE_SYSTEM_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val mainActivityIntent = Intent(this, MainActivity::class.java)
                    startActivity(mainActivityIntent)
                    finish()

                } else {
                    if (!checkRequestRationale()) {
                        val fragmentTransaction = supportFragmentManager.beginTransaction()
                        fragmentTransaction.replace(R.id.permissionFragmentContainer, permissionsInstructionFragment)
                        fragmentTransaction.commit()
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object{
        fun requestStoragePermissions(activity: Activity, permissionRequestCode: Int) {
            ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    permissionRequestCode)
        }
    }
}