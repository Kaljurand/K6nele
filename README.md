Kõnele
======

The source code behind

http://market.android.com/details?id=ee.ioc.phon.android.speak


Compilation
-----------

Go into the app-directory and execute

> ant clean release

Note that you need to have 3 additional files that are not part of this
repository:

  * app/libs/httpmime-4.1.1.jar
  * app/local.properties
  * app/speak.keystore

Read the Android developer docs for instructions on how to generate the
last two files.


Tags
----

Version tags are set by e.g.

	git tag -a v0.8.18 -m 'version 0.8.18'

The last number should be even.


Lint
----

	lint --html report.html app
