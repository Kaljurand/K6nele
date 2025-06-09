package ee.ioc.phon.android.speak.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ee.ioc.phon.android.speak.db.AppDatabase
import ee.ioc.phon.android.speak.db.ComboEntity

class ComboListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getDatabase(this).comboDao()
        setContent {
            val combos by dao.getAll().collectAsState(initial = emptyList())
            ComboListScreen(combos)
        }
    }
}

@Composable
fun ComboListScreen(combos: List<ComboEntity>) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn {
            items(combos, key = { it.id }) { combo ->
                Text(text = combo.longLabel)
            }
        }
    }
}
