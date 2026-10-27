package com.asmrone.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.asmrone.app.data.SettingsRepository
import com.asmrone.app.ui.component.AppTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val repository: com.asmrone.app.data.repository.AsmrOneRepository,
    private val okHttpClient: okhttp3.OkHttpClient,
) : ViewModel() {

    val themeMode: StateFlow<Int> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.THEME_DARK)

    val serverHost: StateFlow<String> = settingsRepository.serverHost

    /** 延迟测量结果：host -> 毫秒，-1 表示超时/失败，缺失表示测量中 */
    private val _latencies = MutableStateFlow<Map<String, Long>>(emptyMap())
    val latencies: StateFlow<Map<String, Long>> = _latencies.asStateFlow()

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setServerHost(host: String) {
        viewModelScope.launch { settingsRepository.setServerHost(host) }
    }

    /** 并发测量各服务器 HEAD 请求延迟 */
    fun checkLatencies() {
        SettingsRepository.SERVERS.forEach { host ->
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val ms = try {
                    val start = android.os.SystemClock.elapsedRealtime()
                    val request = okhttp3.Request.Builder()
                        .url("https://$host/")
                        .head()
                        .build()
                    okHttpClient.newCall(request).execute().use { }
                    android.os.SystemClock.elapsedRealtime() - start
                } catch (_: Exception) {
                    -1L
                }
                _latencies.value = _latencies.value + (host to ms)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }
}

private val themeOptions = listOf(
    SettingsRepository.THEME_SYSTEM to "跟随系统",
    SettingsRepository.THEME_LIGHT to "浅色",
    SettingsRepository.THEME_DARK to "深色",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val serverHost by viewModel.serverHost.collectAsStateWithLifecycle()
    val latencies by viewModel.latencies.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.checkLatencies()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTopBar("设置")
        SectionTitle("API 服务器")
            SettingsRepository.SERVERS.forEach { host ->
                val ms = latencies[host]
                val latencyText = when {
                    ms == null -> "测速中…"
                    ms < 0 -> "超时"
                    else -> "$ms ms"
                }
                val latencyColor = when {
                    ms == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    ms < 0 -> MaterialTheme.colorScheme.error
                    ms < 300 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setServerHost(host) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = serverHost == host,
                        onClick = { viewModel.setServerHost(host) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(host, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                        if (serverHost == host) {
                            Text(
                                "当前使用",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(latencyText, fontSize = 13.sp, color = latencyColor)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { viewModel.checkLatencies() }) {
                    Text("重新测速", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("外观")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                themeOptions.forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(label) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("数据")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("播放历史", fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("清空", color = MaterialTheme.colorScheme.secondary)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("关于")
            SettingRow("版本", "1.0.1")
            SettingRow("项目", "AsmrOne 特供版")
            Text(
                text = "仅供学习交流使用，请遵守当地法律法规与站点使用条款。所有作品版权归原作者及 DLsite 所有。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
