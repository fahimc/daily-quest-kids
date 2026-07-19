package com.dailyquestkids.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dailyquestkids.core.model.PuzzleCategory

private val QuestLightScheme =
    lightColorScheme(
        primary = Color(0xFF2F7D4B),
        onPrimary = Color.White,
        secondary = Color(0xFF5C6BC0),
        onSecondary = Color.White,
        tertiary = Color(0xFFD46A50),
        onTertiary = Color.White,
        background = Color(0xFFFFFCF3),
        onBackground = Color(0xFF263126),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF263126),
        surfaceVariant = Color(0xFFEAF0DF),
        onSurfaceVariant = Color(0xFF56624F),
        error = Color(0xFFB3261E),
    )

private val QuestTypography =
    Typography(
        displaySmall =
            Typography().displaySmall.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
            ),
        headlineMedium =
            Typography().headlineMedium.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
            ),
        titleLarge =
            Typography().titleLarge.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
            ),
        bodyLarge = Typography().bodyLarge.copy(lineHeight = 24.sp),
    )

@Composable
fun DailyQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuestLightScheme,
        typography = QuestTypography,
        content = content,
    )
}

data class QuestCategoryStyle(
    val accent: Color,
    val container: Color,
    val border: Color,
    val initial: String,
)

fun categoryStyle(category: PuzzleCategory): QuestCategoryStyle =
    when (category) {
        PuzzleCategory.WORDLY ->
            QuestCategoryStyle(
                accent = Color(0xFF2F7D4B),
                container = Color(0xFFE7F5EA),
                border = Color(0xFF8BC79A),
                initial = "W",
            )
        PuzzleCategory.SPELLING_B ->
            QuestCategoryStyle(
                accent = Color(0xFFC98200),
                container = Color(0xFFFFF0C2),
                border = Color(0xFFE2B64C),
                initial = "B",
            )
        PuzzleCategory.CROSSWORD ->
            QuestCategoryStyle(
                accent = Color(0xFF2F6FAF),
                container = Color(0xFFE6F0FA),
                border = Color(0xFF8FBCE8),
                initial = "C",
            )
        PuzzleCategory.SUDOKU ->
            QuestCategoryStyle(
                accent = Color(0xFF7451A8),
                container = Color(0xFFF0E8FA),
                border = Color(0xFFBBA3DD),
                initial = "S",
            )
        PuzzleCategory.CONNECTIONS ->
            QuestCategoryStyle(
                accent = Color(0xFFD46A50),
                container = Color(0xFFFFE9E2),
                border = Color(0xFFEBA08D),
                initial = "L",
            )
    }

fun ColorScheme.highContrastCopy(): ColorScheme =
    copy(
        primary = Color(0xFF155C2F),
        secondary = Color(0xFF293A8B),
        tertiary = Color(0xFF9E321C),
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
    )
