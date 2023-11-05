package ee.ioc.phon.android.speak.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.database.Combo1

class Combo1Adapter : ListAdapter<Combo1, Combo1Adapter.Combo1ViewHolder>(Combo1Comparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Combo1ViewHolder {
        return Combo1ViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: Combo1ViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current.shortLabel, current.componentName.flattenToShortString())
    }

    class Combo1ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val language: TextView = itemView.findViewById(R.id.language)
        private val service: TextView = itemView.findViewById(R.id.service)

        fun bind(languageText: String?, serviceText: String?) {
            language.text = languageText
            service.text = serviceText
        }

        companion object {
            fun create(parent: ViewGroup): Combo1ViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_combo, parent, false)
                return Combo1ViewHolder(view)
            }
        }
    }

    class Combo1Comparator : DiffUtil.ItemCallback<Combo1>() {
        override fun areItemsTheSame(oldCombo1: Combo1, newCombo1: Combo1): Boolean {
            return oldCombo1 === newCombo1
        }

        override fun areContentsTheSame(oldCombo1: Combo1, newCombo1: Combo1): Boolean {
            return oldCombo1.id == newCombo1.id
        }
    }
}