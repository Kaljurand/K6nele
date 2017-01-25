#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import division, unicode_literals, print_function

import sys
import re
import json
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import urlparse

"""
Simple dialog engine.
Expects a single query parameter (q) whose value is a natural language uttrance.
Responds with a JSON that describes an Android intent, e.g. RESPONSE_DEFAULT
can be used to launch Kõnele to get answers to possible follow-up questions.

Usage::

    nohup python server.py 8000 &
    curl http://192.168.1.5:8000/?q=play+Gangnam+Style

"""

# Response in the case the query was "play <name>"
RESPONSE_MUSIC = {
    "action": "android.media.action.MEDIA_PLAY_FROM_SEARCH",
    "extras": {
        "android.intent.extra.focus": "vnd.android.cursor.item/*",
        "query": "$1"
    },
    "flags": [4096]
}

# Response in all the other cases
RESPONSE_DEFAULT = {
    "component": "ee.ioc.phon.android.speak/.activity.SpeechActionActivity",
    "extras": {
        "ee.ioc.phon.android.extra.VOICE_PROMPT": "Say for example: play Gangnam Style.",
        "android.speech.extra.PROMPT": "Say: play <name of song or artist>",
        "android.speech.extra.MAX_RESULTS": 1,
        "android.speech.extra.LANGUAGE": "en",
        "ee.ioc.phon.android.extra.AUTO_START": True,
        "ee.ioc.phon.android.extra.RESULT_UTTERANCE": "(.+)",
        "ee.ioc.phon.android.extra.RESULT_COMMAND": "activity",
        "ee.ioc.phon.android.extra.RESULT_ARG1": json.dumps({
            "component": "ee.ioc.phon.android.speak/.activity.FetchUrlActivity",
            "data": "http://192.168.1.5:8000/?q=$1",
            "extras": {"ee.ioc.phon.android.extra.RESULT_LAUNCH_AS_ACTIVITY": True}
        })
    }
}


class S(BaseHTTPRequestHandler):
    def _set_headers(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/plain')
        self.end_headers()

    def do_GET(self):
        self._set_headers()
        o = urlparse.urlparse(self.path)
        d = urlparse.parse_qs(o.query)
        q = d.get('q', None)
        if q:
            m = re.search(u'(?:mängi|laula|play)\s*(.+)', q[0].decode('utf8'), re.IGNORECASE | re.UNICODE)
            if m:
                RESPONSE_MUSIC['extras']['query'] = m.group(1)
                self.wfile.write(json.dumps(RESPONSE_MUSIC))
                return
        self.wfile.write(json.dumps(RESPONSE_DEFAULT))


def run(server_class=HTTPServer, handler_class=S, port=8000):
    print('Starting on port ' + str(port))
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    httpd.serve_forever()

if __name__ == '__main__':
    from sys import argv

    if len(argv) == 2:
        run(port=int(argv[1]))
    else:
        run()
