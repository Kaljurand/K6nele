package ee.ioc.phon.android.speak.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ee.ioc.phon.android.speak.model.ServiceEntity
import ee.ioc.phon.android.speak.viewmodel.ServicePickerViewModel

@Composable
fun ServicePickerScreen(
    viewModel: ServicePickerViewModel,
    onServiceSelected: (ServiceEntity) -> Unit,
    onConfigureService: (ServiceEntity) -> Unit
) {
    val services = viewModel.services.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Recognition Services") })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(services.value.size) { idx ->
                val service = services.value[idx]
                ServiceListItem(
                    service = service,
                    onClick = { onServiceSelected(service) },
                    onConfigure = { onConfigureService(service) }
                )
            }
        }
    }
}

@Composable
fun ServiceListItem(
    service: ServiceEntity,
    onClick: () -> Unit,
    onConfigure: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO: Load icon from service.iconRes
        Icon(Icons.Default.Settings, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(service.name, fontWeight = FontWeight.Bold)
            service.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        TextButton(onClick = onConfigure) {
            Text("Configure")
        }
    }
}
