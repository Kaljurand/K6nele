#!/bin/sh

# Example of launching Kõnele using 'adb shell am' optimized for Android Things.
# First set up Android Things on a Raspberry Pi3 so that you can access it via adb
# by "adb connect Android.local",
# see https://developer.android.com/things/hardware/raspberrypi.html
#
# Setting up Kõnele on the device:
# 1. adb uninstall ee.ioc.phon.android.speak (if old version is installed)
#    adb install /path/to/K6nele-x.y.zz.apk
#      adb shell pm grant ee.ioc.phon.android.speak android.permission.RECORD_AUDIO (granting permissions)
#      or: adb reboot (this grants the permissions)
#    or: adb install -r -g /path/to/K6nele-x.y.zz.apk (reinstall and grant permissions, no reboot needed)
# 2. Import some rewrite rules from a URL
#    Current solution: use adb to write a rewrites table into the local storage
#    adb-pref.sh keyRewritesMap/Hue -e val "https://docs.google.com/spreadsheets/d/1x8FkaMoJ4_gJbg6w1vhir0gkWmqHuXDiB7otNr56Yb4/export?format=tsv" --ez is_url true
# 3. TODO: remove "Tap&Speak" + mic button (which do not make sense for a notouch device)
#    (using adb to modify the preferences + extending Kõnele not to refer to touch in the UI)
#    adb-pref.sh keyHelpText --ez val false
# 4. ...
# .. Look at some settings: adb shell am start -a android.settings.VOICE_INPUT_SETTINGS
# .. adb shell am start -a com.android.settings.TTS_SETTINGS
# .. press BACK: adb shell input keyevent 4
# .. adb shell 'pm list packages'
#
# Usage:
# 1. Launch this script, i.e. launch-k6nele-on-things.sh
# 2. Speak the name of a song/artist into the device
# 3. (TODO) Some music player on the device starts playing the given song/artist
# Note that the service/locale must be among the selected combos, or the default combo.
#
# Shutdown:
#
# adb shell reboot -p

activity="ee.ioc.phon.android.speak/.activity.SpeechActionActivity"
component="ee.ioc.phon.android.speak/.service.WebSocketRecognitionService"

language="et-EE"

# TODO: this fails on Android Things
# Music player intent
#intent='{
#\"action\": \"android.media.action.MEDIA_PLAY_FROM_SEARCH\",
#\"extras\": {
#    \"android.intent.extra.focus\": \"vnd.android.cursor.item/*\",
#    \"query\": \"\$1\"
#    }
#}'

# TODO: For the time being, just relaunch Kõnele to show the transcription
intent='{
\"action\": \"android.speech.action.RECOGNIZE_SPEECH\",
\"component\": \"ee.ioc.phon.android.speak/.activity.SpeechActionActivity\",
\"extras\": {
    \"android.speech.extra.PROMPT\": \"RESULT: \$1\"
    }
}'

# TODO: VOICE_PROMPT is excluded because triggers Google TTS (which resulted in a failed network connection)
# TODO: install an offline TTS, e.g. EKI TTS
# -e ee.ioc.phon.android.extra.VOICE_PROMPT "Öelge laulu või muusiku nimi" \

# TODO: AUDIO_CUES does not work

# TODO: do not require this, instead reuse the existing task
# (Currently results in Warning: Activity not started, its current task has been brought to the front)
adb shell am force-stop ee.ioc.phon.android.speak;

adb shell 'am start \
-n "'$activity'" \
-e android.speech.extra.LANGUAGE_MODEL "free_form" \
-e android.speech.extra.LANGUAGE "'$language'" \
-e ee.ioc.phon.android.extra.SERVICE_COMPONENT "'$component'" \
-e android.speech.extra.PROMPT "Say the name of a song or an artist" \
--ez ee.ioc.phon.android.extra.AUTO_START true \
--ez ee.ioc.phon.android.extra.AUDIO_CUES true \
--ei android.speech.extra.MAX_RESULTS 1 \
--ez ee.ioc.phon.android.extra.RESULT_LAUNCH_AS_ACTIVITY true \
-e ee.ioc.phon.android.extra.RESULT_ARG1 "'$intent'"'
