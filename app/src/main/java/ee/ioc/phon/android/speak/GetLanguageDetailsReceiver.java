/*
 * Copyright 2012-2015, Institute of Cybernetics at Tallinn University of Technology
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;

import java.util.ArrayList;

/**
 * <p>Provides a list of supported languages. This is asked e.g. by the Google
 * Translate app which wants to determine when to display a microphone
 * button next to the text input box.</p>
 * <p/>
 * <p>TODO: we just return et_EE and a few other languages,
 * but actually we should query which languages are supported by the
 * currently selected recognizer server, and return these.</p>
 *
 * @author Kaarel Kaljurand
 */
public class GetLanguageDetailsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("received: " + intent.getAction());

        Bundle resultExtras = getResultExtras(true);

        // TODO: not sure how we are supposed to behave in this case, where
        // another recognizer has already responded to the broadcast and filled
        // in its values.
        if (!resultExtras.isEmpty()) {
            Log.i("Overwriting extras: " + resultExtras);
        }

        // TODO: send different results depending on the service (Ws and Http)
        // in general support different languages). Not sure that the framework supports this.

        ArrayList<String> langs = new ArrayList<>();
        langs.add("et-EE");
        langs.add("en-US");

        Bundle extras = new Bundle();
        extras.putString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "et-EE");
        extras.putStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, langs);
        setResultExtras(extras);
    }
}