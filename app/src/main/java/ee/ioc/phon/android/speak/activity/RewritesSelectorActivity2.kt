package ee.ioc.phon.android.speak.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import ee.ioc.phon.android.speak.K6neleApplication
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.adapter.RewriteListListAdapter
import ee.ioc.phon.android.speak.model.RewriteList
import ee.ioc.phon.android.speak.model.RewriteListViewModel
import ee.ioc.phon.android.speak.model.RewriteListViewModelFactory

// TODO: expandable FAB (new empty table, RewritesLoaderActivity, pick from examples); should work on Wear though
// TODO: context menu for share, delete, etc.
// TODO: publish shortcuts
// TODO: separate selection for Panel vs IME
// TODO: create new table with name, leaving it empty of loading rules into it (from existing, from file manager, from web)
class RewritesSelectorActivity2 : AppCompatActivity() {

    private val vm: RewriteListViewModel by viewModels {
        RewriteListViewModelFactory((application as K6neleApplication).repositoryForList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewrites)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = RewriteListListAdapter(
                { rule -> startView(rule) },
                { rule ->
                    run {
                        vm.delete(rule);
                        Snackbar
                                .make(findViewById<RecyclerView>(R.id.recyclerview), "Deleted: ${rule.name}", Snackbar.LENGTH_LONG)
                                .show()
                    }
                }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        vm.allWords.observe(this) { words ->
            // Update the cached copy of the words in the adapter.
            words.let { adapter.submitList(it) }
        }

        // Add new entry
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@RewritesSelectorActivity2, RewriteRuleAddActivity::class.java)
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentData)

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            intentData?.getStringExtra(RewriteRuleAddActivity.EXTRA_REPLY)?.let { tableName ->
                vm.addEmpty(RewriteList(tableName))
            }
        } else {
            Snackbar
                    .make(findViewById<RecyclerView>(R.id.recyclerview), "Empty not saved", Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.rewrites_selector, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuRewritesAdd -> {
                val intent = Intent(this, RewritesLoaderActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menuRewritesHelp -> {
                val view = Intent(Intent.ACTION_VIEW)
                view.data = Uri.parse(getString(R.string.urlRewritesDoc))
                startActivity(view)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    fun startView(table: RewriteList) {
        val intent = Intent(this@RewritesSelectorActivity2, RewritesActivity2::class.java)
        intent.putExtra(RewritesActivity2.EXTRA_NAME, table.name)
        startActivityForResult(intent, 1)
    }
}