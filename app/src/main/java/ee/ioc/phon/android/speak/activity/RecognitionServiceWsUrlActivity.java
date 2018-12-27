package ee.ioc.phon.android.speak.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class RecognitionServiceWsUrlActivity extends Activity {

    private static final int TIMEOUT_PING = 100;
    private List<String> mList = new ArrayList<>();
    private ServerAdapter mAdapter;
    private Button mBScan;
    private TextView mTvServerStatus;
    private EditText mEtUrl;
    private EditText mEtScan;
    private Scan mScan;
    private String mIp;
    private WebSocket mWebSocket;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recognition_service_ws_url);

        mEtUrl = findViewById(R.id.etWsServerUrl);
        mEtUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String serverUri = mEtUrl.getText().toString();
                setUrl(getBaseUri(serverUri));
            }
            return false;
        });

        mTvServerStatus = findViewById(R.id.tvServerStatus);

        final RecyclerView lvResults = findViewById(R.id.rvIpList);
        //lvResults.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        lvResults.setLayoutManager(mLayoutManager);
        mAdapter = new ServerAdapter(mList);
        lvResults.setAdapter(mAdapter);

        findViewById(R.id.bWsServerDefault1).setOnClickListener(view -> setUrl(getString(R.string.defaultWsServer1)));

        findViewById(R.id.bWsServerDefault2).setOnClickListener(view -> setUrl(getString(R.string.defaultWsServer2)));

        mEtScan = findViewById(R.id.etScanNetwork);
        mEtScan.setText(getIPAddress(true));

        mBScan = findViewById(R.id.bScanNetwork);
        mBScan.setOnClickListener(view -> {
            if (mScan == null) {
                mIp = mEtScan.getText().toString().trim();
                if (mIp.isEmpty()) {
                    toast(getString(R.string.errorNetworkUndefined));
                } else {
                    mScan = new Scan();
                    mScan.execute(mIp);
                }
            } else {
                mScan.cancel(true);
                mScan = null;
            }
        });

        findViewById(R.id.bApplyUrl).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setData(Uri.parse(mEtUrl.getText().toString()));
            setResult(Activity.RESULT_OK, intent);
            finish();
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String serverUri = PreferenceUtils.getPrefString(prefs, getResources(), R.string.keyWsServer, R.string.defaultWsServer);
        setUrl(getBaseUri(serverUri));
    }

    @Override
    public void onStop() {
        super.onStop();
        closeSocket();
    }

    private String getBaseUri(String serverUri) {
        return serverUri.substring(0, serverUri.lastIndexOf('/') + 1);
    }

    private void setUrl(String url) {
        if (mEtUrl != null) {
            mEtUrl.setText(String.format(getString(R.string.wsUrlSuffixSpeech), url));
            mTvServerStatus.setText(getString(R.string.statusServerStatus));
            setSummaryWithStatus(String.format(getString(R.string.wsUrlSuffixStatus), url));
        }
    }

    private void setScanUi() {
        mBScan.setText(getString(R.string.buttonScan));
        // We post the change otherwise onProgressUpdate might change it later (?)
        mEtScan.post(() -> mEtScan.setText(mIp));
        mEtScan.setEnabled(true);
    }

    private void setCancelUi() {
        mEtScan.setEnabled(false);
        mBScan.setText(getString(R.string.buttonCancel));
    }

    private void toast(String msg) {
        Toast.makeText(RecognitionServiceWsUrlActivity.this, msg, Toast.LENGTH_LONG).show();

    }

    /**
     * Get IP address from first non-localhost interface
     * Solution from https://stackoverflow.com/a/13007325/12547
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(ex.getLocalizedMessage());
        }
        return "";
    }

    private class Scan extends AsyncTask<String, Pair<String, Boolean>, String> {
        @Override
        protected String doInBackground(String... ips) {
            String errorMessage = null;
            int start = 0;
            int end = 255;
            for (String ip : ips) {
                String base = ip.substring(0, ip.lastIndexOf('.') + 1);
                try {
                    // TODO: review
                    NetworkInterface iFace = NetworkInterface
                            .getByInetAddress(InetAddress.getByName(ip));

                    for (int i = start; i <= end; i++) {
                        if (isCancelled()) break;
                        InetAddress pingAddr = InetAddress.getByName(base + i);
                        String result = pingAddr.getHostAddress();
                        if (pingAddr.isReachable(iFace, 200, TIMEOUT_PING)) {
                            publishProgress(new Pair<>(result, true));
                            Log.i("FOUND: " + result);
                        } else {
                            publishProgress(new Pair<>(result, false));
                        }
                    }
                } catch (UnknownHostException ex) {
                    Log.e(ex.toString());
                    errorMessage = ex.getLocalizedMessage();
                    break;
                } catch (IOException ex) {
                    Log.e(ex.toString());
                    errorMessage = ex.getLocalizedMessage();
                    break;
                }
            }
            return errorMessage;
        }

        @Override
        protected void onProgressUpdate(Pair<String, Boolean>... progress) {
            Pair<String, Boolean> pair = progress[0];
            mEtScan.setText(pair.first);
            if (pair.second) {
                mList.add(pair.first);
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mList.clear();
            setCancelUi();
        }

        @Override
        protected void onPostExecute(String errorMessage) {
            setScanUi();
            if (errorMessage != null) {
                toast(errorMessage);
            }
        }

        @Override
        protected void onCancelled(String errorMessage) {
            setScanUi();
            if (errorMessage != null) {
                toast(errorMessage);
            }
        }
    }

    private class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.MyViewHolder> {
        private List<String> mDataset;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public Button mView;

            public MyViewHolder(Button v) {
                super(v);
                mView = v;
            }
        }

        public ServerAdapter(List<String> myDataset) {
            mDataset = myDataset;
        }

        @Override
        public ServerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Button v = (Button) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_server_ip, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {
            holder.mView.setText(mDataset.get(position));
            holder.mView.setOnClickListener(view -> setUrl("ws://" + holder.mView.getText() + ":8080/client/ws/"));
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }

    private void closeSocket() {
        if (mWebSocket != null && mWebSocket.isOpen()) {
            mWebSocket.end(); // TODO: or close?
            mWebSocket = null;
        }
    }

    private void setSummaryWithStatus(final String urlStatus) {
        closeSocket();
        AsyncHttpClient.getDefaultInstance().websocket(urlStatus, "", (ex, webSocket) -> {
            mWebSocket = webSocket;
            if (ex != null) {
                mTvServerStatus.post(() -> mTvServerStatus.setText(String.format(getString(R.string.summaryWsServerWithStatusError), ex.getLocalizedMessage())));
                return;
            }
            mWebSocket.setStringCallback(s -> {
                Log.i(s);
                try {
                    final JSONObject json = new JSONObject(s);
                    final int numOfWorkers = json.getInt("num_workers_available");
                    mTvServerStatus.post(() -> mTvServerStatus.setText(getResources().
                            getQuantityString(R.plurals.summaryWsServerWithStatus, numOfWorkers, numOfWorkers)));
                } catch (JSONException e) {
                    mTvServerStatus.post(() -> mTvServerStatus.setText(String.format(getString(R.string.summaryWsServerWithStatusError), ex.getLocalizedMessage())));
                }
            });
        });
    }
}
