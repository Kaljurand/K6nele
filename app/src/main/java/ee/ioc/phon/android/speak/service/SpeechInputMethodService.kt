package ee.ioc.phon.android.speak.service

import android.annotation.TargetApi
import android.content.*
import android.content.res.Resources
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.speech.SpeechRecognizer
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.Toast
import ee.ioc.phon.android.speak.AppDatabase
import ee.ioc.phon.android.speak.Log
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.activity.PermissionsRequesterActivity
import ee.ioc.phon.android.speak.model.CallerInfo
import ee.ioc.phon.android.speak.model.RewriteRuleRepository
import ee.ioc.phon.android.speak.model.Rewrites
import ee.ioc.phon.android.speak.utils.Utils
import ee.ioc.phon.android.speak.view.AbstractSpeechInputViewListener
import ee.ioc.phon.android.speak.view.SpeechInputView
import ee.ioc.phon.android.speak.view.SpeechInputView.SpeechInputViewListener
import ee.ioc.phon.android.speechutils.Extras
import ee.ioc.phon.android.speechutils.editor.*
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*
import java.util.regex.Matcher

class SpeechInputMethodService : InputMethodService() {
    private var mFlagPersonalizedLearning = true
    private var mInputMethodManager: InputMethodManager? = null
    private var mInputView: SpeechInputView? = null
    private var mCommandEditor: CommandEditor? = null
    private var mShowPartialResults = false
    private var mPrefs: SharedPreferences? = null
    private var mRes: Resources? = null
    private lateinit var mRuleManager: RuleManager
    private lateinit var repository: RewriteRuleRepository

    override fun onCreate() {
        super.onCreate()
        Log.i("onCreate")

        mInputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        mCommandEditor = InputConnectionCommandEditor(applicationContext)
        mPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        mRes = resources
    }

    // TODO: use it for #r, just with a different command
    suspend fun clipToCommand(text: String, ruleManager: RuleManager, repository: RewriteRuleRepository) {
        val cal = Calendar.getInstance()
        val timeInMillis = cal.timeInMillis
        val comment = RuleManager.DATE_FORMAT.format(cal.time)
        val textEscaped = Matcher.quoteReplacement(text)
        val command = Command(text, comment, ruleManager.localePattern, ruleManager.servicePattern, ruleManager.appPattern,
                RuleManager.makeUtt(cal), "", CommandEditorManager.REPLACE_SEL, arrayOf(textEscaped))
        // TODO: use:
        //  val time = ZonedDateTime.now().toEpochSecond()
        repository.addNewRule(REWRITES_NAME_CLIP, (timeInMillis / 1000).toInt(), command)
    }

    /**
     * This is called at configuration change. We just kill the running session.
     * TODO: better handle configuration changes
     */
    override fun onInitializeInterface() {
        Log.i("onInitializeInterface")
        closeSession()
    }

    override fun onCreateInputView(): View {
        Log.i("onCreateInputView")
        //ViewGroup view = (ViewGroup) findViewById(android.R.id.content);
        val view = myWindow!!.decorView.rootView as ViewGroup
        mInputView = layoutInflater.inflate(R.layout.voice_ime_view, view, false) as SpeechInputView

        val applicationScope = CoroutineScope(SupervisorJob())
        val database by lazy { AppDatabase.getDatabase(mInputView!!.context, applicationScope) }
        repository = RewriteRuleRepository(database.rewriteRuleDao())

        mRuleManager = RuleManager()
        val rewritesClip = Rewrites(mPrefs, mRes, REWRITES_NAME_CLIP)

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val lambda = ClipboardManager.OnPrimaryClipChangedListener {
            val clipData = clipboard.primaryClip
            if (clipData != null) {
                val clip = clipData.getItemAt(0).text.toString()
                // Empty strings make less sense as clips
                if (!clip.isEmpty()) {

                    GlobalScope.launch {
                        // TODO: put clip into DB + update commandEditor rewriters
                        clipToCommand(clip, mRuleManager, repository)
                    }

                    val ur = mRuleManager.addRecent(clip, rewritesClip.rewrites)
                    PreferenceUtils.putPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, REWRITES_NAME_CLIP, ur?.toTsv())
                    mCommandEditor?.setRewriters(
                            Utils.makeList(
                                    Utils.genRewriters(mPrefs, mRes, null, mRuleManager.commandMatcher)))
                }
            }
        }

