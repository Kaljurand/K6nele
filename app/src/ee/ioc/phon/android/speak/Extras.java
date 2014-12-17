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
 * <p>Set of non-standard extras that K6nele supports.</p>
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

    // Bundle with information about the editor in which the IME is running
    public static final String EXTRA_EDITOR_INFO = "ee.ioc.phon.android.extra.EDITOR_INFO";

    // Boolean to indicate that the recognition service should not stop after delivering the first result
    public static final String EXTRA_UNLIMITED_DURATION = "ee.ioc.phon.android.extra.UNLIMITED_DURATION";

    // Boolean to indicate that the server has sent final=true, i.e. the following hypotheses
    // will not be transcriptions of the same audio anymore.
    public static final String EXTRA_SEMI_FINAL = "ee.ioc.phon.android.extra.SEMI_FINAL";

	// Caller is interested in the recorded audio data (boolean)
	public static final String GET_AUDIO = "android.speech.extra.GET_AUDIO";

	// Caller wants to have the audio data in a certain format (String)
	public static final String GET_AUDIO_FORMAT = "android.speech.extra.GET_AUDIO_FORMAT";

	/**
	 * <p>Key used to retrieve an {@code ArrayList<String>} from the {@link Bundle} passed to the
	 * {@link RecognitionListener#onResults(Bundle)} and
	 * {@link RecognitionListener#onPartialResults(Bundle)} methods. This list represents structured
	 * data:</p>
	 * <ul>
	 *   <li>raw utterance of hypothesis 1
	 *   <li>linearization 1.1
	 *   <li>language code of linearization 1.1
	 *   <li>linearization 1.2
	 *   <li>language code of linearization 1.2
	 *   <li>...
	 *   <li>raw utterance of hypothesis 2
	 *   <li>...
	 * </ul>
	 *
	 * <p>The number of linearizations for each hypothesis is given by an ArrayList<Integer> from a bundle
	 * item accessible via the key RESULTS_RECOGNITION_LINEARIZATION_COUNTS.
	 * Both of these bundle items have to be present for the client to be able to use the results.</p>
	 */
	public static final String RESULTS_RECOGNITION_LINEARIZATIONS = "ee.ioc.phon.android.extra.RESULTS_RECOGNITION_LINEARIZATIONS";
	public static final String RESULTS_RECOGNITION_LINEARIZATION_COUNTS = "ee.ioc.phon.android.extra.RESULTS_RECOGNITION_LINEARIZATION_COUNTS";

}