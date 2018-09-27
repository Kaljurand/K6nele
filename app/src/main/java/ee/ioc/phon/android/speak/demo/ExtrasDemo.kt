/*
 * Copyright 2014, Institute of Cybernetics at Tallinn University of Technology
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

import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.activity.DetailsActivity
import ee.ioc.phon.android.speechutils.Extras
import ee.ioc.phon.android.speechutils.utils.BundleUtils

class ExtrasDemo : AbstractRecognizerDemoActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = createRecognizerIntent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        if (getRecognizers(intent).size == 0) {
            toast(getString(R.string.errorRecognizerNotPresent))
            finish()
        } else {
            intent.putExtra(Extras.EXTRA_RETURN_ERRORS, true)
            intent.putExtra(Extras.EXTRA_GET_AUDIO, true)
            intent.putExtra(Extras.EXTRA_GET_AUDIO_FORMAT, null as String?)
            //intent.putExtra(Extras.EXTRA_GET_AUDIO_FORMAT, Constants.SUPPORTED_AUDIO_FORMATS.iterator().next());
            launchRecognizerIntent(intent)
        }
    }

    /**
     * Show the resulting extras
     */
    override fun onSuccess(intent: Intent?) {
        val list = BundleUtils.ppBundle(intent?.extras)
        val details = Intent(this, DetailsActivity::class.java)
        details.data = intent?.data
        details.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, list.toTypedArray())
        startActivity(details)
        finish()
    }
}