package kaldi.speechkit;

import org.json.JSONException;
import org.json.JSONObject;

public class Result {
    private int status;
    private String transcript;
    private boolean isFinal = false;

    public Result(int status, String trans, boolean fi) {
        this.status = status;
        this.transcript = trans;
        this.isFinal = fi;
    }

    public static Result parseResult(String data) throws JSONException {

        JSONObject jObj = new JSONObject(data);
        int _status = jObj.getInt("status");
        String _transcript = jObj.getJSONObject("result").getJSONArray("hypotheses").getJSONObject(0).getString("transcript");
        boolean _isFinal = jObj.getJSONObject("result").getBoolean("final");

        return new Result(_status, _transcript, _isFinal);
    }

    public int getStatus() {
        return status;
    }

    public String getText() {
        return transcript;
    }

    public boolean isFinal() {
        return isFinal;
    }
}