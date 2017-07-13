Kõnele on Android Wear
======================

(Work in progress)

Introduction
------------

Using Kõnele on Android Wear. (Tested with Wear 2.0 on Huawei Watch 2.)

Installation
------------

Kõnele can work as a standalone app on the watch, but its UI has not been build for the watch,
therefore some configuration steps are only possible using adb.

### Setting up adb

Install Android Debug Bridge (adb), which is part of the
[SDK Platform Tools](https://developer.android.com/studio/releases/platform-tools.html).

Connect to the watch as detailed in [Debugging an Android Wear App](https://developer.android.com/training/wearables/apps/debugging.html).

Some (optional) commands:

    # Look at the installed packages.
    $ adb shell pm list packages

    # Look at settings
    $ adb shell am start -a android.settings.SETTINGS

    # Look at the current voice input settings.
    # Expected result: Activity not started, unable to resolve Intent.
    $ adb shell am start -a android.settings.VOICE_INPUT_SETTINGS

### Importing and configuring rewrite rules

There is no browser on the watch, which is used by default to show the transcription of the audio that is recorded when the Kõnele search panel is started via its launcher icon or from an intent that does not return to the caller (such as the ASSISTANT intent). In order to support these use cases, we first have to import and configure some rewrite rules that define how the transcription is modified and which app is used to handle the (modified) transcription.

Import a rules table from a URL and give it a name ("Test" in the example below):

    $ adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyRewritesMap/Test -e val "http://kaljurand.github.io/K6nele/rewrites/tsv/k6_various.tsv" --ez is_url true

Set the table to be the default. Note that there can be multiple defaults.

    $ adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key defaultRewriteTables --esa val Test

(Optional) Add the table to the list of rewrite rule tables in the Kõnele settings. This is not needed for the rules to work, but needed if one wants to browse the rules via Kõnele settings.

    $ adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyRewritesMap --esa val Test

Examples
--------

### Assistant

Long press on the upper button (on Huawei Watch 2) launces the ASSISTANT intent. Kõnele needs to be configured
with rewrite rules to be of any use, e.g. setting a timer using the timer-intent worked.

### Hangouts

Hangouts offers 3 input modes: RecognizerIntent (microphone icon), emoji, IME (keyboard icon).
Kõnele opens from the microphone icon and is available in the IME rotation. The Kõnele IME does not support
editing on Wear (swipe commands do not work and edits are not visible because the IME is not drawn over the
editor but instead over the input mode selection GUI).

TODO
----

- assign a custom button to K6nele, that would not override Google Assistant

- FetchUrl (as in the Hue use case) does not seem to work

- watch does not have menus (e.g. Action bar)

- some intents are not available: web search, all rec services

- no audio encoders on the watch?

- TTS is very slow

- IME could support swipe commands and show a small editor line
