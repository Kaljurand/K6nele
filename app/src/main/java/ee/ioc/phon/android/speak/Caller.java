/*
 * Copyright 2011-2013, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.speak;

import android.app.PendingIntent;
import android.os.Bundle;
import android.speech.RecognizerIntent;

import ee.ioc.phon.android.speechutils.utils.BundleUtils;

/**
 * <p>Description of the caller that receives the transcription.
 * If the extras specify a pending intent (I've never encountered such an app though),
 * then the pending intent's target package's name is returned.</p>
 *
 * <p>Otherwise we use EXTRA_CALLING_PACKAGE because there does not seem to be a way to
 * find out which Activity called us, i.e. this does not work:</p>
 *
 * <pre>
 * ComponentName callingActivity = getCallingActivity();
 * if (callingActivity != null) {
 *     return callingActivity.getPackageName();
 * }
 * </pre>
 *
 * <p>The above tries to detect the "primary caller" (e.g. a keyboard app). We also
 * look for the "secondary caller" (e.g. an app in which the keyboard is used).
 * by parsing the extras looking for another package name, e.g. included in the
 * <code>android.speech.extras.RECOGNITION_CONTEXT</code> extra which some keyboard
 * apps set.</p>
 *
 * <p>The caller description can be obtained in two ways. First a string in the form
 * "1st-caller/2nd-caller" which can be used as a User-Agent string. Examples:</p>
 *
 * <ul>
 * <li>VoiceIME/com.google.android.apps.plus (standard keyboard in Google Plus app)</li>
 * <li>SwypeIME/com.timsu.astrid</li>
 * <li>mobi.mgeek.TunnyBrowser/null</li>
 * <li>null/null (if no caller-identifying info was found in the extras)</li>
 * </ul>
 *
 * <p>Secondly, we try to determine which caller string is more informative so
 * that it can be used in the Apps-database, for counting, and server/grammar assignment.
 * There should be no difference in terms of grammar assigning if speech recognition
 * in the app is used via the keyboard or via a dedicated speech input button.</p>
 */
public class Caller {

    private static final String KEY_PACKAGE_NAME = "packageName";

    private final String mPrimaryCaller;
    private final String mSecondaryCaller;

    public Caller(PendingIntent pendingIntent, Bundle bundle) {
        if (pendingIntent == null) {
            mPrimaryCaller = bundle.getString(RecognizerIntent.EXTRA_CALLING_PACKAGE);
        } else {
            mPrimaryCaller = pendingIntent.getTargetPackage();
        }
        mSecondaryCaller = getPackageName(bundle);
    }


    public String getActualCaller() {
        if (mSecondaryCaller == null) {
            if (mPrimaryCaller == null) {
                return "null";
            }
            return mPrimaryCaller;
        }
        return mSecondaryCaller;
    }


    public String toString() {
        return mPrimaryCaller + "/" + mSecondaryCaller;
    }


    /**
     * <p>Traverses the given bundle (which can contain other bundles)
     * looking for the key "packageName".
     * Returns its corresponding value if finds it.</p>
     *
     * @param bundle bundle (e.g. intent extras)
     * @return package name possibly hidden deep into the given bundle
     */
    private static String getPackageName(Bundle bundle) {
        Object obj = BundleUtils.getBundleValue(bundle, KEY_PACKAGE_NAME);
        if (obj instanceof String) {
            return (String) obj;
        }
        return null;
    }

}