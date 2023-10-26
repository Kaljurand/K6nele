package ee.ioc.phon.android.speak.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import ee.ioc.phon.android.speak.database.Item
import ee.ioc.phon.android.speak.viewmodel.ItemViewModel

@AndroidEntryPoint
class ServiceListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }

    @Composable
    fun MainScreen(viewModel: ItemViewModel = viewModel()) {
        //val viewModel: ItemViewModel = viewModel()
        //val items by viewModel.items.collectAsState(initial = emptyList())

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    viewModel.addItem(Item(shortLabel = "New", longLabel = "New Item"))
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    ItemRow(item) { clickedItem ->
                        startActivity(
                            Intent(
                                this@ServiceListActivity,
                                ServiceDetailActivity::class.java
                            ).apply {
                                putExtra("ITEM_ID", clickedItem.id)
                            })
                    }
                }
            }
        }
    }


    @Composable
    fun ItemRow(item: Item, onItemClick: (Item) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(item) }
                .padding(16.dp)
        ) {
            Text(text = item.shortLabel, Modifier.weight(1f))
            Text(text = item.longLabel, Modifier.weight(2f))
        }
    }
}