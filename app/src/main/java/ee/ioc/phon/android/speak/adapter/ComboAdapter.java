package ee.ioc.phon.android.speak.adapter;

import android.app.Activity;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = context.getLayoutInflater().inflate(R.layout.list_item_combo, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.icon = view.findViewById(R.id.serviceIcon);
            viewHolder.language = view.findViewById(R.id.language);
            viewHolder.service = view.findViewById(R.id.service);
            viewHolder.checkbox = view.findViewById(R.id.check);
            viewHolder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Combo item = (Combo) viewHolder.checkbox.getTag();
                item.setSelected(isChecked);
            });
            view.setTag(viewHolder);
            viewHolder.checkbox.setTag(list.get(position));
        } else {
            view = convertView;
            ((ViewHolder) view.getTag()).checkbox.setTag(list.get(position));
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        Combo item = list.get(position);
        holder.icon.setImageDrawable(item.getIcon(this.context));
        holder.language.setText(item.getLanguage());
        holder.service.setText(item.getService());
        holder.checkbox.setChecked(item.isSelected());
        return view;
    }
}