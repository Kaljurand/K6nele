/*
 * Copyright 2011-2012, Institute of Cybernetics at Tallinn University of Technology
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

import java.net.MalformedURLException;

import ee.ioc.phon.android.speak.provider.Server;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * <p>This activity displays the list of speech recognition server URLs
 * and allows the user to add/edit/remove entries. The data is
 * populated via SimpleCursorAdapter from the Servers-table.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class ServerListActivity extends RecognizerIntentListActivity {

	private static final Uri CONTENT_URI = Server.Columns.CONTENT_URI;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String[] columns = new String[] {
				Server.Columns._ID,
				Server.Columns.URL
		};

		int[] to = new int[] {
				R.id.itemServerId,
				R.id.itemServerUrl
		};


		Cursor managedCursor = managedQuery(
				CONTENT_URI,
				columns, 
				null,
				null,
				Server.Columns.URL + " ASC"
				);

		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(
				this,
				R.layout.list_item_server,
				managedCursor,
				columns,
				to
				);

		ListView lv = getListView();
		setEmptyView(getString(R.string.emptylistServers));
		lv.setAdapter(mAdapter);

		registerForContextMenu(lv);

		setClickToFinish(CONTENT_URI, Server.Columns._ID);
	}


	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.servers, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuServersAdd:
			Utils.getTextEntryDialog(
					this,
					getString(R.string.dialogTitleNewServer),
					"",
					new ExecutableString() {
						public void execute(String url) {
							try {
								insertUrl(CONTENT_URI, Server.Columns.URL, url);
							} catch (MalformedURLException e) {
								toast(getString(R.string.exceptionMalformedUrl));
							}
						}
					}
					).show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.cm_server, menu);
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
		final long key = cursor.getLong(cursor.getColumnIndex(Server.Columns._ID));
		String url = cursor.getString(cursor.getColumnIndex(Server.Columns.URL));

		switch (item.getItemId()) {
		case R.id.cmServerEdit:
			Utils.getTextEntryDialog(
					this,
					getString(R.string.dialogTitleChangeServer),
					url,
					new ExecutableString() {
						public void execute(String newUrl) {
							try {
								updateUrl(CONTENT_URI, key, Server.Columns.URL, newUrl);
							} catch (MalformedURLException e) {
								toast(getString(R.string.exceptionMalformedUrl));
							}
						}
					}
					).show();
			return true;
		case R.id.cmServerDelete:
			Utils.getYesNoDialog(
					this,
					String.format(getString(R.string.confirmDeleteEntry), url),
					new Executable() {
						public void execute() {
							delete(CONTENT_URI, key);
						}
					}
					).show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
}