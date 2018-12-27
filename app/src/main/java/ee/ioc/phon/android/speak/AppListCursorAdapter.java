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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import ee.ioc.phon.android.speak.provider.App;
import ee.ioc.phon.android.speak.provider.Grammar;
import ee.ioc.phon.android.speak.provider.Server;
import ee.ioc.phon.android.speak.utils.Utils;

/**
 * <p>Frontend that merges data from three database tables (Apps, Grammars, Servers)
 * and presents it in one list.</p>
 *
 * @author Kaarel Kaljurand
 */
public class AppListCursorAdapter extends CursorAdapter {

    private String mDefaultServerUrl;
    private PackageManager mPm;

    public AppListCursorAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mDefaultServerUrl = prefs.getString(context.getString(R.string.keyHttpServer), context.getString(R.string.defaultHttpServer));
        mPm = context.getPackageManager();
    }


    @Override
    public void bindView(View view, Context context, Cursor c) {
        String packageName = c.getString(c.getColumnIndex(App.Columns.FNAME));

        String label = "";
        Drawable icon = null;
        try {
            label = mPm.getApplicationLabel(mPm.getApplicationInfo(packageName, 0)).toString();
            icon = mPm.getApplicationIcon(packageName);
        } catch (NameNotFoundException e) {
            // Intentionally empty
        }

        // App label which can be "" if the app has been uninstalled.
        TextView itemAppName = view.findViewById(R.id.itemAppName);
        itemAppName.setText(label);

        // App package name (comes from the DB)
        TextView itemAppFname = view.findViewById(R.id.itemAppFname);
        itemAppFname.setText(packageName);

        // App usage count (comes from the DB)
        TextView itemAppCount = view.findViewById(R.id.itemAppCount);
        itemAppCount.setText(String.valueOf(c.getInt(c.getColumnIndex(App.Columns.COUNT))));

        // App icon (can be null if the app has been uninstalled)
        ImageView itemAppIcon = view.findViewById(R.id.itemAppIcon);
        if (icon == null) {
            itemAppIcon.setVisibility(View.INVISIBLE);
        } else {
            itemAppIcon.setVisibility(View.VISIBLE);
            itemAppIcon.setImageDrawable(icon);
        }

        // Grammar URL assigned to the app (comes from the DB)
        TextView itemAppGrammar = view.findViewById(R.id.itemAppGrammar);
        TextView itemAppGrammarTargetLang = view.findViewById(R.id.itemAppGrammarTargetLang);
        long grammarId = c.getLong(c.getColumnIndex(App.Columns.GRAMMAR));

        String grammarUrl = Utils.idToValue(context, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.URL, grammarId);
        String grammarTargetLang = Utils.idToValue(context, Grammar.Columns.CONTENT_URI, Grammar.Columns._ID, Grammar.Columns.LANG, grammarId);

        if (grammarUrl == null) {
            // This can happen in two cases:
            // 1. The app has not been assigned a grammar
            // 2. The app had a grammar but it was deleted
            // (with a foreign key constraint in place the 2nd option would
            // merged into the 1st, but until then we would simply have
            // an id that can never apply anymore).
            itemAppGrammar.setVisibility(View.GONE);
            itemAppGrammarTargetLang.setVisibility(View.GONE);
        } else if (grammarUrl.length() == 0) {
            // This can happen if the user wants to use unrestricted
            // input with this app, and has assigned a grammar URL which
            // is an empty string. This this case the target language makes no sense.
            itemAppGrammar.setVisibility(View.VISIBLE);
            itemAppGrammarTargetLang.setVisibility(View.GONE);
            itemAppGrammar.setText(context.getString(R.string.entryGrammarName1));
        } else {
            itemAppGrammar.setVisibility(View.VISIBLE);
            itemAppGrammarTargetLang.setVisibility(View.VISIBLE);
            itemAppGrammar.setText(grammarUrl);
            itemAppGrammarTargetLang.setText(grammarTargetLang);
        }

        // Server URL assigned to the app (comes from the DB)
        TextView itemAppServer = view.findViewById(R.id.itemAppServer);
        long serverId = c.getLong(c.getColumnIndex(App.Columns.SERVER));

        String serverUrl = Utils.idToValue(context, Server.Columns.CONTENT_URI, Server.Columns._ID, Server.Columns.URL, serverId);

        if (serverUrl == null) {
            itemAppServer.setVisibility(View.GONE);
        } else {
            itemAppServer.setVisibility(View.VISIBLE);
            if (serverUrl.equals(mDefaultServerUrl)) {
                itemAppServer.setTextColor(context.getResources().getColor(R.color.green3));
            } else {
                itemAppServer.setTextColor(context.getResources().getColor(R.color.orange));
            }
            itemAppServer.setText(serverUrl);
        }
    }


    @Override
    public View newView(Context context, Cursor c, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.list_item_app, parent, false);
    }
}