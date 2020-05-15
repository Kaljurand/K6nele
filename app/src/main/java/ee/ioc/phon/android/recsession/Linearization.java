package ee.ioc.phon.android.recsession;

public class Linearization {
    private final String mOutput;
    private final String mLang;

    public Linearization(String output, String lang) {
        mOutput = output;
        mLang = lang;
    }

    public String getOutput() {
        return mOutput;
    }

    public String getLang() {
        return mLang;
    }

}