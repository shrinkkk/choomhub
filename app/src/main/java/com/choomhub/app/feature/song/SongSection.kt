package com.choomhub.app.feature.song

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.component.SectionHeader
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.pet.art.PetCat
import com.choomhub.app.feature.pet.art.PetOutfit
import com.choomhub.app.feature.pet.art.PetPose

/** The song your partner sent you, as Home shows it. */
data class SongUi(
    val title: String,
    val artist: String?,
    /** The album cover, once loaded; null shows a plain sleeve. */
    val cover: ImageBitmap?,
    /** "2h ago". */
    val sentLabel: String,
    /** Not played or rated yet. */
    val isNew: Boolean,
    /** What you rated it, 1-10. */
    val myRating: Int?,
)

/** The song you sent them, and what they made of it. */
data class SentSongUi(val title: String, val rating: Int?)

private val StampRed = Color(0xFFE0453A)
private val Vinyl = Color(0xFF0C0A0E)

/** Below the snaps: their latest song, a play button, your rating, and sending one back. */
@Composable
fun SongSection(
    song: SongUi?,
    sent: SentSongUi?,
    partnerName: String?,
    onPlay: () -> Unit,
    onRate: (Int) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val them = partnerName ?: "them"
    Column(modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Latest song",
            eyebrow = if (song != null) "from $them" else "songs",
            trailing = if (song?.isNew == true) {
                { PillBadge(text = "new", color = colors.pink, showDot = true) }
            } else {
                null
            },
        )
        Spacer(Modifier.height(spacing.md))
        ChoomHubCard(
            modifier = Modifier.fillMaxWidth().testTag("song_card"),
            tone = if (song?.isNew == true) CardTone.Glowing else CardTone.Raised,
            glowColor = colors.pink,
        ) {
            if (song == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sleeve(null, null, Modifier.size(84.dp))
                    Spacer(Modifier.width(spacing.lg))
                    Column(Modifier.weight(1f)) {
                        Text("No song from $them yet", style = MaterialTheme.typography.titleMedium, color = colors.textMain)
                        Text(
                            "Each of you has one song at a time. Sending a new one replaces your last.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sleeve(song.cover, song.myRating, Modifier.size(132.dp).testTag("song_cover"))
                    Spacer(Modifier.width(spacing.lg + 14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium, color = colors.textMain, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (song.artist != null) {
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("sent ${song.sentLabel}", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
                        Spacer(Modifier.height(spacing.md))
                        PlayButton(onPlay)
                    }
                }
                Spacer(Modifier.height(spacing.lg))
                Text(
                    if (song.myRating == null) "RATE IT" else "YOUR RATING · TAP TO CHANGE",
                    style = ChoomHubTextStyles.Eyebrow,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(spacing.sm))
                RatingRow(song.myRating, onRate)
            }
            Spacer(Modifier.height(spacing.md))
            HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        sent == null -> "Send one from Spotify: Share → ChoomHub"
                        sent.rating == null -> "You sent “${sent.title}” · not rated yet"
                        else -> "You sent “${sent.title}” · $them gave it ${sent.rating}/10"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sent?.rating != null) colors.candle else colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("song_sent_line"),
                )
                TextButton(onClick = onSend, modifier = Modifier.testTag("song_send")) {
                    Text("SEND A SONG", style = MaterialTheme.typography.labelLarge, color = colors.lavender)
                }
            }
        }
    }
}

