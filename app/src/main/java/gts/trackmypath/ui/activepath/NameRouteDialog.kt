package gts.trackmypath.ui.activepath

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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import gts.trackmypath.R
import gts.trackmypath.ui.theme.TrackMyPathV2Theme

@Composable
internal fun NameRouteDialog(
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
                        text = "Dismiss will not save your route.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    singleLine = true,
                    value = routeName,
                    onValueChange = { newValue ->
                        if (newValue.length <= 50) { // 50 chars limit
                            onRouteNameChange(newValue)
                        }
                    },
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

@PreviewLightDark
@Composable
private fun NameRouteDialogPreview() {
    TrackMyPathV2Theme {
        Box (modifier = Modifier.fillMaxSize()){
            NameRouteDialog(
                routeName = "my route",
                onRouteNameChange = {},
                onConfirmClick = {},
                onDismissClick = {},
            )
        }
    }
}
