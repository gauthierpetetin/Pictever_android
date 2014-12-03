package com.pict.ever;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsEvent;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;

public class Timeline extends PicteverActivity {

	public static final String TAG = "Timeline";
	String[] message,receive_label,contact_name,uri_icon,photo_id,created_at,received_date,receive_color;
	ImageView ivPicture, fullscreen;
	Controller controller;
	View view;
	int initial_scroll;
	ImageButton fb_button_pressed;
	Bitmap[] bmp;
	Bitmap bmp_to_post;
	String message_to_post="",text_to_post="Today on Pictever!";
	ScrollViewExt scrollview;
	Context context;
	int k=0;
	SwipeRefreshLayout swipeLayout;
	Boolean do_initial_scroll=true;
	float X=0,Y=0;
	LinearLayout llFilMessage;
	Typeface font;
	private UiLifecycleHelper uiHelper;

	@Override
	public void onBackPressed() {
		if (fullscreen.getVisibility()==View.VISIBLE){
			fullscreen.setVisibility(View.INVISIBLE);
			TextView tvMessage = (TextView) findViewById(R.id.tvShowMessage);
			tvMessage.setVisibility(View.INVISIBLE);
		}
		else {
			if (controller.from_notif) {
				Log.v(TAG, "from notif");
				controller.from_notif=false;
				startActivity(new Intent(context, CameraActivity.class));
				this.overridePendingTransition(R.animator.animation_enter_on_left,
						R.animator.animation_leave_on_right);
				finish();
			}
			else {
				controller.from_notif=false;
				super.onBackPressed();
				this.overridePendingTransition(R.animator.animation_enter_on_left,
						R.animator.animation_leave_on_right);
			}
		}
	}

	@Override
	public void onWindowFocusChanged(boolean focus) {
		super.onWindowFocusChanged(focus);
		llFilMessage = (LinearLayout) findViewById(R.id.llFilMessage);
		initial_scroll =  (int) llFilMessage.getY();
		scrollview = (ScrollViewExt) findViewById(R.id.scrollFilMessage);
		if (llFilMessage!=null && do_initial_scroll) {
			scrollview.scrollTo(0,initial_scroll);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = Timeline.this;
		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				do_initial_scroll = false;				
			}
		}, 2000);

		uiHelper = new UiLifecycleHelper(this, null);
		uiHelper.onCreate(savedInstanceState);

		if (controller.api < 17) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.timeline);
		}
		else {
			setContentView(R.layout.timeline);
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}
		
		swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
	    swipeLayout.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				if(controller.analytics != null) {
					AnalyticsEvent resendMessageEvent = controller.analytics.
							getEventClient().createEvent("androidActualizeTimeline");
					controller.analytics.getEventClient().recordEvent(resendMessageEvent);
					controller.analytics.getEventClient().submitEvents();
				}
				controller.receive_all();
			}
		});
	    swipeLayout.setColorSchemeResources(R.color.OrangeKeo,R.color.Red,R.color.Goldenrod,R.color.Orange);
		controller.rl_title = (RelativeLayout) findViewById(R.id.rl_title);
		TextView txt2 = (TextView) findViewById(R.id.tvWannaKnow);
		TextView quick = (TextView) findViewById(R.id.quick);
		font = Typeface.createFromAsset(getAssets(), "gabriola.ttf");
		txt2.setTextSize(24);
		txt2.setTypeface(font,Typeface.NORMAL);
		quick.setTextSize(24);
		quick.setTypeface(font,Typeface.NORMAL);
