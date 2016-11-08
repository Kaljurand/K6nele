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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.provider.FileContentProvider;
import ee.ioc.phon.android.speak.utils.IntentUtils;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.RawAudioRecorder;
import ee.ioc.phon.android.speechutils.RecognitionServiceManager;
import ee.ioc.phon.android.speechutils.utils.AudioUtils;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public abstract class AbstractRecognizerIntentActivity extends Activity {

    public static final String AUDIO_FILENAME = "audio.wav";

    public static final String DEFAULT_AUDIO_FORMAT = "audio/wav";
    public static final Set<String> SUPPORTED_AUDIO_FORMATS = Collections.singleton(DEFAULT_AUDIO_FORMAT);

    protected static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    private static final int ACTIVITY_REQUEST_CODE_DETAILS = 1;

    private static final String MSG = "MSG";
    private static final int MSG_TOAST = 1;
    private static final int MSG_RESULT_ERROR = 2;

    private static SparseIntArray mErrorCodesServiceToIntent = IntentUtils.createErrorCodesServiceToIntent();

    private List<byte[]> mBufferList = new ArrayList<>();

    private TextView mTvPrompt;

    private PendingIntent mExtraResultsPendingIntent;

    private Bundle mExtras;

    private SparseArray<String> mErrorMessages;

    private SimpleMessageHandler mMessageHandler;

    // Store the complete audio recording
    private boolean mIsStoreAudio;

    private boolean mIsReturnErrors;

    private boolean mIsAutoStart;

    abstract void showError(String msg);

    abstract String[] getDetails();

    protected Uri getAudioUri(String filename) {
        // TODO: ask the sample rate directly from the recorder
        int sampleRate = PreferenceUtils.getPrefInt(PreferenceManager.getDefaultSharedPreferences(this),
                getResources(), R.string.keyRecordingRate, R.string.defaultRecordingRate);
        byte[] mCompleteRecording = AudioUtils.concatenateBuffers(mBufferList);
        return bytesToUri(filename, RawAudioRecorder.getRecordingAsWav(mCompleteRecording, sampleRate));
    }

    protected Uri bytesToUri(String filename, byte[] bytes) {
        try {
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(bytes);
            fos.close();
            return Uri.parse("content://" + FileContentProvider.AUTHORITY + "/" + filename);
        } catch (FileNotFoundException e) {
            Log.e("FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            Log.e("IOException: " + e.getMessage());
        }
        return null;
    }

    protected boolean isAutoStart() {
        return mIsAutoStart;
    }

    protected Bundle getExtras() {
        return mExtras;
    }

    protected PendingIntent getExtraResultsPendingIntent() {
        return mExtraResultsPendingIntent;
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
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // If the caller did not specify the MAX_RESULTS then we take it from our own settings.
        // Note: the caller overrides the settings.
        if (!mExtras.containsKey(RecognizerIntent.EXTRA_MAX_RESULTS)) {
            mExtras.putInt(RecognizerIntent.EXTRA_MAX_RESULTS,
                    PreferenceUtils.getPrefInt(prefs, getResources(), R.string.keyMaxResults, R.string.defaultMaxResults));
        }

        if (mExtras.containsKey(Extras.EXTRA_SERVICE_COMPONENT)) {
            String combo = mExtras.getString(Extras.EXTRA_SERVICE_COMPONENT);
            if (mExtras.containsKey(RecognizerIntent.EXTRA_LANGUAGE)) {
                combo = RecognitionServiceManager.createComboString(combo, mExtras.getString(RecognizerIntent.EXTRA_LANGUAGE));
            }
            PreferenceUtils.putPrefString(prefs, getResources(), R.string.keyCurrentCombo, combo);
        }

        if (!mExtras.isEmpty()) {
            mExtraResultsPendingIntent = IntentUtils.getPendingIntent(mExtras);
        }

        mIsStoreAudio = mExtras.getBoolean(Extras.EXTRA_GET_AUDIO);

        mIsReturnErrors = mExtras.getBoolean(Extras.EXTRA_RETURN_ERRORS,
                PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyReturnErrors, R.bool.defaultReturnErrors));

        // Launch recognition immediately (if set so).
        // Auto-start only occurs is onCreate is called
        mIsAutoStart =
                isAutoStartAction(getIntent().getAction())
                        || mExtras.getBoolean(Extras.EXTRA_AUTO_START,
                        PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyAutoStart, R.bool.defaultAutoStart));

        mMessageHandler = new SimpleMessageHandler(this);
        mErrorMessages = createErrorMessages();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_CODE_DETAILS) {
            if (resultCode == RESULT_OK && data != null) {
                handleResultByWebSearch(data.getStringExtra(SearchManager.QUERY));
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showError("");
                    setTvPrompt();
                } else {
                    setTvPrompt(getString(R.string.promptPermissionRationale));
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    /**
     * TODO: review
     *
     * @param action intent action used to launch Kõnele
     * @return true iff the given action requires automatic start
     */
    static boolean isAutoStartAction(String action) {
        return
                Extras.ACTION_VOICE_SEARCH_HANDS_FREE.equals(action)
                        || Intent.ACTION_SEARCH_LONG_PRESS.equals(action)
                        || Intent.ACTION_VOICE_COMMAND.equals(action)
                        || Intent.ACTION_ASSIST.equals(action);
    }

    public void registerPrompt(TextView tv) {
        mTvPrompt = tv;
    }

    public void setTvPrompt() {
        setTvPrompt(getPrompt());
    }

    public void setTvPrompt(String prompt) {
        if (prompt == null || prompt.length() == 0) {
            mTvPrompt.setVisibility(View.INVISIBLE);
        } else {
            mTvPrompt.setText(prompt);
            mTvPrompt.setVisibility(View.VISIBLE);
        }
    }

    private String getPrompt() {
        String prompt = getExtras().getString(RecognizerIntent.EXTRA_PROMPT);
        if (prompt == null && getExtraResultsPendingIntent() == null && getCallingActivity() == null) {
            return getString(R.string.promptSearch);
        }
        return prompt;
    }

    /**
     * Sets the RESULT_OK intent. Adds the recorded audio data if the caller has requested it
     * and the requested format is supported or unset.
     * <p>
     * TODO: handle audioFormat inside getAudioUri(), which would return "null"
     * if format is not supported
     */
    private void setResultIntent(final Handler handler, List<String> matches) {
        Intent intent = new Intent();
        if (mIsStoreAudio) {
            String audioFormat = getExtras().getString(Extras.EXTRA_GET_AUDIO_FORMAT);
            if (audioFormat == null) {
                audioFormat = DEFAULT_AUDIO_FORMAT;
            }
            if (SUPPORTED_AUDIO_FORMATS.contains(audioFormat)) {
                Uri uri = getAudioUri(AUDIO_FILENAME);
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

        intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, getResultsAsArrayList(matches));
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
                        outerClass.showError(msgAsString);
                        break;
                    default:
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
    protected void returnOrForwardMatches(List<String> matches) {
        Handler handler = mMessageHandler;

        // Throw away matches that the user is not interested in
        int maxResults = getExtras().getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
        if (maxResults > 0 && matches.size() > maxResults) {
            matches.subList(maxResults, matches.size()).clear();
        }

        String action = getIntent().getAction();
        if (getExtraResultsPendingIntent() == null) {
            // TODO: clean this up: "auto start" should not necessarily mean that we should not return the results
            // to the caller
            // TODO: maybe remove ACTION_WEB_SEARCH (i.e. the results should be returned to the caller)
            if (getCallingActivity() == null
                    || isAutoStartAction(action)
                    || RecognizerIntent.ACTION_WEB_SEARCH.equals(action)
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
            // This is for SwiftKey X (from year 2011), ...
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, getResultsAsArrayList(matches));
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
    private void handleResultsByWebSearch(final List<String> results) {
        if (results.size() == 1) {
            handleResultByWebSearch(results.get(0));
        } else {
            Intent searchIntent = new Intent(this, DetailsActivity.class);
            searchIntent.putExtra(DetailsActivity.EXTRA_TITLE, getString(R.string.dialogTitleHypotheses));
            searchIntent.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, results.toArray(new String[results.size()]));
            startActivityForResult(searchIntent, ACTIVITY_REQUEST_CODE_DETAILS);
        }
    }

    private void handleResultByWebSearch(String result) {
        Bundle extras = getExtras();
        String utterance = extras.getString(Extras.EXTRA_RESULT_UTTERANCE, null);
        String replacement = extras.getString(Extras.EXTRA_RESULT_REPLACEMENT, null);
        if (utterance != null && replacement != null) {
            toast(utterance + "->" + replacement);
            try {
                result = result.replaceAll(utterance, replacement);
            } catch (PatternSyntaxException e) {
                toast(e.getLocalizedMessage());
            }
        }
        IntentUtils.startSearchActivity(this, result);

        // TODO: we should not finish if the activity was launched for a result, otherwise
        // the result would not be processed.

        // Do not finish if in multi window mode because the user might want
        // to ask a follow-up query. Android N only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!isInMultiWindowMode()) {
                finish();
            }
        } else {
            finish();
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

    /**
     * Finish the activity with the given error code. By default the audio/network/etc. errors
     * are handled by the activity so that the activity only returns with success. However, in certain
     * situations (e.g. Tasker integration) it is useful to let the caller handle the errors.
     *
     * @param errorCode SpeechRecognizer service error code
     */
    protected void setResultError(int errorCode) {
        if (mIsReturnErrors) {
            Integer errorCodeIntent = mErrorCodesServiceToIntent.get(errorCode);
            setResult(errorCodeIntent);
            finish();
        }
    }

    protected void clearAudioBuffer() {
        mBufferList = new ArrayList<>();
    }

    protected void addToAudioBuffer(byte[] buffer) {
        if (mIsStoreAudio) {
            mBufferList.add(buffer);
        }
    }

    private ArrayList<String> getResultsAsArrayList(List<String> results) {
        ArrayList<String> resultsAsArrayList = new ArrayList<>();
        resultsAsArrayList.addAll(results);
        return resultsAsArrayList;
    }
}