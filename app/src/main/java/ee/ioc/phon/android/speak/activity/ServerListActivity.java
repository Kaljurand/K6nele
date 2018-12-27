package ee.ioc.phon.android.speak.activity;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.net.MalformedURLException;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.provider.Server;
import ee.ioc.phon.android.speak.utils.Utils;

public class ServerListActivity extends AbstractContentActivity {

    private static final Uri CONTENT_URI = Server.Columns.CONTENT_URI;
    private static final String SORT_ORDER = Server.Columns.URL + " ASC";
    private static final String[] COLUMNS = new String[]{
            Server.Columns._ID,
            Server.Columns.URL
    };

    private static int[] TO = new int[]{
            R.id.itemServerId,
            R.id.itemServerUrl
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ServerListActivity.CursorLoaderListFragment fragment = new ServerListActivity.CursorLoaderListFragment();
        fragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuServersAdd:
                Utils.getTextEntryDialog(
                        this,
                        getString(R.string.dialogTitleNewServer),
                        getString(R.string.defaultUrlPrefix),
                        url -> {
                            try {
                                insertUrl(CONTENT_URI, Server.Columns.URL, url);
                            } catch (MalformedURLException e) {
                                toast(getString(R.string.exceptionMalformedUrl));
                            }
                        }
                ).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cm_server, menu);
    }

    private boolean menuAction(int itemId, Cursor cursor) {
        final long key = cursor.getLong(cursor.getColumnIndex(Server.Columns._ID));
        String url = cursor.getString(cursor.getColumnIndex(Server.Columns.URL));

        switch (itemId) {
            case R.id.cmServerEdit:
                Utils.getTextEntryDialog(
                        this,
                        getString(R.string.dialogTitleChangeServer),
                        url,
                        newUrl -> {
                            try {
                                updateUrl(CONTENT_URI, key, Server.Columns.URL, newUrl);
                            } catch (MalformedURLException e) {
                                toast(getString(R.string.exceptionMalformedUrl));
                            }
                        }
                ).show();
                return true;
            case R.id.cmServerDelete:
                Utils.getYesNoDialog(
                        this,
                        String.format(getString(R.string.confirmDeleteEntry), url),
                        () -> delete(CONTENT_URI, key)
                ).show();
                return true;
            default:
                return false;
        }
    }

    public static class CursorLoaderListFragment extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor> {
        private SimpleCursorAdapter mAdapter;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setEmptyText(getString(R.string.emptylistServers));
            setHasOptionsMenu(true);
            mAdapter = new SimpleCursorAdapter(
                    getActivity(),
                    R.layout.list_item_server,
                    null,
                    COLUMNS,
                    TO,
                    0
            );
            setListAdapter(mAdapter);
            registerForContextMenu(getListView());
            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.servers, menu);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Cursor cursor = (Cursor) l.getItemAtPosition(position);
            final long key = cursor.getLong(cursor.getColumnIndex(Server.Columns._ID));
            ((AbstractContentActivity) getActivity()).returnIntent(CONTENT_URI, key);
        }

        public boolean onContextItemSelected(android.view.MenuItem item) {
            final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
            boolean success = ((ServerListActivity) getActivity()).menuAction(item.getItemId(), cursor);
            return success || super.onContextItemSelected(item);
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(),
                    CONTENT_URI,
                    COLUMNS,
                    null,
                    null,
                    SORT_ORDER);
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
        }

        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }
    }
}