package com.choomhub.app.feature.home.component

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.choomhub.app.feature.home.GestureUi
import com.choomhub.app.feature.home.MyMoodUi
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.choomhub.app.R
import com.choomhub.app.core.common.TimeLabels
import com.choomhub.app.core.designsystem.component.AvatarPlaceholder
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.GestureButton
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.component.SectionHeader
import com.choomhub.app.core.designsystem.icon.GestureIcons
import com.choomhub.app.core.designsystem.illustration.CrowsOnAWirePhoto
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.illustration.RavenMark
import com.choomhub.app.core.designsystem.illustration.WindowWatch
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.PointBalances
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.SharedCatStatus
import com.choomhub.app.feature.home.PartnerMoodUi
import com.choomhub.app.feature.home.SnapUi
import java.time.LocalDate

private val PolaroidInk = Color(0xFF2E2731)
private val PolaroidInkFaint = Color(0xFF6E6470)

@Composable
internal fun HomeHeader(
    dateLabel: String,
    greeting: String,
    viewerName: String?,
    isSampleData: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = ChoomHubTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(dateLabel.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
            Spacer(Modifier.height(ChoomHubTheme.spacing.xs))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textMain,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = if (viewerName != null) "$greeting, $viewerName." else "$greeting.",
                style = ChoomHubTextStyles.Handwritten,
                color = colors.pink,
            )
        }
    }
}

