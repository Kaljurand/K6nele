package ee.ioc.phon.android.speak.demo

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speechutils.utils.AudioUtils

class EncoderListDemo : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_view)
        val info = ArrayList<String>()
        val mime = "audio/flac"
        info.add(mime + " encoders: " + AudioUtils.getEncoderNamesForType(mime).toString())
        info.addAll(AudioUtils.getAvailableEncoders(mime, 16000))
        val listView = findViewById<ListView>(android.R.id.list)
        listView.adapter = ArrayAdapter(this, R.layout.list_item_detail, info.toTypedArray())
    }
}