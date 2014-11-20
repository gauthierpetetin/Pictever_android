package com.pict.ever;

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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
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
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsEvent;

public class NewMessage extends PicteverActivity {

	ImageButton button_send_future;
	ArrayList<String> list_ids_selected,localContacts;
	ListView listView_times,listView_contacts;
	ListAdapter adapter_contacts;

	int xDelta,yDelta,time_start,time_end;
	String send_choice,message,year="2014",month="07",day="00",hour="00",minute="00";
	EditText edit_message;

	String TAG = "NewMessage",contactsTitle = " Who ? ", dateTitle = " When ? ";

	RelativeLayout rl_popup;
	CalendarView calendarView;
	TimePicker time_picker;
	TextView tv_title_popup,tvTimezone;
	ImageView background;
	Typeface font;
	Context context;
	Controller controller;

	@Override
	public void onBackPressed() {
		if (rl_popup.getVisibility()==View.VISIBLE) {
			rl_popup.setVisibility(View.INVISIBLE);
			button_send_future.setImageResource(R.drawable.send_in_the_future);
		}
		else {
			super.onBackPressed();
			this.overridePendingTransition(R.animator.animation_enter_on_right,R.animator.animation_leave_on_left);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(controller.analytics != null)
			controller.analytics.getSessionClient().pauseSession();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();
		context = NewMessage.this;

		if (controller.api < 17) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.newmessage);
		}
		else {
			setContentView(R.layout.newmessage);
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}

		//		RelativeLayout rl_newmessage = (RelativeLayout) findViewById(R.id.newmessage_relative_layout);
		//		rl_newmessage

		TextView txt = (TextView) findViewById(R.id.new_message_title);
		font = Typeface.createFromAsset(getAssets(), "gabriola.ttf");
		txt.setTextSize(30);
		txt.setTypeface(font);
		TextView txt2 = (TextView) findViewById(R.id.filkeo_title);
		txt2.setTextSize(30);
		txt2.setTypeface(font,Typeface.BOLD);

