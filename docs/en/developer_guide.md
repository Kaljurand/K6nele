---
layout: page
comments: true
title: Developer's Guide
---

## Introduction

Kõnele is an app that helps another app to communicate with two online speech recognition servers,
running the following software:

- continuous full-duplex server, <https://github.com/alumae/kaldi-gstreamer-server>,
- grammar-supporting server, <https://github.com/alumae/ruby-pocketsphinx-server>.

You can benefit from Kõnele either by implementing a new app and using
an existing running server, or deploying a new server and reconfiguring Kõnele to use it.

## Calling Kõnele as an activity

Kõnele implements [android.speech.RecognizerIntent](http://developer.android.com/reference/android/speech/RecognizerIntent.html) actions `ACTION_RECOGNIZE_SPEECH` and `ACTION_WEB_SEARCH`,
and supports its EXTRAs up to Android API level 3.

In addition to the standard EXTRAs, Kõnele adds the following EXTRAs:

| `SERVER_URL`          | URL                           | Web address of the speech recognizer server
| `GRAMMAR_URL`         | URL                           | Web address of a speech recognition grammar file
| `GRAMMAR_TARGET_LANG` | Comma-separated langugae codes| One or more identifiers of languages into which the recognizer-server should translate the raw speech recognition output
| `PHRASE`              | String                        | Desired transcription (could be used for adaptation)
| `GET_AUDIO`           | Boolean                       | Return audio iff true
| `GET_AUDIO_FORMAT`    | Mime type (only "audio/wav")  | Audio format

(The `GET_AUDIO` EXTRAs are prefixed by `android.speech.extra`, all others by
`ee.ioc.phon.android.extra`, e.g. `ee.ioc.phon.android.extra.SERVER_URL`.)

Note that the end-user _can_ override the server and grammar EXTRAs via a the Kõnele settings, i.e. your app should not assume that the specified server or grammar was actually used.

If you know that Kõnele is available on the device and is the only one with the required features
then you can call it directly, i.e. without any intermediate user-selection.
To do this, build a Recognizer-intent that can only be serviced by Kõnele's `RecognizerIntentActivity`.

{% highlight java %}
...
mIntent.setComponent(
    new ComponentName(
        "ee.ioc.phon.android.speak",
        "ee.ioc.phon.android.speak.RecognizerIntentActivity"));
{% endhighlight %}

## Calling Kõnele as a service

On Android API level 8+ you can also call Kõnele via [android.speech.SpeechRecognizer](http://developer.android.com/reference/android/speech/SpeechRecognizer.html).
In this case, please use `EXTRA_CALLING_PACKAGE` to identify your app for Kõnele.

To obtain a Kõnele-specific SpeechRecognizer-object, use the two-argument call to `createSpeechRecognizer`:

{% highlight java %}
SpeechRecognizer.createSpeechRecognizer(this,
    new ComponentName(
        "ee.ioc.phon.android.speak",
        "ee.ioc.phon.android.speak.SpeechRecognitionService");
    );
{% endhighlight %}

The available services are:

  - `ee.ioc.phon.android.speak.SpeechRecognitionService` (continuous full-duplex server)
  - `ee.ioc.phon.android.speak.WebSocketRecognizer` (grammar-supporting server)

The above-listed EXTRAs are also supported when calling Kõnele as a service, with the
exception of the `GET_AUDIO` EXTRAs.


## Calling Kõnele via Android Debug Bridge (adb)

The following two `adb shell` commands

  - stop a running instance of Kõnele (if present)
  - start Kõnele in a mode where the expected input is an action command in English (e.g. "one plus two") and the output is the corresponding expression ("1+2"), which gets opened in a web-browser

{% highlight sh %}
adb shell am force-stop ee.ioc.phon.android.speak; \
adb shell am start \
-n ee.ioc.phon.android.speak/.RecognizerIntentActivity \
-a android.speech.action.RECOGNIZE_SPEECH \
-e android.speech.extra.LANGUAGE_MODEL "free_form" \
-e android.speech.extra.LANGUAGE "en-US" \
-e ee.ioc.phon.android.extra.GRAMMAR_URL "http://kaljurand.github.com/Grammars/grammars/pgf/Action.pgf" \
-e ee.ioc.phon.android.extra.GRAMMAR_TARGET_LANG "App"
{% endhighlight %}


## Sample code

List of open-source apps that use Kõnele:

  - <http://github.com/Kaljurand/Arvutaja>
  - <http://github.com/v3rm0n/haalda>
