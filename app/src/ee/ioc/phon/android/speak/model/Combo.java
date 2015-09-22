package ee.ioc.phon.android.speak.model;

import android.content.Context;
import android.util.Pair;

import java.util.Comparator;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.utils.Utils;

public class Combo {

    public static final Comparator SORT_BY_LANGUAGE = new SortByLanguage();
    public static final Comparator SORT_BY_SELECTED_BY_LANGUAGE = new SortBySelectedByLanguage();

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

    public String toString() {
        return String.format(mContext.getString(R.string.labelComboListItem), mService, mLanguage);
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean b) {
        mIsSelected = b;
    }

    private static class SortByLanguage implements Comparator {

        public int compare(Object o1, Object o2) {
            Combo c1 = (Combo) o1;
            Combo c2 = (Combo) o2;
            return c1.getLanguage().compareToIgnoreCase(c2.getLanguage());
        }
    }

    private static class SortBySelectedByLanguage implements Comparator {

        public int compare(Object o1, Object o2) {
            Combo c1 = (Combo) o1;
            Combo c2 = (Combo) o2;
            if (c1.isSelected() && c2.isSelected() || !c1.isSelected() && !c2.isSelected()) {
                return c1.getLanguage().compareToIgnoreCase(c2.getLanguage());
            }
            if (c1.isSelected()) return -1;
            return 1;
        }
    }
}