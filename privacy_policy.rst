Privacy policy
==============

Kõnele consists of two independent parts: the end-user interface and two speech recognition services. The end-user interface components (the voice search panel and the input method editor) provide a visual touch interface to allow the user to record the input audio and see its corresponding transcription. The recognition services (the "fast service" and the "grammar-supporting" service) perform the actual recording and transcribing. These two parts are independent in the sense that the user interface components can use any speech service installed on the device, and any app on the device can use the Kõnele-provided speech services. By default, the Kõnele user interface components use Kõnele's services for transcription. However, the user can select a third-party service in the menu "Settings -> Recognition languages & services". If a third-party service is used, then the privacy policy of that service applies.

Kõnele's user interface components require the audio recording permission. The recording is started only when the user presses the microphone button (which stays red for the duration of the recording) or launches Kõnele with "Auto start" enabled. The recording ends after a longer pause in the input audio, after a set time period, or after pressing the microphone button again, depending on the settings. The Kõnele services can be configured to signal the beginning and end of the recording with audio cues.

In order to transcribe the audio, both Kõnele services require network access to a speech recognition server. The network connection is not encrypted. The connection is established when the recording is started and closed when the recording is stopped and the final transcription has been received.

Each Kõnele service has a single corresponding recognition server, whose web address is visible and changeable in the Kõnele settings. The servers run independently of Kõnele and are covered by their own privacy policy. The privacy policy of the default servers is available at http://phon.ioc.ee. The default servers are based on free and open source software (available at https://github.com/alumae/kaldi-gstreamer-server and https://github.com/alumae/ruby-pocketsphinx-server) allowing the user to install them in a local private network.

The outgoing data (sent via a Kõnele service to the server) consists of:

- the audio stream,
- the desired number of transcription hypotheses,
- the grammar URL (in case of grammar-based speech recognition),
- the Kõnele version number,
- the Kõnele installation ID,
- the device manufacturer and type (e.g. "LGE bullhead"),
- the Android build number,
- the package name of the app that calls Kõnele (e.g. "com.google.android.keep"),
- the text field type and the cursor position (e.g. cursor position 2 in a phone number field) (applies only with the input method editor).

The installation ID is a globally unique number (e.g. "89b4cobb-t82c-4029-b891-1231b049lcc8") that is automatically generated when Kõnele is first installed. The server can use it to relate recognition sessions coming from the same Kõnele instance. This number cannot be used to identify the user, device, installation time, nor any other personal information.

The incoming data (sent by the server to a Kõnele service) must minimally include the transcription hypotheses. Other possible components (ignored by Kõnele) are covered by the server privacy policy.

Apart from allowing the use of third-party speech recognition services and servers as described above, Kõnele does not collect nor share any user data. The source code of all Kõnele components and their dependencies is open and available at https://github.com/Kaljurand/K6nele.
