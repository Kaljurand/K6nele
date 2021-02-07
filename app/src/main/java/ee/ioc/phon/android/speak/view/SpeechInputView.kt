package ee.ioc.phon.android.speak.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Paint
import android.os.Bundle
import android.preference.PreferenceManager
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.text.TextUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import ee.ioc.phon.android.speak.*
import ee.ioc.phon.android.speak.activity.ComboSelectorActivity
import ee.ioc.phon.android.speak.activity.RewritesActivity2
import ee.ioc.phon.android.speak.activity.RewritesSelectorActivity2
import ee.ioc.phon.android.speak.adapter.ButtonsAdapter
import ee.ioc.phon.android.speak.model.CallerInfo
import ee.ioc.phon.android.speak.model.Combo
import ee.ioc.phon.android.speak.model.RewriteRule
import ee.ioc.phon.android.speak.model.RewriteRuleRepository
import ee.ioc.phon.android.speechutils.Extras
import ee.ioc.phon.android.speechutils.editor.CommandMatcher
import ee.ioc.phon.android.speechutils.editor.CommandMatcherFactory
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils
import ee.ioc.phon.android.speechutils.view.MicButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*

class SpeechInputView(context: Context, attrs: AttributeSet?) : LinearLayoutCompat(context, attrs) {
    private var mCentralButtons: View? = null
    private var mBImeStartStop: MicButton? = null
    private var mBImeKeyboard: ImageButton? = null
    private var mBImeAction: ImageButton? = null
    private var mBUiMode: ImageButton? = null
    private var mBComboSelector: Button? = null
    private var mTvInstruction: TextView? = null
    private var mTvMessage: TextView? = null
    private var mRvClipboard: RecyclerView? = null
    private var mRlClipboard: RelativeLayout? = null
    private var mLlEmpty: LinearLayout? = null
    private var mApp: ComponentName? = null
    private var mAppId = ""
    private var mListener: SpeechInputViewListener? = null
    private var mRecognizer: SpeechRecognizer? = null
    private var mSlc: ServiceLanguageChooser? = null
    private var mOstl: OnSwipeTouchListener? = null
    private var mOctl: OnCursorTouchListener? = null
    private var mState: MicButton.State? = null
    private var mUiState: String? = null

    // Y (yellow i.e. not-transcribing)
    // R (red, i.e. transcribing)
    private var mBtnType = "Y"

    // TODO: make it an attribute
    private var mSwipeType = 0

    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { AppDatabase.getDatabase(context, applicationScope) }
    val repository by lazy { RewriteRuleRepository(database.rewriteRuleDao()) }

    interface SpeechInputViewListener {
        fun onComboChange(language: String, service: ComponentName)
        fun onPartialResult(text: List<String>, isSemiFinal: Boolean)
        fun onFinalResult(text: List<String>, bundle: Bundle)
        fun onCommand(text: String)

        /**
         * Switch to the next IME or ask the user to choose the IME.
         *
         * @param isAskUser Iff true then ask the user to choose the IME
         */
        fun onSwitchIme(isAskUser: Boolean)

        /**
         * Switch to the previous IME (the IME that launched this IME).
         */
        fun onSwitchToLastIme()

        /**
         * Perform an editor action (GO, NEXT, ...).
         *
         * @param actionId Action ID
         * @param hide     hide the IME after performing the action, iff true
         */
        fun onAction(actionId: Int, hide: Boolean)
        fun onDeleteLeftChar()
        fun onDeleteLastWord()
        fun goUp()
        fun goDown()
        fun moveRel(numOfChars: Int)
        fun moveRelSel(numOfChars: Int, type: Int)
        fun onExtendSel(regex: String)
        fun onAddNewline()
        fun onAddSpace()
        fun onSelectAll()
        fun onReset()
        fun onBufferReceived(buffer: ByteArray)
        fun onError(errorCode: Int)
        fun onStartListening()
        fun onStopListening()
    }

