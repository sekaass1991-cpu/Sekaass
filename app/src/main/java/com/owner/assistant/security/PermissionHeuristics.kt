package com.owner.assistant.security

data class AppPermissionProfile(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val requestedPermissions: Set<String>
)

data class FlaggedApp(
    val packageName: String,
    val label: String,
    val reasons: List<String>
)

/**
 * Feature D: permissions-and-behavior heuristic advisor — NOT a malware
 * signature scanner (see blueprint section 3.D / 7). Each rule below flags a
 * permission combination that is a known red flag for "app doing more than
 * its stated job," not a proof of malice; false positives are expected and
 * that's fine for an advisory tool the owner reviews themselves.
 */
object PermissionHeuristics {

    private data class Rule(val reason: String, val matches: (AppPermissionProfile) -> Boolean)

    private val DANGEROUS_PERMISSIONS = setOf(
        "android.permission.CAMERA", "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_CONTACTS", "android.permission.READ_SMS",
        "android.permission.SEND_SMS", "android.permission.READ_CALL_LOG",
        "android.permission.READ_PHONE_STATE", "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.PACKAGE_USAGE_STATS", "android.permission.QUERY_ALL_PACKAGES"
    )

    private val rules = listOf(
        Rule("Can send text messages, including premium-rate numbers, without opening a messaging app") {
            "android.permission.SEND_SMS" in it.requestedPermissions
        },
        Rule("Can draw on top of other apps — a technique used by overlay/phishing attacks") {
            "android.permission.SYSTEM_ALERT_WINDOW" in it.requestedPermissions
        },
        Rule("Requests Accessibility Service access, which can read everything on your screen") {
            "android.permission.BIND_ACCESSIBILITY_SERVICE" in it.requestedPermissions
        },
        Rule("Requests camera, microphone, and precise location together") {
            val p = it.requestedPermissions
            "android.permission.CAMERA" in p &&
                "android.permission.RECORD_AUDIO" in p &&
                "android.permission.ACCESS_FINE_LOCATION" in p
        },
        Rule("Can read your contacts and also has full internet access") {
            "android.permission.READ_CONTACTS" in it.requestedPermissions &&
                "android.permission.INTERNET" in it.requestedPermissions
        },
        Rule("Can see which other apps you have installed and how long you use them") {
            "android.permission.PACKAGE_USAGE_STATS" in it.requestedPermissions ||
                "android.permission.QUERY_ALL_PACKAGES" in it.requestedPermissions
        },
        Rule("Requests an unusually high number of sensitive permissions for one app") {
            it.requestedPermissions.count { p -> p in DANGEROUS_PERMISSIONS } >= 6
        }
    )

    fun evaluate(profile: AppPermissionProfile): FlaggedApp? {
        if (profile.isSystemApp) return null
        val reasons = rules.filter { it.matches(profile) }.map { it.reason }
        return if (reasons.isEmpty()) null else FlaggedApp(profile.packageName, profile.label, reasons)
    }
}
