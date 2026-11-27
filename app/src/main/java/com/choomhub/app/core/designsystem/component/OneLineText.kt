package com.choomhub.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * A label that stays on one line: when it doesn't fit it shrinks a little (down to [minScale] of
 * its size), and only past that is it cut short with "…". For buttons, pills and tabs, where a
 * word dropping onto a second line looks broken.
 */
@Composable
fun OneLineText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    minScale: Float = 0.72f,
) {
    val size: TextUnit = if (style.fontSize.isSpecified) style.fontSize else 14.sp
    val merged = style.merge(
        TextStyle(
            color = if (color != Color.Unspecified) color else style.color,
            textAlign = textAlign ?: style.textAlign,
        ),
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = merged,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = size * minScale, maxFontSize = size, stepSize = 0.5.sp),
    )
}

/** A small round count (how many waiting, how many gifts), in candle. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .clip(CircleShape)
            .background(ChoomHubTheme.colors.candle)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            if (count > 99) "99+" else "$count",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold),
            color = ChoomHubTheme.colors.night,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** A tab's label on one line, with its count as a badge beside it rather than in the words. */
@Composable
fun TabLabel(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        OneLineText(label, MaterialTheme.typography.labelLarge, Modifier.weight(1f, fill = false), color = color)
        if (count > 0) {
            Spacer(Modifier.width(5.dp))
            CountBadge(count)
        }
    }
}
