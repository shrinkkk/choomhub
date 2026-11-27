package com.choomhub.app.feature.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.choomhub.app.BuildConfig
import com.choomhub.app.R
import com.choomhub.app.core.designsystem.component.CardTone
import com.choomhub.app.core.designsystem.component.ChoomHubCard
import com.choomhub.app.core.designsystem.component.ChoomHubTextField
import com.choomhub.app.core.designsystem.component.PillBadge
import com.choomhub.app.core.designsystem.component.InkTypeToConfirmDialog
import com.choomhub.app.core.designsystem.component.SectionHeader
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.notifications.AppNotifications
import com.choomhub.app.domain.model.InviteCodes
import com.choomhub.app.domain.model.NotificationCategory
import com.choomhub.app.domain.model.PushAvailability
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.accountOrNull
import com.choomhub.app.domain.model.CoupleNames
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen(contentPadding: PaddingValues, onEditMoods: () -> Unit = {}) {
    composable<SettingsRoute> { SettingsScreen(contentPadding, onEditMoods) }
}

@Composable
fun SettingsScreen(contentPadding: PaddingValues, onEditMoods: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val names by viewModel.names.collectAsStateWithLifecycle()
    val space by viewModel.space.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val leave by viewModel.leave.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Whether this phone lets the app post notifications; re-checked whenever you come back.
    var canPost by remember { mutableStateOf(AppNotifications.canPost(context)) }
    LifecycleResumeEffect(Unit) {
        canPost = AppNotifications.canPost(context)
        onPauseOrDispose { }
    }
    var deniedOnce by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canPost = AppNotifications.canPost(context)
        if (!granted) deniedOnce = true
    }

    SettingsContent(
        names = names,
        space = space,
        contentPadding = contentPadding,
        onRenamePartner = viewModel::renamePartner,
        onShareCode = { code -> context.shareInviteCode(code) },
        onNewCode = viewModel::newInviteCode,
        onSignOut = viewModel::signOut,
        focusPoints = { FocusPointsGroup() },
        notifications = notifications,
        canPostNotifications = canPost,
        onNotificationPermission = {
            val askable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPost && !deniedOnce
            if (askable) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // After a refusal (or to manage channels) only the system screen can change it.
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        },
        onCategoryToggle = viewModel::setNotificationsEnabled,
        onSendTest = viewModel::sendTestNotification,
        onEditMoods = onEditMoods,
        leave = leave,
        onLeave = viewModel::leaveSpace,
        onLeaveDialogClosed = viewModel::clearLeaveError,
    )
}

/**
 * Settings shows names (editable) and the honest status of the app: what is
 * connected, what is not, and what is still to come.
 */
