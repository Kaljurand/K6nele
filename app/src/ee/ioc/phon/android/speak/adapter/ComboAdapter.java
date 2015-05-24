package ee.ioc.phon.android.speak.adapter;

import android.app.Activity;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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

    static class ViewHolder {
        protected TextView language;
        protected TextView service;
        protected CheckBox checkbox;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        if (convertView == null) {
            LayoutInflater inflator = context.getLayoutInflater();
            view = inflator.inflate(R.layout.list_item_combo, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.language = (TextView) view.findViewById(R.id.language);
            viewHolder.service = (TextView) view.findViewById(R.id.service);
            viewHolder.checkbox = (CheckBox) view.findViewById(R.id.check);
            viewHolder.checkbox
                    .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(CompoundButton buttonView,
                                                     boolean isChecked) {
                            Combo element = (Combo) viewHolder.checkbox
                                    .getTag();
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
        holder.language.setText(list.get(position).getLanguage());
        holder.service.setText(list.get(position).getService());
        holder.checkbox.setChecked(list.get(position).isSelected());
        return view;
    }
}