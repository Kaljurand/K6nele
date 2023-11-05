package ee.ioc.phon.android.speak.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListAdapter;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.adapter.ComboAdapter;
import ee.ioc.phon.android.speak.fragment.K6neleListFragment;
import ee.ioc.phon.android.speak.model.Combo;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.RecognitionServiceManager;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

/**
 * @deprecated use Combo1SelectorActivity
 */
public class ComboSelectorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComboSelectorFragment fragment = new ComboSelectorFragment();
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.combos_header, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuServices:
                startActivity(new Intent(this, RecServiceSelectorActivity.class));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void onClicked(View view) {
        CheckBox cb = view.findViewById(R.id.check);
        cb.setChecked(!cb.isChecked());
    }

    public static class ComboSelectorFragment extends K6neleListFragment {

        int mKey = R.string.keyImeCombo;
        int mDefaultCombosExcluded = R.array.defaultImeCombosExcluded;

        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            Bundle args = getArguments();
            if (args != null && getString(R.string.keyCombo).equals(args.getString("key"))) {
                mKey = R.string.keyCombo;
                mDefaultCombosExcluded = R.array.defaultCombosExcluded;
            }
            initModel();
        }

        public void onPause() {
            super.onPause();
            ListAdapter listAdapter = getListAdapter();
            if (listAdapter instanceof ComboAdapter) {
                Set<String> selected = new HashSet<>();
                List<Combo> selectedCombos = new ArrayList<>();
                ComboAdapter comboAdapter = (ComboAdapter) listAdapter;
                for (int i = 0; i < comboAdapter.getCount(); i++) {
                    Combo combo = comboAdapter.getItem(i);
                    if (combo != null && combo.isSelected()) {
                        selected.add(combo.getId());
                        selectedCombos.add(combo);
                    }
                }
                PreferenceUtils.putPrefStringSet(PreferenceManager.getDefaultSharedPreferences(getActivity()), getResources(), mKey, selected);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                    // The app shortcuts correspond to the voice panel combo settings
                    if (mKey == R.string.keyCombo) {
                        Utils.publishShortcuts(getActivity().getApplicationContext(), selectedCombos, PreferenceUtils.getPrefStringSet(
                                PreferenceManager.getDefaultSharedPreferences(getActivity()),
                                getResources(),
                                R.string.defaultRewriteTables));
                    }
                }
            }
        }

        private void initModel() {
            Resources res = getResources();
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            RecognitionServiceManager mngr = new RecognitionServiceManager();
            mngr.setInitiallySelectedCombos(PreferenceUtils.getPrefStringSet(mPrefs, res, mKey));
            mngr.setCombosExcluded(PreferenceUtils.getStringSetFromStringArray(res, mDefaultCombosExcluded));
            mngr.populateCombos(getActivity(), (combos1, selectedCombos) -> {
                List<Combo> list = new ArrayList<>();
                for (String comboAsString : combos1) {
                    Combo combo = get(comboAsString);
                    if (selectedCombos.contains(comboAsString)) {
                        combo.setSelected(true);
                    }
                    list.add(combo);
                }
                Collections.sort(list, Combo.SORT_BY_SELECTED_BY_LANGUAGE);

                ComboAdapter adapter = new ComboAdapter(this, list);
                setListAdapter(adapter);

                // TODO: the fast scroll handle overlaps with the checkboxes
                //getListView().setFastScrollEnabled(true);

                // TODO: provide more info about the number of (selected) services and languages
                //getActivity().getActionBar().setSubtitle("" + adapter.getCount());
            });

        }

        private Combo get(String id) {
            return new Combo(getActivity(), id);
        }
    }
}