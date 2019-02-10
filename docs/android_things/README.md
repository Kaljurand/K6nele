Kõnele on Android Things
========================

_Work in progress_

Introduction
------------

Step-by-step instructions on how to build an Amazon Echo style voice assistant for Estonian using free software.

Steps
-----

Note that downloading and installing of the APKs can be done by executing the script `setup-k6nele-on-things.sh`,
which downloads the needed APKs (using `wget`), installs them to the current device (using `adb`),
and assigns their required permissions (using `adb`).

To complete the installation (e.g. adding the desired rewrite tables), one must separately run
Kõnele's `GetPutPreferenceActivity` to change the Kõnele settings.
This is best done using ``adb-pref.py`` as explained below.

### Pi

Set up a Raspberry Pi 3 with a microphone, a battery, a (USB?) loud speaker, and (optionally) a GPIO board with a button and lamp.
Power it on. A monitor is not required by the application but can be helpful during installation for getting visual feedback.
A mouse is not required but can be helpful during installation and testing. Some of the configuration steps in the instructions below are easier done with a mouse.

Install Android Things and set up wifi. (Internet is required but wifi is optional.)
Tested with `v1.0.8` on rpi3.

Install Android Debug Bridge (adb), which is part of the
[SDK Platform Tools](https://developer.android.com/studio/releases/platform-tools.html).

Connect to Pi.

    $ adb connect Android.local

(Optional) Look at the installed packages.

    $ adb shell pm list packages

(Optional) Look at settings

    $ adb shell am start -a android.settings.SETTINGS

(Optional) Look at the current voice input settings. There shouldn't be any voice input providers.

    $ adb shell am start -a android.settings.VOICE_INPUT_SETTINGS
    $ adb shell am start -n ee.ioc.phon.android.speak/.activity.RecServiceSelectorActivity

You can press BACK to finish the windows.

    $ adb shell input keyevent 4

### Kõnele

Install Kõnele and grant the audio recording permission.

    $ adb install K6nele-1.6.64.apk
    $ adb shell pm grant ee.ioc.phon.android.speak android.permission.RECORD_AUDIO

Configure Kõnele, e.g. import some rewrite tables (which define what the application does).

    # Make sure Kõnele is not running (and is not being restarted)
    adb shell am force-stop ee.ioc.phon.android.things.k6nelelauncher
    adb shell am force-stop ee.ioc.phon.android.speak

    # Disable the confirmation dialog. This way multiple settings can be changed
    # without having to confirm them.
    # If the confirmation dialog is currently enabled, then it pops up.
    # You need e.g. a mouse to press OK to disable it.
    # If the dialog is already disabled then the above command has no effect.
    adb-pref.py --disable-confirmation | sh

    # Install the desired rewrite rules and change some settings
    # (e.g. use a local recognition server instead of the default one).
    adb-pref.py prefs_android_things.yml | sh

    # Show the installed tables (needs a monitor and mouse)
    adb shell am start -n ee.ioc.phon.android.speak/.activity.RewritesSelectorActivity

(Optional) Start Kõnele to test it:

    $ adb shell am start -n ee.ioc.phon.android.speak/.activity.SpeechActionActivity

    # ... possibly overriding some EXTRAs
    $ adb shell am start -n ee.ioc.phon.android.speak/.activity.SpeechActionActivity --ez ee.ioc.phon.android.extra.AUTO_START true


### Speech trigger

Install speech-trigger (https://github.com/Kaljurand/speech-trigger)
and grant the permissions.
speech-trigger is an Android speech recognition service that returns once it hears a given phrase.

    $ adb install SpeechTrigger-0.1.21.apk
    # Recording permission
    $ adb shell pm grant ee.ioc.phon.android.speechtrigger android.permission.RECORD_AUDIO
    # Storage permission
    $ adb shell pm grant ee.ioc.phon.android.speechtrigger android.permission.WRITE_EXTERNAL_STORAGE

    # (Optional) Launch
    $ adb shell 'am start -n "ee.ioc.phon.android.speechtrigger/.MainActivity"'

### TTS

Android Things comes with Google's TTS.
For Estonian support install EKI TTS. Used for voice prompts.
Configure the system to use EKI TTS by default.

    # (Optional) Look at the current TTS settings and go BACK.
    # TODO: TTS_SETTINGS does not seem to be present
    $ adb shell am start -a com.android.settings.TTS_SETTINGS
    $ adb shell input keyevent 4

    # (TODO: maybe not needed) Uninstall/disable Google's TTS
    $ adb root
    $ adb connect Android.local
    $ adb uninstall com.google.android.tts
    $ adb shell 'pm disable com.google.android.tts'

    $ adb install EKISpeak-1.1.02.apk

TODO: don't know how to change the default TTS using adb. Connect a USB mouse, launch the `TTS_SETTINGS`
and use the mouse to change the default TTS engine.

### IoT launcher

Install an app of category `HOME`. This app is launched after Pi has finished booting up.
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
                        "\"ee.ioc.phon.android.extra.RESULT_REWRITES\": [\"Lights\", \"Lights.Hue\"]}}");
        return intent;
    }

