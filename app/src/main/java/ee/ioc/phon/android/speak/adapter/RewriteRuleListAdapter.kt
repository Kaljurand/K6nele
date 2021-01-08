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

class RewriteRuleListAdapter : ListAdapter<RewriteRule, RewriteRuleListAdapter.RewriteRuleViewHolder>(WordsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewriteRuleViewHolder {
        return RewriteRuleViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: RewriteRuleViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current.replacement)
    }

    class RewriteRuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val wordItemView: TextView = itemView.findViewById(R.id.textView)

        fun bind(text: String?) {
            wordItemView.text = text
        }

        companion object {
            fun create(parent: ViewGroup): RewriteRuleViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                        .inflate(R.layout.recyclerview_item_rewrite, parent, false)
                return RewriteRuleViewHolder(view)
            }
        }
    }

    class WordsComparator : DiffUtil.ItemCallback<RewriteRule>() {
        override fun areItemsTheSame(oldItem: RewriteRule, newItem: RewriteRule): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: RewriteRule, newItem: RewriteRule): Boolean {
            return oldItem.replacement == newItem.replacement
        }
    }
}