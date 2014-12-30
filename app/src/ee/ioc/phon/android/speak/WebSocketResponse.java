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
public class WebSocketResponse {

    // Usually used when recognition results are sent.
    public static final int STATUS_SUCCESS = 0;

    // Audio contains a large portion of silence or non-speech.
    public static final int STATUS_NO_SPEECH = 1;

    // Recognition was aborted for some reason.
    public static final int STATUS_ABORTED = 2;

    // Used when all recognizer processes are currently in use and recognition cannot be performed.
    public static final int STATUS_NOT_AVAILABLE = 9;

    private final JSONObject mJson;
    private final int mStatus;

    public WebSocketResponse(String data) throws WebSocketResponseException {
        try {
            mJson = new JSONObject(data);
            mStatus = mJson.getInt("status");
        } catch (JSONException e) {
            throw new WebSocketResponseException(e);
        }
    }

    public int getStatus() {
        return mStatus;
    }

    public boolean isResult() {
        return mJson.has("result");
    }


    public Result parseResult() throws WebSocketResponseException {
        try {
            return new Result(mJson.getJSONObject("result"));
        } catch (JSONException e) {
            throw new WebSocketResponseException(e);
        }
    }


    public Message parseMessage() throws WebSocketResponseException {
        try {
            return new Message(mJson.getString("message"));
        } catch (JSONException e) {
            throw new WebSocketResponseException(e);
        }
    }

    public AdaptationState parseAdaptationState() throws WebSocketResponseException {
        try {
            return new AdaptationState(mJson.getJSONObject("adaptation_state"));
        } catch (JSONException e) {
            throw new WebSocketResponseException(e);
        }
    }


    public static class Result {
        private final ArrayList<String> mHypotheses = new ArrayList<>();
        private final boolean mIsFinal;

        public Result(JSONObject result) throws JSONException {
            // The "final" field does not have to exist, but if it does
            // then it must be a boolean.
            if (result.has("final")) {
                mIsFinal = result.getBoolean("final");
            } else {
                mIsFinal = false;
            }
            JSONArray array = result.getJSONArray("hypotheses");

            for (int i = 0; i < array.length(); i++) {
                mHypotheses.add(array.getJSONObject(i).getString("transcript"));
            }
        }

        public ArrayList<String> getHypotheses() {
            return mHypotheses;
        }

        public boolean isFinal() {
            return mIsFinal;
        }
    }


    public static class Message {
        private final String mMessage;

        public Message(String message) throws JSONException {
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }
    }


    public static class AdaptationState {
        public AdaptationState(JSONObject result) throws JSONException {
        }
    }


    public static class WebSocketResponseException extends Exception {
        public WebSocketResponseException(JSONException e) {
            super(e);
        }
    }
}