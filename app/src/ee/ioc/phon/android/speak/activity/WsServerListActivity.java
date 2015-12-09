/*
 * Copyright 2015, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.speak.activity;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.net.MalformedURLException;

import ee.ioc.phon.android.speak.Executable;
import ee.ioc.phon.android.speak.ExecutableString;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.adapter.WsServerAdapter;
import ee.ioc.phon.android.speak.model.WsServer;
import ee.ioc.phon.android.speak.utils.Utils;
import io.realm.Realm;

/**
 * <p>This activity displays the list of speech recognition server URLs
 * and allows the user to add/edit/remove entries.</p>
 *
 * @author Kaarel Kaljurand
 */
public class WsServerListActivity extends RecognizerIntentListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Realm realm = Realm.getInstance(this);

        // TODO: do this when the app is first installed
        addWsServer(realm, getString(R.string.defaultWsServer));
        addWsServer(realm, "ws://192.168.1.5:8080/client/ws/speech");

        WsServerAdapter mAdapter = new WsServerAdapter(this, 0, realm.where(WsServer.class).findAll(), true);

        ListView lv = getListView();
        setEmptyView(getString(R.string.emptylistServers));
        lv.setAdapter(mAdapter);

        registerForContextMenu(lv);
        setClickToFinish();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.servers, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Realm realm = Realm.getInstance(getApplicationContext());
        switch (item.getItemId()) {
            case R.id.menuServersAdd:
                Utils.getTextEntryDialog(
                        this,
                        getString(R.string.dialogTitleNewServer),
                        "",
                        new ExecutableString() {
                            public void execute(String uri) {
                                addWsServer(realm, uri);
                            }
                        }
                ).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cm_wsserver, menu);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final Realm realm = Realm.getInstance(getApplicationContext());
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String url = "TODO";

        switch (item.getItemId()) {
            case R.id.cmServerEdit:
                Utils.getTextEntryDialog(
                        this,
                        getString(R.string.dialogTitleChangeServer),
                        url,
                        new ExecutableString() {
                            public void execute(String newUrl) {
                                try {
                                    // TODO
                                    toast("Not implemented");
                                    updateUrl(realm, newUrl);
                                } catch (MalformedURLException e) {
                                    toast(getString(R.string.exceptionMalformedUrl));
                                }
                            }
                        }
                ).show();
                return true;
            case R.id.cmServerDelete:
                Utils.getYesNoDialog(
                        this,
                        String.format(getString(R.string.confirmDeleteEntry), url),
                        new Executable() {
                            public void execute() {
                                // TODO
                                toast("Not implemented");
                            }
                        }
                ).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    static void addWsServer(Realm realm, String uri) {
        WsServer obj = new WsServer();
        obj.setUri(uri);
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(obj);
        realm.commitTransaction();
    }
}