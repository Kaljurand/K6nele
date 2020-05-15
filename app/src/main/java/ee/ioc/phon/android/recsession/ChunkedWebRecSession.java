package ee.ioc.phon.android.recsession;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ChunkedWebRecSession implements RecSession {

    public static final String CONF_BASE_URL = "base_url";

    public static final String CONTENT_TYPE = "audio/x-raw-int;rate=16000;channels=1;signed=true;endianness=1234;depth=16;width=16";

    // Parameter keys
    public static final String KEY_LANG = "lang";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_PHRASE = "phrase";

    // API identifier in the User-Agent
    public static final String USER_AGENT = "ChunkedWebRecSession/0.0.8";

    // When HTTP chunked encoding is used, the stream is divided into chunks,
    // each prefixed with a header containing the chunk's size.
    // Setting a large chunk length requires a large internal buffer, potentially wasting memory.
    // Setting a small chunk length increases the number of bytes that must be transmitted because
    // of the header on every chunk.
    // Most caller should use 0 to get the system default.
    // (Note: We used to have 1024 before, this might also be the default, see libcore.net.http)
    private static final int CHUNK_LENGTH = 0;

    private final Map<String, String> mParams = new HashMap<String, String>();

    private String mContentType = CONTENT_TYPE;
    private String userAgent = USER_AGENT;

    private Properties configuration = new Properties();

    private HttpURLConnection connection;
    private OutputStream out;

    private ChunkedWebRecSessionResult result;
    private boolean finished = false;


    public ChunkedWebRecSession(URL wsUrl) {
        this(wsUrl, null, null, 1);
    }


    public ChunkedWebRecSession(URL wsUrl, URL lmUrl) {
        this(wsUrl, lmUrl, null, 1);
    }


    public ChunkedWebRecSession(URL wsUrl, URL lmUrl, String lang) {
        this(wsUrl, lmUrl, lang, 1);
    }


    /**
     * <p>Recognizer session is constructed on the basis of the recognizer
     * webservice URL. Optionally one can specify the speech recognition grammar
     * to guide the recognizer. The grammar must be in either JSGF or PGF format.
     * In the latter case, one can also specify a language into which the
     * raw recognition result is translated. Specifying the language without the
     * grammar does not make sense and in this case the language is ignored.</p>
     *
     * @param wsUrl Recognizer webservice URL
     * @param lmUrl Language model (JSGF or PGF grammar) URL
     * @param lang  Target language to which to translate the raw recognizer output (in case PGF)
     * @param nbest Max number of transcription hypotheses to return
     */
    public ChunkedWebRecSession(URL wsUrl, URL lmUrl, String lang, int nbest) {
        if (lmUrl == null) {
            configuration.setProperty(CONF_BASE_URL, wsUrl.toExternalForm() + "?nbest=" + nbest);
        } else if (lang == null) {
            configuration.setProperty(CONF_BASE_URL, wsUrl.toExternalForm() + "?lm=" + lmUrl.toExternalForm() + "&nbest=" + nbest);
        } else {
            configuration.setProperty(CONF_BASE_URL,
                    wsUrl.toExternalForm() + "?lm=" + lmUrl.toExternalForm() + "&output-lang=" + lang + "&nbest=" + nbest);
        }
    }


    public void create() throws IOException, NotAvailableException {
        String urlAsString = configuration.getProperty(CONF_BASE_URL);
        // Builds the final URL
        // It can technically throw UnsupportedEncodingException (a type of IOException)
        for (String key : mParams.keySet()) {
            urlAsString += "&" + URLEncoder.encode(key, "utf-8") + "=" + URLEncoder.encode(mParams.get(key), "utf-8");
        }

        URL url = new URL(urlAsString);
        connection = (HttpURLConnection) url.openConnection();
        connection.setChunkedStreamingMode(CHUNK_LENGTH);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", mContentType);
        connection.setRequestProperty("User-Agent", userAgent);

        // This should happen automatically with getOutputStream()
        //connection.connect();

        // System.out.println("Created connection: " + connection);
    }


    /**
     * @deprecated
     */
    public String getCurrentResult() throws IOException {
        if (result == null || result.getUtterances().isEmpty()) {
            return "";
        }
        return result.getUtterances().get(0);
    }


    public ChunkedWebRecSessionResult getResult() throws IOException {
        return result;
    }


    public boolean isFinished() {
        return finished;
    }


    public void sendChunk(byte[] bytes, boolean isLast) throws IOException {
        if (bytes != null && bytes.length > 0) {
            if (out == null) {
                out = new BufferedOutputStream(connection.getOutputStream());
            }
            out.write(bytes);
            // System.out.println("Wrote " + bytes.length + " bytes");
        }
        if (isLast) {
            try {
                if (out != null) {
                    out.close();
                }
                InputStream is = new BufferedInputStream(connection.getInputStream());
                result = new ChunkedWebRecSessionResult(new InputStreamReader(is));
            } finally {
                connection.disconnect();
                finished = true;
            }
        }
    }


    public Properties getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }


    public void cancel() {
        try {
            connection.disconnect();
        } catch (Exception e) {
            // silent OK
        } finally {
            finished = true;
        }
    }


    public void setContentType(String contentType) {
        mContentType = contentType;
    }


    /**
     * <p>
     * Adds an additional identifier to the User-Agent string.
     * </p>
     *
     * @param userAgentComment Application identifier in the User-Agent
     */
    public void setUserAgentComment(String userAgentComment) {
        userAgent = USER_AGENT + " (" + userAgentComment + ")";
    }


    /**
     * <p>Declare the language of the input speech, e.g.
     * <code>en-US</code>, <code>et</code>, etc.</p>
     *
     * @param lang input speech language identifier
     */
    public void setLang(String lang) {
        setParam(KEY_LANG, lang);
    }


    /**
     * <p>Sets an identifier, i.e. a string that can be used to
     * reliably group queries by the same speaker.</p>
     * <p>
     * TODO: maybe rename to "speaker_id"
     *
     * @param deviceId device identifier
     */
    public void setDeviceId(String deviceId) {
        setParam(KEY_DEVICE_ID, deviceId);
    }


    /**
     * <p>Sets the desired transcription for the enclosed audio.</p>
     *
     * <p>This parameter is optional and is mostly intended for
     * calibration, speech data collection, and other similar
     * applications.</p>
     *
     * @param phrase desired transcription
     */
    public void setPhrase(String phrase) {
        setParam(KEY_PHRASE, phrase);
    }


    // TODO: make it private by v1.0
    public void setParam(String key, String value) {
        mParams.put(key, value);
    }

}