package com.pict.ever;

import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AdapterCountriesList extends ArrayAdapter<String> {
	private final Context context;
	private final String[] source;
	private ViewHolder holder;

	public AdapterCountriesList(Context context, String[] source) {
		super(context, R.layout.countries_item, source);
		this.context = context;
		this.source = source;
	}

	static class ViewHolder {
		TextView tvCountryName;
		ImageView ivFlag;
		String[] values;
		String pngName;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView==null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.countries_item, parent, false);
			holder = new ViewHolder();
			holder.tvCountryName = (TextView) convertView.findViewById(R.id.tvCountryName);
			holder.ivFlag = (ImageView) convertView.findViewById(R.id.ivFlag);
			convertView.setTag(holder);
		}
		else 
			holder = (ViewHolder) convertView.getTag();
		String ssid = source[position].split(",")[1];
		String code = source[position].split(",")[0];
		Locale locale = new Locale("", ssid);
		holder.tvCountryName.setText(locale.getDisplayCountry(Locale.US).trim() + " (+" + code + ")");
		String pngName = ssid.trim().toLowerCase(locale);
		holder.ivFlag.setImageResource(context.getResources().getIdentifier(
				"drawable/" + pngName, null, context.getPackageName()));
		return convertView;
	}
}