package com.choomhub.app.feature.bounty

import com.choomhub.app.core.designsystem.component.OneLineText
import com.choomhub.app.core.designsystem.icon.PointIcons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import kotlin.random.Random

enum class BountyState { Open, Claimed, Paid, Lapsed }

/** A bounty as the board shows it. [forMe]: your partner posted it, for you to do. */
data class BountyUi(
    val id: String,
    val task: String,
    val reward: Int,
    val forMe: Boolean,
    val due: String?,
    val state: BountyState,
)

private val Paper = Color(0xFFD9CDB4)
private val PaperEdge = Color(0xFFB8A988)
private val InkBrown = Color(0xFF3A2A26)
private val Wine = Color(0xFF6E2638)
private val StampRed = Color(0xFFC8372E)
private val Brass = Color(0xFFB08A4A)

/** Dark planks behind the notices, for the full-screen board. */
fun Modifier.boardPlanks(): Modifier = drawBehind {
    drawRect(Color(0xFF241A18))
    val r = Random(11)
    var y = 0f
    while (y < size.height) {
        val hgt = 70f + r.nextFloat() * 30f
        drawRect(Color(0xFF2E221D).copy(alpha = 0.5f + r.nextFloat() * 0.5f), Offset(0f, y), Size(size.width, hgt - 4f))
        drawLine(Color(0xFF15100E), Offset(0f, y + hgt - 2f), Offset(size.width, y + hgt - 2f), 4f)
        repeat(3) {
            val gx = r.nextFloat() * size.width
            drawLine(Color(0xFF3A2A26).copy(alpha = 0.6f), Offset(gx, y + 10f), Offset(gx + 120f, y + 14f), 1.5f)
        }
        y += hgt
    }
}

