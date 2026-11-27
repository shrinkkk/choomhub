package com.choomhub.app.feature.shop

import com.choomhub.app.core.designsystem.component.TabLabel
import com.choomhub.app.core.designsystem.component.OneLineText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubPrimaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubSecondaryButton
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.component.InkDialog
import com.choomhub.app.core.designsystem.component.InkMenu
import com.choomhub.app.core.designsystem.component.InkMenuOption
import com.choomhub.app.core.designsystem.component.InkPanel
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.ProposalKind
import com.choomhub.app.domain.model.Purchase
import com.choomhub.app.domain.model.PurchaseState
import com.choomhub.app.domain.model.Reward
import com.choomhub.app.domain.model.ShopProposal
import com.choomhub.app.domain.model.ShopRules
import com.choomhub.app.feature.pet.PetIcons
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data object PointsShopRoute

fun NavGraphBuilder.pointsShopScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    composable<PointsShopRoute> { PointsShopScreen(contentPadding, onBack) }
}

@Composable
fun PointsShopScreen(contentPadding: PaddingValues, onBack: () -> Unit, viewModel: ShopViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    PointsShopContent(
        ui = ui,
        message = message,
        contentPadding = contentPadding,
        actions = ShopActions(
            back = onBack,
            buy = viewModel::buy,
            use = viewModel::use,
            giveBack = viewModel::giveBack,
            markDone = viewModel::markDone,
            suggestReward = viewModel::suggestReward,
            suggestPrice = viewModel::suggestPrice,
            suggestRemove = viewModel::suggestRemove,
            accept = viewModel::accept,
            dismiss = viewModel::dismiss,
            clearMessage = viewModel::clearMessage,
        ),
    )
}

/** Everything the shop screen can do (a view model in the app, fakes in tests). */
class ShopActions(
    val back: () -> Unit = {},
    val buy: (Reward) -> Boolean = { true },
    val use: (Purchase) -> Unit = {},
    val giveBack: (Purchase) -> Unit = {},
    val markDone: (Purchase) -> Unit = {},
    val suggestReward: (String, String) -> String? = { _, _ -> null },
    val suggestPrice: (Reward, String) -> String? = { _, _ -> null },
    val suggestRemove: (Reward) -> Unit = {},
    val accept: (ShopProposal) -> Unit = {},
    val dismiss: (ShopProposal) -> Unit = {},
    val clearMessage: () -> Unit = {},
)

private enum class ShopTab { Shop, Yours, Theirs }

/** A pop-up the shop has open. */
private sealed interface ShopDialog {
    data class Buy(val reward: Reward) : ShopDialog
    data class Options(val reward: Reward) : ShopDialog
    data class Price(val reward: Reward) : ShopDialog
    data class Remove(val reward: Reward) : ShopDialog
    data object Suggest : ShopDialog
    data class Use(val purchase: Purchase) : ShopDialog
    data class GiveBack(val purchase: Purchase) : ShopDialog
}

