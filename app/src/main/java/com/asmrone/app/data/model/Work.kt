package com.asmrone.app.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * asmr.one 的后端（Laravel/PHP）在字段为 null 时会把它序列化成 [] 或 {}，
 * 例如 "rank":[]、"original_workno":[]。以下 Lenient* 序列化器对可空字段做宽容解析：
 * 任何类型不匹配的值（空数组 / 空对象 / 数字 / 布尔）都降级为 null，绝不抛异常。
 */

object LenientIntSerializer : KSerializer<Int?> {
    private val delegate = Int.serializer().nullable

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Int? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
        return when {
            element == null || element is JsonNull -> null
            element is JsonPrimitive && element.isString -> element.content.toIntOrNull()
            element is JsonPrimitive -> element.intOrNull
            else -> null // JsonArray / JsonObject（如 "rank":[]）一律视为 null
        }
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        delegate.serialize(encoder, value)
    }
}

/** 同上，用于可空 Double 字段 */
object LenientDoubleSerializer : KSerializer<Double?> {
    private val delegate = Double.serializer().nullable

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Double? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
        return when {
            element == null || element is JsonNull -> null
            element is JsonPrimitive && element.isString -> element.content.toDoubleOrNull()
            element is JsonPrimitive -> element.doubleOrNull
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        delegate.serialize(encoder, value)
    }
}

/** 同上，用于可空 Long 字段 */
object LenientLongSerializer : KSerializer<Long?> {
    private val delegate = Long.serializer().nullable

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Long? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
        return when {
            element == null || element is JsonNull -> null
            element is JsonPrimitive && element.isString -> element.content.toLongOrNull()
            element is JsonPrimitive -> element.content.toLongOrNull()
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        delegate.serialize(encoder, value)
    }
}

/** 同上，用于可空 Boolean 字段 */
object LenientBooleanSerializer : KSerializer<Boolean?> {
    private val delegate = Boolean.serializer().nullable

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Boolean? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
        return when {
            element == null || element is JsonNull -> null
            element is JsonPrimitive && element.isString -> element.content.toBooleanStrictOrNull()
            element is JsonPrimitive -> element.booleanOrNull
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean?) {
        delegate.serialize(encoder, value)
    }
}

/** 同上，用于可空 String 字段（[] / {} / 数字 / 布尔 一律视为 null） */
object LenientStringSerializer : KSerializer<String?> {
    private val delegate = String.serializer().nullable

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): String? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
        return when {
            element == null || element is JsonNull -> null
            element is JsonPrimitive && element.isString -> element.content
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        delegate.serialize(encoder, value)
    }
}

/**
 * 作品（Work）数据模型，对应 /api/works 与 /api/search 返回的 works 数组元素。
 * 字段名与 asmr.one 真实 JSON 保持一致。
 */
