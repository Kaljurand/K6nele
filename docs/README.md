Various documents, notes, examples
----------------------------------

[launch-k6nele.sh](launch-k6nele.sh) is an example of launching Kõnele using `adb shell am`.

[components.dot](components.dot) is a diagram of Kõnele components among Android APIs and apps.

[rewrites.et.tsv](rewrites.et.tsv) is a simple 2-column file that specifies rewrite rules. Applied as

    text = java.util.regex.Pattern.compile( COL1 ).matcher(text).replaceAll( COL2 )
