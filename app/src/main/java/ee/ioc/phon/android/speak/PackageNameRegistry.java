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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import ee.ioc.phon.android.speak.provider.App;
import ee.ioc.phon.android.speak.provider.Grammar;
import ee.ioc.phon.android.speak.provider.Server;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.utils.IntentUtils;

/**
 * <p>Database front-end that looks up the grammar and the server URLs
 * that correspond to the given app package name.
 * If the package name is <code>null</code>, then just sets the grammar and server IDs to 0
 * and ignores the database.
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
        long grammarId = 0;
        long serverId = 0;

        if (packageName != null) {
            // Notice: we use query instead of managedQuery
            Cursor cursor = mContext.getContentResolver().query(
                    App.Columns.CONTENT_URI,
                    new String[]{App.Columns._ID, App.Columns.GRAMMAR, App.Columns.SERVER, App.Columns.COUNT},
                    App.Columns.FNAME + "= ?",
                    new String[]{packageName},
                    null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    grammarId = cursor.getLong(cursor.getColumnIndexOrThrow(App.Columns.GRAMMAR));
                    serverId = cursor.getLong(cursor.getColumnIndexOrThrow(App.Columns.SERVER));
                }
                cursor.close();
            }
        }
        mGrammarId = grammarId;
        mServerId = serverId;
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

    /**
     * Extracts the package name from the given extras and/or the calling activity.
     * If the package name is not <code>null</code> and not in the database, then adds it there.
     * Increases the package counter (i.e. a number that shows how many times this lookup has been called).
     */
    public static void increaseAppCount(Context context, Bundle extras, ComponentName callingActivity) {
        PendingIntent pendingIntent = IntentUtils.getPendingIntent(extras);
        String packageName;
        if (callingActivity == null) {
            Caller caller1 = new Caller(pendingIntent, extras);
            packageName = caller1.getActualCaller();
        } else {
            // TODO: this is never called
            packageName = Caller.getCaller(callingActivity, pendingIntent);
        }

        if (packageName != null) {
            // Notice: we use query instead of managedQuery
            Cursor cursor = context.getContentResolver().query(
                    App.Columns.CONTENT_URI,
                    new String[]{App.Columns._ID, App.Columns.GRAMMAR, App.Columns.SERVER, App.Columns.COUNT},
                    App.Columns.FNAME + "= ?",
                    new String[]{packageName},
                    null);

            if (cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(App.Columns._ID));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow(App.Columns.COUNT));

                ContentValues values = new ContentValues();
                values.put(App.Columns.COUNT, ++count);
                Uri appUri = ContentUris.withAppendedId(App.Columns.CONTENT_URI, id);
                context.getContentResolver().update(appUri, values, null, null);
            } else {
                ContentValues values = new ContentValues();
                values.put(App.Columns.FNAME, packageName);
                values.put(App.Columns.COUNT, 1);
                context.getContentResolver().insert(App.Columns.CONTENT_URI, values);
            }
            cursor.close();
        }
    }
}