//		TextView timeline_title = (TextView) findViewById(R.id.timeline_title);
//		timeline_title.setTextSize(30);
//		timeline_title.setTypeface(font);
		display();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
			@Override
			public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
				Log.e("Activity", String.format("Error: %s", error.toString()));
			}

			@Override
			public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
				Log.i("Activity", "Success!");
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();	
		uiHelper.onPause();
		NotificationManager nma = (NotificationManager) getSystemService("notification");
		nma.cancelAll();
		if(controller.analytics != null) {
			controller.analytics.getEventClient().submitEvents();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
		if(controller.analytics != null)  {
			controller.analytics.getSessionClient().resumeSession();
		}
		controller.ll_progress = (LinearLayout) findViewById(R.id.ll_progress);
		if (controller.upload_is_active.equals("yes")) {
			if (controller.upload!=null) {
				controller.ll_progress.setVisibility(View.VISIBLE);
				controller.ll_progress_params = new RelativeLayout.LayoutParams(
						Math.round((float) controller.uploadProgression/100*
								controller.prefs.getInt("SCREEN_HEIGHT",0)),
						Math.round((float) controller.prefs.getInt("SCREEN_WIDTH", 500)/100));
				if (controller.rl_title!=null) {
					controller.ll_progress_params.addRule(RelativeLayout.BELOW,controller.rl_title.getId());
				}
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

		ImageView panda_top = (ImageView) findViewById(R.id.ivPandaTop);
		panda_top.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				ImageView panda_top = (ImageView) findViewById(R.id.ivPandaTop);
				panda_top.setImageResource(R.drawable.panda_parle);
				if(controller.analytics != null) {
					AnalyticsEvent resendMessageEvent = controller.analytics.
							getEventClient().createEvent("androidWannaKnow");
					controller.analytics.getEventClient().recordEvent(resendMessageEvent);
				}
				controller.tvCounter = (TextView) findViewById(R.id.shyfts_waiting2);
				controller.tvCounter.setText(controller.prefs.getString("number_of_future_messages","0") + " !");
				ConnectivityManager cm = (ConnectivityManager) Timeline.this.getSystemService(
						Context.CONNECTIVITY_SERVICE);
				if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
					controller.get_number_of_future_messages();
				}
				controller.tvCounter.setVisibility(View.VISIBLE);
				controller.tvCounter.postDelayed(new Runnable() {
					@Override
					public void run() {
						controller.tvCounter.setVisibility(View.INVISIBLE);
						ImageView panda_top = (ImageView) findViewById(R.id.ivPandaTop);
						panda_top.setImageResource(R.drawable.panda_top_timeline);
					}
				}, 1500);
			}
		});
	}
	
	@Override
	public void onContentChanged() {
		Log.v(TAG,"oncontentchanged");
		display();
		super.onContentChanged();
	}

	public void display() {
		k=0;
//		((ImageButton) findViewById(R.id.button_settings)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if(controller.analytics != null) {
//					AnalyticsEvent resendMessageEvent = controller.analytics.
//							getEventClient().createEvent("androidSettingsFromTimeline");
//					controller.analytics.getEventClient().recordEvent(resendMessageEvent);
//					controller.analytics.getEventClient().submitEvents();
//				}
//				startActivity(new Intent(Timeline.this,Settings.class));
//				Timeline.this.overridePendingTransition(R.animator.animation_enter_on_bottom2,R.animator.do_not_move);
//			}
//		});
		llFilMessage = (LinearLayout) findViewById(R.id.llFilMessage);
		llFilMessage.removeAllViewsInLayout();

		fullscreen = (ImageView) findViewById(R.id.filkeo_fullscreen);
		fullscreen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				fullscreen.setVisibility(View.INVISIBLE);
				TextView tvMessage = (TextView) findViewById(R.id.tvShowMessage);
				tvMessage.setVisibility(View.INVISIBLE);
			}
		});

		if (controller.listMessages==null) {
			Set<String> setMessages = controller.prefs.getStringSet("set_messages", new HashSet<String>());
			controller.listMessages = new ArrayList<String>(setMessages);
		}
		controller.sortMessages(controller.listMessages);
		if (controller.listMessages.size() > 1) 
			((RelativeLayout) findViewById(R.id.rlPandaBottom)).setVisibility(View.VISIBLE);
		else 
			((RelativeLayout) findViewById(R.id.rlPandaBottom)).setVisibility(View.GONE);
		if (controller.listMessages.size() < 1) {
			((TextView) findViewById(R.id.no_messages_yet)).setVisibility(View.VISIBLE);
		}
		else
			((TextView) findViewById(R.id.no_messages_yet)).setVisibility(View.INVISIBLE);

		receive_label = new String[controller.listMessages.size()];
		receive_color = new String[controller.listMessages.size()];
		message = new String[controller.listMessages.size()];
		created_at = new String[controller.listMessages.size()];
		received_date = new String[controller.listMessages.size()];
		photo_id = new String[controller.listMessages.size()];
		contact_name = new String[controller.listMessages.size()];
		uri_icon = new String[controller.listMessages.size()];
		bmp = new Bitmap[controller.listMessages.size()];

		controller.extractMessagesInfo(receive_color,received_date,receive_label,
				created_at,message, photo_id, contact_name, uri_icon);

		addMessagesToGallery(llFilMessage,0,Math.min(5,controller.listMessages.size()));
	}

	@SuppressLint("NewApi")
	public void addMessagesToGallery(LinearLayout linlay_filkeo, int start, int end) {

		for (int i = start; i < end ; i++) {

			RelativeLayout rl_item = new RelativeLayout(this);
			LinearLayout.LayoutParams rl_item_params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			if (i!=end-1)
				rl_item_params.setMargins(0,0,0,15);
			rl_item.setId(k);
			rl_item.setBackgroundResource(R.color.White);
			linlay_filkeo.addView(rl_item, i, rl_item_params);

			// RL HEADER
			RelativeLayout rl_header = new RelativeLayout(this);
			RelativeLayout.LayoutParams rl_header_params = new RelativeLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);		
			rl_header.setPadding(10,0,0,0);
			rl_header.setId(k+1);
			rl_header.setBackgroundResource(R.color.White);
			rl_item.addView(rl_header, 0, rl_header_params);

			// CONTACT ICON
			ImageView ivContactPicture = new ImageView(this);
			RelativeLayout.LayoutParams contact_picture_params = new RelativeLayout.
					LayoutParams(70,70);
			contact_picture_params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			if (!uri_icon[i].isEmpty()) {
				if (uri_icon[i].equals("my_keo_image")) {
					ivContactPicture.setImageResource(R.drawable.my_keo_image);
				}
				else {
					ivContactPicture.setImageURI(Uri.parse(uri_icon[i]));
				}
			}
			else {
				ivContactPicture.setImageResource(R.drawable.button_contacts);
			}
			ivContactPicture.setId(k+2);
			rl_header.addView(ivContactPicture,contact_picture_params);

			// CONTACT NAME
			TextView tvContactName = new TextView(this);
			RelativeLayout.LayoutParams contact_name_params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			contact_name_params.addRule(RelativeLayout.RIGHT_OF,ivContactPicture.getId());
			contact_name_params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			contact_name_params.setMargins(20, 0, 0, 0);
			tvContactName.setText(contact_name[i]);
			tvContactName.setTextAppearance(this,android.R.style.TextAppearance_Medium);
			tvContactName.setTextColor(getResources().getColor(R.color.DarkCyan));
			tvContactName.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
			rl_header.addView(tvContactName,contact_name_params);

//			// DATE OF RECEPTION
//			TextView tvReceivedDate = new TextView(this);
//			RelativeLayout.LayoutParams received_date_params = new RelativeLayout.LayoutParams(
//					RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
//			received_date_params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
//			received_date_params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
//			received_date_params.setMargins(0, 0, 5, 0);
//			tvReceivedDate.setText(received_date[i]);
//			tvReceivedDate.setTextAppearance(this, android.R.style.TextAppearance_Small);
//			tvReceivedDate.setTextColor(getResources().getColor(R.color.black_overlay));
//			tvReceivedDate.setTypeface(Typeface.DEFAULT);
//			rl_header.addView(tvReceivedDate ,received_date_params);
			
			TextView tvSendChoice = new TextView(this);
			RelativeLayout.LayoutParams tvSendChoice_params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			tvSendChoice_params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			tvSendChoice_params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			tvSendChoice.setText(receive_label[i]);
			tvSendChoice.setTypeface(font,Typeface.BOLD);
			tvSendChoice.setPadding(20, -2, 20, -2);
			tvSendChoice.setTextSize(25);
			tvSendChoice.setId(k+5);
			tvSendChoice.setBackgroundColor(controller.which_color(receive_color[i]));
			tvSendChoice.setTextColor(getResources().getColor(R.color.White));
			rl_header.addView(tvSendChoice,tvSendChoice_params);


			RelativeLayout rl_picture = new RelativeLayout(this);
			RelativeLayout.LayoutParams rl_picture_params = new RelativeLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);	
			rl_picture_params.addRule(RelativeLayout.BELOW,rl_header.getId());
			rl_picture.setId(k+3);
			rl_item.addView(rl_picture, 0, rl_picture_params);			

			if (!photo_id[i].isEmpty()) {
				String clean_path = (photo_id[i].replaceAll(":","")).replaceAll("-", "");
				bmp[i] = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES) + "/Pictever/" + clean_path);
				if (bmp[i]==null) {
					Log.v(TAG,"clean path not working");
					bmp[i] = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(
							Environment.DIRECTORY_PICTURES) + "/Pictever/" + photo_id[i]);
				}
				ivPicture = new ImageView(this);
				RelativeLayout.LayoutParams ivPicture_params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,800);
				ivPicture.setScaleType(ScaleType.CENTER);
				if (bmp[i]!=null){
					if (bmp[i].getWidth()!=controller.prefs.getInt("SCREEN_HEIGHT",0)) {
						Log.v(TAG,"RESIZE PHOTO FROM" + contact_name[i]);
						double ratio = (double) bmp[i].getWidth()/bmp[i].getHeight();
						int desired_image_width = controller.prefs.getInt("SCREEN_HEIGHT",0);
						int desired_image_height = (int) Math.round((double) desired_image_width/ratio);
						bmp[i] = Bitmap.createScaledBitmap(bmp[i], desired_image_width,desired_image_height,false);
						File imageFileFolder = new File(Environment.getExternalStoragePublicDirectory(
								Environment.DIRECTORY_PICTURES),"Pictever");
						imageFileFolder.mkdir();
						File file_to_store_picture = new File(imageFileFolder,clean_path);
						FileOutputStream out = null;
						try {
							out = new FileOutputStream(file_to_store_picture);
							bmp[i].compress(Bitmap.CompressFormat.PNG, 100, out);
							out.flush();
							out.close();
							out = null;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					ivPicture.setImageBitmap(bmp[i]);
				}
				else {
					// add message[i] to the error_download_box
					ivPicture.setImageResource(R.drawable.image_not_available2);
				}
				ivPicture.setId(k+4);
				ivPicture.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						view = v ;
						int j = (view.getId()-4)/10;
						if (bmp[(int)Math.floor(j)]==null) {
							new AlertDialog.Builder(context) 
							//set message, title, and icon
							.setTitle("Retry to download this picture ?") 
							.setPositiveButton("Download", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) { 
									int j = (view.getId()-4)/10;
									ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
											Context.CONNECTIVITY_SERVICE);
									if (cm!=null && cm.getActiveNetworkInfo()!=null && 
											cm.getActiveNetworkInfo().isConnected()) {
										controller.photo_path = photo_id[(int)Math.floor(j)];
										controller.is_retry_download = true;
										controller.downloadCloudFront();
									}
									else {
										Toast.makeText(context,"There is not enough network right now. " +
												"Please try again later",Toast.LENGTH_SHORT).show();
									}
									dialog.dismiss();
								}   
							})
							.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.show();
						}
						else {
							fullscreen.setImageBitmap(bmp[(int)Math.floor(j)]);
							fullscreen.setVisibility(View.VISIBLE);
							TextView tvMessage = (TextView) findViewById(R.id.tvShowMessage);
							tvMessage.setVisibility(View.INVISIBLE);
						}
					}
				});
				ivPicture.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						view = v;
						new AlertDialog.Builder(context) 
						//set message, title, and icon
						.setTitle("Delete Message") 
						.setMessage("Do you want to delete this message ?") 
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) { 
								int j = (view.getId()-4)/10;
								Log.v(TAG,"position to remove : " + Integer.toString(j));
								controller.listMessages.remove((int) Math.floor((j)));
								if (!photo_id[(int) Math.floor((j))].isEmpty()){
									File imageFileFolder2 = new File(Environment.getExternalStoragePublicDirectory(
											Environment.DIRECTORY_PICTURES),"Pictever");
									imageFileFolder2.mkdir();
									Log.v(TAG, "image file to delete : " + photo_id[(int) Math.floor(((j)))]);
									File imageFileName2 = new File(imageFileFolder2,photo_id[(int) Math.floor((j))]);
									imageFileName2.delete();
								}
								SharedPreferences.Editor editor = controller.prefs.edit();
								editor.putStringSet("set_messages", new HashSet<String>(controller.listMessages));
								editor.commit();
								dialog.dismiss();
								controller.scrollX = view.getX();
								controller.scrollY = view.getY();
								display();
							}   
						})
						.setNegativeButton("No", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						})
						.show();
						return false;
					}
				});
				rl_picture.addView(ivPicture,0,ivPicture_params);
			}
			else {
				TextView tvMessage = new TextView(this);
				RelativeLayout.LayoutParams tvMessage_params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
				tvMessage_params.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE);
				tvMessage.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
				tvMessage.setMinHeight(600);
				tvMessage.setPadding(20, 20, 20, 20);
				tvMessage.setId(k+4);
				tvMessage.setBackgroundResource(R.color.AliceBlue);
				tvMessage.setTextColor(controller.which_color(receive_color[i]));
				tvMessage.setText(message[i]);
				tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
				tvMessage.setTypeface(Typeface.DEFAULT);
				tvMessage.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						int j = (v.getId()-4)/10;
						ColorDrawable colordrawable = new ColorDrawable(controller.which_color(receive_color[j]));
						fullscreen.setImageDrawable(colordrawable);
						fullscreen.setVisibility(View.VISIBLE);
						TextView tvMessage = (TextView) findViewById(R.id.tvShowMessage);
						tvMessage.setText(message[j]);
						tvMessage.setTextColor(getResources().getColor(R.color.White));
						tvMessage.setVisibility(View.VISIBLE);
					}
				});
				tvMessage.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						view = v;
						new AlertDialog.Builder(context) 
						//set message, title, and icon
						.setTitle("Delete Message") 
						.setMessage("Do you want to delete this message ?") 
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) { 
								int j = (view.getId()-4)/10;
								Log.v(TAG,"position to remove : " + Integer.toString(j));
								controller.listMessages.remove((int) Math.floor((j)));
								if (!photo_id[(int) Math.floor((j))].isEmpty()){
									File imageFileFolder2 = new File(Environment.getExternalStoragePublicDirectory(
											Environment.DIRECTORY_PICTURES),"Pictever");
									imageFileFolder2.mkdir();
									Log.v(TAG, "image file to delete : " + photo_id[(int) Math.floor((j))]);
									File imageFileName2 = new File(imageFileFolder2,photo_id[(int) Math.floor((j))]);
									imageFileName2.delete();
								}
								SharedPreferences.Editor editor = controller.prefs.edit();
								editor.putStringSet("set_messages", new HashSet<String>(controller.listMessages));
								editor.commit();
								dialog.dismiss();
								controller.scrollX = view.getX();
								controller.scrollY = view.getY();
								display();
							}   
						})
						.setNegativeButton("No", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						})
						.show();
						return false;
					}
				});
				rl_picture.addView(tvMessage,0,tvMessage_params);
			}

