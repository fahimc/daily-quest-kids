package com.dailyquestkids.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyquestkids.core.model.ShareCardModel

@Composable
internal fun PuzzleResultShareActions(
    shareCard: ShareCardModel?,
    shareActions: ShareActions,
    tagPrefix: String,
    textScale: Float,
    modifier: Modifier = Modifier,
) {
    if (shareCard == null) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((6f * textScale).dp),
    ) {
        Button(
            onClick = { shareActions.share(shareCard) },
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = (38f * textScale).coerceAtLeast(34f).dp)
                    .testTag("${tagPrefix}ShareButton"),
            shape = RoundedCornerShape((14f * textScale).dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
        ) {
            Text(
                "Share",
                fontWeight = FontWeight.Black,
                fontSize = (12f * textScale).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(
            onClick = { shareActions.save(shareCard) },
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = (38f * textScale).coerceAtLeast(34f).dp)
                    .testTag("${tagPrefix}SaveButton"),
            shape = RoundedCornerShape((14f * textScale).dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
        ) {
            Text(
                "Save",
                fontWeight = FontWeight.Black,
                fontSize = (12f * textScale).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
