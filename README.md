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

Point to the SDK directory by setting the environment variable
`ANDROID_HOME`, e.g.

	ANDROID_HOME=${HOME}/myapps/android-studio/sdk/

Then run

	gradle build

For the listing of more Gradle tasks, run:

	gradle tasks


If you have access to the release keystore then

  - point to its location by setting the environment variable `KEYSTORE`
  - set `KEY_ALIAS` to the key alias
  - create the file `gradle.properties` containing the lines:

		storePassword=<password1>
		keyPassword=<password2>
