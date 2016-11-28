package com.android.tracklocation;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.widget.Toast;

import com.android.tracklocation.FetchLocation.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class GPSTrackerActivity extends FragmentActivity {

	// Google Map
	private GoogleMap googleMap;
	public final static int RESPONSE_GPS_LOCATION = 3, UNABLE_TO_GET_LOCATION = 4, RESPONSE_ERROR_MYLOCATION = 5,
			RESPONSE_SUCCESFUL_MYLOCATION = 6;

	FetchLocation current_location;
	private LocationManager locationManager;
	private boolean network_enabled, gps_enabled;
	MarkerOptions currentLocationMarker, newLocationMarker;
	public LatLng Initial_latlan, New_atlan;
	public double longitude, latitude;
	AlertDialog alert_dialog;
	Location Initial_Location;
	private boolean isForeGround = false;
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;
	public static final int NOTIFICATION_ID = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onStart() {
		super.onStart();
		isForeGround = true;
		initilizeMap();
		startGPS(mHandler);
	}

	@Override
	protected void onStop() {
		super.onStop();
		isForeGround = false;
	}

	/**
	 * function to load map. If map is not created it will create it for you
	 * */
	private void initilizeMap() {
		if (googleMap == null) {
			googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

			// check if map is created successfully or not
			if (googleMap == null) {
				Toast.makeText(getApplicationContext(), "Unable to initiate google map", Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Handles controller response..
	 * 
	 */
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			switch (msg.what) {

			case UNABLE_TO_GET_LOCATION:
				Toast.makeText(getApplicationContext(),
						"Unable to retrive Location at this time.\nRetry after some time.", Toast.LENGTH_SHORT).show();
				break;

			// called from MyLocation.locationResult implementation on ERROR
			case RESPONSE_ERROR_MYLOCATION:

				// show error dialogs.
				GPSErrorReport();
				break;

			// called from MyLocation.locationResult implementation on
			// SUCCESFULL LOGIN
			case RESPONSE_SUCCESFUL_MYLOCATION:
				// cancle progress dialog.
				Location location = (Location) msg.obj;
				longitude = location.getLongitude();
				latitude = location.getLatitude();

				// new RegistrationTask().execute();

				if (Initial_latlan == null) {
					// create marker
					Initial_latlan = new LatLng(latitude, longitude);

					currentLocationMarker = new MarkerOptions().position(Initial_latlan).title("Location1");

					// adding marker
					googleMap.addMarker(currentLocationMarker);
					googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(Initial_latlan, 16));
					Initial_Location = location;
				} else {
					New_atlan = new LatLng(latitude, longitude);

					double distance = Initial_Location.distanceTo(location);
//					double distance = calculateDistance(New_atlan.latitude, New_atlan.longitude,
//							Initial_latlan.latitude, Initial_latlan.longitude);

//					Toast.makeText(getApplicationContext(), "Location Update Distance : " + distance,
//							Toast.LENGTH_SHORT).show();
					if (distance >= 1609) {

						if (isForeGround) {
							newLocationMarker = new MarkerOptions().position(New_atlan).title("Location2");
							googleMap.addMarker(newLocationMarker);
							googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(New_atlan, 16));
						} else {

							sendNotification();

						}
						try {
							current_location.shutDown();
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

				}
				break;

			default:
				break;
			}

		}

	};

	private void sendNotification() {
		mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

		Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

		Intent homeIntent = new Intent(this, GPSTrackerActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, homeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("Track Location").setSound(uri)
				.setStyle(new NotificationCompat.BigTextStyle().bigText("You have travelled 1 mile"))
				.setContentText("You have travelled 1 mile").setAutoCancel(true);

		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

	}

	/**
	 * Starts GPS and retrives lat,long.
	 */
	public void startGPS(Handler handler) {

		// Check if GPS or Network based GPS is availbale

		if (locationManager == null)
			locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

		try {
			gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}
		try {
			network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}

		if (!gps_enabled && !network_enabled) {
			// There is no way we can get GPS location
			// SHow Error message : GPS
			// AlertDialog("Location not found!", false);
			GPSAlertDialog(handler);
			return;
		} else {

			current_location = new FetchLocation();
			current_location.getLocation(this, locationResult, handler);
		}

		/***********
		 * // Start GPS if (gpsLocation == null) gpsLocation = new
		 * GPSLocation(mContext, this);
		 * 
		 * Log.d(tag, "Calling getLoc"); gpsLocation.getLocation(mHandler,
		 * this);
		 ***/
	}

	public void GPSErrorReport() {

		try {
			current_location.shutDown();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Toast.makeText(getApplicationContext(), "Unable to retrieve Location at this time.\nRetry after some time.",
				Toast.LENGTH_LONG).show();

	}

	public LocationResult locationResult = new LocationResult() {

		@Override
		public void gotLocation(Location location, Handler mHandler) {

			if (location == null) {
				// GPS cordinates not available
				Message msg = new Message();
				msg.what = RESPONSE_ERROR_MYLOCATION;
				mHandler.sendMessage(msg);
			} else {

				Message msg = new Message();
				msg.what = RESPONSE_SUCCESFUL_MYLOCATION;
				msg.obj = location;
				mHandler.sendMessage(msg);

				// getClosestAccessNo("" + location.getLatitude() ,
				// "" +location.getLongitude());

			}
		}
	};

	public void GPSAlertDialog(final Handler mHandler) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		if (alert_dialog == null) {
			alertDialogBuilder.setTitle("Track Location!");
			alertDialogBuilder.setMessage("GPS is disable. Do you want to turn on?").setCancelable(false)
					.setPositiveButton(Html.fromHtml("Turn On"), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Intent callGPSSettingIntent = new Intent(
									android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);

							startActivity(callGPSSettingIntent);
							dialog.dismiss();

						}
					});
			alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {

					// GPS cordinates not available
					Message msg = new Message();
					msg.what = RESPONSE_ERROR_MYLOCATION;
					mHandler.sendMessage(msg);
					dialog.dismiss();
				}
			});

			alert_dialog = alertDialogBuilder.show();
			// TextView messageText = (TextView)
			// alert_dialog.findViewById(android.R.id.message);
			// messageText.setGravity(Gravity.CENTER);
			// Button positiveButton =
			// alert_dialog.getButton(DialogInterface.BUTTON_POSITIVE);
			// Button negativeButton =
			// alert_dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
			// if (positiveButton != null) {
			// positiveButton.setTextColor(Color.parseColor("#4594d9"));
			// positiveButton.setTextSize(18);
			//
			// }
			// if (negativeButton != null) {
			// negativeButton.setTextColor(Color.parseColor("#4594d9"));
			// negativeButton.setTextSize(18);
			//
			// }
			alert_dialog.show();
		}
	}

	private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))
				* Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		return dist;
//		DecimalFormat df = new DecimalFormat("#.00");
//		return Double.parseDouble(df.format(dist));
	}

	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	private double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}

}
