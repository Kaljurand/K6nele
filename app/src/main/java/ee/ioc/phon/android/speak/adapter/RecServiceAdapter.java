package ee.ioc.phon.android.speak.adapter;

import android.app.Activity;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.model.RecService;

public class RecServiceAdapter extends ArrayAdapter<RecService> {

    private final List<RecService> list;
    private final Activity context;

    public RecServiceAdapter(Fragment context, List<RecService> list) {
        super(context.getActivity(), R.layout.list_item_recservice, list);
        this.context = context.getActivity();
        this.list = list;
    }

    private static class ViewHolder {
        private ImageView icon;
        private TextView service;
        private TextView desc;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = context.getLayoutInflater().inflate(R.layout.list_item_recservice, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.icon = view.findViewById(R.id.serviceIcon);
            viewHolder.service = view.findViewById(R.id.service);
            viewHolder.desc = view.findViewById(R.id.desc);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        RecService item = list.get(position);
        holder.icon.setImageDrawable(item.getIcon(this.context));
        holder.service.setText(item.getService());
        holder.desc.setText(item.getDesc());
        return view;
    }
}