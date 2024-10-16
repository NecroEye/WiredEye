package com.muratcangzm.ui.composable

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File


@Composable
fun LoadImageFromUrl(
    url: String,
    modifier: Modifier = Modifier,
    @DrawableRes placeholderResId: Int? = null,
    context: Context
) {
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .apply {
                placeholderResId?.let { placeholder(it) }
                crossfade(true)
            }
            .build(),
        contentDescription = null,
        modifier = modifier
    )
}


@Composable
fun LoadImageFromFile(
    file: File,
    modifier: Modifier = Modifier,
    @DrawableRes placeholderResId: Int? = null,
    context:Context
) {
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(file)
            .apply {
                placeholderResId?.let { placeholder(it) }
                crossfade(true)
            }.build(),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun LoadImageFromDrawable(
    @DrawableRes drawableResId:Int,
    modifier: Modifier = Modifier,
    context: Context
){
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(drawableResId)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier
    )
}