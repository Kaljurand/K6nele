package ee.ioc.phon.android.speak.service;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class UtteranceRewriter {

    // Rewrites applied the final result
    private final List<Pair<Pattern, String>> mRewrites;

    // TODO: Rewrites applied to the whole text
    private static final Set<Triple> COMMANDS;

    static class Triple {
        private Pattern mCommand;
        private String mIn;
        private String mOut;

        Triple(String command, String in, String out) {
            mCommand = Pattern.compile(command);
            mIn = in;
            mOut = out;
        }

        String matchCommand(String commandsAsString, CharSequence text) {
            Pattern pattern = Pattern.compile(mCommand.matcher(commandsAsString).replaceFirst(mIn));
            return pattern.matcher(text).replaceAll(mOut);
        }
    }

    static {
        Set<Triple> commands = new HashSet<>();
        // TODO: read these from a flat file delivered by a file picker intent
        commands.add(new Triple("[Kk]ustuta (.+)", "$1", ""));
        commands.add(new Triple("[Aa]senda (.+) fraasiga (.+)", "$1", "$2"));
        COMMANDS = Collections.unmodifiableSet(commands);
    }

    public UtteranceRewriter() {
        mRewrites = Collections.emptyList();
    }

    public UtteranceRewriter(List<Pair<Pattern, String>> rewrites) {
        assert rewrites != null;
        mRewrites = rewrites;
    }

    public UtteranceRewriter(String str) {
        this(loadRewrites(str));
    }

    public UtteranceRewriter(ContentResolver contentResolver, Uri uri) throws IOException {
        this(loadRewrites(contentResolver, uri));
    }


    public CharSequence applyCommand(String commandsAsString, CharSequence text) {
        for (Triple triple : COMMANDS) {
            text = triple.matchCommand(commandsAsString, text);
        }
        return text;
    }

    /**
     * @return map of pattern-string rewrite pairs
     */
    public List<Pair<Pattern, String>> getRewrites() {
        return mRewrites;
    }

    /**
     * Rewrites and returns the given string.
     */
    public String rewrite(String str) {
        for (Pair<Pattern, String> entry : mRewrites) {
            str = entry.first.matcher(str).replaceAll(entry.second);
        }
        return str;
    }

    /**
     * Rewrites and returns the first item in the given list, ignores all others.
     */
    public String rewrite(List<String> results) {
        if (results == null || results.size() < 1) {
            return "";
        }
        return rewrite(results.get(0));
    }

    /**
     * Serializes the rewrites as tab-separated-values.
     */
    public String toTsv() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Pair<Pattern, String> entry : mRewrites) {
            stringBuilder.append(escape(entry.first.toString()));
            stringBuilder.append('\t');
            stringBuilder.append(escape(entry.second));
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    public String[] toStringArray() {
        String[] array = new String[mRewrites.size()];
        int i = 0;
        for (Pair<Pattern, String> entry : mRewrites) {
            array[i] = pp(entry.first.toString()) + "\n" + pp(entry.second);
            i++;
        }
        return array;
    }


    /**
     * Loads the rewrites from tab-separated values.
     */
    private static List<Pair<Pattern, String>> loadRewrites(String str) {
        assert str != null;
        List<Pair<Pattern, String>> rewrites = new ArrayList<>();
        for (String line : str.split("\n")) {
            addLine(rewrites, line);
        }
        return Collections.unmodifiableList(rewrites);
    }


    /**
     * Loads the rewrites from an URI using a ContentResolver.
     */
    private static List<Pair<Pattern, String>> loadRewrites(ContentResolver contentResolver, Uri uri) throws IOException {
        InputStream inputStream = contentResolver.openInputStream(uri);
        List<Pair<Pattern, String>> rewrites = new ArrayList<>();
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                addLine(rewrites, line);
            }
            inputStream.close();
        }
        return Collections.unmodifiableList(rewrites);
    }

    private static void addLine(List<Pair<Pattern, String>> rewrites, String line) {
        String[] splits = line.split("\t");
        if (splits.length == 2) {
            try {
                Pair<Pattern, String> pair = new Pair<>(Pattern.compile(unescape(splits[0])), unescape(splits[1]));
                rewrites.add(pair);
            } catch (PatternSyntaxException e) {
                // TODO: collect and expose buggy entries
            }
        }
    }

    /**
     * Maps newlines and tabs to literals of the form "\n" and "\t".
     */
    private static String escape(String str) {
        return str.replace("\n", "\\n").replace("\t", "\\t");
    }

    private static String pp(String str) {
        return escape(str).replace(" ", "·");
    }

    /**
     * Maps literals of the form "\n" and "\t" to newlines and tabs.
     */
    private static String unescape(String str) {
        return str.replace("\\n", "\n").replace("\\t", "\t");
    }
}