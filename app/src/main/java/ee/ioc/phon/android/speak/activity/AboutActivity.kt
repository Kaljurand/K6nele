/*
 * Copyright 2021, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.speak.activity

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NavUtils
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.activity.ui.theme.K6neleTheme
import ee.ioc.phon.android.speak.utils.Utils
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils

// TODO: show scrollbars
// TODO: review if BACK navigation is done correctly
// TODO: add more space between HTML <p>
// TODO: fix use of Theme (in both Light and Dark mode)
// TODO: hide developer menu by default, but show it if the user has clicked on
// some part of the About text for 5 times
class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val upIntent = NavUtils.getParentActivityIntent(this)

        val title = "Kõnele" + " v" + Utils.getVersionName(this)

        val about: String = String.format(
            getString(R.string.tvAbout),
            getString(R.string.labelApp),
            PreferenceUtils.getUniqueId(PreferenceManager.getDefaultSharedPreferences(this))
        )

        setContent {
            K6neleTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Content(title = title, text = about,
                        onBack = { NavUtils.navigateUpTo(this, upIntent!!) })
                }
            }
        }
    }
}

@Composable
fun Content(title: String, text: String, onBack: () -> Unit) {
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
        ScrollBoxesSmooth(text)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    K6neleTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
            Content(
                title = "Kõnele\nv1.2.03",
                text = "<p>First line contains <b>bold</b> and <i>italic</i>.</p>" +
                        "<p>Second line contains a <a href='http://example.org'>link</a>.</p>",
                onBack = {}
            )
        }
    }

}

@Composable
private fun AboutText(description: String) {
    // Remembers the HTML formatted description. Re-executes on a new description
    val htmlDescription = remember(description) {
        HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    // Displays the TextView on the screen and updates with the HTML description when inflated
    // Updates to htmlDescription will make AndroidView recompose and update the text
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = {
            it.text = htmlDescription
            it.textSize = 16.0F
        }
    )
}

@Composable
private fun ScrollBoxesSmooth(text: String) {

    val state = rememberScrollState()
    // Smoothly scroll 100px on first composition
    //LaunchedEffect(Unit) { state.animateScrollTo(100) }

    Column(
        modifier = Modifier
            .padding(6.dp)
            // TODO: how to add scrollbars
            .indication(state.interactionSource, indication = null)
            .verticalScroll(state)
    ) {
        AboutText(text)
    }
}