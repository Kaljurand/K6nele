/*
 * Copyright 2012-2014, Institute of Cybernetics at Tallinn University of Technology
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

import java.util.List;

public final class Log {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String LOG_TAG = "k6nele";

    private Log() {
    }

    public static void i(String msg) {
        if (DEBUG) android.util.Log.i(LOG_TAG, msg);
    }

    public static void i(List<String> msgs) {
        if (DEBUG) {
            for (String msg : msgs) {
                if (msg == null) {
                    msg = "<null>";
                }
                android.util.Log.i(LOG_TAG, msg);
            }
        }
    }

    public static void e(String msg) {
        if (DEBUG) android.util.Log.e(LOG_TAG, msg);
    }

    public static void e(String msg, Throwable throwable) {
        if (DEBUG) android.util.Log.e(LOG_TAG, msg, throwable);
    }

    public static void i(String tag, String msg) {
        if (DEBUG) android.util.Log.i(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (DEBUG) android.util.Log.e(tag, msg);
    }
}