package ee.ioc.phon.android.speak.demo

import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ee.ioc.phon.android.speak.R

class FormDemoActivity : AppCompatActivity() {

    inner class WebAppInterface
    /**
     * Instantiate the interface and set the context
     */
    internal constructor(internal var mContext: Context) {

        /**
         * Show a toast from the web page
         */
        @JavascriptInterface
        fun showToast(toast: String) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webview = WebView(this)
        setContentView(webview)
        val webSettings = webview.settings
        webSettings.javaScriptEnabled = true
        webview.addJavascriptInterface(WebAppInterface(this), "Android")
        webview.loadUrl(getString(R.string.fileFormDemo))
    }

}