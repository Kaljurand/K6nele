package ee.ioc.phon.android.speak.adapter;

import android.app.Activity;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.model.Combo;

public class ComboAdapter extends ArrayAdapter<Combo> {

    private final List<Combo> list;
    private final Activity context;

    public ComboAdapter(Fragment context, List<Combo> list) {
        super(context.getActivity(), R.layout.list_item_combo, list);
        this.context = context.getActivity();
        this.list = list;
    }

    private static class ViewHolder {
        private ImageView icon;
        private TextView language;
        private TextView service;
        private CheckBox checkbox;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflator = context.getLayoutInflater();
            view = inflator.inflate(R.layout.list_item_combo, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.icon = view.findViewById(R.id.serviceIcon);
            viewHolder.language = view.findViewById(R.id.language);
            viewHolder.service = view.findViewById(R.id.service);
            viewHolder.checkbox = view.findViewById(R.id.check);
            viewHolder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Combo element = (Combo) viewHolder.checkbox.getTag();
                    element.setSelected(buttonView.isChecked());
                }
            });
            view.setTag(viewHolder);
            viewHolder.checkbox.setTag(list.get(position));
        } else {
            view = convertView;
            ((ViewHolder) view.getTag()).checkbox.setTag(list.get(position));
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        Combo combo = list.get(position);
        holder.icon.setImageDrawable(combo.getIcon(this.context));
        holder.language.setText(combo.getLanguage());
        holder.service.setText(combo.getService());
        holder.checkbox.setChecked(combo.isSelected());
        return view;
    }
}