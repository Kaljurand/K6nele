package ee.ioc.phon.android.speak.model

import android.content.ComponentName
import android.os.Bundle
import android.view.inputmethod.EditorInfo

// TODO: preserve the full component name
class CallerInfo {

    val extras: Bundle?
    val editorInfo: EditorInfo?
    val packageName: String?

    constructor(extras: Bundle, editorInfo: EditorInfo, packageName: String) {
        this.extras = extras
        this.editorInfo = editorInfo
        this.packageName = packageName
    }

    constructor(extras: Bundle, componentName: ComponentName?) {
        this.extras = extras
        editorInfo = null
        packageName = componentName?.packageName
    }
}