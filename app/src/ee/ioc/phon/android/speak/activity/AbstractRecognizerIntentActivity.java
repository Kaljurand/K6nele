/*
 * Copyright 2015, Institute of Cybernetics at Tallinn University of Technology
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

package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.Constants;
import ee.ioc.phon.android.speak.DetailsActivity;
import ee.ioc.phon.android.speak.Extras;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.Preferences;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.Utils;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;

public abstract class AbstractRecognizerIntentActivity extends Activity {

    private static final String MSG = "MSG";
    private static final int MSG_TOAST = 1;
    private static final int MSG_RESULT_ERROR = 2;

    private ChunkedWebRecSessionBuilder mRecSessionBuilder;

    private PendingIntent mExtraResultsPendingIntent;

    private Bundle mExtras;

    private SparseArray<String> mErrorMessages;

    private SimpleMessageHandler mMessageHandler;

    abstract Uri getAudioUri();

    abstract void showError();


    protected Bundle getExtras() {
        return mExtras;
    }

    protected PendingIntent getExtraResultsPendingIntent() {
        return mExtraResultsPendingIntent;
    }

    protected ChunkedWebRecSessionBuilder getRecSessionBuilder() {
        return mRecSessionBuilder;
    }

    protected SparseArray<String> getErrorMessages() {
        return mErrorMessages;
    }

    protected void setUpSettingsButton() {
        // Short click opens the settings
        ImageButton bSettings = (ImageButton) findViewById(R.id.bSettings);
        bSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Preferences.class));
            }
        });

        // Long click shows some technical details (for developers)
        bSettings.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent details = new Intent(getApplicationContext(), DetailsActivity.class);
                details.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, getDetails());
                startActivity(details);
                return false;
            }
        });
    }

    protected void setUpActivity(int layout) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void setUpExtras() {
        mExtras = getIntent().getExtras();
        if (mExtras == null) {
            // For some reason getExtras() can return null, we map it
            // to an empty Bundle if this occurs.
            mExtras = new Bundle();
        } else {
            mExtraResultsPendingIntent = Utils.getPendingIntent(mExtras);
        }

        mMessageHandler = new SimpleMessageHandler(this);
        mErrorMessages = createErrorMessages();

        try {
            mRecSessionBuilder = new ChunkedWebRecSessionBuilder(this, mExtras, getCallingActivity());
        } catch (MalformedURLException e) {
            // The user has managed to store a malformed URL in the configuration.
            handleResultError(RecognizerIntent.RESULT_CLIENT_ERROR, "", e);
        }

        try {
            mRecSessionBuilder = new ChunkedWebRecSessionBuilder(this, mExtras, getCallingActivity());
        } catch (MalformedURLException e) {
            // The user has managed to store a malformed URL in the configuration.
            handleResultError(RecognizerIntent.RESULT_CLIENT_ERROR, "", e);
        }
    }

    /**
     * <p>Only for developers, i.e. we are not going to localize these strings.</p>
     */
    private String[] getDetails() {
        String callingActivityClassName = null;
        String callingActivityPackageName = null;
        String pendingIntentTargetPackage = null;
        ComponentName callingActivity = getCallingActivity();
        if (callingActivity != null) {
            callingActivityClassName = callingActivity.getClassName();
            callingActivityPackageName = callingActivity.getPackageName();
        }
        if (getExtraResultsPendingIntent() != null) {
            pendingIntentTargetPackage = getExtraResultsPendingIntent().getTargetPackage();
        }
        List<String> info = new ArrayList<>();
        info.add("ID: " + PreferenceUtils.getUniqueId(PreferenceManager.getDefaultSharedPreferences(this)));
        info.add("User-Agent comment: " + getRecSessionBuilder().getUserAgentComment());
        info.add("Calling activity class name: " + callingActivityClassName);
        info.add("Calling activity package name: " + callingActivityPackageName);
        info.add("Pending intent target package: " + pendingIntentTargetPackage);
        info.add("Selected grammar: " + getRecSessionBuilder().getGrammarUrl());
        info.add("Selected target lang: " + getRecSessionBuilder().getGrammarTargetLang());
        info.add("Selected server: " + getRecSessionBuilder().getServerUrl());
        info.add("Intent action: " + getIntent().getAction());
        info.addAll(Utils.ppBundle(getExtras()));
        return info.toArray(new String[info.size()]);
    }

    /**
     * Sets the RESULT_OK intent. Adds the recorded audio data if the caller has requested it
     * and the requested format is supported or unset.
     *
     * TODO: handle audioFormat inside getAudioUri(), which would return "null"
     * if format is not supported
     */
    private void setResultIntent(final Handler handler, ArrayList<String> matches) {
        Intent intent = new Intent();
        if (getExtras().getBoolean(Extras.GET_AUDIO)) {
            String audioFormat = getExtras().getString(Extras.GET_AUDIO_FORMAT);
            if (audioFormat == null) {
                audioFormat = Constants.DEFAULT_AUDIO_FORMAT;
            }
            if (Constants.SUPPORTED_AUDIO_FORMATS.contains(audioFormat)) {
                Uri uri = getAudioUri();
                if (uri != null) {
                    // TODO: not sure about the type (or if it's needed)
                    intent.setDataAndType(uri, audioFormat);
                }
            } else {
                if (Log.DEBUG) {
                    handler.sendMessage(createMessage(MSG_TOAST,
                            String.format(getString(R.string.toastRequestedAudioFormatNotSupported), audioFormat)));
                }
            }
        }
        intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, matches);
        setResult(Activity.RESULT_OK, intent);
    }

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    protected static Message createMessage(int type, String str) {
        Bundle b = new Bundle();
        b.putString(MSG, str);
        Message msg = Message.obtain();
        msg.what = type;
        msg.setData(b);
        return msg;
    }


    protected static class SimpleMessageHandler extends Handler {
        private final WeakReference<AbstractRecognizerIntentActivity> mRef;

        public SimpleMessageHandler(AbstractRecognizerIntentActivity c) {
            mRef = new WeakReference<>(c);
        }

        public void handleMessage(Message msg) {
            AbstractRecognizerIntentActivity outerClass = mRef.get();
            if (outerClass != null) {
                Bundle b = msg.getData();
                String msgAsString = b.getString(MSG);
                switch (msg.what) {
                    case MSG_TOAST:
                        outerClass.toast(msgAsString);
                        break;
                    case MSG_RESULT_ERROR:
                        outerClass.showError();
                        break;
                }
            }
        }
    }


    /**
     * <p>Returns the transcription results (matches) to the caller,
     * or sends them to the pending intent, or performs a web search.</p>
     * <p/>
     * <p>If a pending intent was specified then use it. This is the case with
     * applications that use the standard search bar (e.g. Google Maps and YouTube).</p>
     * <p/>
     * <p>Otherwise. If there was no caller (i.e. we cannot return the results), or
     * the caller asked us explicitly to perform "web search", then do that, possibly
     * disambiguating the results or redoing the recognition.
     * This is the case when K6nele was launched from its launcher icon (i.e. no caller),
     * or from a browser app.
     * (Note that trying to return the results to Google Chrome does not seem to work.)</p>
     * <p/>
     * <p>Otherwise. Just return the results to the caller.</p>
     * <p/>
     * <p>Note that we assume that the given list of matches contains at least one
     * element.</p>
     *
     * @param matches transcription results (one or more hypotheses)
     */
    protected void returnOrForwardMatches(ArrayList<String> matches) {
        Handler handler = mMessageHandler;

        // Throw away matches that the user is not interested in
        int maxResults = getExtras().getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
        if (maxResults > 0 && matches.size() > maxResults) {
            matches.subList(maxResults, matches.size()).clear();
        }

        if (getExtraResultsPendingIntent() == null) {
            if (getCallingActivity() == null
                    || RecognizerIntent.ACTION_WEB_SEARCH.equals(getIntent().getAction())
                    || getExtras().getBoolean(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY)) {
                handleResultsByWebSearch(matches);
                return;
            } else {
                setResultIntent(handler, matches);
            }
        } else {
            Bundle bundle = getExtras().getBundle(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE);
            if (bundle == null) {
                bundle = new Bundle();
            }
            String match = matches.get(0);
            //mExtraResultsPendingIntentBundle.putString(SearchManager.QUERY, match);
            Intent intent = new Intent();
            intent.putExtras(bundle);
            // This is for Google Maps, YouTube, ...
            intent.putExtra(SearchManager.QUERY, match);
            // This is for SwiftKey X, ...
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, matches);
            String message;
            if (matches.size() == 1) {
                message = match;
            } else {
                message = matches.toString();
            }
            // Display a toast with the transcription.
            handler.sendMessage(createMessage(MSG_TOAST, String.format(getString(R.string.toastForwardedMatches), message)));
            try {
                getExtraResultsPendingIntent().send(this, Activity.RESULT_OK, intent);
            } catch (PendingIntent.CanceledException e) {
                handler.sendMessage(createMessage(MSG_TOAST, e.getMessage()));
            }
        }
        finish();
    }


    protected void handleResultError(int resultCode, String type, Exception e) {
        if (e != null) {
            Log.e("Exception: " + type + ": " + e.getMessage());
        }
        mMessageHandler.sendMessage(createMessage(MSG_RESULT_ERROR, getErrorMessages().get(resultCode)));
    }


    // In case of multiple hypotheses, ask the user to select from a list dialog.
    // TODO: fetch also confidence scores and treat a very confident hypothesis
    // as a single hypothesis.
    private void handleResultsByWebSearch(final ArrayList<String> results) {
        // Some tweaking to cleanup the UI that would show under the
        // dialog window that we are about to open.
        // TODO: temporarily commented out
        /*
        runOnUiThread(new Runnable() {
            public void run() {
                mLlTranscribing.setVisibility(View.GONE);
            }
        });
        */

        Intent searchIntent;
        if (results.size() == 1) {
            // We construct a list of search intents.
            // The first one that can be handled by the device is launched.
            CharSequence query = results.get(0);
            Intent intent1 = new Intent(Intent.ACTION_WEB_SEARCH);
            intent1.putExtra(SearchManager.QUERY, query);
            Intent intent2 = new Intent(Intent.ACTION_SEARCH);
            intent2.putExtra(SearchManager.QUERY, query);

            Utils.startActivityIfAvailable(this, intent1, intent2);
        } else {
            // TODO: it would be a bit cleaner to pass ACTION_WEB_SEARCH
            // via a pending intent
            searchIntent = new Intent(this, DetailsActivity.class);
            searchIntent.putExtra(DetailsActivity.EXTRA_TITLE, getString(R.string.dialogTitleHypotheses));
            searchIntent.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, results.toArray(new String[results.size()]));
            startActivity(searchIntent);
        }
    }


    private SparseArray<String> createErrorMessages() {
        SparseArray<String> errorMessages = new SparseArray<>();
        errorMessages.put(RecognizerIntent.RESULT_AUDIO_ERROR, getString(R.string.errorResultAudioError));
        errorMessages.put(RecognizerIntent.RESULT_CLIENT_ERROR, getString(R.string.errorResultClientError));
        errorMessages.put(RecognizerIntent.RESULT_NETWORK_ERROR, getString(R.string.errorResultNetworkError));
        errorMessages.put(RecognizerIntent.RESULT_SERVER_ERROR, getString(R.string.errorResultServerError));
        errorMessages.put(RecognizerIntent.RESULT_NO_MATCH, getString(R.string.errorResultNoMatch));
        return errorMessages;
    }
}