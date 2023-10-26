package ee.ioc.phon.android.speak.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import ee.ioc.phon.android.speak.viewmodel.ItemViewModel

@AndroidEntryPoint
class ServiceDetailActivity : ComponentActivity() {
    private val viewModel: ItemViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getIntExtra("ITEM_ID", -1)
        setContent {
            EditScreen(itemId)
        }
    }

    @Composable
    fun EditScreen(itemId: Int) {
        val item by viewModel.getItemById(itemId).collectAsState(initial = null)

        if (item != null) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = item.shortLabel,
                    onValueChange = { newLabel ->
                        viewModel.updateItem(item.copy(shortLabel = newLabel))
                    },
                    label = { Text("Short Label") }
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = item.longLabel,
                    onValueChange = { newLabel ->
                        viewModel.updateItem(item.copy(longLabel = newLabel))
                    },
                    label = { Text("Long Label") }
                )
                // Add key-value pairs UI here
            }
        }
    }
}