@Composable
internal fun PartnerMoodCard(
    partner: PartnerMoodUi?,
    myMood: MyMoodUi?,
    partnerName: String?,
    onSetMood: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ChoomHubTheme.colors
    ChoomHubCard(modifier = modifier.fillMaxWidth().testTag("partner_mood_card")) {
        if (partner == null) {
            Text(
                text = (partnerName ?: "they").uppercase(),
                style = ChoomHubTextStyles.Eyebrow,
                color = colors.textSecondary,
            )
            Text("No mood shared yet", style = MaterialTheme.typography.titleMedium, color = colors.textMain)
        } else {
            PartnerMoodBody(partner)
        }
        // Your own mood, small, with a way to change it.
        Spacer(Modifier.height(ChoomHubTheme.spacing.md))
        HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onSetMood)
                .padding(top = ChoomHubTheme.spacing.md)
                .testTag("set_my_mood"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("YOU", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
                Text(
                    text = myMood?.let { m -> m.mood.label + (m.note?.let { " \u00B7 $it" } ?: "") } ?: "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = myMood?.let { moodColor(it.mood) } ?: colors.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(ChoomHubTheme.spacing.sm))
            PillBadge(text = if (myMood == null) "set yours" else "change", color = colors.lavender)
        }
    }
}

@Composable
private fun PartnerMoodBody(partner: PartnerMoodUi) {
    val colors = ChoomHubTheme.colors
    run {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarPlaceholder(initial = partner.name, accent = moodColor(partner.mood))
            Spacer(Modifier.width(ChoomHubTheme.spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(
                    text = partner.name.uppercase(),
                    style = ChoomHubTextStyles.Eyebrow,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(partner.mood.label, style = MaterialTheme.typography.headlineMedium, color = moodColor(partner.mood))
            }
        }
        if (partner.note != null) {
            Spacer(Modifier.height(ChoomHubTheme.spacing.md))
            Text(text = partner.note, style = ChoomHubTextStyles.Handwritten, color = colors.textMain)
        }
        Spacer(Modifier.height(ChoomHubTheme.spacing.md))
        Text("updated ${partner.updatedLabel}", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
    }
}

@Composable
internal fun LatestSnapSection(snap: SnapUi?, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Latest snap",
            eyebrow = snap?.let { "from ${it.senderName}" } ?: "snaps",
            trailing = { PillBadge(text = "sample", color = colors.textSecondary) },
        )
        Spacer(Modifier.height(ChoomHubTheme.spacing.md))
        ChoomHubCard(
            modifier = Modifier.fillMaxWidth().testTag("latest_snap_card"),
            tone = if (snap?.isNew == true) CardTone.Glowing else CardTone.Raised,
            glowColor = colors.pink,
        ) {
            if (snap == null) {
                Text("No snaps yet", style = MaterialTheme.typography.titleMedium, color = colors.textMain)
                return@ChoomHubCard
            }
            Polaroid(
                caption = snap.caption,
                stamp = snapStamp(snap),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = ChoomHubTheme.spacing.sm),
            )
            Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
            Text(
                text = "received ${snap.relativeLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun snapStamp(snap: SnapUi): String {
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val time = TimeLabels.clockTime(snap.sentAt, use24Hour)
    val day = if (snap.sentAt.toLocalDate() == LocalDate.now(snap.sentAt.zone)) "today" else snap.relativeLabel
    return "$day · $time"
}

@Composable
private fun Polaroid(caption: String?, stamp: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth(0.86f)
            .rotate(-1.2f)
            .shadow(14.dp, RoundedCornerShape(2.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(2.dp))
            .background(ChoomHubTheme.colors.bone)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
    ) {
        CrowsOnAWirePhoto(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .semantics { contentDescription = "Snap placeholder: crows on a telephone wire at dusk" },
        )
        Spacer(Modifier.height(10.dp))
        if (caption != null) {
            Text(
                text = caption,
                style = ChoomHubTextStyles.Handwritten,
                color = PolaroidInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(text = stamp, style = MaterialTheme.typography.labelSmall, color = PolaroidInkFaint)
    }
}

@Composable
internal fun GestureSection(
    onGesture: (AffectionKind) -> Unit,
    lastReceived: GestureUi?,
    lastSent: GestureUi?,
    modifier: Modifier = Modifier,
) {
    val colors = ChoomHubTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(title = "Send something", eyebrow = "gestures")
        Spacer(Modifier.height(ChoomHubTheme.spacing.md))
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(ChoomHubTheme.spacing.sm),
        ) {
            AffectionKind.entries.forEach { kind ->
                GestureButton(
                    label = kind.label,
                    icon = when (kind) {
                        AffectionKind.Hug -> GestureIcons.Hug
                        AffectionKind.ForeheadKiss -> GestureIcons.ForeheadKiss
                        AffectionKind.HeadPats -> GestureIcons.Paw
                        AffectionKind.TomatoDelivery -> GestureIcons.Tomato
                    },
                    tint = if (kind == AffectionKind.TomatoDelivery) colors.tomato else colors.textMain,
                    onClick = { onGesture(kind) },
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("affection_${kind.name}"),
                )
            }
        }
        listOfNotNull(lastReceived, lastSent).forEachIndexed { i, g ->
            Spacer(Modifier.height(if (i == 0) ChoomHubTheme.spacing.md else ChoomHubTheme.spacing.xs))
            Row(Modifier.testTag(if (g === lastReceived) "gesture_received" else "gesture_sent"), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = g.text,
                    style = if (g === lastReceived) ChoomHubTextStyles.Handwritten else MaterialTheme.typography.bodySmall,
                    color = if (g === lastReceived) colors.textMain else colors.textSecondary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(" \u00B7 ${g.whenLabel}", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
            }
        }
    }
}

@Composable
internal fun PointsSection(points: PointBalances, myRole: PointsRole?, partnerName: String?, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(title = "Points", eyebrow = "rewards", trailing = { PillBadge(text = "sample", color = colors.textSecondary) })
        Spacer(Modifier.height(ChoomHubTheme.spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(ChoomHubTheme.spacing.md)) {
            fun owner(role: PointsRole) = when (myRole) {
                null -> null
                role -> "you"
                else -> partnerName
            }
            PointsCard("Good Boy points", owner(PointsRole.GoodBoy), points.goodBoy, colors.lavender, Modifier.weight(1f).testTag("points_good_boy"))
            PointsCard("Good Girl points", owner(PointsRole.GoodGirl), points.goodGirl, colors.pink, Modifier.weight(1f).testTag("points_good_girl"))
        }
    }
}

@Composable
private fun PointsCard(title: String, owner: String?, value: Int, accent: Color, modifier: Modifier = Modifier) {
    ChoomHubCard(modifier = modifier, tone = CardTone.Calm) {
        Text(title.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = ChoomHubTheme.colors.textSecondary)
        if (owner != null) {
            Text(owner, style = ChoomHubTextStyles.Handwritten, color = ChoomHubTheme.colors.textMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(ChoomHubTheme.spacing.xs))
        Text(
            text = value.toString(),
            // Lining figures: Cormorant's default old-style numerals make "128" read like "I28".
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "lnum"),
            color = accent,
        )
    }
}

@Composable
internal fun SharedCatSection(cat: SharedCatStatus, modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(title = cat.name ?: "The cat", eyebrow = "shared cat")
        Spacer(Modifier.height(ChoomHubTheme.spacing.md))
        ChoomHubCard(modifier = Modifier.fillMaxWidth().testTag("shared_cat_card")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WindowWatch(Modifier.size(width = 124.dp, height = 109.dp))
                Spacer(Modifier.width(ChoomHubTheme.spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (cat.isAdopted) "${cat.name} is around" else "Unnamed, for now",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textMain,
                    )
                    Spacer(Modifier.height(ChoomHubTheme.spacing.xs))
                    Text(
                        text = "Has claimed the windowsill and is watching the crows. Naming, feeding and moods come later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SampleDataNote(modifier: Modifier = Modifier) {
    ChoomHubCard(
        modifier = modifier.fillMaxWidth(),
        tone = CardTone.Calm,
        contentPadding = PaddingValues(ChoomHubTheme.spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RavenMark(Modifier.size(40.dp))
            Spacer(Modifier.width(ChoomHubTheme.spacing.md))
            Text(
                text = "Snaps, points and the cat are sample data for now. Names, moods, gestures and the meetup are real.",
                style = MaterialTheme.typography.bodySmall,
                color = ChoomHubTheme.colors.textSecondary,
            )
        }
    }
}
