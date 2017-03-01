#!/bin/sh

# Example of launching Kõnele using 'adb shell am'
# 1. Launch this script
# 2. Speak the name of a song/artist into the device
# 3. Some music player on the device starts playing the given song/artist
# Note that the service/locale must be among the selected combos.

component="ee.ioc.phon.android.speak/.service.WebSocketRecognitionService"
#component="com.google.android.googlequicksearchbox/com.google.android.voicesearch.serviceapi.GoogleRecognitionService"

language="et-EE"
#language="de-DE"

# Music player intent
intent='{
\"action\": \"android.media.action.MEDIA_PLAY_FROM_SEARCH\",
\"extras\": {
    \"android.intent.extra.focus\": \"vnd.android.cursor.item/*\",
    \"query\": \"\$1\"
    }
}'

# Google search query
#intent='play $1 on YouTube'

adb shell am force-stop ee.ioc.phon.android.speak;

adb shell 'am start \
-n ee.ioc.phon.android.speak/.activity.SpeechActionActivity \
-e android.speech.extra.LANGUAGE_MODEL "free_form" \
-e android.speech.extra.LANGUAGE "'$language'" \
-e ee.ioc.phon.android.extra.SERVICE_COMPONENT "'$component'" \
-e android.speech.extra.PROMPT "Say the name of a song or an artist" \
-e ee.ioc.phon.android.extra.VOICE_PROMPT "Öelge laulu või muusiku nimi" \
--ez ee.ioc.phon.android.extra.AUTO_START true \
--ei android.speech.extra.MAX_RESULTS 1 \
--ez ee.ioc.phon.android.extra.RESULT_LAUNCH_AS_ACTIVITY true \
-e ee.ioc.phon.android.extra.RESULT_ARG1 "'$intent'"'
