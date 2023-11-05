package ee.ioc.phon.android.speak

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.TextUtils
import ee.ioc.phon.android.speak.model.CallerInfo
import ee.ioc.phon.android.speak.utils.Utils
import ee.ioc.phon.android.speechutils.Extras
import ee.ioc.phon.android.speechutils.RecognitionServiceManager
import ee.ioc.phon.android.speechutils.utils.IntentUtils
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils
import kotlinx.coroutines.runBlocking

class ServiceLanguageChooser(
    private val mContext: Context,
    private val mPrefs: SharedPreferences,
    keys: Int,
    private val mCallerInfo: CallerInfo,
    private val mAppId: String
) {
    private var mCombosAsList: List<String>
    private var mKeyCurrentCombo = 0
    private var mIndex = 0
    lateinit var speechRecognizer: SpeechRecognizer
        private set
    lateinit var intent: Intent
        private set
    var language: String? = null
        private set
    lateinit var service: ComponentName
        private set

    init {
        // If SERVICE_COMPONENT is defined, we do not use the combos selected in the settings.
        var comboOverride: String? = null
        val extras = mCallerInfo.extras
        if (extras!!.containsKey(Extras.EXTRA_SERVICE_COMPONENT)) {
            comboOverride = extras.getString(Extras.EXTRA_SERVICE_COMPONENT)
            if (extras.containsKey(RecognizerIntent.EXTRA_LANGUAGE)) {
                comboOverride = RecognitionServiceManager.createComboString(
                    comboOverride,
                    extras.getString(RecognizerIntent.EXTRA_LANGUAGE)
                )
            }
        }
        if (comboOverride == null) {
            val res = mContext.resources
            val keysAsTypedArray = res.obtainTypedArray(keys)
            val keyCombo = keysAsTypedArray.getResourceId(0, 0)
            mKeyCurrentCombo = keysAsTypedArray.getResourceId(1, 0)
            val defaultCombos = keysAsTypedArray.getResourceId(2, 0)
            keysAsTypedArray.recycle()

            // TODO: pull the combos from Room
            val mCombos = PreferenceUtils.getPrefStringSet(
                mPrefs, res, keyCombo
            )

            val baseList = listOf(
                "ee.ioc.phon.android.speak/.service.WebSocketRecognitionService;et-EE",
                "ee.ioc.phon.android.k6neleservice/.service.WebSocketRecognitionService;et-EE",
                "com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService;en-US",
                "com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService;de-AT"
            )
            val repository = (mContext.applicationContext as K6neleApplication).repository
            lateinit var comboList: List<String>
            runBlocking {
                // TODO: using FLow here crashes
                //comboList = repository.allItems
                //    .map { "ee.ioc.phon.android.speak/.service.WebSocketRecognitionService;en-US" }
                //    .toList()

                comboList = baseList + repository.getAllItems0()
                    .take(2)
                    .map { it.componentName.flattenToShortString() + ";en-US" }
                    .toList()
            }
            mCombosAsList = if (mCombos == null || mCombos.isEmpty()) {
                // If the user has chosen an empty set of combos
                PreferenceUtils.getStringListFromStringArray(res, defaultCombos)
            } else {
                ArrayList(mCombos)
                //ArrayList(comboList)
            }
            val currentCombo =
                PreferenceUtils.getPrefMapEntry(mPrefs, res, mKeyCurrentCombo, mAppId)
            mIndex = mCombosAsList.indexOf(currentCombo)
            // If the current combo was not found among the choices then select the first combo.
            if (mIndex == -1) {
                mIndex = 0
                PreferenceUtils.putPrefMapEntry(
                    mPrefs,
                    res,
                    mKeyCurrentCombo,
                    mAppId,
                    mCombosAsList[0]
                )
            }
        } else {
            mCombosAsList = listOf(comboOverride)
            mIndex = 0
            mKeyCurrentCombo = -1
        }
        update()
    }

    fun size(): Int {
        return mCombosAsList.size
    }

    /**
     * Switch to the "next" combo and set it as default.
     * Only done if there are more than 1 combos, meaning that defining SERVICE_COMPONENT (which
     * creates a single-element combo list) does not change the default combo.
     */
    operator fun next() {
        if (size() > 1) {
            if (++mIndex >= size()) {
                mIndex = 0
            }
            PreferenceUtils.putPrefMapEntry(
                mPrefs,
                mContext.resources,
                mKeyCurrentCombo,
                mAppId,
                mCombosAsList[mIndex]
            )
        }
        update()
    }

    operator fun get(position: Int): String {
        return mCombosAsList[position]
    }

    fun isSelected(position: Int): Boolean {
        return mIndex == position
    }

    fun set(position: Int) {
        if (size() > 1) {
            mIndex = if (position >= size()) {
                0
            } else {
                position
            }
            PreferenceUtils.putPrefMapEntry(
                mPrefs,
                mContext.resources,
                mKeyCurrentCombo,
                mAppId,
                mCombosAsList[mIndex]
            )
        }
        update()
    }

    val combo: String
        get() = mCombosAsList[mIndex]

    private fun update() {
        var language: String? = null
        val splits = TextUtils.split(combo, ";")
        service = ComponentName.unflattenFromString(splits[0])!!
        if (splits.size > 1) {
            language = splits[1]
        }

        // If the stored combo name does not refer to an existing service on the device then we use
        // the default service. This can happen if services get removed or renamed.
        if (!IntentUtils.isRecognitionAvailable(mContext, service)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext)
        } else {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext, service)
        }

        // TODO: support other actions
        intent = Utils.getRecognizerIntent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH,
            mCallerInfo,
            language
        )
        this.language = language
    }
}