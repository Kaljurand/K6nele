Kõnele on Android Wear
======================

(Work in progress)

Introduction
------------

Using Kõnele on Android Wear. (Tested with Wear 2.0 on Huawei Watch 2.)

Installation
------------

Kõnele can work as a standalone app on the watch, but its UI has not been built for the watch,
therefore some configuration steps are only possible using adb.

### Setting up adb

Install Android Debug Bridge (adb), which is part of the
[SDK Platform Tools](https://developer.android.com/studio/releases/platform-tools.html).

Connect to the watch as detailed in [Debugging an Android Wear App](https://developer.android.com/training/wearables/apps/debugging.html).


### Installing Kõnele

In case the Wear-compatible version of Kõnele is not available on Google Play, then install it using adb:

    # use install -r for reinstallations
    adb install K6nele-1.6.62.apk

    # (Optional) Verify that the package was installed
    adb shell pm list packages | fgrep "ee.ioc.phon.android.speak"

Configuring
-----------

### Global settings

    # Look at settings
    adb shell am start -a android.settings.SETTINGS

    # The standard Android voice input settings are not present on Wear.
    # Expected result: Activity not started, unable to resolve Intent.
    adb shell am start -a android.settings.VOICE_INPUT_SETTINGS

The menu `Settings -> Personalization -> Customize hardwear buttons` lets
you define an app to be launched (e.g. Kõnele) when pressing a hardware button.

### Kõnele UI configuration

Suggestions:

- enable "Auto start"
- disable "Help text"

### Kõnele services configuration

- enable IME
- keep the raw encoding (FLAC is not supported on Wear)

### Importing and configuring rewrite rules

There is no browser on the watch, which is used by default to show the transcription of the audio that is recorded when the Kõnele search panel is started via its launcher icon or from an intent that does not return to the caller (such as the `ASSIST` action). In order to support these use cases, we first have to import and configure some rewrite rules that define how the transcription is modified and which app is used to handle the (modified) transcription.

Import a rules table from a URL and give it a name ("Test" in the example below):

    adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyRewritesMap/Test -e val "http://kaljurand.github.io/K6nele/rewrites/tsv/k6_various.tsv" --ez is_url true

Set the table to be the default. Note that there can be multiple defaults.

    adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key defaultRewriteTables --esa val Test

(Optional) Add the table to the list of rewrite rule tables in the Kõnele settings. This is not needed for the rules to work, but needed if one wants to browse the rules via Kõnele settings.

    adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyRewritesMap --esa val Test

There is a script <https://github.com/Kaljurand/K6nele/blob/master/docs/adb-put-rewrites.sh> that makes this configuration step simpler.

### Notes about rewrite rules

- voice prompts use the system default TTS engine,
  which on Huawei Watch 2 is Google's text-to-speech engine by default,
  which does not support Estonian, so Kõnele falls back to Finnish.
  This triggers the downloading of Finnish TTS data when first used, which lasts a while.
  In general, TTS is very slow on Wear (e.g. Google's Finnish takes 20 sec to warm up), so consider designing your rewrite rules without voice prompts.

### Apps to install

- TODO: something that supports the `VIEW` or `WEB_SEARCH` actions
- for Estonian TTS, install e.g. the EKI TTS service (e.g. from https://github.com/Kaljurand/EKISpeak/releases,
  this service is also slow, starting to speak ~20 sec after being started).
  The default TTS enging can be set in `Settings -> Accesibility -> Text-to-speech output`.


Examples
--------

### Assistant

Long press on the upper button (on Huawei Watch 2) launces action `ASSIST`. Kõnele needs to be configured
with rewrite rules to be of any use, e.g. setting a timer using action `SET_TIMER` worked.

### Hangouts

Hangouts offers 3 input modes: action `RECOGNIZE_SPEECH` (microphone icon), emoji, IME (keyboard icon).
Kõnele opens from the microphone icon and is available in the IME rotation. The Kõnele IME does not support
editing on Wear (swipe commands do not work and edits are not visible because the IME is not drawn over the
editor but instead over the input mode selection GUI).

TODO
----

- do not declare support for ASSISTANT (only on Wear), because it overrides Google Assistant

- FetchUrlActivity does not work with some (local?) URLs: connect timed out.
  The solution seems to be to disable Bluetooth and enable Wifi on the watch.

- watch does not have menus (e.g. Action bar)

- some actions are not available: VIEW?, `WEB_SEARCH`, `VOICE_INPUT_SETTINGS`

- the providers of `RECOGNIZE_SPEECH` and `ASSIST` are not configurable.
  On Huawei Watch 2 by default, Google's responds to these actions, but once Kõnele is installed, it overrides Google.

- (Google's) TTS is very slow on Wear

- IME could support swipe commands and show a small editor line which reflects rewritten text and editing results
