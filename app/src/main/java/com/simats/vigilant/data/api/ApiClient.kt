package com.simats.vigilant.data.api

import android.content.Context
import com.simats.vigilant.data.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 10.0.2.2 is for Emulator -> Localhost (Best for Emulator)
    // 172.23.18.96 is your laptop's Wi-Fi IP (For Physical Device on same network)
    // If testing on Emulator, UNCOMMENT the next line and COMMENT the IP line
    // private const val BASE_URL = "http://10.0.2.2/vigilant_backend/public/v1/"
    
    // Current Active IP:
    private const val BASE_URL = "http://172.23.21.99/vigilant_backend/public/v1/"

    private var apiService: VigilantApiService? = null

    fun getService(context: Context): VigilantApiService {
        if (apiService == null) {
            val tokenManager = TokenManager(context)
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenManager))
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            apiService = retrofit.create(VigilantApiService::class.java)
        }
        return apiService!!
    }
}
