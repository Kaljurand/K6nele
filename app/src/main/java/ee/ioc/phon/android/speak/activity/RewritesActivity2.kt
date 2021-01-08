package ee.ioc.phon.android.speak.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ee.ioc.phon.android.speak.K6neleApplication
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.adapter.RewriteRuleListAdapter
import ee.ioc.phon.android.speak.model.RewriteRule
import ee.ioc.phon.android.speak.model.RewriteRuleViewModel
import ee.ioc.phon.android.speak.model.RewriteRuleViewModelFactory

class RewritesActivity2 : AppCompatActivity() {

    private val newWordActivityRequestCode = 1
    private val wordViewModel: RewriteRuleViewModel by viewModels {
        RewriteRuleViewModelFactory((application as K6neleApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewrites)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = RewriteRuleListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Add an observer on the LiveData returned by getAlphabetizedWords.
        // The onChanged() method fires when the observed data changes and the activity is
        // in the foreground.
        // Was: "owner = this", but it caused "Named arguments not allowed for non-Kotlin functions"
        wordViewModel.allWords.observe(this) { words ->
            // Update the cached copy of the words in the adapter.
            words.let { adapter.submitList(it) }
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@RewritesActivity2, RewriteRuleAddActivity::class.java)
            startActivityForResult(intent, newWordActivityRequestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentData)

        if (requestCode == newWordActivityRequestCode && resultCode == Activity.RESULT_OK) {
            intentData?.getStringExtra(RewriteRuleAddActivity.EXTRA_REPLY)?.let { reply ->
                val word = RewriteRule(reply)
                wordViewModel.insert(word)
            }
        } else {
            Toast.makeText(
                    applicationContext,
                    "empty not saved",
                    Toast.LENGTH_LONG
            ).show()
        }
    }
}