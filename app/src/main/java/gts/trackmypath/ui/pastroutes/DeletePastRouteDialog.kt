package gts.trackmypath.ui.pastroutes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import gts.trackmypath.ui.theme.TrackMyPathV2Theme

@Composable
internal fun DeletePastRouteDialog(
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit,
) {
    Dialog(onDismissRequest = { onDismissClick() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(size = 16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Are you sure you want to delete this route?",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissClick() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = { onConfirmClick() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DeletePastRouteDialogPreview() {
    TrackMyPathV2Theme {
        DeletePastRouteDialog(
            onConfirmClick = {},
            onDismissClick = {},
        )
    }
}
