package ee.ioc.phon.android.speak.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.speech.SpeechRecognizer
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.ServiceLanguageChooser
import ee.ioc.phon.android.speak.activity.ComboSelectorActivity
import ee.ioc.phon.android.speak.adapter.ComboButtonsAdapter
import ee.ioc.phon.android.speak.adapter.ComboButtonsAdapter.ComboButtonsAdapterListener
import ee.ioc.phon.android.speak.model.CallerInfo
import ee.ioc.phon.android.speak.model.Combo

class ComboSelectorView : LinearLayoutCompat {
    private val DEFAULT_MIN_BUTTONS: Int = 3
    private lateinit var mSlc: ServiceLanguageChooser
    private lateinit var mListener: ComboSelectorListener
    private val mMinButtons: Int;

    interface ComboSelectorListener {
        fun onComboChange(language: String?, service: ComponentName?)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        // TODO: use defStyle
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.ComboSelectorView,
                0, 0).apply {

            try {
                mMinButtons = getInteger(R.styleable.ComboSelectorView_minButtons, DEFAULT_MIN_BUTTONS)
            } finally {
                recycle()
            }
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.ComboSelectorView,
                0, 0).apply {

            try {
                mMinButtons = getInteger(R.styleable.ComboSelectorView_minButtons, DEFAULT_MIN_BUTTONS)
            } finally {
                recycle()
            }
        }
    }

    constructor(context: Context) : super(context) {
        mMinButtons = DEFAULT_MIN_BUTTONS
    }

    fun init(context: Context, prefs: SharedPreferences?, keys: Int, callerInfo: CallerInfo?, appId: String?, key: Int, listener: ComboSelectorListener) {
        // TODO: check for null? (test by deinstalling a recognizer but not changing K6nele settings)
        mSlc = ServiceLanguageChooser(context, prefs, keys, callerInfo, appId)
        mListener = listener
        val mBComboSelector = findViewById<Button>(R.id.tvComboSelector)
        val mRvComboButtons: RecyclerView = findViewById(R.id.rvComboButtons)
        val size = mSlc.size()
        if (size >= mMinButtons) {
            mBComboSelector.visibility = GONE
            mRvComboButtons.visibility = VISIBLE
            visibility = VISIBLE
            mRvComboButtons.setHasFixedSize(true)
            mRvComboButtons.layoutManager = GridLayoutManager(context, size + 1)
            val adapter = ComboButtonsAdapter(object : ComboButtonsAdapterListener {
                override fun onComboChange(language: String, service: ComponentName) {
                    mListener.onComboChange(language, service)
                }

                override fun onOpenConf() {
                    comboSelector(context, key)
                }
            }, mSlc)
            mRvComboButtons.adapter = adapter
        } else if (size > 1) {
            mRvComboButtons.visibility = GONE
            mBComboSelector.visibility = VISIBLE
            visibility = VISIBLE
            mBComboSelector.setOnClickListener { v: View? ->
                mSlc.next()
                mListener.onComboChange(mSlc.language, mSlc.service)
                val combo = Combo(context, mSlc.combo)
                mBComboSelector.text = combo.longLabel
            }
            mBComboSelector.setOnLongClickListener { view: View? ->
                comboSelector(context, key)
                true
            }
            val combo = Combo(context, mSlc.combo)
            mBComboSelector.text = combo.longLabel
        } else {
            visibility = GONE
        }
    }

    val speechRecognizer: SpeechRecognizer
        get() = mSlc.speechRecognizer
    val intent: Intent
        get() = mSlc.intent

    private fun comboSelector(context: Context, key: Int) {
        val intent = Intent(context, ComboSelectorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("key", context.getString(key))
        context.startActivity(intent)
    }
}