package ee.ioc.phon.android.speak.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.utils.Utils;

public class PermissionsRequesterActivity extends Activity {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(PermissionsRequesterActivity.this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted: just finish
                    finish();
                } else {
                    // Permission not granted: explain the consequences and offer a link to the app settings
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    Utils.getLaunchIntentDialog(this, getString(R.string.promptPermissionRationale), intent).show();
                }
                break;
            }
            default: {
                break;
            }
        }
    }
}