/** The album cover on its sleeve, with a record peeking out, and the rating stamped on it. */
@Composable
internal fun Sleeve(cover: ImageBitmap?, rating: Int?, modifier: Modifier) {
    val colors = ChoomHubTheme.colors
    Box(modifier) {
        // The record, slid half out to the right.
        Canvas(Modifier.fillMaxSize().offset(x = 14.dp).padding(4.dp)) {
            val r = size.minDimension / 2f
            drawCircle(Vinyl, r, center)
            drawCircle(colors.lavender.copy(alpha = 0.28f), r - 0.5f, center, style = Stroke(1.5f))
            for (i in 1..4) drawCircle(Color.White.copy(alpha = 0.05f), r * (0.45f + i * 0.12f), center, style = Stroke(1f))
            drawArc(Color.White.copy(alpha = 0.08f), -60f, 50f, false, Offset(center.x - r * 0.8f, center.y - r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(2f))
            drawCircle(StampRed.copy(alpha = 0.85f), r * 0.28f, center)
            drawCircle(Vinyl, r * 0.05f, center)
        }
        Box(
            Modifier
                .fillMaxSize()
                .shadow(10.dp, RoundedCornerShape(3.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.surfaceHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(cover, contentDescription = "Album cover", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                // No cover: an ink note on a plain sleeve.
                Canvas(Modifier.fillMaxSize(0.42f)) {
                    val w = size.width
                    val stroke = w * 0.07f
                    drawCircle(colors.lavender.copy(alpha = 0.5f), w * 0.16f, Offset(w * 0.24f, w * 0.8f))
                    drawCircle(colors.lavender.copy(alpha = 0.5f), w * 0.16f, Offset(w * 0.76f, w * 0.68f))
                    drawLine(colors.lavender.copy(alpha = 0.5f), Offset(w * 0.38f, w * 0.8f), Offset(w * 0.38f, w * 0.12f), stroke)
                    drawLine(colors.lavender.copy(alpha = 0.5f), Offset(w * 0.9f, w * 0.68f), Offset(w * 0.9f, 0f), stroke)
                    drawLine(colors.lavender.copy(alpha = 0.5f), Offset(w * 0.38f, w * 0.12f), Offset(w * 0.9f, 0f), stroke * 1.6f)
                }
            }
        }
        if (rating != null) {
            RatingStamp(rating, Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 8.dp).testTag("song_stamp"))
        }
    }
}

/** A rubber stamp: "8/10" in a double ring, pressed on at an angle. */
@Composable
private fun RatingStamp(rating: Int, modifier: Modifier) {
    Box(modifier.size(62.dp).rotate(-14f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(StampRed.copy(alpha = 0.18f), r, center)
            drawCircle(StampRed.copy(alpha = 0.9f), r - 2f, center, style = Stroke(5f))
            drawCircle(StampRed.copy(alpha = 0.9f), r - 11f, center, style = Stroke(2f))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$rating", color = StampRed, fontSize = 22.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Text("/10", color = StampRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun PlayButton(onPlay: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Row(
        Modifier
            .clip(CircleShape)
            .background(colors.bone)
            .clickable(role = Role.Button, onClickLabel = "Play on Spotify", onClick = onPlay)
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
            .testTag("song_play"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(22.dp)) {
            drawCircle(colors.night, size.minDimension / 2f)
            val p = Path().apply {
                moveTo(size.width * 0.4f, size.height * 0.28f)
                lineTo(size.width * 0.74f, size.height * 0.5f)
                lineTo(size.width * 0.4f, size.height * 0.72f)
                close()
            }
            drawPath(p, colors.bone)
        }
        Spacer(Modifier.width(8.dp))
        Text("PLAY ON SPOTIFY", style = MaterialTheme.typography.labelLarge, color = colors.night)
    }
}

/** Ten little circles, 1-10; yours is filled in. */
@Composable
private fun RatingRow(rating: Int?, onRate: (Int) -> Unit) {
    val colors = ChoomHubTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (n in 1..10) {
            val picked = rating == n
            Box(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (picked) StampRed else Color.Transparent)
                    .border(1.dp, if (picked) StampRed else colors.lavender.copy(alpha = 0.3f), CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Rate it $n") { onRate(n) }
                    .testTag("song_rate_$n"),
                contentAlignment = Alignment.Center,
            ) {
                Text("$n", style = MaterialTheme.typography.labelMedium, color = if (picked) colors.bone else colors.textSecondary)
            }
        }
    }
}

// --- Sending ---

/** Where the send pop-up is at. */
sealed interface SendSongState {
    /** Waiting for a link (pasted, or shared in from Spotify). */
    data class Link(val text: String = "", val error: String? = null) : SendSongState

    data object Looking : SendSongState

    /** Found it: what's about to be sent. */
    data class Found(val title: String, val artist: String?, val cover: ImageBitmap?) : SendSongState
}

/** The cat, in headphones, nodding along to something. */
@Composable
fun VibingCat(modifier: Modifier = Modifier) {
    val colors = ChoomHubTheme.colors
    val animate = LocalAmbientMotionEnabled.current
    val bob = if (animate) {
        val t = rememberInfiniteTransition(label = "vibe")
        t.animateFloat(-1f, 1f, infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse), label = "bob").value
    } else {
        -0.6f
    }
    val drift = if (animate) {
        val t = rememberInfiniteTransition(label = "notes")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing)), label = "drift").value
    } else {
        0.3f
    }
    Box(modifier) {
        PetCat(
            PetPose.Sit,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = bob * 7f
                    translationY = -kotlin.math.abs(bob) * 3f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.95f)
                },
            expression = CatExpression.Ecstatic,
            outfit = PetOutfit.Headphones,
        )
        // Notes drifting up off the headphones.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            listOf(0f to colors.pink, 0.5f to colors.candle).forEachIndexed { i, (phase, color) ->
                val p = (drift + phase) % 1f
                val x = w * (if (i == 0) 0.86f else 0.12f) + (if (i == 0) 1 else -1) * p * w * 0.06f
                val y = w * 0.42f - p * w * 0.34f
                val a = (1f - p).coerceIn(0f, 1f)
                val s = w * 0.035f
                drawCircle(color.copy(alpha = a), s, Offset(x, y))
                drawLine(color.copy(alpha = a), Offset(x + s * 0.9f, y), Offset(x + s * 0.9f, y - s * 3.4f), s * 0.45f, StrokeCap.Round)
                drawLine(color.copy(alpha = a), Offset(x + s * 0.9f, y - s * 3.4f), Offset(x + s * 2.2f, y - s * 2.6f), s * 0.45f, StrokeCap.Round)
            }
        }
    }
}