An example is https://github.com/Kaljurand/things-k6nelelauncher

### Use

Reboot Pi and wait for the voice prompt (e.g. "Say the trigger phrase hey wake up").

    $ adb shell reboot

Alternatively launch it using adb.

    # Start recognition automatically.
    $ adb shell am start -n ee.ioc.phon.android.things.k6nelelauncher/.ActivityLauncherActivity

    # Start recognition when BCM4 is pressed.
    $ adb shell am start -n ee.ioc.phon.android.things.k6nelelauncher/.ActivityLauncherActivity --ez auto_start false -e gpio_button BCM4

Say "hey wake up" and wait for the next prompt (e.g. "öelge lambikäsk"), or repeat "hey wake up" if no prompt followed.

The rest of the dialog is defined by the triggered rewrite rules (e.g. "Lights" and "Lights.Hue").
Once the dialog and its launched activities finish, the trigger service continues with the prompt e.g. "Say the trigger phrase hey wake up").

(Optional) Stopping apps:

    $ adb shell am force-stop ee.ioc.phon.android.speechtrigger

Alternative way
---------------

Based on <https://developer.android.com/things/console/app_bundle.html>.

1. Install this image: TODO. It contains the latest Android Things + a bundle created like this:

       zip -j bundle.zip $APK/K6nele-1.6.64.apk $APK/SpeechTrigger-0.1.21.apk $APK/EKISpeak-1.1.04.apk $APK/app-debug.apk

2. Configure the Kõnele rewrite rules using GetPutPreferenceActivity

3. Reboot

Future updates (to step 1) will come as over-the-air (OTA) updates.

Guidelines for rewrite rules
----------------------------

- All activities started by the rules must finish immediately, e.g. switch on the lights and then finish.
  This way the control is given back to the trigger service. I.e. the rules should not start a web browser,
  which would stay on top of the activity stack.


Issues
------

- compile EKI TTS without the storage permission, it does not seem to be using it
- Google's TTS very slow to start up (before every utterance): 25 sec
- EKI TTS does not support English. TODO: this could be added to be able to test the speed.
- try also some other TTS engine
- make sure there is no other app using the GPIO BCM4
- analog audio/TTS does not work. TODO: Use a USB speaker.
- how to change the default TTS provider using adb
- scrolling the settings (with the mouse) does not work
- IME is not present: remove the IME settings on Things

Other
-----

Android Things does not offer a UI for changing the active IME, and as a result
tapping on the top item in Kõnele's settings causes Kõnele to crash.
The Kõnele IME can be enabled as follows:

    # Show the list of all IMEs,
    # Kõnele should show up, but is not enabled.
    adb shell ime list -a

    # Enable it.
    adb shell ime enable ee.ioc.phon.android.speak/.service.SpeechInputMethodService
