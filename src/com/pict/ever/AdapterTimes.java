package com.pict.ever;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;

public class AdapterTimes extends ArrayAdapter<String> implements Filterable {
	Context context;
	ArrayList<String> list_send_choices;

	AdapterTimes(Context c,ArrayList<String> list) {
		super(c, R.layout.adapter_times, R.id.tvSendChoice, list);
		this.context = c;
		this.list_send_choices = list;
	}
	public View getView(int position, View convertView, ViewGroup parent) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View row = inflater.inflate(R.layout.adapter_times, parent, false);
		
		TextView myTitle = (TextView) row.findViewById(R.id.tvSendChoice);
		JSONObject json_send_choice;
		try {
			json_send_choice = new JSONObject(list_send_choices.get(position));
			myTitle.setText(json_send_choice.getString("send_label"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return row;
	}
}