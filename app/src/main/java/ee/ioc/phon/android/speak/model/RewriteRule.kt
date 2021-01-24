package ee.ioc.phon.android.speak.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ee.ioc.phon.android.speechutils.editor.Command
import java.util.regex.Pattern

@Entity(tableName = "rewrite_rules")
data class RewriteRule(
        val ownerId: Int,
        @ColumnInfo(name = "utterance") val utterance: Pattern?,
        @ColumnInfo(name = "replacement") val replacement: String?) {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    val utteranceAsStr: String?
        get() = utterance?.pattern()

    companion object {
        fun fromCommand(command: Command): RewriteRule {
            return RewriteRule(1, command.utterance, command.replacement)
        }

        /**
         * public Command(String label, String comment, Pattern locale, Pattern service, Pattern app, Pattern utt, String replacement, String id, String[] args)
         *
         * @param rule
         * @return
         */
        fun toCommand(rule: RewriteRule): Command {
            return Command("", "", null, null, null, rule.utterance, rule.replacement, null, null)
        } // TODO
    }
}