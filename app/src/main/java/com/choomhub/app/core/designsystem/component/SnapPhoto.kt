package com.choomhub.app.core.designsystem.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes JPEG bytes off the main thread; null while decoding or if they're unreadable. */
@Composable
fun rememberDecodedImage(bytes: ByteArray?): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, bytes) {
        value = if (bytes == null) null else withContext(Dispatchers.Default) {
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
        }
    }
    return image
}

/**
 * A snap's picture at its own [aspect] ratio, or a quiet dark placeholder while
 * it loads (it may still be on its way over the network).
 */
@Composable
fun SnapPhoto(
    bytes: ByteArray?,
    aspect: Float,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: ColorFilter? = null,
) {
    val image = rememberDecodedImage(bytes)
    val shaped = modifier.aspectRatio(aspect.coerceIn(0.5f, 2f))
    if (image != null) {
        Image(image, contentDescription, shaped, contentScale = contentScale, colorFilter = colorFilter)
    } else {
        Box(shaped.background(ChoomHubTheme.colors.nightDeep), contentAlignment = Alignment.Center) {
            Text("developing…", style = MaterialTheme.typography.labelSmall, color = ChoomHubTheme.colors.textFaint)
        }
    }
}
