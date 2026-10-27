package com.asmrone.app.di

import com.asmrone.app.data.SettingsRepository
import com.asmrone.app.data.remote.AsmrOneApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 服务器选择拦截器：把请求 host 动态改写为用户在设置页选定的服务器。
 * Retrofit baseUrl 固定为 api.asmr.one，其余镜像（asmr-100/200/300.com）仅主机名不同。
 */
class HostSelectionInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = settingsRepository.currentServerHost
        val newRequest = if (request.url.host != host) {
            request.newBuilder()
                .url(request.url.newBuilder().host(host).build())
                .build()
        } else {
            request
        }
        return chain.proceed(newRequest)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** 默认 baseUrl；实际 host 由 HostSelectionInterceptor 按设置动态替换 */
    private const val BASE_URL = "https://api.asmr.one/api/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        settingsRepository: SettingsRepository,
        hostSelectionInterceptor: HostSelectionInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(hostSelectionInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "AsmrOne/1.0 (Android)")
                    .header("Accept", "application/json")
                    .header("Referer", "https://www.asmr.one/")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAsmrOneApi(retrofit: Retrofit): AsmrOneApi =
        retrofit.create(AsmrOneApi::class.java)
}
