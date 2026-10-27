package com.asmrone.app.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

/**
 * 用 Coil 实现的 Media3 BitmapLoader，用于通知栏 / 系统媒体面板加载封面图。
 */
@OptIn(UnstableApi::class)
class CoilBitmapLoader(
    private val context: Context,
    private val imageLoader: ImageLoader,
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        if (bitmap != null) future.set(bitmap)
        else future.setException(IllegalArgumentException("Failed to decode bitmap data"))
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false) // 通知栏需要 software bitmap
            .target(
                onSuccess = { drawable ->
                    val bitmap = (drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) future.set(bitmap)
                    else future.setException(IllegalStateException("Expected a bitmap drawable"))
                },
                onError = {
                    future.setException(IllegalStateException("Failed to load artwork"))
                },
            )
            .build()
        imageLoader.enqueue(request)
        return future
    }
}