/** What the send pop-up shows (inside an ink panel). */
@Composable
fun ColumnScope.SendSongContent(
    state: SendSongState,
    partnerName: String,
    onLinkChange: (String) -> Unit,
    onFind: () -> Unit,
    onSend: () -> Unit,
    onDifferent: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    VibingCat(Modifier.size(120.dp))
    Spacer(Modifier.height(spacing.sm))
    Text("Send a song", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
    Text("it replaces the last one you sent", style = ChoomHubTextStyles.Handwritten, color = colors.pink, textAlign = TextAlign.Center)
    Spacer(Modifier.height(spacing.lg))
    when (state) {
        is SendSongState.Link -> {
            ChoomHubTextField(
                value = state.text,
                onValueChange = onLinkChange,
                label = "Spotify link",
                supportingText = state.error ?: "Or in Spotify: Share → ChoomHub",
                isError = state.error != null,
                onDone = onFind,
                modifier = Modifier.fillMaxWidth().testTag("song_link"),
            )
            Spacer(Modifier.height(spacing.lg))
            ChoomHubPrimaryButton("Find it", onFind, Modifier.fillMaxWidth().testTag("song_find"), enabled = state.text.isNotBlank())
        }
        SendSongState.Looking -> {
            Text("Looking it up…", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(vertical = spacing.lg))
        }
        is SendSongState.Found -> {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.surface)
                    .padding(spacing.md)
                    .testTag("song_preview"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(3.dp)).background(colors.surfaceHighest)) {
                    if (state.cover != null) Image(state.cover, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(state.title, style = MaterialTheme.typography.titleSmall, color = colors.textMain, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (state.artist != null) Text(state.artist, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
                }
            }
            Spacer(Modifier.height(spacing.lg))
            ChoomHubPrimaryButton("Send to $partnerName", onSend, Modifier.fillMaxWidth().testTag("song_confirm"))
            TextButton(onDifferent, Modifier.fillMaxWidth()) {
                Text("A DIFFERENT SONG", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
        }
    }
    TextButton(onClose, Modifier.fillMaxWidth()) {
        Text("NEVER MIND", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
    }
}

/** The song card with its view model, and the send pop-up when it's open. */
@Composable
fun SongHomeSection(modifier: Modifier = Modifier, viewModel: SongViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val sending by viewModel.sending.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    SongSection(
        song = ui.song,
        sent = ui.sent,
        partnerName = ui.partnerName,
        onPlay = {
            viewModel.play()?.let { url ->
                try {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                } catch (e: android.content.ActivityNotFoundException) {
                    android.widget.Toast.makeText(context, "Couldn't open Spotify on this phone.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        },
        onRate = viewModel::rate,
        onSend = viewModel::openSend,
        modifier = modifier,
    )
    sending?.let { state ->
        com.choomhub.app.core.designsystem.component.InkPanel(viewModel::close, Modifier.testTag("song_send_dialog")) {
            SendSongContent(
                state = state,
                partnerName = ui.partnerName ?: "them",
                onLinkChange = viewModel::linkChanged,
                onFind = viewModel::find,
                onSend = viewModel::send,
                onDifferent = viewModel::different,
                onClose = viewModel::close,
            )
        }
    }
}