@Composable
fun SettingsContent(
    names: CoupleNames,
    contentPadding: PaddingValues,
    onRenamePartner: (String) -> String?,
    space: SpaceState? = SpaceState.DeviceOnly,
    onShareCode: (String) -> Unit = {},
    onNewCode: () -> Unit = {},
    onSignOut: () -> Unit = {},
    notifications: NotificationsUi = NotificationsUi(),
    canPostNotifications: Boolean = true,
    onNotificationPermission: () -> Unit = {},
    onCategoryToggle: (NotificationCategory, Boolean) -> Unit = { _, _ -> },
    onSendTest: () -> Unit = {},
    onEditMoods: () -> Unit = {},
    focusPoints: @Composable () -> Unit = {},
    leave: LeaveState = LeaveState.Idle,
    onLeave: () -> Unit = {},
    onLeaveDialogClosed: () -> Unit = {},
) {
    val spacing = ChoomHubTheme.spacing
    val colors = ChoomHubTheme.colors
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmingLeave by rememberSaveable { mutableStateOf(false) }
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }
    // Once you've left, the space is gone from here; close the confirmation.
    if (confirmingLeave && space !is SpaceState.Waiting && space !is SpaceState.Paired) confirmingLeave = false

    Box(Modifier.fillMaxSize().testTag("screen_settings"), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Settings", style = MaterialTheme.typography.displaySmall, color = colors.textMain)
                    Text("the quiet corner", style = ChoomHubTextStyles.Handwritten, color = colors.pink)
                }
                GothicMotifArt(GothicMotif.Candle, Modifier.size(64.dp))
            }

            SettingsGroup("Names") {
                if (renaming) {
                    InlineRename(
                        current = names.partnerName.orEmpty(),
                        onSave = onRenamePartner,
                        onClose = { renaming = false },
                    )
                } else {
                    SettingsRow(
                        title = "You call them",
                        value = names.partnerName ?: "Not set",
                        badge = "edit",
                        onClick = { renaming = true },
                        modifier = Modifier.testTag("settings_partner_name"),
                    )
                }
                SettingsRow(
                    title = "Points",
                    value = names.myRole?.let { "You're the ${it.label} \u00B7 ${names.partnerName ?: "they"} ${if (names.partnerName != null) "is" else "are"} the ${it.other.label}" }
                        ?: "Not chosen yet",
                )
                SettingsRow(
                    title = "They call you",
                    value = names.myName ?: "They pick this on their phone. It shows up once you're connected.",
                    divider = false,
                )
            }

            SettingsGroup("Moods") {
                SettingsRow(
                    title = "Mood list",
                    value = "Add your own, remove any. Shared by both of you.",
                    badge = "edit",
                    onClick = onEditMoods,
                    divider = false,
                    modifier = Modifier.testTag("settings_mood_list"),
                )
            }

            focusPoints()

            SettingsGroup("Notifications") {
                NotificationSettings(
                    ui = notifications,
                    canPost = canPostNotifications,
                    onPermission = onNotificationPermission,
                    onToggle = onCategoryToggle,
                    onSendTest = onSendTest,
                )
            }

            SettingsGroup("Your space") {
                when (space) {
                    is SpaceState.Waiting -> {
                        if (space.inviteCode.isBlank()) {
                            SettingsRow("Waiting for them", "Making a new invite code\u2026", badge = "new", onClick = onNewCode, modifier = Modifier.testTag("settings_invite"))
                        } else {
                            SettingsRow(
                                title = "Waiting for them to join",
                                value = "Invite code ${InviteCodes.display(space.inviteCode)} \u00B7 works once, for 48 hours",
                                badge = "share",
                                onClick = { onShareCode(space.inviteCode) },
                                modifier = Modifier.testTag("settings_invite"),
                            )
                            SettingsRow("New code", "If the old one expired or got lost", badge = "new", onClick = onNewCode)
                        }
                    }
                    is SpaceState.Paired -> SettingsRow("Shared space", "Connected. Everything here syncs between your phones.", badge = "synced", badgeColor = colors.leaf)
                    SpaceState.DeviceOnly, null -> SettingsRow(
                        "Shared space",
                        "Not set up in this build \u00B7 everything stays on this phone",
                        badge = "device",
                        badgeColor = colors.textSecondary,
                    )
                    else -> SettingsRow("Shared space", "Not connected yet", badge = "later")
                }
                val account = space?.accountOrNull
                if (account != null) {
                    SettingsRow(
                        title = "Signed in",
                        value = account.email ?: "Google account",
                        badge = "sign out",
                        badgeColor = colors.textSecondary,
                        onClick = { confirmingSignOut = true },
                        divider = space !is SpaceState.Waiting && space !is SpaceState.Paired,
                    )
                }
                if (space is SpaceState.Waiting || space is SpaceState.Paired) {
                    SettingsRow(
                        title = "Leave this space",
                        value = if (space is SpaceState.Paired) {
                            "You can come back with a code from ${names.partnerName ?: "them"} and pick up where you left off"
                        } else {
                            "You're the only one in it, so leaving deletes everything in it for good"
                        },
                        badge = "leave",
                        badgeColor = colors.tomato,
                        onClick = { confirmingLeave = true },
                        divider = false,
                        modifier = Modifier.testTag("settings_leave"),
                    )
                }
            }

            SettingsGroup("Appearance") {
                SettingsRow("Theme", "Always dark, by design")
                SettingsRow("Sky", "Follows the time of day: dawn, day, dusk, night")
                SettingsRow("Motion", "Follows your system “remove animations” setting", divider = false)
            }

            SettingsGroup("About") {
                SettingsRow(stringResource(R.string.app_name), "Version ${BuildConfig.VERSION_NAME}")
                SettingsRow("Fonts", "Cormorant Garamond and Space Mono, SIL Open Font License", divider = false)
            }
            Spacer(Modifier.height(spacing.lg))
        }
    }
    if (confirmingSignOut) {
        com.choomhub.app.feature.account.SignOutDialog(onSignOut = onSignOut, onDismiss = { confirmingSignOut = false })
    }
    if (confirmingLeave) {
        val partner = names.partnerName ?: "them"
        val close = {
            confirmingLeave = false
            onLeaveDialogClosed()
        }
        InkTypeToConfirmDialog(
            title = "Leave this space?",
            message = if (space is SpaceState.Paired) {
                "$partner stays, and your points and everything else wait here. Rejoin with a code from $partner (any Google account) to pick up where you left off. If you both leave, it's all deleted for good."
            } else {
                "You're the only one in it, so everything in it (snaps, points, the cat) is deleted for good."
            },
            phrase = "I hate $partner",
            confirm = "Leave",
            onConfirm = onLeave,
            onDismissRequest = { if (leave != LeaveState.Leaving) close() },
            busy = leave == LeaveState.Leaving,
            error = (leave as? LeaveState.Failed)?.message,
            modifier = Modifier.testTag("leave_dialog"),
        ) {
            GothicMotifArt(GothicMotif.SkeletonKey, Modifier.size(72.dp))
        }
    }
}

