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
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>Some methods that various list activities can share by extending
 * this class rather than extending the ListActivity-class directly.</p>
 *
 * @author Kaarel Kaljurand
 * @deprecated use K6neleListFragment
 */
public abstract class RecognizerIntentListActivity extends ListActivity {

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }


    protected void setEmptyView(String text) {
        ListView lv = getListView();
        TextView emptyView = (TextView) getLayoutInflater().inflate(R.layout.empty_list, null);
        emptyView.setText(text);
        emptyView.setVisibility(View.GONE);
        ((ViewGroup) lv.getParent()).addView(emptyView);
        lv.setEmptyView(emptyView);
    }


    protected void setClickToFinish(final Uri contentUri, final String columnName) {
        getListView().setOnItemClickListener((parent, view, position, id) -> {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            final long key = cursor.getLong(cursor.getColumnIndex(columnName));
            Intent intent = new Intent();
            intent.setData(ContentUris.withAppendedId(contentUri, key));
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }


    protected void insertUrl(Uri contentUri, String fieldKey, String url) throws MalformedURLException {
        if (url.length() > 0) {
            new URL(url);
            ContentValues values = new ContentValues();
            values.put(fieldKey, url);
            insert(contentUri, values);
        }
    }


    protected void insert(Uri contentUri, ContentValues values) {
        getContentResolver().insert(contentUri, values);
    }


    protected void updateUrl(Uri contentUri, long key, String fieldKey, String url) throws MalformedURLException {
        new URL(url);
        update(contentUri, key, fieldKey, url);
    }


    protected void update(Uri contentUri, long key, String fieldKey, String str) {
        ContentValues values = new ContentValues();
        values.put(fieldKey, str);
        update(contentUri, key, values);
    }


    protected void update(Uri contentUri, long key, ContentValues values) {
        Uri uri = ContentUris.withAppendedId(contentUri, key);
        getContentResolver().update(uri, values, null, null);
    }


    protected void delete(Uri contentUri, long key) {
        Uri uri = ContentUris.withAppendedId(contentUri, key);
        getContentResolver().delete(uri, null, null);
    }
}