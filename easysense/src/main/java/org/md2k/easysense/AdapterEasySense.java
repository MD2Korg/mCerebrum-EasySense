package org.md2k.easysense;

/**
 * Created by smhssain on 11/4/2015.
 */

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.md2k.datakitapi.source.platform.PlatformType;

import java.util.List;

public class AdapterEasySense extends BaseAdapter {

    private LayoutInflater layoutinflater;
    private List<ViewContent> listStorage;
    private Context context;

    public AdapterEasySense(Context context, List<ViewContent> customizedListView) {
        this.context = context;
        layoutinflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        listStorage = customizedListView;
    }

    @Override
    public int getCount() {
        return listStorage.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder listViewHolder;
        if (convertView == null) {
            listViewHolder = new ViewHolder();
            convertView = layoutinflater.inflate(R.layout.listview_with_text_image, parent, false);
            listViewHolder.textInListView = (TextView) convertView.findViewById(R.id.textView);
            listViewHolder.imageInListView = (ImageView) convertView.findViewById(R.id.imageView);
            convertView.setTag(listViewHolder);
        } else {
            listViewHolder = (ViewHolder) convertView.getTag();
        }

        listViewHolder.textInListView.setText(listStorage.get(position).getName());
        if (listStorage.get(position).getPlatformType().equals(PlatformType.EASYSENSE))
            listViewHolder.imageInListView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_easysense_teal_48dp));
        return convertView;
    }


    static class ViewHolder {
        TextView textInListView;
        ImageView imageInListView;
    }
}