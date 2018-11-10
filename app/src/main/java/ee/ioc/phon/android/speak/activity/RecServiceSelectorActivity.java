package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.adapter.RecServiceAdapter;
import ee.ioc.phon.android.speak.fragment.K6neleListFragment;
import ee.ioc.phon.android.speak.model.RecService;
import ee.ioc.phon.android.speechutils.RecognitionServiceManager;

public class RecServiceSelectorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SelectorFragment fragment = new SelectorFragment();
        fragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
    }

    public static class SelectorFragment extends K6neleListFragment {

        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            Activity activity = getActivity();
            RecognitionServiceManager mngr = new RecognitionServiceManager();
            List<RecService> list = new ArrayList<>();
            for (String comboAsString : mngr.getServices(activity.getPackageManager())) {
                list.add(new RecService(activity, comboAsString));
            }
            RecServiceAdapter adapter = new RecServiceAdapter(SelectorFragment.this, list);
            setListAdapter(adapter);
            //getActivity().getActionBar().setSubtitle("" + adapter.getCount());
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            RecService recservice = (RecService) l.getItemAtPosition(position);
            Intent intent = recservice.getSettingsIntent();
            if (intent == null) {
                toast(getString(R.string.errorRecognizerSettingsNotPresent));
            } else {
                startActivity(intent);
            }

            // TODO: context menu based add/remove to IME/search plain service (i.e. no languages)
            /*
            int mKey = R.string.keyImeCombo;
            int mDefaultCombos = R.array.defaultImeCombos;
            Resources res = getResources();
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Set<String> combos = PreferenceUtils.getPrefStringSet(mPrefs, res, mKey);
            if (combos == null) {
                combos = PreferenceUtils.getStringSetFromStringArray(res, mDefaultCombos);
            }
            combos.add(recservice.getComponentName().flattenToShortString());
            PreferenceUtils.putPrefStringSet(PreferenceManager.getDefaultSharedPreferences(getActivity()), getResources(), mKey, combos);
            */
        }
    }
}