---
layout: page
title: About
permalink: /about/
---

Kõnele is an Android app that offers speech-to-text services to other apps.
Many apps contain a text area or a text field (e.g. a search bar) that can be edited using
the keyboard. Kõnele provides a __speech keyboard__, a one-button keyboard, which allows speech to be converted to text.
Many apps (e.g. intelligent assistants, keyboard apps, navigation apps) also contain a microphone button that
is linked to the __standard Android speech recognition interface__. Kõnele provides two implementations of this interface.

In the background, Kõnele uses two speech recognition servers.
One supports __grammar-based speech recognition__,
the other supports __continuous full-duplex speech recognition__.
Both servers focus on __Estonian speech recognition__, but both
are based on a fully open-source stack that makes them easy to deploy and customize for other
languages.

The main goals of the Kõnele project are to provide Estonian speech recognition on the
Android platform, and to provide grammar-based speech recognition for highly accurate voice command applications.
Kõnele is an open, configurable and powerful alternative to speech recognition services
currently available on Android.

<table>
<tr>
<td class="logo">
<img class="logo" title="Screenshot: address search using Kõnele" src="{{ site.baseurl }}/images/en/screenshot_portrait_framed_20150712_resize20.png">
</td>
<td class="logo">
<img class="logo" title="Screenshot: composing an email with Kõnele" src="{{ site.baseurl }}/images/en/screenshot_landscape_framed_20150712_resize20.png">
</td>
</tr>
</table>

## Features

__For the end-user, Kõnele__

  - provides a simple speech recognition activity, that opens from the Kõnele launcher icon, and that is also callable from other Android apps;
  - provides a speech keyboard that can be used to dictate into any text field (apart from password fields) in any app using any speech recognition service/language available on the device;
  - provides two speech recognition services that are pre-configured to use Estonian speech recognition;
  - provides a grammar-based speech recognition service for English and Estonian voice actions applications (e.g. alarm clock, unit converter, address search), for the list of existing grammars see <http://kaljurand.github.io/Grammars/>;
  - requires only two permissions (to access the microphone and the internet);
  - comes with two user interface languages: English, Estonian.

See more in the [User Guide]({{ site.baseurl }}/docs/et/user_guide.html) (only in Estonian).

__For the app developer, Kõnele__

  - implements the [android.speech.RecognizerIntent](http://developer.android.com/reference/android/speech/RecognizerIntent.html) actions `ACTION_RECOGNIZE_SPEECH` and `ACTION_WEB_SEARCH`, and supports most of its EXTRAs;
  - implements [android.speech.RecognitionService](http://developer.android.com/reference/android/speech/RecognitionService.html), backed by two open servers:
    - <http://github.com/alumae/kaldi-gstreamer-server> (continuous full-duplex server),
    - <http://github.com/alumae/ruby-pocketsphinx-server> (grammar-supporting server);
  - is open and configurable, e.g. allowing one to specify the grammars and recognition servers via intent EXTRAs.

See more in the [Developer's Guide]({{ site.baseurl }}/docs/en/developer_guide.html).

## Availability

Kõnele is free and open source software.
Visit the [Kõnele GitHub page][k6nele-github] for the source code, bug reporting, etc.
The release information and downloadable APK-packages are on the [Releases page][k6nele-releases].

The latest stable version is available in the [Google Play Store][k6nele-play]
and on [F-Droid][k6nele-fdroid].
The latest beta version is available in the Google Play Store after joining
[the beta program][k6nele-beta-link]. (Alternatively, you can also join
[this Google+ community][k6nele-beta] and click on "Signup for Kõnele".)

![](https://developer.android.com/images/brand/en_app_rgb_wo_60.png)
![]({{ site.baseurl }}/images/en/available-on-fdroid.png)

## Other

"Kõnele" is the Estonian word for "to speak" (imperative form).

Kõnele is part of the project that was awarded Estonian Language Deed (_Keeletegu_)
for the year 2011.
See more [here](http://plus.google.com/+KaarelKaljurand/posts/QMArF5Yvegs).


[k6nele-play]:          http://play.google.com/store/apps/details?id=ee.ioc.phon.android.speak
[k6nele-beta]:          http://plus.google.com/communities/116163027457318257829
[k6nele-beta-link]:     https://play.google.com/apps/testing/ee.ioc.phon.android.speak
[k6nele-github]:        http://github.com/Kaljurand/K6nele
[k6nele-releases]:      http://github.com/Kaljurand/K6nele/releases
[k6nele-releases-old]:  http://code.google.com/p/recognizer-intent/downloads/list
[k6nele-fdroid]:        https://f-droid.org/repository/browse/?fdid=ee.ioc.phon.android.speak
