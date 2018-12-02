package com.progressifff.filemanager

import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class PermissionsRationaleFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.permission_rationale_fragment, null)
        val requestPermissionsBtn = view.findViewById<Button>(R.id.requestPermissionsBtn)
        requestPermissionsBtn.setOnClickListener {
            PermissionActivity.requestStoragePermissions(activity!!, Constants.FILE_SYSTEM_PERMISSION_REQUEST_CODE)
        }
        return view
    }
}