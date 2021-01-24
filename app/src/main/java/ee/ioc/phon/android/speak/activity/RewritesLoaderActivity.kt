/*
 * Copyright 2015-2020, Institute of Cybernetics at Tallinn University of Technology
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

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ee.ioc.phon.android.speak.K6neleApplication
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.databinding.ActivityRewritesLoaderBinding
import ee.ioc.phon.android.speak.model.RewriteRule
import ee.ioc.phon.android.speak.model.RewriteRuleViewModel
import ee.ioc.phon.android.speak.model.RewriteRuleViewModelFactory
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.*

/**
 * Loads the rewrites from the EXTRAs of an incoming VIEW- or SEND-intent, or if they are missing,
 * then launches ACTION_GET_CONTENT to load the rewrites from its result data.
 * In case of an incoming VIEW/SEND-intent we only accept "text/tab-separated-values" (see the manifest).
 * However, if the user explicitly launches a file picker from Kõnele, then any "text/ *" files
 * can be picked.
 */
class RewritesLoaderActivity : AppCompatActivity() {

    private val wordViewModel: RewriteRuleViewModel by viewModels {
        RewriteRuleViewModelFactory((application as K6neleApplication).repository)
    }

    private var utteranceRewriter: UtteranceRewriter? = null
    private var binding: ActivityRewritesLoaderBinding? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewritesLoaderBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val res = resources
        binding!!.etRewritesNameText.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding!!.bRewritesNameOk.performClick()
                return@setOnEditorActionListener true
            }
            false
        }
        binding!!.bRewritesNameOk.setOnClickListener { view: View? -> saveAndShow(prefs, res, binding!!.etRewritesNameText.text.toString()) }
        val keysSorted: List<String> = ArrayList(PreferenceUtils.getPrefMapKeys(prefs, res, R.string.keyRewritesMap))

        // If there are already some rewrites then we show their names as well
        if (!keysSorted.isEmpty()) {
            Collections.sort(keysSorted)
            val names = keysSorted.toTypedArray()
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
            binding!!.etRewritesNameText.setAdapter(adapter)
            binding!!.llRewritesChooser.visibility = View.VISIBLE
            binding!!.lvRewrites.adapter = ArrayAdapter(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, names)
            binding!!.lvRewrites.onItemClickListener = OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long -> saveAndShow(prefs, res, binding!!.lvRewrites.getItemAtPosition(position) as String) }
        }
        var intent = intent
        var uri = intent.data
        if (uri == null && intent.extras == null) {
            intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = TYPE
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            val chooser = Intent.createChooser(intent, "")
            startActivityForResult(chooser, GET_CONTENT_REQUEST_CODE)
        } else {
            // Responding to SEND and VIEW actions
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            if (subject != null) {
                binding!!.etRewritesNameText.setText(subject)
                binding!!.etRewritesNameText.setSelection(subject.length)
            }
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text == null) {
                if (uri == null) {
                    uri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    if ("k6" == uri.scheme) {
                        val data = Base64.decode(uri.schemeSpecificPart.substring(2), Base64.NO_WRAP or Base64.URL_SAFE)
                        try {
                            utteranceRewriter = UtteranceRewriter(data.toString(Charsets.UTF_8))
                        } catch (e: UnsupportedEncodingException) {
                            // TODO: dont ignore
                        }
                    } else {
                        utteranceRewriter = loadFromUri(uri)
                    }
                }
            } else {
                utteranceRewriter = UtteranceRewriter(text)
            }
            finishIfFailed()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == GET_CONTENT_REQUEST_CODE && resultCode == RESULT_OK && resultData != null) {
            val uri = resultData.data
            if (uri != null) {
                utteranceRewriter = loadFromUri(uri)
            }
        }
        finishIfFailed()
    }

    private fun loadFromUri(uri: Uri): UtteranceRewriter? {
        try {
            return UtteranceRewriter(contentResolver, uri)
        } catch (e: IOException) {
            val errorMessage = String.format(getString(R.string.errorLoadRewrites), e.localizedMessage)
            toast(errorMessage)
        }
        return null
    }

    private fun finishIfFailed() {
        if (utteranceRewriter == null) {
            finish()
        }
    }

    // Using Room instead of prefs
    // TODO: cleanup
    private fun saveAndShow(prefs: SharedPreferences, res: Resources, name: String) {
        if (utteranceRewriter != null) {
            val intent = Intent(this, RewritesActivity::class.java)
            intent.putExtra(RewritesActivity.EXTRA_NAME, name)

            for (command in utteranceRewriter!!.commands) {
                wordViewModel.insert((RewriteRule.fromCommand(command)))
            }
            // TODO: remove once Room is ready
            PreferenceUtils.putPrefMapEntry(prefs, res, R.string.keyRewritesMap, name, utteranceRewriter!!.toTsv())
            startActivity(intent)

            // If there were errors then we load another activity to show them.
            // TODO: verify that this is a correct way to start multiple activities
            val errors = utteranceRewriter!!.errorsAsStringArray
            if (errors.size > 0) {
                val searchIntent = Intent(this, RewritesErrorsActivity::class.java)
                searchIntent.putExtra(RewritesErrorsActivity.EXTRA_TITLE, name)
                searchIntent.putExtra(RewritesErrorsActivity.EXTRA_STRING_ARRAY, errors)
                startActivity(searchIntent)
            }
        }
        finish()
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        //private static final String TYPE = "text/tab-separated-values";
        private const val TYPE = "text/*"
        private const val GET_CONTENT_REQUEST_CODE = 1
    }
}