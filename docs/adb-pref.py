#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This script generates Android Debug Bridge (ADB) commands that change the preferences in a Kõnele instance
via GetPutPreferenceActivity.
Kõnele must be connected to ADB and for "is_url" preferences also connected to the internet.

Usage:

- Enable developer options on the device and connect to the device via adb.
  See: https://developer.android.com/studio/command-line/adb.html
  For Wear, see: https://developer.android.com/training/wearables/apps/debugging.html
- (Optional) Check the Kõnele developer option "Run GetPutPreference without confirmation" to suppress the
  confirmation dialog.
- The input to this script is a list of YAML preference definitions.
- The output of this script should be piped into "sh".
  The progress is printed on the console and shown as "toasts" on the device.

Usage:

    adb-pref.py prefs_developer.yml prefs_user_guide_rewrites.yml prefs_private.yml | sh

"""

from __future__ import division, unicode_literals, print_function

import sys
import argparse
import re
import yaml

DEFAULT_ADB_COMMAND = 'shell'
#DEFAULT_ADB_COMMAND = 'exec-out'

DEFAULT_PREF_DISABLE_CONFIRMATION = {
    'key': 'keyGetPutPrefSkipUi',
    'val': True
}

def get_args():
    """Get command line arguments"""
    parser = argparse.ArgumentParser(description='Converts a YAML list of preferences to ADB calls that set these preferences using GetPutPreferenceActivity.')
    parser.add_argument('fns', metavar='FILE', type=str, nargs='*',
                        help='preference file')
    parser.add_argument('--disable-confirmation', action='store_true', dest='disable_confirmation')
    parser.add_argument('-v', '--version', action='version', version='%(prog)s v0.0.1')
    return parser.parse_args()

def create_adb(pref):
    """Return the preference as an ADB call"""
    def escape_esa(text):
        """Escape for sh.
        TODO: incomplete + should escape for adb as well (e.g. the comma)
        """
        return re.sub(';', '\\;', text)
    def escape_s(text):
        """Escape for sh.
        TODO: incomplete + should escape for adb as well (e.g. the comma)
        """
        text = re.sub('\n', '\\\n', text)
        text = re.sub('\t', '\\\t', text)
        text = re.sub(' ', '\\ ', text)
        return text

    key = escape_s(pref['key'])
    val = pref.get('val')
    if val is None:
        val_str = '--esn val'
    elif type(val) is list:
        val_str = '--esa val "{}"'.format(','.join([escape_esa(text) for text in val]))
    elif type(val) is bool:
        val_str = '--ez val {}'.format(val)
    else:
        val_str = '-e val "{}"'.format(escape_s(val))
        if pref.get('is_url'):
            val_str += ' --ez is_url true'
    return 'adb {0} am start -n ee.ioc.phon.android.speak/.activity.GetPutPreferenceActivity -e key "{1}" {2}'.format(DEFAULT_ADB_COMMAND, key, val_str)


def process(prefs):
    """Output preferences as ADB calls"""
    for pref in prefs:
        print(create_adb(pref))

def main():
    """Main"""
    args = get_args()
    if args.disable_confirmation:
        print(create_adb(DEFAULT_PREF_DISABLE_CONFIRMATION))
    for fn in args.fns:
        with open(fn, 'r') as stream:
            try:
                prefs = yaml.load(stream)
                process(prefs)
            except yaml.YAMLError as exc:
                print(exc, file=sys.stderr)

if __name__ == "__main__":
    main()
