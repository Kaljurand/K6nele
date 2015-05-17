package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.speech.RecognitionService;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.utils.PreferenceUtils;

public class RecognitionServiceManager {

    private final Context mContext;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private Set<String> mValues;
    private String mValuesPp;

    RecognitionServiceManager(Context context, Set<String> combos, int resFallbackCombos) {
        mContext = context;
        Set<String> fallbackCombos = PreferenceUtils.getStringSetFromStringArray(mContext.getResources(), resFallbackCombos);
        populateRecognitionServiceLanguageSet(combos, fallbackCombos);
    }

    public CharSequence[] getEntries() {
        return mEntries;
    }

    public CharSequence[] getEntryValues() {
        return mEntryValues;
    }

    public Set<String> getValues() {
        return mValues;
    }

    public String getValuesPp() {
        return mValuesPp;
    }


    /**
     * TODO: decide which should be selected based on the (possibly stored) selection, or if null/empty
     * then the fallbackCombo.
     */
    private void populateRecognitionServiceLanguageSet(Set<String> combos, Set<String> fallbackCombos) {
        PackageManager pm = mContext.getPackageManager();
        int flags = 0;
        //int flags = PackageManager.GET_META_DATA;
        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(RecognitionService.SERVICE_INTERFACE), flags);

        int numberOfServices = services.size();

        mEntries = new CharSequence[numberOfServices];
        mEntryValues = new CharSequence[numberOfServices];

        Set<String> selectedCombos = new HashSet<>();

        int index = 0;
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
            Log.i(index + ") " + label + ": " + component + ": meta = " + Utils.ppBundle(si.metaData));
            mEntries[index] = label;
            mEntryValues[index] = component;
            if (combos.contains(component)) {
                selectedCombos.add(component);
            }
            index++;
        }

        if (selectedCombos.isEmpty()) {
            mValues = fallbackCombos;
        } else {
            mValues = selectedCombos;
        }

        mValuesPp = TextUtils.join("\n", mValues);
    }
}