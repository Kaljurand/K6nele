package ee.ioc.phon.android.recsession;

import java.util.List;

public class Hypothesis {
    private final String mUtterance;
    private final List<Linearization> mLinearizations;

    public Hypothesis(String utterance, List<Linearization> linearizations) {
        mUtterance = utterance;
        mLinearizations = linearizations;
    }

    public String getUtterance() {
        return mUtterance;
    }

    public List<Linearization> getLinearizations() {
        return mLinearizations;
    }
}