/** One notice: torn parchment, a brass pin, the task in handwriting, the reward, and its state. */
@Composable
fun BountyNotice(
    bounty: BountyUi,
    partner: String,
    me: String,
    angle: Float,
    onDone: () -> Unit,
    onPay: () -> Unit,
    onNotYet: () -> Unit,
    modifier: Modifier = Modifier,
    onTakeDown: () -> Unit = {},
) {
    val faded = bounty.state == BountyState.Lapsed
    Box(
        modifier
            .rotate(angle)
            .drawBehind {
                val r = Random(bounty.id.hashCode())
                val torn = Path().apply {
                    moveTo(0f, 6f)
                    var x = 0f
                    while (x < size.width) {
                        lineTo(x, r.nextFloat() * 6f)
                        x += 14f
                    }
                    lineTo(size.width, 4f)
                    var yy = 0f
                    while (yy < size.height) {
                        lineTo(size.width - r.nextFloat() * 5f, yy)
                        yy += 18f
                    }
                    lineTo(size.width - 3f, size.height)
                    x = size.width
                    while (x > 0f) {
                        lineTo(x, size.height - r.nextFloat() * 7f)
                        x -= 12f
                    }
                    lineTo(2f, size.height - 3f)
                    close()
                }
                drawPath(torn, Color.Black.copy(alpha = 0.4f), style = Stroke(10f))
                drawPath(torn, Brush.verticalGradient(listOf(Paper, PaperEdge)))
                if (faded) drawPath(torn, Color(0xFF241A18).copy(alpha = 0.45f))
                // The pin.
                drawCircle(Color.Black.copy(alpha = 0.35f), 9f, Offset(size.width / 2f + 2f, 16f))
                drawCircle(Brass, 8f, Offset(size.width / 2f, 14f))
                drawCircle(Color(0xFFE6C36A), 3f, Offset(size.width / 2f - 2f, 12f))
            }
            .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 12.dp)
            .testTag("bounty_${bounty.id}"),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("BOUNTY", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp), color = Wine, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(bounty.task, style = ChoomHubTextStyles.Handwritten.copy(fontSize = 19.sp, lineHeight = 22.sp), color = InkBrown, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("${bounty.reward}", style = MaterialTheme.typography.displaySmall, color = Wine)
            Text("POINTS", style = ChoomHubTextStyles.Eyebrow, color = InkBrown)
            Spacer(Modifier.height(6.dp))
            Text(
                (if (bounty.forMe) "for $me · from $partner" else "for $partner · from you") + (bounty.due?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = InkBrown.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            when {
                bounty.state == BountyState.Open && bounty.forMe -> PaperButton("I DID IT", onDone, tag = "bounty_done_${bounty.id}")
                bounty.state == BountyState.Open -> PaperButton("TAKE DOWN", onTakeDown, quiet = true, tag = "bounty_take_down_${bounty.id}")
                bounty.state == BountyState.Claimed && bounty.forMe -> Text("waiting for $partner to pay", style = ChoomHubTextStyles.Handwritten, color = Wine)
                bounty.state == BountyState.Claimed -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaperButton("NOT YET", onNotYet, quiet = true, tag = "bounty_not_yet_${bounty.id}")
                    PaperButton("PAY ${bounty.reward}", onPay, tag = "bounty_pay_${bounty.id}")
                }
                bounty.state == BountyState.Lapsed && !bounty.forMe -> PaperButton("TAKE DOWN", onTakeDown, quiet = true, tag = "bounty_take_down_${bounty.id}")
                bounty.state == BountyState.Lapsed -> Text("lapsed", style = ChoomHubTextStyles.Handwritten, color = InkBrown.copy(alpha = 0.6f))
                else -> Spacer(Modifier.height(24.dp))
            }
        }
        if (bounty.state == BountyState.Paid) PaidStamp(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun PaperButton(text: String, onClick: () -> Unit, quiet: Boolean = false, tag: String? = null) {
    Box(
        Modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .border(1.5.dp, if (quiet) InkBrown.copy(alpha = 0.4f) else Wine, MaterialTheme.shapes.small)
            .background(if (quiet) Color.Transparent else Wine.copy(alpha = 0.12f), MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = if (quiet) InkBrown else Wine, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PaidStamp(modifier: Modifier) {
    Box(modifier.rotate(-16f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(128.dp, 54.dp)) {
            drawRoundRect(StampRed.copy(alpha = 0.85f), size = size, cornerRadius = CornerRadius(8f), style = Stroke(5f))
            drawRoundRect(StampRed.copy(alpha = 0.6f), Offset(9f, 9f), Size(size.width - 18f, size.height - 18f), CornerRadius(5f), style = Stroke(2f))
        }
        Text("PAID", color = StampRed.copy(alpha = 0.9f), fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
    }
}

/** Posting a bounty: what, how many points (1-20), and by when. */
@Composable
fun ColumnScope.PostBountyContent(
    task: String,
    onTask: (String) -> Unit,
    reward: Int,
    onReward: (Int) -> Unit,
    due: String,
    onDue: (String) -> Unit,
    partner: String,
    onPost: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Text("Post a bounty", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
    Text("for $partner to claim", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
    Spacer(Modifier.height(spacing.lg))
    ChoomHubTextField(task, onTask, label = "The job", supportingText = "${task.length}/80", modifier = Modifier.fillMaxWidth().testTag("bounty_task"))
    Spacer(Modifier.height(spacing.md))
    Text("REWARD", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Step(PointIcons.Minus, "Less") { onReward((reward - 1).coerceAtLeast(1)) }
        Text("$reward", style = MaterialTheme.typography.displaySmall, color = colors.candle)
        Step(PointIcons.Plus, "More") { onReward((reward + 1).coerceAtMost(20)) }
    }
    Text("points, paid when you agree it's done", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
    Spacer(Modifier.height(spacing.md))
    Text("BY", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
    Spacer(Modifier.height(spacing.xs))
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        listOf("Tonight", "This week", "Whenever").forEach { option ->
            val on = option == due
            Box(
                Modifier
                    .border(1.dp, if (on) colors.pink else colors.lavender.copy(alpha = 0.25f), MaterialTheme.shapes.small)
                    .background(if (on) colors.pink.copy(alpha = 0.14f) else Color.Transparent, MaterialTheme.shapes.small)
                    .clickable(role = Role.RadioButton) { onDue(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text(option, style = MaterialTheme.typography.labelLarge, color = if (on) colors.pink else colors.textSecondary) }
        }
    }
    Spacer(Modifier.height(spacing.lg))
    ChoomHubPrimaryButton("Pin it up", onPost, Modifier.fillMaxWidth(), enabled = task.isNotBlank())
    TextButton(onClose, Modifier.fillMaxWidth()) { OneLineText("NEVER MIND", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
}

@Composable
private fun Step(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Box(
        Modifier.size(44.dp).clip(CircleShape).border(1.dp, colors.lavender.copy(alpha = 0.5f), CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = label, tint = colors.textMain, modifier = Modifier.size(20.dp)) }
}

/** A hanging plank button for the board ("Post a bounty"). */
@Composable
fun PlankButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .drawBehind {
                drawLine(Color(0xFF15111A), Offset(size.width * 0.2f, -14f), Offset(size.width * 0.2f, 4f), 3f)
                drawLine(Color(0xFF15111A), Offset(size.width * 0.8f, -14f), Offset(size.width * 0.8f, 4f), 3f)
                drawRoundRect(Color(0xFF4D3830), cornerRadius = CornerRadius(6f))
                drawRoundRect(Color(0xFF241A18), cornerRadius = CornerRadius(6f), style = Stroke(3f))
                drawCircle(Brass, 4f, Offset(size.width * 0.2f, 10f))
                drawCircle(Brass, 4f, Offset(size.width * 0.8f, 10f))
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text.uppercase(), style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp), color = Color(0xFFE6DED0)) }
}

/** The bottom of the board when there's nothing pinned. */
@Composable
fun EmptyBoard(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Nothing pinned up. Post the first one.", style = ChoomHubTextStyles.Handwritten, color = Color(0xFFB9B2A6))
    }
}
