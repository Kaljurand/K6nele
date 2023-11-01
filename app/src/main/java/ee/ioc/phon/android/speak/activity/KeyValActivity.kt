package ee.ioc.phon.android.speak.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import ee.ioc.phon.android.speak.R

class KeyValActivity : AppCompatActivity() {

    private lateinit var keyView: EditText
    private lateinit var valView: EditText

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_val)
        keyView = findViewById(R.id.combo_key)
        valView = findViewById(R.id.combo_val)

        val button = findViewById<Button>(R.id.button_save)
        button.setOnClickListener {
            val replyIntent = Intent()
            if (TextUtils.isEmpty(keyView.text)) {
                setResult(Activity.RESULT_CANCELED, replyIntent)
            } else {
                replyIntent.putExtra(EXTRA_KEY, keyView.text.toString())
                replyIntent.putExtra(EXTRA_VAL, valView.text.toString())
                setResult(Activity.RESULT_OK, replyIntent)
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_KEY = "ee.ioc.phon.android.speak.KEY"
        const val EXTRA_VAL = "ee.ioc.phon.android.speak.VAL"
    }
}