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

package ee.ioc.phon.android.speak.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

import ee.ioc.phon.android.speak.GrammarListActivity;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.provider.Grammar;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.Extras;

/**
 * <p>This demo shows how to create an input to RecognizerIntent.ACTION_RECOGNIZE_SPEECH
 * and how to respond to its output (list of matched words or an error code). This is
 * an abstract class, the UI part is in the extensions of this class.</p>
 *
 * @author Kaarel Kaljurand
 */
public abstract class AbstractRecognizerDemoActivity extends Activity {

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private static final int ACTIVITY_SELECT_GRAMMAR_URL = 1;

    private long mGrammarId;

    protected abstract void onSuccess(Intent intent);

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.demo_grammar, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuDemoGrammarAssign:
                Intent intent = new Intent(AbstractRecognizerDemoActivity.this, GrammarListActivity.class);
                startActivityForResult(intent, ACTIVITY_SELECT_GRAMMAR_URL);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    protected void onError(int errorCode) {
        toast(getErrorMessage(errorCode));
    }

    protected String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case RecognizerIntent.RESULT_AUDIO_ERROR:
                return getString(R.string.errorResultAudioError);
            case RecognizerIntent.RESULT_CLIENT_ERROR:
                return getString(R.string.errorResultClientError);
            case RecognizerIntent.RESULT_NETWORK_ERROR:
                return getString(R.string.errorResultNetworkError);
            case RecognizerIntent.RESULT_SERVER_ERROR:
                return getString(R.string.errorResultServerError);
            case RecognizerIntent.RESULT_NO_MATCH:
                return getString(R.string.errorResultNoMatch);
            default:
                return "Unknown error";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_SELECT_GRAMMAR_URL) {
            if (resultCode != RESULT_OK) {
                return;
            }
            Uri grammarUri = data.getData();
            if (grammarUri == null) {
                toast(getString(R.string.errorFailedGetGrammarUrl));
            } else {
                mGrammarId = Long.parseLong(grammarUri.getPathSegments().get(1));
                toast(String.format(getString(R.string.toastAssignGrammar),
                        Utils.idToValue(this, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.NAME, mGrammarId)));
            }
        } else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                onSuccess(data);
            } else if (resultCode == RESULT_CANCELED) {
                toast(getString(R.string.errorResultCanceled));
            } else {
                onError(resultCode);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    protected List<ResolveInfo> getRecognizers(Intent intent) {
        PackageManager pm = getPackageManager();
        return pm.queryIntentActivities(intent, 0);
    }


    protected void launchRecognizerIntent(Intent intent) {
        if (mGrammarId == 0) {
            String[] phrasesDemo = getResources().getStringArray(R.array.phrasesDemo);
            String phraseDemo = phrasesDemo[(new Random()).nextInt(phrasesDemo.length)];
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, String.format(getString(R.string.promptDemo), phraseDemo));
            intent.putExtra(Extras.EXTRA_PHRASE, phraseDemo);
        } else {
            String grammarTargetLang = getGrammarTargetLang();
            intent.putExtra(Extras.EXTRA_GRAMMAR_URL, getGrammarUrl());
            intent.putExtra(Extras.EXTRA_GRAMMAR_TARGET_LANG, grammarTargetLang);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak " + getGrammarName() + " to " + grammarTargetLang);
        }
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }


    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }


    private String getGrammarName() {
        return Utils.idToValue(this, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.NAME, mGrammarId);
    }


    private String getGrammarUrl() {
        return Utils.idToValue(this, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.URL, mGrammarId);
    }


    private String getGrammarTargetLang() {
        return Utils.idToValue(this, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.LANG, mGrammarId);
    }
}