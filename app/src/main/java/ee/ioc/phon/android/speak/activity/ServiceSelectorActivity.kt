package ee.ioc.phon.android.speak.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import ee.ioc.phon.android.speak.adapter.RecServiceAdapter
import ee.ioc.phon.android.speak.fragment.K6neleListFragment
import ee.ioc.phon.android.speak.model.RecService
import ee.ioc.phon.android.speechutils.RecognitionServiceManager

/**
 * TODO: add a way to view the service settings, via recservice.settingsIntent
 */
class ServiceSelectorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragment = SelectorFragment()
        fragment.arguments = intent.extras
        supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commit()
    }

    class SelectorFragment : K6neleListFragment() {
        override fun onCreate(icicle: Bundle?) {
            super.onCreate(icicle)
            val activity: Activity? = activity
            val mngr = RecognitionServiceManager()
            val list: MutableList<RecService> = ArrayList()
            for (comboAsString in mngr.getServices(activity!!.packageManager)) {
                list.add(RecService(activity, comboAsString))
            }
            val adapter = RecServiceAdapter(this, list)
            listAdapter = adapter
            //getActivity().getActionBar().setSubtitle("" + adapter.getCount());
        }

        override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
            val recservice = l.getItemAtPosition(position) as RecService
            val replyIntent = Intent()
            replyIntent.putExtra(
                EXTRA_COMPONENT_NAME,
                recservice.componentName.flattenToShortString()
            )
            requireActivity().setResult(Activity.RESULT_OK, replyIntent)
            requireActivity().finish()
        }

        /*
        val intent = recservice.settingsIntent
        if (intent == null) {
            toast(getString(R.string.errorRecognizerSettingsNotPresent))
        } else {
            IntentUtils.startActivityWithCatch(requireActivity(), intent)
        }
        */
    }

    companion object {
        const val EXTRA_COMPONENT_NAME = "ee.ioc.phon.android.speak.COMPONENT_NAME"
    }
}