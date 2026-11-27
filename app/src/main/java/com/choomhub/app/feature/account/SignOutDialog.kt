package com.choomhub.app.feature.account

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.component.InkDialog
import com.choomhub.app.core.designsystem.illustration.GothicMotif
import com.choomhub.app.core.designsystem.illustration.GothicMotifArt

/** "Sign out?" before actually signing out. */
@Composable
fun SignOutDialog(
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
    message: String = "You'll stay in your space. Sign back in with the same Google account to pick up where you left off.",
) {
    InkDialog(
        title = "Sign out?",
        message = message,
        confirm = "Sign out",
        onConfirm = {
            onDismiss()
            onSignOut()
        },
        dismissive = "Stay",
        onDismissive = onDismiss,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("sign_out_dialog"),
    ) {
        GothicMotifArt(GothicMotif.SkeletonKey, Modifier.size(64.dp))
    }
}
