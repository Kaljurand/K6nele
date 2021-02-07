package ee.ioc.phon.android.speak.adapter

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.model.RewriteRule
import ee.ioc.phon.android.speak.view.OnPressAndHoldListener
import ee.ioc.phon.android.speak.view.SpeechInputView.SpeechInputViewListener

/**
 * List of button/clip labels mapped to
 * utterances. Clicking on a clip will return the utterance via onFinalResult.
 *
 *
 * TODO: improve specification of header (load only the columns that are needed)
 * TODO: implement putPrefMapMap (takes map instead of key and val)
 * TODO: improve dealing with nulls
 * TODO: convert utterance (i.e. regex) to a string (e.g. the first string matched by the utterance)
 */
class ButtonsAdapter(private val mListener: SpeechInputViewListener) :
        ListAdapter<RewriteRule, ButtonsAdapter.MyViewHolder>(MyComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_clip, parent, false) as TextView
        return MyViewHolder(view, mListener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class MyViewHolder(val itemView: TextView, val mListener: SpeechInputViewListener) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView as TextView

        private var current: RewriteRule? = null

        fun bind(rewriteRule: RewriteRule) {
            current = rewriteRule

            val command = RewriteRule.toCommand(rewriteRule)
            val utt = command.makeUtt()
            var label = command.label
            if (label == null || label.isEmpty()) {
                label = utt ?: command.toString()
            }
            textView.text = label
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                textView.tooltipText = label
            }

            // TODO: Note that "press and hold" buttons are not compatible with scrolling the keyboard
            // TODO: show them with a different background
            if (command.isRepeatable) {
                textView.setBackgroundResource(R.drawable.button_repeatable)
                textView.setOnClickListener(null)
                textView.setOnTouchListener(object : OnPressAndHoldListener() {
                    public override fun onAction() {
                        if (utt != null) {
                            mListener.onFinalResult(listOf(utt), Bundle())
                        }
                    }
                })
            } else {
                textView.setBackgroundResource(R.drawable.button_clip)
                textView.setOnTouchListener(null)
                textView.setOnClickListener { view: View? ->
                    if (utt != null) {
                        mListener.onFinalResult(listOf(utt), Bundle())
                    }
                }

                // TODO: launch regex generator picker instead
                //textView.setOnLongClickListener { v ->
                //    Toast.makeText(v.getContext(), label, Toast.LENGTH_SHORT).show();
                //    return true;
                //}

            }
        }
    }

    // TODO: understand why this is needed
    class MyComparator : DiffUtil.ItemCallback<RewriteRule>() {
        override fun areItemsTheSame(oldItem: RewriteRule, newItem: RewriteRule): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: RewriteRule, newItem: RewriteRule): Boolean {
            return oldItem.id == newItem.id
        }
    }
}