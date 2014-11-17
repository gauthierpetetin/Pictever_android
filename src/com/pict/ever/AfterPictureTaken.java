package com.pict.ever;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CalendarView;
import android.widget.CalendarView.OnDateChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import android.widget.Toast;

public class AfterPictureTaken extends PicteverActivity {

	public ImageButton button_color,button_cancel,button_send_future;
	ArrayList<String> list_ids_selected,localContacts;
	ListView listView_times,listView_contacts;
	ListAdapter adapter_contacts;
	int counter,pos,xDelta,yDelta,time_start,time_end;
	String contact_ids,send_choice,photo_path,message,year="2014",month="00",day="00",hour="00",minute="00";
	EditText edit_message;
	Bitmap bmp;
	static Context context;
	String TAG = "AfterPic";
	String from,contactsTitle = " Who ? ", dateTitle = " When ? ";
	RelativeLayout rl_popup;
	CalendarView calendarView;
	TimePicker time_picker;
	TextView tv_title_popup,tvTimezone;
	ImageView display_image;
	Typeface font;
	Controller controller;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.after_picture_taken);

		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();
		photo_path = getIntent().getStringExtra("photo_path");
		from = getIntent().getStringExtra("from");
		context = AfterPictureTaken.this;

		Log.v(TAG,controller.upload_is_active);
		if (controller.prefs.getInt("after_picture_taken_permanent",0) < 2){
			((TextView) findViewById(R.id.tapToAddaText)).setVisibility(View.VISIBLE);
			int counter = controller.prefs.getInt("after_picture_taken_permanent",0) + 1;
			controller.editor= controller.prefs.edit();
			controller.editor.putInt("after_picture_taken_permanent", counter);
			controller.editor.commit();
		}
		else {
			((TextView) findViewById(R.id.tapToAddaText)).setVisibility(View.INVISIBLE);
		}
		list_ids_selected = new ArrayList<String>();
		counter = controller.prefs.getInt("color_counter", 0);
		font = Typeface.createFromAsset(getAssets(), "gabriola.ttf");
		new AsyncTask<String, String, String>() {
			@Override
			protected String doInBackground(String... params) {
				localContacts = controller.sortedlistContacts;
				controller.sortContacts(localContacts);
				controller.unselectAllContacts(localContacts);
				return null;
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");	
	}
	@Override
	public void onBackPressed() {
		if (rl_popup.getVisibility()==View.VISIBLE) {
			rl_popup.setVisibility(View.INVISIBLE);
			button_send_future.setImageResource(R.drawable.send_in_the_future_white);
		}
		else 
			super.onBackPressed();
	}

	@Override
	public void onResume() {
		super.onResume();	
		// Hide status bar
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE );
		display();
		controller.ll_progress = (LinearLayout) findViewById(R.id.ll_progress);
		if (controller.upload_is_active.equals("yes")) {
			if (controller.upload!=null) {
				controller.ll_progress.setVisibility(View.VISIBLE);
				controller.ll_progress_params = new RelativeLayout.LayoutParams(
						Math.round((float) controller.uploadProgression/100*controller.prefs.getInt("SCREEN_HEIGHT",0)),
						Math.round((float) controller.prefs.getInt("SCREEN_WIDTH", 500)/100));
				if (controller.rl_title!=null)
					controller.ll_progress_params.addRule(RelativeLayout.BELOW,controller.rl_title.getId());
				controller.ll_progress.setLayoutParams(controller.ll_progress_params);
			}
		}
		else {
			if (controller.upload_is_active.equals("failed")) {
				controller.ll_progress.setVisibility(View.VISIBLE);
				controller.ll_progress.setBackgroundResource(R.color.LightGrey);
				controller.ll_progress_params = new RelativeLayout.LayoutParams(
						Math.round((float) controller.uploadProgression/100*
								controller.prefs.getInt("SCREEN_HEIGHT",0)),
								Math.round((float) controller.prefs.getInt("SCREEN_WIDTH", 500)/100));
				if (controller.rl_title!=null)
					controller.ll_progress_params.addRule(RelativeLayout.BELOW,controller.rl_title.getId());
				controller.ll_progress.setLayoutParams(controller.ll_progress_params);

			}
			else 
				controller.ll_progress.setVisibility(View.INVISIBLE);
		}
	}

	public void display() {

		File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
				+ "/Pictever/" + photo_path);
		Log.v(TAG,"size file : " + Integer.toString((int)file.length()));
		bmp = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
				+ "/Pictever/" + photo_path);

		display_image = (ImageView) findViewById(R.id.display_image);
		edit_message = (EditText) findViewById(R.id.edit_message);
		edit_message.setMaxWidth((int) Math.round((float) controller.prefs.getInt("SCREEN_HEIGHT",500)
				- controller.prefs.getInt("SCREEN_HEIGHT",500)/10));
		edit_message.setTextIsSelectable(true);
		rl_popup = (RelativeLayout) findViewById(R.id.rl_popup);
		tv_title_popup = (TextView) findViewById(R.id.tv_title_popup);
		listView_times = (ListView) findViewById(R.id.listView_times);
		listView_contacts = (ListView) findViewById(R.id.list_pick_contact);
		tvTimezone  = (TextView) findViewById(R.id.help_timezone);
		calendarView = (CalendarView) findViewById(R.id.calendarView);
		time_picker = (TimePicker) findViewById(R.id.timePicker);
		button_cancel = (ImageButton) findViewById(R.id.button_cancel);
		button_color = (ImageButton) findViewById(R.id.button_color);
		button_color.setVisibility(View.INVISIBLE);
		change_color();
		button_send_future = (ImageButton) findViewById(R.id.button_send_future);
		button_send_future.setImageResource(R.drawable.send_in_the_future_white);
		Calendar cal = Calendar.getInstance();
		year = Integer.toString(cal.get(Calendar.YEAR));
		month = Integer.toString(cal.get(Calendar.MONTH)+1);
		day = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
		hour = Integer.toString(cal.get(Calendar.HOUR_OF_DAY));
		minute = Integer.toString(cal.get(Calendar.MINUTE));
		button_cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				File imageFileFolder2 = new File(Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES),"Pictever");
				imageFileFolder2.mkdir();
				File imageFileName2 = new File(imageFileFolder2,photo_path);
				Boolean deleted = imageFileName2.delete();
				context.deleteFile(photo_path);
				if (deleted)
					Log.v(TAG,"photo file deleted at : "+ imageFileName2.getAbsolutePath());
				finish();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(edit_message.getWindowToken(), 
						InputMethodManager.RESULT_UNCHANGED_SHOWN);
			}
		});
		button_color.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				counter+=1;
				change_color();
			}
		});
		if (from.equals("camera")) {
			int surface_width = controller.prefs.getInt(controller.CAMERA_FACING +
					"_display_height",controller.SCREEN_HEIGHT);
			int surface_height = controller.prefs.getInt(controller.CAMERA_FACING +
					"_display_width",controller.SCREEN_WIDTH);
			Log.v(TAG,"surface WxH = " + Integer.toString(surface_width) + "x" + Integer.toString(surface_height));
			display_image.setImageBitmap(bmp);
			float xpreview = (float) (surface_width - controller.SCREEN_HEIGHT);
			float ypreview = (float) (surface_height-controller.SCREEN_WIDTH);
			Log.v(TAG,"set left and top :" + Float.toString(xpreview) +" , " + Float.toString(ypreview));
			display_image.setPadding((int) Math.round(xpreview-1),(int) Math.round(ypreview-1),0,0);
			display_image.setScaleType(ScaleType.CENTER_CROP);
		}
		else {
			display_image.setImageBitmap(bmp);
			display_image.setScaleType(ScaleType.FIT_CENTER);
		}

		// BUTTON CENTRAL
		button_send_future.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (rl_popup.getVisibility()==View.VISIBLE) {
					// POPUP IS VISIBLE
					if (listView_contacts.getVisibility()==View.VISIBLE) {
						// CONTACTS ARE VISIBLE -> SHOW DATE !
						calendarView.setVisibility(View.INVISIBLE);
						time_picker.setVisibility(View.INVISIBLE);
						tvTimezone.setVisibility(View.INVISIBLE);
						listView_contacts.setVisibility(View.INVISIBLE);
						tv_title_popup.setText(dateTitle);
						tv_title_popup.setTextSize(30);
						tv_title_popup.setTypeface(font);
						listView_times.setVisibility(View.VISIBLE);
						button_send_future.setImageResource(R.drawable.back);
					}
					else {
						if (listView_times.getVisibility()==View.VISIBLE) {
							// DATES ARE VISIBLE -> BACK TO CONTACTS
							listView_contacts.setVisibility(View.VISIBLE);
							tv_title_popup.setText(contactsTitle);
							tv_title_popup.setTextSize(30);
							tv_title_popup.setTypeface(font);
							listView_times.setVisibility(View.INVISIBLE);
							calendarView.setVisibility(View.INVISIBLE);
							time_picker.setVisibility(View.INVISIBLE);
							tvTimezone.setVisibility(View.INVISIBLE);
							button_send_future.setImageResource(R.drawable.next);
						}
						if (calendarView.getVisibility()==View.VISIBLE) {
							if (!list_ids_selected.isEmpty()) {
								ConnectivityManager cm = (ConnectivityManager) 
										context.getSystemService(Context.CONNECTIVITY_SERVICE);
								if (cm!=null && cm.getActiveNetworkInfo()!=null 
										&& cm.getActiveNetworkInfo().isConnected()) {
									if (controller.upload_is_active == "no") {
										// CALENDAR IS VISIBLE -> SEND IN THE FUTURE
										message = edit_message.getText().toString();
										rl_popup.setVisibility(View.INVISIBLE);
										button_cancel.setVisibility(View.INVISIBLE);
										button_send_future.setVisibility(View.INVISIBLE);
										button_color.setVisibility(View.INVISIBLE);
										((TextView) findViewById(R.id.tapToAddaText)).setVisibility(View.INVISIBLE);
										Handler chandler = new Handler();
										chandler.postDelayed(new Runnable() {
											public void run() {
												View main_view = ((RelativeLayout) findViewById(
														R.id.display_relative_layout));
												main_view.setDrawingCacheEnabled(true);
												bmp = Bitmap.createBitmap(main_view.getDrawingCache());
												main_view.setDrawingCacheEnabled(false);
												Log.v(TAG,"bitmap created");
												int size = bmp.getWidth()*bmp.getHeight() ;
												int limit_size = 500000;
//												if (size>limit_size) {
//													int bmp_width = bmp.getWidth();
//													int bmp_height = bmp.getHeight();
//													bmp_width = (int) Math.round(
//															bmp_width*Math.sqrt(limit_size)/Math.sqrt(size));
//													bmp_height = (int) Math.round(
//															(float)bmp_height*Math.sqrt(limit_size)/Math.sqrt(size));
//													bmp = Bitmap.createScaledBitmap(bmp,bmp_width,bmp_height, false);
//													Log.v(TAG,"bitmap resized");
//												}
												File imageFileFolder = new File(
														Environment.getExternalStoragePublicDirectory(
																Environment.DIRECTORY_PICTURES),"Pictever");
												imageFileFolder.mkdir();
												FileOutputStream out = null;
												File imageFileName = new File(imageFileFolder, photo_path);
												try {
													out = new FileOutputStream(imageFileName);
													bmp.compress(Bitmap.CompressFormat.JPEG,100, out);
													out.flush();
													out.close();
													out = null;
												} catch (Exception e) {
													e.printStackTrace();
												}
												JSONArray json_ids = new JSONArray(list_ids_selected);
												contact_ids = json_ids.toString();
												controller.last_photo_path = photo_path;
												controller.last_contact_ids = contact_ids;
												controller.last_send_choice = send_choice;
												controller.last_list_ids_selected = list_ids_selected;
												controller.upload_is_active = "yes";
												finish();
											}
										}, 100);
									}
									else {
										Toast.makeText(context,"Please wait for the previous picture to be sent.",
												Toast.LENGTH_SHORT).show();
									}
								}
								else {
									Toast.makeText(context,"Not enough network right now. Please try again later",
											Toast.LENGTH_SHORT).show();
									button_cancel.setVisibility(View.VISIBLE);
									button_send_future.setVisibility(View.VISIBLE);
								}
							}
							else {
								Toast.makeText(context,"Please select at least one contact ;)",
										Toast.LENGTH_SHORT).show();
							}
						}
					}
				}
				else {
					if (adapter_contacts==null)
						adapter_contacts = new AdapterContacts(context,localContacts);
					listView_contacts.setAdapter(adapter_contacts);
					Animation anim = AnimationUtils.loadAnimation(context,R.animator.animation_enter_on_top);
					rl_popup.startAnimation(anim);
					rl_popup.setVisibility(View.VISIBLE);
					rl_popup.setEnabled(true);
					tv_title_popup.setText(contactsTitle);
					tv_title_popup.setTextSize(30);
					tv_title_popup.setTypeface(font);
					listView_times.setVisibility(View.INVISIBLE);
					calendarView.setVisibility(View.INVISIBLE);
					time_picker.setVisibility(View.INVISIBLE);
					tvTimezone.setVisibility(View.INVISIBLE);
					listView_contacts.setVisibility(View.VISIBLE);	
					button_send_future.setImageResource(R.drawable.next);
					controller.sortSendChoices(controller.listSendChoices);
					listView_times.setAdapter(new AdapterTimes(context, controller.listSendChoices));
				}
			}
		});

		// SET UP CONTACT PICKER
		listView_contacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				JSONObject json_contact;
				String is_selected="";
				try {
					json_contact = new JSONObject(localContacts.get(position));
					is_selected = json_contact.getString("is_selected");
					if (is_selected.equals("unselected")) {
						is_selected = "selected";
						json_contact.put("is_selected", is_selected);
						list_ids_selected.add(json_contact.getString("contact_id"));
						localContacts.set(position, json_contact.toString());
					}
					else {
						list_ids_selected.remove(json_contact.getString("contact_id"));
						is_selected = "unselected";
						json_contact.put("is_selected",is_selected);
						localContacts.set(position, json_contact.toString());
					}
					controller.sortContacts(localContacts);
					adapter_contacts = new AdapterContacts(context,localContacts);
					listView_contacts.setAdapter(adapter_contacts);
					listView_contacts.setSelectionFromTop(position, (int)view.getY());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		// SET UP DATE PICKER
		listView_times.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {	
				if (position!=0) {
					if (!list_ids_selected.isEmpty()) {
						ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
								Context.CONNECTIVITY_SERVICE);
						if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
							if (controller.upload_is_active == "no") {
								Log.v(TAG, "upoload_not_active");
								view.setBackgroundResource(R.color.MidnightBlue);
								Log.v(TAG, "after midnight blue");
								listView_times.setVisibility(View.INVISIBLE);
								listView_contacts.setVisibility(View.INVISIBLE);
								rl_popup.setVisibility(View.INVISIBLE);
								button_cancel.setVisibility(View.INVISIBLE);
								button_send_future.setVisibility(View.INVISIBLE);
								button_color.setVisibility(View.INVISIBLE);
								((TextView) findViewById(R.id.tapToAddaText)).setVisibility(View.INVISIBLE);
								pos = position;
								Handler handler = new Handler();
								handler.postDelayed(new Runnable() {
									public void run() {
										try {
											JSONObject json_send_choice = new JSONObject(
													controller.listSendChoices.get(pos));
											JSONObject json_send_choice_to_send = new JSONObject();
											json_send_choice_to_send.put("type",json_send_choice.getString("key"));
											controller.last_send_label = json_send_choice.getString("send_label");
											String timezone = Integer.toString((int) TimeUnit.MILLISECONDS.toHours(
													(long)TimeZone.getDefault().getRawOffset()));
											json_send_choice_to_send.put("timezone",timezone);
											send_choice = json_send_choice_to_send.toString();						
										} catch (JSONException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										Log.v(TAG,"after send_choice");
										View main_view = ((RelativeLayout) findViewById(R.id.display_relative_layout));
										main_view.setDrawingCacheEnabled(true);
										bmp = Bitmap.createBitmap(main_view.getDrawingCache());
										main_view.setDrawingCacheEnabled(false);
										Log.v(TAG,"bitmap created");
										int size = bmp.getWidth()*bmp.getHeight() ;
										int limit_size = 500000;
//										if (size>limit_size) {
//											int bmp_width = bmp.getWidth();
//											int bmp_height = bmp.getHeight();
//											bmp_width = (int) Math.round(
//													bmp_width*Math.sqrt(limit_size)/Math.sqrt(size));
//											bmp_height = (int) Math.round(
//													(float)bmp_height*Math.sqrt(limit_size)/Math.sqrt(size));
//											bmp = Bitmap.createScaledBitmap(bmp,bmp_width,bmp_height, false);
//											Log.v(TAG,"bitmap resized");
//										}

										File imageFileFolder = new File(Environment.getExternalStoragePublicDirectory(
												Environment.DIRECTORY_PICTURES),"Pictever");
										imageFileFolder.mkdir();
										FileOutputStream out = null;
										File imageFileName = new File(imageFileFolder, photo_path);
										try {
											out = new FileOutputStream(imageFileName);
											bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
											out.flush();
											out.close();
											out = null;
										} catch (Exception e) {
											e.printStackTrace();
										}
										Log.v(TAG,"bitmap stored");
										JSONArray json_ids = new JSONArray(list_ids_selected);
										contact_ids = json_ids.toString();
										controller.last_photo_path = photo_path;
										controller.last_contact_ids = contact_ids;
										controller.last_send_choice = send_choice;
										controller.last_list_ids_selected = list_ids_selected;
										controller.upload_is_active = "yes";
										finish();
									}
								}, 100);
							}
							else 
								Toast.makeText(context,"Please wait for the previous picture to be sent.",
										Toast.LENGTH_SHORT).show();
						}
						else
							Toast.makeText(context,"Not enough network right now. Please try again later",
									Toast.LENGTH_SHORT).show();
					}
					else 
						Toast.makeText(context,"Please select at least one contact ;)",Toast.LENGTH_SHORT).show();
				}
				else {
					tv_title_popup.setText("Choose from the calendar");
					tv_title_popup.setTextSize(30);
					tv_title_popup.setTypeface(font);
					listView_times.setVisibility(View.INVISIBLE);
					listView_contacts.setVisibility(View.INVISIBLE);
					calendarView.setVisibility(View.VISIBLE);
					time_picker.setVisibility(View.VISIBLE);
					tvTimezone.setVisibility(View.VISIBLE);
					JSONObject json_send_choice_to_send = new JSONObject();
					try {
						json_send_choice_to_send.put("type","calendar");
						controller.last_send_label = "calendar";
						String timezone = Integer.toString((int) TimeUnit.MILLISECONDS.toHours(
								(long)TimeZone.getDefault().getRawOffset()));
						json_send_choice_to_send.put("timezone",timezone);
						json_send_choice_to_send.put("parameters", Long.toString(
								TimeUnit.MILLISECONDS.toSeconds((new Date().getTime())+1)));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					send_choice = json_send_choice_to_send.toString();
					button_send_future.setImageResource(R.drawable.send_in_the_future_white);
					rl_popup.setBackgroundResource(R.color.DarkCyan);
					calendarView.setOnDateChangeListener(new OnDateChangeListener() {
						@Override
						public void onSelectedDayChange(CalendarView view, int cyear, int cmonth,
								int cdayOfMonth) {
							year = Integer.toString(cyear);
							month = Integer.toString(cmonth+1);
							day = Integer.toString(cdayOfMonth);
							hour = Integer.toString(time_picker.getCurrentHour());
							minute = Integer.toString(time_picker.getCurrentMinute());
							SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
							sdf.setTimeZone(TimeZone.getDefault());
							String datestring = year +"-"+month+"-"+ day +" "+ hour + ":" + minute + ":" + "00";
							Date date_to_send = new Date();
							try {
								date_to_send = sdf.parse(datestring);
							} catch (ParseException e) {
								e.printStackTrace();
							}
							long tempmillis = date_to_send.getTime();
							long seconds_from_1970 = TimeUnit.MILLISECONDS.toSeconds(tempmillis);
							JSONObject json_send_choice_to_send = new JSONObject();
							try {
								json_send_choice_to_send.put("type","calendar");
								String timezone = Integer.toString((int) TimeUnit.MILLISECONDS.toHours(
										(long)TimeZone.getDefault().getRawOffset()));
								json_send_choice_to_send.put("timezone",timezone);
								if (Long.toString(seconds_from_1970).isEmpty())
									json_send_choice_to_send.put("parameters", Long.toString(
											TimeUnit.MILLISECONDS.toSeconds((new Date().getTime())+1)));
								else
									json_send_choice_to_send.put("parameters", Long.toString(seconds_from_1970));

							} catch (JSONException e) {
								e.printStackTrace();
							}
							send_choice = json_send_choice_to_send.toString();
						}
					});
					time_picker.requestFocus();
					time_picker.setIs24HourView(true);
					time_picker.setOnTimeChangedListener(new OnTimeChangedListener() {
						@Override
						public void onTimeChanged(TimePicker view, int chourOfDay, int cminute) {
							hour = Integer.toString(chourOfDay);
							minute = Integer.toString(cminute);
							SimpleDateFormat sdf=  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
							sdf.setTimeZone(TimeZone.getDefault());
							String datestring = year +"-"+month+"-"+ day +" "+ hour + ":" + minute + ":" + "00";
							Date date_to_send = new Date();
							try {
								date_to_send = sdf.parse(datestring);
							} catch (ParseException e) {
								e.printStackTrace();
							}
							long tempmillis = date_to_send.getTime();
							long seconds_from_1970 = TimeUnit.MILLISECONDS.toSeconds(tempmillis);
							JSONObject json_send_choice_to_send = new JSONObject();
							try {
								json_send_choice_to_send.put("type","calendar");
								String timezone = Integer.toString((int) TimeUnit.MILLISECONDS.toHours(
										(long)TimeZone.getDefault().getRawOffset()));
								json_send_choice_to_send.put("timezone",timezone);
								if (Long.toString(seconds_from_1970).isEmpty())
									json_send_choice_to_send.put("parameters",Long.toString(
											TimeUnit.MILLISECONDS.toSeconds((new Date().getTime())+1)));
								else 
									json_send_choice_to_send.put("parameters", Long.toString(seconds_from_1970));
							} catch (JSONException e) {
								e.printStackTrace();
							}
							send_choice = json_send_choice_to_send.toString();
						}
					});
				}
			}
		});

		// EDIT MESSAGE ON CLICK
		display_image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((TextView) findViewById(R.id.tapToAddaText)).setVisibility(View.INVISIBLE);
				if (edit_message.getVisibility()==View.VISIBLE) {
					if (edit_message.getText().toString().equals("")) {
						edit_message.setVisibility(View.INVISIBLE);
					}
					edit_message.clearFocus();
					edit_message.setCursorVisible(false);
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(edit_message.getWindowToken(), 
							InputMethodManager.RESULT_UNCHANGED_SHOWN);
				}
				else {
					// SHOW EDIT MESSAGE
					Animation anim = AnimationUtils.loadAnimation(context,R.animator.animation_enter_on_top);
					edit_message.setAnimation(anim);
					edit_message.setVisibility(View.VISIBLE);
					button_color.setVisibility(View.VISIBLE);
					edit_message.setEnabled(true);
					edit_message.setAlpha((float) 1);
					Boolean b = edit_message.requestFocus();
					if (!b) {
						Log.v(TAG, "unable to receive focus");
					}
					edit_message.setCursorVisible(true);
					// SHOW KEYBOARD
					View decorView = getWindow().getDecorView();
					decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
							| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(edit_message, InputMethodManager.SHOW_IMPLICIT);
				}
				
				if (button_color.getVisibility()==View.VISIBLE) 
					button_color.setVisibility(View.INVISIBLE);
				else
					button_color.setVisibility(View.VISIBLE);
			}
		});
		edit_message.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (edit_message.getText().toString().isEmpty())
					edit_message.setAlpha((float)0.5);
				else
					edit_message.setAlpha((float) 1);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});

		// ON TOUCH EVENT
		edit_message.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				edit_message.setFocusable(false);
				edit_message.setFocusableInTouchMode(false);
				edit_message.setCursorVisible(false);
				final int Y = (int) event.getRawY();
				final int X = (int) event.getRawX();
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) 
					edit_message.getLayoutParams();
					yDelta = Y - lParams.topMargin;
					xDelta = X - lParams.leftMargin;
					time_start = (int) SystemClock.elapsedRealtime();
					break;
				case MotionEvent.ACTION_UP:
					time_end = (int) SystemClock.elapsedRealtime();
					int delta_time = time_end-time_start;
					if (delta_time < 150) {
						RelativeLayout.LayoutParams lParams2 = (RelativeLayout.LayoutParams) 
								edit_message.getLayoutParams();
						if (lParams2.topMargin > 400) {
							lParams2.topMargin = 390;
							edit_message.setLayoutParams(lParams2);
						}
						edit_message.performClick();
						edit_message.setCursorVisible(true);
						edit_message.setFocusable(true);
						edit_message.setFocusableInTouchMode(true);
						edit_message.requestFocus();
						View decorView = getWindow().getDecorView();
						decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.showSoftInput(edit_message,InputMethodManager.SHOW_IMPLICIT);
					}
					break;
				case MotionEvent.ACTION_POINTER_DOWN:
					break;
				case MotionEvent.ACTION_POINTER_UP:
					break;
				case MotionEvent.ACTION_MOVE:
					RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
					edit_message.getLayoutParams();
					layoutParams.topMargin = Y - yDelta;
					layoutParams.leftMargin = X - xDelta;
					layoutParams.rightMargin = -250;
					layoutParams.bottomMargin = -250;
					edit_message.setLayoutParams(layoutParams);
					break;
				}
				((RelativeLayout) findViewById(R.id.display_relative_layout)).invalidate();
				return false;
			}
		});
		((ImageView) findViewById(R.id.hide_popup)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				rl_popup.setVisibility(View.INVISIBLE);
				button_send_future.setImageResource(R.drawable.send_in_the_future_white);
			}
		});
	}
	
	public void change_color() {
		Log.v(TAG,"counter"+ mod(counter, 6));
		switch (mod(counter,6)) {
		case 0:
			button_color.setImageResource(R.drawable.round_orangekeo);
			edit_message.setBackgroundResource(R.drawable.edit_text_orangekeo);
			break;
		case 1:
			button_color.setImageResource(R.drawable.round_green);
			edit_message.setBackgroundResource(R.drawable.edit_text_green);
			break;
		case 2:
			button_color.setImageResource(R.drawable.round_gold);
			edit_message.setBackgroundResource(R.drawable.edit_text_gold);
			break;
		case 3:
			button_color.setImageResource(R.drawable.round_darkslateblue);
			edit_message.setBackgroundResource(R.drawable.edit_text_darkslateblue);
			break;
		case 4:
			button_color.setImageResource(R.drawable.round_red);
			edit_message.setBackgroundResource(R.drawable.edit_text_red);
			break;
		case 5:
			button_color.setImageResource(R.drawable.round_darkcyan);
			edit_message.setBackgroundResource(R.drawable.edit_text_darkcyan);
			break;
		}
		controller.editor = controller.prefs.edit();
		controller.editor.putInt("color_counter",counter);
		controller.editor.commit();
	}
	
	private int mod(int x, int y) {
	    int result = x % y;
	    if (result < 0)
	        result += y;
	    return result;
	}
}


