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
import ee.ioc.phon.android.speak.adapter.RewriteListListAdapter
import ee.ioc.phon.android.speak.model.RewriteList
import ee.ioc.phon.android.speak.model.RewriteListViewModel
import ee.ioc.phon.android.speak.model.RewriteListViewModelFactory

class RewritesSelectorActivity2 : AppCompatActivity() {

    private val vm: RewriteListViewModel by viewModels {
        RewriteListViewModelFactory((application as K6neleApplication).repositoryForList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewrites)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = RewriteListListAdapter(
                { rule -> vm.view(rule) },
                { rule -> vm.delete(rule) }
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
}