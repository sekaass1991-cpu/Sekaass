package com.owner.assistant.data

import android.content.Context

/**
 * Stores the owner's voice-lock embedding(s) and the personal-safety details
 * (emergency contact) that gate the sensitive features in the blueprint.
 * Embeddings are stored as comma-separated floats — small (tens of values),
 * so no need for a binary blob format.
 */
object OwnerProfileStore {

    private const val KEY_EMBEDDING_PREFIX = "voice_embedding_"
    private const val KEY_EMBEDDING_COUNT = "voice_embedding_count"
    private const val KEY_EMERGENCY_CONTACT = "emergency_contact_number"
    private const val KEY_EMERGENCY_MESSAGE = "emergency_message_template"
    private const val KEY_WAKE_PHRASE = "wake_phrase"
    private const val KEY_VOICE_THRESHOLD = "voice_similarity_threshold"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_GEMINI_MODEL = "gemini_model"

    fun saveEnrollmentSample(context: Context, embedding: FloatArray) {
        val prefs = SecureStore.get(context)
        val count = prefs.getInt(KEY_EMBEDDING_COUNT, 0)
        prefs.edit()
            .putString(KEY_EMBEDDING_PREFIX + count, embedding.joinToString(","))
            .putInt(KEY_EMBEDDING_COUNT, count + 1)
            .apply()
    }

    fun loadEnrolledEmbeddings(context: Context): List<FloatArray> {
        val prefs = SecureStore.get(context)
        val count = prefs.getInt(KEY_EMBEDDING_COUNT, 0)
        return (0 until count).mapNotNull { i ->
            prefs.getString(KEY_EMBEDDING_PREFIX + i, null)
                ?.split(",")
                ?.mapNotNull { it.toFloatOrNull() }
                ?.toFloatArray()
        }
    }

    fun isVoiceEnrolled(context: Context): Boolean =
        SecureStore.get(context).getInt(KEY_EMBEDDING_COUNT, 0) >= MIN_ENROLLMENT_SAMPLES

    fun clearVoiceProfile(context: Context) {
        val prefs = SecureStore.get(context)
        val count = prefs.getInt(KEY_EMBEDDING_COUNT, 0)
        val editor = prefs.edit()
        for (i in 0 until count) editor.remove(KEY_EMBEDDING_PREFIX + i)
        editor.putInt(KEY_EMBEDDING_COUNT, 0)
        editor.apply()
    }

    fun setEmergencyContact(context: Context, phoneNumber: String) {
        SecureStore.get(context).edit().putString(KEY_EMERGENCY_CONTACT, phoneNumber).apply()
    }

    fun getEmergencyContact(context: Context): String? =
        SecureStore.get(context).getString(KEY_EMERGENCY_CONTACT, null)

    fun setEmergencyMessageTemplate(context: Context, template: String) {
        SecureStore.get(context).edit().putString(KEY_EMERGENCY_MESSAGE, template).apply()
    }

    fun getEmergencyMessageTemplate(context: Context): String =
        SecureStore.get(context).getString(
            KEY_EMERGENCY_MESSAGE,
            "I need help. This is my current location: %s"
        ) ?: "I need help. This is my current location: %s"

    fun setWakePhrase(context: Context, phrase: String) {
        SecureStore.get(context).edit().putString(KEY_WAKE_PHRASE, phrase.lowercase().trim()).apply()
    }

    fun getWakePhrase(context: Context): String =
        SecureStore.get(context).getString(KEY_WAKE_PHRASE, DEFAULT_WAKE_PHRASE)
            ?: DEFAULT_WAKE_PHRASE

    fun setVoiceSimilarityThreshold(context: Context, threshold: Float) {
        SecureStore.get(context).edit().putFloat(KEY_VOICE_THRESHOLD, threshold).apply()
    }

    fun getVoiceSimilarityThreshold(context: Context): Float =
        SecureStore.get(context).getFloat(KEY_VOICE_THRESHOLD, DEFAULT_SIMILARITY_THRESHOLD)

    /** Gemini API key for the conversational chat mode ([com.owner.assistant.chat]). Encrypted at rest like everything else in this store — never logged, never sent anywhere but generativelanguage.googleapis.com. */
    fun setGeminiApiKey(context: Context, apiKey: String) {
        SecureStore.get(context).edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    fun getGeminiApiKey(context: Context): String? =
        SecureStore.get(context).getString(KEY_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun setGeminiModel(context: Context, model: String) {
        SecureStore.get(context).edit().putString(KEY_GEMINI_MODEL, model.trim()).apply()
    }

    fun getGeminiModel(context: Context): String =
        SecureStore.get(context).getString(KEY_GEMINI_MODEL, DEFAULT_GEMINI_MODEL)
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_GEMINI_MODEL

    const val MIN_ENROLLMENT_SAMPLES = 3
    const val DEFAULT_WAKE_PHRASE = "hey assistant"
    const val DEFAULT_SIMILARITY_THRESHOLD = 0.82f
    const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
}
