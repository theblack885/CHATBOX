package com.example.insta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Gallery image ko chhota karke base64 text banata hai (Firestore me save karne ke liye). */
fun uriToBase64(ctx: Context, uri: Uri, maxSide: Int, quality: Int): String? {
    return try {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        val scale = maxSide.toFloat() / max(bmp.width, bmp.height)
        val out = if (scale < 1f)
            Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        else bmp
        val bos = ByteArrayOutputStream()
        out.compress(Bitmap.CompressFormat.JPEG, quality, bos)
        Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

fun timeAgo(ts: Long): String {
    val d = (System.currentTimeMillis() - ts) / 1000
    return when {
        d < 60 -> "abhi"
        d < 3600 -> "${d / 60} min pehle"
        d < 86400 -> "${d / 3600} ghante pehle"
        else -> "${d / 86400} din pehle"
    }
}

@Composable
fun Base64Image(data: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bmp by produceState<ImageBitmap?>(null, data) {
        value = withContext(Dispatchers.Default) {
            if (data.isEmpty()) null else try {
                val b = Base64.decode(data, Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    val img = bmp
    if (img != null) Image(img, null, modifier, contentScale = contentScale)
    else Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}

/** Profile photo, ya photo na ho to gradient circle me pehla akshar. */
@Composable
fun Avatar(photo: String, name: String, size: Dp = 36.dp) {
    if (photo.isNotEmpty()) {
        Base64Image(photo, Modifier.size(size).clip(CircleShape))
    } else {
        Box(
            Modifier.size(size).clip(CircleShape).background(BrandBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/** Gradient ring wala avatar (Story jaisa look). */
@Composable
fun RingAvatar(photo: String, name: String, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).border(2.5.dp, BrandBrush, CircleShape).padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Avatar(photo, name, size - 15.dp)
    }
}

@Composable
fun Wordmark(size: TextUnit = 34.sp) {
    Text(
        "Insta",
        style = TextStyle(
            brush = BrandBrush, fontFamily = FontFamily.Cursive,
            fontSize = size, fontWeight = FontWeight.Bold
        )
    )
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(BrandBrush)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
)
