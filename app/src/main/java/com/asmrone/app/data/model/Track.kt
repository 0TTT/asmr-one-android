@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.asmrone.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/** asmr.one API 主机，用于把相对路径的媒体 URL 补全为绝对地址 */
const val ASMR_ONE_ORIGIN = "https://api.asmr.one"

/**
 * 音轨 / 文件节点。对应 /api/tracks/{workId}?v=1 返回的递归结构。
 * 真实 API 的 type 取值："folder" | "audio" | "image" | "text"。
 * 真实 API 字段为驼峰命名（mediaStreamUrl 等），蛇形作为备选名兼容部分镜像。
 */
@Serializable
data class TrackNode(
    @Serializable(LenientStringSerializer::class) val type: String? = null,
    @Serializable(LenientStringSerializer::class) val title: String? = null,
    val children: List<TrackNode> = emptyList(),
    @Serializable(LenientStringSerializer::class) val hash: String? = null,
    val work: TrackWork? = null,
    @SerialName("workTitle") @JsonNames("work_title")
    @Serializable(LenientStringSerializer::class) val workTitle: String? = null,
    @SerialName("mediaStreamUrl") @JsonNames("media_stream_url")
    @Serializable(LenientStringSerializer::class) val mediaStreamUrl: String? = null,
    @SerialName("mediaDownloadUrl") @JsonNames("media_download_url")
    @Serializable(LenientStringSerializer::class) val mediaDownloadUrl: String? = null,
    @Serializable(LenientLongSerializer::class) val size: Long? = null,            // 接口为 int64，大 WAV 可能超过 Int 上限
) {
    val isFolder: Boolean get() = type == "folder"
    /** 非文件夹即为具体文件节点（audio / image / text） */
    val isFile: Boolean get() = type != null && type != "folder"
    /** 可播放的音频文件 */
    val isAudio: Boolean get() = type == "audio"
    /** 字幕文件（.lrc / .vtt / .srt），与音频同目录、文件名相同或接近 */
    val isSubtitle: Boolean
        get() = type != "folder" && title?.let { t ->
            t.endsWith(".lrc", true) || t.endsWith(".vtt", true) || t.endsWith(".srt", true)
        } == true
}

@Serializable
data class TrackWork(
    @Serializable(LenientIntSerializer::class) val id: Int? = null,
    @Serializable(LenientStringSerializer::class) val title: String? = null,
)

/** 作品文件树，/api/tracks/{id} 直接返回 List<TrackNode> */
typealias TracksResponse = List<TrackNode>

/** 把相对路径的媒体 URL 补全为绝对地址（绝对地址原样返回） */
fun absoluteMediaUrl(url: String?): String? = url?.let {
    when {
        it.startsWith("http://") || it.startsWith("https://") -> it
        it.startsWith("/") -> ASMR_ONE_ORIGIN + it
        else -> "$ASMR_ONE_ORIGIN/$it"
    }
}

/** 可直接交给 ExoPlayer / OkHttp 的在线播放地址 */
val TrackNode.resolvedStreamUrl: String? get() = absoluteMediaUrl(mediaStreamUrl)

/** 可直接用于下载的地址 */
val TrackNode.resolvedDownloadUrl: String? get() = absoluteMediaUrl(mediaDownloadUrl)
