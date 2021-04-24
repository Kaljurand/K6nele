package ee.ioc.phon.android.speak.adapter

import android.content.ComponentName
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.ServiceLanguageChooser
import ee.ioc.phon.android.speak.model.Combo

class ComboButtonsAdapter(private val mListener: ComboButtonsAdapterListener, private val mSlc: ServiceLanguageChooser) : RecyclerView.Adapter<ComboButtonsAdapter.MyViewHolder>() {
    private lateinit var mSelectedView: View

    interface ComboButtonsAdapterListener {
        fun onComboChange(language: String, service: ComponentName)
        fun onOpenConf()
    }

    class MyViewHolder(var mView: Button) : RecyclerView.ViewHolder(mView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_combo_button, parent, false) as Button)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (mSlc.size() == position) {
            holder.mView.text = "+"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.mView.tooltipText = "Open conf"
            }
            holder.mView.setOnClickListener { view: View -> mListener.onOpenConf() }
        } else {
            val context = holder.mView.context
            val combo = Combo(context, mSlc[position])
            if (mSlc.isSelected(position)) {
                mSelectedView = holder.mView
                holder.mView.alpha = 1f
            } else {
                holder.mView.alpha = 0.5f
            }
            holder.mView.text = combo.localeAsStr
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.mView.tooltipText = combo.longLabel
            }
            holder.mView.setOnClickListener { view: View ->
                mSlc.set(position)
                mSelectedView.alpha = 0.5f
                mSelectedView = view
                mSelectedView.alpha = 1f
                mListener.onComboChange(mSlc.language, mSlc.service)
            }
        }
    }

    override fun getItemCount(): Int {
        return mSlc.size() + 1
    }
}