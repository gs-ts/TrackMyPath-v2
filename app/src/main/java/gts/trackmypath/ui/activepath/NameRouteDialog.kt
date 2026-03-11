package gts.trackmypath.ui.activepath

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun NameRouteDialog(
    routeName: String,
    onRouteNameChange: (String) -> Unit,
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
                    text = "Give a name to your route:",
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )
                OutlinedTextField( // TODO: add size limit
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    value = routeName,
                    onValueChange = { onRouteNameChange(it) },
                    label = {
                        Text(text = "Name")
                    }
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
                        enabled = routeName.isNotBlank(),
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
private fun NameRouteDialogPreview() {
    NameRouteDialog(
        routeName = "my route",
        onRouteNameChange = {},
        onConfirmClick = {},
        onDismissClick = {},
    )
}
