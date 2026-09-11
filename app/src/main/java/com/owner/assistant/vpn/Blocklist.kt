package com.owner.assistant.vpn

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Domain blocklist for [AdBlockVpnService]. Ships with a small starter list
 * (assets/blocklist.txt) of well-known ad/tracker domains — swap in a bigger
 * hosts-file-style list (e.g. Steven Black's) for real coverage; just keep
 * one bare domain per line.
 */
object Blocklist {

    @Volatile
    private var domains: Set<String> = emptySet()

    fun load(context: Context) {
        if (domains.isNotEmpty()) return
        domains = try {
            context.assets.open(ASSET_NAME).use { stream ->
                BufferedReader(InputStreamReader(stream)).readLines()
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toSet()
            }
        } catch (e: Exception) {
            Log.e("Blocklist", "Failed to load $ASSET_NAME", e)
            emptySet()
        }
    }

    fun isBlocked(domain: String): Boolean {
        val d = domain.lowercase().removeSuffix(".")
        if (d in domains) return true
        // Also block subdomains of a blocked domain (ads.example.com when example.com is listed).
        var suffix = d
        while (true) {
            val dot = suffix.indexOf('.')
            if (dot < 0) return false
            suffix = suffix.substring(dot + 1)
            if (suffix in domains) return true
        }
    }

    fun size(): Int = domains.size
}
