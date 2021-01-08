package ee.ioc.phon.android.speak.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.regex.Pattern;

import ee.ioc.phon.android.speechutils.editor.Command;

@Entity(tableName = "rewrite_rules")
public class RewriteRule {
    @PrimaryKey
    public int id;

    public Pattern utterance;
    public String replacement;

    public RewriteRule() {
    }

    public RewriteRule(String repl) {
        replacement = repl;
    }

    public static RewriteRule fromCommand(int id, Command command) {
        RewriteRule rr = new RewriteRule();
        rr.id = id;
        rr.utterance = command.getUtterance();
        rr.replacement = command.getReplacement();
        return rr;
    }

    /**
     * public Command(String label, String comment, Pattern locale, Pattern service, Pattern app, Pattern utt, String replacement, String id, String[] args)
     *
     * @param rule
     * @return
     */
    public static Command toCommand(RewriteRule rule) {
        return new Command("", "", null, null, null, rule.utterance, rule.replacement, null, null);
    }

    // TODO
}