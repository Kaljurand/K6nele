package kaldi.speechkit;

public interface RecorderListener {
    abstract void onRecorderBuffer(byte[] buffer);
}
