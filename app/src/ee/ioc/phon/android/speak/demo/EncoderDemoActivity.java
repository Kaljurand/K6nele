package ee.ioc.phon.android.speak.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.activity.DetailsActivity;
import ee.ioc.phon.android.speak.provider.FileContentProvider;
import ee.ioc.phon.android.speechutils.AudioRecorder;
import ee.ioc.phon.android.speechutils.EncodedAudioRecorder;
import ee.ioc.phon.android.speechutils.utils.AudioUtils;

public class EncoderDemoActivity extends Activity {

    private static final String VOICE_IME_SUBTYPE_MODE = "voice";
    private static final String VOICE_IME_PACKAGE_PREFIX = "";

    private AudioRecorder mRecorder;
    private Handler mStopHandler = new Handler();
    private Runnable mStopTask;

    private byte[] mRecording;

    private Button mBTest1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encoder_demo);
        mBTest1 = (Button) findViewById(R.id.buttonTest1);
        Button bTest2 = (Button) findViewById(R.id.buttonTest2);
        mBTest1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBTest1.setText(R.string.buttonImeStopByPause);
                try {
                    recordUntilPause(new EncodedAudioRecorder(16000));
                } catch (IOException e) {
                    toast(e.getMessage());
                }
            }
        });
        bTest2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> info = new ArrayList<>();
                info.add("FLAC encoders: " + AudioUtils.getEncoderNamesForType("audio/flac").toString());
                info.addAll(AudioUtils.getAvailableEncoders(16000));
                Intent details = new Intent(getApplicationContext(), DetailsActivity.class);
                details.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, info.toArray(new String[info.size()]));
                startActivity(details);
            }
        });

        Button bTest3 = (Button) findViewById(R.id.buttonTest3);
        bTest3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> info = getVoiceImeInputMethodInfo(getPackageManager(),
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE));
                Intent details = new Intent(getApplicationContext(), DetailsActivity.class);
                details.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, info.toArray(new String[info.size()]));
                startActivity(details);
            }
        });
    }

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    private void recordUntilPause(AudioRecorder audioRecorder) throws IOException {
        mRecorder = audioRecorder;
        if (mRecorder.getState() == AudioRecorder.State.ERROR) {
            throw new IOException("ERROR");
        }

        if (mRecorder.getState() != AudioRecorder.State.READY) {
            throw new IOException("not READY");
        }

        mRecorder.start();

        if (mRecorder.getState() != AudioRecorder.State.RECORDING) {
            throw new IOException("not RECORDING");
        }

        // Check if we should stop recording
        mStopTask = new Runnable() {
            public void run() {
                if (mRecorder != null) {
                    if (mRecorder.isPausing()) {
                        onEndOfSpeech();
                    } else {
                        mStopHandler.postDelayed(this, 1000);
                    }
                }
            }
        };

        mStopHandler.postDelayed(mStopTask, 500);
    }

    protected void onEndOfSpeech() {
        if (mRecorder != null) {
            mRecording = mRecorder.consumeRecording();
        }
        stopRecording0();
    }

    private void stopRecording0() {
        releaseRecorder();
        if (mStopHandler != null) mStopHandler.removeCallbacks(mStopTask);
        byte[] recordingAsWav = AudioUtils.getRecordingAsWav(mRecording, 16000, (short) 2, (short) 1);
        mBTest1.setText(R.string.buttonImeSpeak);

        try {
            Uri uriWav = getAudioUri("audio.wav", recordingAsWav);
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.setDataAndType(uriWav, null);
            startActivity(intent);
        } catch (IOException e) {
            Log.e(e.getMessage(), e);
        }
    }

    private void releaseRecorder() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    private Uri getAudioUri(String filename, byte[] recording) throws IOException {
        FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(recording);
        fos.close();
        return Uri.parse("content://" + FileContentProvider.AUTHORITY + "/" + filename);
    }

    private static List<String> getVoiceImeInputMethodInfo(PackageManager pm, InputMethodManager inputMethodManager)
            throws SecurityException, IllegalArgumentException {
        List<String> imeInfos = new ArrayList<>();
        for (InputMethodInfo inputMethodInfo : inputMethodManager.getEnabledInputMethodList()) {
            for (int i = 0; i < inputMethodInfo.getSubtypeCount(); i++) {
                InputMethodSubtype subtype = inputMethodInfo.getSubtypeAt(i);
                if (VOICE_IME_SUBTYPE_MODE.equals(subtype.getMode()) &&
                        inputMethodInfo.getComponent().getPackageName().startsWith(VOICE_IME_PACKAGE_PREFIX)) {
                    CharSequence label = inputMethodInfo.loadLabel(pm);
                    imeInfos.add(label + "@" + inputMethodInfo.getComponent());
                }
            }
        }
        return imeInfos;
    }
}