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

class ComboSelectorView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    private lateinit var mSlc: ServiceLanguageChooser
    private lateinit var mListener: ComboSelectorListener
    private val mMinButtons: Int

    interface ComboSelectorListener {
        fun onComboChange(language: String, service: ComponentName)
    }

    init {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.ComboSelectorView,
                0, 0).apply {

            try {
                mMinButtons = getInteger(R.styleable.ComboSelectorView_minButtons, 3)
            } finally {
                recycle()
            }
        }
    }

    fun init(context: Context, prefs: SharedPreferences?, keys: Int, callerInfo: CallerInfo?, appId: String?, key: Int, listener: ComboSelectorListener) {
        // TODO: check for null? (test by deinstalling a recognizer but not changing K6nele settings)
        mSlc = ServiceLanguageChooser(context, prefs, keys, callerInfo, appId)
        mListener = listener
        val mBComboSelector = findViewById<Button>(R.id.tvComboSelector)
        val mRvComboButtons: RecyclerView = findViewById(R.id.rvComboButtons)
        val size = mSlc.size()
        if (size >= mMinButtons && mMinButtons > 0) {
            // We show buttons if the user has requested at least one button (by default at least 3),
            // and there are at least that many to show.
            mBComboSelector.visibility = GONE
            mRvComboButtons.visibility = VISIBLE
            visibility = VISIBLE
            //mRvComboButtons.setHasFixedSize(true)
            mRvComboButtons.layoutManager = GridLayoutManager(context, size + 1)
            val adapter = ComboButtonsAdapter(object : ComboButtonsAdapterListener {
                override fun onComboChange(language: String, service: ComponentName) {
                    mListener.onComboChange(language, service)
                }

                override fun onMore() {
                    comboSelector(context, key)
                }
            }, mSlc)
            mRvComboButtons.adapter = adapter
        } else if (mMinButtons < 0 || size > 1) {
            // We show the single multitap button if the user has requested a negative number of buttons,
            // or if there are at least 2 buttons, but the user does not want to see them as individual buttons
            // (the preceding check failed).
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
            // We hide the combo switching possibility if there is a single button and the user
            // has requested to see buttons only if there are at least 2.
            visibility = GONE
        }
    }

    fun click() {
        mListener.onComboChange(mSlc.language, mSlc.service);
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