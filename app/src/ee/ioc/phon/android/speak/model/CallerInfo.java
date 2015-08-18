package ee.ioc.phon.android.speak.model;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

public class CallerInfo {

    private final Bundle mExtras;
    private final EditorInfo mEditorInfo;
    private final String mPackageName;

    public CallerInfo(Bundle extras, EditorInfo editorInfo, String packageName) {
        mExtras = extras;
        mEditorInfo = editorInfo;
        mPackageName = packageName;
    }

    public CallerInfo(Bundle extras, String packageName) {
        mExtras = extras;
        mEditorInfo = null;
        mPackageName = packageName;
    }

    public CallerInfo(Bundle extras, ComponentName componentName) {
        mExtras = extras;
        mEditorInfo = null;
        mPackageName = getPackageName(componentName);
    }

    public CallerInfo(EditorInfo editorInfo, String packageName) {
        mExtras = null;
        mEditorInfo = editorInfo;
        mPackageName = packageName;
    }

    public Bundle getExtras() {
        return mExtras;
    }

    public EditorInfo getEditorInfo() {
        return mEditorInfo;
    }

    public String getPackageName() {
        return mPackageName;
    }

    private static String getPackageName(ComponentName componentName) {
        if (componentName == null) {
            return null;
        }
        return componentName.getPackageName();
    }
}