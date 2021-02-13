package ee.ioc.phon.android.speak.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import ee.ioc.phon.android.speak.K6neleApplication
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.adapter.RewriteRuleListAdapter
import ee.ioc.phon.android.speak.model.RewriteRule
import ee.ioc.phon.android.speak.model.RewriteRuleViewModel
import ee.ioc.phon.android.speak.model.RewriteRuleViewModelFactory
import java.util.regex.Pattern

// Replaces RewritesActivity
// TODO: single press to open in a details view (that allows editing)
// Long-press to delete
// TODO: filtering by every field; initial search values loaded from extras
// TODO: share menus
// TODO: make it possible to select multiple rows to convert them to a new table,
// or insert to the beginning or end of an existing table
class RewritesActivity2 : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME = "EXTRA_NAME"
        const val EXTRA_LOCALE = "EXTRA_LOCALE"
        const val EXTRA_APP = "EXTRA_APP"
        const val EXTRA_SERVICE = "EXTRA_SERVICE"
    }

    private val newWordActivityRequestCode = 1
    private val wordViewModel: RewriteRuleViewModel by viewModels {
        RewriteRuleViewModelFactory((application as K6neleApplication).repository)
    }
    private var tableName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewrites)

        tableName = intent.getStringExtra(EXTRA_NAME).orEmpty()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = RewriteRuleListAdapter(
                {},
                //{ rule -> wordViewModel.incFreq(rule) },
                { rule ->
                    Snackbar
                            .make(findViewById<RecyclerView>(R.id.recyclerview), rule.toString(), Snackbar.LENGTH_LONG)
                            .show()
                }
                //{ rule -> wordViewModel.delete(rule) },
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Add an observer on the LiveData returned by getAlphabetizedWords.
        // The onChanged() method fires when the observed data changes and the activity is
        // in the foreground.
        // Was: "owner = this", but it caused "Named arguments not allowed for non-Kotlin functions"
        /*
        wordViewModel.allWords.observe(this) { words ->
            // Update the cached copy of the words in the adapter.
            words.let { adapter.submitList(it) }
        }
        */

        wordViewModel.rulesByOwnerName(tableName).observe(this) { words ->
            // Update the cached copy of the words in the adapter.
            words.let { adapter.submitList(it) }
        }

        // Add new entry
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@RewritesActivity2, RewriteRuleAddActivity::class.java)
            startActivityForResult(intent, newWordActivityRequestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentData)

        if (requestCode == newWordActivityRequestCode && resultCode == Activity.RESULT_OK) {
            intentData?.getStringExtra(RewriteRuleAddActivity.EXTRA_REPLY)?.let { label ->
                val rewriteRule = RewriteRule(
                        0,
                        Pattern.compile("myapp3"),
                        Pattern.compile("et-EE"),
                        Pattern.compile("K6neleService"),
                        Pattern.compile("(.+)"),
                        "repl",
                        "replace", "$1", "$2",
                        "This is a comment",
                        label,
                        // TODO: should be one larger than the largest rank number (for this owner)
                        0,
                )
                wordViewModel.addNewRule(tableName, rewriteRule)
            }
        } else {
            Snackbar
                    .make(findViewById<RecyclerView>(R.id.recyclerview), "Empty not saved", Snackbar.LENGTH_LONG)
                    .show()
        }
    }
}