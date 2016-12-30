Various documents, notes, examples
----------------------------------

[launch-k6nele.sh](launch-k6nele.sh) is an example of launching Kõnele using `adb shell am`.

[components.dot](components.dot) is a diagram of Kõnele components among Android APIs and apps.

[intents.dot](intents.dot) is a diagram showing how information can flow from Kõnele to external devices/apps and back.

[Rewrite rule examples](http://kaljurand.github.io/K6nele/docs/et/user_guide.html#nited)
contains (mostly) Estonian rewrite rule tables (documented in English).
A rewrite rule table contains the following tab-separated columns:

- Comment. Free-form comment
- Locale. Regular expression to match the locale of the utterance (e.g. "et")
- Service. Regular expression to match the recognizer service class name
- App. Regular expression to match the app package name
- Utterance. Regular expression to match (parts of) the utterance
- Replacement. String that replaces the matched parts of the utterance
- Command. Command to be called if the expression fully matches the utterance
- Arg1. First argument of the command
- Arg2. Second argument of the command

The first line of the table is a header that names the columns. Only the Utterance and Replacement columns must be present.
Non-header lines can be commented out using an initial '#'.

Rewriting is done by:

    utt = java.util.regex.Pattern.compile( Utterance ).matcher(utt).replaceAll( Replacement )

The pre-defined commands cover cursor movement within the text and between fields, selection, replacement, copy/paste/cut,
and the editor actions `search`, `send`, `go`, and `done`. Most of the commands can be repeated or undone multiple times.
The arguments can reference expression groups by `$1`, `$2`, ... and the current selection by `{}`.
