package kaldi.speechkit;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Parses the JSON package delivered by the webservice.
 */
public class Response {

    private Response() {
    }

    public static Response parseResponse(String data) throws ResponseException {
        try {
            JSONObject json = new JSONObject(data);
            int status = json.getInt("status");

            try {
                return new ResponseResult(status, json.getJSONObject("result"));
            } catch (JSONException e) {
            }

            try {
                return new ResponseAdaptationState(status, json.getJSONObject("adaptation_state"));
            } catch (JSONException e) {
            }
        } catch (JSONException e) {
            throw new ResponseException(e);
        }

        throw new ResponseException();
    }


    /*
    {"status": 0, "result": {"hypotheses": [{"transcript": "elas metsas..."}], "final": false}}
    {"status": 0, "result": {"hypotheses": [{"transcript": "elas metsas..."}], "final": true}}
    */
    public static class ResponseResult extends Response {
        private int mStatus;
        private String mText;
        private boolean mIsFinal;

        public ResponseResult(int status, JSONObject result) throws JSONException {
            mStatus = status;
            mText = result.getJSONArray("hypotheses").getJSONObject(0).getString("transcript");
            mIsFinal = result.getBoolean("final");
        }

        public int getStatus() {
            return mStatus;
        }

        public String getText() {
            return mText;
        }

        public boolean isFinal() {
            return mIsFinal;
        }
    }

    /*
    {"status": 0, "adaptation_state": {"type": "string+gzip+base64", "value": "eJxlvcu7"}}
    */
    public static class ResponseAdaptationState extends Response {
        public ResponseAdaptationState(int status, JSONObject result) throws JSONException {
        }
    }


    public static class ResponseException extends Exception {
        public ResponseException() {
            super();
        }

        public ResponseException(JSONException e) {
            super(e);
        }
    }
}