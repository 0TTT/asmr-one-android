package com.asmrone.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 应用设置（DataStore 持久化）。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
        private val KEY_THEME_MODE = intPreferencesKey("theme_mode")

        const val SERVER_ASMR_ONE = "api.asmr.one"
        /** 可用 API 服务器（多源） */
        val SERVERS = listOf(
            SERVER_ASMR_ONE,
            "api.asmr-100.com",
            "api.asmr-200.com",
            "api.asmr-300.com",
        )
        private val KEY_SERVER_HOST = stringPreferencesKey("server_host")

        private val KEY_FAVORITE_FILTERS = stringSetPreferencesKey("favorite_filters")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _serverHost = MutableStateFlow(SERVER_ASMR_ONE)

    /** 当前 API 服务器主机（供拦截器同步读取） */
    val serverHost: StateFlow<String> = _serverHost.asStateFlow()
    val currentServerHost: String get() = _serverHost.value

    /** 收藏的筛选条件（key 形如 "TAG:12"） */
    private val _favoriteFilters = MutableStateFlow<Set<String>>(emptySet())
    val favoriteFilters: StateFlow<Set<String>> = _favoriteFilters.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.collect { prefs ->
                _serverHost.value = prefs[KEY_SERVER_HOST] ?: SERVER_ASMR_ONE
                _favoriteFilters.value = prefs[KEY_FAVORITE_FILTERS] ?: emptySet()
            }
        }
    }

    /** 主题模式：0 跟随系统 / 1 浅色 / 2 深色（默认深色） */
    val themeMode: Flow<Int> = context.dataStore.data.map { it[KEY_THEME_MODE] ?: THEME_DARK }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setServerHost(host: String) {
        _serverHost.value = host
        context.dataStore.edit { it[KEY_SERVER_HOST] = host }
    }

    fun toggleFavoriteFilter(key: String) {
        scope.launch {
            context.dataStore.edit { prefs ->
                val current = prefs[KEY_FAVORITE_FILTERS] ?: emptySet()
                prefs[KEY_FAVORITE_FILTERS] = if (key in current) current - key else current + key
            }
        }
    }
}