        if (rewritesClip.isSelected) {
            // TODO: remove the listener onFinish
            clipboard.removePrimaryClipChangedListener(lambda)
            clipboard.addPrimaryClipChangedListener(lambda)
        }

        return mInputView!!
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        // Returning true only if Wear
        return resources.getBoolean(R.bool.isWatch)
    }

    /**
     * We check the type of editor control and if we probably cannot handle it (e.g. dates)
     * or do not want to (e.g. passwords) then we hand the editing over to another keyboard.
     * TODO: handle inputType = 0
     */
    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        var type = "UNKNOWN"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mFlagPersonalizedLearning = attribute.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        }
        when (attribute.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER -> type = "NUMBER"
            InputType.TYPE_CLASS_DATETIME -> {
                type = "DATETIME"
                switchToLastIme()
            }
            InputType.TYPE_CLASS_PHONE -> type = "PHONE"
            InputType.TYPE_CLASS_TEXT -> {
                val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
                type = "TEXT/"
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // We refuse to recognize passwords for privacy reasons.
                    type += "PASSWORD || VISIBLE_PASSWORD"
                    switchToLastIme()
                } else if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                    type += "EMAIL_ADDRESS"
                } else if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
                    // URI bar of Chrome and Firefox, can also handle search queries, thus supported
                    type += "URI"
                } else if (variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // List filtering? Used in the Dialer search bar, thus supported
                    type += "FILTER"
                } else {
                    type += variation
                }

                // This is used in the standard search bar (e.g. in Google Play).
                if (attribute.inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0) {
                    type += "FLAG_AUTO_COMPLETE"
                }
            }
            else -> {
            }
        }
        Log.i("onStartInput: " + type + ", " + attribute.inputType + ", " + attribute.imeOptions + ", " + restarting + ", learning: " + mFlagPersonalizedLearning)
    }

    /**
     * Note that when editing a HTML page, then switching between form fields might fail to call
     * this method with restarting=false, we thus always update the editor info (incl. inputType).
     */
    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        Log.i("onStartInputView: " + editorInfo.inputType + "/" + editorInfo.imeOptions + "/" + restarting)
        val ic = currentInputConnection
        // InputConnectionCommandEditor cannot be called with a null InputConnection.
        // We do not expect this to happen, but Google Play crash reports show that it does.
        // TODO: review, e.g. move to after restarting-check
        if (ic == null) {
            Toast.makeText(applicationContext, R.string.errorFailedGetCurrentInputConnection, Toast.LENGTH_LONG).show()
            //switchToLastIme();
            return
        }
        (mCommandEditor as InputConnectionCommandEditor?)!!.setInputConnection(ic)

        // TODO: quick hack to add app to the matcher, not sure if we can access the class name of the app
        // TODO: use getPackageName() or editorInfo.packageName, but not both
        val packageName = editorInfo.packageName
        val app = ComponentName(packageName, packageName)
        mInputView!!.init(
                R.array.keysIme,
                CallerInfo(makeExtras(), editorInfo, getPackageName()),
                PreferenceUtils.getPrefInt(mPrefs, mRes, R.string.keyImeMode, R.string.defaultImeMode),
                app
        )

        // TODO: update this less often (in onStart)
        closeSession()
        if (restarting) {
            return
        }
        mInputView!!.setListener(getSpeechInputViewListener(myWindow, app, mRuleManager), editorInfo)
        mShowPartialResults = PreferenceUtils.getPrefBoolean(mPrefs, mRes, R.string.keyImeShowPartialResults, R.bool.defaultImeShowPartialResults)

        // Launch recognition immediately (if set so)
        if (PreferenceUtils.getPrefBoolean(mPrefs, mRes, R.string.keyImeAutoStart, R.bool.defaultImeAutoStart)) {
            Log.i("Auto-starting")
            mInputView!!.start()
        }
    }

    /**
     * Called when the input view is being hidden from the user.
     * This will be called either prior to hiding the window,
     * or prior to switching to another target for editing.
     *
     * @param finishingInput If true, onFinishInput() will be called immediately after.
     */
    override fun onFinishInputView(finishingInput: Boolean) {
        // TODO: maybe do not call super
        super.onFinishInputView(finishingInput)
        Log.i("onFinishInputView: $finishingInput")
        if (!finishingInput) {
            closeSession()
        }
    }

    /**
     * Called to inform the input method that text input has finished in the last editor.
     * At this point there may be a call to onStartInput(EditorInfo, boolean) to perform input in a new editor,
     * or the input method may be left idle.
     * This method is not called when input restarts in the same editor.
     */
    override fun onFinishInput() {
        // TODO: maybe do not call super
        super.onFinishInput()
        Log.i("onFinishInput")
        closeSession()
    }

    public override fun onCurrentInputMethodSubtypeChanged(subtype: InputMethodSubtype) {
        Log.i("onCurrentInputMethodSubtypeChanged: " + subtype + ": " + subtype.extraValue)
        closeSession()
    }

    private fun closeSession() {
        mInputView?.cancel()
        val window = myWindow
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private val token: IBinder?
        private get() {
            val window = myWindow ?: return null
            return window.attributes.token
        }
    private val myWindow: Window?
        private get() {
            val dialog = window ?: return null
            return dialog.window
        }

    /**
     * Switch to another IME by selecting it from the list of all active IMEs (isAskUser==true), or
     * by taking the next IME in the IME rotation (isAskUser==false on JELLY_BEAN).
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun switchIme(isAskUser: Boolean) {
        closeSession()
        if (isAskUser) {
            mInputMethodManager!!.showInputMethodPicker()
        } else {
            val token = token
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mInputMethodManager!!.switchToNextInputMethod(token, false /* not onlyCurrentIme */)
                } else {
                    mInputMethodManager!!.switchToLastInputMethod(token)
                }
            } catch (e: NoSuchMethodError) {
                Log.e("IME switch failed", e)
            }
        }
    }

    /**
     * Switch to the previous IME, either when the user tries to edit an unsupported field (e.g. password),
     * or when they explicitly want to be taken back to the previous IME e.g. in case of a one-shot
     * speech input.
     */
    private fun switchToLastIme() {
        closeSession()
        mInputMethodManager!!.switchToLastInputMethod(token)
    }

    private fun getSpeechInputViewListener(window: Window?, app: ComponentName, ruleManager: RuleManager): SpeechInputViewListener {
        return object : AbstractSpeechInputViewListener() {
            val mApp = app
            val mRuleManager = ruleManager
            private fun runOp(op: Op) {
                mCommandEditor!!.runOp(op, false)
            }

            private fun setKeepScreenOn(b: Boolean) {
                if (window != null) {
                    if (b) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }

            private fun commitResults(results: List<String>) {
                val text = getText(results)
                val editorResult = mCommandEditor!!.commitFinalResult(text)
                if (editorResult != null && mInputView != null && editorResult.isCommand) {
                    mInputView!!.showMessage(editorResult.rewrite.ppCommand(), editorResult.isSuccess)
                }
                if (editorResult != null && mFlagPersonalizedLearning) {
                    var isSelected = false
                    val rewritesRec = Rewrites(mPrefs, mRes, REWRITES_NAME_RECENT)
                    if (rewritesRec.isSelected) {
                        isSelected = true
                        val ur = mRuleManager.addRecent(editorResult, rewritesRec.rewrites)
                        PreferenceUtils.putPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, REWRITES_NAME_RECENT, ur.toTsv())

                        // TODO: add to DB
                        val cal = Calendar.getInstance()
                        val comment = RuleManager.DATE_FORMAT.format(cal.time)
                        val command = mRuleManager.makeCommand(editorResult.rewrite, RuleManager.makeUtt(cal), comment)
                        // TODO: use:
                        //  val time = ZonedDateTime.now().toEpochSecond()
                        GlobalScope.launch {
                            repository.addNewRule(REWRITES_NAME_RECENT, (cal.timeInMillis / 1000).toInt(), command)
                        }
                    }
                    val rewritesFreq = Rewrites(mPrefs, mRes, REWRITES_NAME_FREQUENT)
                    if (rewritesFreq.isSelected) {
                        isSelected = true
                        val ur = mRuleManager.addFrequent(editorResult, rewritesFreq.rewrites)
                        PreferenceUtils.putPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, REWRITES_NAME_FREQUENT, ur.toTsv())
                    }
                    // Update rewriters because the tables have changed
                    if (isSelected) {
                        mCommandEditor!!.rewriters = Utils.makeList(
                                Utils.genRewriters(mPrefs, mRes, null, mRuleManager.commandMatcher))
                    }
                }
            }

            override fun onComboChange(language: String, service: ComponentName) {
                mRuleManager.setMatchers(language, service, mApp)
                // TODO: name of the rewrites table configurable
                mCommandEditor!!.rewriters = Utils.makeList(
                        Utils.genRewriters(mPrefs, mRes, null, mRuleManager.commandMatcher))
            }

            override fun onPartialResult(results: List<String>, isSemiFinal: Boolean) {
                if (isSemiFinal) {
                    commitResults(results)
                } else {
                    if (mShowPartialResults) {
                        mCommandEditor!!.commitPartialResult(getText(results))
                    }
                }
            }

            override fun onFinalResult(results: List<String>, bundle: Bundle) {
                commitResults(results)
                setKeepScreenOn(false)
            }

            override fun onCommand(text: String) {
                val op = mCommandEditor!!.getOpOrNull(text, false)
                if (op == null) {
                    // Mic button swipe did not match any rule, so we show the swipe crossed out.
                    // TODO: maybe just clear the status bar
                    if (mInputView != null) {
                        mInputView!!.showMessage(text, false)
                    }
                } else {
                    val success = mCommandEditor!!.runOp(op)
                    if (mInputView != null) {
                        // TODO: show executed command or replacement text, not op.toString()
                        mInputView!!.showMessage(op.toString(), success)
                    }
                    setKeepScreenOn(false)
                }
            }

            override fun onSwitchIme(isAskUser: Boolean) {
                switchIme(isAskUser)
            }

            override fun onSwitchToLastIme() {
                switchToLastIme()
            }

            override fun onAction(editorAction: Int, hide: Boolean) {
                if (hide) {
                    closeSession()
                }
                runOp(mCommandEditor!!.imeAction(editorAction))
                if (hide) {
                    requestHideSelf(0)
                }
            }

            override fun onDeleteLeftChar() {
                // TODO: indicate somehow (e.g. vibration, different background color) that the Op failed
                runOp(mCommandEditor!!.deleteChars(-1))
                // TODO: might be better, i.e. able to delete non-text (checkboxes), but not undoable
                //runOp(mCommandEditor.keyCode(KeyEvent.KEYCODE_DEL));
            }

            override fun onDeleteLastWord() {
                runOp(mCommandEditor!!.deleteLeftWord())
            }

            override fun onAddNewline() {
                runOp(mCommandEditor!!.replaceSel("\n"))
            }

            override fun goUp() {
                runOp(mCommandEditor!!.keyUp())
            }

            override fun goDown() {
                runOp(mCommandEditor!!.keyDown())
            }

            override fun moveRel(numOfChars: Int) {
                runOp(mCommandEditor!!.moveRel(numOfChars))
            }

            override fun moveRelSel(numOfChars: Int, type: Int) {
                runOp(mCommandEditor!!.moveRelSel(numOfChars, type))
            }

            override fun onExtendSel(regex: String) {
                runOp(mCommandEditor!!.selectRe(regex, false))
            }

            override fun onAddSpace() {
                runOp(mCommandEditor!!.replaceSel(" "))
            }

            override fun onSelectAll() {
                // TODO: show ContextMenu
                runOp(mCommandEditor!!.selectAll())
            }

            override fun onReset() {
                // TODO: hide ContextMenu (if visible)
                runOp(mCommandEditor!!.moveRel(0))
            }

            override fun onStartListening() {
                Log.i("IME: onStartListening")
                mCommandEditor!!.reset()
                setKeepScreenOn(true)
            }

            override fun onStopListening() {
                Log.i("IME: onStopListening")
                setKeepScreenOn(false)
            }

            // TODO: add onCancel()
            override fun onError(errorCode: Int) {
                setKeepScreenOn(false)
                Log.i("IME: onError: $errorCode")
                if (errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    val intent = Intent(this@SpeechInputMethodService, PermissionsRequesterActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    this@SpeechInputMethodService.startActivity(intent)
                }
            }
        }
    }

    companion object {
        // TODO: move somewhere else and make end-user configurable
        private const val REWRITES_NAME_RECENT = "#r"
        private const val REWRITES_NAME_FREQUENT = "#f"
        private const val REWRITES_NAME_CLIP = "#c"
        private fun getText(results: List<String>): String {
            return if (results.size > 0) {
                results[0]
            } else ""
        }

        private fun makeExtras(): Bundle {
            val extras = Bundle()
            extras.putBoolean(Extras.EXTRA_UNLIMITED_DURATION, true)
            extras.putBoolean(Extras.EXTRA_DICTATION_MODE, true)
            return extras
        }
    }
}