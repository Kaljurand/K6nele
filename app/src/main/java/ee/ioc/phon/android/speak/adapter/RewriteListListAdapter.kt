package ee.ioc.phon.android.speak.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.model.RewriteList

class RewriteListListAdapter(private val onClick: (RewriteList) -> Unit, private val onLongClick: (RewriteList) -> Unit) :
        ListAdapter<RewriteList, RewriteListListAdapter.RewriteRuleViewHolder>(MyComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewriteRuleViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_rewrites, parent, false)
        return RewriteRuleViewHolder(view, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: RewriteRuleViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class RewriteRuleViewHolder(itemView: View, val onClick: (RewriteList) -> Unit, val onLongClick: (RewriteList) -> Unit) :
            RecyclerView.ViewHolder(itemView) {
        // TODO: use view binder
        private val rewriteListId: TextView = itemView.findViewById(R.id.rewriteListId)
        private val name: TextView = itemView.findViewById(R.id.rewritesId)
        private val isSelected: CheckBox = itemView.findViewById(R.id.rewritesIsSelected)
        private var current: RewriteList? = null

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

        fun bind(rewriteList: RewriteList) {
            current = rewriteList
            rewriteListId.text = rewriteList.rewriteListId.toString()
            name.text = rewriteList.name
            isSelected.isChecked = rewriteList.isEnabled
        }
    }

    // TODO: understand why this is needed
    class MyComparator : DiffUtil.ItemCallback<RewriteList>() {
        override fun areItemsTheSame(oldItem: RewriteList, newItem: RewriteList): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: RewriteList, newItem: RewriteList): Boolean {
            return oldItem.rewriteListId == newItem.rewriteListId
        }
    }
}