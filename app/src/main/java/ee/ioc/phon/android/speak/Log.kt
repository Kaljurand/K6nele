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
package ee.ioc.phon.android.speak

import android.util.Log

object Log {
    // TODO: restore = BuildConfig.DEBUG once we understand how it works
    @JvmField
    val DEBUG = false
    private const val LOG_TAG = "k6nele"
    private const val NULL = "NULL"

    @JvmStatic
    fun i(msg: String?) {
        if (DEBUG) Log.i(LOG_TAG, msg ?: NULL)
    }

    @JvmStatic
    fun i(msgs: List<String?>) {
        if (DEBUG) {
            for (msg in msgs) {
                Log.i(LOG_TAG, msg ?: NULL)
            }
        }
    }

    @JvmStatic
    fun e(msg: String?) {
        if (DEBUG) Log.e(LOG_TAG, msg ?: NULL)
    }

    @JvmStatic
    fun e(msg: String?, throwable: Throwable?) {
        if (DEBUG) Log.e(LOG_TAG, msg ?: NULL, throwable)
    }

    @JvmStatic
    fun i(tag: String?, msg: String?) {
        if (DEBUG) Log.i(tag, msg ?: NULL)
    }

    @JvmStatic
    fun e(tag: String?, msg: String?) {
        if (DEBUG) Log.e(tag, msg ?: NULL)
    }
}