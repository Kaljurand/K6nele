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

package ee.ioc.phon.android.speak.provider;

import java.util.HashMap;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

// TODO: rename to SpeakContentProvider (or ContentProvider)
public class AppsContentProvider extends ContentProvider {

	public static final String APPS_TABLE_NAME = "apps";
	public static final String GRAMMARS_TABLE_NAME = "grammars";
	public static final String SERVERS_TABLE_NAME = "servers";

	public static final String AUTHORITY = "ee.ioc.phon.android.speak.provider.AppsContentProvider";

	private static final String TAG = "AppsContentProvider";

	private static final String DATABASE_NAME = "speak.db";

	private static final int DATABASE_VERSION = 3;

	private static final UriMatcher sUriMatcher;

	private static final int APPS = 1;
	private static final int APP_ID = 2;
	private static final int GRAMMARS = 3;
	private static final int GRAMMAR_ID = 4;
	private static final int SERVERS = 5;
	private static final int SERVER_ID = 6;

	private static HashMap<String, String> appsProjectionMap;
	private static HashMap<String, String> grammarsProjectionMap;
	private static HashMap<String, String> serversProjectionMap;

	private DatabaseHelper dbHelper;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		private final Context mContext;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}

		/**
		 * GRAMMAR and SERVER should be a foreign keys
		 * Grammar should have language ID
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + APPS_TABLE_NAME + " ("
					+ App.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ App.Columns.FNAME + " TEXT NOT NULL,"
					+ App.Columns.GRAMMAR + " INTEGER,"
					+ App.Columns.SERVER + " INTEGER,"
					+ App.Columns.COUNT + " INTEGER,"
					+ "UNIQUE(" + App.Columns.FNAME + ") ON CONFLICT REPLACE"
					+ ");");

			db.execSQL("CREATE TABLE " + GRAMMARS_TABLE_NAME + " ("
					+ Grammar.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ Grammar.Columns.NAME + " VARCHAR(255),"
					+ Grammar.Columns.DESC + " TEXT,"
					+ Grammar.Columns.LANG + " VARCHAR(255),"
					+ Grammar.Columns.URL + " TEXT NOT NULL,"
					+ "UNIQUE(" + Grammar.Columns.URL + "," + Grammar.Columns.LANG + ") ON CONFLICT REPLACE"
					+ ");");

			db.execSQL("CREATE TABLE " + SERVERS_TABLE_NAME + " ("
					+ Server.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ Server.Columns.URL + " TEXT NOT NULL,"
					+ "UNIQUE(" + Server.Columns.URL + ") ON CONFLICT REPLACE"
					+ ");");

			// TODO: check that the default service name does not contain any apostrophes
			db.execSQL("INSERT INTO " + SERVERS_TABLE_NAME + " VALUES (" +
					"'1', " +
					"'" +
					mContext.getString(R.string.defaultHttpServer) +
					"'" + ");");

			// Predefined grammar URLs
			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'1', " +
					"'" + mContext.getString(R.string.entryGrammarName1) + "', " +
					"'" + mContext.getString(R.string.entryGrammarDesc1) + "', " +
					"'', " +
					"''" +
					");");

			// TODO: externalize all these strings
			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'2', " +
					"'Go', " +
					"'Väike näitegrammatika, nt ''mine kolm meetrit edasi'', ''mine neli meetrit tagasi''', " +
					"'Eng', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Go.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'3', " +
					"'Expr (left-associative)', " +
					"'Aritmeetilised avaldised, nt ''miinus kaks pluss kolm koma neli korda viis''', " +
					"'App', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Expr.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'4', " +
					"'Unitconv', " +
					"'Mõõtude teisendamine, nt ''kaksteist ruutjalga ruutkilomeetrites''', " +
					"'App', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Unitconv.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'5', " +
					"'Action', " +
					"'Alarm + Calc + Direction', " +
					"'App', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Action.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'6', " +
					"'Direction', " +
					"'Eesti aadressid, nt ''Algus Pariisi Lõpp Sõpruse puiestee kakssada kolmteist Tallinn''', " +
					"'App', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Direction.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'7', " +
					"'Calc', " +
					"'Expr + Unitconv', " +
					"'App', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Calc.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'8', " +
					"'Symbols', " +
					"'Suvalise pikkusega numbrite ja tähtede jada', " +
					"'App', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Symbols.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'9', " +
					"'Estvrp', " +
					"'Eesti autonumbrimärgi keel, nt ''aaa bee tsee üks kaks kolm''', " +
					"'App', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Estvrp.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'10', " +
					"'Direction (Tallinn)', " +
					"'Keel ''Direction'' alamhulk, mis katab ainult Tallinna', " +
					"'App', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Tallinndirection.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'11', " +
					"'Direction (Tallinn)', " +
					"'Keel ''Direction'' alamhulk, mis katab ainult Tallinna', " +
					"'Edwin', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Tallinndirection.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'12', " +
					"'Go', " +
					"'Väike näitegrammatika, nt ''mine kolm meetrit edasi'', ''mine neli meetrit tagasi''', " +
					"NULL, " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Go.pgf'" +
					");");

			db.execSQL("INSERT INTO " + GRAMMARS_TABLE_NAME + " VALUES (" +
					"'13', " +
					"'Alarm', " +
					"'Nt ''ärata mind kell kaheksa null üks''', " +
					"'App', " +
					"'http://kaljurand.github.com/Grammars/grammars/pgf/Alarm.pgf'" +
					");");
		}


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "Upgrading database v" + oldVersion + " -> v" + newVersion + ", which will destroy all old data.");
			db.execSQL("DROP TABLE IF EXISTS " + APPS_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + GRAMMARS_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + SERVERS_TABLE_NAME);
			onCreate(db);
		}
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case APPS:
			count = db.delete(APPS_TABLE_NAME, where, whereArgs);
			break;

		case APP_ID:
			String appId = uri.getPathSegments().get(1);
			count = db.delete(
					APPS_TABLE_NAME,
					App.Columns._ID + "=" + appId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		case GRAMMARS:
			count = db.delete(GRAMMARS_TABLE_NAME, where, whereArgs);
			break;

		case GRAMMAR_ID:
			String grammarId = uri.getPathSegments().get(1);
			count = db.delete(
					GRAMMARS_TABLE_NAME,
					Grammar.Columns._ID + "=" + grammarId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		case SERVERS:
			count = db.delete(SERVERS_TABLE_NAME, where, whereArgs);
			break;

		case SERVER_ID:
			String serverId = uri.getPathSegments().get(1);
			count = db.delete(
					SERVERS_TABLE_NAME,
					Server.Columns._ID + "=" + serverId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;


		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}


	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case APPS:
			return App.Columns.CONTENT_TYPE;
		case GRAMMARS:
			return Grammar.Columns.CONTENT_TYPE;
		case SERVERS:
			return Server.Columns.CONTENT_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}


	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = 0;
		Uri returnUri = null;

		switch (sUriMatcher.match(uri)) {
		case APPS:
			rowId = db.insert(APPS_TABLE_NAME, App.Columns.FNAME, values);
			if (rowId <= 0) {
				throw new SQLException("Failed to insert row into " + uri);
			}
			returnUri = ContentUris.withAppendedId(App.Columns.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			return returnUri;

		case GRAMMARS:
			rowId = db.insert(GRAMMARS_TABLE_NAME, Grammar.Columns.DESC, values);
			if (rowId <= 0) {
				throw new SQLException("Failed to insert row into " + uri);
			}
			returnUri = ContentUris.withAppendedId(Grammar.Columns.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			return returnUri;

		case SERVERS:
			rowId = db.insert(SERVERS_TABLE_NAME, Server.Columns.URL, values);
			if (rowId <= 0) {
				throw new SQLException("Failed to insert row into " + uri);
			}
			returnUri = ContentUris.withAppendedId(Server.Columns.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			return returnUri;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}


	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case APPS:
			qb.setTables(APPS_TABLE_NAME);
			qb.setProjectionMap(appsProjectionMap);
			break;
		case GRAMMARS:
			qb.setTables(GRAMMARS_TABLE_NAME);
			qb.setProjectionMap(grammarsProjectionMap);
			break;
		case SERVERS:
			qb.setTables(SERVERS_TABLE_NAME);
			qb.setProjectionMap(serversProjectionMap);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}


	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case APPS:
			count = db.update(APPS_TABLE_NAME, values, where, whereArgs);
			break;

		case GRAMMARS:
			count = db.update(GRAMMARS_TABLE_NAME, values, where, whereArgs);
			break;

		case SERVERS:
			count = db.update(SERVERS_TABLE_NAME, values, where, whereArgs);
			break;

		case APP_ID:
			String appId = uri.getPathSegments().get(1);
			count = db.update(
					APPS_TABLE_NAME,
					values,
					App.Columns._ID + "=" + appId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		case GRAMMAR_ID:
			String grammarId = uri.getPathSegments().get(1);
			count = db.update(
					GRAMMARS_TABLE_NAME,
					values,
					Grammar.Columns._ID + "=" + grammarId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		case SERVER_ID:
			String serverId = uri.getPathSegments().get(1);
			count = db.update(
					SERVERS_TABLE_NAME,
					values,
					Server.Columns._ID + "=" + serverId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, APPS_TABLE_NAME, APPS);
		sUriMatcher.addURI(AUTHORITY, APPS_TABLE_NAME + "/#", APP_ID);
		sUriMatcher.addURI(AUTHORITY, GRAMMARS_TABLE_NAME, GRAMMARS);
		sUriMatcher.addURI(AUTHORITY, GRAMMARS_TABLE_NAME + "/#", GRAMMAR_ID);
		sUriMatcher.addURI(AUTHORITY, SERVERS_TABLE_NAME, SERVERS);
		sUriMatcher.addURI(AUTHORITY, SERVERS_TABLE_NAME + "/#", SERVER_ID);

		appsProjectionMap = new HashMap<>();
		appsProjectionMap.put(App.Columns._ID, App.Columns._ID);
		appsProjectionMap.put(App.Columns.FNAME, App.Columns.FNAME);
		appsProjectionMap.put(App.Columns.GRAMMAR, App.Columns.GRAMMAR);
		appsProjectionMap.put(App.Columns.SERVER, App.Columns.SERVER);
		appsProjectionMap.put(App.Columns.COUNT, App.Columns.COUNT);

		grammarsProjectionMap = new HashMap<>();
		grammarsProjectionMap.put(Grammar.Columns._ID, Grammar.Columns._ID);
		grammarsProjectionMap.put(Grammar.Columns.NAME, Grammar.Columns.NAME);
		grammarsProjectionMap.put(Grammar.Columns.LANG, Grammar.Columns.LANG);
		grammarsProjectionMap.put(Grammar.Columns.DESC, Grammar.Columns.DESC);
		grammarsProjectionMap.put(Grammar.Columns.URL, Grammar.Columns.URL);

		serversProjectionMap = new HashMap<>();
		serversProjectionMap.put(Server.Columns._ID, Server.Columns._ID);
		serversProjectionMap.put(Server.Columns.URL, Server.Columns.URL);

	}
}