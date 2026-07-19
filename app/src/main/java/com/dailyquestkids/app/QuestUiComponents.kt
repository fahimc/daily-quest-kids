package com.dailyquestkids.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyquestkids.core.design.categoryStyle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzleStatus
import kotlin.math.min

internal data class QuestScreenMetrics(
    val widthDp: Float,
    val heightDp: Float,
    val scale: Float,
    val pagePadding: Dp,
    val gap: Dp,
    val cardCorner: Dp,
    val compact: Boolean,
)

internal object QuestLayoutMetrics {
    fun calculate(
        widthDp: Float,
        heightDp: Float,
    ): QuestScreenMetrics {
        val baseScale = min(widthDp / 393f, heightDp / 852f).coerceIn(0.72f, 1.16f)
        return QuestScreenMetrics(
            widthDp = widthDp,
            heightDp = heightDp,
            scale = baseScale,
            pagePadding = (18f * baseScale).dp,
            gap = (8f * baseScale).dp,
            cardCorner = (22f * baseScale).dp,
            compact = heightDp < 700f || widthDp < 360f,
        )
    }
}

@Composable
internal fun QuestSceneFrame(
    modifier: Modifier = Modifier,
    testTag: String,
    content: @Composable BoxScope.(QuestScreenMetrics) -> Unit,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(testTag),
    ) {
        val metrics =
            remember(maxWidth, maxHeight) {
                QuestLayoutMetrics.calculate(maxWidth.value, maxHeight.value)
            }
        QuestSceneBackground()
        content(metrics)
    }
}

@Composable
internal fun QuestSceneBackground(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF24A8F4),
                            Color(0xFF83E4F2),
                            Color(0xFFB7F4D7),
                            Color(0xFF62BF54),
                        ),
                    ),
                ),
    ) {
        val scale = min(maxWidth.value / 393f, maxHeight.value / 852f).coerceIn(0.72f, 1.18f)
        QuestCloud(Modifier.align(Alignment.TopStart).offset((-34f * scale).dp, (76f * scale).dp), scale)
        QuestCloud(Modifier.align(Alignment.TopEnd).offset((38f * scale).dp, (118f * scale).dp), scale * 0.86f)
        QuestHill(
            modifier = Modifier.align(Alignment.BottomStart).offset((-54f * scale).dp, (38f * scale).dp),
            width = (260f * scale).dp,
            height = (148f * scale).dp,
            color = Color(0xFF3A9F47),
        )
        QuestHill(
            modifier = Modifier.align(Alignment.BottomEnd).offset((54f * scale).dp, (16f * scale).dp),
            width = (310f * scale).dp,
            height = (180f * scale).dp,
            color = Color(0xFF55B94F),
        )
        QuestTrail(Modifier.align(Alignment.BottomCenter), scale)
    }
}

@Composable
internal fun QuestLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val scale = if (compact) 0.72f else 1f
    Column(
        modifier =
            modifier
                .semantics { contentDescription = "Daily Quest Kids" }
                .testTag("questLogo"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-2f * scale).dp),
    ) {
        QuestLogoLine(
            text = "DAILY",
            fontSize = 38f * scale,
            fill = Color.White,
            shadow = Color(0xFF053B86),
        )
        QuestLogoLine(
            text = "QUEST",
            fontSize = 48f * scale,
            fill = Color(0xFFFFB61F),
            shadow = Color(0xFF052D6D),
        )
        Surface(
            color = Color(0xFF137ED8),
            border = BorderStroke((2f * scale).dp, Color(0xFF04336F)),
            shape = RoundedCornerShape((10f * scale).dp),
        ) {
            Text(
                text = "KIDS",
                modifier = Modifier.padding(horizontal = (16f * scale).dp, vertical = (2f * scale).dp),
                color = Color.White,
                fontSize = (22f * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
            )
        }
    }
}

