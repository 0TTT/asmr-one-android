package com.asmrone.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 筛选面板条目（标签 / 社团 / 声优列表项）。
 * 字段名做了多版本兼容（works_count / worksCount / count），取不到计数时展示层省略。
 */
@Serializable
data class FilterItem(
    @Serializable(LenientIntSerializer::class) val id: Int? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
    val i18n: TagI18n? = null,
    @SerialName("works_count") @Serializable(LenientIntSerializer::class) val worksCount1: Int? = null,
    @SerialName("worksCount") @Serializable(LenientIntSerializer::class) val worksCount2: Int? = null,
    @Serializable(LenientIntSerializer::class) val count: Int? = null,
) {
    /** 展示名称，优先中文 */
    val displayName: String
        get() = i18n?.zhCn?.name?.takeIf { it.isNotBlank() }
            ?: i18n?.jaJp?.name?.takeIf { it.isNotBlank() }
            ?: name ?: ""

    val displayCount: Int?
        get() = worksCount1 ?: worksCount2 ?: count
}