    fun setListener(listener: SpeechInputViewListener?, editorInfo: EditorInfo?) {
        mListener = listener
        mRvClipboard?.adapter = ButtonsAdapter(mListener!!)
        if (mBImeAction != null && editorInfo != null) {
            // TODO: test
            val overrideEnter = editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0
            var useEnter = !overrideEnter
            val imeAction = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            if (overrideEnter) {
                var hide = true
                if (imeAction == EditorInfo.IME_ACTION_GO) {
                    mBImeAction!!.setImageResource(R.drawable.ic_go)
                } else if (imeAction == EditorInfo.IME_ACTION_SEARCH) {
                    mBImeAction!!.setImageResource(R.drawable.ic_search)
                } else if (imeAction == EditorInfo.IME_ACTION_SEND) {
                    mBImeAction!!.setImageResource(R.drawable.ic_send)
                } else if (imeAction == EditorInfo.IME_ACTION_DONE) {
                    mBImeAction!!.setImageResource(R.drawable.ic_done)
                    hide = false
                } else if (imeAction == EditorInfo.IME_ACTION_NEXT) {
                    mBImeAction!!.setImageResource(R.drawable.ic_next)
                    hide = false
                } else {
                    useEnter = true
                }
                val finalHide = hide
                // The content description is based on the text field's action label,
                // which might not always be present, or be the best description of the content.
                // TODO: fall back to a description like "go", "send" if action label is missing.
                mBImeAction!!.contentDescription = editorInfo.actionLabel
                mBImeAction!!.setOnClickListener { v: View? ->
                    if (finalHide) {
                        cancelOrDestroy()
                    }
                    mListener!!.onAction(imeAction, finalHide)
                }
            }

            // If no action was defined, then we show the Enter icon,
            // even if we were allowed to override Enter.
            if (useEnter) {
                mBImeAction!!.setImageResource(R.drawable.ic_newline)
                mBImeAction!!.setOnClickListener { v: View? -> mListener!!.onAddNewline() }
            }

            // if mBImeKeyboard is available then we are in the IME mode where changing
            // the UI mode is possible.
            if (mBImeKeyboard != null) {
                val context = context
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val res = resources
                mUiState = PreferenceUtils.getPrefMapEntry(prefs, res, R.string.mapAppToMode, mAppId)
                mBUiMode!!.setImageResource(R.drawable.ic_baseline_swap_vert_24)
                showUi(mUiState)
                mBUiMode!!.setOnClickListener { v: View? ->
                    mUiState = if (mUiState == null) {
                        "1"
                    } else if ("1" == mUiState) {
                        "2"
                    } else {
                        null
                    }
                    PreferenceUtils.putPrefMapEntry(prefs, res, R.string.mapAppToMode, mAppId, mUiState)
                    showUi(mUiState)
                }
                // TODO: experimental: long press controls mic
                mBUiMode!!.setOnLongClickListener { v: View? ->
                    changeState()
                    true
                }
                mBImeKeyboard!!.setImageResource(R.drawable.ic_ime)
                mBImeKeyboard!!.setOnClickListener { v: View? -> mListener!!.onSwitchToLastIme() }
                mBImeKeyboard!!.setOnLongClickListener { v: View? ->
                    mListener!!.onSwitchIme(false)
                    true
                }
            }
        }
        val buttonDelete = findViewById<ImageButton>(R.id.bImeDelete)
        if (buttonDelete != null) {
            buttonDelete.setImageResource(R.drawable.ic_backspace)
            buttonDelete.setOnTouchListener(object : OnPressAndHoldListener() {
                public override fun onAction() {
                    mListener!!.onDeleteLeftChar()
                }
            })
        }
        mOstl = object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                mListener!!.onDeleteLastWord()
            }

            override fun onSwipeRight() {
                mListener!!.onAddNewline()
            }

            override fun onSwipeUp() {
                mListener!!.goUp()
            }

