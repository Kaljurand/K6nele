#!/bin/sh

# Example of launching Kõnele using 'adb shell am'
#
# Usage:
#
# adb install /path/to/K6nele-x.y.zz.apk
# launch-k6nele.sh RecognizerIntentActivity

activity=SpeechActionActivity

if [ $# -eq 1 ]
then
    activity=$1
fi


adb shell am force-stop ee.ioc.phon.android.speak;

adb shell am start \
-n ee.ioc.phon.android.speak/.activity.${activity} \
-a android.speech.action.RECOGNIZE_SPEECH \
-e android.speech.extra.LANGUAGE_MODEL "free_form" \
-e android.speech.extra.LANGUAGE "en-US" \
-e android.speech.extra.MAX_RESULTS "10" \
-e ee.ioc.phon.android.extra.GRAMMAR_URL "http://kaljurand.github.com/Grammars/grammars/pgf/Action.pgf" \
-e ee.ioc.phon.android.extra.GRAMMAR_TARGET_LANG "App,Est" \
-e android.speech.extra.PROMPT "Please say: one plus two!" \
-e ee.ioc.phon.android.extra.PHRASE "one plus two"
