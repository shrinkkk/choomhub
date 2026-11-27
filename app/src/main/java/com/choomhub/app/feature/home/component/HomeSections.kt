package com.choomhub.app.feature.home.component

import com.choomhub.app.core.designsystem.component.GestureRow
import com.choomhub.app.core.designsystem.component.OneLineText
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import com.choomhub.app.feature.home.AwardUi
import com.choomhub.app.domain.model.PointRules
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.choomhub.app.core.designsystem.component.SnapPhoto
import androidx.compose.material3.TextButton
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
import com.choomhub.app.core.designsystem.icon.PointIcons
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
                val line = myMood?.let { m ->
                    val span = moodSpan(m.mood)
                    androidx.compose.ui.text.buildAnnotatedString {
                        pushStyle(span)
                        append(m.mood.label)
                        pop()
                        m.note?.let { pushStyle(androidx.compose.ui.text.SpanStyle(color = moodColor(m.mood))); append(" \u00B7 $it"); pop() }
                    }
                } ?: androidx.compose.ui.text.AnnotatedString("Not set")
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (myMood == null) colors.textFaint else androidx.compose.ui.graphics.Color.Unspecified,
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
                Text(partner.mood.label, style = moodStyle(partner.mood, MaterialTheme.typography.headlineMedium))
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
internal fun LatestSnapSection(
    snap: SnapUi?,
    image: ByteArray?,
    partnerName: String?,
    mySnapLabel: String?,
    onOpen: (String) -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ChoomHubTheme.colors
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
        Spacer(Modifier.height(ChoomHubTheme.spacing.md))
        ChoomHubCard(
            modifier = Modifier.fillMaxWidth().testTag("latest_snap_card"),
            tone = if (snap?.isNew == true) CardTone.Glowing else CardTone.Raised,
            glowColor = colors.pink,
        ) {
            if (snap == null) {
                Text(
                    "Nothing from ${partnerName ?: "them"} yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textMain,
                )
                Text(
                    "Each of you has one snap at a time. Sending a new one replaces your last, unless one of you kept it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            } else {
                Polaroid(
                    caption = snap.caption,
                    stamp = snapStamp(snap),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = ChoomHubTheme.spacing.sm)
                        .clickable(role = Role.Button, onClickLabel = "Open snap") { onOpen(snap.id) }
                        .testTag("latest_snap_open"),
                ) {
                    SnapPhoto(image, snap.aspect, contentDescription = snap.caption ?: "Snap from ${snap.senderName}", Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
                Text(
                    text = "received ${snap.relativeLabel} \u00B7 tap to open",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textFaint,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(Modifier.height(ChoomHubTheme.spacing.md))
            HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mySnapLabel ?: "Send one",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onTakePhoto, modifier = Modifier.testTag("snap_camera")) {
                    OneLineText("TAKE A SNAP", style = MaterialTheme.typography.labelLarge, color = colors.lavender)
                }
            }
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

/** A slightly tilted polaroid frame around [photo]. */
@Composable
internal fun Polaroid(caption: String?, stamp: String, modifier: Modifier = Modifier, photo: @Composable () -> Unit) {
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
        photo()
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
        GestureRow(AffectionKind.entries.map { it.label }, rowModifier = Modifier.height(IntrinsicSize.Min)) { labelStyle ->
            AffectionKind.entries.forEach { kind ->
                GestureButton(
                    label = kind.label,
                    labelStyle = labelStyle,
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

/** Which way the points form is going. */
private enum class PointsAction { Award, TakeAway }

@Composable
internal fun PointsSection(
    points: PointBalances,
    myRole: PointsRole?,
    partnerName: String?,
    lastAward: AwardUi?,
    onAward: (amount: String, reason: String, takeAway: Boolean) -> String?,
    modifier: Modifier = Modifier,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    var action by rememberSaveable { mutableStateOf<PointsAction?>(null) }
    Column(modifier.fillMaxWidth()) {
        SectionHeader(title = "Points", eyebrow = "rewards")
        Spacer(Modifier.height(spacing.md))
        // Your own points first; theirs below, with + and - to award or take away.
        val mine = myRole ?: PointsRole.GoodBoy
        listOf(mine, mine.other).forEachIndexed { i, role ->
            val isMine = myRole != null && role == myRole
            val canAct = myRole != null && !isMine
            PointsRow(
                title = "${role.label} points",
                owner = when {
                    myRole == null -> null
                    isMine -> "you"
                    else -> partnerName ?: "them"
                },
                value = if (role == PointsRole.GoodBoy) points.goodBoy else points.goodGirl,
                accent = if (role == PointsRole.GoodBoy) colors.lavender else colors.pink,
                action = if (canAct) action else null,
                onAction = if (canAct) ({ picked -> action = if (action == picked) null else picked }) else null,
                modifier = Modifier.fillMaxWidth().testTag(if (role == PointsRole.GoodBoy) "points_good_boy" else "points_good_girl"),
            )
            if (i == 0) Spacer(Modifier.height(spacing.sm))
        }
        val current = action
        if (current != null && myRole != null) {
            Spacer(Modifier.height(spacing.sm))
            PointsForm(
                partnerName = partnerName ?: "them",
                role = myRole.other,
                takeAway = current == PointsAction.TakeAway,
                onSubmit = { amount, reason ->
                    onAward(amount, reason, current == PointsAction.TakeAway).also { if (it == null) action = null }
                },
                onCancel = { action = null },
            )
        }
        if (lastAward != null) {
            Spacer(Modifier.height(spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("last_award")) {
                Text(lastAward.text, style = MaterialTheme.typography.bodySmall, color = colors.textMain)
                Text(" \u00B7 ${lastAward.whenLabel}", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
            }
            if (lastAward.reason != null) {
                Text(lastAward.reason, style = ChoomHubTextStyles.Handwritten, color = colors.textSecondary)
            }
        }
    }
}

/** A short, wide box: name on the left, the total on the right, and (on theirs) bold + and - marks. */
@Composable
private fun PointsRow(
    title: String,
    owner: String?,
    value: Int,
    accent: Color,
    action: PointsAction?,
    onAction: ((PointsAction) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ChoomHubCard(
        modifier = modifier,
        tone = CardTone.Calm,
        contentPadding = PaddingValues(horizontal = ChoomHubTheme.spacing.lg, vertical = ChoomHubTheme.spacing.md),
    ) {
        // Same height whether or not the + and - marks are there, so both boxes match.
        Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = ChoomHubTheme.colors.textSecondary, maxLines = 1)
                if (owner != null) {
                    Text(owner, style = ChoomHubTextStyles.Handwritten, color = ChoomHubTheme.colors.textMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                // A real minus sign, so negative totals read cleanly.
                text = if (value < 0) "\u2212${-value}" else value.toString(),
                // Lining figures: Cormorant's default old-style numerals make "128" read like "I28".
                style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "lnum"),
                color = accent,
            )
            if (onAction != null) {
                Spacer(Modifier.width(ChoomHubTheme.spacing.sm))
                PointsMark(
                    icon = if (action == PointsAction.TakeAway) PointIcons.Close else PointIcons.Minus,
                    label = if (action == PointsAction.TakeAway) "Close" else "Take away points",
                    tint = accent,
                    onClick = { onAction(PointsAction.TakeAway) },
                    tag = "points_take_away",
                )
                PointsMark(
                    icon = if (action == PointsAction.Award) PointIcons.Close else PointIcons.Plus,
                    label = if (action == PointsAction.Award) "Close" else "Award points",
                    tint = accent,
                    onClick = { onAction(PointsAction.Award) },
                    tag = "points_award",
                )
            }
        }
    }
}

/** Just the mark, no circle; a comfortable 44dp tap area around it. */
@Composable
private fun PointsMark(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit, tag: String) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** How many points and why, inline under the boxes. */
@Composable
private fun PointsForm(
    partnerName: String,
    role: PointsRole,
    takeAway: Boolean,
    onSubmit: (amount: String, reason: String) -> String?,
    onCancel: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    var amount by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val submit = { error = onSubmit(amount, reason) }
    ChoomHubCard(modifier = Modifier.fillMaxWidth().testTag("award_form"), tone = CardTone.Raised) {
        Text(
            if (takeAway) "Take away $partnerName's ${role.label} points" else "Award $partnerName ${role.label} points",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textMain,
        )
        Spacer(Modifier.height(spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            listOf(1, 2, 3, 5).forEach { n ->
                val selected = amount == n.toString()
                PillBadge(
                    text = "$n",
                    color = if (selected) colors.pink else colors.textSecondary,
                    modifier = Modifier
                        .clickable(role = Role.RadioButton) { amount = n.toString(); error = null }
                        .testTag("award_amount_$n"),
                )
            }
        }
        Spacer(Modifier.height(spacing.sm))
        ChoomHubTextField(
            value = amount,
            onValueChange = { amount = it.filter(Char::isDigit).take(2); error = null },
            label = "How many (1\u2013${PointRules.MAX})",
            supportingText = error,
            isError = error != null,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth().testTag("award_amount"),
        )
        ChoomHubTextField(
            value = reason,
            onValueChange = { reason = it.take(PointRules.MAX_REASON) },
            label = "What for (optional)",
            capitalization = KeyboardCapitalization.Sentences,
            onDone = submit,
            modifier = Modifier.fillMaxWidth().testTag("award_reason"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                OneLineText("CANCEL", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
            TextButton(onClick = submit, modifier = Modifier.testTag("award_give")) {
                OneLineText(if (takeAway) "TAKE AWAY" else "AWARD", style = MaterialTheme.typography.labelLarge, color = colors.pink)
            }
        }
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
                        text = if (cat.isAdopted) "${cat.name} is at home" else "Unnamed, for now",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textMain,
                    )
                    Spacer(Modifier.height(ChoomHubTheme.spacing.xs))
                    Text(
                        text = cat.status ?: "A kitten turned up at the Pet House. Name it together there.",
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
                text = "Points and the cat are sample data for now. Everything else here is real.",
                style = MaterialTheme.typography.bodySmall,
                color = ChoomHubTheme.colors.textSecondary,
            )
        }
    }
}
