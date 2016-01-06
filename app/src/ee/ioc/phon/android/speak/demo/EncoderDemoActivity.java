package ee.ioc.phon.android.speak.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.DetailsActivity;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speechutils.EncodedAudioRecorder;

public class EncoderDemoActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encoder_demo);
        ImageButton bRecord = (ImageButton) findViewById(R.id.buttonMicrophone);
        Button bTest1 = (Button) findViewById(R.id.buttonTest1);
        Button bTest2 = (Button) findViewById(R.id.buttonTest2);
        bRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast("Not implemented");
                // TODO: record for 3 seconds and play back the encoded result
                // report size reduction
            }
        });
        bTest1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EncodedAudioRecorder audioRecorder = new EncodedAudioRecorder();
                //audioRecorder.testFlacEncoder();
                audioRecorder.testAMRWBEncoder();
            }
        });
        bTest2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EncodedAudioRecorder audioRecorder = new EncodedAudioRecorder();
                List<String> info = new ArrayList<>();
                info.addAll(audioRecorder.getAvailableEncoders());
                Intent details = new Intent(getApplicationContext(), DetailsActivity.class);
                details.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, info.toArray(new String[info.size()]));
                startActivity(details);
            }
        });
    }

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}