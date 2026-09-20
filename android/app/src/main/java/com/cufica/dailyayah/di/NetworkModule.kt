package com.cufica.dailyayah.di

import com.cufica.dailyayah.data.remote.DailyAyahApi
import com.cufica.dailyayah.data.remote.DailyAyahClient
import com.cufica.dailyayah.data.remote.RetrofitDailyAyahClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientModule {
    @Binds
    @Singleton
    abstract fun bindDailyAyahClient(implementation: RetrofitDailyAyahClient): DailyAyahClient
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BaseUrl = "https://daily-ayah-production.up.railway.app/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    fun provideDailyAyahApi(json: Json, client: OkHttpClient): DailyAyahApi = Retrofit.Builder()
        .baseUrl(BaseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(DailyAyahApi::class.java)
}