package ee.ioc.phon.android.speak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {

	// When does the chunk sending start and what is its interval
	public static final int TASK_INTERVAL_SEND = 300;
	public static final int TASK_DELAY_SEND = 100;

	public static final String AUDIO_FILENAME = "audio.wav";

	public static final String DEFAULT_AUDIO_FORMAT = "audio/wav";
	public static final Set<String> SUPPORTED_AUDIO_FORMATS =
			new HashSet<String>(Arrays.asList(DEFAULT_AUDIO_FORMAT));
}