package ee.ioc.phon.android.speak.demo

import android.app.ListActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speechutils.utils.AudioUtils
import java.util.*

class EncoderListDemo : ListActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val info = ArrayList<String>()
        info.add("FLAC encoders: " + AudioUtils.getEncoderNamesForType("audio/flac").toString())
        info.addAll(AudioUtils.getAvailableEncoders(16000))
        listAdapter = ArrayAdapter(this, R.layout.list_item_detail, info.toTypedArray())
    }
}