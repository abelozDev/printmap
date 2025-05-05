package ru.maplyb.printmap.impl.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.time.Duration

internal class Networking {

    companion object {
        const val TIMEOUT = 30_000L
    }
    fun httpClient(): Api {
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(
                HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY)
            )
            .connectTimeout(Duration.ofMillis(TIMEOUT))
            .readTimeout(Duration.ofMillis(TIMEOUT))
            .writeTimeout(Duration.ofMillis(TIMEOUT))
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .baseUrl("https://mt0.google.com/vt/lyrs=s/")
            .build()

        return retrofit.create()
    }
}
