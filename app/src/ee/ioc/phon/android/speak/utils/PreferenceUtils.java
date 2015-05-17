package ee.ioc.phon.android.speak.utils;

import android.content.SharedPreferences;
import android.content.res.Resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PreferenceUtils {

    public static String getPrefString(SharedPreferences prefs, Resources res, int key, int defaultValue) {
        return prefs.getString(res.getString(key), res.getString(defaultValue));
    }

    public static String getPrefString(SharedPreferences prefs, Resources res, int key) {
        return prefs.getString(res.getString(key), null);
    }

    public static Set<String> getPrefStringSet(SharedPreferences prefs, Resources res, int key) {
        return prefs.getStringSet(res.getString(key), Collections.<String>emptySet());
    }

    public static Set<String> getPrefStringSet(SharedPreferences prefs, Resources res, int key, int defaultValue) {
        return prefs.getStringSet(res.getString(key), getStringSetFromStringArray(res, defaultValue));
    }

    public static boolean getPrefBoolean(SharedPreferences prefs, Resources res, int key, int defaultValue) {
        return prefs.getBoolean(res.getString(key), res.getBoolean(defaultValue));
    }

    public static String getUniqueId(SharedPreferences settings) {
        String id = settings.getString("id", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("id", id);
            editor.apply();
        }
        return id;
    }

    public static Set<String> getStringSetFromStringArray(Resources res, int key) {
        return new HashSet<>(Arrays.asList(res.getStringArray(key)));
    }
}