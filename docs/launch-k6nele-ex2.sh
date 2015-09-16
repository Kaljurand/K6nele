#!/bin/sh

# Example of launching Kõnele using 'adb shell am'
#-e android.speech.extra.MAX_RESULTS 1 \

adb shell am force-stop ee.ioc.phon.android.speak;

adb shell 'am start \
-n ee.ioc.phon.android.speak/.activity.SpeechActionActivity \
-a android.speech.action.VOICE_SEARCH_HANDS_FREE \
-e android.speech.extra.LANGUAGE_MODEL "free_form" \
-e android.speech.extra.LANGUAGE "et-EE" \
-e android.speech.extra.PROMPT "Say a YouTube query" \
-e ee.ioc.phon.android.extra.RESULT_PREFIX "play " \
-e ee.ioc.phon.android.extra.RESULT_SUFFIX " on YouTube"'
