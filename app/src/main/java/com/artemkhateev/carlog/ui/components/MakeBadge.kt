package com.artemkhateev.carlog.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemkhateev.carlog.data.makes.MakeLogo
import com.artemkhateev.carlog.data.makes.searchKey
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import kotlin.math.sqrt

/** Логотипы марок по ключу поиска марки. Пока файл не прочитан, карта пуста — у машин видны буквы. */
val LocalMakeLogos = staticCompositionLocalOf<Map<String, MakeLogo>> { emptyMap() }

/** Контур, обрезанный по своим границам, и его пропорции — ширина к высоте. */
private class LogoVector(val image: ImageVector, val aspect: Float)

// Разобранные контуры и картинки кешируются: в списке марок их сотни, а рисуются они на каждом кадре прокрутки.
private val vectors = HashMap<String, LogoVector>()
private val bitmaps = LruCache<String, ImageBitmap>(48)

private fun vectorOf(make: String, pathData: String): LogoVector = vectors.getOrPut(make) {
    val parser = PathParser().parsePathString(pathData)
    val bounds = parser.toPath().getBounds()
    val width = bounds.width.coerceAtLeast(1f)
    val height = bounds.height.coerceAtLeast(1f)
    val image = ImageVector.Builder(
        name = make,
        defaultWidth = width.dp,
        defaultHeight = height.dp,
        viewportWidth = width,
        viewportHeight = height,
    )
        .addGroup(translationX = -bounds.left, translationY = -bounds.top)
        .addPath(pathData = parser.toNodes(), fill = SolidColor(Color.Black))
        .clearGroup()
        .build()
    LogoVector(image, width / height)
}

/** Логотип без прозрачных полей: иначе в круге он выглядел бы мельче соседних. */
private fun Bitmap.trimTransparent(): Bitmap {
    val pixels = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (pixels[y * width + x] ushr 24 > 16) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    if (right < 0) return this
    return Bitmap.createBitmap(this, left, top, right - left + 1, bottom - top + 1)
}

@Composable
private fun rememberAssetBitmap(path: String): ImageBitmap? {
    val assets = LocalContext.current.assets
    return remember(path) {
        bitmaps.get(path) ?: runCatching { assets.open(path).use(BitmapFactory::decodeStream) }.getOrNull()
            ?.trimTransparent()
            ?.asImageBitmap()
            ?.also { bitmaps.put(path, it) }
    }
}

/**
 * Размер логотипа с пропорциями [aspect], вписанного в круг диаметром [size] с полями.
 * Квадратный значок занимает около 60% диаметра, длинная надпись растягивается почти во всю ширину.
 */
private fun fitInCircle(size: Dp, aspect: Float): DpSize {
    val halfWidth = size.value * 0.44f / sqrt(1f + 1f / (aspect * aspect))
    return DpSize((halfWidth * 2).dp, (halfWidth * 2 / aspect).dp)
}

private val DarkLogo = Color(0xFF263238)

/** Фирменный цвет; светлый (жёлтый, светло-серый) на белом круге не читается — тогда тёмно-серый. */
private fun MakeLogo.tint(): Color {
    val rgb = color?.toLongOrNull(16) ?: return DarkLogo
    val brand = Color(0xFF000000L or rgb)
    return if (brand.luminance() > 0.4f) DarkLogo else brand
}

/** Логотип марки в белом круге — как значок на руле. Для TalkBack без подписи: название марки всегда рядом. */
@Composable
fun MakeLogoBadge(logo: MakeLogo, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val path = logo.path
        val image = logo.image
        if (path != null) {
            val vector = vectorOf(logo.make, path)
            Icon(
                imageVector = vector.image,
                contentDescription = null,
                tint = logo.tint(),
                modifier = Modifier.size(fitInCircle(size, vector.aspect)),
            )
        } else if (image != null) {
            val bitmap = rememberAssetBitmap(image) ?: return@Box
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = if (logo.fill) ContentScale.Crop else ContentScale.Fit,
                modifier = if (logo.fill) {
                    Modifier.matchParentSize()
                } else {
                    Modifier.size(fitInCircle(size, bitmap.width.toFloat() / bitmap.height))
                },
            )
        }
    }
}

/** Значок марки для списков: логотип, а если его нет — первая буква на нейтральном круге. */
@Composable
fun MakeBadge(make: String, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val logo = LocalMakeLogos.current[searchKey(make)]
    if (logo != null) {
        MakeLogoBadge(logo, modifier, size)
        return
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(CarLogTheme.colors.divider),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = make.trim().take(1).uppercase(),
            color = CarLogTheme.colors.textSecondary,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
