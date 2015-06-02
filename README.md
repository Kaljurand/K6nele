Kõnele
======

Kõnele is an Android app that offers speech-to-text services to other apps.
It contains the following main components:

  - RecognizerIntent activity
  - Voice keyboard that implements the input method editor (IME) API
  - SpeechRecognizer implementation that uses <https://github.com/alumae/kaldi-gstreamer-server>
  - SpeechRecognizer implementation that uses <https://github.com/alumae/ruby-pocketsphinx-server>

The main goals of this project is to offer support for Estonian speech recognition on the
Android platform, as well as grammar-based speech recognition for voice command applications.

For more information see <http://kaljurand.github.io/K6nele/>.


Compilation
-----------

Point to the SDK directory by setting the environment variable
`ANDROID_HOME`, e.g.

	ANDROID_HOME=${HOME}/myapps/android-sdk/

Then run

    gradle makeIcons
    gradle build
    gradle lint
    gradle assembleRelease

For the listing of more Gradle tasks, run:

	gradle tasks


If you have access to the release keystore then

  - point to its location by setting the environment variable `KEYSTORE`
  - set `KEY_ALIAS` to the key alias
  - create the file `gradle.properties` containing the lines:

		storePassword=<password1>
		keyPassword=<password2>


Tags
----

Version tags are set by e.g.

    git tag -a v1.1.02 -m 'version 1.1.02'

The last number should be even.


Contributions
-------------

The client for <https://github.com/alumae/kaldi-gstreamer-server>
was originally based on <https://github.com/truongdq54/kaldi-gstreamer-android-client>.
