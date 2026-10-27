package com.asmrone.app.data.remote

import com.asmrone.app.data.model.FilterItem
import com.asmrone.app.data.model.TracksResponse
import com.asmrone.app.data.model.WorksResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * asmr.one 后端 API 接口。
 * baseUrl: https://api.asmr.one/api（实际 host 由拦截器按设置动态替换）
 */
interface AsmrOneApi {

    /** 作品列表（分页、排序） */
    @GET("works")
    suspend fun getWorks(
        @Query("page") page: Int = 1,
        @Query("order") order: String = "create_date",
        @Query("sort") sort: String = "desc",
        @Query("subtitle") subtitle: Int = 0,
    ): WorksResponse

    /**
     * 搜索作品。
     * keyword 支持站点原生筛选语法：`关键词@tag:a/b,-tag:c,va:x,circle:y`（- 前缀=排除，/ 分隔多值）。
     */
    @GET("search/{keyword}")
    suspend fun searchWorks(
        @Path("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("order") order: String = "create_date",
        @Query("sort") sort: String = "desc",
        @Query("subtitle") subtitle: Int = 0,
        @Query("includeTranslationWorks") includeTranslationWorks: Boolean = true,
    ): WorksResponse

    /** 获取作品音轨/文件树 */
    @GET("tracks/{workId}")
    suspend fun getTracks(
        @Path("workId") workId: String,
        @Query("v") v: Int = 1,
    ): TracksResponse

    /** 标签列表（筛选面板用，字段待真机核对） */
    @GET("tags")
    suspend fun getTags(): List<FilterItem>

    /** 社团列表 */
    @GET("circles")
    suspend fun getCircles(): List<FilterItem>

    /** 声优列表 */
    @GET("vas")
    suspend fun getVas(): List<FilterItem>

    /** 拉取字幕等纯文本内容（URL 来自 tracks 树的 mediaStreamUrl） */
    @GET
    suspend fun fetchText(@Url url: String): ResponseBody
}
