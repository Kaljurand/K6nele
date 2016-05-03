package ee.ioc.phon.android.speak.demo;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import ee.ioc.phon.android.speak.R;

public class FormDemoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webview = new WebView(this);
        setContentView(webview);
        webview.loadUrl(getString(R.string.fileFormDemo));
    }

}