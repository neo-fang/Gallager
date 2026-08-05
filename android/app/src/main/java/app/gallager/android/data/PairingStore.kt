package app.gallager.android.data

import android.content.Context
import android.os.Build
import app.gallager.android.model.PairedHost
import java.util.UUID

class PairingStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val deviceId: String
        get() = preferences.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_DEVICE_ID, it).apply()
        }

    val defaultDeviceName: String
        get() = preferences.getString(KEY_DEVICE_NAME, null)
            ?: "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android" }

    fun setDeviceName(value: String) {
        preferences.edit().putString(KEY_DEVICE_NAME, value.trim()).apply()
    }

    fun loadHost(): PairedHost? {
        val pairId = preferences.getString(KEY_PAIR_ID, null) ?: return null
        return PairedHost(
            pairId = pairId,
            hostName = preferences.getString(KEY_HOST_NAME, null) ?: "Mac",
            username = preferences.getString(KEY_USERNAME, null).orEmpty(),
            partnerPublicKey = preferences.getString(KEY_PARTNER_PUBLIC_KEY, null) ?: return null,
            partnerPublicKeyId = preferences.getString(KEY_PARTNER_PUBLIC_KEY_ID, null) ?: return null,
            serverUrl = preferences.getString(KEY_SERVER_URL, null) ?: DEFAULT_SERVER_URL,
        )
    }

    fun saveHost(host: PairedHost) {
        preferences.edit()
            .putString(KEY_PAIR_ID, host.pairId)
            .putString(KEY_HOST_NAME, host.hostName)
            .putString(KEY_USERNAME, host.username)
            .putString(KEY_PARTNER_PUBLIC_KEY, host.partnerPublicKey)
            .putString(KEY_PARTNER_PUBLIC_KEY_ID, host.partnerPublicKeyId)
            .putString(KEY_SERVER_URL, host.serverUrl)
            .apply()
    }

    fun clearHost() {
        preferences.edit()
            .remove(KEY_PAIR_ID)
            .remove(KEY_HOST_NAME)
            .remove(KEY_USERNAME)
            .remove(KEY_PARTNER_PUBLIC_KEY)
            .remove(KEY_PARTNER_PUBLIC_KEY_ID)
            .remove(KEY_SERVER_URL)
            .apply()
    }

    companion object {
        const val DEFAULT_SERVER_URL = "wss://relay.gallager.app"
        private const val PREFERENCES_NAME = "gallager_android_pairing"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_PAIR_ID = "pair_id"
        private const val KEY_HOST_NAME = "host_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_PARTNER_PUBLIC_KEY = "partner_public_key"
        private const val KEY_PARTNER_PUBLIC_KEY_ID = "partner_public_key_id"
        private const val KEY_SERVER_URL = "server_url"
    }
}
