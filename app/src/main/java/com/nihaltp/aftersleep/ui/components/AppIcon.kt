package com.nihaltp.aftersleep.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.nihaltp.aftersleep.ui.theme.SleepSurfaceAlt

@Composable
fun rememberAppIcon(packageName: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        bitmap = loadIcon(context, packageName)
    }
    return bitmap
}

@Composable
fun AppIcon(
    packageName: String?,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberAppIcon(packageName)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier =
                modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(SleepSurfaceAlt),
        )
    }
}

private fun loadIcon(
    context: Context,
    packageName: String?,
): ImageBitmap? {
    if (packageName.isNullOrBlank()) return null
    return runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        drawable.toBitmap().asImageBitmap()
    }.getOrNull()
}