@Serializable
data class Work(
    @Serializable(LenientIntSerializer::class) val id: Int? = null,
    @Serializable(LenientStringSerializer::class) val title: String? = null,
    @SerialName("circle_id") @Serializable(LenientIntSerializer::class) val circleId: Int? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
    @Serializable(LenientBooleanSerializer::class) val nsfw: Boolean? = null,
    @Serializable(LenientStringSerializer::class) val release: String? = null,
    @SerialName("dl_count") @Serializable(LenientIntSerializer::class) val dlCount: Int? = null,
    @Serializable(LenientIntSerializer::class) val price: Int? = null,
    @SerialName("review_count") @Serializable(LenientIntSerializer::class) val reviewCount: Int? = null,
    @SerialName("rate_count") @Serializable(LenientIntSerializer::class) val rateCount: Int? = null,
    @SerialName("rate_average_2dp") @Serializable(LenientDoubleSerializer::class) val rateAverage2dp: Double? = null,
    @SerialName("rate_count_detail") val rateCountDetail: List<RateCountDetail> = emptyList(),
    @Serializable(LenientIntSerializer::class) val rank: Int? = null,
    @SerialName("has_subtitle") @Serializable(LenientBooleanSerializer::class) val hasSubtitle: Boolean? = null,
    @SerialName("create_date") @Serializable(LenientStringSerializer::class) val createDate: String? = null,
    val vas: List<Va> = emptyList(),
    val tags: List<Tag> = emptyList(),
    @SerialName("language_editions") val languageEditions: List<LanguageEdition> = emptyList(),
    @SerialName("original_workno") @Serializable(LenientStringSerializer::class) val originalWorkno: String? = null,
    @SerialName("age_category_string") @Serializable(LenientStringSerializer::class) val ageCategoryString: String? = null,
    @Serializable(LenientIntSerializer::class) val duration: Int? = null,
    @SerialName("source_type") @Serializable(LenientStringSerializer::class) val sourceType: String? = null,
    @SerialName("source_id") @Serializable(LenientStringSerializer::class) val sourceId: String? = null,
    @SerialName("source_url") @Serializable(LenientStringSerializer::class) val sourceUrl: String? = null,
    val circle: Circle? = null,
    // 注意：asmr.one 使用驼峰命名
    @SerialName("samCoverUrl") @Serializable(LenientStringSerializer::class) val samCoverUrl: String? = null,
    @SerialName("thumbnailCoverUrl") @Serializable(LenientStringSerializer::class) val thumbnailCoverUrl: String? = null,
    @SerialName("mainCoverUrl") @Serializable(LenientStringSerializer::class) val mainCoverUrl: String? = null,
) {
    /** 作品编号，如 RJ01000001 */
    val rjCode: String
        get() = sourceId ?: id?.let { "RJ${it.toString().padStart(8, '0')}" } ?: ""

    /** 封面图，按优先级回退 */
    val coverUrl: String?
        get() = mainCoverUrl ?: samCoverUrl ?: thumbnailCoverUrl

    /** 评分（0-5，如 4.75） */
    val rating: Float
        get() = (rateAverage2dp ?: 0.0).toFloat()

    /** 社团名 */
    val circleName: String get() = circle?.name ?: name ?: ""

    /** 声优名列表 */
    val vaNames: List<String> get() = vas.mapNotNull { it.name }
}

@Serializable
data class Va(
    @Serializable(LenientStringSerializer::class) val id: String? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
)

@Serializable
data class Circle(
    @Serializable(LenientIntSerializer::class) val id: Int? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
    @SerialName("source_id") @Serializable(LenientStringSerializer::class) val sourceId: String? = null,
    @SerialName("source_type") @Serializable(LenientStringSerializer::class) val sourceType: String? = null,
)

@Serializable
data class Tag(
    @Serializable(LenientIntSerializer::class) val id: Int? = null,
    val i18n: TagI18n? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
    @Serializable(LenientIntSerializer::class) val upvote: Int? = null,
    @Serializable(LenientIntSerializer::class) val downvote: Int? = null,
) {
    /** 展示名称，优先中文 */
    val displayName: String
        get() = i18n?.zhCn?.name?.takeIf { it.isNotBlank() }
            ?: i18n?.jaJp?.name?.takeIf { it.isNotBlank() }
            ?: name ?: ""
}

@Serializable
data class TagI18n(
    @SerialName("en-us") val enUs: TagLocale? = null,
    @SerialName("ja-jp") val jaJp: TagLocale? = null,
    @SerialName("zh-cn") val zhCn: TagLocale? = null,
)

@Serializable
data class TagLocale(
    @Serializable(LenientStringSerializer::class) val name: String? = null,
)

@Serializable
data class RateCountDetail(
    @SerialName("review_point") @Serializable(LenientIntSerializer::class) val reviewPoint: Int? = null,
    @Serializable(LenientIntSerializer::class) val count: Int? = null,
    @Serializable(LenientIntSerializer::class) val ratio: Int? = null,
)

@Serializable
data class LanguageEdition(
    @Serializable(LenientStringSerializer::class) val lang: String? = null,
    @Serializable(LenientStringSerializer::class) val title: String? = null,
)

@Serializable
data class Pagination(
    @SerialName("currentPage") @Serializable(LenientIntSerializer::class) val currentPage: Int? = null,
    @SerialName("pageSize") @Serializable(LenientIntSerializer::class) val pageSize: Int? = null,
    @SerialName("totalCount") @Serializable(LenientIntSerializer::class) val totalCount: Int? = null,
)

@Serializable
data class WorksResponse(
    val works: List<Work> = emptyList(),
    val pagination: Pagination? = null,
)
