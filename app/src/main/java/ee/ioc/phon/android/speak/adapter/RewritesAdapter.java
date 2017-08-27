package ee.ioc.phon.android.speak.adapter;

import android.app.Activity;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
        private TextView isSelected;
    }

    @Override
    public
    @NonNull
    View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflator = context.getLayoutInflater();
            view = inflator.inflate(R.layout.list_item_rewrites, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.id = (TextView) view.findViewById(R.id.rewritesId);
            viewHolder.isSelected = (TextView) view.findViewById(R.id.rewritesIsSelected);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        Rewrites rewrites = list.get(position);
        holder.id.setText(rewrites.getId());
        if (rewrites.isSelected()) {
            holder.isSelected.setVisibility(View.VISIBLE);
        } else {
            holder.isSelected.setVisibility(View.INVISIBLE);
        }
        return view;
    }
}