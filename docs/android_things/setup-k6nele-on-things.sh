#!/bin/bash

# Downloads the needed APKs (using wget), installs them to the current device (using adb),
# and configures them (using adb). The configuring consists of permission assignment
# and running Kõnele's GetPutPreferenceActivity to change the Kõnele settings.
# Work in progress (relies on an unreleased version of Kõnele).

cmd=adb
basedir=$(mktemp -d)

ver_k6nele=1.6.98
ver_ekispeak=1.2.03
ver_trigger=0.1.21
ver_launcher=0.0.2

fakedir=$HOME/Desktop/APK/

function download {
    echo "wget -P $basedir $1"
    wget -P $basedir $1
}

$cmd connect Android.local

echo "Installing Kõnele..."
download https://github.com/Kaljurand/K6nele/releases/download/v${ver_k6nele}/K6nele-${ver_k6nele}.apk
##cp $fakedir/K6nele-${ver_k6nele}.apk $basedir
$cmd install $basedir/K6nele-${ver_k6nele}.apk
$cmd shell pm grant ee.ioc.phon.android.speak android.permission.RECORD_AUDIO

echo "Installing Speech Trigger..."
download https://github.com/Kaljurand/speech-trigger/releases/download/v${ver_trigger}/SpeechTrigger-${ver_trigger}.apk
$cmd install $basedir/SpeechTrigger-${ver_trigger}.apk
$cmd shell pm grant ee.ioc.phon.android.speechtrigger android.permission.RECORD_AUDIO
$cmd shell pm grant ee.ioc.phon.android.speechtrigger android.permission.WRITE_EXTERNAL_STORAGE

echo "Installing EKI Speak..."
download https://github.com/Kaljurand/EKISpeak/releases/download/v${ver_ekispeak}/EKISpeak-${ver_ekispeak}.apk
$cmd install $basedir/EKISpeak-${ver_ekispeak}.apk

echo "Installing Things Kõnele Launcher..."
download https://github.com/Kaljurand/things-k6nelelauncher/releases/download/v${ver_launcher}/things-k6nelelauncher-${ver_launcher}.apk
$cmd install $basedir/things-k6nelelauncher-${ver_launcher}.apk
$cmd shell pm grant ee.ioc.phon.android.things.k6nelelauncher com.google.android.things.permission.USE_PERIPHERAL_IO

echo "Installed the following APKs:"
ls -l $basedir
$cmd shell 'pm list packages'

echo "Installation done. Reboot using: adb shell reboot"
echo "or start the app by: adb shell am start -n ee.ioc.phon.android.things.k6nelelauncher/.ActivityLauncherActivity"
