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

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.ImageButton
import ee.ioc.phon.android.speak.R


/**
 * Simple button that lets you start speech recording/recognizing.
 * After it is finished you are taken immediately to the search results
 * that match the recognized words.
 *
 * @author Kaarel Kaljurand
 */
class VoiceSearchDemo : AbstractRecognizerDemoActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.voice_search_demo)
        val speakButton = findViewById(R.id.buttonMicrophone) as ImageButton
        speakButton.setOnClickListener({ launchRecognizerIntent(createVoiceAppSearchIntent()) })
    }

    override fun onSuccess(intent: Intent?) {
        finish()
    }

    /**
     *
     * Create and return an Intent that can launch the voice search activity, perform a specific
     * voice transcription, and forward the results to the searchable activity.
     *
     * @return A completely-configured intent ready to send to the voice search activity
     */
    private fun createVoiceAppSearchIntent(): Intent {
        // Create the necessary intent to set up a search-and-forward operation
        // in the voice search system. We have to keep the bundle separate,
        // because it becomes immutable once it enters the PendingIntent.
        val queryIntent = Intent(Intent.ACTION_SEARCH)
        val pending = PendingIntent.getActivity(this, 0, queryIntent, PendingIntent.FLAG_ONE_SHOT)

        // Now set up the bundle that will be inserted into the pending intent
        // when it is time to do the search. We always build it here (even if empty)
        // because the voice search activity will always need to insert "QUERY" into
        // it anyway.
        val queryExtras = Bundle()

        val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        //voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, (String) null);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        // Add the values that configure forwarding the results
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, pending)
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE, queryExtras)

        return voiceIntent
    }
}