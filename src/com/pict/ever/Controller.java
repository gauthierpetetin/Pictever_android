package com.pict.ever;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera.Parameters;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Data;
import android.support.v4.widget.SwipeRefreshLayout;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsConfig;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsEvent;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.regions.Regions;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class Controller {

	//---GENERAL---//
	String TAG = "Controller";
	SharedPreferences prefs;
	SharedPreferences.Editor editor;
	Context context;
	String current_activity;
	int api;
	int SCREEN_WIDTH=0,SCREEN_HEIGHT=0;
	//---REQUESTS--//
	CookieStore cookieStore;
	String json_phones_to_upload_after_login="";
	Boolean ok_for_receive=true;
	List<NameValuePair> nVP;
	String update_link="",url_to_get_after_401="",
			url_to_post_after_401="",origin_url,message_timestamp,photo_path;
	String local_server="http://192.168.1.10:5000/";
	//---AMAZON---//
	CognitoCachingCredentialsProvider cognitoProvider;
	TransferManager transferManagerUp,transferManagerDown;
	Download download;
	Upload upload;
	TextView tvRetry;
	int uploadProgression=0;
	String picture_from,upload_is_active ="no",last_photo_path,
			last_picture_name,last_contact_ids,last_send_choice,last_send_label="";
	private String APP_ID,AWS_ACCOUNT_ID,COGNITO_POOL_ID,COGNITO_ROLE_UNAUTH,COGNITO_ROLE_AUTH,CLOUDFRONT;
	ArrayList<String> last_list_ids_selected;
	Boolean is_retry_download = false;
	File file_to_store_picture;
	String BUCKET_NAME = "picteverbucket";
	public MobileAnalyticsManager analytics;
	//---GCM---//
	String SENDER_ID = "952675823733";
	GoogleCloudMessaging gcm;
	//---OTHER VARIABLES---//
	public final String FROM_LOGIN = "from_login";
	int wait = 300;
	ArrayList<String> contacts_to_remove,newlistContacts,sortedlistContacts,listSendChoices,listMessages;	
	String display_notification="";
	public boolean from_notif = false;
	String number_of_future_messages,status="",photo_id="";
	//---VIEWS---//
	Button btn_login,btn_signup,btn_verify;
	Toast iolos;
	ProgressBar loader;
	AlertDialog update_dialog;
	String popup_extra;
	TextView tvCounter;
	LinearLayout ll_progress;
	RelativeLayout.LayoutParams ll_progress_params;
	RelativeLayout rl_title;
	//---CAMERA---//
	public static final int PICTURE_MAX_SIZE = 1000000;
	public static final double ASPECT_TOLERANCE = 0.05;
	public String FLASH_MODE = Parameters.FLASH_MODE_OFF;
	public String CAMERA_FACING = "back";
	//---TIMELINE---//
	float scrollX=0,scrollY=0;
	String[] created_ats,messages,uri_icons,contact_names,photo_ids;
	Bitmap[] bmp_photos;

	//------------------------------------------------------------------------------------------------//
	//------------------------------------------------------------------------------------------------//

	Controller (Context contxt) {
		this.context = contxt;
		this.cookieStore = new BasicCookieStore();
		this.prefs = contxt.getSharedPreferences("Pictever", Context.MODE_PRIVATE);
		nVP = new ArrayList<NameValuePair>();
		iolos = Toast.makeText(context,"Hello you !", Toast.LENGTH_SHORT);
		if (local_server.isEmpty())
			origin_url = prefs.getString("origin_url", "http://instant-pictever.herokuapp.com/");
		else
			origin_url = local_server;
		SCREEN_HEIGHT = prefs.getInt("SCREEN_HEIGHT", 0);
		SCREEN_WIDTH = prefs.getInt("SCREEN_WIDTH", 0);
		Set<String> setMessages = prefs.getStringSet("set_messages", new HashSet<String>());
		listMessages = new ArrayList<String>(setMessages);
		Set<String> setContacts = prefs.getStringSet("set_contacts", new HashSet<String>());
		sortedlistContacts = new ArrayList<String>(setContacts);
		Set<String> setSendChoices = prefs.getStringSet("set_send_choices", new HashSet<String>());
		listSendChoices = new ArrayList<String>(setSendChoices);	
		new AsyncTask<String, String, String> () {
			@Override
			protected String doInBackground(String... params) {
				sortContacts(sortedlistContacts);
				sortMessages(listMessages);
				return null;
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");

		message_timestamp = prefs.getString("message_timestamp",
				Double.toString((double)System.currentTimeMillis()/1000));
		AWS_ACCOUNT_ID = prefs.getString("aws_account_id","090152412356");
		COGNITO_POOL_ID = prefs.getString("cognito_pool_id","us-east-1:cf4486fa-e5e0-423f-9399-e6aae8d08e3f");
		COGNITO_ROLE_UNAUTH = prefs.getString("cognito_role_unauth",
				"arn:aws:iam::090152412356:role/Cognito_PicteverUnauth_DefaultRole");
		COGNITO_ROLE_AUTH = prefs.getString("cognito_role_auth",
				"arn:aws:iam::090152412356:role/Cognito_PicteverAuth_DefaultRole");
		BUCKET_NAME = prefs.getString("bucket_name","picteverbucket");
		APP_ID = prefs.getString("amazon_app_id","9948cbe136a5487fa592e71985e2cdaa");
		CLOUDFRONT = prefs.getString("cloudfront", "http://d380gpjtb0vxfw.cloudfront.net/");
		api = prefs.getInt("api_level", android.os.Build.VERSION.SDK_INT);
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected())
				analytics = getMobileAnalytics();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MobileAnalyticsManager getMobileAnalytics () {
		try {
			new AsyncTask<String,String, String>() {
				@Override
				protected String doInBackground(String... params) {
					try {
						cognitoProvider = getCredProvider(context);
					} catch (Exception e) {
						Log.v(TAG,"error cognito providers");
					}
					try {
						AnalyticsConfig options = new AnalyticsConfig();
						options.withAllowsWANDelivery(true);
						analytics = MobileAnalyticsManager.getOrCreateInstance(
								context,
								APP_ID,
								Regions.US_EAST_1,
								cognitoProvider,
								options
								);
					} catch(InitializationException ex) {
						Log.e(this.getClass().getName(), 
								"Failed to initialize Amazon Mobile Analytics", ex);
					}
					return null;
				} 
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
		}
		catch (Exception e) {
			Log.v(TAG, "unable to get mobile analytics");
		}
		return analytics;
	}

	public CognitoCachingCredentialsProvider getCredProvider(Context appContext) {
		if(cognitoProvider == null) {
			cognitoProvider = new CognitoCachingCredentialsProvider(
					context,
					AWS_ACCOUNT_ID,
					COGNITO_POOL_ID,
					COGNITO_ROLE_UNAUTH,
					COGNITO_ROLE_AUTH,
					Regions.US_EAST_1
					);
			cognitoProvider.refresh();
		}
		return cognitoProvider;
	}

	public void signUp (String email, String hashpass){
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("email",email));
		nameValuePairs.add(new BasicNameValuePair("password",hashpass));
		nVP = nameValuePairs;
		String url = origin_url + "signup";
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
				new asyn_post_request().execute(url);
			}
			else {
				iolos.setText("No network right now. Please try again later");
				iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				iolos.show();
				if (btn_signup!=null && loader!=null) {
					btn_signup.setEnabled(true);
					btn_signup.setAlpha(1);
					loader.clearAnimation();
					loader.setVisibility(View.INVISIBLE);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void defineNewPassword (String user_email, String verification_code, String new_password){
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
		nameValuePairs.add(new BasicNameValuePair("email",user_email));
		nameValuePairs.add(new BasicNameValuePair("verification_code",verification_code));
		nameValuePairs.add(new BasicNameValuePair("new_password",new_password));
		nVP = nameValuePairs;
		String url = origin_url + "define_new_password";
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
				new asyn_post_request().execute(url);
			}
			else {
				iolos.setText("No network right now. Please try again later");
				iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				iolos.show();
				if (loader!=null) {
					loader.clearAnimation();
					loader.setVisibility(View.INVISIBLE);
				}
			};
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void defineFirstPhoneNumber (String user_phone){
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("phone_number",user_phone));
		nameValuePairs.add(new BasicNameValuePair("reg_id",prefs.getString("reg_id", "")));
		nameValuePairs.add(new BasicNameValuePair("os","android"));
		nVP = nameValuePairs;
		String url = origin_url + "define_first_phone_number";
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
				new asyn_post_request().execute(url);
			}
			else {
				iolos.setText("No network right now. Please try again later");
				iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				iolos.show();
				if (btn_verify!=null && loader!=null) {
					btn_verify.setEnabled(true);
					btn_verify.setAlpha(1);
					loader.clearAnimation();
					loader.setVisibility(View.INVISIBLE);
				}
			};
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String sendMessage (String message,String photo_id,String contact_ids,String send_choice){
		editor = prefs.edit();
		int messages_sent = prefs.getInt("messages_sent", 0);
		editor.putInt("messages_sent", messages_sent+1);
		editor.commit();
		String no_network = "";
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
		nameValuePairs.add(new BasicNameValuePair("message",message));
		nameValuePairs.add(new BasicNameValuePair("photo_id",photo_id));
		nameValuePairs.add(new BasicNameValuePair("receiver_ids",contact_ids));
		nameValuePairs.add(new BasicNameValuePair("delivery_option", send_choice));
		nVP=nameValuePairs;
		String url = origin_url + "send";
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			new asyn_post_request().execute(url);
		}
		else {
			no_network = "No network right now. Please try again later";
		}
		return no_network;
	}

	public String resend (String message_id){
		String no_network = "";
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
		nameValuePairs.add(new BasicNameValuePair("message_id",message_id));
		nVP=nameValuePairs;
		String url = origin_url + "resend";
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			new asyn_post_request().execute(url);
		}
		else {
			no_network = "No network right now. Please try again later";
		}
		return no_network;
	}

	public void block_contacts(String json_ids) {
		String url = origin_url + "block_contacts";
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("contacts_to_block",json_ids));
		nVP = nameValuePairs;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			new asyn_post_request().execute(url);
		}
		else {
			Toast.makeText(context,"No network right now.",Toast.LENGTH_SHORT).show();
		}
	}

	public void upload_contacts(String json_phones) {
		String url = origin_url + "upload_contacts";
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("contacts",json_phones));
		nVP = nameValuePairs;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			new asyn_post_request().execute(url);
		}
		else {
			Toast.makeText(context,"No network right now.",Toast.LENGTH_SHORT).show();
		}
	}

	public void get_send_choices() {
		String url = origin_url + "get_send_choices";
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			new asyn_get_request().execute(url);
		}
	}

	public void get_my_status() {
		String url = origin_url + "get_my_status";
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			new asyn_get_request().execute(url);
		}
	}

	public void send_reset_mail(String email) {
		String url = origin_url + "send_reset_mail?email=" + email;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			new asyn_get_request().execute(url);
		}
	}

	public void get_number_of_future_messages() {
		String url = origin_url + "get_number_of_future_messages";
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			new asyn_get_request().execute(url);
		}
	}

	public void receive_all () {
		if (ok_for_receive) {
			String url = origin_url + "receive_all?ts=" + message_timestamp;
			ConnectivityManager cm = (ConnectivityManager) context.
					getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
				ok_for_receive = false;
				new asyn_get_request().execute(url);
				Handler chandler = new Handler();
				chandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						ok_for_receive = true;
					}
				}, 1000);
			}
		}
		else {
			Handler chandler = new Handler();
			chandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					receive_all();
				}
			}, 2000);
		}
	}

	private class asyn_get_request extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			InputStream inputStream = null;
			String response_from_server="";
			String error_code="";
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpContext localContext = new BasicHttpContext();
				localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
				HttpGet httpGet = new HttpGet(urls[0]);
				HttpResponse httpResponse = httpclient.execute(httpGet,localContext);
				error_code = Integer.toString(httpResponse.getStatusLine().getStatusCode());
				inputStream = httpResponse.getEntity().getContent();
				if(inputStream != null)
					response_from_server= convertInputStreamToString(inputStream);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}catch (IllegalArgumentException e) {
				e.printStackTrace();
			}catch (IllegalStateException e) {
				e.printStackTrace();
			}
			JSONObject json_response = new JSONObject();
			try {
				if (error_code!=null)
					json_response.put("error_code", error_code);
				else
					json_response.put("error_code","");
				if (response_from_server!=null)
					json_response.put("response_from_server",response_from_server);
				else 
					json_response.put("response_from_server","");
				json_response.put("url",urls[0]);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return json_response.toString();
		}
		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject json_result = new JSONObject(result);
				String url = json_result.optString("url");
				String error_code = json_result.optString("error_code");
				String response_from_server = json_result.optString("response_from_server");
				manage_response_from_get(url,error_code,response_from_server);
			}
			catch (JSONException e ) {
				e.printStackTrace();
			}
		}
	}

	private class asyn_post_request extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			InputStream inputStream = null;
			String error_code="";
			String response_from_server="";
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpContext localContext = new BasicHttpContext();
				localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
				HttpPost httpPost = new HttpPost(urls[0]);
				httpPost.setEntity(new UrlEncodedFormEntity(nVP,HTTP.UTF_8));
				HttpResponse httpResponse = httpclient.execute(httpPost,localContext);
				error_code = Integer.toString(httpResponse.getStatusLine().getStatusCode());
				inputStream = httpResponse.getEntity().getContent();
				if(inputStream != null)
					response_from_server= convertInputStreamToString(inputStream);
				else
					response_from_server = "Did not work!";
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}catch (IllegalArgumentException e) {
				e.printStackTrace();
			}catch (IllegalStateException e) {
				e.printStackTrace();
			}
			JSONObject json_response = new JSONObject();
			try {
				if (error_code!=null)
					json_response.put("error_code", error_code);
				else
					json_response.put("error_code","");
				if (response_from_server!=null)
					json_response.put("response_from_server",response_from_server);
				else 
					json_response.put("response_from_server","");
				json_response.put("url",urls[0]);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return json_response.toString();
		}
		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject json_result = new JSONObject(result);
				String url = json_result.optString("url");
				String error_code = json_result.optString("error_code");
				String response_from_server = json_result.optString("response_from_server");
				manage_response_from_post(nVP,url,error_code,response_from_server);
			}
			catch (JSONException e ) {
				e.printStackTrace();
			}		
		}
	}

	public void login(String from) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			new asyn_login().execute(from);
		}
		else {
			iolos.setText("No network right now. Please try again later");
			iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			iolos.show();
			if (loader!=null && btn_login!=null) {
				btn_login.setEnabled(true);
				btn_login.setAlpha(1);
				loader.clearAnimation();
				loader.setVisibility(View.INVISIBLE);
			}
		}
	}

	public class asyn_login extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... datas) {
			InputStream inputStream = null;
			String from = datas[0];
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
			nameValuePairs.add(new BasicNameValuePair("facebook_id",prefs.getString("facebook_id", "")));
			nameValuePairs.add(new BasicNameValuePair("facebook_name",prefs.getString("facebook_name", "")));
			nameValuePairs.add(new BasicNameValuePair("facebook_birthday",prefs.getString("facebook_birthday", "")));
			nameValuePairs.add(new BasicNameValuePair("email",prefs.getString("user_email", "")));
			nameValuePairs.add(new BasicNameValuePair("password",prefs.getString("user_password", "")));
			nameValuePairs.add(new BasicNameValuePair("os","android"));
			if (prefs.getString("reg_id", "").isEmpty()) {
				try {
					gcm = GoogleCloudMessaging.getInstance(context);
					String reg_id = gcm.register(SENDER_ID);
					Log.v(TAG,reg_id);
					editor=prefs.edit();
					editor.putString("reg_id", reg_id);
					editor.commit();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			nameValuePairs.add(new BasicNameValuePair("reg_id",prefs.getString("reg_id", "")));
			PackageInfo pInfo;
			try {
				pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
				editor=prefs.edit();
				editor.putString("version", pInfo.versionName);
				editor.commit();
				nameValuePairs.add(new BasicNameValuePair("app_version", pInfo.versionName));
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}
			String url = origin_url + "login";
			String error_code = "";
			String response_from_login = "";
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpContext localContext = new BasicHttpContext();
				localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
				HttpPost httpPost = new HttpPost(url);
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
				HttpResponse httpResponse = httpclient.execute(httpPost,localContext);
				error_code = Integer.toString(httpResponse.getStatusLine().getStatusCode());
				inputStream = httpResponse.getEntity().getContent();
				if(inputStream != null)
					response_from_login= convertInputStreamToString(inputStream);
				else
					response_from_login = "Did not work!";
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IllegalStateException e ) {
				e.printStackTrace();
			}
			JSONObject json_response = new JSONObject();
			try {
				if (error_code!=null)
					json_response.put("error_code", error_code);
				else
					json_response.put("error_code","");
				if (response_from_login!=null && !response_from_login.isEmpty())
					json_response.put("response_from_login",response_from_login);
				else 
					json_response.put("response_from_login","");
				json_response.put("url",url);
				json_response.put("from", from);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return json_response.toString();
		}
		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject json_result = new JSONObject(result);
				String url = json_result.optString("url");
				String from = json_result.optString("from");
				String error_code = json_result.optString("error_code");
				String response_from_login = json_result.optString("response_from_login");
				Log.v(TAG,"manage login : url_code = " 
						+ url.substring(origin_url.length(),  url.length()) + " ; error_code = " 
						+ error_code + " ; response = " + response_from_login);
				if (result==null) {
					ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
							Context.CONNECTIVITY_SERVICE);
					if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
						try {
							new asyn_login().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,from);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					else {
						Toast.makeText(context,"No network right now.",Toast.LENGTH_SHORT).show();
					}
				}
				else {
					if (error_code.equals("200")) {
						if (!url_to_get_after_401.isEmpty()) {
							new asyn_get_request().execute(url_to_get_after_401);
						}
						url_to_get_after_401="";
						if (!url_to_post_after_401.isEmpty()) {
							new asyn_post_request().execute(url_to_post_after_401);
						}
						url_to_post_after_401="";
						try {
							JSONObject json_infos = new JSONObject(response_from_login);
							String user_id = "id" + json_infos.optString("user_id");
							String version_needed = json_infos.optString("android_version_needed");
							update_link = json_infos.optString("android_update_link");
							String force_update = json_infos.optString("force_update");
							if (!json_infos.optString("aws_account_id").isEmpty()) {
								AWS_ACCOUNT_ID = json_infos.getString("aws_account_id");
								APP_ID = json_infos.getString("amazon_app_id");
								COGNITO_POOL_ID = json_infos.getString("cognito_pool_id");
								COGNITO_ROLE_AUTH = json_infos.getString("cognito_role_auth");
								COGNITO_ROLE_UNAUTH = json_infos.getString("cognito_role_unauth");
								BUCKET_NAME = json_infos.getString("bucket_name");
								CLOUDFRONT = json_infos.getString("cloudfront");
							}

							String hash_user_id = computeHash(user_id);
							editor = prefs.edit();
							if (force_update.isEmpty()) {
								editor.putString("force_update","false");
							}
							else {
								editor.putString("force_update",force_update);
							}						
							editor.putString("user_id",user_id);
							editor.putString("amazon_app_id", APP_ID);
							editor.putString("aws_account_id",AWS_ACCOUNT_ID);
							editor.putString("cognito_pool_id",COGNITO_POOL_ID);
							editor.putString("cognito_role_auth",COGNITO_ROLE_AUTH);
							editor.putString("cognito_role_unauth",COGNITO_ROLE_UNAUTH);
							editor.putString("bucket_name", BUCKET_NAME);
							editor.putString("cloudfront", CLOUDFRONT);
							editor.putString("hash_user_id", hash_user_id);
							if (local_server.isEmpty()) {
								origin_url = json_infos.optString("web_app_url");
								editor.putString("origin_url", origin_url);
							}
							editor.commit();

							if (Float.parseFloat(prefs.getString("version","1.1")) < Float.parseFloat(version_needed)) {
								PicteverApp mPicteverApp = (PicteverApp) context.getApplicationContext();
								if (mPicteverApp!=null && mPicteverApp.getCurrentActivity()!=null) {
									new AlertDialog.Builder(mPicteverApp.getCurrentActivity())
									.setTitle("A new version of Pictever is available!")
									.setMessage("Do you want to install it now ?")
									.setNegativeButton("Install", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											final String appPackageName = context.getPackageName();
											try {
												Intent intent = new Intent(Intent.ACTION_VIEW, 
														Uri.parse("market://details?id=" + appPackageName));
												intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | 
														Intent.FLAG_ACTIVITY_NEW_TASK);
												context.startActivity(intent);
											} catch (android.content.ActivityNotFoundException anfe) {
												Intent intent = new Intent(Intent.ACTION_VIEW, 
														Uri.parse("http://play.google.com/store/apps/details?id="
																+ appPackageName));										
												intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | 
														Intent.FLAG_ACTIVITY_NEW_TASK);
												context.startActivity(intent);
											}
											dialog.dismiss();
										}
									})
									.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) { 
											dialog.dismiss();
										}
									})
									.setCancelable(false)
									.show();
								}
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!json_phones_to_upload_after_login.isEmpty()) {
							upload_contacts(json_phones_to_upload_after_login);
						}
						
						if (!prefs.getString("facebook_id", "").isEmpty()) {
							message_timestamp = Double.toString((double)System.currentTimeMillis()/1000);
							editor = prefs.edit();
							editor.putString("message_timestamp", message_timestamp);
							editor.commit();
						}

						if (from.equals(FROM_LOGIN)) {
							if (loader!=null) {
								loader.clearAnimation();
								loader.setVisibility(View.INVISIBLE);
							}
							message_timestamp = Double.toString((double)System.currentTimeMillis()/1000);
							editor = prefs.edit();
							editor.putString("message_timestamp", message_timestamp);
							editor.commit();
							Intent intent = new Intent(context,SetPhoneNumber.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(intent);
						}
					}
					else {
						if (from.equals(FROM_LOGIN) && btn_login!=null) {
							btn_login.setEnabled(true);
							btn_login.setAlpha(1);
							if (loader!=null) {
								loader.clearAnimation();
								loader.setVisibility(View.INVISIBLE);
							}
						}
						if (error_code.equals("404") || error_code.equals("500")) {
							Toast.makeText(context,"Server Error. Please report to Pictever Team",
									Toast.LENGTH_SHORT).show();
							if (local_server.isEmpty()) {
								origin_url = "http://instant-pictever.herokuapp.com/";
								editor = prefs.edit();
								editor.putString("origin_url", origin_url);
								editor.commit();
							}
						}
						if (error_code.equals("401")) {
							iolos.setText("There is a problem with your credentials. Please login again");
							iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
							iolos.show();
							if (!current_activity.equals(Login.class.getSimpleName())) {
								Intent intent = new Intent(context,Login.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
								context.startActivity(intent);
							}
						}
					}
				}
			}
			catch (JSONException e ) {
				e.printStackTrace();
			}
		}
	}

	public void manage_response_from_post(List<NameValuePair> NameVP,String u_r_l, String err_code , String response) {
		String url_code =  u_r_l.substring(origin_url.length(),  u_r_l.length());
		if (err_code != null) {
			if (err_code.equals("404") || err_code.equals("500")) {
				Toast.makeText(context,"Server Error. Please report to Pictever Team",Toast.LENGTH_SHORT).show();
				if (local_server.isEmpty()) {
					origin_url = "http://instant-pictever.herokuapp.com/";
					editor = prefs.edit();
					editor.putString("origin_url", origin_url);
					editor.commit();
				}
			}
			if (err_code.equals("401") || err_code.equals("503")) {
				ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
					try {
						Log.v(TAG,"OK to re-post the url " + u_r_l);
						url_to_post_after_401 = u_r_l;
						nVP = NameVP;
						new asyn_login().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}				
			}

			if (url_code.equals("signup")) {
				if (btn_signup!=null && loader!=null) {
					btn_signup.setEnabled(true);
					btn_signup.setAlpha(1);
					loader.clearAnimation();
					loader.setVisibility(View.INVISIBLE);
				}
				switch (Integer.parseInt(err_code)) {
				case 200:
					message_timestamp = "1412932000";
					editor = prefs.edit();
					editor.putString("message_timestamp", message_timestamp);
					editor.commit();
					Intent intent = new Intent(context,SetPhoneNumber.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
					break;
				case 406:
					iolos.setText("This email already corresponds to an account. Please login.");
					iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					iolos.show();
					break;
				}
			}

			if (url_code.equals("define_new_password")) { 
				if (loader!=null) {
					loader.clearAnimation();
					loader.setVisibility(View.INVISIBLE);
				}
				switch (Integer.parseInt(err_code)) {
				case 200:
					Intent intent = new Intent(context,SetPhoneNumber.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
					break;
				default:
					iolos.setText("Unable to reset your password");
					iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					iolos.show();
					break;
				}
			}

			if (url_code.equals("define_first_phone_number")) { 
				if (btn_verify!=null && loader!=null) {
					btn_verify.setEnabled(true);
					btn_verify.setAlpha(1);
					loader.clearAnimation();
					loader.setVisibility(View.INVISIBLE);
				}
				switch (Integer.parseInt(err_code)) {
				case 200:
					editor = prefs.edit();
					editor.putString("is_connected","true");
					editor.commit();
					wait = 5000;
					Intent intent = new Intent(context,Load.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
					break;
				case 406:
					iolos.setText("Sorry this phone number already corresponds to an account.");
					iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					iolos.show();
					break;
				}
			}

			if (err_code.equals("200") && url_code.equals("upload_contacts")) {
				new AsyncTask<String, String, String>() {
					@Override
					protected String doInBackground(String... response) {
						try {
							JSONArray json_array = new JSONArray(response[0]);
							for (int i = 0; i < json_array.length(); i++) {
								JSONObject json_object = json_array.getJSONObject(i);
								String email = json_object.optString("email");
								String user_id = json_object.optString("user_id");
								String phoneNumber1 = json_object.optString("phoneNumber1");
								String status = json_object.optString("status");
								String facebook_id = json_object.optString("facebook_id");
								String facebook_name = json_object.optString("facebook_name");
								String facebook_birthday = json_object.optString("facebook_birthday");
								putInfosInContact(email,user_id,phoneNumber1,status,facebook_id,facebook_name,facebook_birthday);
							}
							editor = prefs.edit();
							Set<String> setContacts = new HashSet<String>(newlistContacts);
							editor.putStringSet("set_contacts", setContacts);
							editor.commit();
							sortContacts(newlistContacts);
							sortedlistContacts = newlistContacts;
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return null;
					}
					@Override
					protected void onPostExecute (String result) {
						get_send_choices();
					}
				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,response);
			}
			if (err_code.equals("200") && url_code.equals("send")) {
				int messages_sent = prefs.getInt("messages_sent", 0);
				Log.v(TAG,"messages_sent " + messages_sent);
				int contacts_invited = prefs.getInt("contacts_invited", 0);
				Log.v(TAG,"contacts_invited " + contacts_invited);
				PicteverApp mPicteverApp = (PicteverApp) context.getApplicationContext();
				if (mPicteverApp!=null && mPicteverApp.getCurrentActivity()!=null) {
					String title="";
					String message="";
					popup_extra="";
					switch (messages_sent) {
					case 1:
						title = "Congratulations! You sent your first message on Pictever!";
						if (contacts_invited==0)
							message= "Tip 1 : You can send messages to anybody in your address book!";
						else 
							message="Tip 1 : Wanna know how many messages wait for you in the future ?" +
									" Go to your Timeline and ask Billy!";
						break;
					case 2:
						title = "Your message has been sent successfully!";
						if (contacts_invited==0) 
							message= "Tip 2 : Wanna know how many messages wait for you in the future ?" +
									" Go to your Timeline and ask Billy!";
						else 
							message="Tip 2 : You can give us your feedback on the app from the settings ;)";
						break;
					case 3:
						title = "Your message has been sent successfully!";
						message="Wanna help us ? Please leave a comment on Google Play ;)";
						popup_extra ="review";
						break;
					case 4:
						title = "Your message has been sent successfully!";
						message="You are a master! Please join our community! (last popup you'll see we promise ;))";
						popup_extra = "community";
						break;
					default:
						Toast.makeText(context,"Message sent!",Toast.LENGTH_SHORT).show();
						break;
					}
					if (!message.isEmpty()&&!title.isEmpty()) {
						if (popup_extra.equals("community")) {
							AlertDialog dialog = new AlertDialog.Builder(mPicteverApp.getCurrentActivity())
							.setTitle(title)
							.setMessage(message)
							.setPositiveButton("Ok!", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) { 
									Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
											Uri.parse("https://www.facebook.com/pictever"));
									browserIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | 
											Intent.FLAG_ACTIVITY_NEW_TASK);
									context.startActivity(browserIntent);
									dialog.dismiss();
								}
							})
							.setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) { 
									dialog.dismiss();
								}
							})
							.create();
							dialog.getWindow().getAttributes().windowAnimations=R.style.dialog_animation;
							dialog.show();
						}
						else {
							if (popup_extra.equals("review")) {
								new AlertDialog.Builder(mPicteverApp.getCurrentActivity())
								.setTitle(title)
								.setMessage(message)
								.setPositiveButton("Ok!", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) { 
										final String appPackageName = context.getPackageName();
										try {
											Intent intent = new Intent(Intent.ACTION_VIEW, 
													Uri.parse("market://details?id=" + appPackageName));
											intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | 
													Intent.FLAG_ACTIVITY_NEW_TASK);
											context.startActivity(intent);
										} catch (android.content.ActivityNotFoundException anfe) {
											Intent intent = new Intent(Intent.ACTION_VIEW, 
													Uri.parse("http://play.google.com/store/apps/details?id=" 
															+ appPackageName));
											intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | 
													Intent.FLAG_ACTIVITY_NEW_TASK);
											context.startActivity(intent);
										}
										dialog.dismiss();
									}
								})
								.setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) { 
										dialog.dismiss();
									}
								})
								.show();
							}
							else {
								AlertDialog dialog = new AlertDialog.Builder(mPicteverApp.getCurrentActivity())
								.setTitle(title)
								.setMessage(message)
								.setNeutralButton("Ok!", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) { 
										dialog.dismiss();
									}
								})
								.create();
								dialog.getWindow().getAttributes().windowAnimations=R.style.dialog_animation;
								dialog.show();
							}
						}


						if (contacts_invited==3) {
							if (prefs.getBoolean("show_unlock", true)) {
								new AlertDialog.Builder(mPicteverApp.getCurrentActivity())
								.setTitle("Awesome! You unlocked some future times!")
								.setMessage("You can now send messages to a random day in the future!")
								.setNeutralButton("Great!", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) { 
										dialog.dismiss();
									}
								})
								.show();
								editor = prefs.edit();
								editor.putBoolean("show_unlock",false);
								editor.commit();
							}
						}
					}
				}
				get_my_status();
			}
			if (err_code.equals("200") && url_code.equals("resend")) {
				Toast.makeText(context,"Message resent!",Toast.LENGTH_SHORT).show();
				get_my_status();
			}
			Log.v(TAG,"postrequest : url_code = " + url_code + " ; error_code = " 
					+ err_code + " ; response = " + response);
		}
	}

	public void manage_response_from_get(String u_r_l, String err_code , String response) {
		String url_code =  u_r_l.substring(origin_url.length(),  u_r_l.length());
		if (err_code!=null) {
			if (err_code.equals("404") || err_code.equals("500")) {
				Toast.makeText(context,"Server Error. Please report to Pictever Team",Toast.LENGTH_SHORT).show();
				if (local_server.isEmpty()) {
					origin_url = "http://instant-pictever.herokuapp.com/";
					editor = prefs.edit();
					editor.putString("origin_url", origin_url);
					editor.commit();
				}
			}
			if (err_code.equals("401")) {
				ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
					try {
						Log.v(TAG,"OK to re-get the url " + u_r_l);
						url_to_get_after_401 = u_r_l;
						new asyn_login().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (err_code.equals("500") || err_code.equals("503")) {
				Toast.makeText(context,"Server Error. Please report to Pictever Team",Toast.LENGTH_SHORT).show();
			}

			if (err_code.equals("200") && url_code.startsWith("send_reset_mail")) {
				Intent intent = new Intent(context,ResetPassword.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			}

			if (err_code.equals("200") && url_code.startsWith("get_my_status")) {
				if (status.isEmpty()) {
					status = prefs.getString("status", "Newbie");
				}
				if (!response.isEmpty() && !response.equals(status)) {
					status = response;
					iolos.setText("You're now a " + status + " !");
					iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					iolos.show();
				}
				try {
					String[] contact = findContactFromPhone(sortedlistContacts,prefs.getString("user_phone", ""));
					if (!contact[0].isEmpty()) {
						JSONObject json_contact = new JSONObject(contact[0]);
						json_contact.put("status",status);
						contact[0] = json_contact.toString();
						sortedlistContacts.set(Integer.parseInt(contact[1]),contact[0]);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				editor = prefs.edit();
				Set<String> setContacts = new HashSet<String>(sortedlistContacts);
				editor.putStringSet("set_contacts", setContacts);
				editor.putString("status",status);
				editor.commit();
			}
			if (err_code.equals("200") && url_code.startsWith("get_number_of_future_messages")) {
				if (response.equals("0"))
					number_of_future_messages = "1";
				else 
					number_of_future_messages = response;
				if (number_of_future_messages!=null) {
					editor = prefs.edit();
					editor.putString("number_of_future_messages",number_of_future_messages);
					editor.commit();
					tvCounter.setText(number_of_future_messages + " !");
				}
			}
			if (url_code.startsWith("receive_all")) {
				if (err_code.equals("200")) {
					try {
						JSONObject json_response = new JSONObject(response);
						message_timestamp = json_response.optString("ts");
						ok_for_receive = true;
						String messages = json_response.getString("new_messages");
						JSONArray json_messages = new JSONArray(messages);
						if (json_messages.length()==0)
							stopRefreshingOnTimeline();
						for (int j = 0; j < json_messages.length(); j++) {
							JSONObject message = json_messages.getJSONObject(j);
							message.put("timestamp", json_response.getString("ts"));
							display_notification = message.getString("receive_label");
							String[] contact = findContactFromPhone(sortedlistContacts, message.optString("from_numero"));
							if (contact[0].isEmpty()) {
								JSONObject jsoncontact = new JSONObject();
								try {
									String name=message.optString("from_numero");
									String facebook_id= message.optString("from_facebook_id");
									String facebook_name= message.optString("from_facebook_name");
									String facebook_birthday= message.optString("from_facebook_birthday");
									if (!facebook_name.isEmpty())
										name = facebook_name;
									else {
										if (message.optString("from_email").length() > 5) {
											name = name + " (" + message.optString("from_email").substring(0,5) + "...)";
										}
										else {
											name = name + " (" + message.optString("from_email") + " )";
										}
									}
									jsoncontact.put("name",name);
									jsoncontact.put("phoneNumber1", message.optString("from_numero"));
									jsoncontact.put("phoneNumber2", "");
									jsoncontact.put("uri_photo", "");
									jsoncontact.put("facebook_id",facebook_id);
									jsoncontact.put("facebook_name",facebook_name);
									jsoncontact.put("facebook_birthday",facebook_birthday);
									jsoncontact.put("status","");
									jsoncontact.put("contact_id", "id"+message.optString("from_id"));
									jsoncontact.put("is_selected","unselected");
									contact[0] = jsoncontact.toString();
									sortedlistContacts.add(contact[0]);
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							else {
								try {
									JSONObject json_contact = new JSONObject(contact[0]);
									json_contact.put("contact_id","id"+message.optString("from_id"));
									contact[0] = json_contact.toString();
									sortedlistContacts.set(Integer.parseInt(contact[1]),contact[0]);
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
							if (!message.getString("photo_id").isEmpty()) {
								photo_path = message.optString("photo_id");
								downloadCloudFront();
								listMessages.add(message.toString());
							}
							else {
								listMessages.add(message.toString());
								sendNotification(message.optString("receive_label"));
							}
						}
						Set<String> setMessages = new HashSet<String>(listMessages);
						editor = prefs.edit();
						editor.putString("message_timestamp", message_timestamp);
						editor.putStringSet("set_messages", setMessages);
						Set<String> setContacts = new HashSet<String>(sortedlistContacts);
						editor.putStringSet("set_contacts", setContacts);
						editor.commit();
					}
					catch (JSONException e) {
						e.printStackTrace();
					}
					get_my_status();
				}
				else {
					stopRefreshingOnTimeline();
				}
			}
			if (err_code.equals("200") && url_code.equals("get_send_choices")) {
				new AsyncTask<String, String, String>() {
					@Override
					protected String doInBackground(String... response) {
						ArrayList<String> newlistSendChoices = new ArrayList<String>();
						try {
							JSONArray all_send_choices = new JSONArray(response[0]);
							for (int j = 0; j < all_send_choices.length(); j++) {
								JSONObject single_send_choice = all_send_choices.getJSONObject(j);
								newlistSendChoices.add(single_send_choice.toString());
							}
							JSONObject json_calendar = new JSONObject();
							json_calendar.put("order_id"," 0");
							json_calendar.put("key", "calendar");
							json_calendar.put("send_label"," Calendar");
							newlistSendChoices.add(json_calendar.toString());
							sortSendChoices(newlistSendChoices);
							listSendChoices = newlistSendChoices;
							Set<String> setSendChoices = new HashSet<String>(newlistSendChoices);
							editor = prefs.edit();
							editor.putStringSet("set_send_choices", setSendChoices);
							editor.commit();
						}
						catch (JSONException e) {
							e.printStackTrace();
						}
						return null;
					}
					@Override
					protected void onPostExecute (String result) {
//						get_my_status();
					}
				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,response);
			}
		}
		Log.v(TAG,"getrequest : url_code = " + url_code
				+ " ; error_code = " + err_code + " ; response = " + response);
	}

	public void downloadCloudFront() {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			if (cognitoProvider==null) {
				try {
					new AsyncTask<String,Void,String>() {
						@Override
						protected String doInBackground(String... params) {
							try {
								cognitoProvider = getCredProvider(context);
							} catch (Exception e) {
								Log.v(TAG,"error cognito providers");
							}
							return "cognito done";
						}
						@Override
						protected void onPostExecute(String result) {
							if (cognitoProvider !=null)
								get_from_cloudfront();
							else
								Log.e(TAG,"Impossible to get cognito credentials !");
						}
					}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
				}
				catch (Exception e) {
				}
			}
			else
				get_from_cloudfront();
		}
	}

	public void stopRefreshingOnTimeline()  {
		try {
			Log.v(TAG,"begin stop refresh");
			PicteverApp mPicteverApp = (PicteverApp) context.getApplicationContext();
			if (mPicteverApp!=null && mPicteverApp.getCurrentActivity()!=null){
				Log.v(TAG,mPicteverApp.getCurrentActivity().getClass().getSimpleName());
				if (mPicteverApp.getCurrentActivity().getClass().getSimpleName().equals("Timeline")) {
					mPicteverApp.getCurrentActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							PicteverApp mPicteverApp = (PicteverApp) context.getApplicationContext();
							if (mPicteverApp!=null && mPicteverApp.getCurrentActivity()!=null){
								Log.v(TAG,mPicteverApp.getCurrentActivity().getClass().getSimpleName());
								if (mPicteverApp.getCurrentActivity().getClass().getSimpleName().equals("Timeline")) {
									Log.v(TAG,"ok this is timeline");
									mPicteverApp.getCurrentActivity().onContentChanged();
									SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) 
											mPicteverApp.getCurrentActivity().
											findViewById(R.id.swipe_container);
									if (swipeLayout.isRefreshing()) {
										swipeLayout.setRefreshing(false);
									}
								}
							}
						}
					});

				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void get_from_cloudfront() {
		String url_to_get = CLOUDFRONT + photo_path;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
			Log.v(TAG,"Start getting from Cloudfront");
			new AsyncTask<String, String,String>() {
				@Override
				protected String doInBackground(String... params) {
					InputStream inputStream = null;
					Bitmap bmp=null;
					String error_code="";
					try {
						HttpClient httpclient = new DefaultHttpClient();
						HttpContext localContext = new BasicHttpContext();
						localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
						HttpGet httpGet = new HttpGet(params[0]);
						HttpResponse httpResponse = httpclient.execute(httpGet,localContext);
						error_code = Integer.toString(httpResponse.getStatusLine().getStatusCode());
						inputStream = httpResponse.getEntity().getContent();
						if(inputStream != null)
							bmp = BitmapFactory.decodeStream(inputStream);
						if (bmp!=null) {
							Log.v(TAG,"ok j'ai le bitmap");
							int bmpwidth = bmp.getWidth();
							int bmpheight = bmp.getHeight();
							double ratio = (double) bmpwidth/bmpheight;
							if (SCREEN_HEIGHT==0) {
								SCREEN_HEIGHT = prefs.getInt("SCREEN_HEIGHT",0);
							}
							int desired_image_width = SCREEN_HEIGHT;
							int desired_image_height = (int) Math.round((double) desired_image_width/ratio);

							bmp = Bitmap.createScaledBitmap(bmp, desired_image_width,desired_image_height,false);
							Log.v(TAG,"ok bitmap rescaled");
							FileOutputStream out = null;
							File imageFileFolder = new File(Environment.getExternalStoragePublicDirectory(
									Environment.DIRECTORY_PICTURES),"Pictever");
							imageFileFolder.mkdirs();
							File imageFileName = new File(imageFileFolder,
									(photo_path.replaceAll(":", "")).replaceAll("-", ""));
							Log.v(TAG,"File where to store the picture : " + imageFileName.getPath());	
							try {
								out = new FileOutputStream(imageFileName);
								bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
								out.flush();
								out.close();
								out = null;
							} catch (Exception e) {
								e.printStackTrace();
							}		
							Log.v(TAG,"bitmap stored in file");
							sendNotification(display_notification);
						}
						else
							Log.v(TAG,"Unable to get Bitmap from file : " + photo_path);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}catch (IllegalArgumentException e) {
						e.printStackTrace();
					}catch (IllegalStateException e) {
						e.printStackTrace();
					}
					Log.v(TAG, "error_code = "+ error_code);
					return error_code;
				}			
				@Override
				protected void onPostExecute(String error) {
					if (error==null || error.isEmpty() || error.equals("403")) {
						Log.v(TAG,"Download from Cloudfront failed");
					}
				}
			}.execute(url_to_get);
		}
	}

	public void putInfosInContact(String mail, String user_ID, String phone1, String status,
			String facebook_id, String facebook_name,String facebook_birthday) {
		for (int i = 0; i < newlistContacts.size(); i++) {
			String c = newlistContacts.get(i);
			try {
				JSONObject json_contact = new JSONObject(c);
				if (json_contact.get("phoneNumber1").equals(phone1) 
						|| json_contact.get("phoneNumber2").equals(phone1)) {
					json_contact.put("contact_id", "id"+user_ID);
					json_contact.put("facebook_id",facebook_id);
					json_contact.put("facebook_name",facebook_name);
					json_contact.put("facebook_birthday",facebook_birthday);
					json_contact.put("email",mail);
					json_contact.put("status",status);
				}
				newlistContacts.set(i,json_contact.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private static String convertInputStreamToString(InputStream inputStream) throws IOException{
		BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
		String line = "";
		String result = "";
		while((line = bufferedReader.readLine()) != null)
			result += line;
		inputStream.close();
		return result;

	} 

	public void contacts_from_phone() {
		new asyn_contacts_from_phone().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
	}

	private class asyn_contacts_from_phone extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			newlistContacts = new ArrayList<String>();
			ContentResolver resolver = context.getContentResolver();
			Cursor c = resolver.query(
					Data.CONTENT_URI, 
					null, 
					Data.HAS_PHONE_NUMBER + "!=0 AND (" + Data.MIMETYPE + "=? OR " + Data.MIMETYPE + "=?)", 
					new String[]{Phone.CONTENT_ITEM_TYPE,Photo.PHOTO_URI},
					Data.TIMES_CONTACTED);
			String last_name="";
			String contact="";
			while (c.moveToNext()) {
				String name = c.getString(c.getColumnIndex(Data.DISPLAY_NAME));
				String phone1or2 = ""+ c.getString(c.getColumnIndex(Data.DATA1));
				String uri_photo = ""+ c.getString(c.getColumnIndex(Photo.PHOTO_URI));
				String times_contacted = c.getString(c.getColumnIndex(Data.TIMES_CONTACTED));
				phone1or2 = formatPhone(phone1or2);
				if (!name.equals(last_name)) {
					JSONObject jsoncontact = new JSONObject();
					try {
						jsoncontact.put("name", name);
						jsoncontact.put("phoneNumber1", phone1or2);
						jsoncontact.put("phoneNumber2", "");
						if (!uri_photo.equals(null)&& !uri_photo.equals("null")) 
							jsoncontact.put("uri_photo", uri_photo);
						else 
							jsoncontact.put("uri_photo", "");
						jsoncontact.put("contact_id", "num"+phone1or2);
						jsoncontact.put("is_selected","unselected");
						jsoncontact.put("times_contacted",times_contacted);
						contact = jsoncontact.toString();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					newlistContacts.add(contact);

				}
				else {
					try {
						JSONObject jsoncontact = new JSONObject(contact);
						jsoncontact.put("phoneNumber2", phone1or2);
						contact = jsoncontact.toString();
					} catch (JSONException e) {
						e.printStackTrace();
					}
					newlistContacts.set(newlistContacts.size()-1,contact);
				}
				last_name=name;
			}
			return "done";
		}

		protected void onPostExecute(String result){
			new AsyncTask<String, Void, String>(){
				@Override
				protected String doInBackground(String... params) {
					try {
						String[] contact_me = findContactFromPhone(newlistContacts,prefs.getString("user_phone", ""));
						if(contact_me[0].isEmpty()) {
							JSONObject json_me = new JSONObject();
							json_me.put("name", " Me");
							json_me.put("phoneNumber1", prefs.getString("user_phone", ""));
							json_me.put("phoneNumber2", "");
							json_me.put("uri_photo", "my_keo_image");
							json_me.put("contact_id", "num"+prefs.getString("user_phone", ""));
							json_me.put("is_selected","unselected");
							json_me.put("times_contacted","0");
							String contact = json_me.toString();
							newlistContacts.add(contact);
						}
						else {
							JSONObject json_me = new JSONObject(contact_me[0]);
							json_me.putOpt("name", " Me");
							json_me.putOpt("uri_photo", "my_image");
							contact_me[0] = json_me.toString();
							newlistContacts.set(Integer.parseInt(contact_me[1]), contact_me[0]);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					String json_phones = extractPhonesfromListContacts(newlistContacts);
					json_phones_to_upload_after_login = json_phones;
					ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
							Context.CONNECTIVITY_SERVICE);
					if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
						try {
							new asyn_login().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}	
					return null;
				}
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");		
		}
	}

	public String storePictureAfterTaken (byte[] data) {
		Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length); 
		//		Log.v(TAG,"bitmap initial size : " + bmp.getWidth() + " x " + bmp.getHeight());
		Matrix rotation_matrix = new Matrix();
		if (CAMERA_FACING.equals("back"))
			rotation_matrix.postRotate(90);
		else {
			rotation_matrix.postRotate(270);
			rotation_matrix.preScale(1.0f, -1.0f);
		}
		bmp = Bitmap.createBitmap(bmp,0,0,bmp.getWidth(),bmp.getHeight(),rotation_matrix,true);

		File imageFileFolder = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES),"Pictever");
		imageFileFolder.mkdirs();
		FileOutputStream out = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss",Locale.US);
		String photo_path = "photoPath" + sdf.format(new Date());
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
		return photo_path;
	}

	public String[] findContactFromPhone(ArrayList<String> listContacts2,String phone1or2_to_search) 
			throws JSONException {
		String[] response = new String[2];
		response[0] = "";
		response[1] = "";
		int i=0;
		for (String icontact : listContacts2) {
			JSONObject iJSONcontact = new JSONObject(icontact);
			if (iJSONcontact.get("phoneNumber1").equals(phone1or2_to_search)) {
				response[0] = iJSONcontact.toString();
				response[1] = Integer.toString(i);
				return response;
			}
			if (iJSONcontact.get("phoneNumber2").equals(phone1or2_to_search)) {
				response[0] = iJSONcontact.toString();
				response[1] = Integer.toString(i);
				return response;
			}
			i=i+1;
		}
		return response;
	}

	private String extractPhonesfromListContacts(ArrayList<String> list_contacts) {
		ArrayList<String> listPhones = new ArrayList<String>(); 
		for (String c : list_contacts) {
			try {
				JSONObject json_contact = new JSONObject(c);
				String phone1 = json_contact.optString("phoneNumber1");
				String phone2 = json_contact.optString("phoneNumber2");
				if (!phone1.isEmpty())
					listPhones.add(phone1);
				if (!phone2.isEmpty())
					listPhones.add(phone2);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		JSONArray json_array = new JSONArray(listPhones);
		return json_array.toString();
	}

	public String formatPhone (String phone) {
		phone = phone.replaceAll(" ", "");
		phone = phone.replaceAll("-", "");
		phone = phone.replaceAll("/","");
		if (phone.length() > 2) {
			if (!phone.startsWith("00")) {
				if (phone.startsWith("+")) {
					phone = phone.substring(1);
					phone = "00" + phone;
				}
				else {
					phone = phone.substring(1);
					if (prefs.getString("user_phone","").length() > 4)
						phone = "00" + prefs.getString("user_phone","").substring(2,4) + phone;
					else 
						phone = "0033"+ phone;
				}
			}
		}
		return phone;
	}

	public void sortSendChoices(ArrayList<String> listSendChoices) {
		Collections.sort(listSendChoices, new Comparator<String>() {
			public int compare(String c1, String c2) {
				String id1="",id2="";
				try {
					JSONObject jSON1 = new JSONObject(c1);
					JSONObject jSON2 = new JSONObject(c2);
					id1 = jSON1.optString("order_id");
					id2 = jSON2.optString("order_id");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return id1.compareTo(id2);
			}
		});

	}

	public void sortMessages(ArrayList<String> listMessages) {
		Collections.sort(listMessages, new Comparator<String>() {
			public int compare(String c1, String c2) {
				Double timestamp1=0.0,timestamp2=0.0,created_at1=0.0,created_at2=0.0;
				try {
					JSONObject jSON1 = new JSONObject(c1);
					JSONObject jSON2 = new JSONObject(c2);
					timestamp1 = Double.valueOf(jSON1.optString("timestamp"));
					timestamp2 = Double.valueOf(jSON2.optString("timestamp"));
					created_at1 = Double.valueOf(jSON1.optString("created_at"));
					created_at2 = Double.valueOf(jSON2.optString("created_at"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (timestamp1.compareTo(timestamp2)==0)
					return -created_at1.compareTo(created_at2);
				else
					return -timestamp1.compareTo(timestamp2);
			}
		});
	}

	public void sortContacts(ArrayList<String> listContacts2) {
		contacts_to_remove = new ArrayList<String>();
		Collections.sort(listContacts2, new Comparator<String>() {
			public int compare(String c1, String c2) {
				String id1="",id2="",name1="",name2="";
				int times_contacted1=0,times_contacted2=0,timesContacted1=0,timesContacted2=0;
				try {
					JSONObject jSON1 = new JSONObject(c1);
					JSONObject jSON2 = new JSONObject(c2);
					if (jSON1.optString("contact_id").equals(jSON2.optString("contact_id")))
						contacts_to_remove.add(c2);
					times_contacted1 = prefs.getInt(jSON1.optString("contact_id")+ "_times_contacted", 0);
					times_contacted2 = prefs.getInt(jSON2.optString("contact_id")+ "_times_contacted", 0);
					id1 = jSON1.optString("contact_id").substring(0,2);
					id2 = jSON2.optString("contact_id").substring(0,2);
					if (id1.equals("nu")&& id2.equals("nu")) {
						if (!jSON1.optString("times_contacted").isEmpty() 
								&& !jSON2.optString("times_contacted").isEmpty()) {
							timesContacted1 = Integer.parseInt(jSON1.optString("times_contacted"));
							timesContacted2 = Integer.parseInt(jSON2.optString("times_contacted"));
						}
					}
					name1 = jSON1.optString("name");
					name2 = jSON2.optString("name");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (times_contacted1!=times_contacted2)
					return times_contacted2-times_contacted1;
				if (id1.compareTo(id2) != 0)
					return id1.compareTo(id2);
				else {
					if (timesContacted1!=timesContacted2 && (timesContacted1 > 1 || timesContacted2 > 1))
						return timesContacted2-timesContacted1;
					else
						return name1.compareTo(name2);
				}
			}
		});
		for (String c : contacts_to_remove)
			listContacts2.remove(c);
	}

	public String computeHash(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException{
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.reset();
		byte[] byteData = digest.digest(input.getBytes("UTF-8"));
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteData.length; i++)
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		return sb.toString();
	}

	public void updateTvContact(TextView tvContact, ArrayList<String> list_names_selected) throws JSONException {
		int size = list_names_selected.size();
		if (size==0){
			tvContact.setText("");
			tvContact.setVisibility(View.INVISIBLE);
		}
		else {
			String name = list_names_selected.get(0);
			int separation = name.indexOf(" ");
			if (separation!=-1 && separation!=0)
				name = name.substring(0, separation);
			tvContact.setText(name);
			if (size > 1) {
				for (int i = 1; i < size; i++) {
					name = list_names_selected.get(i);
					separation = name.indexOf(" ");
					if (separation!=-1 && separation!=0)
						name = name.substring(0, separation);
					tvContact.setText(name + ", " + tvContact.getText());
				}
			}
		}
		if (tvContact.getText().toString().length() > 13 )
			tvContact.setText(tvContact.getText().toString().substring(0, 13)+ " ...");
	}

	public void sendNotification(String display) {
		Log.v(TAG,"refresh timeline");
		stopRefreshingOnTimeline();
		NotificationManager mNotificationManager = (NotificationManager)
				context.getSystemService(Context.NOTIFICATION_SERVICE);
		from_notif = true;
		PendingIntent contentIntent;
		contentIntent = PendingIntent.getActivity(context, 0,
				new Intent(context, Timeline.class), 0);
		Notification.Builder mBuilder =
				new Notification.Builder(context)
		.setSmallIcon(R.drawable.notif_spiral36)
		.setContentTitle("Pictever")
		.setStyle(new Notification.BigTextStyle()
		.bigText(display))
		.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.notif_spiral))
		.setAutoCancel(true)
		.setTicker("New message on Pictever!")
		.setContentText(display)
		.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(1, mBuilder.build());
	}

	public void unselectAllContacts(ArrayList<String> list_contacts) {
		for (int i = 0; i < list_contacts.size(); i++) {
			try {
				JSONObject json_contact = new JSONObject(list_contacts.get(i));
				json_contact.put("is_selected", "unselected");
				list_contacts.set(i,json_contact.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public int which_color(String receive_color) {
		if (receive_color.equals("null"))
			return Color.parseColor("purple");
		else {
			int color = R.color.DarkCyan;
			try {
				color = Color.parseColor("#"+receive_color);
			}
			catch (IllegalArgumentException e) {
				color = R.color.DarkCyan;
			}
			return color;
		}
	}

	public String dateDisplay (long millis_to_display, long current_millis) {
		String display = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
		SimpleDateFormat sdf_week = new SimpleDateFormat("EEEE",Locale.US);
		String date_to_display = sdf.format(new Date(millis_to_display));
		String current_date = sdf.format(new Date(current_millis));
		String day_of_the_week_to_display = sdf_week.format(new Date(millis_to_display));
		String year_to_display = date_to_display.substring(0,4);
		String month_to_display = date_to_display.substring(5,7);
		String day_to_display = date_to_display.substring(8,10);
		String hour_to_display = date_to_display.substring(11,13);
		String minutes_to_display = date_to_display.substring(14,16);
		String current_year = current_date.substring(0,4);
		String current_month = current_date.substring(5,7);
		String current_day = current_date.substring(8,10);
		String current_hour = current_date.substring(11,13);
		String current_minutes = current_date.substring(14,16);

		if (!year_to_display.equals(current_year))
			display = day_to_display + " " + monthToString(month_to_display) + " " + year_to_display;
		else {
			if (month_to_display.equals(current_month)) {
				if (day_to_display.equals(current_day)) {
					if (hour_to_display.equals(current_hour)) {
						if (minutes_to_display.equals(current_minutes))
							display = "Now";
						else {
							int diff = Integer.parseInt(current_minutes) - Integer.parseInt(minutes_to_display);
							if (diff < 1 )
								display = "Now";
							else 
								display = Integer.toString(diff) + " mins ago";
						}
					}
					else {
						int diff = Integer.parseInt(current_hour) - Integer.parseInt(hour_to_display);
						if (diff==1)
							display = Integer.toString(diff) + " hour ago";
						else
							display = Integer.toString(diff) + " hours ago";
					}
				}
				else {
					int diff = Integer.parseInt(current_day) - Integer.parseInt(day_to_display);
					if (diff==1)
						display = "Yesterday at " + hour_to_display + ":"+ minutes_to_display;
					else {
						if (diff > 0 && diff < 6 )
							display = day_of_the_week_to_display + " at " + hour_to_display + ":"+ minutes_to_display;
						else
							display = day_to_display + " " + monthToString(month_to_display) + " at " 
									+ hour_to_display + ":"+ minutes_to_display;
					}
				}
			}
			else 
				display = day_to_display + " " + monthToString(month_to_display) + " at " 
						+ hour_to_display + ":"+ minutes_to_display;
		}
		return display;
	}

	public String monthToString(String month) {
		switch (Integer.parseInt(month)) {
		case 1 : return "jan.";
		case 2 : return "feb.";
		case 3 : return "mar.";
		case 4 : return "apr.";
		case 5 : return "may";
		case 6 : return "jun.";
		case 7 : return "jul.";
		case 8 : return "aug.";
		case 9 : return "sep.";
		case 10 : return "oct.";
		case 11 : return "nov.";
		case 12 : return "dec.";
		}
		return "";
	}

	public void extractMessagesInfo (String[] receive_color, String[] received_date, 
			String[] received_label, String[] created_at, String[] message,  
			String[] photo_id,String[] contact_name, String[] uri_icon) {
		int i=0;
		for (String jmessage : listMessages) {
			try {
				JSONObject json_message = new JSONObject(jmessage);
				received_date[i] = " Before update ";
				receive_color[i] = json_message.optString("receive_color","008b8b");
				long received_long = json_message.optLong("received_at");
				if (received_long!=0)
					received_date[i]=dateDisplay(received_long*1000, System.currentTimeMillis());
				received_label[i] = json_message.optString("receive_label");
				message[i] = json_message.optString("message");
				String created = json_message.optString("created_at");
				long longc = (long)(1000*Double.parseDouble(created));
				created_at[i] = dateDisplay(longc, System.currentTimeMillis());
				if (received_label[i].equals("calendar"))
					received_label[i] = created_at[i];
				photo_id[i] = json_message.optString("photo_id");
				String[] contact = findContactFromPhone(sortedlistContacts, json_message.optString("from_numero"));
				if (!contact[0].isEmpty()) {
					contact_name[i] = (new JSONObject(contact[0])).optString("name");
					uri_icon[i] = (new JSONObject(contact[0])).optString("uri_photo");
				}
				else {
					String name= json_message.optString("from_numero");
					if (json_message.optString("from_email").length() > 5) 
						name = name + " (" + json_message.optString("from_email").substring(0,5) + "...)";
					else 
						name = name + " (" + json_message.optString("from_email") + " )";
					contact_name[i] = name;
					uri_icon[i]="";
				}
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			i=i+1;
		}
	}

	public void manage_progress_bar() {
		if (upload_is_active.equals("yes")) {
			if (upload==null) {
				File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
						+ "/Pictever/" + last_photo_path);
				Log.v(TAG,"size file : " + Integer.toString((int)file.length()));
				sendToAmazon();
			}
			else {
				ll_progress.setVisibility(View.VISIBLE);
				ll_progress_params = new RelativeLayout.LayoutParams(
						Math.round((float) uploadProgression/100*prefs.
								getInt("SCREEN_HEIGHT",0)),
								Math.round((float) prefs.getInt("SCREEN_WIDTH", 500)/100));
				if (rl_title!=null)
					ll_progress_params.addRule(RelativeLayout.BELOW,rl_title.getId());
				ll_progress.setLayoutParams(ll_progress_params);
			}
		}
		else {
			if (upload_is_active.equals("failed")) {
				ll_progress.setVisibility(View.VISIBLE);
				ll_progress.setBackgroundResource(R.color.LightGrey);
				ll_progress_params = new RelativeLayout.LayoutParams(
						Math.round((float) uploadProgression/100*prefs.
								getInt("SCREEN_HEIGHT",0)),
								Math.round((float) prefs.getInt("SCREEN_WIDTH", 500)/100));
				if (rl_title!=null)
					ll_progress_params.addRule(RelativeLayout.BELOW,rl_title.getId());
				ll_progress.setLayoutParams(ll_progress_params);
				if (tvRetry!=null) {
					tvRetry.setVisibility(View.VISIBLE);
				}
			}
			else {
				ll_progress.setVisibility(View.INVISIBLE);
				if (tvRetry!=null) {
					tvRetry.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	public void sendToAmazon() {
		if (cognitoProvider==null) {
			try {
				new AsyncTask<String,Void,String>() {
					@Override
					protected String doInBackground(String... params) {
						try {
							cognitoProvider = getCredProvider(context);
						} catch (Exception e) {
							Log.v(TAG,"error cognito providers");
						}
						return "cognito done";
					}
					@Override
					protected void onPostExecute(String result) {
						if (cognitoProvider !=null)
							upload_picture();
						else 
							Log.e(TAG,"Impossible to get cognito credentials !");
					}
				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
			}
			catch (Exception e) {
			}
		}
		else
			upload_picture();
	}

	public void upload_picture() {
		try {
			new AsyncTask<String,Void,String>() {
				@Override
				protected String doInBackground(String... params) {
					File imageFileFolder2 = new File(Environment.getExternalStoragePublicDirectory(
							Environment.DIRECTORY_PICTURES),"Pictever");
					imageFileFolder2.mkdir();
					File imageFileName2 = new File(imageFileFolder2,last_photo_path);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss",Locale.US);

					last_picture_name = prefs.getString("hash_user_id", "")
							+ sdf.format(new Date())+ ".jpg";
					transferManagerUp = new TransferManager(cognitoProvider);
					Log.v(TAG,"uploading on " + BUCKET_NAME);
					upload = transferManagerUp.upload(
							BUCKET_NAME,
							last_picture_name,
							imageFileName2);
					ll_progress.setVisibility(View.VISIBLE);
					upload.addProgressListener(new ProgressListener() {
						@Override
						public void progressChanged(ProgressEvent progressEvent) {
							PicteverApp mPicteverApp = (PicteverApp) context.getApplicationContext();							
							Log.i(TAG,"upload progress : " + upload.getProgress().
									getPercentTransferred() + "%");
							uploadProgression = Math.round(
									(float) upload.getProgress().getPercentTransferred());
							if (mPicteverApp!=null && mPicteverApp.getCurrentActivity()!=null) {
								mPicteverApp.getCurrentActivity().runOnUiThread(new Runnable(){
									@Override
									public void run(){
										ll_progress.setBackgroundResource(R.color.OrangeKeo);
										ll_progress_params = new RelativeLayout.LayoutParams(
												Math.round((float) uploadProgression/100*
														prefs.getInt("SCREEN_HEIGHT",0)),
														Math.round((float) 
																prefs.getInt("SCREEN_WIDTH", 500)/100));
										if (rl_title!=null) {
											ll_progress_params.addRule(RelativeLayout.BELOW,
													rl_title.getId());
										}
										ll_progress.setLayoutParams(ll_progress_params);
									}
								});
							}
							if (upload.getState().name().equals("Failed") || 
									upload.getState().name().equals("Canceled")) {
								if (mPicteverApp!=null && mPicteverApp.getCurrentActivity()!=null) {
									mPicteverApp.getCurrentActivity().runOnUiThread(new Runnable(){
										@Override
										public void run(){
											Log.v(TAG,"upload failed or canceled : "
													+ upload.getState().name() );
											if (tvRetry!=null)
												tvRetry.setVisibility(View.VISIBLE);
											upload_is_active = "failed";
										}
									});
								}
							}

							if (progressEvent.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
								Log.v(TAG,"Upload complete!");
								if (mPicteverApp!=null && mPicteverApp.getCurrentActivity()!=null) {
									mPicteverApp.getCurrentActivity().runOnUiThread(new Runnable(){
										@Override
										public void run(){
											ll_progress.setVisibility(View.INVISIBLE);
											if (tvRetry!=null)
												tvRetry.setVisibility(View.INVISIBLE);
										}
									});
								}
								upload_is_active = "no";
								uploadProgression  = 0; 
								File imageFileFolder2 = new File(Environment.getExternalStoragePublicDirectory(
										Environment.DIRECTORY_PICTURES),"Pictever");
								imageFileFolder2.mkdir();
								File imageFileName2 = new File(imageFileFolder2,last_photo_path);
								Boolean deleted = imageFileName2.delete();
								context.deleteFile(last_photo_path);
								if (deleted) {
									if(analytics != null) {
										JSONArray json_array;
										try {
											json_array = new JSONArray(last_contact_ids);
											AnalyticsEvent sendTextMessageEvent = analytics.
													getEventClient().createEvent(
															"androidSendPhotoFrom"+picture_from);
											sendTextMessageEvent.addAttribute("number_of_receivers",
													Integer.toString(json_array.length()));
											sendTextMessageEvent.addAttribute("send_label",last_send_label);
											analytics.getEventClient().recordEvent(sendTextMessageEvent);
										} catch (JSONException e) {
											e.printStackTrace();
										}
									}
									sendMessage("",last_picture_name, 
											last_contact_ids,last_send_choice);
									for (String id : last_list_ids_selected) {
										editor = prefs.edit();
										int times_contacted = prefs.getInt(id+"_times_contacted", 0);
										editor.putInt(id+"_times_contacted", times_contacted+1);
										if (id.startsWith("num")){
											int contacts_invited = prefs.getInt("contacts_invited", 0);
											editor.putInt("contacts_invited", contacts_invited+1);
											SmsManager smsManager = SmsManager.getDefault();
											smsManager.sendTextMessage(id.substring(3), null, 
													"I just sent you a photo in the future on Pictever! " +
															"To get the app and see it: " +
															"http://pictever.com",
															null, null);
											editor.commit();
										}
										
									}
								}
								upload = null;
							}
						}
					});
					return "done";
				}
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
		}
		catch (Exception e) {
		}
	}
}
