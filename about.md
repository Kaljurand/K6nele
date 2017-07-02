---
layout: page
title: About
permalink: /about/
---

Kõnele is an Android app that offers speech-to-text services to other apps.
Many apps contain a text area or a text field (e.g. a search bar) that can be edited using
the keyboard. Kõnele provides a __speech keyboard__, which allows text to be created via speaking.
Many apps (e.g. intelligent assistants, keyboard apps, navigation apps) also contain a microphone button that
is linked to either the __standard Android speech recognition activity__
or the __standard Android speech recognition service__.
Kõnele implements both the activity and the service.

In the background, Kõnele uses two speech recognition servers.
One supports __grammar-based speech recognition__,
the other supports __continuous full-duplex speech recognition__.
Both servers focus on __Estonian speech recognition__, but both
are based on a fully open-source stack that makes them easy to deploy and customize for other
languages.

The main goals of the Kõnele project are to provide Estonian speech recognition on the
Android platform, and to provide grammar-based speech recognition for voice command applications.
Kõnele is an open, configurable and powerful alternative to the speech recognition apps and services
currently available on Android.

<table>
<tr>
<td class="logo">
<img class="logo" title="Screenshot: address search with the Kõnele speech keyboard" src="{{ site.baseurl }}/images/en/screenshot_portrait_1_framed_20170702_resize20.png">
</td>
<td class="logo">
<img class="logo" title="Screenshot: directions search with the Kõnele voice search panel in split screen mode" src="{{ site.baseurl }}/images/en/screenshot_portrait_framed_20170702_resize20.png">
</td>
</tr>
<tr>
<td class="logo" colspan="2" align="center">
<img class="logo" title="Screenshot: composing an email with the Kõnele speech keyboard in landscape mode" src="{{ site.baseurl }}/images/en/screenshot_landscape_framed_20170702_resize20.png">
</td>
</tr>
</table>

## Features

Kõnele provides two user interface components that can use any speech recognition service/language available on the device:

  - the voice search panel can be called from other apps (typically by pressing a microphone button), and returns the recognition results to the app; in case the panel is opened via the Kõnele launcher icon, custom rewrite rules allow one to define which app is launched to interpret the recognition results (by default, the results are used to perform a web search using the default browser);
  - the speech keyboard can be used to dictate into any text field in any app, edit the result with swipe commands, and execute user-defined editor commands (select, copy, replace, undo, move cursor to next regex match etc.)

Kõnele also provides two speech recognition services that are pre-configured to use Estonian speech recognition:

  - a continuous full-duplex service supports audio input of unlimited length where dictation results are returned already while dictating;
  - a grammar-based speech recognition service supports English and Estonian voice actions (e.g. alarm clock, unit converter, address search), for the list of existing grammars see <http://kaljurand.github.io/Grammars/>.

The services can be used via Kõnele's own user interface components or by external apps like [Arvutaja](http://kaljurand.github.io/Arvutaja/).

Kõnele requires very few permissions (only the access to the microphone is essential), see the details in the [manifest](https://github.com/Kaljurand/K6nele/blob/master/app/AndroidManifest.xml).

See also:

- [User Guide]({{ site.baseurl }}/docs/et/user_guide.html) (in Estonian);
- [Developer's Guide]({{ site.baseurl }}/docs/en/developer_guide.html);
- [various notes](https://github.com/Kaljurand/K6nele/tree/master/docs).

## Availability

Kõnele is free and open source software.
Visit the [Kõnele GitHub page][k6nele-github] for the source code, bug reporting, etc.
The information and downloadable APK-packages for all the released versions are on the [Releases page][k6nele-releases].

A recent stable version is also available on [Google Play][k6nele-play], and
the latest testing version is available on Google Play after joining
[the testing program][k6nele-beta-link].

## Other

"Kõnele" is the Estonian word for "to speak" (imperative form).

Kõnele is part of the project that was awarded Estonian Language Deed (_Keeletegu_)
for the year 2011.
See more [here](http://plus.google.com/+KaarelKaljurand/posts/QMArF5Yvegs).


[k6nele-play]:          http://play.google.com/store/apps/details?id=ee.ioc.phon.android.speak
[k6nele-beta-link]:     https://play.google.com/apps/testing/ee.ioc.phon.android.speak
[k6nele-github]:        http://github.com/Kaljurand/K6nele
[k6nele-releases]:      http://github.com/Kaljurand/K6nele/releases
