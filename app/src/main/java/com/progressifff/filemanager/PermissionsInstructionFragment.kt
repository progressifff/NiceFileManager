package com.progressifff.filemanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class PermissionsInstructionFragment : Fragment(){

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.permission_instruction_fragment, null)
        val requestPermissionsBtn = view.findViewById<Button>(R.id.goToSettingsBtn)
        requestPermissionsBtn.setOnClickListener {v: View ->
            val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + v.context.packageName))
            startActivityForResult(appSettingsIntent, Constants.SETTINGS_STORAGE_PERMISSION_REQUEST_CODE)
        }
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == Constants.SETTINGS_STORAGE_PERMISSION_REQUEST_CODE &&
                (ContextCompat.checkSelfPermission(activity as Context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)){
            val mainActivityIntent = Intent(activity, MainActivity::class.java)
            startActivity(mainActivityIntent)
            activity!!.finish()
        }
    }
}