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

/**
 * <p>Set of non-standard extras that our RecognizerIntentActivity supports.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class Extras {

	// SERVER_URL should be a legal URL
	public static final String EXTRA_SERVER_URL = "ee.ioc.phon.android.extra.SERVER_URL";

	// GRAMMAR_URL should be a legal URL
	public static final String EXTRA_GRAMMAR_URL = "ee.ioc.phon.android.extra.GRAMMAR_URL";

	// Identifier of the target language (any string)
	public static final String EXTRA_GRAMMAR_TARGET_LANG = "ee.ioc.phon.android.extra.GRAMMAR_TARGET_LANG";

	// Desired transcription.
	// Using this extra, the user can specify to which string the enclosed audio
	// should be transcribed.
	public static final String EXTRA_PHRASE = "ee.ioc.phon.android.extra.PHRASE";

	// TODO: better names
	/**
	 * Key used to retrieve an {@code ArrayList<String>} from the {@link Bundle} passed to the
	 * {@link RecognitionListener#onResults(Bundle)} and
	 * {@link RecognitionListener#onPartialResults(Bundle)} methods. This list represents structured
	 * data: hyp1, out11, lang11, out12, lang12, ..., hyp2, out21, lang21, ...
	 * The number of linearizations for each hypothesis is given by an ArrayList<Integer> from a bundle
	 * item accessible via the key RESULTS_RECOGNITION_LINEARIZATION_COUNTS
	 */
	public static final String RESULTS_RECOGNITION_LINEARIZATIONS = "ee.ioc.phon.android.extra.RESULTS_RECOGNITION_LINEARIZATIONS";

	// TODO: document
	public static final String RESULTS_RECOGNITION_LINEARIZATION_COUNTS = "ee.ioc.phon.android.extra.RESULTS_RECOGNITION_LINEARIZATION_COUNTS";

}