@Composable
internal fun PointsShopContent(ui: ShopUi, message: String?, contentPadding: PaddingValues, actions: ShopActions) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    var tab by rememberSaveable { mutableStateOf(ShopTab.Shop) }
    var dialog by androidx.compose.runtime.remember { mutableStateOf<ShopDialog?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            delay(4_000)
            actions.clearMessage()
        }
    }
    val owedToThem = ui.purchases.count { !it.byMe && it.state == PurchaseState.Used }
    val waitingOnMe = ui.proposals.count { !it.byMe }
    Box(Modifier.fillMaxSize().testTag("screen_shop"), contentAlignment = Alignment.TopCenter) {
        val scroll = key(tab) { rememberScrollState() }
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Back") { actions.back() }.testTag("shop_back"),
                    contentAlignment = Alignment.Center,
                ) { Icon(PetIcons.Back, contentDescription = "Back", tint = colors.textMain) }
                Spacer(Modifier.width(spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text("Points shop", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
                    Text("spend what you've earned", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
                }
                GothicMotifArt(GothicMotif.Candle, Modifier.size(52.dp))
            }
            Spacer(Modifier.height(spacing.lg))
            ChoomHubCard(Modifier.fillMaxWidth(), tone = CardTone.Raised) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("YOU HAVE", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary)
                        Text("${ui.myPoints}", style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "lnum"), color = colors.candle, modifier = Modifier.testTag("shop_my_points"))
                        Text(ui.myPointsLabel, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(ui.partnerName.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textFaint)
                        Text("${ui.partnerPoints}", style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "lnum"), color = colors.textSecondary)
                        Text(ui.partnerPointsLabel, style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
                    }
                }
            }
            Spacer(Modifier.height(spacing.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                TabChip("Shop", tab == ShopTab.Shop, waitingOnMe, Modifier.weight(1f).testTag("shop_tab_shop")) { tab = ShopTab.Shop }
                TabChip("Yours", tab == ShopTab.Yours, 0, Modifier.weight(1f).testTag("shop_tab_yours")) { tab = ShopTab.Yours }
                TabChip(possessive(ui.partnerName), tab == ShopTab.Theirs, owedToThem, Modifier.weight(1f).testTag("shop_tab_theirs")) { tab = ShopTab.Theirs }
            }
            Text(
                message ?: " ",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.candle,
                minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm).testTag("shop_message"),
            )
            when (tab) {
                ShopTab.Shop -> ShopTabContent(ui, actions) { dialog = it }
                ShopTab.Yours -> YoursTab(ui) { dialog = it }
                ShopTab.Theirs -> TheirsTab(ui, actions)
            }
            Spacer(Modifier.height(spacing.xxl))
        }
    }
    ShopDialogs(dialog, ui, actions) { dialog = it }
}

private fun possessive(name: String) = if (name == "them") "Theirs" else "$name's"

@Composable
private fun TabChip(label: String, selected: Boolean, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Box(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.pink.copy(alpha = 0.14f) else colors.surface)
            .border(1.dp, if (selected) colors.pink.copy(alpha = 0.6f) else colors.lavender.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = ChoomHubTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        TabLabel(label, badge, if (selected) colors.pink else colors.textSecondary)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(ChoomHubTheme.spacing.md))
    Text(text.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = ChoomHubTheme.colors.textSecondary)
    Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
}

// --- Shop ---

@Composable
private fun ShopTabContent(ui: ShopUi, actions: ShopActions, open: (ShopDialog) -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val adds = ui.proposals.filter { it.kind == ProposalKind.Add }
    if (adds.isNotEmpty()) {
        SectionLabel("Custom rewards waiting for a yes")
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            adds.forEach { p ->
                ProposalCard(
                    p,
                    ui,
                    title = p.name.orEmpty(),
                    detail = "${p.price} points · requested by ${if (p.byMe) "you" else ui.partnerName}",
                    actions = actions,
                )
            }
        }
    }
    SectionLabel("Rewards")
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        ui.catalogue.forEach { reward ->
            val pending = ShopRules.pendingFor(reward.id, ui.proposals)
            ChoomHubCard(Modifier.fillMaxWidth().testTag("reward_${reward.id}"), tone = CardTone.Calm, contentPadding = PaddingValues(spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(reward.name, style = MaterialTheme.typography.titleSmall, color = colors.textMain)
                        Text(
                            "${reward.price} points" + if (reward.custom) " · one of yours" else "",
                            style = ChoomHubTextStyles.Eyebrow,
                            color = colors.textFaint,
                        )
                    }
                    Spacer(Modifier.width(spacing.sm))
                    TextButton(
                        onClick = { open(ShopDialog.Options(reward)) },
                        enabled = pending == null,
                        modifier = Modifier.testTag("reward_edit_${reward.id}"),
                    ) { OneLineText("EDIT", style = MaterialTheme.typography.labelLarge, color = if (pending == null) colors.textSecondary else colors.textFaint) }
                    ChoomHubPrimaryButton("Buy", { open(ShopDialog.Buy(reward)) }, Modifier.testTag("buy_${reward.id}"), enabled = ui.myPoints >= reward.price)
                }
                if (pending != null) {
                    Spacer(Modifier.height(spacing.sm))
                    PendingChange(pending, ui, actions)
                }
            }
        }
    }
    Spacer(Modifier.height(spacing.lg))
    ChoomHubSecondaryButton("Suggest a reward", { open(ShopDialog.Suggest) }, Modifier.fillMaxWidth().testTag("shop_suggest"))
    Text(
        "New rewards, new prices and taking things out all need ${ui.partnerName} to agree.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
    )
}

