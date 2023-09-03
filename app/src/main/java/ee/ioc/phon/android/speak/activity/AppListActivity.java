/*
 * Copyright 2011-2020, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import ee.ioc.phon.android.speak.AppListCursorAdapter;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.fragment.K6neleListFragment;
import ee.ioc.phon.android.speak.provider.App;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.utils.IntentUtils;

/**
 * <p>This activity shows the list of apps where the user
 * has used speech input before. The list content is pulled from
 * the database via AppListCursorAdapter. The user can interact with the
 * list items in various ways:</p>
 * <p/>
 * <ul>
 * <li>tapping on a list entry launches the corresponding app;</li>
 * <li>long-tapping on the entry opens the context menu which
 * allows the user to assign dedicated grammars and servers to individual apps;</li>
 * <li>the global menu allows the user to sort the list in various ways.</li>
 * </ul>
 * <p>
 */
public class AppListActivity extends AbstractContentActivity {

    private static final Uri CONTENT_URI = App.Columns.CONTENT_URI;

    private static final String[] COLUMNS = new String[]{
            App.Columns._ID,
            App.Columns.FNAME,
            App.Columns.GRAMMAR,
            App.Columns.SERVER,
            App.Columns.COUNT
    };

    private long mCurrentAppId;

    ActivityResultLauncher<Intent> mStartForResultGrammar = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    Uri grammarUri = intent.getData();
                    if (grammarUri == null) {
                        toast(getString(R.string.errorFailedGetGrammarUrl));
                    } else {
                        updateApp(mCurrentAppId, App.Columns.GRAMMAR, grammarUri);
                    }
                }
            });


    ActivityResultLauncher<Intent> mStartForResultServer = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    Uri serverUri = intent.getData();
                    if (serverUri == null) {
                        toast(getString(R.string.errorFailedGetServerUrl));
                    } else {
                        updateApp(mCurrentAppId, App.Columns.SERVER, serverUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppListActivity.CursorLoaderListFragment fragment = new AppListActivity.CursorLoaderListFragment();
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
    }

    private boolean menuAction(int itemId, Cursor cursor) {
        final long key = cursor.getLong(cursor.getColumnIndexOrThrow(App.Columns._ID));
        String fname = cursor.getString(cursor.getColumnIndexOrThrow(App.Columns.FNAME));
        mCurrentAppId = key;

        switch (itemId) {
            case R.id.cmAppAssignGrammar:
                Intent pickSpeakerIntent = new Intent(AppListActivity.this, GrammarListActivity.class);
                mStartForResultGrammar.launch(pickSpeakerIntent);
                return true;
            case R.id.cmAppRemoveGrammar:
                Utils.getYesNoDialog(
                        this,
                        getString(R.string.confirmRemoveGrammar),
                        () -> removeApp(key, App.Columns.GRAMMAR)
                ).show();
                return true;
            case R.id.cmAppAssignServer:
                Intent intentServer = new Intent(AppListActivity.this, ServerListActivity.class);
                mStartForResultServer.launch(intentServer);
                return true;
            case R.id.cmAppRemoveServer:
                Utils.getYesNoDialog(
                        this,
                        getString(R.string.confirmRemoveServer),
                        () -> removeApp(key, App.Columns.SERVER)
                ).show();
                return true;
            case R.id.cmAppDelete:
                Utils.getYesNoDialog(
                        this,
                        String.format(getString(R.string.confirmDeleteEntry), fname),
                        () -> delete(CONTENT_URI, key)
                ).show();
                return true;
            default:
                return false;
        }
    }

    private void updateApp(long key, String columnName, Uri uri) {
        long id = Long.parseLong(uri.getPathSegments().get(1));
        ContentValues values = new ContentValues();
        values.put(columnName, id);
        Uri appUri = ContentUris.withAppendedId(CONTENT_URI, key);
        getContentResolver().update(appUri, values, null, null);
    }


    // TODO: not sure which one to use: putNull vs remove
    private void removeApp(long key, String columnName) {
        ContentValues values = new ContentValues();
        values.putNull(columnName);
        //values.remove(columnName);
        Uri appUri = ContentUris.withAppendedId(CONTENT_URI, key);
        getContentResolver().update(appUri, values, null, null);
    }


    public static class CursorLoaderListFragment extends K6neleListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
        private AppListCursorAdapter mAdapter;
        private String mCurrentSortOrder = App.Columns.COUNT + " DESC";

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            mCurrentSortOrder = prefs.getString(getString(R.string.prefCurrentSortOrder), mCurrentSortOrder);

            setEmptyText(getString(R.string.emptylistApps));
            setHasOptionsMenu(true);
            mAdapter = getAdapter(mCurrentSortOrder);
            setListAdapter(mAdapter);
            registerForContextMenu(getListView());
            LoaderManager.getInstance(this).initLoader(0, null, this);
        }

        @Override
        public void onStop() {
            super.onStop();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.prefCurrentSortOrder), mCurrentSortOrder);
            editor.apply();
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.apps, menu);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            getActivity().getMenuInflater().inflate(R.menu.cm_app, menu);

            // Disable some menu items if they do not make sense in this context.
            // We could also remove them but this might be confusing for the user.

            final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            Cursor c = (Cursor) getListView().getItemAtPosition(info.position);
            long grammarId = c.getLong(c.getColumnIndexOrThrow(App.Columns.GRAMMAR));
            long serverId = c.getLong(c.getColumnIndexOrThrow(App.Columns.SERVER));
            if (grammarId == 0) {
                menu.findItem(R.id.cmAppRemoveGrammar).setEnabled(false);
            }
            if (serverId == 0) {
                menu.findItem(R.id.cmAppRemoveServer).setEnabled(false);
            }
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Cursor cursor = (Cursor) l.getItemAtPosition(position);
            String fname = cursor.getString(cursor.getColumnIndexOrThrow(App.Columns.FNAME));
            Intent intent = IntentUtils.getAppIntent(getContext(), fname);
            if (intent != null) {
                startActivity(intent);
            } else {
                toast(String.format(getString(R.string.errorFailedLaunchApp), fname));
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menuAppsSortByName:
                    setSortOrder(App.Columns.FNAME + " ASC");
                    return true;
                case R.id.menuAppsSortByCount:
                    setSortOrder(App.Columns.COUNT + " DESC");
                    return true;
                case R.id.menuAppsSortByGrammar:
                    setSortOrder(App.Columns.GRAMMAR + " DESC");
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }

        public boolean onContextItemSelected(MenuItem item) {
            final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
            boolean success = ((AppListActivity) getActivity()).menuAction(item.getItemId(), cursor);
            return success || super.onContextItemSelected(item);
        }

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
            return new CursorLoader(getActivity(),
                    CONTENT_URI,
                    COLUMNS,
                    null,
                    null,
                    mCurrentSortOrder);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }

        private AppListCursorAdapter getAdapter(String sortOrder) {
            Cursor managedCursor = getActivity().managedQuery(
                    CONTENT_URI,
                    COLUMNS,
                    null,
                    null,
                    sortOrder
            );

            return new AppListCursorAdapter(getContext(), managedCursor, true);
        }

        private void setSortOrder(String sortOrder) {
            mCurrentSortOrder = sortOrder;
            getListView().setAdapter(getAdapter(sortOrder));
        }
    }
}