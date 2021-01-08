package ee.ioc.phon.android.speak.model

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.speech.RecognizerIntent
import android.text.SpannableStringBuilder
import android.util.Base64
import androidx.room.Room
import ee.ioc.phon.android.speak.AppDatabase
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.activity.RewritesActivity
import ee.ioc.phon.android.speechutils.Extras
import ee.ioc.phon.android.speechutils.editor.Command
import ee.ioc.phon.android.speechutils.editor.CommandMatcher
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter.CommandHolder
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils

class RoomRewrites(context: Context, private val mPrefs: SharedPreferences, private val mRes: Resources, val id: String) {
    private val mDao: RewriteRuleDao
    override fun toString(): String {
        return id
    }

    var isSelected: Boolean
        get() = defaults.contains(id)
        set(b) {
            val set: MutableSet<String> = HashSet(defaults)
            if (set.contains(id)) {
                if (!b) {
                    set.remove(id)
                    putDefaults(set)
                }
            } else {
                if (b) {
                    set.add(id)
                    putDefaults(set)
                }
            }
        }
    val k6neleIntent: Intent
        get() {
            val intent = Intent()
            intent.setClassName("ee.ioc.phon.android.speak", "ee.ioc.phon.android.speak.activity.SpeechActionActivity")
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, id)
            intent.putExtra(Extras.EXTRA_AUTO_START, true)
            intent.putExtra(Extras.EXTRA_RESULT_REWRITES, arrayOf(id))
            return intent
        }
    val showIntent: Intent
        get() {
            val intent = Intent()
            intent.setClassName("ee.ioc.phon.android.speak", "ee.ioc.phon.android.speak.activity.RewritesActivity")
            intent.putExtra(RewritesActivity.EXTRA_NAME, id)
            return intent
        }

    // EXTRA_TITLE is shown in Android Q
    val sendIntent: Intent
        get() {
            val ur = UtteranceRewriter(rewrites)
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_SUBJECT, id)
            intent.putExtra(Intent.EXTRA_TEXT, ur.toTsv())
            // EXTRA_TITLE is shown in Android Q
            intent.putExtra(Intent.EXTRA_TITLE, mRes.getString(R.string.labelRewritesShare))
            intent.type = "text/tab-separated-values"
            return intent
        }
    val intentSendBase64: Intent
        get() {
            val ur = UtteranceRewriter(rewrites)
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_SUBJECT, id)
            intent.putExtra(Intent.EXTRA_TEXT, "k6://" + Base64.encodeToString(ur.toTsv().toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE))
            intent.type = "text/plain"
            return intent
        }

    fun getRules(commandMatcher: CommandMatcher?): Array<SpannableStringBuilder?> {
        val holder = getCommandHolder(commandMatcher)
        val header: Collection<String> = holder.header.values
        val array = arrayOfNulls<SpannableStringBuilder>(holder.size())
        var i = 0
        for (command in holder.commands) {
            array[i++] = pp(command.toMap(header))
        }
        return array
    }

    /**
     * Saves the rewrites under a new name. If the new name equals the old name, then nothing is done.
     * If the new name is null then the rewrites are deleted.
     *
     * @param newName New name of the rewrites or null if the rewrites should be deleted.
     */
    fun rename(newName: String?) {
        if (id != newName) {
            if (newName != null) {
                PreferenceUtils.putPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, newName, rewrites)
            }
            val deleteKeys: MutableSet<String> = HashSet()
            deleteKeys.add(id)
            PreferenceUtils.clearPrefMap(mPrefs, mRes, R.string.keyRewritesMap, deleteKeys)
            // Replace the name in the defaults
            val defaults: MutableSet<String> = HashSet(defaults)
            if (defaults.remove(id)) {
                if (newName != null) {
                    defaults.add(newName)
                }
                putDefaults(defaults)
            }
        }
    }

    fun delete() {
        rename(null)
    }

    // TODO: convert to string
    //List<RewriteRule> rules = dao.getAll();
    val rewrites: String
        get() =// TODO: convert to string
                //List<RewriteRule> rules = dao.getAll();
            PreferenceUtils.getPrefMapEntry(mPrefs, mRes, R.string.keyRewritesMap, id)

    private fun getCommandHolder(matcher: CommandMatcher?): CommandHolder {
        val ur = UtteranceRewriter(rewrites, matcher)
        return ur.commandHolder
        //return new GetAllAsyncTask(mDao).execute();
    }

    private val defaults: Set<String>
        private get() = PreferenceUtils.getPrefStringSet(mPrefs, mRes, R.string.defaultRewriteTables)

    private fun putDefaults(set: Set<String>) {
        PreferenceUtils.putPrefStringSet(mPrefs, mRes, R.string.defaultRewriteTables, set)
    }

    /*
    public static List<RoomRewrites> getTables(SharedPreferences prefs, Resources res) {
        List<String> rewritesIds = new ArrayList<>(PreferenceUtils.getPrefMapKeys(prefs, res, R.string.keyRewritesMap));
        List<RoomRewrites> rewritesTables = new ArrayList<>();
        for (String id : rewritesIds) {
            rewritesTables.add(new RoomRewrites(prefs, res, id));
        }
        Collections.sort(rewritesTables, SORT_BY_ID);
        return rewritesTables;
    }
    */
    /*
    private static class GetAllAsyncTask extends AsyncTask<Void, Void, List<RewriteRule>> {

        private RewriteRuleDao mDao;

        GetAllAsyncTask(RewriteRuleDao dao) {
            mDao = dao;
        }

        protected List<RewriteRule> doInBackground(Void... params) {
            return mDao.getAll();
        }
    }
     */
    private class SortById : Comparator<Any?> {
        override fun compare(o1: Any?, o2: Any?): Int {
            val c1 = o1 as RoomRewrites?
            val c2 = o2 as RoomRewrites?
            return c1!!.id.compareTo(c2!!.id, ignoreCase = true)
        }
    }

    companion object {
        private val SORT_BY_ID: Comparator<*> = SortById()

        /**
         * Loads the rewrites from a string of tab-separated values,
         * guessing the header from the string itself.
         */
        private fun loadRewrites(rules: List<RewriteRule>, commandMatcher: CommandMatcher): CommandHolder {
            val commands: MutableList<Command> = ArrayList()
            for (rule in rules) {
                // TODO: add command matching
                commands.add(RewriteRule.toCommand(rule))
            }
            return CommandHolder(UtteranceRewriter.DEFAULT_HEADER, commands)
        }

        @JvmStatic
        fun ppComboMatcher(app: String?, locale: String?, service: String?): String {
            // The middot is between U+202F (NARROW NO-BREAK SPACE)
            return toPp(app) + " • " + toPp(locale) + " • " + toPp(service)
        }

        fun getDefaults(prefs: SharedPreferences?, res: Resources?): Set<String> {
            return PreferenceUtils.getPrefStringSet(prefs, res, R.string.defaultRewriteTables)
        }

        /**
         * Pretty-print the rule, assuming that space-character sequences actually stand
         * for newline (single space) followed by tab (two spaces) sequences.
         * Sequence of 1 or 2 spaces is kept as it is.
         *
         *
         * TODO: map it to a layout file instead of using spans (?)
         *
         * @param map Mapping of rule component names to the corresponding components
         * @return pretty-printed rule
         */
        private fun pp(map: Map<String, String>): SpannableStringBuilder {
            val ssb = SpannableStringBuilder()
            // Matcher with its 3 constraints
            ssb.append(ppMatcher(map))
            ssb.append('\n')
            // Utterance
            // int start = ssb.length();
            ssb.append(map[UtteranceRewriter.HEADER_UTTERANCE])
            // Use a layout file to make it easier to style the output
            // ssb.setSpan(new ForegroundColorSpan(0xffFAFAFA), start, ssb.length(), 0);
            ssb.append('\n')
            // Replacement
            ssb.append(toPp(map[UtteranceRewriter.HEADER_REPLACEMENT])
                    .replace("         ", "\n\t\t\t\t")
                    .replace("       ", "\n\t\t\t")
                    .replace("     ", "\n\t\t")
                    .replace("   ", "\n\t"))
            // Command with arguments
            ssb.append(ppCommand(map))
            // Label
            val label = map[UtteranceRewriter.HEADER_LABEL]
            if (label != null && !label.isEmpty()) {
                ssb.append("\n\n")
                ssb.append(label)
            }
            // Comment
            val comment = map[UtteranceRewriter.HEADER_COMMENT]
            if (comment != null && !comment.isEmpty()) {
                ssb.append("\n\n")
                // start = ssb.length();
                ssb.append(comment)
                // ssb.setSpan(new StyleSpan(Typeface.ITALIC), start, ssb.length(), 0);
            }
            return ssb
        }

        private fun ppCommand(map: Map<String, String>): String {
            val id = map[UtteranceRewriter.HEADER_COMMAND]
            if (id == null || id.isEmpty()) {
                return ""
            }
            val sb = StringBuilder()
            sb.append('\n')
            sb.append(id)
            val arg1 = toPp(map[UtteranceRewriter.HEADER_ARG1])
            if (!arg1.isEmpty()) {
                sb.append('\n')
                sb.append(' ')
                sb.append('(')
                sb.append(arg1)
                sb.append(')')
                val arg2 = toPp(map[UtteranceRewriter.HEADER_ARG2])
                if (!arg2.isEmpty()) {
                    sb.append('\n')
                    sb.append(' ')
                    sb.append('(')
                    sb.append(arg2)
                    sb.append(')')
                }
            }
            return sb.toString()
        }

        private fun ppMatcher(map: Map<String, String>): String {
            return ppComboMatcher(map[UtteranceRewriter.HEADER_APP], map[UtteranceRewriter.HEADER_LOCALE], map[UtteranceRewriter.HEADER_SERVICE])
        }

        private fun toPp(str: String?): String {
            return str ?: ""
        }
    }

    init {
        val db = Room.databaseBuilder(context,
                AppDatabase::class.java, id).build()
        mDao = db.rewriteRuleDao()
    }
}