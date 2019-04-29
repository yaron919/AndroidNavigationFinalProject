package com.example.yashual.androidnavigationfinalproject;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CitiesCustomAdapter extends BaseAdapter {


    Activity activity;
    List<City> cities;
    LayoutInflater inflater;

    //short to create constructer using command+n for mac & Alt+Insert for window


    public CitiesCustomAdapter(Activity activity) {
        this.activity = activity;
    }

    public CitiesCustomAdapter(Activity activity, List<City> cities) {
        this.activity   = activity;
        this.cities = cities;
        inflater = activity.getLayoutInflater();
    }


    @Override
    public int getCount() {
        return cities.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        ViewHolder holder = null;

        if (view == null){

            view = inflater.inflate(R.layout.list_view_item, viewGroup, false);

            holder = new ViewHolder();

            holder.tvUserName = (TextView)view.findViewById(R.id.tv_user_name);
            holder.ivCheckBox = (ImageView) view.findViewById(R.id.iv_check_box);

            view.setTag(holder);
        }else
            holder = (ViewHolder)view.getTag();

        City model = cities.get(i);

        holder.tvUserName.setText(model.getName());

        if (model.isSelected())
            holder.ivCheckBox.setBackgroundResource(R.drawable.circle_ok);

        else
            holder.ivCheckBox.setBackgroundResource(R.drawable.circle_empty);

        return view;

    }

    public void updateRecords(List<City> cities){
        this.cities = cities;

        notifyDataSetChanged();
    }

    class ViewHolder{

        TextView tvUserName;
        ImageView ivCheckBox;

    }
}
