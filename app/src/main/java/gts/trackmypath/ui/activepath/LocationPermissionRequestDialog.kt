package gts.trackmypath.ui.activepath

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPermissionRequestDialog(
    onConfirmClick: () -> Unit,
    onDismissRequest: () -> Unit,
    isUpgradeFromCoarseToFine: Boolean,
) {
    BasicAlertDialog(
        content = {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isUpgradeFromCoarseToFine) "Precise Location Required" else "Location Required",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isUpgradeFromCoarseToFine) {
                            "Track My Path cannot function with approximate location. Please upgrade to precise location to track your route."
                        } else {
                            "Track My Path requires precise location permissions to function."
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = onDismissRequest,
                        ) {
                            Text("Dismiss")
                        }
                        TextButton(
                            onClick = onConfirmClick,
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
