package ee.ioc.phon.android.recsession;

import java.io.IOException;

public interface RecSession {

    void create() throws IOException, NotAvailableException;

    void sendChunk(byte[] bytes, boolean isLast) throws IOException;

    String getCurrentResult() throws IOException;

    RecSessionResult getResult() throws IOException;

    boolean isFinished();

    void cancel();
}