Kõnele
======

Kõnele is an Android app that offers speech-to-text service to other apps.

See <https://code.google.com/p/recognizer-intent/> for

  - more information
  - issues list
  - APK downloads
  - end-user and developer documentation

The latest stable APK is available on
[Google Play](https://play.google.com/store/apps/details?id=ee.ioc.phon.android.speak).


Compilation
-----------

Go into the app-directory and execute

	ant clean release

Note that you need to have 2 additional files that are not part of this
repository:

  - app/local.properties (pointer to the Android SDK)
  - app/speak.keystore (release keys)

Read the Android developer docs for instructions on how to generate them.
