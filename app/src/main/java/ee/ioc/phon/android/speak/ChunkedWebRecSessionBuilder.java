/*
 * Copyright 2012-2013, Institute of Cybernetics at Tallinn University of Technology
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

package ee.ioc.phon.android.speak;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.utils.BundleUtils;
import ee.ioc.phon.android.speechutils.utils.IntentUtils;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;
import ee.ioc.phon.netspeechapi.recsession.ChunkedWebRecSession;

/**
 * <p>Builds a query for the speech recognizer server combing information from
 * various sources:</p>
 *
 * <ul>
 * <li>input extras</li>
 * <li>name of the calling app</li>
 * <li>stored preferences</li>
 * <li>app/grammar database</li>
 * </ul>
 *
 * <p>The following is some of the information that is sent to the server along with the audio.</p>
 *
 * <pre>
 * contentType
 * 		content type of the audio (e.g. "audio/x-flac;rate=16000")
 * serverUrl
 * 		URL of the recognizer server
 * grammarUrl
 * 		URL of the speech recognition grammar
 * grammarTargetLang
 * 		name of the target language (in case of GF grammars)
 * nbest
 * 		number of requested hypothesis
 * </pre>
 */
public class ChunkedWebRecSessionBuilder {

    public static final int MAX_RESULTS = 5;

    private final Context mContext;

    private URL mWsUrl;
    private URL mLmUrl;
    private int mNbest;
    private String mGrammarTargetLang;
    private String mLang;
    private boolean mPartialResults = false;
    private String mPhrase;
    private String mContentType;
    private String mUserAgentComment;
    private String mDeviceId;
    private String mCaller;

    public ChunkedWebRecSessionBuilder(Context context, Bundle extras, ComponentName callingActivity) throws MalformedURLException {
        mContext = context;

        if (extras == null) {
            extras = new Bundle();
        }

        if (Log.DEBUG) Log.i(BundleUtils.ppBundle(extras));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mDeviceId = PreferenceUtils.getUniqueId(prefs);

        PendingIntent pendingIntent = IntentUtils.getPendingIntent(extras);

        if (callingActivity == null) {
            Caller caller1 = new Caller(pendingIntent, extras);
            mCaller = caller1.getActualCaller();
            setUserAgentComment(caller1.toString());
        } else {
            // TODO: integrate this into the caller-object
            mCaller = getCaller(callingActivity, pendingIntent);
            setUserAgentComment(mCaller);
        }

        // Calling the constructor modifies the database
        PackageNameRegistry wrapper = new PackageNameRegistry(context, mCaller);
        String urlService = prefs.getString(context.getString(R.string.keyHttpServer), context.getString(R.string.defaultHttpServer));
        setFromExtras(extras, wrapper, urlService);
        mNbest = makeNbest(extras);
    }


    public void setUserAgentComment(String caller) {
        // TODO: rename "RecognizerIntentActivity" to "K6nele"
        mUserAgentComment = Utils.makeUserAgentComment("RecognizerIntentActivity", Utils.getVersionName(mContext), caller);
    }


    public void setContentType(String contentType) {
        mContentType = contentType;
    }


    public void setContentType(String mime, int sampleRate) {
        setContentType(makeContentType(mime, sampleRate));
    }