@Composable
internal fun QuestIconTile(
    category: PuzzleCategory,
    modifier: Modifier = Modifier,
    completed: Boolean = false,
    scale: Float = 1f,
) {
    val style = categoryStyle(category)
    Box(modifier = modifier, contentAlignment = Alignment.TopEnd) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = style.accent,
            border = BorderStroke((2f * scale).dp, style.border),
            shape = RoundedCornerShape((16f * scale).dp),
            shadowElevation = (3f * scale).dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                QuestCategoryGlyph(category = category, scale = scale)
            }
        }
        if (completed) {
            Surface(
                modifier = Modifier.size((24f * scale).dp),
                color = Color(0xFF48B95F),
                shape = CircleShape,
                border = BorderStroke((2f * scale).dp, Color.White),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✓", color = Color.White, fontSize = (16f * scale).sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
internal fun QuestStatusBadge(
    status: PuzzleStatus,
    category: PuzzleCategory,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val style = categoryStyle(category)
    val label = status.label.uppercase()
    Surface(
        modifier = modifier,
        color = if (status == PuzzleStatus.COMPLETED) Color(0xFF50B848) else style.container,
        shape = RoundedCornerShape((14f * scale).dp),
    ) {
        QuestAutoText(
            text = label,
            modifier = Modifier.padding(horizontal = (10f * scale).dp, vertical = (3f * scale).dp),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 11f * scale,
                    minFontSizeSp = 6f,
                    maxLines = 1,
                    color = if (status == PuzzleStatus.COMPLETED) Color.White else style.accent,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                ),
        )
    }
}

@Composable
internal fun QuestActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        QuestAutoText(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 20f,
                    minFontSizeSp = 8f,
                    maxLines = 1,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                ),
        )
    }
}

@Composable
internal fun QuestPill(
    text: String,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.86f),
        shape = RoundedCornerShape((24f * scale).dp),
        shadowElevation = (2f * scale).dp,
    ) {
        QuestAutoText(
            text = text,
            modifier = Modifier.padding(horizontal = (18f * scale).dp, vertical = (9f * scale).dp),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 15f * scale,
                    minFontSizeSp = 7f,
                    maxLines = 1,
                    color = Color(0xFF0B2A48),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                ),
        )
    }
}

@Composable
internal fun QuestProgressSegments(
    completedCount: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (index < completedCount) Color(0xFF8BE234) else Color(0xFF0D315D)),
            )
        }
    }
}

@Composable
internal fun QuestAutoText(
    text: String,
    modifier: Modifier,
    spec: QuestAutoTextSpec,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier, contentAlignment = spec.contentAlignment) {
        val constraints =
            Constraints(
                maxWidth = with(density) { maxWidth.toPx().toInt().coerceAtLeast(1) },
                maxHeight = with(density) { maxHeight.toPx().toInt().coerceAtLeast(1) },
            )
        val fitted =
            remember(text, constraints, spec) {
                questFittedFontSizeSp(
                    text = text,
                    textMeasurer = textMeasurer,
                    constraints = constraints,
                    spec = spec,
                )
            }
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = spec.color,
            fontWeight = spec.fontWeight,
            fontSize = fitted.sp,
            lineHeight = (fitted * 1.14f).sp,
            maxLines = spec.maxLines,
            overflow = TextOverflow.Clip,
            softWrap = spec.softWrap,
            textAlign = spec.textAlign,
            letterSpacing = 0.sp,
        )
    }
}

internal data class QuestAutoTextSpec(
    val maxFontSizeSp: Float,
    val minFontSizeSp: Float,
    val maxLines: Int,
    val color: Color,
    val fontWeight: FontWeight,
    val textAlign: TextAlign,
    val softWrap: Boolean = true,
    val contentAlignment: Alignment = Alignment.Center,
)

