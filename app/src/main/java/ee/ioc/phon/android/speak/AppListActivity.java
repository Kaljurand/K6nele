/*
 * Copyright 2011, Institute of Cybernetics at Tallinn University of Technology
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

package ee.ioc.phon.android.speak;

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

import ee.ioc.phon.android.speak.activity.GrammarListActivity;
import ee.ioc.phon.android.speak.activity.ServerListActivity;
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
 * TODO: implement using CursorLoader (see e.g. ServerListActivity)
 */
public class AppListActivity extends RecognizerIntentListActivity {

    private static final int ACTIVITY_SELECT_GRAMMAR_URL = 1;
    private static final int ACTIVITY_SELECT_SERVER_URL = 2;

    private static final Uri CONTENT_URI = App.Columns.CONTENT_URI;

    private String mCurrentSortOrder;

    private long mCurrentAppId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentSortOrder = prefs.getString(getString(R.string.prefCurrentSortOrder), App.Columns.COUNT + " DESC");

        ListView lv = getListView();
        setEmptyView(getString(R.string.emptylistApps));
        AppListCursorAdapter mAdapter = getAdapter(mCurrentSortOrder);
        lv.setAdapter(mAdapter);

        registerForContextMenu(lv);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            String fname = cursor.getString(cursor.getColumnIndex(App.Columns.FNAME));
            Intent intent = IntentUtils.getAppIntent(getApplicationContext(), fname);
            if (intent != null) {
                startActivity(intent);
            } else {
                toast(String.format(getString(R.string.errorFailedLaunchApp), fname));
            }
        });
    }


    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.prefCurrentSortOrder), mCurrentSortOrder);
        editor.apply();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.apps, menu);
        return true;
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


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cm_app, menu);

        // Disable some menu items if they do not make sense in this context.
        // We could also remove them but this might be confusing for the user.
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor c = (Cursor) getListView().getItemAtPosition(info.position);
        long grammarId = c.getLong(c.getColumnIndex(App.Columns.GRAMMAR));
        long serverId = c.getLong(c.getColumnIndex(App.Columns.SERVER));
        if (grammarId == 0) {
            menu.findItem(R.id.cmAppRemoveGrammar).setEnabled(false);
        }
        if (serverId == 0) {
            menu.findItem(R.id.cmAppRemoveServer).setEnabled(false);
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
        final long key = cursor.getLong(cursor.getColumnIndex(App.Columns._ID));
        String fname = cursor.getString(cursor.getColumnIndex(App.Columns.FNAME));
        mCurrentAppId = key;

        switch (item.getItemId()) {
            case R.id.cmAppAssignGrammar:
                Intent pickSpeakerIntent = new Intent(AppListActivity.this, GrammarListActivity.class);
                startActivityForResult(pickSpeakerIntent, ACTIVITY_SELECT_GRAMMAR_URL);
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
                startActivityForResult(intentServer, ACTIVITY_SELECT_SERVER_URL);
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
                return super.onContextItemSelected(item);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case ACTIVITY_SELECT_GRAMMAR_URL:
                Uri grammarUri = data.getData();
                if (grammarUri == null) {
                    toast(getString(R.string.errorFailedGetGrammarUrl));
                } else {
                    updateApp(mCurrentAppId, App.Columns.GRAMMAR, grammarUri);
                }
                break;
            case ACTIVITY_SELECT_SERVER_URL:
                Uri serverUri = data.getData();
                if (serverUri == null) {
                    toast(getString(R.string.errorFailedGetServerUrl));
                } else {
                    updateApp(mCurrentAppId, App.Columns.SERVER, serverUri);
                }
                break;
            default:
                break;
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


    private AppListCursorAdapter getAdapter(String sortOrder) {
        String[] columns = new String[]{
                App.Columns._ID,
                App.Columns.FNAME,
                App.Columns.GRAMMAR,
                App.Columns.SERVER,
                App.Columns.COUNT
        };

        Cursor managedCursor = managedQuery(
                CONTENT_URI,
                columns,
                null,
                null,
                sortOrder
        );

        return new AppListCursorAdapter(this, managedCursor, true);
    }


    private void setSortOrder(String sortOrder) {
        mCurrentSortOrder = sortOrder;
        getListView().setAdapter(getAdapter(sortOrder));
    }
}