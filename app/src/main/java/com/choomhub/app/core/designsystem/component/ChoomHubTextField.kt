package com.choomhub.app.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/** Single-line text input styled for the dark theme. */
@Composable
fun ChoomHubTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    onDone: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    singleLine: Boolean = true,
) {
    val colors = ChoomHubTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label.uppercase(), style = MaterialTheme.typography.labelSmall) },
        supportingText = supportingText?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        isError = isError,
        singleLine = singleLine,
        // Longer notes read better a size down.
        textStyle = if (singleLine) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(
            capitalization = if (keyboardType == KeyboardType.Number) KeyboardCapitalization.None else capitalization,
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textMain,
            unfocusedTextColor = colors.textMain,
            focusedBorderColor = colors.lavender.copy(alpha = 0.7f),
            unfocusedBorderColor = colors.outline,
            focusedLabelColor = colors.lavender,
            unfocusedLabelColor = colors.textSecondary,
            cursorColor = colors.lavender,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            errorBorderColor = colors.tomato,
            errorLabelColor = colors.tomato,
            errorSupportingTextColor = colors.tomato,
            focusedSupportingTextColor = colors.textSecondary,
            unfocusedSupportingTextColor = colors.textFaint,
        ),
    )
}
