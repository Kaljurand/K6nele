package ee.ioc.phon.android.speak.activity;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class AbstractContentActivity extends AppCompatActivity {

    protected void returnIntent(Uri contentUri, long key) {
        Intent intent = new Intent();
        intent.setData(ContentUris.withAppendedId(contentUri, key));
        setResult(RESULT_OK, intent);
        finish();
    }

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
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