package ee.ioc.phon.android.speak.model;

import java.util.Comparator;

public class Rewrites {

    public static final Comparator SORT_BY_ID = new Rewrites.SortById();
    public static final Comparator SORT_BY_SELECTED_BY_ID = new Rewrites.SortBySelectedById();

    private final String mId;
    private boolean mIsSelected;

    public Rewrites(String id) {
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

    public void setSelected(boolean b) {
        mIsSelected = b;
    }

    private static class SortById implements Comparator {

        public int compare(Object o1, Object o2) {
            Rewrites c1 = (Rewrites) o1;
            Rewrites c2 = (Rewrites) o2;
            return c1.getId().compareToIgnoreCase(c2.getId());
        }
    }

    private static class SortBySelectedById implements Comparator {

        public int compare(Object o1, Object o2) {
            Rewrites c1 = (Rewrites) o1;
            Rewrites c2 = (Rewrites) o2;
            if (c1.isSelected() && c2.isSelected() || !c1.isSelected() && !c2.isSelected()) {
                return c1.getId().compareToIgnoreCase(c2.getId());
            }
            if (c1.isSelected()) return -1;
            return 1;
        }
    }
}
