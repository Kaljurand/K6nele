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
        private final ArrayList<String> mHypothesesPp = new ArrayList<>();
        private final boolean mIsFinal;

        public Result(JSONObject result) throws JSONException {
            // The "final" field does not have to exist, but if it does
            // then it must be a boolean.
            mIsFinal = result.has("final") && result.getBoolean("final");
            JSONArray array = result.getJSONArray("hypotheses");

            for (int i = 0; i < array.length(); i++) {
                String transcript = array.getJSONObject(i).getString("transcript");
                mHypotheses.add(transcript);
                mHypothesesPp.add(pp(transcript));

            }
        }

        public ArrayList<String> getHypotheses() {
            return mHypotheses;
        }

        public ArrayList<String> getHypothesesPp() {
            return mHypothesesPp;
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

    /**
     * Pretty-prints the string returned by the server to be ortographically correct (Estonian),
     * assuming that the string represents a sequence of tokens separated by a single space character.
     * Note that a text editor (which has additional information about the context of the cursor)
     * will need to do additional pretty-printing, e.g. capitalization if the cursor follows a
     * sentence end marker.
     *
     * @param str String to be pretty-printed
     * @return Pretty-printed string (never null)
     */
    private static String pp(String str) {
        boolean isSentenceStart = false;
        boolean isWhitespaceBefore = false;
        String text = "";
        for (String tok : str.split(" ")) {
            if (tok.length() == 0) {
                continue;
            }
            String glue = " ";
            char firstChar = tok.charAt(0);
            if (isWhitespaceBefore
                    || Constants.CHARACTERS_WS.contains(firstChar)
                    || Constants.CHARACTERS_PUNCT.contains(firstChar)) {
                glue = "";
            }

            if (isSentenceStart) {
                tok = Character.toUpperCase(firstChar) + tok.substring(1);
            }

            if (text.length() == 0) {
                text = tok;
            } else {
                text += glue + tok;
            }

            isWhitespaceBefore = Constants.CHARACTERS_WS.contains(firstChar);

            // If the token is not a character then we are in the middle of the sentence.
            // If the token is an EOS character then a new sentences has started.
            // If the token is some other character other than whitespace (then we are in the
            // middle of the sentences. (The whitespace characters are transparent.)
            if (tok.length() > 1) {
                isSentenceStart = false;
            } else if (Constants.CHARACTERS_EOS.contains(firstChar)) {
                isSentenceStart = true;
            } else if (!isWhitespaceBefore) {
                isSentenceStart = false;
            }
        }
        return text;
    }
}