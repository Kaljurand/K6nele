package ee.ioc.phon.android.speak;

import android.app.Activity;
import android.os.Bundle;

public class ComboSelectorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComboSelectorFragment details = new ComboSelectorFragment();
        details.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
    }
}