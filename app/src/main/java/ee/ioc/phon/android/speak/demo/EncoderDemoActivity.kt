package ee.ioc.phon.android.speak.demo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import ee.ioc.phon.android.speak.Log
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.activity.DetailsActivity
import ee.ioc.phon.android.speak.provider.FileContentProvider
import ee.ioc.phon.android.speechutils.AudioRecorder
import ee.ioc.phon.android.speechutils.EncodedAudioRecorder
import ee.ioc.phon.android.speechutils.utils.AudioUtils
import java.io.IOException

class EncoderDemoActivity : Activity() {

    private var mRecorder: AudioRecorder? = null
    private val mStopHandler = Handler()
    private var mStopTask: Runnable? = null

    private var mRecording: ByteArray? = null

    private var mBTest1: Button? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.encoder_demo)

        val editText = findViewById<EditText>(R.id.etTest0)
        editText.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                toast(v.text.toString())
                return@OnEditorActionListener true
            }
            false
        })


        mBTest1 = findViewById(R.id.buttonTest1) as Button
        (findViewById(R.id.buttonTest1) as Button).setOnClickListener {
            mBTest1?.setText(R.string.buttonImeStopByPause)
            try {
                recordUntilPause(EncodedAudioRecorder(16000))
            } catch (e: IOException) {
                toast(e.message)
            }
        }
    }

    protected fun toast(message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }


    @Throws(IOException::class)
    private fun recordUntilPause(audioRecorder: AudioRecorder) {
        mRecorder = audioRecorder
        if (mRecorder?.state == AudioRecorder.State.ERROR) {
            throw IOException("ERROR")
        }

        if (mRecorder?.state != AudioRecorder.State.READY) {
            throw IOException("not READY")
        }

        mRecorder?.start()

        if (mRecorder?.state != AudioRecorder.State.RECORDING) {
            throw IOException("not RECORDING")
        }

        // Check if we should stop recording
        mStopTask = object : Runnable {
            override fun run() {
                if (mRecorder != null) {
                    if (mRecorder!!.isPausing) {
                        onEndOfSpeech()
                    } else {
                        mStopHandler.postDelayed(this, 1000)
                    }
                }
            }
        }

        mStopHandler.postDelayed(mStopTask, 500)
    }

    protected fun onEndOfSpeech() {
        if (mRecorder != null) {
            mRecording = mRecorder!!.consumeRecording()
        }
        stopRecording0()
    }

    private fun stopRecording0() {
        releaseRecorder()
        mStopHandler.removeCallbacks(mStopTask)
        val recordingAsWav = AudioUtils.getRecordingAsWav(mRecording, 16000, 2.toShort(), 1.toShort())
        mBTest1?.setText(R.string.buttonImeSpeak)

        try {
            val uriWav = getAudioUri("audio.wav", recordingAsWav)
            val intent = Intent(this, DetailsActivity::class.java)
            intent.setDataAndType(uriWav, null)
            startActivity(intent)
        } catch (e: IOException) {
            Log.e(e.message, e)
        }

    }

    private fun releaseRecorder() {
        if (mRecorder != null) {
            mRecorder!!.release()
            mRecorder = null
        }
    }

    @Throws(IOException::class)
    private fun getAudioUri(filename: String, recording: ByteArray): Uri {
        val fos = openFileOutput(filename, Context.MODE_PRIVATE)
        fos.write(recording)
        fos.close()
        return Uri.parse("content://" + FileContentProvider.AUTHORITY + "/" + filename)
    }
}