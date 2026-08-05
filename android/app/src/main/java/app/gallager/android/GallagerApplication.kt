package app.gallager.android

import android.app.Application
import app.gallager.android.crypto.SecureKeyRepository
import app.gallager.android.data.PairingStore
import app.gallager.android.network.PairingApi
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class GallagerApplication : Application() {
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val pairingStore: PairingStore by lazy { PairingStore(this) }
    val crypto by lazy { SecureKeyRepository(this).getOrCreate() }
    val pairingApi: PairingApi by lazy { PairingApi(httpClient) }
}
