package ee.ioc.phon.android.speak.view;

import android.os.Bundle;

import java.util.List;


public abstract class AbstractSpeechInputViewListener implements SpeechInputView.SpeechInputViewListener {

    @Override
    public void onPartialResult(List<String> results) {
    }

    @Override
    public void onFinalResult(List<String> results, Bundle bundle) {
    }

    @Override
    public void onSwitchIme(boolean isAskUser) {
    }

    @Override
    public void onSearch() {
    }

    @Override
    public void onDeleteLastWord() {
    }

    @Override
    public void onAddNewline() {
    }

    @Override
    public void goUp() {
    }

    @Override
    public void goDown() {
    }

    @Override
    public void onAddSpace() {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onSelectAll() {
    }

    @Override
    public void onReset() {
    }


    @Override
    public void onStartListening() {
    }

    @Override
    public void onStopListening() {
    }

    // TODO: add onCancel()

    @Override
    public void onError(int errorCode) {
    }
}
