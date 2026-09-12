package com.owner.assistant.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Every piece of owner-identifying data (voice embedding, emergency contact,
 * enrollment state) lives in one encrypted, on-device-only prefs file.
 * Nothing here is ever synced off the phone — see blueprint section 5
 * ("local-first, no cloud sync unless the owner explicitly wants backup").
 */
object SecureStore {

    private const val FILE_NAME = "assistant_secure_prefs"
    private var prefs: SharedPreferences? = null

    fun get(context: Context): SharedPreferences {
        return prefs ?: synchronized(this) {
            prefs ?: build(context.applicationContext).also { prefs = it }
        }
    }

    private fun build(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
