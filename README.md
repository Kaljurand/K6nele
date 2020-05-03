Kõnele
======

[![Codacy Badge](https://api.codacy.com/project/badge/grade/b15968aa0a524d2986ba5deac6801196)](https://www.codacy.com/app/kaljurand/K6nele)

Kõnele is an Android app that offers speech-to-text services to other apps.
Its main components are:

  - a voice search panel (i.e. a [RecognizerIntent](http://developer.android.com/reference/android/speech/RecognizerIntent.html) activity)
  - two implementations of [SpeechRecognizer](http://developer.android.com/reference/android/speech/SpeechRecognizer.html), backed by two open source speech recognition servers
    - <https://github.com/alumae/kaldi-gstreamer-server>
    - <https://github.com/alumae/ruby-pocketsphinx-server>
  - a speech keyboard that implements the [input method editor (IME) API](http://developer.android.com/reference/android/inputmethodservice/InputMethodService.html)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/ee.ioc.phon.android.speak/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=ee.ioc.phon.android.speak)

The diagram below shows Kõnele's main components in yellow, while the standard Android interfaces via which other apps can interact with Kõnele are in green.

![Components](https://rawgithub.com/Kaljurand/K6nele/master/docs/components.dot.svg)

The main goals of this project is to offer support for Estonian speech recognition on the
Android platform, as well as grammar-based speech recognition for voice command applications.

How to add support for other languages than Estonian is detailed in https://github.com/Kaljurand/K6nele/issues/38

For documentation, APKs, app store links, news etc. see <http://kaljurand.github.io/K6nele/>.


Building the APK from source
----------------------------

Clone the source code including the `net-speech-api` and `speechutils` submodules:

    git clone --recursive git@github.com:Kaljurand/K6nele.git


Point to the Android SDK directory by setting the environment variable
`ANDROID_HOME`, e.g.

    ANDROID_HOME=${HOME}/myapps/android-sdk/

Create the file `gradle.properties` containing the lines:

    android.enableD8=true

Build the Kõnele app

    ./gradlew assemble


If you have access to a release keystore then

  - point to its location by setting the environment variable `KEYSTORE`
  - set `KEY_ALIAS` to the key alias
  - add these lines to `gradle.properties`:

        storePassword=<password1>
        keyPassword=<password2>


The (signed and unsigned) APKs will be generated into `app/build/outputs/apk/`.


Contributions
-------------

The client for <https://github.com/alumae/kaldi-gstreamer-server>
was originally based on <https://github.com/truongdq54/kaldi-gstreamer-android-client>.

Please read through the [Contributing Guide](CONTRIBUTING.md) before making a pull request.
