package ee.ioc.phon.android.speak.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.view.OnPressAndHoldListener;
import ee.ioc.phon.android.speak.view.SpeechInputView;
import ee.ioc.phon.android.speechutils.editor.Command;
import ee.ioc.phon.android.speechutils.editor.CommandMatcher;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;

public class ClipboardAdapter extends RecyclerView.Adapter<ClipboardAdapter.MyViewHolder> {
    private final SpeechInputView.SpeechInputViewListener mListener;
    private final UtteranceRewriter mUr;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView mView;

        public MyViewHolder(TextView v) {
            super(v);
            mView = v;
        }
    }

    /**
     * List of button/clip labels mapped to
     * utterances. Clicking on a clip will return the utterance via onFinalResult.
     * <p>
     * TODO: improve specification of header (load only the columns that are needed)
     * TODO: implement putPrefMapMap (takes map instead of key and val)
     * TODO: improve dealing with nulls
     * TODO: convert utterance (i.e. regex) to a string (e.g. the first string matched by the utterance)
     */
    public ClipboardAdapter(SpeechInputView.SpeechInputViewListener speechInputView, CommandMatcher commandMatcher, String rewritesAsStr) {
        mListener = speechInputView;
        mUr = new UtteranceRewriter(rewritesAsStr, commandMatcher);
    }

    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder((TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_clip, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {
        final Command command = mUr.getCommands().get(position);
        holder.mView.setText(command.getLabelOrString());
        String val = command.makeUtt();
        // TODO: Note that "press and hold" buttons are not compatible with scrolling the keyboard
        // TODO: show them with a different background
        if (command.isRepeatable()) {
            holder.mView.setOnClickListener(null);
            holder.mView.setOnTouchListener(new OnPressAndHoldListener() {
                @Override
                public void onAction() {
                    if (val != null) {
                        mListener.onFinalResult(Collections.singletonList(val), new Bundle());
                    }
                }
            });
        } else {
            holder.mView.setOnTouchListener(null);
            holder.mView.setOnClickListener(view -> {
                        if (val != null) {
                            mListener.onFinalResult(Collections.singletonList(val), new Bundle());
                        }
                    }
            );
            // TODO: launch regex generator picker instead
            holder.mView.setOnLongClickListener(v -> {
                //speechInputView.showMessage(command.toString());
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return mUr.getCommandHolder().size();
    }
}
