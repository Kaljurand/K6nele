package ee.ioc.phon.android.speak.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ee.ioc.phon.android.speechutils.editor.Command
import java.util.regex.Pattern

// TODO: review mapping of arg1, arg2 to/from array
// TODO: add timestamp, frequency (or maybe support these in a separate table)
// TODO: is it OK to use var for columns?
@Entity(tableName = "rewrite_rules")
data class RewriteRule(
        var ownerId: Long,
        @ColumnInfo(name = "app") val app: Pattern?,
        @ColumnInfo(name = "locale") val locale: Pattern?,
        @ColumnInfo(name = "service") val service: Pattern?,
        @ColumnInfo(name = "utterance") val utterance: Pattern?,
        @ColumnInfo(name = "replacement") val replacement: String?,
        @ColumnInfo(name = "command") val command: String?,
        @ColumnInfo(name = "arg1") val arg1: String?,
        @ColumnInfo(name = "arg2") val arg2: String?,
        @ColumnInfo(name = "comment") val comment: String?,
        @ColumnInfo(name = "label") val label: String?,
        @ColumnInfo(name = "rank") val rank: Int,
) {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    companion object {

        fun fromCommand(ownerId: Long, rank: Int, command: Command): RewriteRule {
            return RewriteRule(
                    ownerId,
                    command.app,
                    command.locale,
                    command.service,
                    command.utterance,
                    command.replacement,
                    command.id,
                    command.args.getOrNull(0),
                    command.args.getOrNull(1),
                    command.comment,
                    command.label,
                    rank,
            )
        }

        /**
         * @param rule
         * @return Command(String label, String comment, Pattern locale, Pattern service, Pattern app, Pattern utt, String replacement, String id, String[] args)
         */
        fun toCommand(rule: RewriteRule): Command {
            return Command(
                    rule.label,
                    rule.comment,
                    rule.locale,
                    rule.service,
                    rule.app,
                    rule.utterance,
                    rule.replacement,
                    rule.command,
                    arrayOf(rule.arg1, rule.arg2)
            )
        }
    }
}