/** A suggested price change or removal, shown on the reward it's about. */
@Composable
private fun PendingChange(p: ShopProposal, ui: ShopUi, actions: ShopActions) {
    val colors = ChoomHubTheme.colors
    val who = if (p.byMe) "You" else ui.partnerName
    val text = when (p.kind) {
        ProposalKind.Price -> "$who suggested a new price: ${p.price} points"
        ProposalKind.Remove -> "$who suggested taking this out of the shop"
        ProposalKind.Add -> ""
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.candle.copy(alpha = 0.07f))
            .padding(ChoomHubTheme.spacing.sm)
            .testTag("pending_${p.rewardId}"),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.candle)
        ProposalButtons(p, ui, actions)
    }
}

@Composable
private fun ProposalCard(p: ShopProposal, ui: ShopUi, title: String, detail: String, actions: ShopActions) {
    val colors = ChoomHubTheme.colors
    ChoomHubCard(Modifier.fillMaxWidth().testTag("proposal_${p.id}"), tone = CardTone.Glowing, glowColor = colors.candle, contentPadding = PaddingValues(ChoomHubTheme.spacing.md)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textMain)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        ProposalButtons(p, ui, actions)
    }
}

@Composable
private fun ProposalButtons(p: ShopProposal, ui: ShopUi, actions: ShopActions) {
    val colors = ChoomHubTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        if (p.byMe) {
            Text("Waiting for ${ui.partnerName}", style = MaterialTheme.typography.bodySmall, color = colors.textFaint, modifier = Modifier.weight(1f))
            TextButton({ actions.dismiss(p) }, Modifier.testTag("proposal_withdraw_${p.id}")) {
                OneLineText("TAKE BACK", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
        } else {
            TextButton({ actions.dismiss(p) }, Modifier.testTag("proposal_decline_${p.id}")) {
                OneLineText("NO", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
            TextButton({ actions.accept(p) }, Modifier.testTag("proposal_accept_${p.id}")) {
                OneLineText("AGREE", style = MaterialTheme.typography.labelLarge, color = colors.pink)
            }
        }
    }
}

// --- Yours: what you bought ---

@Composable
private fun YoursTab(ui: ShopUi, open: (ShopDialog) -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    val mine = ui.purchases.filter { it.byMe && it.state != PurchaseState.Returned }
    if (mine.isEmpty()) {
        EmptyLine("Nothing bought yet. What you buy waits here until you use it.")
        return
    }
    val ready = mine.filter { it.state == PurchaseState.Owned }
    val cashed = mine.filter { it.state == PurchaseState.Used }
    val done = mine.filter { it.state == PurchaseState.Done }
    if (ready.isNotEmpty()) {
        SectionLabel("Ready to use")
        ready.forEach { p ->
            PurchaseRow(p, "Bought for ${p.price}") {
                TextButton({ open(ShopDialog.GiveBack(p)) }, Modifier.testTag("give_back_${p.id}")) {
                    OneLineText("GIVE BACK", style = MaterialTheme.typography.labelLarge, color = colors.textFaint)
                }
                ChoomHubPrimaryButton("Use it", { open(ShopDialog.Use(p)) }, Modifier.testTag("use_${p.id}"))
            }
        }
    }
    if (cashed.isNotEmpty()) {
        SectionLabel("Cashed in, ${ui.partnerName}'s turn")
        cashed.forEach { p -> PurchaseRow(p, "Waiting for ${ui.partnerName} to do it") {} }
    }
    if (done.isNotEmpty()) {
        SectionLabel("Used")
        done.forEach { p -> PurchaseRow(p, "Done", faded = true) {} }
    }
    Spacer(Modifier.height(spacing.sm))
}

// --- Theirs: what they bought from you ---

@Composable
private fun TheirsTab(ui: ShopUi, actions: ShopActions) {
    val spacing = ChoomHubTheme.spacing
    val theirs = ui.purchases.filter { !it.byMe && it.state != PurchaseState.Returned }
    if (theirs.isEmpty()) {
        EmptyLine("${ui.partnerName.replaceFirstChar { it.uppercase() }} hasn't bought anything yet. When they do, it shows here.")
        return
    }
    val cashed = theirs.filter { it.state == PurchaseState.Used }
    val owned = theirs.filter { it.state == PurchaseState.Owned }
    val done = theirs.filter { it.state == PurchaseState.Done }
    if (cashed.isNotEmpty()) {
        SectionLabel("Cashed in: your turn")
        cashed.forEach { p ->
            PurchaseRow(p, "They want it now", highlight = true) {
                ChoomHubPrimaryButton("Done", { actions.markDone(p) }, Modifier.testTag("done_${p.id}"))
            }
        }
    }
    if (owned.isNotEmpty()) {
        SectionLabel("They own, not used yet")
        owned.forEach { p -> PurchaseRow(p, "Bought for ${p.price}") {} }
    }
    if (done.isNotEmpty()) {
        SectionLabel("Used")
        done.forEach { p -> PurchaseRow(p, "Done", faded = true) {} }
    }
    Spacer(Modifier.height(spacing.sm))
}

@Composable
private fun EmptyLine(text: String) {
    Spacer(Modifier.height(ChoomHubTheme.spacing.lg))
    Text(text, style = MaterialTheme.typography.bodyMedium, color = ChoomHubTheme.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PurchaseRow(p: Purchase, line: String, faded: Boolean = false, highlight: Boolean = false, buttons: @Composable () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    ChoomHubCard(
        Modifier.fillMaxWidth().padding(bottom = spacing.sm).testTag("purchase_${p.id}"),
        tone = if (highlight) CardTone.Glowing else CardTone.Calm,
        glowColor = colors.candle,
        contentPadding = PaddingValues(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleSmall, color = if (faded) colors.textFaint else colors.textMain)
                Text(line, style = MaterialTheme.typography.bodySmall, color = if (highlight) colors.candle else colors.textFaint)
            }
            buttons()
        }
    }
}

// --- Pop-ups ---

@Composable
private fun ShopDialogs(dialog: ShopDialog?, ui: ShopUi, actions: ShopActions, set: (ShopDialog?) -> Unit) {
    val close = { set(null) }
    when (dialog) {
        null -> Unit
        is ShopDialog.Buy -> InkDialog(
            title = "Buy it?",
            message = "${dialog.reward.name}, for ${dialog.reward.price} of your ${ui.myPoints} ${ui.myPointsLabel}. ${ui.partnerName.replaceFirstChar { it.uppercase() }} owes it to you when you use it.",
            confirm = "Buy",
            onConfirm = {
                actions.buy(dialog.reward)
                close()
            },
            dismissive = "Never mind",
            onDismissive = close,
            onDismissRequest = close,
            modifier = Modifier.testTag("dialog_buy"),
        ) { GothicMotifArt(GothicMotif.Candle, Modifier.size(64.dp)) }
        is ShopDialog.Options -> InkMenu(
            title = dialog.reward.name,
            whisper = "${dialog.reward.price} points",
            options = listOf(
                InkMenuOption("Change the price", "reward_change_price") { set(ShopDialog.Price(dialog.reward)) },
                InkMenuOption("Take it out of the shop", "reward_remove") { set(ShopDialog.Remove(dialog.reward)) },
            ),
            onDismissRequest = close,
        ) { GothicMotifArt(GothicMotif.SkeletonKey, Modifier.size(56.dp)) }
        is ShopDialog.Price -> PriceDialog(dialog.reward, ui.partnerName, onSubmit = { actions.suggestPrice(dialog.reward, it) }, onClose = close)
        is ShopDialog.Remove -> InkDialog(
            title = "Take it out?",
            message = "${dialog.reward.name} comes out of the shop once ${ui.partnerName} agrees. Anything already bought stays.",
            confirm = "Suggest it",
            onConfirm = {
                actions.suggestRemove(dialog.reward)
                close()
            },
            dismissive = "Never mind",
            onDismissive = close,
            onDismissRequest = close,
        ) { GothicMotifArt(GothicMotif.Cobweb, Modifier.size(64.dp)) }
        ShopDialog.Suggest -> SuggestDialog(ui.partnerName, onSubmit = actions.suggestReward, onClose = close)
        is ShopDialog.Use -> InkDialog(
            title = "Use it now?",
            message = "${ui.partnerName.replaceFirstChar { it.uppercase() }} gets told you're cashing in ${dialog.purchase.name}.",
            confirm = "Use it",
            onConfirm = {
                actions.use(dialog.purchase)
                close()
            },
            dismissive = "Not yet",
            onDismissive = close,
            onDismissRequest = close,
            modifier = Modifier.testTag("dialog_use"),
        ) { GothicMotifArt(GothicMotif.Moth, Modifier.size(64.dp)) }
        is ShopDialog.GiveBack -> InkDialog(
            title = "Give it back?",
            message = "You get the ${dialog.purchase.price} points back for ${dialog.purchase.name}.",
            confirm = "Give it back",
            onConfirm = {
                actions.giveBack(dialog.purchase)
                close()
            },
            dismissive = "Keep it",
            onDismissive = close,
            onDismissRequest = close,
            modifier = Modifier.testTag("dialog_give_back"),
        ) { GothicMotifArt(GothicMotif.Raven, Modifier.size(64.dp)) }
    }
}

@Composable
private fun PriceDialog(reward: Reward, partner: String, onSubmit: (String) -> String?, onClose: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    var price by rememberSaveable { mutableStateOf(reward.price.toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val submit = { error = onSubmit(price); if (error == null) onClose() }
    InkPanel(onClose, Modifier.testTag("dialog_price")) {
        Text("New price", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        Text(reward.name, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.lg))
        ChoomHubTextField(
            value = price,
            onValueChange = { v -> price = v.filter { it.isDigit() }.take(3) },
            label = "Points",
            supportingText = error ?: "Changes once $partner agrees",
            isError = error != null,
            keyboardType = KeyboardType.Number,
            onDone = submit,
            modifier = Modifier.fillMaxWidth().testTag("price_field"),
        )
        Spacer(Modifier.height(spacing.lg))
        ChoomHubPrimaryButton("Suggest", submit, Modifier.fillMaxWidth().testTag("dialog_confirm"))
        TextButton(onClose, Modifier.fillMaxWidth()) { OneLineText("NEVER MIND", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
    }
}

@Composable
private fun SuggestDialog(partner: String, onSubmit: (String, String) -> String?, onClose: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val submit = { error = onSubmit(name, price); if (error == null) onClose() }
    InkPanel(onClose, Modifier.testTag("dialog_suggest")) {
        GothicMotifArt(GothicMotif.Candle, Modifier.size(56.dp))
        Spacer(Modifier.height(spacing.sm))
        Text("Suggest a reward", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
        Text("goes in the shop once $partner agrees", style = ChoomHubTextStyles.Handwritten, color = colors.pink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.lg))
        ChoomHubTextField(
            value = name,
            onValueChange = { name = it.take(ShopRules.MAX_NAME) },
            label = "Reward",
            supportingText = "${name.length}/${ShopRules.MAX_NAME}",
            modifier = Modifier.fillMaxWidth().testTag("suggest_name"),
        )
        Spacer(Modifier.height(spacing.sm))
        ChoomHubTextField(
            value = price,
            onValueChange = { v -> price = v.filter { it.isDigit() }.take(3) },
            label = "Price in points",
            supportingText = error,
            isError = error != null,
            keyboardType = KeyboardType.Number,
            onDone = submit,
            modifier = Modifier.fillMaxWidth().testTag("suggest_price"),
        )
        Spacer(Modifier.height(spacing.lg))
        ChoomHubPrimaryButton("Suggest", submit, Modifier.fillMaxWidth().testTag("dialog_confirm"), enabled = name.isNotBlank() && price.isNotBlank())
        TextButton(onClose, Modifier.fillMaxWidth()) { OneLineText("NEVER MIND", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
    }
}
