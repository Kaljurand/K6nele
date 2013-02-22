package ee.ioc.phon.android.speak;

import android.content.SharedPreferences;
import android.content.res.Resources;

public class PrefStore {

	private final SharedPreferences mPrefs;
	private final Resources mRes;

	public PrefStore(SharedPreferences prefs, Resources res) {
		mPrefs = prefs;
		mRes = res;
	}

	public int getInteger(int key, int defaultValue) {
		return Integer.parseInt(mPrefs.getString(mRes.getString(key), mRes.getString(defaultValue)));
	}

	public boolean getBoolean(int key, int defaultValue) {
		return mPrefs.getBoolean(mRes.getString(key), mRes.getBoolean(defaultValue));
	}

}