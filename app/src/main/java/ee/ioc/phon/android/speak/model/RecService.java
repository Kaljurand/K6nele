package ee.ioc.phon.android.speak.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speechutils.RecognitionServiceManager;

public class RecService {

    private final String mLabel;
    private final String mDesc;
    private String mSettingsActivity;
    private final ComponentName mComponentName;

    public RecService(Context context, String id) {
        // Can return <null, "">
        Pair<ComponentName, String> pair = RecognitionServiceManager.unflattenFromString(id);
        mComponentName = pair.first;
        mLabel = RecognitionServiceManager.getServiceLabel(context, mComponentName);
        ServiceInfo si = RecognitionServiceManager.getServiceInfo(context, mComponentName);
        int resId = si.descriptionRes;
        if (resId == 0) {
            mDesc = "";
        } else {
            mDesc = context.getString(si.descriptionRes);
        }
        try {
            mSettingsActivity = RecognitionServiceManager.getSettingsActivity(context, si);
            Log.i(mSettingsActivity);
        } catch (XmlPullParserException e) {
            mSettingsActivity = null;
        } catch (IOException e) {
            mSettingsActivity = null;
        }
    }

    public String getService() {
        return mLabel;
    }

    public String getDesc() {
        return mDesc;
    }

    public Drawable getIcon(Context context) {
        return RecognitionServiceManager.getServiceIcon(context, mComponentName);
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public Intent getSettingsIntent() {
        if (mSettingsActivity == null) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName(mComponentName.getPackageName(), mSettingsActivity);
        return intent;
    }
}