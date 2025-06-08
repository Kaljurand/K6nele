package ee.ioc.phon.android.speak.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ee.ioc.phon.android.speak.model.ComboEntity
import ee.ioc.phon.android.speak.viewmodel.ComboDetailsViewModel

@Composable
fun ComboDetailsScreen(
    viewModel: ComboDetailsViewModel,
    onSave: (ComboEntity) -> Unit,
    onConfigureService: () -> Unit
) {
    val comboState = viewModel.combo.collectAsState()
    val combo = comboState.value ?: return

    var shortLabel by remember { mutableStateOf(TextFieldValue(combo.shortLabel)) }
    var longLabel by remember { mutableStateOf(TextFieldValue(combo.longLabel)) }
    var tinyLabel by remember { mutableStateOf(TextFieldValue(combo.tinyLabel)) }
    var inputLanguage by remember { mutableStateOf(TextFieldValue(combo.inputLanguage ?: "")) }
    var enabled by remember { mutableStateOf(combo.enabled) }
    var extras by remember { mutableStateOf(TextFieldValue(combo.extras ?: "")) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Combo Details") }, actions = {
                IconButton(onClick = onConfigureService) {
                    Icon(Icons.Default.Settings, contentDescription = "Configure Service")
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val updatedCombo = combo.copy(
                    shortLabel = shortLabel.text,
                    longLabel = longLabel.text,
                    tinyLabel = tinyLabel.text,
                    inputLanguage = inputLanguage.text,
                    enabled = enabled,
                    extras = extras.text
                )
                onSave(updatedCombo)
            }) {
                Icon(Icons.Default.Settings, contentDescription = "Save")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = shortLabel,
                onValueChange = { shortLabel = it },
                label = { Text("Short Label") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = longLabel,
                onValueChange = { longLabel = it },
                label = { Text("Long Label") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = tinyLabel,
                onValueChange = { tinyLabel = it },
                label = { Text("Tiny Label") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = inputLanguage,
                onValueChange = { inputLanguage = it },
                label = { Text("Input Language") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("Enabled")
            }
            OutlinedTextField(
                value = extras,
                onValueChange = { extras = it },
                label = { Text("EXTRAs (JSON)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
