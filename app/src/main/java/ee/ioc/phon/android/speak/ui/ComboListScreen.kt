package ee.ioc.phon.android.speak.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ee.ioc.phon.android.speak.model.ComboEntity
import ee.ioc.phon.android.speak.viewmodel.ComboListViewModel

@Composable
fun ComboListScreen(
    viewModel: ComboListViewModel,
    onComboClick: (ComboEntity) -> Unit,
    onCloneCombo: (ComboEntity) -> Unit,
    onRemoveCombo: (ComboEntity) -> Unit,
    onAddCombo: () -> Unit,
    onLoadCombos: () -> Unit,
    onSaveCombos: () -> Unit
) {
    val combos = viewModel.combos.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Current Combos") }, actions = {
                IconButton(onClick = onAddCombo) { Icon(Icons.Default.Add, contentDescription = "Add") }
                IconButton(onClick = onLoadCombos) { Icon(Icons.Default.FileUpload, contentDescription = "Load") }
                IconButton(onClick = onSaveCombos) { Icon(Icons.Default.Save, contentDescription = "Save") }
            })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(combos.value.size) { idx ->
                val combo = combos.value[idx]
                ComboListItem(
                    combo = combo,
                    onClick = { onComboClick(combo) },
                    onClone = { onCloneCombo(combo) },
                    onRemove = { onRemoveCombo(combo) }
                )
            }
        }
    }
}

@Composable
fun ComboListItem(
    combo: ComboEntity,
    onClick: () -> Unit,
    onClone: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (combo.enabled) Color.Transparent else Color.LightGray)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO: Load service icon
        Icon(Icons.Default.Edit, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(combo.shortLabel, fontWeight = FontWeight.Bold)
            Text(combo.longLabel, style = MaterialTheme.typography.bodySmall)
            combo.inputLanguage?.let { Text("Lang: $it", style = MaterialTheme.typography.bodySmall) }
        }
        IconButton(onClick = onClone) { Icon(Icons.Default.Add, contentDescription = "Clone") }
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
    }
}
