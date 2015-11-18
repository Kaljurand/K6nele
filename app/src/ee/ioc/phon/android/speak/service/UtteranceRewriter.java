package ee.ioc.phon.android.speak.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ee.ioc.phon.android.speak.Log;

public class UtteranceRewriter {

    private final boolean mIsRewrite;

    // Rewrites applied the final result
    private final Map<Pattern, String> REWRITES;

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

    public UtteranceRewriter(String str, boolean isRewrite) {
        mIsRewrite = isRewrite;
        if (mIsRewrite) {
            REWRITES = loadRewrites(str);
            Log.i("Loaded rewrites: " + REWRITES.size());
        } else {
            REWRITES = null;
        }
    }

    public CharSequence applyCommand(String commandsAsString, CharSequence text) {
        for (Triple triple : COMMANDS) {
            text = triple.matchCommand(commandsAsString, text);
        }
        return text;
    }

    private String rewrite(String str) {
        for (Map.Entry<Pattern, String> entry : REWRITES.entrySet()) {
            str = entry.getKey().matcher(str).replaceAll(entry.getValue());
        }
        return str;
    }


    public String rewrite(List<String> results) {
        if (results == null || results.size() < 1) {
            return "";
        }
        if (mIsRewrite) {
            return rewrite(results.get(0));
        }
        return results.get(0);
    }

    // TODO: do the loading at a higher level, not to unnecessarily repeat it
    private Map<Pattern, String> loadRewrites(String str) {
        Map<Pattern, String> rewrites = new HashMap<>();
        for (String line : str.split("\n")) {
            String[] splits = line.split("\t");
            if (splits.length == 2) {
                rewrites.put(Pattern.compile(splits[0]), splits[1].replace("\\n", "\n"));
                Log.i("Loading: " + splits[0] + "==>" + splits[1]);
            }
        }
        return Collections.unmodifiableMap(rewrites);
    }
}