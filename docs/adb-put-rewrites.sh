#!/bin/bash

# This script uses the Android Debug Bridge (adb) to populate a Kõnele instance with a set of rewrite rule tables.
# Kõnele must be connected to adb and also connected to the internet.

# Usage:
#
# 0. Customize this script (all the lines that start with "add", "make_visible", "make_default")
# 1. Enable developer options on the device and connect to the device via adb.
#    See: https://developer.android.com/studio/command-line/adb.html
#    For Wear, see: https://developer.android.com/training/wearables/apps/debugging.html
# 2. Run this script. You are first required to enable automatic preference changes in the Kõnele settings.
#    The progress is printed on the console and shown as "toasts" on the device
# 3. Review the rewrite rules on the device, e.g. by running
#    adb shell am start -n ee.ioc.phon.android.speak/.activity.RewritesSelectorActivity

# General helper functions (do not need to be changed)
function add {
    name=$1
    url=$2

    echo "Adding $name from $url"
    adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyRewritesMap/$name -e val "$url" --ez is_url true
}

function make_visible {
    echo "Making visible: $1"
    adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyRewritesMap --esa val $1
}

function make_default {
    echo "Making default: $1"
    adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key defaultRewriteTables --esa val $1
}

echo "Enable the Kõnele setting \"Run GetPutPreference without confirmation\" and then press Enter."
echo "To enable this setting via ADB, run:"
echo "adb shell am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key keyGetPutPrefSkipUi --ez val true"
echo "and press OK on the device."
echo "It is recommended to disable this setting again after this script has finished."
read

# Downloading and naming several rewrite tables
# add <name of the table> <url of the table>

# add 01Base "TODO_CUSTOMIZE"
# add 02Act "TODO_CUSTOMIZE"
# add 03Ime "TODO_CUSTOMIZE"

# Demo application for composing and sending notes
add Send "http://kaljurand.github.io/K6nele/rewrites/tsv/k6_skill_send.tsv"

# Demo application for controlling Hue lights
add Lights "https://docs.google.com/spreadsheets/d/1ZAlBIZniTNorGn8U_WwOxNURT9NlyiGfzjGslIbNx2k/export?format=tsv"
add Lights.Hue "https://docs.google.com/spreadsheets/d/1owXRMDRIGvi4Ya0lP6_LXsbZXs-sslwhzEye5pGAXbo/export?format=tsv"

# Making them all visible in Kõnele's RewritesSelectorActivity.
# This step can be skipped if you never need to launch this activity, e.g. on a displayless Android Things.
# The order of these comma separated values does not matter.
make_visible "01Base,02Act,03Ime,Send,Lights,Lights.Hue"

# Making some of them default.
# All tables cannot be default at the same time because they effectively define
# different applications, which can be in conflict with each other.
# This step can be skipped if you always override the default rewrite tables using input EXTRAs when launching Kõnele.
# The order of these comma separated values does not matter.
# Note that the execution order is alphabetical by name.
#make_default "01Base,02Act,03Ime"
#make_default "01Base,Send"
make_default "Lights,Lights.Hue"
