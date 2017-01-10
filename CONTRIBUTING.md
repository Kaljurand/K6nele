Contributing
============

Introduction
------------

Contribute via [GitHub pull requests](https://help.github.com/articles/using-pull-requests/).


Possible tasks
--------------

- support for other languages, see [Issue #38](https://github.com/Kaljurand/K6nele/issues/38)
- optimize UX for Android Wear, Auto, TV, Things, ...
- audio compression
- support for external (e.g. Bluetooth) microphone
- improve the offered intents/extras to better integrate with Tasker and similar apps
- hotword detection, see [VoiceInteractionService](https://developer.android.com/reference/android/service/voice/VoiceInteractionService.html)


Generating icons and diagrams
-----------------------------

Generating PNG launcher icons and diagrams:

    gradle -b other.gradle makeIcons
    gradle -b other.gradle makeDiagrams


Submodules
----------

    git submodule add git@github.com:Kaljurand/speechutils.git speechutils
    git submodule add git@github.com:Kaljurand/net-speech-api.git net-speech-api


Version tags
------------

Version tags are set by e.g.

    git tag -a v1.1.02 -m 'version 1.1.02'

The last number should be even.

See also
--------

- [docs/](docs/)
