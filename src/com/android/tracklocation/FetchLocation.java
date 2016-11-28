/**
 * root
 * 2:19:40 PM
 */
package com.android.tracklocation;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class FetchLocation {
	Timer timer1;
	LocationManager locationManager;
	LocationResult locationResult;
	boolean gps_enabled = false;
	boolean network_enabled = false;
	Context context;
	final int MAX_ATTEMPTS = 10;
	public static int GPS_OPTION_REQ_CODE = 1; // Meters
	int currentAttempt = 1;
	Handler mHandler;

	public boolean getLocation(Context context, LocationResult result, Handler handler) {
		this.context = context;
		this.mHandler = handler;
		// I use LocationResult callback class to pass location value from
		// MyLocation to user code.
		locationResult = result;
		if (locationManager == null)
			locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		// exceptions will be thrown if provider is not permitted.
		try {
			gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}
		try {
			network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}

		// don't start listeners if no provider is enabled
		if (!gps_enabled && !network_enabled)
			return false;

		if (gps_enabled)
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
		if (network_enabled)
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);

		timer1 = new Timer();
		timer1.schedule(new GetLastLocation(), 5000, 5000);
		return true;
	}

	LocationListener locationListenerGps = new LocationListener() {
		public void onLocationChanged(Location location) {
			// timer1.cancel();
			locationResult.gotLocation(location, mHandler);
			// lm.removeUpdates(this);
			// lm.removeUpdates(locationListenerNetwork);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	LocationListener locationListenerNetwork = new LocationListener() {
		public void onLocationChanged(Location location) {
			// timer1.cancel();
			locationResult.gotLocation(location, mHandler);
			// two change
			// lm.removeUpdates(this);
			// lm.removeUpdates(locationListenerGps);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	class GetLastLocation extends TimerTask {
		@Override
		public void run() {
			// two change
			// lm.removeUpdates(locationListenerGps);
			// lm.removeUpdates(locationListenerNetwork);

			Log.d("iGPS", "GPSLastLocation- GET LAST LOCATION ** Attempt =" + currentAttempt++);

			Location net_loc = null, gps_loc = null;
			if (gps_enabled)
				gps_loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (network_enabled)
				net_loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

			// if there are both values use the latest one
			if (gps_loc != null && net_loc != null) {
				if (gps_loc.getTime() > net_loc.getTime())
					locationResult.gotLocation(gps_loc, mHandler);
				else
					locationResult.gotLocation(net_loc, mHandler);
				return;
			}

			if (gps_loc != null) {
				locationResult.gotLocation(gps_loc, mHandler);
				return;
			}
			if (net_loc != null) {
				locationResult.gotLocation(net_loc, mHandler);
				return;
			}

			if (currentAttempt > MAX_ATTEMPTS) {
				Log.d("tag", "Sorry :Finishing it up.");
				locationResult.gotLocation(null, mHandler);
			}
		}
	}

	public static abstract class LocationResult {
		public abstract void gotLocation(Location location, Handler handler);
	}

	public void shutDown() {
		
		if (timer1 != null) {
			timer1.cancel();
		}
		locationManager.removeUpdates(locationListenerGps);
		locationManager.removeUpdates(locationListenerNetwork);

	}

//	public void shutTime() {
//		if (timer1 != null) {
//			timer1.cancel();
//		}
//	}
}
