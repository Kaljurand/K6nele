package ee.ioc.phon.android.speak;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.utils.PreferenceUtils;

public class RecognitionServiceManager {

    private final PackageManager mPm;
    private List<String> mServices = new ArrayList<>();
    private Set<String> mInitiallySelectedCombos = new HashSet<>();
    private Set<String> mCombosExcluded = new HashSet<>();

    interface Listener {
        void onComplete(List<String> combos, Set<String> selectedCombos);
    }

    RecognitionServiceManager(Context context, Set<String> selectedCombos) {
        mPm = context.getPackageManager();

        Resources res = context.getResources();

        mCombosExcluded = PreferenceUtils.getStringSetFromStringArray(res, R.array.defaultImeCombosExcluded);

        if (selectedCombos == null) {
            mInitiallySelectedCombos = PreferenceUtils.getStringSetFromStringArray(res, R.array.defaultImeCombos);
        } else {
            mInitiallySelectedCombos = selectedCombos;
        }
        populateServices();
    }

    /**
     * Collect together the languages supported by the given services and call back once done.
     */
    public void populateCombos(Activity activity, final Listener listener) {
        populateCombos(activity, 0, listener, new ArrayList<String>(), new HashSet<String>());
    }

    private void populateCombos(final Activity activity, final int counter, final Listener listener,
                                final List<String> combos, final Set<String> selectedCombos) {

        if (mServices.size() == counter) {
            listener.onComplete(combos, selectedCombos);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        // TODO: this seems to be only for activities that implement ACTION_WEB_SEARCH
        //Intent intent = RecognizerIntent.getVoiceDetailsIntent(this);

        final String service = mServices.get(counter);
        ComponentName serviceComponent = ComponentName.unflattenFromString(service);
        if (serviceComponent != null) {
            intent.setPackage(serviceComponent.getPackageName());
            // TODO: ideally we would like to query the component, because the package might
            // contain services (= components) with different capabilities.
            //intent.setComponent(serviceComponent);
        }

        // This is needed to include newly installed apps or stopped apps
        // as receivers of the broadcast.
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        activity.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                // Service that does not report which languages it supports
                if (getResultCode() != Activity.RESULT_OK) {
                    Log.i(combos.size() + ") NO LANG: " + service);
                    combos.add(service);
                    populateCombos(activity, counter + 1, listener, combos, selectedCombos);
                    return;
                }

                Bundle results = getResultExtras(true);

                // Supported languages
                String prefLang = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
                ArrayList<CharSequence> allLangs = results.getCharSequenceArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);

                Log.i("Supported langs: " + prefLang + ": " + allLangs);
                if (allLangs == null) {
                    allLangs = new ArrayList<>();
                }
                // We add the preferred language to the list of supported languages, if not already there.
                if (prefLang != null && !allLangs.contains(prefLang)) {
                    allLangs.add(prefLang);
                }

                for (CharSequence lang : allLangs) {
                    String combo = service + ";" + lang;
                    if (!mCombosExcluded.contains(combo)) {
                        String langPp = Utils.makeLangLabel(lang.toString());
                        Log.i(combos.size() + ") " + combo);
                        combos.add(combo);
                        if (mInitiallySelectedCombos.contains(combo)) {
                            selectedCombos.add(combo);
                        }
                    }
                }

                populateCombos(activity, counter + 1, listener, combos, selectedCombos);
            }
        }, null, Activity.RESULT_OK, null, null);
    }


    private void populateServices() {
        int flags = 0;
        //int flags = PackageManager.GET_META_DATA;
        List<ResolveInfo> services = mPm.queryIntentServices(
                new Intent(RecognitionService.SERVICE_INTERFACE), flags);

        int index = 0;
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (si == null) {
                Log.i("serviceInfo == null");
                continue;
            }
            String pkg = si.packageName;
            String cls = si.name;
            String component = (new ComponentName(pkg, cls)).flattenToShortString();
            if (!mCombosExcluded.contains(component)) {
                Log.i(index + ") " + component + ": meta = " + Utils.ppBundle(si.metaData));
                mServices.add(component);
                index++;
            }
        }
    }
}