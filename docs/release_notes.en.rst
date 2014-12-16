Release notes
=============

v0.8.5x
-------

1. Added a new speech recognizer service that uses the continuous dictation server
(https://github.com/alumae/kaldi-gstreamer-server), offering faster and more accurate
transcription results (but no grammar support).
If set to default in ``Settings -> Language & input -> Speech -> Voice input`` then this
service can be used e.g. with Google Translate (for Estonian voice input).

2. Added support for input method editor (IME), which offers a one-button keyboard:

- press the button to start/stop/cancel speech recognition
- swipe left to delete the word left from cursor
- swipe right to add a newline
- double tap to insert a space
- press the keyboard icon to switch to the next/last keyboard
- long press the keyboard icon to open a keyboard switcher
- press the search icon to launch the search (if editing a search bar)

The IME must be first enabled in
``Settings -> Language & input -> Keyboard & input methods``.
Note that it uses the new service by default but can be configured in the
Kõnele settings to use any speech recognizer service available on the phone
(although it has been tested only with Kõnele's own services).