    public String getLang() {
        return mLang;
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public URL getServerUrl() {
        return mWsUrl;
    }

    public URL getGrammarUrl() {
        return mLmUrl;
    }

    public String getGrammarTargetLang() {
        return mGrammarTargetLang;
    }

    public String getCaller() {
        return mCaller;
    }

    public String getUserAgentComment() {
        return mUserAgentComment;
    }

    public boolean isPartialResults() {
        return mPartialResults;
    }


    public ChunkedWebRecSession build() {
        ChunkedWebRecSession recSession = new ChunkedWebRecSession(mWsUrl, mLmUrl, mGrammarTargetLang, mNbest);

        if (mPhrase != null) {
            recSession.setPhrase(mPhrase);
        }

        if (mLang != null) {
            recSession.setLang(mLang);
        }


        if (mUserAgentComment != null) {
            recSession.setUserAgentComment(mUserAgentComment);
        }

        if (mContentType != null) {
            recSession.setContentType(mContentType);
        }

        if (mDeviceId != null) {
            recSession.setDeviceId(mDeviceId);
        }

        if (mPartialResults) {
            recSession.setParam("partial", "true");
        }

        return recSession;
    }


    public List<String> toStringArrayList() {
        List<String> list = new ArrayList<>();
        list.add(mWsUrl == null ? null : mWsUrl.toString());
        list.add(mLmUrl == null ? null : mLmUrl.toString());
        list.add(mContentType);
        list.add(mGrammarTargetLang);
        list.add(mLang);
        list.add(mNbest + "");
        list.add(mPhrase);
        list.add(mDeviceId);
        list.add(mUserAgentComment);
        list.add(mPartialResults + "");
        return list;
    }


    private void setFromExtras(Bundle extras, PackageNameRegistry wrapper, String urlServer) throws MalformedURLException {
        mLang = makeLang(extras);

        mPartialResults = extras.getBoolean(RecognizerIntent.EXTRA_PARTIAL_RESULTS);

        // K6nele-specific extras
        mPhrase = extras.getString(Extras.EXTRA_PHRASE);

        mGrammarTargetLang = Utils.chooseValue(wrapper.getGrammarLang(), extras.getString(Extras.EXTRA_GRAMMAR_TARGET_LANG));

        // The server URL should never be null
        mWsUrl = new URL(
                Utils.chooseValue(
                        wrapper.getServerUrl(),
                        extras.getString(Extras.EXTRA_SERVER_URL),
                        urlServer
                ));

        // If the user has not overridden the grammar then use the app's EXTRA.
        String urlAsString = Utils.chooseValue(wrapper.getGrammarUrl(), extras.getString(Extras.EXTRA_GRAMMAR_URL));
        if (urlAsString != null && urlAsString.length() > 0) {
            mLmUrl = new URL(urlAsString);
        }
    }


    /**
     * <p>Returns the package name of the app that receives the transcription,
     * or <code>null</code> if the package name could not be resolved.</p>
     */
    private static String getCaller(ComponentName callingActivity, PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            if (callingActivity != null) {
                return callingActivity.getPackageName();
            }
        } else {
            return pendingIntent.getTargetPackage();
        }
        return null;
    }


    private static String makeContentType(String mime, int sampleRate) {
        // little endian = 1234
        // big endian = 4321
        if ("audio/x-flac".equals(mime)) {
            return "audio/x-flac";
        }
        return "audio/x-raw-int,channels=1,signed=true,endianness=1234,depth=16,width=16,rate=" + sampleRate;
    }


    /**
     * <p>If {@code EXTRA_MAX_RESULTS} was set (i.e. it is larger than 0) then we
     * pass it on to the server.</p>
     * <p>If it was not set then we check the type of the language model (this is an obligatory input parameter).
     * If the language model is unset (e.g. K6nele was launched via its own launcher icon), or
     * the model is "web search" (this is the case with some web browsers), then we ask the server
     * for several results. (TODO: this could be user-configurable.)
     * Otherwise we ask for just a single result.</p>
     */
    private static int makeNbest(Bundle extras) {
        int maxResults = extras.getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
        if (maxResults <= 0) {
            String model = extras.getString(RecognizerIntent.EXTRA_LANGUAGE_MODEL);
            if (model == null || model.equals(RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)) {
                return MAX_RESULTS;
            } else {
                return 1;
            }
        }
        return maxResults;
    }


    /**
     * <p>We choose the input language, preferring the language specified in EXTRA_LANGUAGE,
     * if this is unspecified then we look into the bundle to see if "selectedLanguage" is set
     * (by an IME). If this is also unspecified then we return the current locale as required
     * by the Android specification:
     * {@link RecognizerIntent#EXTRA_LANGUAGE}</p>
     *
     * <blockquote>
     * <p>Optional IETF language tag (as defined by BCP 47), for example "en-US".
     * This tag informs the recognizer to perform speech recognition in a
     * language different than the one set in the getDefault().</p>
     * </blockquote>
     */
    private String makeLang(Bundle extras) {
        String lang = extras.getString(RecognizerIntent.EXTRA_LANGUAGE);
        if (lang != null) {
            return lang;
        }

        // If EXTRA_LANGUAGE is not set but the bundle contains "selectedLanguage" (as is the case with some IMEs)
        // then use a value from the latter.
        Object selectedLanguage = BundleUtils.getBundleValue(extras, "selectedLanguage");
        if (selectedLanguage != null) {
            return selectedLanguage.toString();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (prefs.getBoolean(mContext.getString(R.string.keyRespectLocale), false)) {
            Locale locale = Locale.getDefault();
            if (locale != null) {
                return locale.toString();
            }
        }

        return null;
    }
}
