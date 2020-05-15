package ee.ioc.phon.android.recsession;

import java.util.List;

/**
 * <p>The result of transcription is a complex object,
 * possibly containing:</p>
 *
 * <ul>
 * <li>multiple transcription hypothesis with optional confidence scores;</li>
 * <li>the raw recognition result, Utterance (<code>String</code>),
 * as well as normalizations into languages identified by language codes (Linearizations).</li>
 * </ul>
 *
 * @author Kaarel Kaljurand
 */
public interface RecSessionResult {

    /**
     * <p>Returns a flat list of linearizations where
     * the information about which hypothesis produced
     * the linearizations and what is the language of
     * the linearization is not preserved.</p>
     *
     * <p>The implementation MUST return a (possibly empty) list
     * which MUST NOT contain empty <code>String</code>s.
     * <code>null</code> is not allowed as a return value.</p>
     *
     * @return (flat) list of linearizations
     */
    public List<String> getLinearizations();


    /**
     * <p>The implementation MUST return a (possibly empty) list
     * which MUST NOT contain empty <code>String</code>s
     * <code>null</code> is not allowed as a return value.</p>
     *
     * @return list of utterances
     */
    public List<String> getUtterances();

    public List<Hypothesis> getHypotheses();

}
