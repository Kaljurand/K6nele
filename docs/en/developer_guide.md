---
layout: page
title: Developer's Guide
---

Introduction
------------

Kõnele is a component that helps an Android app to communicate with an online speech recognition server.

    app <-> Kõnele <-> server

i.e. you can benefit from Kõnele either by implementing an app or a server.

Calling Kõnele from Android apps
--------------------------------

Kõnele can be potentially used together with any Android app on the phone. How to build speech-enabled apps on Android is explained in:

  - <http://developer.android.com/resources/articles/speech-input.html>
  - <http://developer.android.com/guide/topics/search/search-dialog.html#VoiceSearch>

On API level 8+ you can also call Kõnele via [SpeechRecognizer](http://developer.android.com/reference/android/speech/SpeechRecognizer.html). In this case, please use EXTRA_CALLING_PACKAGE to identify your app for Kõnele.

Kõnele supports EXTRAs up to Android API Level 3 (see the documentation at <http://developer.android.com/reference/android/speech/RecognizerIntent.html>), and adds some additional EXTRAs:

`ee.ioc.phon.android.extra.SERVER_URL`
: Web address of the speech recognizer web service

`ee.ioc.phon.android.extra.GRAMMAR_URL`
: Web address of a speech recognition grammar file

`ee.ioc.phon.android.extra.GRAMMAR_TARGET_LANG`
: Identifier of the language into which the recognizer-server should translate the raw speech recognition output

`ee.ioc.phon.android.extra.PHRASE`
: Desired transcription for the enclosed audio

Note that the end-user *can* override these EXTRAs via a the Apps and Grammars settings, i.e. your app should not assume that the specified server or grammar was actually used.

Calling Kõnele directly
-----------------------

If you know that Kõnele is available on the device and is the only one with the required features (e.g. can do Estonian speech recognition, can do grammar-based speech recognition) then you can call it directly (i.e. without any intermediate user-selection):

First define two strings to hold the package and class names of Kõnele:

{% highlight xml %}
<string name="nameRecognizerPkg" translatable="false">ee.ioc.phon.android.speak</string>
<string name="nameRecognizerCls" translatable="false">ee.ioc.phon.android.speak.RecognizerIntentActivity</string>
{% endhighlight %}

and then build a Recognizer-intent that can only be serviced by the specified component (i.e. Kõnele's !RecognizerIntentActivity):

{% highlight java %}
String nameRecognizerPkg = getString(R.string.nameRecognizerPkg);
String nameRecognizerCls = getString(R.string.nameRecognizerCls);
...
mIntent.setComponent(new ComponentName(nameRecognizerPkg, nameRecognizerCls));
{% endhighlight %}

To obtain a Kõnele-specific [SpeechRecognizer](http://developer.android.com/reference/android/speech/SpeechRecognizer.html) object, use the two-argument call to `createSpeechRecognizer`:

{% highlight java %}
SpeechRecognizer.createSpeechRecognizer(this,
    new ComponentName(nameRecognizerPkg,
        "ee.ioc.phon.android.speak.SpeechRecognitionService");
    );
{% endhighlight %}


Calling Kõnele from adb (example)
---------------------------------

The following two adb shell commands

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


Interacting with online speech recognition servers
--------------------------------------------------

Kõnele can work with any online speech recognition server, provided that it is accessible via a simple HTTP-interface. The interaction with the server takes place via [Net Speech API]([http://code.google.com/p/net-speech-api/), i.e. the server must be compatible with this API.

Existing server implementations are:

  - <http://github.com/alumae/ruby-pocketsphinx-server>

Sample code
-----------

List of open-source apps that use Kõnele:

  - <http://github.com/Kaljurand/Arvutaja>
  - <http://github.com/v3rm0n/haalda>
