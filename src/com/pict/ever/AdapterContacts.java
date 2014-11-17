package com.pict.ever;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AdapterContacts extends ArrayAdapter<String> implements Filterable {
	Context context;
	ArrayList<String> listContacts;  
	ViewHolder holder;

	AdapterContacts(Context c, ArrayList<String> list_of_contacts) {
		super(c, R.layout.adapter_contacts, R.id.contact_name,list_of_contacts);
		this.context = c;
		this.listContacts = list_of_contacts;
	}
	static class ViewHolder {
		RelativeLayout rl_adapter_contacts;
		TextView contactName;
		TextView isOnPictever;
		ImageView contactPicture;
	}
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView ==null ) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			convertView = inflater.inflate(R.layout.adapter_contacts, parent, false);
			holder = new ViewHolder();
			holder.rl_adapter_contacts = (RelativeLayout) convertView.findViewById(R.id.rl_adapter_contacts);
			holder.contactPicture = (ImageView) convertView.findViewById(R.id.contact_icon_send);
			holder.contactName = (TextView) convertView.findViewById(R.id.contact_name);
			holder.isOnPictever = (TextView) convertView.findViewById(R.id.contact_is_on_pictever);
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}

		JSONObject contact;
		try {
			contact = new JSONObject(listContacts.get(position));
			String image = contact.getString("uri_photo");

			if (image.equals("")) {
				holder.contactPicture.setImageResource(R.drawable.button_contacts);
			}
			else {
				if (image.equals("my_keo_image")) {
					holder.contactPicture.setImageResource(R.drawable.my_keo_image);
				}
				else {
					holder.contactPicture.setImageURI(Uri.parse(image));
				}
			}
			String name =  contact.getString("name");
			holder.contactName.setText(name);

			String is_on_pictever = contact.getString("contact_id");
			String status = contact.optString("status");

			if (is_on_pictever.startsWith("num")) {
				holder.isOnPictever.setText("");		}
			else {
				if (status.isEmpty()) {
					holder.isOnPictever.setText("No status");
				}
				else {
					holder.isOnPictever.setText(status);
				}
			}
			String is_selected = contact.getString("is_selected");
			if (is_selected.equals("selected")) {
				//				contactIsSelected.setVisibility(View.VISIBLE);
				holder.rl_adapter_contacts.setBackgroundResource(R.color.MidnightBlue);
			}
			else {
				holder.rl_adapter_contacts.setBackgroundResource(R.color.DarkCyan);
				//				contactIsSelected.setVisibility(View.INVISIBLE);
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return convertView;
	}
}