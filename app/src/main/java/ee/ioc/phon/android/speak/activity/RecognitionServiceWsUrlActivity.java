package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

// TODO: close socket on BACK or if new socket is opened
public class RecognitionServiceWsUrlActivity extends Activity {

    private List<String> mList = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private Button mBScan;
    private TextView mTvServerStatus;
    private EditText mEtUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recognition_service_ws_url);

        mEtUrl = findViewById(R.id.etWsServerUrl);
        mEtUrl.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String serverUri = mEtUrl.getText().toString();
                    String baseUri = serverUri.substring(0, serverUri.lastIndexOf('/') + 1);
                    setUrl(baseUri);
                }
                return false;
            }
        });

        mTvServerStatus = findViewById(R.id.tvServerStatus);

        final ListView lvResults = findViewById(R.id.lvRewrites);

        lvResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String repl = lvResults.getItemAtPosition(position).toString();
                setUrl("ws://192.168.0." + repl + ":8080/client/ws/");
            }

        });

        mAdapter = new ArrayAdapter<>(RecognitionServiceWsUrlActivity.this,
                android.R.layout.simple_list_item_1, android.R.id.text1, mList);
        lvResults.setAdapter(mAdapter);

        findViewById(R.id.bWsServerDefault1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUrl("ws://bark.phon.ioc.ee:82/dev/duplex-speech-api/ws/");
            }
        });

        findViewById(R.id.bWsServerDefault2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUrl("wss://bark.phon.ioc.ee:8443/dev/duplex-speech-api/ws/");
            }
        });

        mBScan = findViewById(R.id.bScanNetwork);
        mBScan.setEnabled(true);
        mBScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                new Scan().execute("192.168.0.0");
            }
        });

        findViewById(R.id.bApplyUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setData(Uri.parse(mEtUrl.getText().toString()));
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String serverUri = PreferenceUtils.getPrefString(prefs, getResources(), R.string.keyWsServer);
        String baseUri = serverUri.substring(0, serverUri.lastIndexOf('/') + 1);
        setUrl(baseUri);
    }

    private void setUrl(String url) {
        if (mEtUrl != null) {
            mEtUrl.setText(url + "speech");
            setSummaryWithStatus(url + "status");
        }
    }

    private class Scan extends AsyncTask<String, Pair<Integer, Boolean>, List<String>> {
        protected List<String> doInBackground(String... ips) {
            List<String> results = new ArrayList<>();
            for (String ip : ips) {
                String base = ip.substring(0, ip.lastIndexOf('.') + 1);
                // Escape early if cancel() is called
                if (isCancelled()) break;
                try {
                    NetworkInterface iFace = NetworkInterface
                            .getByInetAddress(InetAddress.getByName(ip));

                    for (int i = 2; i <= 255; i++) {
                        InetAddress pingAddr = InetAddress.getByName(base + i);

                        // 50ms Timeout for the "ping"
                        if (pingAddr.isReachable(iFace, 200, 50)) {
                            String result = pingAddr.getHostAddress();
                            results.add(result);
                            publishProgress(new Pair<>(i, true));
                        } else {
                            publishProgress(new Pair<>(i, false));
                        }
                    }
                } catch (UnknownHostException ex) {
                    Log.e(ex.toString());
                    break;
                } catch (IOException ex) {
                    Log.e(ex.toString());
                    break;
                }
            }
            return results;
        }

        protected void onProgressUpdate(Pair<Integer, Boolean>... progress) {
            Pair<Integer, Boolean> pair = progress[0];
            mBScan.setText(pair.first + "/255");
            if (pair.second) {
                mList.add(pair.first + "");
                mAdapter.notifyDataSetChanged();
                Log.i("FOUND: " + pair.first);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mList.clear();
            mBScan.setEnabled(false);
            /*
            progressDialog = new ProgressDialog(QuickSettingsActivity.this);
            progressDialog.setCancelable(true);
            progressDialog.setMessage("Loading...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setProgress(0);
            progressDialog.show();
            */
        }

        protected void onPostExecute(List<String> results) {
            Log.i(results.toString());
            mBScan.setEnabled(true);
            mBScan.setText("Scan");
        }
    }

    private void setSummaryWithStatus(final String urlStatus) {
        AsyncHttpClient.getDefaultInstance().websocket(urlStatus, "", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(final Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    mTvServerStatus.post(new Runnable() {
                        @Override
                        public void run() {
                            mTvServerStatus.setText(String.format(getString(R.string.summaryWsServerWithStatusError), ex.getLocalizedMessage()));
                        }
                    });
                    return;
                }
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {
                        Log.i(s);
                        try {
                            final JSONObject json = new JSONObject(s);
                            final int numOfWorkers = json.getInt("num_workers_available");
                            mTvServerStatus.post(new Runnable() {
                                @Override
                                public void run() {
                                    mTvServerStatus.setText(String.format(getString(R.string.summaryWsServerWithStatus), numOfWorkers));
                                }
                            });
                        } catch (JSONException e) {
                            mTvServerStatus.post(new Runnable() {
                                @Override
                                public void run() {
                                    mTvServerStatus.setText(String.format(getString(R.string.summaryWsServerWithStatusError), ex.getLocalizedMessage()));
                                }
                            });
                        }
                    }
                });
            }
        });
    }
}