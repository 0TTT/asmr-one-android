package com.asmrone.app.di

import com.asmrone.app.service.DownloadManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 供 Compose 中通过 EntryPointAccessors 获取单例。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadManagerEntryPoint {
    fun downloadManager(): DownloadManager
}
