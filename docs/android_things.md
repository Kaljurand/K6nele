Free hands-free light switch or something else
==============================================

Introduction
------------

(Work in progress. Most links do not work.)

Step-by-step instructions on how to build an Amazon Echo style voice assistant for Estonian using free software.

Steps
-----

Set up a Raspberry Pi 3 with a microphone, a battery, a loud speaker, and (optionally) a GPIO board with a button and lamp.
Power it on. A monitor is not required by the application but can be helpful during installation for getting visual feedback.

Install Android Things and set up wifi.

Install Android Debug Bridge (adb), which is part of the
[SDK Platform Tools](https://developer.android.com/studio/releases/platform-tools.html).

Connect to Pi.

    $ adb connect Android.local

Install Kõnele and grant the audio recording permission.

    $ ver=1.6.56
    $ wget https://github.com/Kaljurand/K6nele/releases/download/v$ver/K6nele-$ver.apk
    $ adb install K6nele-$ver.apk
    $ adb shell pm grant ee.ioc.phon.android.speak android.permission.RECORD_AUDIO

Configure Kõnele, e.g. import for some rewrite tables (which define what the application does).

    # The Hue-table can be used to control Hue lights (needs to be customized with the the local IP and the Hue ID).
    $ adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyRewritesMap/Hue -e val "https://docs.google.com/spreadsheets/d/1x8FkaMoJ4_gJbg6w1vhir0gkWmqHuXDiB7otNr56Yb4/export?format=tsv" --ez is_url true

Install speech-trigger and grant the audio recording permission. TODO: and the storage permission?
speech-trigger is an Android speech recognition service that returns once it hears a given phrase.

    # TODO

Install EKI TTS. Used for voice prompts.
Configure the system to use EKI TTS by default.

    # TODO

Install an app of category `IOT_LAUNCHER`. This app is launched after Pi has finished booting up.
It should either immediately launch Kõnele (with the speech-trigger service) or do that when a GPIO button
is pressed. The intent to launch Kõnele would look something like this:

    // locale1: trigger locale (e.g. "en-US")
    // phrase1: trigger phrase (e.g. "hey wake up")
    // locale2: locale of the main command (e.g. "et-EE")
    // prompt2: voice prompt before listening for the main command (e.g. "öelge lambikäsk")
    // service2: service on the main command (e.g. "ee.ioc.phon.android.speak/.service.WebSocketRecognitionService")
    private static Intent createRecognizerIntent(String locale1, String phrase1,
                                                 String locale2, String prompt2, String service2) {
        Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        intent.setComponent(ComponentName.unflattenFromString("ee.ioc.phon.android.speak/.activity.SpeechActionActivity"));
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(Extras.EXTRA_VOICE_PROMPT, "Say the trigger phrase " + phrase1);
        intent.putExtra(Extras.EXTRA_SERVICE_COMPONENT, "ee.ioc.phon.android.speechtrigger/.TriggerService");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale1);
        intent.putExtra(Extras.EXTRA_AUTO_START, true);
        intent.putExtra(Extras.EXTRA_RETURN_ERRORS, true);
        intent.putExtra(Extras.EXTRA_FINISH_AFTER_LAUNCH_INTENT, false);
        intent.putExtra(Extras.EXTRA_PHRASE, phrase1);
        intent.putExtra(Extras.EXTRA_RESULT_UTTERANCE, ".+");
        intent.putExtra(Extras.EXTRA_RESULT_COMMAND, "activity");
        intent.putExtra(Extras.EXTRA_RESULT_ARG1,
                "{\"component\": \"ee.ioc.phon.android.speak/.activity.SpeechActionActivity\"," +
                        "\"extras\":{" +
                        "\"ee.ioc.phon.android.extra.SERVICE_COMPONENT\":\"" + service2 + "\"," +
                        "\"android.speech.extra.MAX_RESULTS\":1," +
                        "\"android.speech.extra.LANGUAGE\":\"" + locale2 + "\"," +
                        "\"ee.ioc.phon.android.extra.AUTO_START\":True," +
                        "\"ee.ioc.phon.android.extra.VOICE_PROMPT\":\"" + prompt2 + "\"," +
                        "\"ee.ioc.phon.android.extra.FINISH_AFTER_LAUNCH_INTENT\": True," +
                        "\"ee.ioc.phon.android.extra.RETURN_ERRORS\": True," +
                        "\"ee.ioc.phon.android.extra.RESULT_REWRITES\": [\"Hue\"]}}");
        return intent;
    }

Reboot Pi and wait for the voice prompt (e.g. "Say the trigger phrase hey wake up").

Say "hey wake up" and wait for the next prompt (e.g. "öelge lambikäsk"), or repeat "hey wake up" if no prompt followed.

The rest of the dialog is defined by the triggered rewrite rules (e.g. "Hue"). Once the dialog and its launched activities
finish, the trigger service continues with the prompt e.g. "Say the trigger phrase hey wake up").


Guidelines for rewrite rules
----------------------------

- All activities started by the rules must finish immediately, e.g. switch on the lights and then finish.
  This way the control is given back to the trigger service. I.e. the rules should not start a web browser,
  which would stay on top of the activity stack.