@Composable
private fun QuestLogoLine(
    text: String,
    fontSize: Float,
    fill: Color,
    shadow: Color,
) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = shadow,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
            modifier = Modifier.offset(0.dp, 3.dp),
        )
        Text(
            text = text,
            color = fill,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun QuestCategoryGlyph(
    category: PuzzleCategory,
    scale: Float,
) {
    when (category) {
        PuzzleCategory.WORDLY -> WordlyGlyph(scale)
        PuzzleCategory.SPELLING_B -> Text("B", color = Color(0xFF6E3900), fontSize = (36f * scale).sp, fontWeight = FontWeight.Black)
        PuzzleCategory.CROSSWORD -> Text("▦", color = Color.White, fontSize = (34f * scale).sp, fontWeight = FontWeight.Black)
        PuzzleCategory.SUDOKU -> SudokuGlyph(scale)
        PuzzleCategory.CONNECTIONS -> Text("●─●", color = Color.White, fontSize = (22f * scale).sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WordlyGlyph(scale: Float) {
    Surface(
        modifier = Modifier.size((44f * scale).dp).rotate(-10f),
        color = Color.White,
        shape = RoundedCornerShape((8f * scale).dp),
        shadowElevation = (3f * scale).dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("W", color = Color(0xFF248C2A), fontSize = (28f * scale).sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SudokuGlyph(scale: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("1 3", color = Color.White, fontSize = (15f * scale).sp, fontWeight = FontWeight.Black)
        Text("6 9", color = Color.White, fontSize = (15f * scale).sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun QuestCloud(
    modifier: Modifier,
    scale: Float,
) {
    Box(modifier = modifier.size((138f * scale).dp, (58f * scale).dp)) {
        listOf(
            CloudBubble(0f, 20f, 52f),
            CloudBubble(36f, 4f, 66f),
            CloudBubble(86f, 18f, 52f),
        ).forEach { bubble ->
            Box(
                modifier =
                    Modifier
                        .offset((bubble.x * scale).dp, (bubble.y * scale).dp)
                        .size((bubble.size * scale).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.78f)),
            )
        }
    }
}

@Composable
private fun QuestHill(
    modifier: Modifier,
    width: Dp,
    height: Dp,
    color: Color,
) {
    Box(
        modifier =
            modifier
                .size(width, height)
                .clip(RoundedCornerShape(topStart = height, topEnd = height))
                .background(color.copy(alpha = 0.72f)),
    )
}

@Composable
private fun QuestTrail(
    modifier: Modifier,
    scale: Float,
) {
    Box(
        modifier =
            modifier
                .width((138f * scale).dp)
                .height((190f * scale).dp)
                .clip(RoundedCornerShape(topStart = (70f * scale).dp, topEnd = (70f * scale).dp))
                .background(Color(0xFFE6B95F).copy(alpha = 0.55f)),
    )
}

private fun questFittedFontSizeSp(
    text: String,
    textMeasurer: TextMeasurer,
    constraints: Constraints,
    spec: QuestAutoTextSpec,
): Float {
    var low = spec.minFontSizeSp
    var high = spec.maxFontSizeSp.coerceAtLeast(low)
    repeat(9) {
        val mid = (low + high) / 2f
        val result =
            textMeasurer.measure(
                text = AnnotatedString(text),
                style =
                    androidx.compose.ui.text.TextStyle(
                        fontSize = mid.sp,
                        lineHeight = (mid * 1.14f).sp,
                        fontWeight = spec.fontWeight,
                        textAlign = spec.textAlign,
                    ),
                constraints = constraints,
                maxLines = spec.maxLines,
                softWrap = spec.softWrap,
                overflow = TextOverflow.Clip,
            )
        if (result.didOverflowWidth || result.didOverflowHeight) {
            high = mid
        } else {
            low = mid
        }
    }
    return low.coerceIn(spec.minFontSizeSp, spec.maxFontSizeSp)
}

private data class CloudBubble(
    val x: Float,
    val y: Float,
    val size: Float,
)
