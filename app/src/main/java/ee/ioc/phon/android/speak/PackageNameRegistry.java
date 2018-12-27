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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import ee.ioc.phon.android.speak.provider.App;
import ee.ioc.phon.android.speak.provider.Grammar;
import ee.ioc.phon.android.speak.provider.Server;
import ee.ioc.phon.android.speak.utils.Utils;

/**
 * <p>Database front-end that looks up the grammar and the server URLs
 * that correspond to the given app package name.
 * If the package name is <code>null</code>, then just sets the grammar and server IDs to 0
 * and ignores the database.
 * If the package name is not <code>null</code> and not in the database, then adds it there.
 * Increases the package counter (i.e. a number that shows how many times this lookup has been called).
 *
 * <p>If the grammar ID is 0 then getGrammarUrl() returns <code>null</code>, otherwise returns the grammar URL.
 * If the server ID is 0 then getServerUrl() returns the default server URL, otherwise the listed URL.</p>
 *
 * @author Kaarel Kaljurand
 */
public class PackageNameRegistry {

    private final Context mContext;
    private final long mGrammarId;
    private final long mServerId;


    public PackageNameRegistry(Context context, String packageName) {
        mContext = context;

        if (packageName == null) {
            mGrammarId = 0;
            mServerId = 0;
        } else {
            // Notice: we use query instead of managedQuery
            Cursor cursor = mContext.getContentResolver().query(
                    App.Columns.CONTENT_URI,
                    new String[]{App.Columns._ID, App.Columns.GRAMMAR, App.Columns.SERVER, App.Columns.COUNT},
                    App.Columns.FNAME + "= ?",
                    new String[]{packageName},
                    null);

            if (cursor.moveToFirst()) {
                mGrammarId = cursor.getLong(cursor.getColumnIndex(App.Columns.GRAMMAR));
                mServerId = cursor.getLong(cursor.getColumnIndex(App.Columns.SERVER));
                long id = cursor.getLong(cursor.getColumnIndex(App.Columns._ID));
                int count = cursor.getInt(cursor.getColumnIndex(App.Columns.COUNT));

                ContentValues values = new ContentValues();
                values.put(App.Columns.COUNT, ++count);
                Uri appUri = ContentUris.withAppendedId(App.Columns.CONTENT_URI, id);
                mContext.getContentResolver().update(appUri, values, null, null);
            } else {
                mGrammarId = 0;
                mServerId = 0;
                ContentValues values = new ContentValues();
                values.put(App.Columns.FNAME, packageName);
                values.put(App.Columns.COUNT, 1);
                mContext.getContentResolver().insert(App.Columns.CONTENT_URI, values);
            }
            cursor.close();
        }
    }


    public String getGrammarUrl() {
        if (mGrammarId == 0) {
            return null;
        }
        return Utils.idToValue(
                mContext,
                Grammar.Columns.CONTENT_URI,
                Grammar.Columns._ID,
                Grammar.Columns.URL,
                mGrammarId);
    }


    public String getGrammarLang() {
        if (mGrammarId == 0) {
            return null;
        }
        return Utils.idToValue(
                mContext,
                Grammar.Columns.CONTENT_URI,
                Grammar.Columns._ID,
                Grammar.Columns.LANG,
                mGrammarId);
    }


    public String getServerUrl() {
        if (mServerId == 0) {
            return null;
        }
        return Utils.idToValue(
                mContext,
                Server.Columns.CONTENT_URI,
                Server.Columns._ID,
                Server.Columns.URL,
                mServerId);
    }
}