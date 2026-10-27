package com.asmrone.app.data.model

/**
 * 一行字幕：时间戳（毫秒）+ 文本。
 */
data class SubtitleLine(
    val timeMs: Long,
    val text: String,
)

/**
 * 解析 LRC / VTT / SRT 字幕文本，返回按时间升序的字幕行。
 * 自动识别格式：包含 [mm:ss] 风格时间戳按 LRC 解析，否则按 VTT/SRT 解析。
 */
fun parseSubtitle(raw: String): List<SubtitleLine> {
    // ---------- LRC ----------
    val lrcRegex = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    val lrcLines = raw.lines()
        .flatMap { line ->
            val matches = lrcRegex.findAll(line).toList()
            if (matches.isEmpty()) return@flatMap emptyList()
            val text = line.replace(lrcRegex, "").trim()
            if (text.isEmpty()) return@flatMap emptyList()
            matches.map { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val fracStr = m.groupValues[3]
                val frac = when (fracStr.length) {
                    0 -> 0L
                    1 -> fracStr.toLong() * 100
                    2 -> fracStr.toLong() * 10
                    else -> fracStr.take(3).toLong()
                }
                SubtitleLine((min * 60 + sec) * 1000 + frac, text)
            }
        }
        .sortedBy { it.timeMs }
    if (lrcLines.isNotEmpty()) return lrcLines

    // ---------- VTT / SRT ----------
    val cueRegex = Regex(
        """(?:(\d{1,2}):)?(\d{1,2}):(\d{2})[.,](\d{1,3})\s*-->"""
    )
    val result = mutableListOf<SubtitleLine>()
    var currentTime: Long? = null
    val textBuilder = StringBuilder()

    fun flush() {
        val t = currentTime
        val text = textBuilder.toString().trim()
        if (t != null && text.isNotEmpty()) result += SubtitleLine(t, text)
        currentTime = null
        textBuilder.clear()
    }

    raw.lines().forEach { rawLine ->
        val line = rawLine.trim()
        val m = cueRegex.find(line)
        if (m != null) {
            flush()
            val h = m.groupValues[1].ifEmpty { "0" }.toLong()
            val min = m.groupValues[2].toLong()
            val sec = m.groupValues[3].toLong()
            val ms = m.groupValues[4].padEnd(3, '0').toLong()
            currentTime = (h * 3600 + min * 60 + sec) * 1000 + ms
        } else if (currentTime != null) {
            if (line.isEmpty() || line == "WEBVTT" || line.all { it.isDigit() }) {
                if (line.isEmpty()) flush()
                // 序号行（SRT 的纯数字行）忽略
            } else {
                textBuilder.appendLine(line)
            }
        }
    }
    flush()
    return result.sortedBy { it.timeMs }
}

/**
 * 二分查找当前播放进度对应的字幕行下标；无对应行返回 -1。
 */
fun List<SubtitleLine>.currentIndexFor(positionMs: Long): Int {
    if (isEmpty()) return -1
    var lo = 0
    var hi = lastIndex
    var ans = -1
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        if (this[mid].timeMs <= positionMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return ans
}

/**
 * 文件名规范化（用于字幕-音频模糊匹配）：
 * 小写化并去掉所有非字母/数字字符（含全角符号、♡、空白、下划线等）。
 * 如 "01_パパ大好き♡（プロローグ）_SEなし" -> "01パパ大好きプロローグseなし"
 */
fun normalizeSubtitleBaseName(name: String): String =
    name.lowercase().filter { it.isLetterOrDigit() }
