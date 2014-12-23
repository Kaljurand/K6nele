---
layout: page
title: About
permalink: /about/
---

Kõnele is an Android app that offers speech-to-text services to other apps.
Many apps contain a text area or a text field (e.g. a search field) that can be filled using
the keyboard. Kõnele provides a __voice keyboard__, a one-button keyboard, which allows speech to be converted to text.
Many apps (e.g. intelligent assistants, keyboard apps, navigation apps) also contain a microphone button that
is linked to the __standard Android speech recognition interface__. Kõnele provides two implementations of this interface.

In the background, Kõnele uses two cloud-based speech recognition servers.
One supports __grammar-based speech recognition__,
the other supports __continuous full-duplex speech recognition__.
Both servers focus on __Estonian speech recognition__, but both
are based on a fully open-source stack that make them easy to deploy and customize for other
languages.

The main goals of the Kõnele project is to offer support for Estonian speech recognition on the
Android platform, as well as grammar-based speech recognition for highly accurate voice command applications.
Kõnele is an open, configurable and powerful alternative to speech recognition services that are
currently available on Android.

<img src="../docs/img/Screenshot_2014-12-23-12-34-27_framed.png" width="250">
<img src="../docs/img/Screenshot_2014-12-23-12-30-19_framed.png" height="250">

## Features

For the end-user, Kõnele

  - provides a voice keyboard that can be used with any speech recognition service available on the device;
  - provides two speech recognition services that are pre-configured to use Estonian speech recognition;
  - provides a grammar-based speech recognition service for English and Estonian voice actions applications (e.g. alarm clock, unit converter, address search), for the list of existing Estonian and English grammars to use, see <http://kaljurand.github.io/Grammars/>
  - requires only 2 permissions (for recording and internet usage);
  - comes with two UI languages: English, Estonian.

For the app developer, Kõnele

  - implements [android.speech.RecognizerIntent](http://developer.android.com/reference/android/speech/RecognizerIntent.html) as of API level 3, with actions `ACTION_RECOGNIZE_SPEECH` and `ACTION_WEB_SEARCH`;
  - implements [android.speech.RecognitionService](http://developer.android.com/reference/android/speech/RecognitionService.html) (only in v0.8+, requires Android 2.2), in two ways:
    - one implementation uses <https://github.com/alumae/kaldi-gstreamer-server> (continuous full-duplex server);
    - the other uses <https://github.com/alumae/ruby-pocketsphinx-server> (grammar-supporting server);
  - is open and configurable, allowing one specify the grammars and recognition servers via intent extras.

## Availability

![](http://www.android.com/images/brand/android_app_on_play_logo_small.png)

  - The latest stable version is available on [Google Play Store][k6nele-play]
  - The latest beta version is available on Google Play after joining [this Google+ community][k6nele-beta] and clicking on "Signup for Kõnele"
  - The latest APK-versions are available at [GitHub Releases][k6nele-releases] (for older APKs see [here][k6nele-releases-old])
  - The source code is available at <https://github.com/Kaljurand/K6nele>

## Other

Kõnele is part of the project that was awarded Estonian Language Deed of the Year 2011.
See more [here](https://plus.google.com/u/0/111912457138686732050/posts/QMArF5Yvegs).


[k6nele-play]:          http://play.google.com/store/apps/details?id=ee.ioc.phon.android.speak
[k6nele-beta]:          http://plus.google.com/communities/116163027457318257829 
[k6nele-releases]:      http://github.com/Kaljurand/K6nele/releases
[k6nele-releases-old]:  http://code.google.com/p/recognizer-intent/downloads/list
