package ee.ioc.phon.android.speak.model;

import android.content.Context;
import android.util.Pair;

import java.util.Comparator;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.Utils;

public class Combo {

    public static final SortByLangauge SORT_BY_LANGAUGE = new SortByLangauge();

    private final String mId;
    private final String mService;
    private final String mLanguage;
    private final Context mContext;
    private boolean mIsSelected;

    public Combo(Context context, String id) {
        mContext = context;
        mId = id;
        Pair<String, String> comboPair = Utils.getLabel(context, id);
        mService = comboPair.first;
        mLanguage = comboPair.second;
    }

    public String getId() {
        return mId;
    }

    public String getService() {
        return mService;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public String getName() {
        return String.format(mContext.getString(R.string.labelComboListItem), mService, mLanguage);
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean b) {
        mIsSelected = b;
    }

    private static class SortByLangauge implements Comparator {

        public int compare(Object o1, Object o2) {
            Combo c1 = (Combo) o1;
            Combo c2 = (Combo) o2;
            return c1.getLanguage().compareToIgnoreCase(c2.getLanguage());
        }
    }
}