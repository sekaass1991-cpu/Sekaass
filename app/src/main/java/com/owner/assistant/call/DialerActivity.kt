package com.owner.assistant.call

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

/**
 * The minimum an app must provide to be eligible for [android.app.role.RoleManager.ROLE_DIALER]:
 * something that handles [android.content.Intent.ACTION_DIAL] /
 * `tel:` links. Deliberately bare-bones — this app's point is voice control
 * of calls already in progress ([InCallActivity]), not replacing a full
 * contacts-integrated dialer.
 */
class DialerActivity : AppCompatActivity() {

    private lateinit var numberInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }

        root.addView(TextView(this).apply {
            text = "Dial a number"
            textSize = 20f
            setPadding(0, 0, 0, 32)
        })

        numberInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            hint = "Phone number"
            setText(intent?.data?.schemeSpecificPart ?: "")
        }
        root.addView(numberInput)

        root.addView(Button(this).apply {
            text = "Call"
            setOnClickListener { placeCall() }
        })

        setContentView(root)
    }

    private fun placeCall() {
        val number = numberInput.text.toString().trim()
        if (number.isBlank()) return

        if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PERMISSION_REQUEST_CODE)
            return
        }

        val telecomManager = getSystemService(TelecomManager::class.java)
        telecomManager.placeCall(Uri.fromParts("tel", number, null), null)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PERMISSION_REQUEST_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            placeCall()
        }
    }

    companion object {
        private const val CALL_PERMISSION_REQUEST_CODE = 3001
    }
}