/** Permission, per-category switches and the test button. */
@Composable
private fun NotificationSettings(
    ui: NotificationsUi,
    canPost: Boolean,
    onPermission: () -> Unit,
    onToggle: (NotificationCategory, Boolean) -> Unit,
    onSendTest: () -> Unit,
) {
    val colors = ChoomHubTheme.colors
    if (ui.availability == PushAvailability.DeviceOnly) {
        SettingsRow(
            title = "Notifications",
            value = "Need the shared space \u00B7 this build keeps everything on this phone",
            badge = "device",
            badgeColor = colors.textSecondary,
            divider = false,
        )
        return
    }
    SettingsRow(
        title = if (canPost) "On for this phone" else "Off on this phone",
        value = if (canPost) "Sounds and channels are in Android's settings" else "Turn them on to hear when they change something",
        badge = if (canPost) "system" else "turn on",
        badgeColor = if (canPost) colors.textSecondary else colors.lavender,
        onClick = onPermission,
        modifier = Modifier.testTag("notifications_permission"),
    )
    NotificationCategory.entries.forEach { category ->
        val enabled = ui.prefs.isEnabled(category)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch) { onToggle(category, !enabled) }
                .padding(vertical = ChoomHubTheme.spacing.md)
                .testTag("notifications_${category.key}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(category.label, style = MaterialTheme.typography.titleSmall, color = colors.textMain)
                Text(category.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Spacer(Modifier.size(ChoomHubTheme.spacing.sm))
            Switch(
                checked = enabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.night,
                    checkedTrackColor = colors.lavender,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.night,
                    uncheckedBorderColor = colors.outline,
                ),
            )
        }
        HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))
    }
    when (ui.availability) {
        PushAvailability.NotSetUp -> SettingsRow(
            title = "Push service",
            value = "Not set up in this build yet \u00B7 see docs/NOTIFICATIONS_SETUP.md",
            badge = "setup",
            badgeColor = colors.textSecondary,
            divider = false,
        )
        else -> {
            val test = ui.test
            SettingsRow(
                title = "Send a test",
                value = when (test) {
                    TestSendState.Idle -> "Sends a notification to this phone"
                    TestSendState.Sending -> "Sending\u2026"
                    is TestSendState.Done -> test.message
                },
                badge = if (test == TestSendState.Sending) "\u2026" else "send",
                badgeColor = if (test is TestSendState.Done && !test.ok) colors.pink else colors.lavender,
                onClick = onSendTest.takeIf { test != TestSendState.Sending },
                divider = false,
                modifier = Modifier.testTag("notifications_test"),
            )
        }
    }
}

/** Edits the partner's name in place, inside the Names card. */
@Composable
private fun InlineRename(
    current: String,
    onSave: (String) -> String?,
    onClose: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }
    val colors = ChoomHubTheme.colors
    val save = {
        error = onSave(input)
        if (error == null) onClose()
    }
    Column(Modifier.fillMaxWidth().padding(vertical = ChoomHubTheme.spacing.md)) {
        ChoomHubTextField(
            value = input,
            onValueChange = {
                input = it
                error = null
            },
            label = "You call them",
            supportingText = error ?: "They'll see this as their name once you're connected.",
            isError = error != null,
            onDone = save,
            modifier = Modifier.fillMaxWidth().testTag("rename_field"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClose) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
            TextButton(onClick = save, modifier = Modifier.testTag("rename_save")) {
                Text("SAVE", style = MaterialTheme.typography.labelLarge, color = colors.lavender)
            }
        }
    }
    HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))
}

@Composable
internal fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        SectionHeader(title = title)
        Spacer(Modifier.height(ChoomHubTheme.spacing.sm))
        ChoomHubCard(
            modifier = Modifier.fillMaxWidth(),
            tone = CardTone.Calm,
            contentPadding = PaddingValues(horizontal = ChoomHubTheme.spacing.lg, vertical = ChoomHubTheme.spacing.xs),
        ) {
            content()
        }
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColor: Color = ChoomHubTheme.colors.lavender,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = ChoomHubTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = ChoomHubTheme.colors.textMain)
            Text(value, style = MaterialTheme.typography.bodySmall, color = ChoomHubTheme.colors.textSecondary)
        }
        if (badge != null) {
            Spacer(Modifier.size(ChoomHubTheme.spacing.sm))
            PillBadge(badge, color = badgeColor)
        }
    }
    if (divider) HorizontalDivider(color = ChoomHubTheme.colors.outline.copy(alpha = 0.4f))
}

/** Opens the system share sheet with the invite code. */
private fun android.content.Context.shareInviteCode(code: String) {
    val text = "Join me on ChoomHub. Invite code: ${InviteCodes.display(code)} (works once, for 48 hours)"
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
    startActivity(android.content.Intent.createChooser(send, "Share invite code"))
}
