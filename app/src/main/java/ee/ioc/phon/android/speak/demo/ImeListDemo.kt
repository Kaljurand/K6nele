package ee.ioc.phon.android.speak.demo

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ee.ioc.phon.android.speak.R

class ImeListDemo : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_view)

        val mngr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val info = getVoiceImeInputMethodInfo(packageManager, mngr)

        val listView = findViewById<ListView>(android.R.id.list)
        listView.adapter = ArrayAdapter(this, R.layout.list_item_detail, info.toTypedArray())

        listView.onItemClickListener = OnItemClickListener { _, view, _, _ ->
            val content = (view as TextView).text.toString()
            val settingsActivity = content.split('\n')[2]
            startActivity(Intent(this, Class.forName(settingsActivity)))
        }
    }

    companion object {
        private const val VOICE_IME_SUBTYPE_MODE = "voice"

        @Throws(SecurityException::class, IllegalArgumentException::class)
        private fun getVoiceImeInputMethodInfo(
            pm: PackageManager,
            inputMethodManager: InputMethodManager
        ): List<String> {
            val imeInfos = ArrayList<String>()
            for (inputMethodInfo in inputMethodManager.enabledInputMethodList) {
                for (i in 0 until inputMethodInfo.subtypeCount) {
                    val subtype = inputMethodInfo.getSubtypeAt(i)
                    if (VOICE_IME_SUBTYPE_MODE == subtype.mode) {
                        val label = inputMethodInfo.loadLabel(pm)
                        imeInfos.add(label.toString() + '\n' + inputMethodInfo.component + '\n' + inputMethodInfo.settingsActivity)
                    }
                }
            }
            return imeInfos
        }
    }
}