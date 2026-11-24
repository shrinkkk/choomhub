package com.afterglow.app.feature.home.component

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afterglow.app.R
import com.afterglow.app.core.common.TimeLabels
import com.afterglow.app.core.designsystem.component.AffectionButton
import com.afterglow.app.core.designsystem.component.AfterglowCard
import com.afterglow.app.core.designsystem.component.AvatarPlaceholder
import com.afterglow.app.core.designsystem.component.CardTone
import com.afterglow.app.core.designsystem.component.PillBadge
import com.afterglow.app.core.designsystem.component.SectionHeader
import com.afterglow.app.core.designsystem.illustration.BlackCat
import com.afterglow.app.core.designsystem.illustration.CrescentMoon
import com.afterglow.app.core.designsystem.illustration.HugGlyph
import com.afterglow.app.core.designsystem.illustration.KissGlyph
import com.afterglow.app.core.designsystem.illustration.PawGlyph
import com.afterglow.app.core.designsystem.illustration.RainyDuskPhoto
import com.afterglow.app.core.designsystem.illustration.RainyWindowWithCat
import com.afterglow.app.core.designsystem.illustration.Sparkle
import com.afterglow.app.core.designsystem.illustration.Tomato
import com.afterglow.app.core.designsystem.theme.AfterglowTextStyles
import com.afterglow.app.core.designsystem.theme.AfterglowTheme
import com.afterglow.app.domain.model.AffectionKind
import com.afterglow.app.domain.model.PointBalances
import com.afterglow.app.domain.model.SharedCatStatus
import com.afterglow.app.feature.home.PartnerMoodUi
import com.afterglow.app.feature.home.SnapUi
import java.time.LocalDate

private val PolaroidPaper = Color(0xFFEDE4EE)
private val PolaroidInk = Color(0xFF3B2F45)
private val PolaroidInkFaint = Color(0xFF7D6E88)

@Composable
internal fun HomeHeader(
    dateLabel: String,
    greeting: String,
    viewerName: String,
    isSampleData: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AfterglowTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(dateLabel.uppercase(), style = AfterglowTextStyles.Eyebrow, color = colors.textSecondary)
            Spacer(Modifier.height(AfterglowTheme.spacing.xs))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textMain,
            )
            Text(
                text = "$greeting, $viewerName.",
                style = AfterglowTextStyles.Handwritten,
                color = colors.pink,
            )
            if (isSampleData) {
                Spacer(Modifier.height(AfterglowTheme.spacing.sm))
                PillBadge(text = "sample data", color = colors.textSecondary)
            }
        }
        Box(Modifier.size(84.dp)) {
            CrescentMoon(Modifier.size(84.dp))
            Sparkle(Modifier.size(14.dp).offset(x = 8.dp, y = 10.dp), color = colors.pink)
            Sparkle(Modifier.size(9.dp).align(Alignment.BottomEnd).offset(x = (-6).dp, y = (-10).dp), color = colors.candle)
        }
    }
}

