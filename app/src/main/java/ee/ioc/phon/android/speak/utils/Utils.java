/*
 * Copyright 2011-2015, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.speak.utils;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import ee.ioc.phon.android.speak.Executable;
import ee.ioc.phon.android.speak.ExecutableString;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.activity.SpeechActionActivity;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.model.Combo;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.editor.Command;
import ee.ioc.phon.android.speechutils.editor.CommandEditorResult;
import ee.ioc.phon.android.speechutils.editor.CommandMatcher;
import ee.ioc.phon.android.speechutils.editor.CommandMatcherFactory;
import ee.ioc.phon.android.speechutils.editor.Constants;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

/**
 * <p>Some useful static methods.</p>
 *
 * @author Kaarel Kaljurand
 */
public final class Utils {

    private Utils() {
    }

    /**
     * TODO: should we immediately return null if id = 0?
     */
    public static String idToValue(Context context, Uri contentUri, String columnId, String columnUrl, long id) {
        String value = null;
        Cursor c = context.getContentResolver().query(
                contentUri,
                new String[]{columnUrl},
                columnId + "= ?",
                new String[]{String.valueOf(id)},
                null);

        if (c.moveToFirst()) {
            value = c.getString(0);
        }
        c.close();
        return value;
    }

    /**
     * Creates a non-cancelable dialog with two buttons, both finish the activity,
     * one launches the given intent first.
     * TODO: note that we explicitly set the dialog style. This is because if the caller activity's style
     * is Theme.Translucent.NoTitleBar then the dialog is unstyled (maybe an Android bug?)
     */
    public static AlertDialog getLaunchIntentDialog(final AppCompatActivity activity, String msg, final Intent intent) {
        return new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog)
                .setPositiveButton(activity.getString(R.string.buttonGoToSettings), (dialog, id) -> {
                    activity.startActivity(intent);
                    activity.finish();
                })
                .setNegativeButton(activity.getString(R.string.buttonCancel), (dialog, id) -> {
                    dialog.cancel();
                    activity.finish();
                })
                .setMessage(msg)
                .setCancelable(false)
                .create();
    }


    public static AlertDialog getYesNoDialog(Context context, String confirmationMessage, final Executable ex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setMessage(confirmationMessage)
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.buttonYes), (dialog, id) -> ex.execute())
                .setNegativeButton(context.getString(R.string.buttonNo), (dialog, id) -> dialog.cancel());
        return builder.create();
    }

    public static AlertDialog getYesNoDialog(Context context, String confirmationMessage, final Executable ex1, final Executable ex2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog);
        builder
                .setMessage(confirmationMessage)
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.buttonOk), (dialog, id) -> ex1.execute())
                .setNegativeButton(context.getString(R.string.buttonCancel), (dialog, id) -> {
                    dialog.cancel();
                    ex2.execute();
                });
        return builder.create();
    }

    public static AlertDialog getTextEntryDialog(Context context, String title, String initialText, final ExecutableString ex) {
        final View textEntryView = LayoutInflater.from(context).inflate(R.layout.alert_dialog_url_entry, null);
        final EditText et = (EditText) textEntryView.findViewById(R.id.url_edit);
        if (initialText != null) {
            et.setText(initialText);
            et.setSelection(initialText.length());
        }
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(textEntryView)
                .setPositiveButton(R.string.buttonOk, (dialog, whichButton) -> ex.execute(et.getText().toString()))
                .setNegativeButton(R.string.buttonCancel, (dialog, whichButton) -> dialog.cancel())
                .create();
    }


    public static String getVersionName(Context c) {
        PackageInfo info = getPackageInfo(c);
        if (info == null) {
            return "?.?.?";
        }
        return info.versionName;
    }


    private static PackageInfo getPackageInfo(Context c) {
        PackageManager manager = c.getPackageManager();
        try {
            return manager.getPackageInfo(c.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Log.e("Couldn't find package information in PackageManager: " + e);
        }
        return null;
    }


    public static String chooseValue(String firstChoice, String secondChoice) {
        if (firstChoice == null) {
            return secondChoice;
        }
        return firstChoice;
    }


    public static String chooseValue(String firstChoice, String secondChoice, String thirdChoice) {
        String choice = chooseValue(firstChoice, secondChoice);
        if (choice == null) {
            return thirdChoice;
        }
        return choice;
    }


    public static String makeUserAgentComment(String tag, String versionName, String caller) {
        return tag + "/" + versionName + "; " +
                Build.MANUFACTURER + "/" +
                Build.DEVICE + "/" +
                Build.DISPLAY + "; " +
                caller;
    }

    /**
     * Generates rewriters based on the list of names of rewrite tables.
     * If a name does not resolve to a rewrite table then generates null.
     * If the given list is null, then the default rewriter is returned (currently at most one).
     * Passing an empty list effectively turns off rewriting.
     */
    public static Iterable<UtteranceRewriter> genRewriters(final SharedPreferences prefs,
                                                           final Resources resources,
                                                           String[] rewritesByName,
                                                           String language,
                                                           ComponentName service,
                                                           ComponentName app) {
        final String[] names;
        if (rewritesByName == null) {
            Set<String> defaults = PreferenceUtils.getPrefStringSet(prefs, resources, R.string.defaultRewriteTables);
            if (defaults.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            names = defaults.toArray(new String[defaults.size()]);
            // TODO: defaults should be a list (not a set that needs to be sorted)
            Arrays.sort(names);
        } else {
            names = rewritesByName;
        }

        final int length = names.length;
        if (length == 0) {
            return Collections.EMPTY_LIST;
        }
        final CommandMatcher commandMatcher = CommandMatcherFactory.createCommandFilter(language, service, app);

        return () -> new Iterator<UtteranceRewriter>() {

            private int mCurrent = 0;

            @Override
            public boolean hasNext() {
                return mCurrent < length;
            }

            @Override
            public UtteranceRewriter next() {
                String rewritesAsStr = PreferenceUtils.getPrefMapEntry(prefs, resources, R.string.keyRewritesMap, names[mCurrent++]);
                if (rewritesAsStr == null) {
                    return null;
                }
                return new UtteranceRewriter(rewritesAsStr, commandMatcher);
            }
        };
    }

    /**
     * Creates a rule where the utterance is a new unique pattern based on the timestamp.
     * The replacement and the possible command with arguments are based on the rewriting results.
     * In case of commands the clip label is the command's comment or, if missing, the pretty-printed command.
     * For a simple replacement the clip label is the replacement.
     * Clicking on a Recent clip will just push it to the top of the list, i.e. make it most recent.
     * <p>
     * TODO: review and update doc
     * TODO: do we need to generate the utterance field at all?
     *
     * @param text         Spoken input, used as the clipboard label, as well as the replacement if command == null
     * @param editorResult Command (if spoken input triggered a command). Used to populate the clips's
     *                     replacement, and command.
     */
    public static List<Command> addRule(String text, CommandEditorResult editorResult, @NonNull String rewrites, ComponentName app) {
        UtteranceRewriter.Rewrite rewrite = editorResult.getRewrite();
        Log.i("Add rule: " + text + "|" + editorResult.getStr() + "|" + rewrite.getCommand());
        Calendar cal = Calendar.getInstance();
        long uttId = cal.getTimeInMillis();
        // TODO: come up with a better utterance
        String uttAsStr = "^<" + uttId + ">$";
        Pattern utt = Pattern.compile(uttAsStr, Constants.REWRITE_PATTERN_FLAGS);
        Pattern appPattern = Pattern.compile(Pattern.quote(app.getPackageName()), Constants.REWRITE_PATTERN_FLAGS);
        // TODO
        Pattern localePattern = null;
        Pattern servicePattern = null;
        // cal.getTime().toString()
        Command newCommand;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String comment = sdf.format(cal.getTime()) + ", " + text;
        if (rewrite.isCommand()) {
            // We store the matched command, but change the utterance, comment, and the command matcher.
            // TODO: review this
            String label = rewrite.getCommand().getLabel();
            if (label == null) {
                label = rewrite.ppCommand();
            }
            // Rewrite args is the output of command.parse, i.e. the evaluated args
            newCommand = new Command(label, comment, localePattern, servicePattern, appPattern, utt, rewrite.mStr, rewrite.mId, rewrite.mArgs);
        } else {
            newCommand = new Command(rewrite.mStr, comment, localePattern, servicePattern, appPattern, utt, rewrite.mStr, null);
        }
        UtteranceRewriter ur = new UtteranceRewriter(rewrites);
        List<Command> commands = ur.getCommands();
        // Delete commands that produce the same result.
        // TODO: perform this as part of UtteranceRewriter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            commands.removeIf(newCommand::equalsCommand);
        }
        // Add a rule
        commands.add(0, newCommand);
        return commands;
    }

    public static <E> List<E> makeList(Iterable<E> iter) {
        List<E> list = new ArrayList<>();
        for (E item : iter) {
            list.add(item);
        }
        return list;
    }

    public static Intent getRecognizerIntent(String action, CallerInfo callerInfo, String language) {
        Intent intent = new Intent(action);
        Bundle extras = callerInfo.getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, callerInfo.getPackageName());
        if (callerInfo.getEditorInfo() != null) {
            intent.putExtra(Extras.EXTRA_EDITOR_INFO, toBundle(callerInfo.getEditorInfo()));
        }
        // Declaring that in the IME we would like to allow longer pauses (2 sec).
        // The service might not implement these (e.g. Kõnele currently does not)
        // TODO: what is the difference of these two constants?
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);

        if (language != null) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
            // TODO: make this configurable
            intent.putExtra(Extras.EXTRA_ADDITIONAL_LANGUAGES, new String[]{});
        }
        return intent;
    }

    /**
     * Constructs and publishes the list of app shortcuts, one for each combo that is selected for the
     * search panel. The intent behind the shortcut sets AUTO_START=true and sets RESULTS_REWRITES
     * to the list of default rewrites (at creation time), and PROMPT to the list of rewrite names.
     * All other settings (e.g. MAX_RESULTS) depend on the settings at execution time.
     */
    @TargetApi(Build.VERSION_CODES.N_MR1)
    public static void publishShortcuts(Context context, List<Combo> selectedCombos, Set<String> rewriteTables) {
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcuts = new ArrayList<>();
        int maxShortcutCountPerActivity = shortcutManager.getMaxShortcutCountPerActivity();
        int counter = 0;

        // TODO: rewriteTables should be a list (not a set that needs to be sorted)
        String[] names = rewriteTables.toArray(new String[rewriteTables.size()]);
        Arrays.sort(names);
        String rewritesId = TextUtils.join(", ", names);

        for (Combo combo : selectedCombos) {
            Intent intent = new Intent(context, SpeechActionActivity.class);
            intent.setAction(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, combo.getLocaleAsStr());
            intent.putExtra(Extras.EXTRA_SERVICE_COMPONENT, combo.getServiceComponent().flattenToShortString());
            if (names.length > 0) {
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, rewritesId);
            }
            intent.putExtra(Extras.EXTRA_RESULT_REWRITES, names);
            intent.putExtra(Extras.EXTRA_AUTO_START, true);
            // Launch the activity so that the existing Kõnele activities are not in the background stack.
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            shortcuts.add(new ShortcutInfo.Builder(context, combo.getId() + rewritesId)
                    .setIntent(intent)
                    .setShortLabel(combo.getShortLabel())
                    .setLongLabel(combo.getLongLabel() + "; " + rewritesId)
                    .setIcon(Icon.createWithBitmap(drawableToBitmap(combo.getIcon(context))))
                    .build());
            counter++;
            // We are only allowed a certain number (5) of shortcuts
            if (counter >= maxShortcutCountPerActivity) {
                break;
            }
        }
        shortcutManager.setDynamicShortcuts(shortcuts);
    }

    private static Bundle toBundle(EditorInfo attribute) {
        Bundle bundle = new Bundle();
        bundle.putBundle("extras", attribute.extras);
        bundle.putInt("inputType", attribute.inputType);
        bundle.putInt("initialSelStart", attribute.initialSelStart);
        bundle.putInt("initialSelEnd", attribute.initialSelEnd);
        bundle.putString("actionLabel", asString(attribute.actionLabel));
        bundle.putString("fieldName", asString(attribute.fieldName));
        bundle.putString("hintText", asString(attribute.hintText));
        bundle.putString("label", asString(attribute.label));
        // This line gets the actual caller package registered in the package registry.
        // The key needs to be "packageName".
        bundle.putString("packageName", asString(attribute.packageName));
        return bundle;
    }

    private static String asString(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof SpannableString) {
            SpannableString ss = (SpannableString) o;
            return ss.subSequence(0, ss.length()).toString();
        }
        return o.toString();
    }

    /**
     * This is needed to convert a Drawable to an Icon (we need to convert the Drawable first
     * to Bitmap). Solution from
     * http://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
     * Starting with API 23, we might make combo.getIcon return Icon (instead of a Drawable), which
     * will simplify things.
     */
    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}