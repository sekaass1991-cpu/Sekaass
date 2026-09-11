package com.owner.assistant.onboarding

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.owner.assistant.call.PhoneAdminReceiver
import com.owner.assistant.data.AssistantPrefs
import com.owner.assistant.data.OwnerProfileStore
import com.owner.assistant.security.SecurityWatchdogWorker
import com.owner.assistant.service.BatteryDataMonitorWorker
import com.owner.assistant.service.DndScheduler
import com.owner.assistant.util.PermissionUtils
import com.owner.assistant.voice.VoiceEnrollment
import kotlin.concurrent.thread

/**
 * Walks the owner through every piece of setup the blueprint calls out as
 * "can't be auto-granted" (section 4): runtime permissions, notification
 * listener access, device admin, DND policy access, usage access, the VPN
 * one-time consent dialog, the call-screening role, and — the one step no
 * Settings screen can do for us — recording the owner's voice profile.
 */
class OnboardingActivity : AppCompatActivity() {

    private var step = 0
    private val container by lazy { LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) } }

    private val runtimePermissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.ANSWER_PHONE_CALLS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.READ_CALENDAR
    ).let { base ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) base + android.Manifest.permission.POST_NOTIFICATIONS else base
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        renderStep()
    }
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        renderStep()
    }
    private val vpnConsentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        renderStep()
    }
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        renderStep()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(container)
        renderStep()
    }

    private fun renderStep() {
        container.removeAllViews()
        when (step) {
            0 -> stepRuntimePermissions()
            1 -> stepNotificationListener()
            2 -> stepDeviceAdmin()
            3 -> stepDndAccess()
            4 -> stepUsageAccess()
            5 -> stepVpnConsent()
            6 -> stepCallScreeningRole()
            7 -> stepVoiceEnrollment()
            8 -> stepEmergencyContact()
            else -> stepFinished()
        }
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setPadding(0, 0, 0, 24)
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 15f
        setPadding(0, 0, 0, 32)
    }

    private fun continueButton(label: String = "Continue", action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun skipButton() = Button(this).apply {
        text = "Skip for now"
        setOnClickListener { step++; renderStep() }
    }

    // ---- Steps ----

    private fun stepRuntimePermissions() {
        container.addView(title("1. Core permissions"))
        container.addView(body("Personal Assistant needs microphone, camera, location, phone, SMS, contacts, and calendar access to work. You'll be asked once."))
        container.addView(continueButton("Grant permissions") {
            permissionLauncher.launch(runtimePermissions)
            step++
        })
    }

    private fun stepNotificationListener() {
        container.addView(title("2. Read your notifications"))
        container.addView(body("So the assistant can tell you \"what messages did I get?\", enable it under Notification access. You'll see this app in the list — turn it on, then come back here."))
        container.addView(continueButton("Open notification access settings") {
            settingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        container.addView(nextOrSkipRow(PermissionUtils.isNotificationListenerEnabled(this)))
    }

    private fun stepDeviceAdmin() {
        container.addView(title("3. Lock your phone on command"))
        container.addView(body("To let \"lock my phone\" actually lock the screen, activate device admin for this app."))
        container.addView(continueButton("Activate device admin") {
            val admin = ComponentName(this, PhoneAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Lets Personal Assistant lock your screen when you ask.")
            }
            settingsLauncher.launch(intent)
        })
        container.addView(nextOrSkipRow(PermissionUtils.isDeviceAdminActive(this)))
    }

    private fun stepDndAccess() {
        container.addView(title("4. Scheduled Do Not Disturb"))
        container.addView(body("To auto-enable Do Not Disturb on a sleep schedule or during calendar events, grant Do Not Disturb access."))
        container.addView(continueButton("Open Do Not Disturb settings") {
            settingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        })
        container.addView(nextOrSkipRow(PermissionUtils.isNotificationPolicyAccessGranted(this)))
    }

    private fun stepUsageAccess() {
        container.addView(title("5. App usage access"))
        container.addView(body("For the battery/data usage summary and the security watchdog's app scan, grant Usage access."))
        container.addView(continueButton("Open usage access settings") {
            settingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        container.addView(nextOrSkipRow(PermissionUtils.isUsageAccessGranted(this)))
    }

    private fun stepVpnConsent() {
        container.addView(title("6. Ad blocking"))
        container.addView(body("Ad blocking runs a local, on-device VPN that only filters ad domains — no traffic ever leaves your phone through it. Android requires a one-time consent dialog."))
        container.addView(continueButton("Approve local VPN") {
            val intent = VpnService.prepare(this)
            if (intent != null) vpnConsentLauncher.launch(intent) else renderStep()
        })
        container.addView(nextOrSkipRow(PermissionUtils.isVpnPrepared(this)))
    }

    private fun stepCallScreeningRole() {
        container.addView(title("7. Call control"))
        container.addView(body("To answer/decline/mute calls by voice, make Personal Assistant your call-screening app. This does NOT replace your regular Phone app."))
        container.addView(continueButton("Set up call control") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(android.app.role.RoleManager::class.java)
                if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                    roleLauncher.launch(roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING))
                    return@continueButton
                }
            }
            Toast.makeText(this, "Not supported on this Android version — skipping.", Toast.LENGTH_SHORT).show()
            step++; renderStep()
        })
        container.addView(skipButton())
    }

    private fun stepVoiceEnrollment() {
        container.addView(title("8. Teach it your voice"))
        val enrollment = VoiceEnrollment(this)
        val required = VoiceEnrollment.REQUIRED_SAMPLES
        val recorded = enrollment.samplesRecorded()

        container.addView(body(
            "This is the most important step: only YOUR voice will ever trigger the assistant. " +
                "Say your wake phrase (\"hey assistant\") naturally, ${required - recorded} more time(s)."
        ))
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = required; setProgress(recorded)
        }
        container.addView(progress)
        val status = TextView(this).apply { text = "$recorded of $required samples recorded"; gravity = Gravity.CENTER }
        container.addView(status)

        val recordButton = Button(this).apply { text = "Record a sample" }
        recordButton.setOnClickListener {
            recordButton.isEnabled = false
            status.text = "Listening... speak now"
            thread {
                enrollment.recordOneSample()
                runOnUiThread {
                    if (enrollment.isComplete()) {
                        Toast.makeText(this, "Voice profile complete!", Toast.LENGTH_SHORT).show()
                        step++
                        renderStep()
                    } else {
                        recordButton.isEnabled = true
                        renderStep()
                    }
                }
            }
        }
        container.addView(recordButton)

        if (enrollment.isComplete()) {
            container.addView(continueButton { step++; renderStep() })
        }
    }

    private fun stepEmergencyContact() {
        container.addView(title("9. Emergency contact"))
        container.addView(body("If you ever say \"I'm in danger\", this number gets your live location by SMS."))
        val input = EditText(this).apply {
            hint = "Phone number, e.g. +15551234567"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(OwnerProfileStore.getEmergencyContact(this@OnboardingActivity) ?: "")
        }
        container.addView(input)
        container.addView(continueButton("Save and continue") {
            val number = input.text.toString().trim()
            if (number.isNotBlank()) OwnerProfileStore.setEmergencyContact(this, number)
            step++
            renderStep()
        })
    }

    private fun stepFinished() {
        AssistantPrefs.setSleepWindow(this, 23, 0, 7, 0)
        DndScheduler(this).scheduleSleepWindow(23, 0, 7, 0)
        SecurityWatchdogWorker.schedule(this)
        BatteryDataMonitorWorker.schedule(this)

        container.addView(title("All set"))
        container.addView(body(
            "Setup is complete. The assistant now only responds to your voice, DND is scheduled " +
                "for 11 PM - 7 AM by default (change this later from the app), and the daily security " +
                "and usage checks are scheduled."
        ))
        container.addView(continueButton("Done") { finish() })
    }

    private fun nextOrSkipRow(alreadyGranted: Boolean): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        if (alreadyGranted) {
            row.addView(TextView(this).apply { text = "Enabled ✓" })
            row.addView(continueButton("Next") { step++; renderStep() })
        } else {
            row.addView(skipButton())
        }
        return row
    }
}