		txt2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		((ImageButton) findViewById(R.id.button_settings)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(NewMessage.this,Settings.class));
				NewMessage.this.overridePendingTransition(
						R.animator.animation_enter_on_bottom2,R.animator.do_not_move);
			}
		});

		edit_message = (EditText) findViewById(R.id.edit_newmessage);
		edit_message.setMaxLines(5);
		edit_message.setTextSize(21);
		edit_message.postDelayed(new Runnable() {
			@Override
			public void run() {
				edit_message.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(edit_message, 0);
			}
		}, 100);

		list_ids_selected = new ArrayList<String>(); 
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
	public void onResume() {
		super.onResume();
		if(controller.analytics != null) 
			controller.analytics.getSessionClient().resumeSession();
		display();
	}

	public void display() {

		controller.ll_progress = (LinearLayout) findViewById(R.id.ll_progress);
		rl_popup = (RelativeLayout) findViewById(R.id.rl_popup);
		controller.rl_title = (RelativeLayout) findViewById(R.id.rl_title);
		tv_title_popup = (TextView) findViewById(R.id.tv_title_popup);
		listView_times = (ListView) findViewById(R.id.listView_times);
		listView_contacts = (ListView) findViewById(R.id.list_pick_contact);
		listView_contacts.setFastScrollEnabled(true);
		calendarView = (CalendarView) findViewById(R.id.calendarView);
		tvTimezone  = (TextView) findViewById(R.id.help_timezone);
		time_picker = (TimePicker) findViewById(R.id.timePicker);
		time_picker.setIs24HourView(true);
		button_send_future = (ImageButton) findViewById(R.id.button_send_future);
		background = (ImageView) findViewById(R.id.new_message_background);
		//		background.setOnTouchListener(new OnSwipeTouchListener(){
		//		    public void onSwipeLeft() {
		//				startActivity(new Intent(NewMessage.this,CameraActivity.class));
		//				NewMessage.this.overridePendingTransition(
		//						R.animator.animation_enter_on_right,R.animator.animation_leave_on_left);
		//				finish();
		//		    }
		//		});

		Calendar cal = Calendar.getInstance();
		year = Integer.toString(cal.get(Calendar.YEAR));
		month = Integer.toString(cal.get(Calendar.MONTH)+1);
		day = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
		hour = Integer.toString(cal.get(Calendar.HOUR_OF_DAY));
		minute = Integer.toString(cal.get(Calendar.MINUTE));

		if (controller.upload_is_active.equals("yes")) {
			if (controller.upload!=null) {
				controller.ll_progress.setVisibility(View.VISIBLE);
				controller.ll_progress_params = new RelativeLayout.LayoutParams(
						Math.round((float) controller.uploadProgression/100*
								controller.prefs.getInt("SCREEN_HEIGHT",0)),
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
				if (controller.rl_title!=null) {
					controller.ll_progress_params.addRule(RelativeLayout.BELOW,controller.rl_title.getId());
				}
				controller.ll_progress.setLayoutParams(controller.ll_progress_params);

			}
			else {
				controller.ll_progress.setVisibility(View.INVISIBLE);
			}
		}

		// BUTTON CENTRAL
		button_send_future.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				View decorView = getWindow().getDecorView();
				decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
				edit_message.clearFocus();
				edit_message.setCursorVisible(false);

				if (edit_message.getText().toString().isEmpty()) {
					Toast.makeText(context,"Please enter a message",Toast.LENGTH_SHORT).show();
				}
				else {
					if (rl_popup.getVisibility()==View.VISIBLE) {
						// POPUP IS VISIBLE
						if (listView_contacts.getVisibility()==View.VISIBLE) {
							// CONTACTS ARE VISIBLE -> SHOW DATE !
							calendarView.setVisibility(View.INVISIBLE);
							tvTimezone.setVisibility(View.INVISIBLE);
							time_picker.setVisibility(View.INVISIBLE);
							listView_contacts.setVisibility(View.INVISIBLE);
							tv_title_popup.setText(dateTitle);
							tv_title_popup.setTextSize(30);
							tv_title_popup.setTypeface(font);
							listView_times.setVisibility(View.VISIBLE);
							button_send_future.setImageResource(R.drawable.back_cyan);
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
								button_send_future.setImageResource(R.drawable.next_cyan);
							}
							if (calendarView.getVisibility()==View.VISIBLE) {
								// CALENDAR IS VISIBLE -> SEND IN THE FUTURE
								message = edit_message.getText().toString();
								if (!list_ids_selected.isEmpty()) {
									JSONArray json_receiver_ids = new JSONArray(list_ids_selected);
									ConnectivityManager cm = (ConnectivityManager) 
											context.getSystemService(Context.CONNECTIVITY_SERVICE);

									if (cm!=null && cm.getActiveNetworkInfo()!=null && 
											cm.getActiveNetworkInfo().isConnected()) {
										if(controller.analytics != null) {
											AnalyticsEvent sendTextMessageEvent = 
													controller.analytics.getEventClient().createEvent("androidTextMessageSent");
											sendTextMessageEvent.addAttribute("number_of_receivers",
													Integer.toString(json_receiver_ids.length()));
											sendTextMessageEvent.addAttribute("send_label",
													controller.last_send_label);
											//Record the Level Complete event
											controller.analytics.getEventClient().recordEvent(sendTextMessageEvent);
											controller.analytics.getEventClient().submitEvents();
										}
										controller.sendMessage(message,"",json_receiver_ids.toString(),send_choice);
										for (String id_selected : list_ids_selected) {
											int times_contacted = controller.prefs.getInt(
													id_selected+"_times_contacted", 0);
											controller.editor = controller.prefs.edit();
											controller.editor.putInt(id_selected+"_times_contacted", 
													times_contacted +1);
											Log.v(TAG,id_selected + "_times_contacted : "
													+ controller.prefs.getInt(id_selected+ "_times_contacted", 0));
											if (id_selected.startsWith("num")){
												int contacts_invited = controller.prefs.getInt("contacts_invited", 0);
												controller.editor.putInt("contacts_invited", contacts_invited+1);
												SmsManager smsManager = SmsManager.getDefault();
												smsManager.sendTextMessage(id_selected.substring(3), null, 
														"I just sent you a message in the future on Pictever!" +
																" To get the app and see the message: http://pictever.com"
																, null, null);
											}
											controller.editor.commit();
										}
										finish();
									}
									else
										Toast.makeText(context,"No network right now. Please try again later",
												Toast.LENGTH_SHORT).show();
								} 
								else 
									Toast.makeText(context,"Please select at least one contact ;)",
											Toast.LENGTH_SHORT).show();
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
						button_send_future.setImageResource(R.drawable.next_cyan);
						controller.sortSendChoices(controller.listSendChoices);
						listView_times.setAdapter(new AdapterTimes(context,controller,controller.listSendChoices));
					}
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
						String cid = json_contact.getString("contact_id");
						Log.v(TAG,cid + "_times_contacted : " + controller.prefs.getInt(cid+ "_times_contacted", 0));
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
					try {
						JSONObject json_send_choice = new JSONObject(controller.listSendChoices.get(position));
						if ((json_send_choice.getString("key").equals("53b95c02edb08c3d6885041f")
								|| json_send_choice.getString("key").equals("53b95c02edb08c3d68850420")) &&
								controller.prefs.getInt("contacts_invited",0) < 3) {
							Log.v(TAG,"contacts invited" + controller.prefs.getInt("contacts_invited",0));
							new AlertDialog.Builder(context)
							.setTitle("Want to unlock this future time ?")
							.setMessage("Just send a message to 3 friends that are not on Pictever ;)")
							.setNeutralButton("Ok cool!", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.show();
						}
						else {
							JSONObject json_send_choice_to_send = new JSONObject();
							json_send_choice_to_send.put("type",json_send_choice.getString("key"));
							controller.last_send_label = json_send_choice.getString("send_label");
							String timezone = Integer.toString((int) TimeUnit.MILLISECONDS.toHours(
									(long)TimeZone.getDefault().getRawOffset()));
							json_send_choice_to_send.put("timezone",timezone);
							send_choice = json_send_choice_to_send.toString();	
							message = edit_message.getText().toString();
							if (!list_ids_selected.isEmpty()) {
								view.setBackgroundResource(R.color.MidnightBlue);
								JSONArray json_receiver_ids = new JSONArray(list_ids_selected);
								ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
										Context.CONNECTIVITY_SERVICE);
								if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
									if(controller.analytics != null) {
										AnalyticsEvent sendTextMessageEvent = 
												controller.analytics.getEventClient().createEvent("androidTextMessageSent");
										sendTextMessageEvent.addAttribute("number_of_receivers", 
												Integer.toString(json_receiver_ids.length()));
										sendTextMessageEvent.addAttribute("send_label",controller.last_send_label);
										//Record the Level Complete event
										controller.analytics.getEventClient().recordEvent(sendTextMessageEvent);
										controller.analytics.getEventClient().submitEvents();
									}
									controller.sendMessage(message,"",json_receiver_ids.toString(),send_choice);
									for (String id_selected : list_ids_selected) {
										int times_contacted = controller.prefs.getInt(id_selected+"_times_contacted", 0);
										controller.editor = controller.prefs.edit();
										controller.editor.putInt(id_selected+"_times_contacted", times_contacted +1);
										Log.v(TAG,id_selected + "_times_contacted : " + controller.prefs.getInt(
												id_selected+ "_times_contacted", 0));
										if (id_selected.startsWith("num")){
											int contacts_invited = controller.prefs.getInt("contacts_invited", 0);
											controller.editor.putInt("contacts_invited", contacts_invited+1);
											SmsManager smsManager = SmsManager.getDefault();
											smsManager.sendTextMessage(id_selected.substring(3), null,
													"I just sent you a message in the future on Pictever!" +
															" To get the app and see the message : http://pictever.com", null, null);
										}
										controller.editor.commit();
									}
									rl_popup.setVisibility(View.INVISIBLE);
									button_send_future.setVisibility(View.INVISIBLE);
									listView_times.setVisibility(View.INVISIBLE);
									listView_contacts.setVisibility(View.INVISIBLE);
									finish();
								}
								else {
									Toast.makeText(context,"No network right now. Please try again later",
											Toast.LENGTH_SHORT).show();
									view.setBackgroundResource(R.color.DarkCyan);
								}
							}
							else 
								Toast.makeText(context,"Please select at least one contact ;)",Toast.LENGTH_SHORT).show();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				else {
					listView_times.setVisibility(View.INVISIBLE);
					listView_contacts.setVisibility(View.INVISIBLE);
					tv_title_popup.setText("Choose date and time");
					tv_title_popup.setTextSize(30);
					tv_title_popup.setTypeface(font);
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					send_choice = json_send_choice_to_send.toString();
					button_send_future.setImageResource(R.drawable.send_in_the_future);
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
								Log.v(TAG,timezone);
								json_send_choice_to_send.put("timezone",timezone);
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

		background.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				edit_message.clearFocus();
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(edit_message.getWindowToken(), 
						InputMethodManager.RESULT_UNCHANGED_SHOWN);
			}
		});

		((ImageView) findViewById(R.id.hide_popup)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				rl_popup.setVisibility(View.INVISIBLE);
				button_send_future.setImageResource(R.drawable.send_in_the_future);
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
			public void afterTextChanged(Editable s) {
			}
		});

		edit_message.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				edit_message.setCursorVisible(true);
			}
		});

	}
}