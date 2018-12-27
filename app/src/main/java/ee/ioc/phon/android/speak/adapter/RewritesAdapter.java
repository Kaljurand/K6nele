package ee.ioc.phon.android.speak.adapter;

import android.app.Activity;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.model.Rewrites;

public class RewritesAdapter extends ArrayAdapter<Rewrites> {

    private final List<Rewrites> list;
    private final Activity context;

    public RewritesAdapter(Fragment context, List<Rewrites> list) {
        super(context.getActivity(), R.layout.list_item_rewrites, list);
        this.context = context.getActivity();
        this.list = list;
    }

    private static class ViewHolder {
        private TextView id;
        private CheckBox checkbox;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = context.getLayoutInflater().inflate(R.layout.list_item_rewrites, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.id = view.findViewById(R.id.rewritesId);
            viewHolder.checkbox = view.findViewById(R.id.rewritesIsSelected);
            viewHolder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Rewrites item = (Rewrites) viewHolder.checkbox.getTag();
                item.setSelected(isChecked);
            });
            view.setTag(viewHolder);
            viewHolder.checkbox.setTag(list.get(position));
        } else {
            view = convertView;
            ((ViewHolder) view.getTag()).checkbox.setTag(list.get(position));
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        Rewrites item = list.get(position);
        holder.id.setText(item.getId());
        holder.checkbox.setChecked(item.isSelected());
        return view;
    }
}