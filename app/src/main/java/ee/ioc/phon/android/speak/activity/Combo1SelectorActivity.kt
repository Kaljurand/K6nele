package ee.ioc.phon.android.speak.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ee.ioc.phon.android.speak.K6neleApplication
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.adapter.Combo1Adapter
import ee.ioc.phon.android.speak.database.Combo1
import ee.ioc.phon.android.speak.viewmodel.Combo1ViewModel
import ee.ioc.phon.android.speak.viewmodel.Combo1ViewModelFactory

class Combo1SelectorActivity : AppCompatActivity() {

    private val addRequestCode = 1
    private val combo1ViewModel: Combo1ViewModel by viewModels {
        Combo1ViewModelFactory((application as K6neleApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_combo1_list)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = Combo1Adapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        combo1ViewModel.allItems.observe(this, Observer { words ->
            // Update the cached copy of the words in the adapter.
            words?.let { adapter.submitList(it) }
        })

        // TODO: use normal Add menu (like in the rest of the app) instead of the FAB
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@Combo1SelectorActivity, ServiceSelectorActivity::class.java)
            startActivityForResult(intent, addRequestCode)
        }

        // TODO: start Combo1DetailsActivity if list item is clicked
        //val intent = Intent(this@Combo1SelectorActivity, KeyValActivity::class.java)

        // TODO: add reordering
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == addRequestCode && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(ServiceSelectorActivity.EXTRA_COMPONENT_NAME)?.let {
                val word = Combo1(componentName = it)
                combo1ViewModel.insert(word)
            }
        } else {
            Toast.makeText(
                applicationContext, "EMPTY",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}