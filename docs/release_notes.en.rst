Release notes
=============

v0.8.xx
-------

Added support for input method editor (IME), which offers a one-button keyboard:

- press the button to start/stop speech recognition
- swipe left to delete the word left from cursor
- swipe right to add a newline
- long press on the text under the button to insert a space
- press the keyboard icon to switch to the next/last keyboard
- long press on the keyboard icon to open a keyboard switcher
- press the search icon to launch the search (if editing the search bar)

The IME can be enabled in "Settings -> Language & input -> Keyboard & input methods".
It can be used with any speech recognizer service installed on the phone (although
note that it has been tested only with Kõnele's own services).

Added a new speech recognizer service that uses the continuous dictation server
(https://github.com/alumae/kaldi-gstreamer-server), offering faster and more accurate
transcription results (but no grammar support). The new IME uses this service by default.
If set to default in "Settings -> Language & input -> Speech -> Voice input" then this
service can be used e.g. with Google Translate (for Estonian voice input).