//			TextView tvSendChoice = new TextView(this);
//			RelativeLayout.LayoutParams tvSendChoice_params = new RelativeLayout.LayoutParams(
//					RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
//			tvSendChoice_params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
//			tvSendChoice_params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
//			tvSendChoice.setText(receive_label[i]);
//			tvSendChoice.setTypeface(font,Typeface.BOLD);
//			tvSendChoice.setPadding(20, -2, 20, -2);
//			tvSendChoice.setTextSize(25);
//			tvSendChoice.setId(k+5);
//			tvSendChoice.setBackgroundColor(controller.which_color(receive_color[i]));
//			tvSendChoice.setTextColor(getResources().getColor(R.color.White));
//			rl_picture.addView(tvSendChoice,tvSendChoice_params);

			LinearLayout ll_bottom = new LinearLayout(this);
			RelativeLayout.LayoutParams ll_bottom_params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			ll_bottom_params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			ll_bottom_params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
			ll_bottom.setOrientation(LinearLayout.VERTICAL);
			ll_bottom.setId(k+6);
			ll_bottom.setPadding(0, 0, 15, 10);
			rl_picture.addView(ll_bottom,ll_bottom_params);

			ImageButton ibShare = new ImageButton(this);
			ibShare.setPadding(0,0,0,1);
			ibShare.setBackgroundResource(R.color.Transparent);
			ibShare.setId(k+7);
			ibShare.setImageResource(R.drawable.buton_fb);
			ibShare.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Animation anim = AnimationUtils.loadAnimation(Timeline.this,R.animator.anim_on_click);
					((ImageButton) v).startAnimation(anim);
					Toast.makeText(Timeline.this,"Facebook sharing is coming soon! ;) ",Toast.LENGTH_SHORT).show();
					if(controller.analytics != null) {
						AnalyticsEvent resendMessageEvent = controller.analytics.
								getEventClient().createEvent("androidFacebookSharing");
						//Record the Level Complete event
						controller.analytics.getEventClient().recordEvent(resendMessageEvent);
					}
				}
			});
			ll_bottom.addView(ibShare);
			ImageButton ibResend = new ImageButton(this);
			ibResend.setBackgroundResource(R.color.Transparent);
			ibResend.setPadding(0,1,0,0);
			ibResend.setId(k+8);
			ibResend.setImageResource(R.drawable.bulle_resend);
			ibResend.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					view = v;
					Animation anim = AnimationUtils.loadAnimation(Timeline.this,R.animator.anim_on_click);
					((ImageButton) v).startAnimation(anim);
					new AlertDialog.Builder(context) 
					//set message, title, and icon
					.setIcon(R.drawable.bulle_resend)
					.setTitle("Want to remember this message ?") 
					.setMessage("Resend it randomly in the future ! (the message will disappear again ;))") 
					.setPositiveButton("Send", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) { 
							int j = (view.getId()-8)/10;
							try {
								String message_to_get = controller.listMessages.get((int) Math.floor((j)));
								JSONObject json_message = new JSONObject(message_to_get);
								if (!json_message.optString("message_id").isEmpty()) {
									if(controller.analytics != null) {
										AnalyticsEvent resendMessageEvent = controller.analytics.
												getEventClient().createEvent("androidResend");
										//Record the Level Complete event
										controller.analytics.getEventClient().recordEvent(resendMessageEvent);
									}
									String no_network = controller.resend(json_message.optString("message_id"));
									if (no_network.isEmpty()) {
										controller.listMessages.remove((int) Math.floor((j)));
										if (!photo_id[(int) Math.floor(j)].isEmpty()){
											File imageFileFolder2 = new File(Environment.
													getExternalStoragePublicDirectory(
															Environment.DIRECTORY_PICTURES),"Pictever");
											imageFileFolder2.mkdir();
											Log.v(TAG, "image file to delete : " + photo_id[(int) Math.floor((j))]);
											File imageFileName2 = new File(imageFileFolder2,
													photo_id[(int) Math.floor(j)]);
											imageFileName2.delete();
										}
										SharedPreferences.Editor editor = controller.prefs.edit();
										editor.putStringSet("set_messages", new HashSet<String>(
												controller.listMessages));
										editor.commit();
										dialog.dismiss();
										controller.scrollX = view.getX();
										controller.scrollY = view.getY();
										display();
									}
									else
										Toast.makeText(context,no_network,Toast.LENGTH_SHORT).show();
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}   
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
					.show();
				}
			});
			ll_bottom.addView(ibResend);
			k=k+10;
		} 
	}
}