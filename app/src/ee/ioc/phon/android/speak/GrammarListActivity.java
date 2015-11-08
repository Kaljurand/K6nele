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

import java.net.MalformedURLException;
import java.net.URL;

import ee.ioc.phon.android.speak.provider.Grammar;
import ee.ioc.phon.android.speak.utils.Utils;

import android.content.ContentValues;
import android.content.Intent;
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
 * <p>This activity lists all the grammar URLs and allows the
 * user to add more. The list data comes from the Grammar-table
 * via the standard SimpleCursorAdapter. Long-tapping on a
 * list item allows the user to:</p>
 * 
 * <ul>
 * <li>view the content of the grammar (in a browser)</li>
 * <li>edit the entry, i.e. the name, URL, etc.</li>
 * <li>delete the entry</li>
 * </ul>
 * 
 * @author Kaarel Kaljurand
 */
public class GrammarListActivity extends RecognizerIntentListActivity {

	private static final Uri CONTENT_URI = Grammar.Columns.CONTENT_URI;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String[] columns = new String[] {
				Grammar.Columns._ID,
				Grammar.Columns.NAME,
				Grammar.Columns.LANG,
				Grammar.Columns.DESC,
				Grammar.Columns.URL
		};

		int[] to = new int[] {
				R.id.itemGrammarId,
				R.id.itemGrammarName,
				R.id.itemGrammarLang,
				R.id.itemGrammarDesc,
				R.id.itemGrammarUrl
		};


		Cursor managedCursor = managedQuery(
				CONTENT_URI,
				columns, 
				null,
				null,
				Grammar.Columns.NAME + " ASC"
		);

		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(
				this,
				R.layout.list_item_grammar,
				managedCursor,
				columns,
				to
		);

		ListView lv = getListView();
		setEmptyView(getString(R.string.emptylistGrammars));
		lv.setAdapter(mAdapter);

		registerForContextMenu(lv);
		setClickToFinish(CONTENT_URI, Grammar.Columns._ID);
	}


	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.grammars, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuGrammarsAdd:
			Utils.getTextEntryDialog(
					this,
					getString(R.string.dialogTitleNewGrammar),
					"",
					new ExecutableString() {
						public void execute(String url) {
							if (url.length() > 0) {
								try {
									new URL(url);
									String name = url.replaceFirst(".*\\/", "").replaceFirst("\\.[^.]*$", "");
									ContentValues values = new ContentValues();
									values.put(Grammar.Columns.NAME, name);
									values.put(Grammar.Columns.URL, url);
									insert(CONTENT_URI, values);
								} catch (MalformedURLException e) {
									toast(getString(R.string.exceptionMalformedUrl));
								}
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
		inflater.inflate(R.menu.cm_grammar, menu);
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
		final long key = cursor.getLong(cursor.getColumnIndex(Grammar.Columns._ID));
		String name = cursor.getString(cursor.getColumnIndex(Grammar.Columns.NAME));
		String grammarName = cursor.getString(cursor.getColumnIndex(Grammar.Columns.NAME));
		String grammarLang = cursor.getString(cursor.getColumnIndex(Grammar.Columns.LANG));
		String grammarUrl = cursor.getString(cursor.getColumnIndex(Grammar.Columns.URL));

		switch (item.getItemId()) {
		case R.id.cmGrammarView:
			Intent intentView = new Intent();  
			intentView.setAction(Intent.ACTION_VIEW);
			intentView.setDataAndType(Uri.parse(grammarUrl), "text/plain");  
			startActivity(intentView);
			return true;
		case R.id.cmGrammarEditName:
			Utils.getTextEntryDialog(
					this,
					getString(R.string.dialogTitleChangeGrammarName),
					grammarName,
					new ExecutableString() {
						public void execute(String name) {
							if (name != null && name.length() == 0) {
								name = null;
							}
							update(CONTENT_URI, key, Grammar.Columns.NAME, name);
						}
					}
			).show();
			return true;
		case R.id.cmGrammarEditLang:
			Utils.getTextEntryDialog(
					this,
					getString(R.string.dialogTitleChangeGrammarLang),
					grammarLang,
					new ExecutableString() {
						public void execute(String lang) {
							if (lang != null && lang.length() == 0) {
								lang = null;
							}
							update(CONTENT_URI, key, Grammar.Columns.LANG, lang);
						}
					}
			).show();
			return true;
		case R.id.cmGrammarEditUrl:
			Utils.getTextEntryDialog(
					this,
					getString(R.string.dialogTitleChangeGrammarUrl),
					grammarUrl,
					new ExecutableString() {
						public void execute(String newUrl) {
							try {
								updateUrl(CONTENT_URI, key, Grammar.Columns.URL, newUrl);
							} catch (MalformedURLException e) {
								toast(getString(R.string.exceptionMalformedUrl));
							}
						}
					}
			).show();
			return true;
		case R.id.cmGrammarDelete:
			Utils.getYesNoDialog(
					this,
					String.format(getString(R.string.confirmDeleteEntry), name),
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