            override fun onSwipeDown() {
                mListener!!.goDown()
            }

            override fun onSingleTapMotion() {
                mListener!!.onReset()
            }

            override fun onDoubleTapMotion() {
                mListener!!.onAddSpace()
            }

            override fun onLongPressMotion() {
                mListener!!.onSelectAll()
            }
        }

        // TODO: move to utilities (48dp for the edges)
        val displayMetrics = resources.displayMetrics
        val edge = Math.round(48 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
        mOctl = object : OnCursorTouchListener(edge) {
            override fun onMove(numOfChars: Int) {
                mListener!!.moveRel(numOfChars)
                showMessageArrow(numOfChars, DASH_CUR)
            }

            override fun onMoveSel(numOfChars: Int, type: Int) {
                mListener!!.moveRelSel(numOfChars, type)
                showMessageArrow(numOfChars, DASH_SEL)
            }

            override fun onLongPress() {
                // Selects current word.
                // The selection can be later changed, e.g. include punctuation.
                mListener!!.onExtendSel("\\w+")
                setBackgroundResource(R.drawable.rectangle_gradient_light)
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }

            override fun onSingleTapMotion() {
                mListener!!.onReset()
            }

            override fun onDoubleTapMotion() {
                mListener!!.onAddSpace()
            }

            override fun onDown() {
                mBImeKeyboard!!.visibility = INVISIBLE
                mBImeAction!!.visibility = INVISIBLE
                setVisibility(mBUiMode, INVISIBLE)
                if (mRlClipboard!!.visibility == GONE) {
                    setVisibility(mCentralButtons, INVISIBLE)
                } else {
                    setVisibility(mRlClipboard, INVISIBLE)
                }
                setVisibility(mBComboSelector, INVISIBLE)
                showMessage("")
            }

            override fun onUp() {
                showMessage("")
                mBImeKeyboard!!.visibility = VISIBLE
                mBImeAction!!.visibility = VISIBLE
                setVisibility(mBUiMode, VISIBLE)
                if (mRlClipboard!!.visibility == GONE) {
                    setVisibility(mCentralButtons, VISIBLE)
                } else {
                    setVisibility(mRlClipboard, VISIBLE)
                }
                setVisibility(mBComboSelector, VISIBLE)
                setBackgroundResource(R.drawable.rectangle_gradient)
            }

            override fun onSwipeUp() {
                mListener!!.onAction(EditorInfo.IME_ACTION_PREVIOUS, false)
            }

            override fun onSwipeDown() {
                mListener!!.onAction(EditorInfo.IME_ACTION_NEXT, false)
            }
        }
        setGuiInitState(0)
        makeComboChange()
    }

    fun init(keys: Int, callerInfo: CallerInfo?, swipeType: Int, app: ComponentName?) {
        mSwipeType = swipeType
        // These controls are optional (i.e. can be null),
        // except for mBImeStartStop (TODO: which should also be optional)
        mCentralButtons = findViewById(R.id.centralButtons)
        mBImeStartStop = findViewById(R.id.bImeStartStop)
        mBImeKeyboard = findViewById(R.id.bImeKeyboard)
        mBImeAction = findViewById(R.id.bImeAction)
        mBUiMode = findViewById(R.id.bClipboard)
        mBComboSelector = findViewById(R.id.tvComboSelector)
        mTvInstruction = findViewById(R.id.tvInstruction)
        mTvMessage = findViewById(R.id.tvMessage)
        mRvClipboard = findViewById(R.id.rvClipboard)
        mRlClipboard = findViewById(R.id.rlClipboard)
        mLlEmpty = findViewById(R.id.empty)
        val context = context
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val res = resources
        mApp = app
        mAppId = if (mApp == null) "" else mApp!!.flattenToShortString()
        if (mRvClipboard != null) {
            mRvClipboard?.setHasFixedSize(true)
            // TODO: make span count configurable
            mRvClipboard?.layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.spanCount))
        }
        if (mSwipeType == 2) {
            // Turning from GONE to VISIBLE
            findViewById<View>(R.id.rlKeyButtons).visibility = VISIBLE
        }

        // TODO: check for null? (test by deinstalling a recognizer but not changing K6nele settings)
        mSlc = ServiceLanguageChooser(context, prefs, keys, callerInfo, mAppId)
        if (mBComboSelector != null) {
            if (mSlc!!.size() > 1) {
                mBComboSelector!!.visibility = VISIBLE
            } else {
                mBComboSelector!!.visibility = GONE
                mBComboSelector = null
            }
        }
        updateServiceLanguage(mSlc!!.speechRecognizer)
        if (mBComboSelector != null) {
            updateComboSelector(mSlc)
        }
        showMessage("")
        val keysAsTypedArray = res.obtainTypedArray(keys)
        val key = keysAsTypedArray.getResourceId(0, 0)
        val keyHelpText = keysAsTypedArray.getResourceId(7, 0)
        val defaultHelpText = keysAsTypedArray.getResourceId(8, 0)
        keysAsTypedArray.recycle()
        if (mTvInstruction != null) {
            if (PreferenceUtils.getPrefBoolean(prefs, res, keyHelpText, defaultHelpText)) {
                mTvInstruction!!.visibility = VISIBLE
            } else {
                mTvInstruction!!.visibility = GONE
            }
        }
        mBImeStartStop?.setOnTouchListener(object : OnSwipeTouchListener(getContext(), mBImeStartStop) {
            override fun onSwipeLeft() {
                mListener!!.onCommand("K6_" + mBtnType + "_BTN_MIC_LEFT")
            }

            override fun onSwipeRight() {
                mListener!!.onCommand("K6_" + mBtnType + "_BTN_MIC_RIGHT")
            }

            override fun onSwipeUp() {
                mListener!!.onCommand("K6_" + mBtnType + "_BTN_MIC_UP")
            }

            override fun onSwipeDown() {
                mListener!!.onCommand("K6_" + mBtnType + "_BTN_MIC_DOWN")
            }

            override fun onSingleTapMotion() {
                changeState()
            }

            override fun onDoubleTapMotion() {
                mListener!!.onCommand("K6_" + mBtnType + "_BTN_MIC_DOUBLETAP")
            }

            override fun onLongPressMotion() {
                mListener!!.onCommand("K6_" + mBtnType + "_BTN_MIC_LONGPRESS")
            }
        })
        if (mBComboSelector != null) {
            mBComboSelector!!.setOnClickListener { v: View? -> nextCombo() }
            mBComboSelector!!.setOnLongClickListener { view: View? ->
                comboSelector(key)
                true
            }
        }
    }

    /**
     * Performs an action after a press on the mic button, and given a current state.
     */
    private fun changeState() {
        Log.i("Microphone button pressed: state = $mState")
        when (mState) {
            MicButton.State.INIT, MicButton.State.ERROR -> startListening(mSlc)
            MicButton.State.RECORDING -> stopListening()
            MicButton.State.LISTENING, MicButton.State.TRANSCRIBING -> {
                cancelOrDestroy()
                setGuiInitState(0)
            }
            else -> {
            }
        }
    }

    fun start() {
        if (mState == MicButton.State.INIT || mState == MicButton.State.ERROR) {
            // TODO: fix this
            startListening(mSlc)
        }
    }

    // TODO: make public?
    private fun stopListening() {
        if (mRecognizer != null) {
            mRecognizer!!.stopListening()
        }
        mListener!!.onStopListening()
    }

    fun cancel() {
        cancelOrDestroy()
        setGuiInitState(0)
    }

    fun showMessage(message: CharSequence?) {
        if (mTvMessage != null) {
            if (message == null || message.length == 0) {
                setText(mTvMessage, "")
            } else {
                mTvMessage!!.ellipsize = TextUtils.TruncateAt.START
                mTvMessage!!.paintFlags = mTvMessage!!.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv() and Paint.UNDERLINE_TEXT_FLAG.inv()
                setText(mTvMessage, message)
            }
        }
    }

    fun showMessage(message: CharSequence?, isSuccess: Boolean) {
        if (mTvMessage != null) {
            if (message == null || message.length == 0) {
                setText(mTvMessage, "")
            } else {
                mTvMessage!!.ellipsize = TextUtils.TruncateAt.MIDDLE
                if (isSuccess) {
                    mTvMessage!!.paintFlags = mTvMessage!!.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv() or Paint.UNDERLINE_TEXT_FLAG
                } else {
                    mTvMessage!!.paintFlags = mTvMessage!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv() or Paint.STRIKE_THRU_TEXT_FLAG
                }
                setText(mTvMessage, message)
            }
        }
    }

    private fun updateTouchListener(type: Int) {
        if (type == 1) {
            setOnTouchListener(mOstl)
        } else if (type == 2) {
            setOnTouchListener(mOctl)
        } else {
            setOnTouchListener(null)
        }
    }

    private fun makeComboChange() {
        val language = mSlc!!.language
        val service = mSlc!!.service
        mListener!!.onComboChange(language, service)
        if (mRvClipboard != null) {
            updateClipboard(context, language, service, mApp)
        }
    }

    // TODO: filter using commandMatcher
    private suspend fun updateClipboardAdapter(tabName: String, commandMatcher: CommandMatcher): List<RewriteRule> {
        // TODO: convert to a list of RewriteRule, with commandMatcher-based filtering
        val rules: List<RewriteRule> = repository.rulesByOwnerNameSus(tabName)
        //Log.i("Rules: count: ${rules.count()}")
        //Log.i("Rules: ${rules}")
        return rules
    }

    /**
     * TODO: hide tabs without rewrites, or at least block the long press on an empty rewrites tab
     */
    private fun updateClipboard(context: Context, language: String, service: ComponentName, app: ComponentName?) {
        val tabs: TabLayout = findViewById(R.id.tlClipboardTabs)
        tabs.clearOnTabSelectedListeners()
        tabs.removeAllTabs()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val res = resources
        val defaults = PreferenceUtils.getPrefStringSet(prefs, res, R.string.defaultRewriteTables)
        if (defaults.isEmpty()) {
            mLlEmpty!!.visibility = VISIBLE
            mRvClipboard!!.visibility = GONE
            tabs.visibility = GONE
            findViewById<View>(R.id.buttonOpenRewrites).setOnClickListener { v: View? ->
                val intent = Intent(getContext(), RewritesSelectorActivity2::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return
        } else {
            mLlEmpty!!.visibility = GONE
            mRvClipboard!!.visibility = VISIBLE
            tabs.visibility = VISIBLE
        }
        val commandMatcher = CommandMatcherFactory.createCommandFilter(language, service, app)
        // TODO: used to be toArray, but Kotlin didn't like it
        //val names: Array<String> = defaults.toArray<String>(EMPTY_STRING_ARRAY)
        val names = defaults.toTypedArray()
        // TODO: defaults should be a list (not a set that needs to be sorted)
        Arrays.sort(names)

        val appId = app!!.flattenToShortString()
        val selectedTabName = getTabName(prefs, res, appId)
        tabs.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val name = tab.text.toString()
                if (NEW_TAB_LABEL == tab.tag) {
                    val intent = Intent(getContext(), RewritesSelectorActivity2::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    Log.i("SpeechInputView: launch: " + name)
                    GlobalScope.launch { // launch a new coroutine in background and continue
                        // No adapter attached; skipping layout
                        Log.i("SpeechInputView: GlobalScope: " + name)
                        val rules = updateClipboardAdapter(name, commandMatcher)
                        Log.i("SpeechInputView: GlobalScope: " + rules)
                        (mRvClipboard?.adapter as ButtonsAdapter).submitList(rules)
                        val count = mRvClipboard!!.adapter?.itemCount
                        Log.i("SpeechInputView: GlobalScope: count: " + count)
                    }
                    setTabName(prefs, res, appId, name)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        for (tabName in names) {
            val tab = tabs.newTab()
            tab.text = tabName
            tabs.addTab(tab, tabName == selectedTabName)
        }
        val tab = tabs.newTab()
        tab.text = NEW_TAB_LABEL
        tab.tag = NEW_TAB_LABEL
        tabs.addTab(tab, false)

        // If the previously selected rewrites table is not among the defaults anymore then
        // we select the first one (but do not save it).
        if (tabs.selectedTabPosition == -1) {
            tabs.getTabAt(0)!!.select()
        }
        val tabStrip = tabs.getChildAt(0) as LinearLayout
        // We exclude the NEW_TAB_LABEL
        for (i in 0 until tabStrip.childCount - 1) {
            val name = tabs.getTabAt(i)!!.text.toString()
            // Long click loads the rewrites view (without populating the tab)
            tabStrip.getChildAt(i).setOnLongClickListener { v: View? ->
                val intent = Intent(getContext(), RewritesActivity2::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(RewritesActivity2.EXTRA_NAME, name)
                intent.putExtra(RewritesActivity2.EXTRA_LOCALE, language)
                intent.putExtra(RewritesActivity2.EXTRA_APP, appId)
                intent.putExtra(RewritesActivity2.EXTRA_SERVICE, service.flattenToShortString())
                context.startActivity(intent)
                false
            }
        }
    }

    private fun getTabName(prefs: SharedPreferences, res: Resources, appId: String): String {
        return PreferenceUtils.getPrefMapEntry(prefs, res, R.string.mapClipboardTabName, appId)
    }

    private fun setTabName(prefs: SharedPreferences, res: Resources, appId: String, name: String) {
        PreferenceUtils.putPrefMapEntry(prefs, res, R.string.mapClipboardTabName, appId, name)
    }

    private fun nextCombo() {
        if (mState == MicButton.State.RECORDING) {
            stopListening()
        }
        mSlc!!.next()
        makeComboChange()
        updateComboSelector(mSlc)
    }

    private fun comboSelector(key: Int) {
        cancelOrDestroy()
        val context = context
        val intent = Intent(context, ComboSelectorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("key", context.getString(key))
        context.startActivity(intent)
    }

    private fun showUi(state: String?) {
        if (state == null) {
            mRlClipboard!!.visibility = GONE
            mCentralButtons!!.visibility = VISIBLE
        } else if ("1" == state) {
            mCentralButtons!!.visibility = GONE
            mRlClipboard!!.visibility = VISIBLE
        } else {
            mCentralButtons!!.visibility = GONE
            mRlClipboard!!.visibility = GONE
        }
    }

    /*
    private void loadDrawable(ImageView view, int res) {
        view.setBackground(AppCompatResources.getDrawable(getContext(), res));
    }
    */
    private fun showMessageArrow(numOfChars: Int, dash: String) {
        if (numOfChars < 0) {
            val num = -1 * numOfChars
            if (DASH_LENGTH > num) {
                showMessage("◄" + dash.substring(0, num))
            }
        } else if (DASH_LENGTH > numOfChars) {
            showMessage(dash.substring(0, numOfChars) + "►")
        }
    }

    private fun setGuiState(state: MicButton.State) {
        mState = state
        if (mBImeStartStop != null) {
            mBImeStartStop!!.post { mBImeStartStop!!.setState(mState) }
        }
    }

    private fun setGuiInitState(message: Int) {
        if (message == 0) {
            // Do not clear a possible error message
            //showMessage("");
            setGuiState(MicButton.State.INIT)
            setVisibility(findViewById(R.id.rlKeyButtons), VISIBLE)
        } else {
            setGuiState(MicButton.State.ERROR)
            showMessage(String.format(resources.getString(R.string.labelSpeechInputViewMessage), resources.getString(message)))
        }
        updateTouchListener(mSwipeType)
        mBUiMode?.setColorFilter(null)
        setText(mTvInstruction, R.string.buttonImeSpeak)
    }

    private fun updateComboSelector(slc: ServiceLanguageChooser?) {
        val combo = Combo(context, slc!!.combo)
        mBComboSelector!!.text = combo.longLabel
    }

    private fun updateServiceLanguage(sr: SpeechRecognizer) {
        cancelOrDestroy()
        mRecognizer = sr
        mRecognizer!!.setRecognitionListener(SpeechInputRecognitionListener())
    }

    private fun startListening(slc: ServiceLanguageChooser?) {
        setGuiState(MicButton.State.WAITING)
        updateServiceLanguage(slc!!.speechRecognizer)
        try {
            mRecognizer!!.startListening(slc.intent)
            mListener!!.onStartListening()
            // Increases the counter of the app that calls the recognition service.
            // TODO: we could define it slightly differently, e.g. only count successful recognitions,
            // count also commands executed via swipes and/or buttons (but maybe not count every deletion
            // and cursor movement).
            // TODO: we could also count languages, services, etc.
            PackageNameRegistry.increaseAppCount(context, slc.intent.extras, null)
        } catch (e: SecurityException) {
            // TODO: review this.
            // On Android 11 we get the SecurityException when calling "Kõnele service" and Kõnele does not
            // have the mic permission. This does not happen when calling Google's service. Instead
            // there is a callback to RecognitionListener.onError. We act here as if this callback happened.
            mListener!!.onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        }
    }

    /**
     * TODO: not sure if it is better to call cancel or destroy
     * Note that SpeechRecognizer#destroy calls cancel first.
     */
    private fun cancelOrDestroy() {
        mBtnType = "Y"
        if (mRecognizer != null) {
            mRecognizer!!.destroy()
            mRecognizer = null
        }
    }

    private inner class SpeechInputRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle) {
            Log.i("onReadyForSpeech: state = $mState")
            setGuiState(MicButton.State.LISTENING)
            if (mBUiMode != null) {
                mBUiMode!!.setColorFilter(MicButton.COLOR_LISTENING)
            }
            mBtnType = "R"
            setText(mTvInstruction, R.string.buttonImeStop)
            showMessage("")
        }

        override fun onBeginningOfSpeech() {
            Log.i("onBeginningOfSpeech: state = $mState")
            setGuiState(MicButton.State.RECORDING)
            mBtnType = "R"
            setVisibility(findViewById(R.id.rlKeyButtons), INVISIBLE)
        }

        override fun onEndOfSpeech() {
            Log.i("onEndOfSpeech: state = $mState")
            // We go into the TRANSCRIBING-state only if we were in the RECORDING-state,
            // otherwise we ignore this event. This improves compatibility with
            // Google Voice Search, which calls EndOfSpeech after onResults.
            if (mState == MicButton.State.RECORDING) {
                setGuiState(MicButton.State.TRANSCRIBING)
                if (mBUiMode != null) {
                    mBUiMode!!.setColorFilter(MicButton.COLOR_TRANSCRIBING)
                }
                setText(mTvInstruction, R.string.statusImeTranscribing)
            }
        }

        /**
         * We process all possible SpeechRecognizer errors. Most of them
         * are generated by our implementation, others can be generated by the
         * framework, e.g. ERROR_CLIENT results from
         * "stopListening called with no preceding startListening".
         *
         * @param errorCode SpeechRecognizer error code
         */
        override fun onError(errorCode: Int) {
            Log.i("onError: $errorCode")
            mListener!!.onError(errorCode)
            when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> setGuiInitState(R.string.errorImeResultAudioError)
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> setGuiInitState(R.string.errorImeResultRecognizerBusy)
                SpeechRecognizer.ERROR_SERVER -> setGuiInitState(R.string.errorImeResultServerError)
                SpeechRecognizer.ERROR_NETWORK -> setGuiInitState(R.string.errorImeResultNetworkError)
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> setGuiInitState(R.string.errorImeResultNetworkTimeoutError)
                SpeechRecognizer.ERROR_CLIENT -> setGuiInitState(R.string.errorImeResultClientError)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> setGuiInitState(R.string.errorImeResultInsufficientPermissions)
                SpeechRecognizer.ERROR_NO_MATCH -> setGuiInitState(R.string.errorImeResultNoMatch)
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> setGuiInitState(R.string.errorImeResultSpeechTimeout)
                else -> {
                    Log.e("This might happen in future Android versions: code $errorCode")
                    setGuiInitState(R.string.errorImeResultClientError)
                }
            }
            mBtnType = "Y"
        }

        override fun onPartialResults(bundle: Bundle) {
            Log.i("onPartialResults: state = $mState")
            val results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (results != null && !results.isEmpty()) {
                // This can be true only with kaldi-gstreamer-server
                // ... and with Tilde's version of kaldi-gstreamer-server
                val isSemiFinal = (bundle.getBoolean(Extras.EXTRA_SEMI_FINAL)
                        || bundle.getBoolean("com.tilde.tildesbalss.extra.SEMI_FINAL"))
                showMessage(lastChars(results, isSemiFinal))
                mListener!!.onPartialResult(results, isSemiFinal)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle) {
            // TODO: future work: not sure how this can be generated by the service
            Log.i("onEvent: type = $eventType")
        }

        override fun onResults(bundle: Bundle) {
            Log.i("onResults: state = $mState")
            val results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.i("onResults: results = $results")
            if (results == null || results.isEmpty()) {
                // If we got empty results then assume that the session ended,
                // e.g. cancel was called.
                // TODO: not sure why this was needed
                //mListener.onFinalResult(Collections.<String>emptyList(), bundle);
            } else {
                showMessage(lastChars(results, true))
                mListener!!.onFinalResult(results, bundle)
            }
            setGuiInitState(0)
            mBtnType = "Y"
        }

        override fun onRmsChanged(rmsdB: Float) {
            //Log.i("onRmsChanged");
            setMicButtonVolumeLevel(mBImeStartStop, rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray) {
            Log.i("View: onBufferReceived: " + buffer.size)
            mListener!!.onBufferReceived(buffer)
        }
    }

    companion object {
        private val EMPTY_STRING_ARRAY = arrayOf<String>()
        private const val DASH_CUR = "――――――――――――――――――――"
        private const val DASH_SEL = "■■■■■■■■■■■■■■■■■■■■"
        private const val DASH_LENGTH = DASH_CUR.length
        private const val NEW_TAB_LABEL = "+"
        private fun selectFirstResult(results: List<String>?): String? {
            return if (results == null || results.size < 1) {
                null
            } else results[0]
        }

        private fun lastChars(results: List<String>, isFinal: Boolean): String {
            return lastChars(selectFirstResult(results), isFinal)
        }

        private fun lastChars(str: String?, isFinal: Boolean): String {
            var str = str
            str = str?.replace("\\n".toRegex(), "↲") ?: ""
            return if (isFinal) {
                "$str▪"
            } else str
        }

        private fun setText(textView: TextView?, text: CharSequence) {
            if (textView != null && textView.visibility != GONE) {
                textView.post(Runnable { textView.text = text })
            }
        }

        private fun setText(textView: TextView?, text: Int) {
            if (textView != null && textView.visibility != GONE) {
                textView.post(Runnable { textView.setText(text) })
            }
        }

        private fun setMicButtonVolumeLevel(button: MicButton?, rmsdB: Float) {
            button?.post { button.setVolumeLevel(rmsdB) }
        }

        private fun setVisibility(view: View?, visibility: Int) {
            if (view != null && view.visibility != GONE) {
                view.post(Runnable { view.visibility = visibility })
            }
        }
    }
}