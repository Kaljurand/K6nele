package ee.ioc.phon.android.recsession;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * For the input format description see: http://bark.phon.ioc.ee/speech-api/v1/
 *
 * @author Kaarel Kaljurand
 */
public class ChunkedWebRecSessionResult implements RecSessionResult {

    private final List<Hypothesis> mHypotheses = new ArrayList<>();
    private final List<String> mUtterances = new ArrayList<>();
    private final List<String> mLinearizations = new ArrayList<>();

    public ChunkedWebRecSessionResult(InputStreamReader reader) throws IOException {
        Object obj = JSONValue.parse(reader);

        if (obj == null) {
            throw new IOException("Server response is not well-formed");
        }

        JSONObject jsonObj = (JSONObject) obj;
        for (Object o1 : (JSONArray) jsonObj.get("hypotheses")) {
            JSONObject jo1 = (JSONObject) o1;
            add(mUtterances, jo1.get("utterance"));
            Object lins = jo1.get("linearizations");
            List<Linearization> linearizations = new ArrayList<>();
            if (lins != null) {
                for (Object o2 : (JSONArray) lins) {
                    JSONObject jo2 = (JSONObject) o2;
                    add(mLinearizations, jo2.get("output"));

                    String output = objToString(jo2.get("output"));
                    String lang = objToString(jo2.get("lang"));
                    linearizations.add(new Linearization(output, lang));
                }
            }
            mHypotheses.add(new Hypothesis(objToString(jo1.get("utterance")), linearizations));
        }
    }


    public List<String> getLinearizations() {
        if (mLinearizations.isEmpty()) {
            return mUtterances;
        }
        return mLinearizations;
    }


    public List<String> getUtterances() {
        return mUtterances;
    }


    public List<Hypothesis> getHypotheses() {
        return mHypotheses;
    }


    private void add(List<String> list, Object obj) {
        if (obj != null) {
            String str = obj.toString();
            if (str.length() > 0) {
                list.add(str);
            }
        }
    }


    private String objToString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }
}