package ee.ioc.phon.android.speak.adapter

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.view.OnPressAndHoldListener
import ee.ioc.phon.android.speak.view.SpeechInputView.SpeechInputViewListener
import ee.ioc.phon.android.speechutils.editor.Command
import ee.ioc.phon.android.speechutils.editor.CommandMatcher
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter

/**
 * List of rules that have a label as buttons.
 * Tapping on the button returns the corresponding utterance via onFinalResult.
 *
 * TODO: improve specification of header (load only the columns that are needed)
 * TODO: implement putPrefMapMap (takes map instead of key and val)
 * TODO: convert utterance (i.e. regex) to a string (e.g. the first string matched by the utterance)
 */
class ClipboardAdapter(private val mListener: SpeechInputViewListener, commandMatcher: CommandMatcher, rewritesAsStr: String) : RecyclerView.Adapter<ClipboardAdapter.MyViewHolder>() {
    private val mCommands: List<Command> = UtteranceRewriter(rewritesAsStr, commandMatcher).commands.filterNot { it.label.isNullOrEmpty() }

    inner class MyViewHolder(var mView: TextView) : RecyclerView.ViewHolder(mView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_clip, parent, false) as TextView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val command = mCommands[position]
        val label = command.label
        holder.mView.text = label
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.mView.tooltipText = label
        }

        // TODO: Note that "press and hold" buttons are not compatible with scrolling the keyboard
        // TODO: show them with a different background
        if (command.isRepeatable) {
            holder.mView.setBackgroundResource(R.drawable.button_repeatable)
            holder.mView.setOnClickListener(null)
            holder.mView.setOnTouchListener(object : OnPressAndHoldListener() {
                public override fun onAction() {
                    val utt = command.makeUtt();
                    if (utt != null) {
                        mListener.onFinalResult(listOf(utt), Bundle())
                    }
                }
            })
        } else {
            holder.mView.setBackgroundResource(R.drawable.button_clip)
            holder.mView.setOnTouchListener(null)
            holder.mView.setOnClickListener { view: View? ->
                val utt = command.makeUtt()
                if (utt != null) {
                    mListener.onFinalResult(listOf(utt), Bundle())
                }
            }
            // TODO: launch regex generator picker instead
            /*
            holder.mView.setOnLongClickListener(v -> {
                Toast.makeText(v.getContext(), label, Toast.LENGTH_SHORT).show();
                return true;
            });
             */
        }
    }

    override fun getItemCount(): Int {
        return mCommands.size
    }

}