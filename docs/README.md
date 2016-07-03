Various documents, notes, examples
----------------------------------

[launch-k6nele.sh](launch-k6nele.sh) is an example of launching Kõnele using `adb shell am`.

[components.dot](components.dot) is a diagram of Kõnele components among Android APIs and apps.

[rewrites.et.tsv](rewrites.et.tsv) is a simple tab-separated file that specifies rewrite rules and commands.
The columns are:

1. Regular expression to match (parts of) the utterance
2. Replacement string for the matched parts
3. Command to be called if the expression fully matches the utterance
4. Argument 1
5. Argument 2
6. ...

Rewriting is done by:

    utterance = java.util.regex.Pattern.compile( COL1 ).matcher(utterance).replaceAll( COL2 )

The pre-defined commands cover cursor movement within the text and between fields, selection, replacement, copy/paste/cut,
and the editor actions `search`, `send`, `go`, and `done`. Most of the commands can be undone.
The arguments can back reference expression groups (`$1`, `$2`, ...) and refer to the current selection by `{}`.
