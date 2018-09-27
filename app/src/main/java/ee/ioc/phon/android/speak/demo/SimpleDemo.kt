/*
 * Copyright 2011, Institute of Cybernetics at Tallinn University of Technology
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

package ee.ioc.phon.android.speak.demo

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.ArrayAdapter
import android.widget.ListView
import ee.ioc.phon.android.speak.R

/**
 * This is a simple activity that calls the RecognizerIntent.ACTION_RECOGNIZE_SPEECH
 * and populates a list view with the returned extra (EXTRA_RESULTS)
 * (i.e. the words/phrases that alternatively match the input speech).
 * (Note that using ACTION_WEB_SEARCH here would forward the results to an external
 * app, instead of returning them.)
 *
 * @author Kaarel Kaljurand
 */
class SimpleDemo : AbstractRecognizerDemoActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_demo)

        val intent = createRecognizerIntent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        if (getRecognizers(intent).size == 0) {
            toast(getString(R.string.errorRecognizerNotPresent))
            finish()
        } else {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            launchRecognizerIntent(intent)
        }
    }

    /**
     * Show the results i.e. an ordered list of transcription candidates.
     */
    override fun onSuccess(intent: Intent?) {
        val matches = intent?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        (findViewById(R.id.list_matches) as ListView).adapter =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, matches)
    }
}