package ee.ioc.phon.android.speak;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import ee.ioc.phon.netspeechapi.recsession.ChunkedWebRecSession;

/**
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
 */
public class ChunkedWebRecSessionBuilder {

	private final Context mContext;

	private URL mWsUrl;
	private URL mLmUrl;
	private int mNbest;
	private String mGrammarTargetLang;
	private String mLang;
	private String mPhrase;
	private String mContentType;
	private String mUserAgentComment;
	private String mDeviceId;

	public ChunkedWebRecSessionBuilder(Context context, Bundle extras, ComponentName callingActivity) throws MalformedURLException {
		mContext = context;

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

		return recSession;
	}


	public String toString() {
		return
				mWsUrl + ": lm=" + mLmUrl + ": lang=" + mGrammarTargetLang + ": nbest=" + mNbest;
	}


	private void setFromExtras(Bundle extras, PackageNameRegistry wrapper, String urlServer) throws MalformedURLException {
		int maxResults = extras.getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
		mNbest = (maxResults > 1) ? maxResults : 1;

		mLang = extras.getString(RecognizerIntent.EXTRA_LANGUAGE);
		// TODO: use
		// extras.getString(RecognizerIntent.EXTRA_PARTIAL_RESULTS);

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


	private static String makeUserAgentComment(String versionName, String caller) {
		return "RecognizerIntentActivity/" + versionName + "; " +
				Build.MANUFACTURER + "/" +
				Build.DEVICE + "/" +
				Build.DISPLAY + "; " +
				caller;
	}
}