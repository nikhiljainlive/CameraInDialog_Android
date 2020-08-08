package com.nikhiljain.fundoonotes.cameraexample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.activity_main.*

private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setClickToShowDialogButton()
    }

    override fun onResume() {
        super.onResume()
        if (!hasPermissions(this)) {
            // Request camera-related permissions
            button_show_dialog.isEnabled = false
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        } else {
            // If permissions have already been granted, proceed
            button_show_dialog.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
                button_show_dialog.isEnabled = true
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
                button_show_dialog.isEnabled = false
            }
        }
    }

    private fun setClickToShowDialogButton() {
        button_show_dialog.setOnClickListener {
            val authDialogFragment: DialogFragment = AuthCameraDialogFragment()
            authDialogFragment.show(
                supportFragmentManager,
                AuthCameraDialogFragment.TAG
            )
        }
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}