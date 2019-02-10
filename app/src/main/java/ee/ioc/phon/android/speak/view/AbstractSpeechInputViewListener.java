package ee.ioc.phon.android.speak.view;

import android.os.Bundle;

import java.util.List;


public abstract class AbstractSpeechInputViewListener implements SpeechInputView.SpeechInputViewListener {

    @Override
    public void onPartialResult(List<String> results, boolean isSemiFinal) {
        // empty
    }

    @Override
    public void onFinalResult(List<String> results, Bundle bundle) {
        // empty
    }

    @Override
    public void onCommand(String text) {
        // empty
    }

    @Override
    public void onSwitchIme(boolean isAskUser) {
        // empty
    }

    @Override
    public void onSwitchToLastIme() {
        // empty
    }

    @Override
    public void onAction(int actionId, boolean hide) {
        // empty
    }

    @Override
    public void onDeleteLeftChar() {
        // empty
    }

    @Override
    public void onDeleteLastWord() {
        // empty
    }

    @Override
    public void onAddNewline() {
        // empty
    }

    @Override
    public void goUp() {
        // empty
    }

    @Override
    public void goDown() {
        // empty
    }

    @Override
    public void moveRel(int numOfSteps) {
        // empty
    }

    @Override
    public void moveRelSel(int numOfSteps, int type) {
        // empty
    }

    @Override
    public void onExtendSel(String regex) {
        // empty
    }

    @Override
    public void onAddSpace() {
        // empty
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        // empty
    }

    @Override
    public void onSelectAll() {
        // empty
    }

    @Override
    public void onReset() {
        // empty
    }

    @Override
    public void onStartListening() {
        // empty
    }

    @Override
    public void onStopListening() {
        // empty
    }

    // TODO: add onCancel()

    @Override
    public void onError(int errorCode) {
        // empty
    }
}