package com.owner.assistant.security

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the security watchdog's permission heuristics —
 * exercises the rule set directly against synthetic app profiles, no
 * PackageManager or device needed.
 */
class PermissionHeuristicsTest {

    private fun profile(
        packageName: String = "com.example.app",
        isSystemApp: Boolean = false,
        vararg permissions: String
    ) = AppPermissionProfile(packageName, "Example App", isSystemApp, permissions.toSet())

    @Test
    fun `benign app with only internet access is not flagged`() {
        val result = PermissionHeuristics.evaluate(profile(permissions = arrayOf("android.permission.INTERNET")))
        assertNull(result)
    }

    @Test
    fun `system apps are never flagged regardless of permissions`() {
        val result = PermissionHeuristics.evaluate(
            profile(isSystemApp = true, permissions = arrayOf("android.permission.SEND_SMS", "android.permission.SYSTEM_ALERT_WINDOW"))
        )
        assertNull(result)
    }

    @Test
    fun `an app that can send SMS is flagged`() {
        val result = PermissionHeuristics.evaluate(profile(permissions = arrayOf("android.permission.SEND_SMS")))
        assertTrue(result != null)
        assertTrue(result!!.reasons.any { it.contains("send text messages", ignoreCase = true) })
    }

    @Test
    fun `camera plus microphone plus location together is flagged`() {
        val result = PermissionHeuristics.evaluate(
            profile(
                permissions = arrayOf(
                    "android.permission.CAMERA",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.ACCESS_FINE_LOCATION"
                )
            )
        )
        assertTrue(result != null)
        assertTrue(result!!.reasons.any { it.contains("camera, microphone", ignoreCase = true) })
    }

    @Test
    fun `camera alone without the other two is not flagged by that rule`() {
        val result = PermissionHeuristics.evaluate(profile(permissions = arrayOf("android.permission.CAMERA")))
        assertNull(result)
    }

    @Test
    fun `six or more dangerous permissions trips the high-count rule`() {
        val result = PermissionHeuristics.evaluate(
            profile(
                permissions = arrayOf(
                    "android.permission.CAMERA",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.READ_CONTACTS",
                    "android.permission.READ_SMS",
                    "android.permission.READ_CALL_LOG"
                )
            )
        )
        assertTrue(result != null)
        assertTrue(result!!.reasons.any { it.contains("unusually high number", ignoreCase = true) })
    }

    @Test
    fun `flagged app carries the original package name and label through`() {
        val result = PermissionHeuristics.evaluate(
            profile(packageName = "com.sketchy.flashlight", permissions = arrayOf("android.permission.SEND_SMS"))
        )
        assertTrue(result != null)
        assertTrue(result!!.packageName == "com.sketchy.flashlight")
        assertTrue(result.label == "Example App")
    }
}
