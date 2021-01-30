package ee.ioc.phon.android.speak.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.model.RewriteRule

class RewriteRuleListAdapter(private val onClick: (RewriteRule) -> Unit, private val onLongClick: (RewriteRule) -> Unit) :
        ListAdapter<RewriteRule, RewriteRuleListAdapter.RewriteRuleViewHolder>(WordsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewriteRuleViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_item_rewrite, parent, false)
        return RewriteRuleViewHolder(view, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: RewriteRuleViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class RewriteRuleViewHolder(itemView: View, val onClick: (RewriteRule) -> Unit, val onLongClick: (RewriteRule) -> Unit) :
            RecyclerView.ViewHolder(itemView) {
        private val ownerId: TextView = itemView.findViewById(R.id.ownerId)
        private val app: TextView = itemView.findViewById(R.id.app)
        private val utterance: TextView = itemView.findViewById(R.id.utterance)
        private val replacement: TextView = itemView.findViewById(R.id.replacement)
        private var current: RewriteRule? = null

        init {
            itemView.setOnClickListener {
                current?.let {
                    onClick(it)
                }
            }

            itemView.setOnLongClickListener {
                current?.let {
                    onLongClick(it)
                }
                true
            }
        }

        fun bind(rewriteRule: RewriteRule) {
            current = rewriteRule
            ownerId.text = rewriteRule.ownerId.toString()
            app.text = rewriteRule.app?.pattern()
            utterance.text = rewriteRule.utterance?.pattern()
            replacement.text = rewriteRule.replacement
        }
    }

    class WordsComparator : DiffUtil.ItemCallback<RewriteRule>() {
        override fun areItemsTheSame(oldItem: RewriteRule, newItem: RewriteRule): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: RewriteRule, newItem: RewriteRule): Boolean {
            return oldItem.id == newItem.id
        }
    }
}