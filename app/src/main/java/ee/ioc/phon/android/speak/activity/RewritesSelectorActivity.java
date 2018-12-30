package ee.ioc.phon.android.speak.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.adapter.RewritesAdapter;
import ee.ioc.phon.android.speak.fragment.K6neleListFragment;
import ee.ioc.phon.android.speak.model.Combo;
import ee.ioc.phon.android.speak.model.Rewrites;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class RewritesSelectorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().add(android.R.id.content, new RewritesSelectorFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rewrites_selector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRewritesAdd:
                Intent intent = new Intent(this, RewritesLoaderActivity.class);
                startActivity(intent);
                return true;
            case R.id.menuRewritesHelp:
                Intent view = new Intent(Intent.ACTION_VIEW);
                view.setData(Uri.parse(getString(R.string.urlRewritesDoc)));
                startActivity(view);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public static class RewritesSelectorFragment extends K6neleListFragment {

        private SharedPreferences mPrefs;
        private Resources mRes;

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mRes = getResources();
        }

        @Override
        public void onResume() {
            super.onResume();
            initAdapter();
            registerForContextMenu(getListView());
            setEmptyView(getString(R.string.emptylistRewrites));
        }

        @Override
        public void onPause() {
            super.onPause();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                Context context = getContext();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                Resources res = getResources();
                List<Combo> selectedCombos = new ArrayList<>();
                Set<String> selectedCombosAsStrings = PreferenceUtils.getPrefStringSet(prefs, res, R.string.keyCombo);
                for (String combo : selectedCombosAsStrings) {
                    selectedCombos.add(new Combo(context, combo));
                }
                Utils.publishShortcuts(getActivity().getApplicationContext(),
                        selectedCombos,
                        PreferenceUtils.getPrefStringSet(prefs, res, R.string.defaultRewriteTables));
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.cm_rewrites, menu);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            final Rewrites rewrites = (Rewrites) getListView().getItemAtPosition(info.position);
            final String name = rewrites.getId();
            switch (item.getItemId()) {
                case R.id.cmRewritesShare:
                    startActivity(Intent.createChooser(rewrites.getSendIntent(), getResources().getText(R.string.labelRewritesShare)));
                    return true;
                case R.id.cmRewritesSendBase64:
                    startActivity(rewrites.getIntentSendBase64());
                    return true;
                case R.id.cmRewritesTest:
                    startActivity(rewrites.getK6neleIntent());
                    return true;
                case R.id.cmRewritesRename:
                    Utils.getTextEntryDialog(
                            getActivity(),
                            getString(R.string.confirmRename),
                            name,
                            newName -> {
                                if (!newName.isEmpty()) {
                                    rewrites.rename(newName);
                                    initAdapter();
                                }
                            }
                    ).show();
                    return true;
                case R.id.cmRewritesDelete:
                    Utils.getYesNoDialog(
                            getActivity(),
                            String.format(getString(R.string.confirmDelete), name),
                            () -> {
                                rewrites.delete();
                                initAdapter();
                            }
                    ).show();
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            startActivity(((Rewrites) l.getItemAtPosition(position)).getShowIntent());
        }

        private void initAdapter() {
            List<Rewrites> tables = Rewrites.getTables(mPrefs, mRes);
            setListAdapter(new RewritesAdapter(this, tables));
            ActionBar actionBar = getActivity().getActionBar();
            if (actionBar != null) {
                int count = tables.size();
                if (count == 0) {
                    actionBar.setSubtitle("");
                } else {
                    int countSelected = Rewrites.getDefaults(mPrefs, mRes).size();
                    actionBar.setSubtitle(mRes.getQuantityString(R.plurals.subtitleRewritesSelector, count, countSelected, count));
                }
            }
        }
    }
}