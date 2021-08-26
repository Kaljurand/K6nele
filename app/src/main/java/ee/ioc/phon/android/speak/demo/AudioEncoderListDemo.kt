package ee.ioc.phon.android.speak.demo

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NavUtils
import ee.ioc.phon.android.speak.activity.ui.theme.K6neleTheme
import ee.ioc.phon.android.speechutils.utils.AudioUtils

class AudioEncoderListDemo : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val upIntent = NavUtils.getParentActivityIntent(this)
        setContent {
            K6neleTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Content("Audio encoders", onBack = { NavUtils.navigateUpTo(this, upIntent!!) })
                }
            }
        }
    }
}

@Composable
fun Content(title: String, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(text = title) },
            navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(Icons.Filled.ArrowBack, "")
                }
            },
        )
    }) {
        Column {
            val mime = "audio/flac"
            ListItemDetail("$mime encoders: ${AudioUtils.getEncoderNamesForType(mime)}")
            Spacer(modifier = Modifier.height(4.dp))
            AudioEncoderList(AudioUtils.getAvailableEncoders(mime, 16000))
        }
    }
}

@Composable
fun AudioEncoderList(audioEncoders: List<String>) {
    LazyColumn {
        items(audioEncoders) { audioEncoder ->
            ListItemDetail(audioEncoder)
        }
    }
}

@Composable
fun ListItemDetail(text: String) {
    Text(
        modifier = Modifier.padding(8.dp),
        text = text
    )
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun DefaultPreview() {
    K6neleTheme {
        Surface(color = MaterialTheme.colors.background) {
            Content("Audio encoders", onBack = {})
        }
    }
}