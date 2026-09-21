package com.cufica.dailyayah.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cufica.dailyayah.R

private val Ink = Color(0xFF202926)
private val SoftInk = Color(0xFF59635F)
private val Canvas = Color(0xFFF5F6F2)
private val Paper = Color(0xFFFCFCF8)
private val Emerald = Color(0xFF146B52)
private val PaleEmerald = Color(0xFFD8E9E0)
private val Sage = Color(0xFFE8ECE5)
private val Gold = Color(0xFF9A6C1F)
private val PaleGold = Color(0xFFF3E8CE)

private val DailyAyahColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = PaleEmerald,
    onPrimaryContainer = Color(0xFF0A3C2D),
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = PaleGold,
    onSecondaryContainer = Color(0xFF3A290C),
    tertiary = Color(0xFF526A60),
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Sage,
    onSurfaceVariant = SoftInk,
    outline = Color(0xFF7B8580),
    outlineVariant = Color(0xFFD3D9D4),
    error = Color(0xFF9E2F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD7),
    onErrorContainer = Color(0xFF410004)
)

val Manrope = FontFamily(
    Font(R.font.manrope_variable, FontWeight.Normal),
    Font(R.font.manrope_variable, FontWeight.Medium),
    Font(R.font.manrope_variable, FontWeight.SemiBold),
    Font(R.font.manrope_variable, FontWeight.Bold)
)

val Newsreader = FontFamily(
    Font(R.font.newsreader_variable, FontWeight.Normal),
    Font(R.font.newsreader_variable, FontWeight.Medium),
    Font(R.font.newsreader_variable, FontWeight.SemiBold)
)

private val DailyAyahTypography = Typography(
    displayLarge = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 56.sp, lineHeight = 60.sp),
    headlineLarge = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp)
)

private val DailyAyahShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

@Composable
fun DailyAyahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DailyAyahColors,
        typography = DailyAyahTypography,
        shapes = DailyAyahShapes,
        content = content
    )
}