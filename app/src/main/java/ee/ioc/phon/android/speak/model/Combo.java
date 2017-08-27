package ee.ioc.phon.android.speak.model;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import java.util.Comparator;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speechutils.RecognitionServiceManager;

public class Combo {

    public static final Comparator SORT_BY_LANGUAGE = new SortByLanguage();
    public static final Comparator SORT_BY_SELECTED_BY_LANGUAGE = new SortBySelectedByLanguage();

    private final String mId;
    private final String mServiceLabel;
    private final String mLocaleShortLabel;
    private final String mLocaleLongLabel;
    private final String mAsString;
    private final String mFormatLabelComboItem;
    private final ComponentName mComponentName;
    private final String mLocaleAsStr;
    private boolean mIsSelected;

    public Combo(Context context, String id) {
        // Can return <null, "">
        Pair<ComponentName, String> pair = RecognitionServiceManager.unflattenFromString(id);
        mId = id;
        mComponentName = pair.first;
        mLocaleAsStr = pair.second;
        mServiceLabel = RecognitionServiceManager.getServiceLabel(context, mComponentName);
        mLocaleShortLabel = RecognitionServiceManager.getDisplayLanguage(mLocaleAsStr);
        mLocaleLongLabel = RecognitionServiceManager.makeLangLabel(mLocaleAsStr);
        mFormatLabelComboItem = context.getString(R.string.labelComboItem);
        mAsString = String.format(context.getString(R.string.labelComboListItem), mServiceLabel, mLocaleLongLabel);
    }

    public String getId() {
        return mId;
    }

    public ComponentName getServiceComponent() {
        return mComponentName;
    }

    public String getLocaleAsStr() {
        return mLocaleAsStr;
    }

    public String getService() {
        return mServiceLabel;
    }

    public String getLanguage() {
        return mLocaleLongLabel;
    }

    public String getShortLabel() {
        return String.format(mFormatLabelComboItem, mServiceLabel, mLocaleShortLabel);
    }

    public String getLongLabel() {
        return String.format(mFormatLabelComboItem, mServiceLabel, mLocaleLongLabel);
    }

    public Drawable getIcon(Context context) {
        return RecognitionServiceManager.getServiceIcon(context, mComponentName);
    }

    public String toString() {
        return mAsString;
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