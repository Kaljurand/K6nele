Various documents, notes, examples
----------------------------------

[launch-k6nele.sh](launch-k6nele.sh) is an example of launching Kõnele using `adb shell am`.

[components.dot](components.dot) is a diagram of Kõnele components among Android APIs and apps.

[rewrites.tsv](rewrites.tsv) is a sample file that specifies (mostly) Estonian rewrite rules and commands, and conditions under which they should be applied.
The file contains the following tab-separated columns:

1. Comment. Free-form comment
2. Locale. Locale of the utterance (e.g. "et")
3. Service. Regular expression to match the recognizer service class name
4. App. Regular expression to match the app package name
5. Utterance. Regular expression to match (parts of) the utterance
6. Replacement. String that replaces the matched parts of the utterance
7. Command. Command to be called if the expression fully matches the utterance
8. Arg1. First argument of the command
9. Arg2. Second argument of the command

The first line of the table is a header that names the columns. Not all columns must be present.
Non-header lines can be commented out using an initial '#'.

Rewriting is done by:

    utt = java.util.regex.Pattern.compile( Utterance ).matcher(utt).replaceAll( Replacement )

The pre-defined commands cover cursor movement within the text and between fields, selection, replacement, copy/paste/cut,
and the editor actions `search`, `send`, `go`, and `done`. Most of the commands can be repeated or undone multiple times.
The arguments can back-reference expression groups (`$1`, `$2`, ...) and refer to the current selection by `{}`.
