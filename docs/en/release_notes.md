---
layout: page
title: Release notes
---

(For more detailed notes see <https://github.com/Kaljurand/K6nele/commits/master>)

There are currently 2 branches:

  * v0.7.xx (not maintained):
    * requires Android 1.6+
    * launcher icon opens the settings
  * v0.8.xx:
    * requires Android 2.2+
    * can do everything that v0.7.xx can, but additionally implements the RecognitionService-interface
    * settings are part of the global settings
    * launcher submits the transcription to the standard websearch

----

(Old issue numbers refer to <https://code.google.com/p/recognizer-intent/issues/list>)

## v0.8

### v0.8.56 (2015-01-10) (released on Google Play: 2015-01-10)

1. Added a new speech recognizer service that uses the continuous dictation server
<http://github.com/alumae/kaldi-gstreamer-server>, offering faster and more accurate
transcription (but no grammar support).
If set to default in ``Settings -> Language & input -> Speech -> Voice input`` then this
service can be used in other apps (e.g. in Google Translate) for Estonian voice input.

2. Added support for input method editor (IME), which offers a one-button keyboard:

    - press the button to start/stop/cancel speech recognition
    - swipe left to delete the word left from cursor
    - swipe right to add a newline
    - double tap to insert a space
    - press the keyboard icon to switch to the next/last keyboard
    - long press the keyboard icon to open a keyboard switcher
    - press the search icon to launch the search (if editing a search bar)

The IME must be first enabled in
``Settings -> Language & input -> Keyboard & input methods``.
Note that it uses the new service by default but can be configured in the
Kõnele settings to use any speech recognizer service available on the phone
(although it has been tested only with Kõnele's own services).

### v0.8.44 (2014-11-02) (released on Google Play: 2014-11-02)

  - support for extras:
    - `android.speech.extra.GET_AUDIO`
    - `android.speech.extra.GET_AUDIO_FORMAT` (only if set to "audio/wav")
  - minor fixes

### v0.8.42 (2013-08-01) (released on Google Play: 2013-08-03)

  * fixed: audio level was not immediately restored if an error occurred during recognition. (Only in case of GUI-less recognition, e.g. when called from a keyboard app.)

### v0.8.40 (2013-01-22) (released on Google Play: 2013-01-22)

  * improved: now works when called from Google Chrome

### v0.8.38 (2013-01-20)

  * added: now ask the server for max 5 results and pop up a disambiguation dialog in case more than 1 results are delivered (in GUI mode and given that there is no calling activity)
  * added: Settings-button to K6nele GUI, greatly simplifies access to the settings
  * added: boolean setting "respect device default locale" (takes effect in case the calling app does not specify the language of the input)
  * improved: minor GUI and documentation improvements

### v0.8.36 (2013-01-13)

  * added: two K6nele-specific result extras to be able to communicate all the output of the phon-server to the calling activity:
    * `ee.ioc.phon.android.extra.RESULTS_RECOGNITION_LINEARIZATIONS`
    * `ee.ioc.phon.android.extra.RESULTS_RECOGNITION_LINEARIZATION_COUNTS`
  * added: support the "selectedLanguage" extra provided by some IMEs
  * added: communicate the value of `EXTRA_PARTIAL_RESULTS` to the server
  * improved: update net-speech-api (might behave better if network is slow)
  * improved: simpler way to switch recognition servers
  * improved: About/Info activity

### v0.8.30 (2012-10-14) (released on Google Play: 2012-10-14)

  * fixed: Issue 14
  * fixed: potential memory leak (and NPE?)

### v0.8.28 (2012-08-04) (released on Google Play: 2012-08-04)

  * improved: now sending chunks with a smaller time interval, thus obtaining the transcription faster
  * fixed: server connection was sometimes not correctly closed resulting in "no transcription found"

### v0.8.26 (2012-04-27) (released on Google Play: 2012-04-28)

  * improved the recorder

### v0.8.23 (2012-03-04) (released on Android Market: 2012-03-05)

  * added: volume indicator
  * improved: volume indicator in keyboard apps
  * fixed: some (potential) crashes related to stopping the recorder and dropping the network connection
  * fixed: grammars could not be assigned to some apps (e.g. Dolphin Browser and iris.)

### v0.8.22 (2012-02-14) (released on Android Market: 2012-02-14)

  * added: support for `android.speech.action.GET_LANGUAGE_DETAILS`
  * added: more meaningful user agent comment
  * minor fixes

### v0.8.18 (2012-02-10)

  * added: option to move to SD card
  * removed: 8kHz recording option, because this is known to result in low quality transcription
  * improved: Estonian About-page, e.g. describe Speaktoit+Alarm as a possible use case for grammar-based speech recognition
  * added: support for `EXTRA_PHRASE` which could be useful for 3rd party calibration and speech data collection apps


### v0.8.17 (2012-02-06)

  * added: launcher icon. Clicking on it will open the recognizer UI but the recognition results are passed to the standard websearch (because there is no calling activity)
  * added: support for landscape mode and device orientation change during recording/transcribing
  * added: beep for error condition
  * changed: NO_MATCH result is now handled within the Kõnele GUI instead of returning it to the caller
  * changed: some font sizes to be compatible with the recommendations of <http://developer.android.com/design/>

### v0.8.16 (2012-01-17)

  * simplified the way the recorder buffer size is calculated (might make recording work on some devices where it didn't work before)
  * improved recorder performance (affects slower devices like HTC Wildfire)
  * UI change: Stop-button is now hidden if Autostop is enabled

### v0.8.12 (2012-01-02)

It is now possible to configure which sample rate is used when recording the audio. The default 16kHz offers the best quality / upload speed balance, but it might not be available on all devices. If recording does not work with 16kHz then try another rate (in particular 44.1kHz). See also Issue 20.

### v0.8.10 (2011-12-20) (released on Android Market: 2011-12-20)

  * updated the links on the About-page
  * fix: default settings did not correspond to the settings UI when the app was used for the very first time

### v0.8.08 (2011-12-18)

  * fixed: force close if the server was set to a URL which exists but which does not provide speech recognition service (e.g. http://example.com) (fixed by updating to netspeechapi-0.1.5.jar)

### v0.8.07 (2011-12-16)

  * fixed: `RecognitionService`: max recording time was not enforced when pause detection was off
  * fixed: `RecognitionService`: max recording time was incorrectly calculated
  * added: xhdpi icons
  * app name is now Kõnele in every localization
  * tiny UI improvements

### v0.8.06 (2011-12-14)

  * now identifying the `RecognitionService`-user via `EXTRA_CALLING_PACKAGE`
  * improved About-page

### v0.8.05 (2011-12-13)

  * improved: `RecognitionService`: `rmsChanged`
  * now recording from [MediaRecorder.AudioSource.VOICE_RECOGNITION](http://developer.android.com/reference/android/media/MediaRecorder.AudioSource.html#VOICE_RECOGNITION) instead of `MIC`, not sure on which devices it has any effect
  * URL-input field (in Servers and Grammars) requests URI-aware keyboard
  * improved some strings
  * minor cleanup in response to the Android `lint` tool

### v0.8.03 (2011-12-08)

  * `RecognitionService`-interface:
    * bug and stability fixes
    * preliminary support for the waveform-display

### v0.8.02 (2011-12-07)

Preliminary implementation of the `RecognitionService`-interface.

## v0.7

### v0.7.36 (2011-12-05) (released on Android Market: 2011-12-31)

  * All error messages apart from `RESULT_NO_MATCH` are now displayed as part of the Speak UI and not sent to the calling app as result codes (see Issue 15). The two most common errors are:
    * network connection is not switched on
    * recording fails, e.g. another app is already recording
  * Minor GUI changes:
    * nicer record-button
    * removed Add-menu from the App-list
    * App-list sort order is now persistent (e.g. survives rotation)
  * Added: Alarm-grammar (which is also included in the Action-grammar)
    * The Alarm-grammar allows you to set alarms in apps like Speaktoit by saying e.g. "ärata mind kell kaheksa null üks".

### v0.7.34 (2011-12-01)

  * renamed package name: recognizerintent -> speak
  * UI changes:
    * added: menu icons (from Android 4)
    * changed: microphone icons (from Android 4)
    * tiny change to the launcher icon
    * recorder/recognizer prompt can now have 2 lines

### v0.7.33 (2011-11-29)

  * new "start/stop recording" sounds (Issue 2)
    * taken from the [Eyes-Free project](http://eyes-free.googlecode.com): `/trunk/TalkBack/res/raw/explore_(begin|end).ogg`

### v0.7.32 (2011-11-22)

Minor changes:

  * fewer confusing exception messages are shown to the user
  * apps: app frequency counter now has a background color
  * grammars list: Exp -> Expr

### v0.7.31 (2011-11-11)

Now using `netspeechapi-0.1.3.jar` which allows us to tell the server how many transcription hypotheses to generate (`nbest`). This is calculated based on `EXTRA_MAX_RESULTS`:

{% highlight java %}
int nbest = (mExtraMaxResults > 1) ? mExtraMaxResults : 1;
{% endhighlight %}

Note that if the client does not specify `EXTRA_MAX_RESULTS` then a single hypothesis is generated, but it might have several linearizations which all are returned. However, when the client sets `EXTRA_MAX_RESULTS` to 1 then just one linearization is returned, even if there are more. In case `EXTRA_MAX_RESULTS = 2`, then 2 linearizations are returned but the client won't find out if they are from the same hypothesis, if there are more linearizations, etc. This confusion arises from the fact that we are turning the complex list-of-lists structure returned by the server into the simple list required by the standard `RecognizerIntent` return extra.

### v0.7.30 (2011-11-06)

  * improved: removed the need for the `READ_PHONE_STATE` permission. Now sending a UUID which is not connected to any existing user ID and which the user can reset by
    * `Settings -> Applications -> Manage applications -> Speak -> Clear data (or Uninstall)`
  * now uploading a flac-file in the test-menu (this is just to make the APK smaller, and test the content-type setting)

### v0.7.29 (2011-11-04)

  * added: support for `android.speech.extra.PROMPT`. This allows the caller app to display a short message as part of the recorder/recognizer UI. (In the future, this could be used in calibration, where the speaker needs to be shown the phrase that he/she is supposed to utter.)
  * trying out new beep sounds (they are too noisy though)
  * fixed: Issue 8
  * improved the Apps list: the target language is now shown, as well as a marker for the "unrestricted" grammar

### v0.7.28 (2011-11-03)

  * added EXTRA: `ee.ioc.phon.android.extra.SERVER_URL` (Issue 9)
  * worked on Issue 10, previously the user could set the server/grammar EXTRAs, but could not override them if the calling app had set them, now the user can override the following EXTRAs:
    * `SERVER_URL`
    * `GRAMMAR_URL`
    * `GRAMMAR_TARGET_LANG`
  * added: "unrestricted input" grammar (empty string for both the grammar URL and the target lang) which can be used to disable the incoming grammar EXTRAs
  * possibly a bit better pause detection (Issue 3)

### v0.7.27 (2011-11-02)

  * updated to netspeechapi-0.1.1.jar, i.e. empty string is not accepted as transcription anymore
  * tiny UI changes
  * possibly a bit better pause detection (Issue 3)
  * grammar table: URL+Lang must be unique (before URL had to be unique), i.e. the user can add the same PGF (e.g. Address) several times if the languages differ (e.g. Est, Gmaps, Edwin, Wolfram, ...)
  * Apps/Grammars DB update

### v0.7.25 (2011-10-27)

  * renamed the extras (again), new names:
    * `ee.ioc.phon.android.extra.GRAMMAR_URL`
    * `ee.ioc.phon.android.extra.GRAMMAR_TARGET_LANG`
  * improved the recognizer-activity UI
    * it has now a constant size during its 3 states (initial, recording, transcribing)
  * Apps/Grammars DB update

### v0.7.23 (2011-10-21)

Minor changes:

  * more info on the About-page
  * UI improvements, e.g. the size of the waveform is now density-independent

### v0.7.22 (2011-10-13)

Apart from a few UI changes (e.g. some checkboxes are now initially on) all the changes are about grammar-based speech recognition support.

  * now using the new `net-speech-api` that added support for PGF-grammars
  * removed the menu item for notifying the server about the grammar
  * complete update of the grammars database
  * new extras: `EXTRA_GRAMMAR_URL`, `EXTRA_GRAMMAR_LANG`
  * removed extra: `EXTRA_GRAMMAR_JSGF`
  * now all demo activities have an "assign grammar" menu that lets you restrict the input to the given grammar
  * when adding a new grammar URL, the name of the grammar is filled in automatically (filename without the last extension)
  * in addition to changing an existing grammar URL, one can now also change the name and language of the grammar

(Language refers to the machine language that is the target of the translation of the raw speech recognizer result.)

### v0.7.19 (2011-10-07)

  * fixed: issue 1
  * added: more grammar URLs
  * removed: a test audio file to reduce the apk size

### v0.7.18 (2011-10-05)

  * fixed: UI blocked if internet was slow
  * added: a small dot is printed for every chunk that was successfully sent to the server
  * improved: the Repeater-demo now has a menu which allows the user to select a grammar to be used (previously the grammar was preset and could not be changed)

### v0.7.17 (2011-09-26)

  * new package name
  * minor UI fixes

### v0.7.16 (2011-09-25)

  * tapping on an app icon now launches the app (previously one needed a long-tap to open the context menu which contained the launch option)
  * added: some grammar URLs
  * grammar-URL-based sorting now goes in the different direction
  * now always building with !ProGuard resulting in a much smaller package size

### v0.7.15 (2011-08-04)

  * improved Estonian localization
  * fixed some UI bugs

### v0.7.13 (2011-07-13)

  * added: support for different recognition servers
