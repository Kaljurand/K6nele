package ee.ioc.phon.android.speak.adapter

import android.content.ComponentName
import android.graphics.Paint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import ee.ioc.phon.android.speak.R
import ee.ioc.phon.android.speak.ServiceLanguageChooser
import ee.ioc.phon.android.speak.model.Combo

class ComboButtonsAdapter(
    private val mListener: ComboButtonsAdapterListener,
    private val mSlc: ServiceLanguageChooser
) : RecyclerView.Adapter<ComboButtonsAdapter.MyViewHolder>() {
    private lateinit var mSelectedView: Button

    interface ComboButtonsAdapterListener {
        fun onComboChange(language: String?, service: ComponentName)
        fun onMore()
    }

    class MyViewHolder(var mView: Button) : RecyclerView.ViewHolder(mView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_combo_button, parent, false) as Button
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (mSlc.size() == position) {
            holder.mView.text = "+"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.mView.tooltipText = "..."
            }
            holder.mView.setOnClickListener { view: View -> mListener.onMore() }
        } else {
            val context = holder.mView.context
            val combo = Combo(context, mSlc[position])
            if (mSlc.isSelected(position)) {
                mSelectedView = holder.mView
                mSelectedView.alpha = 1f
                mSelectedView.paintFlags = mSelectedView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                mSelectedView.isClickable = false
            } else {
                holder.mView.alpha = 0.5f
                holder.mView.paintFlags = 0
                holder.mView.isClickable = true
            }
            var label = combo.localeAsStr
            if (label.isEmpty() || label.equals("und")) {
                label = combo.service.substring(0, 3)
            }
            holder.mView.text = label
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.mView.tooltipText = combo.longLabel
            }

            holder.mView.setOnClickListener { view: View ->
                if (!mSlc.isSelected(position)) {
                    mSlc.set(position)
                    mSelectedView.alpha = 0.5f
                    mSelectedView.paintFlags = 0
                    mSelectedView.isClickable = true
                    mSelectedView = view as Button
                    mSelectedView.alpha = 1f
                    mSelectedView.paintFlags = holder.mView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    mSelectedView.isClickable = false
                    mListener.onComboChange(mSlc.language, mSlc.service)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return mSlc.size() + 1
    }
}