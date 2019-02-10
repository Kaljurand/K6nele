Kõnele on Android Wear
======================

(Work in progress)

Introduction
------------

Using Kõnele on Android Wear. (Tested with Wear OS by Google 2.2 on Huawei Watch 2.)

Installation
------------

The same Kõnele APK package that works on the phone also works on
the watch. However, the package cannot be installed to the watch via Google Play.
We perform the installation using the Android Debug Bridge (adb).

Also, the Kõnele UI has not been fully customized for the watch,
therefore some configuration steps are only possible using adb, calling
Kõnele's `GetPutPreferenceActivity`.

### Setting up adb

Install adb, which is part of the
[SDK Platform Tools](https://developer.android.com/studio/releases/platform-tools.html).

Connect to the watch as detailed in [Debugging an Android Wear App](https://developer.android.com/training/wearables/apps/debugging.html).

### Installing Kõnele

Download the latest APK from https://github.com/Kaljurand/K6nele/releases

    wget https://github.com/Kaljurand/K6nele/releases/download/v1.6.78/K6nele-1.6.78.apk

Install APK

    adb install K6nele-1.6.78.apk

(Alternative command, that specifies the watch IP address, and the reinstallation flag.)

    adb -s 192.168.0.29:5555 install -r K6nele-1.6.78.apk

(Optional) Verify that the package was installed

    adb shell pm list packages | fgrep "ee.ioc.phon.android.speak"

Configuring
-----------

### Global settings

The menu `Settings -> Personalization -> Customize hardware buttons` lets
you define an app (e.g. Kõnele) to be launched when a hardware button is pressed.

### Kõnele UI configuration

Suggestions:

- enable "Auto start"
- disable "Help text"

### Kõnele services configuration

- enable IME (also possible via `adb shell am start -a android.settings.INPUT_METHOD_SETTINGS`)

### Importing and configuring rewrite rules

There is no browser on the watch, which is used by default to show the transcription of the audio that is recorded when the Kõnele search panel is started via its launcher icon or from an intent that does not return to the caller (such as the `ASSIST` action). In order to support these use cases, we first have to import and configure some rewrite rules that define how the transcription is modified and which app is used to handle the (modified) transcription.

Import a rules table from a URL and give it a name ("Test" in the example below):

    adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyRewritesMap/Test -e val "http://kaljurand.github.io/K6nele/rewrites/tsv/k6_various.tsv" --ez is_url true

Set the table to be the default. Note that there can be multiple defaults.

    adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key defaultRewriteTables --esa val Test

(Optional) Add the table to the list of rewrite rule tables in the Kõnele settings. This is not needed for the rules to work, but needed if one wants to browse the rules via Kõnele settings.

    adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyRewritesMap --esa val Test

There is a script <https://github.com/Kaljurand/K6nele/blob/master/docs/adb-pref.py> that makes this configuration step simpler:

1. Enable `Run GetPutPreference without confirmation` in Kõnele's `Developer settings, demos, tools`
2. Run `adb-pref.py` with the settings overrides specified in YAML files, e.g.:

       adb-pref.py prefs_developer.yml prefs_user_guide_rewrites.yml android_wear/prefs_wear.yml | sh

### Apps to install

- TODO: something that supports the `VIEW` or `WEB_SEARCH` actions
- for Estonian TTS, install e.g. the EKI TTS service.
  The default TTS engine can be set in `Settings -> Accessibility -> Text-to-speech output`.

      wget https://github.com/Kaljurand/EKISpeak/releases/download/v1.2.03/EKISpeak-1.2.03.apk
      adb install EKISpeak-1.2.03.apk


Examples
--------

### Assistant

Long press on the upper button (on Huawei Watch 2) launches action `ASSIST`. Kõnele needs to be configured
with rewrite rules to be of any use, e.g. setting a timer using action `SET_TIMER` worked.

### Hangouts, Keep, etc.

Apps that integrate text input (e.g. Hangouts, Keep) offer 3 input modes:

- action `RECOGNIZE_SPEECH` (microphone icon),
- emoji,
- IME (keyboard icon).

Kõnele opens from the microphone icon and is available in the IME rotation. The first is
suitable for short inputs that cannot be edited. The IME mode offers basic editing with swipe
commands.

Known issues
------------

- FetchUrlActivity does not work with some (local?) URLs: connect timed out.
  The solution seems to be to disable Bluetooth and enable Wifi on the watch.

- searching for local recognition servers does not work. Again, the solution
  is to disable Bluetooth.

- watch does not have phone-like menus, e.g. the Action bar, but some tasks assume them

- the providers of `RECOGNIZE_SPEECH` and `ASSIST` are not configurable.
  On Huawei Watch 2 by default, Google's responds to these actions, but once Kõnele is installed, it overrides Google.
  Kõnele sets the `ASSIST` intent filter priority to -10 to let Google win for the `ASSIST` action.

- voice prompts use the system default TTS engine,
  which on Huawei Watch 2 is Google's text-to-speech engine by default,
  which does not support Estonian, so Kõnele falls back to Finnish.
  This triggers the downloading of Finnish TTS data when first used, which lasts a while.
  In general, TTS is very slow on Wear (e.g. Google's Finnish takes 20 sec to warm up), so consider designing your rewrite rules without voice prompts.
  (In my most recent tests TTS did not seem to work at all.)

- Android Wear does not support the Estonian UI

- the standard Android voice input settings are not present on Wear.

      adb shell am start -a android.settings.VOICE_INPUT_SETTINGS
      # results in: Activity not started, unable to resolve Intent.

- other actions that are not available: `VIEW`?, `WEB_SEARCH`
