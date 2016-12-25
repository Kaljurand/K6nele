package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import ee.ioc.phon.android.speak.Executable;
import ee.ioc.phon.android.speak.ExecutableString;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.adapter.RewritesAdapter;
import ee.ioc.phon.android.speak.fragment.K6neleListFragment;
import ee.ioc.phon.android.speak.model.Rewrites;
import ee.ioc.phon.android.speak.utils.Utils;

public class RewritesSelectorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RewritesSelectorFragment fragment = new RewritesSelectorFragment();
        getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
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
            ListView listView = getListView();
            registerForContextMenu(listView);
            setEmptyView(getString(R.string.emptylistRewrites));
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
                case R.id.cmRewritesActivate:
                    boolean isActive = rewrites.toggle();
                    if (isActive) {
                        toast(String.format(getString(R.string.toastActivated), name));
                    } else {
                        toast(String.format(getString(R.string.toastDeactivated), name));
                    }
                    initAdapter();
                    return true;
                case R.id.cmRewritesShare:
                    Intent intent = rewrites.getShareIntent();
                    startActivity(Intent.createChooser(intent, getResources().getText(R.string.labelRewritesShare)));
                    return true;
                case R.id.cmRewritesTest:
                    startActivity(rewrites.getK6neleIntent());
                    return true;
                case R.id.cmRewritesRename:
                    Utils.getTextEntryDialog(
                            getActivity(),
                            getString(R.string.confirmRenameEntry),
                            name,
                            new ExecutableString() {
                                public void execute(String newName) {
                                    if (!newName.isEmpty()) {
                                        rewrites.rename(newName);
                                        initAdapter();
                                    }
                                }
                            }
                    ).show();
                    return true;
                case R.id.cmRewritesDelete:
                    Utils.getYesNoDialog(
                            getActivity(),
                            String.format(getString(R.string.confirmDeleteEntry), name),
                            new Executable() {
                                public void execute() {
                                    rewrites.delete();
                                    initAdapter();
                                }
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
            setListAdapter(new RewritesAdapter(this, Rewrites.getTables(mPrefs, mRes)));
        }
    }
}