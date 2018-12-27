/*
 * Copyright 2011-2018, Institute of Cybernetics at Tallinn University of Technology
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

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.activity.GrammarListActivity
import ee.ioc.phon.android.speak.provider.Grammar
import ee.ioc.phon.android.speak.utils.Utils
import ee.ioc.phon.android.speechutils.Extras
import java.util.*

/**
 * This demo shows how to create an input to RecognizerIntent.ACTION_RECOGNIZE_SPEECH
 * and how to respond to its output (list of matched words or an error code). This is
 * an abstract class, the UI part is in the extensions of this class.
 *
 * @author Kaarel Kaljurand
 */
abstract class AbstractRecognizerDemoActivity : Activity() {

    private var mGrammarId: Long = 0


    private val grammarName: String?
        get() = Utils.idToValue(this, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.NAME, mGrammarId)


    private val grammarUrl: String?
        get() = Utils.idToValue(this, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.URL, mGrammarId)


    private val grammarTargetLang: String?
        get() = Utils.idToValue(this, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.LANG, mGrammarId)

    protected abstract fun onSuccess(intent: Intent?)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.demo_grammar, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuDemoGrammarAssign -> {
                val intent = Intent(this@AbstractRecognizerDemoActivity, GrammarListActivity::class.java)
                startActivityForResult(intent, ACTIVITY_SELECT_GRAMMAR_URL)
                return true
            }
            else -> return super.onContextItemSelected(item)
        }
    }

    protected open fun onCancel() {
        toast(getString(R.string.errorResultCanceled))
        finish()
    }

    protected open fun onError(errorCode: Int) {
        toast(getErrorMessage(errorCode))
        finish()
    }

    protected fun getErrorMessage(errorCode: Int): String {
        when (errorCode) {
            RecognizerIntent.RESULT_AUDIO_ERROR -> return getString(R.string.errorResultAudioError)
            RecognizerIntent.RESULT_CLIENT_ERROR -> return getString(R.string.errorResultClientError)
            RecognizerIntent.RESULT_NETWORK_ERROR -> return getString(R.string.errorResultNetworkError)
            RecognizerIntent.RESULT_SERVER_ERROR -> return getString(R.string.errorResultServerError)
            RecognizerIntent.RESULT_NO_MATCH -> return getString(R.string.errorResultNoMatch)
            else -> return "Unknown error"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTIVITY_SELECT_GRAMMAR_URL) {
            if (resultCode != Activity.RESULT_OK) {
                return
            }
            val grammarUri = data?.data
            if (grammarUri == null) {
                toast(getString(R.string.errorFailedGetGrammarUrl))
            } else {
                mGrammarId = java.lang.Long.parseLong(grammarUri.pathSegments[1])
                toast(String.format(getString(R.string.toastAssignGrammar),
                        Utils.idToValue(this, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.NAME, mGrammarId)))
            }
        } else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                onSuccess(data)
            } else if (resultCode == Activity.RESULT_CANCELED) {
                onCancel()
            } else {
                onError(resultCode)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }


    protected fun getRecognizers(intent: Intent): List<ResolveInfo> {
        val pm = packageManager
        return pm.queryIntentActivities(intent, 0)
    }

    protected fun createRecognizerIntent(action: String): Intent {
        val intent = Intent(action)
        val prompt: String
        if (mGrammarId == 0L) {
            val phrasesDemo = resources.getStringArray(R.array.phrasesDemo)
            val phraseDemo = phrasesDemo[Random().nextInt(phrasesDemo.size)]
            intent.putExtra(Extras.EXTRA_PHRASE, phraseDemo)
            prompt = String.format(getString(R.string.promptDemo), phraseDemo)
        } else {
            val grammarTargetLang = grammarTargetLang
            intent.putExtra(Extras.EXTRA_GRAMMAR_URL, grammarUrl)
            intent.putExtra(Extras.EXTRA_GRAMMAR_TARGET_LANG, grammarTargetLang)
            prompt = "Speak $grammarName to $grammarTargetLang"
        }
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        intent.putExtra(Extras.EXTRA_VOICE_PROMPT, prompt)
        return intent
    }

    protected fun launchRecognizerIntent(intent: Intent) {
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
    }

    protected fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private val VOICE_RECOGNITION_REQUEST_CODE = 1234
        private val ACTIVITY_SELECT_GRAMMAR_URL = 1
    }
}