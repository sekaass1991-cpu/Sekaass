package com.owner.assistant.util

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import android.provider.Settings
import com.owner.assistant.call.PhoneAdminReceiver

/**
 * Centralizes the "is this special access actually granted" checks the
 * onboarding flow and MainActivity both need. Several of these can't be
 * requested via a normal runtime permission dialog — the owner has to flip
 * them on in Settings (see blueprint section 4).
 */
object PermissionUtils {

    fun hasSelfPermission(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?: return false
        return flat.contains(context.packageName)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, PhoneAdminReceiver::class.java)
        return dpm.isAdminActive(admin)
    }

    fun isVpnPrepared(context: Context): Boolean = VpnService.prepare(context) == null

    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            "android:get_usage_stats", android.os.Process.myUid(), context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun isNotificationPolicyAccessGranted(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.isNotificationPolicyAccessGranted
    }
}
