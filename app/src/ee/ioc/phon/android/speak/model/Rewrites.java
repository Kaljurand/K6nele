package ee.ioc.phon.android.speak.model;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.speech.RecognizerIntent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.activity.DetailsActivity;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class Rewrites {

    private static final Comparator SORT_BY_ID = new Rewrites.SortById();

    private SharedPreferences mPrefs;
    private Resources mRes;

    private final String mId;
    private boolean mIsSelected;

    private Rewrites(SharedPreferences prefs, Resources res, String id) {
        mPrefs = prefs;
        mRes = res;
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public String toString() {
        return mId;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    private void setSelected(boolean b) {
        mIsSelected = b;
    }

    public boolean toggle() {
        String currentDefault = PreferenceUtils.getPrefString(mPrefs, mRes, R.string.defaultRewritesName);
        if (mId.equals(currentDefault)) {
            PreferenceUtils.putPrefString(mPrefs, mRes, R.string.defaultRewritesName, null);
            return false;
        } else {
            PreferenceUtils.putPrefString(mPrefs, mRes, R.string.defaultRewritesName, mId);
            return true;
        }
    }

    public Intent getK6neleIntent() {
        Intent intent = new Intent();
        intent.setClassName("ee.ioc.phon.android.speak", "ee.ioc.phon.android.speak.activity.SpeechActionActivity");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, mId);
        intent.putExtra(Extras.EXTRA_RESULT_REWRITES, new String[]{mId});
        return intent;
    }

    public Intent getShowIntent() {
        String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, mId);
        UtteranceRewriter ur = new UtteranceRewriter(rewrites);
        int count = ur.size();
        Intent intent = new Intent();
        intent.setClassName("ee.ioc.phon.android.speak", "ee.ioc.phon.android.speak.activity.RewritesActivity");
        intent.putExtra(DetailsActivity.EXTRA_TITLE, mId + " · " + mRes.getQuantityString(R.plurals.statusLoadRewrites, count, count));
        intent.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, ur.toStringArray());
        return intent;
    }

    public Intent getSendIntent() {
        String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, mId);
        UtteranceRewriter ur = new UtteranceRewriter(rewrites);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, mId);
        intent.putExtra(Intent.EXTRA_TEXT, ur.toTsv());
        intent.setType("text/tab-separated-values");
        return intent;
    }

    public void rename(String newName) {
        String rewrites = PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, mId);
        PreferenceUtils.putPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, newName, rewrites);
        delete();
        String currentDefault = PreferenceUtils.getPrefString(mPrefs, mRes, R.string.defaultRewritesName);
        if (mId.equals(currentDefault)) {
            PreferenceUtils.putPrefString(mPrefs, mRes, R.string.defaultRewritesName, newName);
        }
    }

    public void delete() {
        Set<String> deleteKeys = new HashSet<>();
        deleteKeys.add(mId);
        PreferenceUtils.clearPrefMap(mPrefs, mRes, R.string.keyRewritesMap, deleteKeys);
    }

    public static List<Rewrites> getTables(SharedPreferences prefs, Resources res) {
        String currentDefault = PreferenceUtils.getPrefString(prefs, res, R.string.defaultRewritesName);
        List<String> rewritesIds = new ArrayList<>(PreferenceUtils.getPrefMapKeys(prefs, res, R.string.keyRewritesMap));
        List<Rewrites> rewritesTables = new ArrayList<>();
        for (String id : rewritesIds) {
            Rewrites rewrites = new Rewrites(prefs, res, id);
            rewrites.setSelected(id.equals(currentDefault));
            rewritesTables.add(rewrites);
        }
        Collections.sort(rewritesTables, SORT_BY_ID);
        return rewritesTables;
    }

    private static class SortById implements Comparator {

        public int compare(Object o1, Object o2) {
            Rewrites c1 = (Rewrites) o1;
            Rewrites c2 = (Rewrites) o2;
            return c1.getId().compareToIgnoreCase(c2.getId());
        }
    }
}