package ch.luethi.skylinesclient;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.geeksville.location.SkyLinesTrackingWriter;

import java.net.SocketException;
import java.net.UnknownHostException;


public class PositionService extends Service implements LocationListener {

    private SkyLinesTrackingWriter skyLinesTrackingWriter = null;
    private LocationManager locationManager;
    private SkyLinesPrefs prefs;
    private static final String TAG = "POS";
    private int posCount = 0;

    private HandlerThread senderThread;


    @Override
    public void onCreate() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        prefs = new SkyLinesPrefs(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        senderThread = new HandlerThread("SenderThread");
        senderThread.start();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefs.getTrackingInterval() * 1000, 0, this, senderThread.getLooper());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(this);
        skyLinesTrackingWriter = null;
        senderThread.getLooper().quit();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getLatitude() != 0.0) {
            double lat = location.getLatitude();
            double longitude = location.getLongitude();
            // convert m/sec to km/hr
            float kmPerHr = location.hasSpeed() ? location.getSpeed() * 3.6F : Float.NaN;
            float[] accelVals = null;
            float vspd = Float.NaN;
            Log.d(TAG, "onLocationChanged, before emitPosition");
            getOrCreateSkyLinesTrackingWriter().emitPosition(location.getTime(), lat, longitude,
                    location.hasAltitude() ? (float) location.getAltitude() : Float.NaN,
                    (int) location.getBearing(), kmPerHr, accelVals, vspd);
            Log.d(TAG, "onLocationChanged, after emitPosition");
            sendPositionStatus();
        }
    }


    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }


    private boolean isEmulator() {
        return Build.MANUFACTURER.equals("unknown");
    }

    private SkyLinesTrackingWriter getOrCreateSkyLinesTrackingWriter() {
        if (skyLinesTrackingWriter == null) {
            String ip_address;
            if (isEmulator()) {
                ip_address = "10.20.11.27";
            } else {
                //ip_address = "78.47.50.46";  // the real one
                ip_address = "luethi.dyndns.org";   // ToDo - this is only for testing
            }
            try {
                skyLinesTrackingWriter = new SkyLinesTrackingWriter(prefs.getTrackingKey(), ip_address);
            } catch (SocketException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (UnknownHostException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return skyLinesTrackingWriter;
    }

    private void sendPositionStatus() {
        Intent intent = new Intent(MainActivity.BROADCAST_STATUS);
        intent.putExtra(MainActivity.MESSAGE_POS_STATUS, ++posCount);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
