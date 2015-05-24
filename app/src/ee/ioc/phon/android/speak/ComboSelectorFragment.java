package ee.ioc.phon.android.speak;

import android.app.ListFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.adapter.ComboAdapter;
import ee.ioc.phon.android.speak.model.Combo;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;

public class ComboSelectorFragment extends ListFragment {

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        initModel();
    }

    public void onPause() {
        super.onPause();
        ArrayAdapter<Combo> adapter = (ArrayAdapter<Combo>) getListAdapter();
        Set<String> selected = new HashSet<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            Combo combo = adapter.getItem(i);
            if (combo.isSelected()) {
                selected.add(combo.getId());
            }
        }
        PreferenceUtils.putPrefStringSet(PreferenceManager.getDefaultSharedPreferences(getActivity()), getResources(), R.string.keyImeCombo, selected);
    }

    private void initModel() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Set<String> combos = PreferenceUtils.getPrefStringSet(mPrefs, getResources(), R.string.keyImeCombo);
        RecognitionServiceManager mngr = new RecognitionServiceManager(getActivity(), combos);
        mngr.populateCombos(getActivity(), new RecognitionServiceManager.Listener() {

            @Override
            public void onComplete(List<String> combos, List<String> combosPp, Set<String> selectedCombos) {
                List<Combo> list = new ArrayList<Combo>();
                for (String comboAsString : combos) {
                    Combo combo = get(comboAsString);
                    if (selectedCombos.contains(comboAsString)) {
                        combo.setSelected(true);
                    }
                    list.add(combo);
                }
                Collections.sort(list, Combo.SORT_BY_LANGAUGE);

                ArrayAdapter<Combo> adapter = new ComboAdapter(ComboSelectorFragment.this, list);
                setListAdapter(adapter);
            }
        });

    }

    private Combo get(String id) {
        return new Combo(getActivity(), id);
    }
}