#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import division, unicode_literals, print_function

import sys
import re
import json
import argparse
from collections import *
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import urlparse

"""
Simple dialog engine. Expects a single query parameter (q) whose value is a
natural language uttrance. Maps the query to an action. Possibly executes the
action in the local OS. Responds with a JSON that maps one-to-one to an Android
intent, e.g. DEFAULT_RESPONSE (see below) can be used to launch Kõnele to get
answers to possible follow-up questions.

Usage::

    sshpass -p raspberry scp server.py pi@192.168.0.11:/home/pi/
    ssh pi@192.168.0.11
    nohup python server.py 8000 &
    exit
    curl http://192.168.0.11/?q=a natural language sentence

TODO:

- dynamically determine IP on which we are running, this is needed for the callback
- generate REPLACEMENT using json.dumps
- blink RPi lights
- play music using a headless music player (e.g. mpsyt)
- play news using EKI TTS
- parse query using EstNLTK
- maintain dialog state
- send back a multi-line rewrite table (where lines correspond to if-then branches)
- add query param that specifies the input language
- fix UTF8 in response
- deal with latency (e.g. respond immediately)
"""

# If this string is a transcription result (possibly due to rewriting),
# or results from startActivityForResult, then Kõnele launches itself to receive
# a speech input and rewrite it to call FetchUrlActivity, which calls a webservice.
# The webservice might execute a command, update the dialog state, and ask for
# further instructions by returning a similar string as this one."
# EXTRAs that could be added:
# "android.speech.extra.LANGUAGE_MODEL": "free_form",
# "TODO.android.speech.extra.LANGUAGE": "et-EE",
# "TODO.ee.ioc.phon.android.extra.SERVICE_COMPONENT": "",
# EXTRAs to be implemented:
# "ee.ioc.phon.android.extra.TRANSPARENT": True,
# "ee.ioc.phon.android.extra.VOICE_PROMPT": True,
# "ee.ioc.phon.android.extra.USE_REWRITES": False,
DEFAULT_RESPONSE = {
    "component": "ee.ioc.phon.android.speak/.activity.SpeechActionActivity",
    "extras": {
      "android.speech.extra.PROMPT": "",
      "android.speech.extra.MAX_RESULTS": 1,
      "ee.ioc.phon.android.extra.AUTO_START": True,
      "ee.ioc.phon.android.extra.RESULT_UTTERANCE": "(.+)",
      "ee.ioc.phon.android.extra.RESULT_REPLACEMENT": "{\"component\": \"ee.ioc.phon.android.speak/.activity.FetchUrlActivity\", \"data\": \"http://192.168.0.11:8000/?q=$1\"}"
    }
}

RESPONSE_MUSIC = {
    "action": "android.media.action.MEDIA_PLAY_FROM_SEARCH",
    "extras": {
        "android.intent.extra.focus": "vnd.android.cursor.item/*",
        "query": "$1"
    },
    "flags": [4096]
}


class S(BaseHTTPRequestHandler):
    def _set_headers(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/plain')
        self.end_headers()

    def do_GET(self):
        self._set_headers()
        print(self.path)
        o = urlparse.urlparse(self.path)
        d = urlparse.parse_qs(o.query)
        print(d)
        q = d.get('q', None)
        if q:
            m = re.search(u'(?:mängi|laula|play)\s*(.+)', q[0], re.IGNORECASE | re.UNICODE)
            if m:
                RESPONSE_MUSIC['extras']['query'] = m.group(1)
                response_as_str = json.dumps(RESPONSE_MUSIC)
            else:
                DEFAULT_RESPONSE['extras']['android.speech.extra.PROMPT'] = 'Command not supported or understood'
                response_as_str = json.dumps(DEFAULT_RESPONSE)
        else:
            DEFAULT_RESPONSE['extras']['android.speech.extra.PROMPT'] = 'Command missing'
            response_as_str = json.dumps(DEFAULT_RESPONSE)
        print(response_as_str)
        self.wfile.write(response_as_str)

    def do_HEAD(self):
        self._set_headers()

    def do_POST(self):
        self._set_headers()
        self.wfile.write("<html><body><h1>POST!</h1></body></html>")

def run(server_class=HTTPServer, handler_class=S, port=8000):
    print('Starting on port ' + str(port))
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    httpd.serve_forever()

if __name__ == "__main__":
    from sys import argv

    if len(argv) == 2:
        run(port=int(argv[1]))
    else:
        run()
