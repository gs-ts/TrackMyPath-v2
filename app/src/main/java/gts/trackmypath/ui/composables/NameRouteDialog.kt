package gts.trackmypath.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gts.trackmypath.R
import gts.trackmypath.domain.route.ROUTE_NAME_MAX_LENGTH
import gts.trackmypath.domain.route.normalizeRouteName
import gts.trackmypath.ui.theme.TrackMyPathV2Theme

/**
 * Asks for a route name.
 *
 * With [onDismissClick], the dialog shows a Dismiss button, and Back or a tap outside also dismiss it.
 * Without it, Back and a tap outside do nothing, so the user must choose one of the buttons.
 * With [onDiscardClick], the dialog shows a Discard button that deletes the route, and a warning that says so.
 */
@Composable
internal fun NameRouteDialog(
    routeName: TextFieldState,
    onConfirmClick: () -> Unit,
    onDismissClick: (() -> Unit)?,
    onDiscardClick: (() -> Unit)? = null,
) {
    // Recomposes the Confirm button only when the name becomes valid or invalid, not on every keystroke.
    val isRouteNameValid by remember(routeName) {
        derivedStateOf { normalizeRouteName(routeName.text.toString()) != null }
    }
    Dialog(
        onDismissRequest = { onDismissClick?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismissClick != null,
            dismissOnClickOutside = onDismissClick != null,
        ),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(size = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    textAlign = TextAlign.Center,
                    text = "Give a name to your route",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .padding(bottom = 16.dp),
                )
                if (onDiscardClick != null) {
                    DiscardRouteWarning()
                }
                OutlinedTextField(
                    state = routeName,
                    modifier = Modifier
                        .fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    // Rejects typing or pasting past the limit, and tells accessibility services the limit.
                    inputTransformation = InputTransformation.maxLength(ROUTE_NAME_MAX_LENGTH),
                    label = {
                        Text(text = "Name")
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (onDiscardClick != null) {
                        TextButton(
                            onClick = onDiscardClick,
                            modifier = Modifier.padding(8.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.name_route_dialog_discard))
                        }
                    }
                    if (onDismissClick != null) {
                        TextButton(
                            onClick = onDismissClick,
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text("Dismiss")
                        }
                    }
                    TextButton(
                        onClick = { onConfirmClick() },
                        modifier = Modifier.padding(8.dp),
                        enabled = isRouteNameValid,
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscardRouteWarning() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(R.drawable.warning_icon),
            tint = MaterialTheme.colorScheme.outline,
            contentDescription = "warning"
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            style = MaterialTheme.typography.labelMedium,
            text = stringResource(R.string.name_route_dialog_discard_warning),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@PreviewLightDark
@Composable
private fun NameRouteDialogPreview() {
    TrackMyPathV2Theme {
        Box(modifier = Modifier.fillMaxSize()) {
            NameRouteDialog(
                routeName = rememberTextFieldState(initialText = "my route"),
                onConfirmClick = {},
                onDismissClick = null,
                onDiscardClick = {},
            )
        }
    }
}
