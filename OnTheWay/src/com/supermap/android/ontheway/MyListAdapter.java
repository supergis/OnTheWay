package com.supermap.android.ontheway;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 自定义的list适配器，方便实现list动态交互。
 *
 */
public class MyListAdapter extends BaseAdapter {
    private LayoutInflater mInflater = null;
    private List<Map<String, Object>> list;
    private PreferencesService preferencesService;

    public MyListAdapter(Context context, List<Map<String, Object>> list) {
        this.mInflater = LayoutInflater.from(context);
        this.list = list;
        this.preferencesService = new PreferencesService(context);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }
    
    

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItemView listItemView = null;
        if (convertView == null) {
            listItemView = new ListItemView();
            convertView = mInflater.inflate(R.layout.list_view_row, null);
            listItemView.num = (TextView) convertView.findViewById(R.id.busNum);
            listItemView.busName = (TextView) convertView.findViewById(R.id.busStation);
            listItemView.line = (TextView) convertView.findViewById(R.id.busLine);
            listItemView.sum = (TextView) convertView.findViewById(R.id.busPeoSum);
            listItemView.join =(ImageView) convertView.findViewById(R.id.joinin);
            convertView.setTag(listItemView);
        } else {
            listItemView = (ListItemView) convertView.getTag();
        }

        listItemView.num.setText(String.valueOf(list.get(position).get("num")));
        listItemView.busName.setText((String) list.get(position).get("busName"));
        listItemView.line.setText((String) list.get(position).get("line"));
        listItemView.sum.setText((String) list.get(position).get("sum"));
        listItemView.join.setVisibility(View.GONE); 
        if(position==preferencesService.getRouteInfo(CommonUtils.ROUTEINFO_FIEL)){
            listItemView.join.setVisibility(View.VISIBLE);   
        } 
        return convertView;

    }

    // ListItemView静态类
    static class ListItemView {
        public TextView num;
        public TextView busName;
        public TextView line;
        public TextView sum;
        public ImageView join;
    }
}