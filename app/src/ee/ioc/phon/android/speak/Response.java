package ee.ioc.phon.android.speak;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Parses the JSON object delivered by the webservice. It can be in one of the following forms:
 * <pre>
 * {"status": 9, "message": "No decoder available, try again later"}
 *
 * {"status": 0, "result": {"hypotheses": [{"transcript": "elas metsas..."}], "final": false}}
 * {"status": 0, "result": {"hypotheses": [{"transcript": "elas metsas..."}], "final": true}}
 *
 * {"status": 0, "adaptation_state": {"type": "string+gzip+base64", "value": "eJxlvcu7"}}
 * </pre>
 */
public abstract class Response {

    public static Response parseResponse(String data) throws ResponseException {
        try {
            JSONObject json = new JSONObject(data);
            int status = json.getInt("status");

            try {
                return new ResponseResult(status, json.getJSONObject("result"));
            } catch (JSONException e) {
            }

            try {
                return new ResponseMessage(status, json.getString("message"));
            } catch (JSONException e) {
            }

            return new ResponseAdaptationState(status, json.getJSONObject("adaptation_state"));

        } catch (JSONException e) {
            throw new ResponseException(e);
        }
    }


    public static class ResponseResult extends Response {
        private final int mStatus;
        private final ArrayList<String> mHypotheses = new ArrayList<>();
        private final boolean mIsFinal;

        public ResponseResult(int status, JSONObject result) throws JSONException {
            mStatus = status;
            mIsFinal = result.getBoolean("final");

            JSONArray array = result.getJSONArray("hypotheses");

            for (int i = 0; i < array.length(); i++) {
                mHypotheses.add(array.getJSONObject(i).getString("transcript"));
            }
        }

        public int getStatus() {
            return mStatus;
        }

        public ArrayList<String> getHypotheses() {
            return mHypotheses;
        }

        public boolean isFinal() {
            return mIsFinal;
        }
    }

    public static class ResponseAdaptationState extends Response {
        public ResponseAdaptationState(int status, JSONObject result) throws JSONException {
        }
    }

    public static class ResponseMessage extends Response {
        private final int mStatus;
        private final String mMessage;

        public ResponseMessage(int status, String message) throws JSONException {
            mStatus = status;
            mMessage = message;
        }

        public int getStatus() {
            return mStatus;
        }

        public String getMessage() {
            return mMessage;
        }
    }


    public static class ResponseException extends Exception {
        public ResponseException(JSONException e) {
            super(e);
        }
    }
}