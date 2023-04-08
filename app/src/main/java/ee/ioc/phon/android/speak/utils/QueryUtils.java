package ee.ioc.phon.android.speak.utils;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speechutils.Extras;

public final class QueryUtils {
    private static final String QUERY_STRING_MARKER = "?";
    private static final String PARAMETER_SEPARATOR = "&";
    private static final String NAME_VALUE_SEPARATOR = "=";

    private QueryUtils() {
    }

    public static String combine(String server, String part) {
        return server + (server.indexOf(QUERY_STRING_MARKER) > 0 ? PARAMETER_SEPARATOR : QUERY_STRING_MARKER) + part;
    }

    /**
     * Extracts the editor info, and uses
     * ChunkedWebRecSessionBuilder to extract some additional extras.
     * TODO: unify this better
     */
    public static List<Pair<String, String>> getQueryParams(Intent intent, ChunkedWebRecSessionBuilder builder) {
        if (Log.DEBUG) Log.i(builder.toStringArrayList());
        List<Pair<String, String>> list = new ArrayList<>();
        flattenBundle("editorInfo_", list, intent.getBundleExtra(Extras.EXTRA_EDITOR_INFO));
        listAdd(list, "lang", builder.getLang());
        listAdd(list, "lm", Objects.toString(builder.getGrammarUrl(), ""));
        listAdd(list, "output-lang", builder.getGrammarTargetLang());
        listAdd(list, "user-agent", builder.getUserAgentComment());
        listAdd(list, "calling-package", builder.getCaller());
        listAdd(list, "user-id", builder.getDeviceId());
        listAdd(list, "partial", "" + builder.isPartialResults());
        return list;
    }

    private static void listAdd(List<Pair<String, String>> list, String key, String value) {
        if (value != null && value.length() > 0) {
            list.add(new Pair<>(key, value));
        }
    }

    private static void flattenBundle(String prefix, List<Pair<String, String>> list, Bundle bundle) {
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null) {
                    if (value instanceof Bundle) {
                        flattenBundle(prefix + key + "_", list, (Bundle) value);
                    } else {
                        listAdd(list, prefix + key, Objects.toString(value, ""));
                    }
                }
            }
        }
    }

    /**
     * Returns a String that is suitable for use as an <code>application/x-www-form-urlencoded</code>
     * list of parameters in an HTTP PUT or HTTP POST.
     * <p/>
     * Modification of org.apache.http.client.utils.URLEncodedUtils#format
     *
     * @param parameters The parameters to include.
     * @param encoding   The encoding to use.
     */
    public static String encodeKeyValuePairs(
            final List<Pair<String, String>> parameters,
            final String encoding) throws UnsupportedEncodingException {
        final StringBuilder result = new StringBuilder();
        for (final Pair<String, String> parameter : parameters) {
            final String encodedName = URLEncoder.encode(parameter.first, encoding);
            final String value = parameter.second;
            final String encodedValue = value != null ? URLEncoder.encode(value, encoding) : "";
            if (result.length() > 0)
                result.append(PARAMETER_SEPARATOR);
            result.append(encodedName);
            result.append(NAME_VALUE_SEPARATOR);
            result.append(encodedValue);
        }
        return result.toString();
    }
}