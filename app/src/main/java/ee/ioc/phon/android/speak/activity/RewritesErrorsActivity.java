/*
 * Copyright 2017, Institute of Cybernetics at Tallinn University of Technology
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

package ee.ioc.phon.android.speak.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;

import ee.ioc.phon.android.speak.R;

/**
 * <p>Simple activity that displays the String-array of error messages.</p>
 *
 * @author Kaarel Kaljurand
 */
public class RewritesErrorsActivity extends ListActivity {

    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    public static final String EXTRA_STRING_ARRAY = "EXTRA_STRING_ARRAY";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            String title = extras.getString(EXTRA_TITLE);
            if (title == null) {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            } else {
                setTitle(title);
            }
            String[] stringArray = extras.getStringArray(EXTRA_STRING_ARRAY);
            if (stringArray != null) {
                setListAdapter(new ArrayAdapter<>(this, R.layout.list_item_error, stringArray));
            }
        }
    }
}