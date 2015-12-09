package ee.ioc.phon.android.speak.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import ee.ioc.phon.android.speak.model.WsServer;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

public class WsServerAdapter extends RealmBaseAdapter<WsServer> implements ListAdapter {

    private static class MyViewHolder {
        TextView timeStamp;
    }

    public WsServerAdapter(Context context, int resId,
                           RealmResults<WsServer> realmResults,
                           boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MyViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1,
                    parent, false);
            viewHolder = new MyViewHolder();
            viewHolder.timeStamp = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (MyViewHolder) convertView.getTag();
        }

        WsServer item = realmResults.get(position);
        viewHolder.timeStamp.setText(item.getUri());
        return convertView;
    }

    public RealmResults<WsServer> getRealmResults() {
        return realmResults;
    }
}