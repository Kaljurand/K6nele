package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.speech.RecognitionService;

import java.util.List;

public class RecognitionServiceManager {

    private final Context mContext;
    private int mSelectedIndex = -1;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;


    RecognitionServiceManager(Context context, String preferredService, String selectedService) {
        mContext = context;
        populateRecognitionServices(preferredService, selectedService);
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public CharSequence[] getEntries() {
        return mEntries;
    }

    public CharSequence[] getEntryValues() {
        return mEntryValues;
    }


    /**
     * Populates the list of available recognizer services and adds a choice for the system default
     * service. If no service is currently selected (when the user accesses the preferences menu
     * for the first time), then selects the item that points to the preferredService (this is
     * Kõnele's own service).
     *
     * @param preferredService Service to select if none was selected
     */
    private void populateRecognitionServices(String preferredService, String selectedService) {
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(RecognitionService.SERVICE_INTERFACE), 0);

        int numberOfServices = services.size();

        // This should never happen because K6nele comes with several services
        if (numberOfServices == 0) {
            return;
        }

        int preferredIndex = 0;

        // Add one entry for the system default service
        mEntries = new CharSequence[numberOfServices + 1];
        mEntryValues = new CharSequence[numberOfServices + 1];

        // System default as the first listed choice
        mEntries[0] = mContext.getString(R.string.labelDefaultRecognitionService);
        mEntryValues[0] = "";

        int index = 1;
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (si == null) {
                Log.i("serviceInfo == null");
                continue;
            }
            String pkg = si.packageName;
            String cls = si.name;
            CharSequence label = si.loadLabel(pm);
            String component = (new ComponentName(pkg, cls)).flattenToShortString();
            Log.i(label + " :: " + component + " :: meta = " + Utils.ppBundle(si.metaData));
            mEntries[index] = label;
            mEntryValues[index] = component;
            Log.i("populateRecognitionServices: " + mEntryValues[index]);
            if (component.equals(selectedService)) {
                mSelectedIndex = index;
            } else if (component.equals(preferredService)) {
                preferredIndex = index;
            }
            index++;
        }

        if (mSelectedIndex == -1) {
            mSelectedIndex = preferredIndex;
        }
    }

}