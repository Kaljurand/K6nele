package ee.ioc.phon.android.speak.utils;

import android.content.Intent;
import android.os.Bundle;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.Extras;
import ee.ioc.phon.android.speak.Log;

public class QueryUtils {

    /**
     * Extracts the editor info, and uses
     * ChunkedWebRecSessionBuilder to extract some additional extras.
     * TODO: unify this better
     */
    public static String getQueryParams(Intent intent, ChunkedWebRecSessionBuilder builder) {
        List<BasicNameValuePair> list = new ArrayList<>();
        flattenBundle("editorInfo_", list, intent.getBundleExtra(Extras.EXTRA_EDITOR_INFO));
        if (Log.DEBUG) Log.i(builder.toStringArrayList());
        // TODO: review these parameter names
        listAdd(list, "lang", builder.getLang());
        listAdd(list, "lm", toString(builder.getGrammarUrl()));
        listAdd(list, "output-lang", builder.getGrammarTargetLang());
        listAdd(list, "user-agent", builder.getUserAgentComment());
        listAdd(list, "calling-package", builder.getCaller());
        listAdd(list, "user-id", builder.getDeviceId());
        listAdd(list, "partial", "" + builder.isPartialResults());
        if (list.size() == 0) {
            return "";
        }
        return "&" + URLEncodedUtils.format(list, "utf-8");
    }

    private static boolean listAdd(List<BasicNameValuePair> list, String key, String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        return list.add(new BasicNameValuePair(key, value));
    }

    private static void flattenBundle(String prefix, List<BasicNameValuePair> list, Bundle bundle) {
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null) {
                    if (value instanceof Bundle) {
                        flattenBundle(prefix + key + "_", list, (Bundle) value);
                    } else {
                        list.add(new BasicNameValuePair(prefix + key, toString(value)));
                    }
                }
            }
        }
    }

    // TODO: replace by a built-in
    private static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }
}
