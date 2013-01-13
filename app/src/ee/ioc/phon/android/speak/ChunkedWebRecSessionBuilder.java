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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import ee.ioc.phon.netspeechapi.recsession.ChunkedWebRecSession;

/**
 * <p>Builds a query for the speech recognizer server combing information from
 * various sources:</p>
 *
 * <ul>
 *   <li>input extras</li>
 *   <li>name of the calling app</li>
 *   <li>stored preferences</li>
 *   <li>app/grammar database</li>
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

	public ChunkedWebRecSessionBuilder(Context context, Bundle extras, ComponentName callingActivity) throws MalformedURLException {
		mContext = context;

		//Log.i(Utils.ppBundle(extras));

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mDeviceId = Utils.getUniqueId(prefs);

		PendingIntent pendingIntent = Utils.getPendingIntent(extras);

		String caller = null;

		if (callingActivity == null) {
			Caller caller1 = new Caller(pendingIntent, extras);
			caller = caller1.getActualCaller();
			setUserAgentComment(caller1.toString());
		} else {
			// TODO: integrate this into the caller-object
			caller = getCaller(callingActivity, pendingIntent);
			setUserAgentComment(caller);
		}

		PackageNameRegistry wrapper = new PackageNameRegistry(context, caller);
		String urlService = prefs.getString(context.getString(R.string.keyService), context.getString(R.string.defaultService));
		setFromExtras(extras, wrapper, urlService);
	}


	public void setUserAgentComment(String caller) {
		mUserAgentComment = makeUserAgentComment(Utils.getVersionName(mContext), caller);
	}


	public void setContentType(String contentType) {
		mContentType = contentType;
	}


	public void setContentType(int sampleRate) {
		setContentType(makeContentType(sampleRate));
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


	public String getUserAgentComment() {
		return mUserAgentComment;
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

		// Inform the server that we want partial results.
		// TODO: this is not supported yet, neither on the server nor the K6nele side
		if (mPartialResults) {
			recSession.setParam("partial", "true");
		}

		return recSession;
	}


	public List<String> toStringArrayList() {
		List<String> list = new ArrayList<String>();
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
		int maxResults = extras.getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
		mNbest = (maxResults > 1) ? maxResults : 1;

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
	private String getCaller(ComponentName callingActivity, PendingIntent pendingIntent) {
		if (pendingIntent == null) {
			if (callingActivity != null) {
				return callingActivity.getPackageName();
			}
		} else {
			return pendingIntent.getTargetPackage();
		}
		return null;
	}


	private static String makeContentType(int sampleRate) {
		// little endian = 1234
		// big endian = 4321
		return "audio/x-raw-int,channels=1,signed=true,endianness=1234,depth=16,width=16,rate=" + sampleRate;
	}


	/**
	 * <p>We choose the input language, preferring the language specified in EXTRA_LANGUAGE,
	 * if this is unspecified then we look into the bundle to see if "selectedLanguage" is set
	 * (by an IME). If this is also unspecified then we return the current locale as required
	 * by the Android specification:
	 * {@link http://developer.android.com/reference/android/speech/RecognizerIntent.html#EXTRA_LANGUAGE}</p>
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
		Object selectedLanguage = Utils.getBundleValue(extras, "selectedLanguage");
		if (selectedLanguage != null) {
			return selectedLanguage.toString();
		}

		// TODO: enable this in the future to be compatible with the Android spec.
		// For the time being it is safer not to send the
		// locale because K6nele users assume Estonian even if the app does not specify it
		// (e.g. Arvutaja v4).
		/*
		Locale locale = Locale.getDefault();
		if (locale != null) {
			return locale.toString();
		}
		*/

		return null;
	}


	private static String makeUserAgentComment(String versionName, String caller) {
		return "RecognizerIntentActivity/" + versionName + "; " +
				Build.MANUFACTURER + "/" +
				Build.DEVICE + "/" +
				Build.DISPLAY + "; " +
				caller;
	}
}