@Composable
internal fun PartnerMoodCard(partner: PartnerMoodUi?, modifier: Modifier = Modifier) {
    val colors = AfterglowTheme.colors
    AfterglowCard(modifier = modifier.fillMaxWidth().testTag("partner_mood_card")) {
        if (partner == null) {
            Text("No mood shared yet", style = MaterialTheme.typography.titleMedium, color = colors.textMain)
            return@AfterglowCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarPlaceholder(initial = partner.name, accent = moodColor(partner.mood))
            Spacer(Modifier.width(AfterglowTheme.spacing.lg))
            Column(Modifier.weight(1f)) {
                Text("${partner.name} is feeling", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Text(partner.mood.label, style = MaterialTheme.typography.headlineSmall, color = moodColor(partner.mood))
            }
            MoodOrb(partner.mood, Modifier.size(40.dp))
        }
        if (partner.note != null) {
            Spacer(Modifier.height(AfterglowTheme.spacing.md))
            Text(
                text = "“${partner.note}”",
                style = AfterglowTextStyles.Handwritten,
                color = colors.textMain,
            )
        }
        Spacer(Modifier.height(AfterglowTheme.spacing.md))
        Text("updated ${partner.updatedLabel}", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
    }
}

@Composable
internal fun LatestSnapSection(snap: SnapUi?, modifier: Modifier = Modifier) {
    val colors = AfterglowTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Latest snap",
            eyebrow = snap?.let { "from ${it.senderName}" } ?: "snaps",
            trailing = if (snap?.isNew == true) {
                { PillBadge(text = "new", color = colors.pink, showDot = true) }
            } else {
                null
            },
        )
        Spacer(Modifier.height(AfterglowTheme.spacing.md))
        AfterglowCard(
            modifier = Modifier.fillMaxWidth().testTag("latest_snap_card"),
            tone = if (snap?.isNew == true) CardTone.Glowing else CardTone.Raised,
            glowColor = colors.pink,
        ) {
            if (snap == null) {
                Text("No snaps yet", style = MaterialTheme.typography.titleMedium, color = colors.textMain)
                return@AfterglowCard
            }
            Polaroid(
                caption = snap.caption,
                stamp = snapStamp(snap),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = AfterglowTheme.spacing.sm),
            )
            Spacer(Modifier.height(AfterglowTheme.spacing.sm))
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
            .rotate(-1.8f)
            .shadow(12.dp, RoundedCornerShape(4.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(4.dp))
            .background(PolaroidPaper)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
    ) {
        RainyDuskPhoto(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(2.dp))
                .semantics { contentDescription = "Snap placeholder: a rainy dusk over hills" },
        )
        Spacer(Modifier.height(10.dp))
        if (caption != null) {
            Text(
                text = caption,
                style = AfterglowTextStyles.Handwritten,
                color = PolaroidInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(text = stamp, style = MaterialTheme.typography.labelSmall, color = PolaroidInkFaint)
    }
}

@Composable
internal fun AffectionSection(onAffection: (AffectionKind) -> Unit, modifier: Modifier = Modifier) {
    val colors = AfterglowTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(title = "Send a little love", eyebrow = "affection")
        Spacer(Modifier.height(AfterglowTheme.spacing.md))
        AfterglowCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = AfterglowTheme.spacing.sm,
                vertical = AfterglowTheme.spacing.lg,
            ),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AffectionKind.entries.forEach { kind ->
                    AffectionButton(
                        label = kind.label,
                        tint = when (kind) {
                            AffectionKind.Hug -> colors.lavender
                            AffectionKind.Kiss -> colors.pink
                            AffectionKind.HeadPats -> colors.purple
                            AffectionKind.TomatoDelivery -> colors.tomato
                        },
                        onClick = { onAffection(kind) },
                        modifier = Modifier.weight(1f).testTag("affection_${kind.name}"),
                    ) {
                        when (kind) {
                            AffectionKind.Hug -> HugGlyph(Modifier.size(34.dp))
                            AffectionKind.Kiss -> KissGlyph(Modifier.size(34.dp))
                            AffectionKind.HeadPats -> PawGlyph(Modifier.size(30.dp))
                            AffectionKind.TomatoDelivery -> Tomato(Modifier.size(36.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(AfterglowTheme.spacing.sm))
            Text(
                text = "prototype · these don't send anything yet",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
internal fun PointsSection(points: PointBalances, modifier: Modifier = Modifier) {
    val colors = AfterglowTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(title = "Good points", eyebrow = "productivity")
        Spacer(Modifier.height(AfterglowTheme.spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(AfterglowTheme.spacing.md)) {
            PointsCard("Good Boy", points.goodBoy, colors.lavender, Modifier.weight(1f).testTag("points_good_boy"))
            PointsCard("Good Girl", points.goodGirl, colors.pink, Modifier.weight(1f).testTag("points_good_girl"))
        }
    }
}

@Composable
private fun PointsCard(title: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    AfterglowCard(modifier = modifier, tone = CardTone.Calm) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sparkle(Modifier.size(14.dp), color = accent)
            Spacer(Modifier.width(AfterglowTheme.spacing.sm))
            Text(title, style = MaterialTheme.typography.labelMedium, color = AfterglowTheme.colors.textSecondary)
        }
        Spacer(Modifier.height(AfterglowTheme.spacing.sm))
        Text(value.toString(), style = MaterialTheme.typography.displaySmall, color = accent)
        Text("points", style = MaterialTheme.typography.labelSmall, color = AfterglowTheme.colors.textFaint)
    }
}

@Composable
internal fun SharedCatSection(cat: SharedCatStatus, modifier: Modifier = Modifier) {
    val colors = AfterglowTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(title = cat.name ?: "Our cat", eyebrow = "shared cat")
        Spacer(Modifier.height(AfterglowTheme.spacing.md))
        AfterglowCard(modifier = Modifier.fillMaxWidth().testTag("shared_cat_card")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RainyWindowWithCat(Modifier.size(width = 118.dp, height = 108.dp))
                Spacer(Modifier.width(AfterglowTheme.spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (cat.isAdopted) "${cat.name} is here" else "Waiting on the windowsill",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textMain,
                    )
                    Spacer(Modifier.height(AfterglowTheme.spacing.xs))
                    Text(
                        text = "A little black cat is waiting for a name. Feeding, pets and moods come in a later milestone.",
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
    AfterglowCard(modifier = modifier.fillMaxWidth(), tone = CardTone.Calm) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BlackCat(Modifier.size(width = 34.dp, height = 42.dp), eyesOpen = true)
            Spacer(Modifier.width(AfterglowTheme.spacing.md))
            Text(
                text = "Everything here is sample data for now. Real moods, snaps and points will appear once your shared space is connected.",
                style = MaterialTheme.typography.bodySmall,
                color = AfterglowTheme.colors.textSecondary,
            )
        }
    }
}
