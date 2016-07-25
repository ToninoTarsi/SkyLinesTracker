/*
 * SkyLines Tracker is a location tracking client for the SkyLines platform <www.skylines-project.org>.
 * Copyright (C) 2013  Andreas Lüthi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.luethi.skylinestracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    private HandlerThread senderThread;
    private String ipAddress;

    private static SkyLinesApp app;
    private static Intent intentPosStatus, intentWaitStatus, intentConStatus;

    private Handler delayHandler = new Handler();
    private Runnable timerRunnable;


    public PositionService() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("SkyLines", "timerRunnable, isOnline()=" + isOnline());
                if (isOnline()) {
                    new DequeueTask().execute();
                }
                startTimer();
            }
        };
    }


    @Override
    public void onCreate() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        prefs = new SkyLinesPrefs(this);
        app = ((SkyLinesApp) getApplicationContext());
        app.positionService = this;
        intentPosStatus = new Intent(MainActivity.BROADCAST_STATUS);
        intentPosStatus.putExtra(MainActivity.MESSAGE_STATUS_TYPE, MainActivity.MESSAGE_POS_STATUS);
        intentWaitStatus = new Intent(MainActivity.BROADCAST_STATUS);
        intentWaitStatus.putExtra(MainActivity.MESSAGE_STATUS_TYPE, MainActivity.MESSAGE_POS_WAIT_STATUS);
        intentConStatus = new Intent(MainActivity.BROADCAST_STATUS);
        intentConStatus.putExtra(MainActivity.MESSAGE_STATUS_TYPE, MainActivity.MESSAGE_CON_STATUS);
        delayHandler = new Handler();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean init = intent.getBooleanExtra("init", false);
        if (prefs.isQueueFixes()) {
            app.fixStack = new FixQueue(getApplicationContext(), init);
        } else {
            app.fixStack = new FixQueueNop(getApplicationContext());
        }
        Log.d("SkyLines", "SkyLinesApp, onStartCommand(), fixStack.size()=" + app.fixStack.size() + ", init=" + init);

        skyLinesTrackingWriter = null;
        ipAddress = prefs.getIpAddress();
        senderThread = new HandlerThread("SenderThread");
        senderThread.start();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefs.getTrackingInterval() * 1000, 0, this, senderThread.getLooper());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(this);
        skyLinesTrackingWriter = null;
        app.positionService = null;
        Looper looper = senderThread == null ? null : senderThread.getLooper();
        if (looper != null) {
            looper.quit();
        }
        stopTimer();
        Log.d("SkyLines", "PositionService, onDestroy()");
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        stopTimer();
        SkyLinesTrackingWriter skyLinesTrackingWriter = getOrCreateSkyLinesTrackingWriter();
        if (skyLinesTrackingWriter != null) { // fix NPE #11
            if (location.getLatitude() != 0.0) {
                app.lastLat = location.getLatitude();
                app.lastLon = location.getLongitude();
                // convert m/sec to km/hr
                float kmPerHr = location.hasSpeed() ? location.getSpeed() * 3.6F : Float.NaN;
                float[] accelVals = null;
                float vspd = Float.NaN;
                skyLinesTrackingWriter.emitPosition(location.getTime(), app.lastLat, app.lastLon,
                        location.hasAltitude() ? (float) location.getAltitude() : Float.NaN,
                        (int) location.getBearing(), kmPerHr, accelVals, vspd);
                if (app.guiActive) {
                    if (isOnline()) {
                        sendPositionStatus();
                    } else {
                        sendConnectionStatus();
                    }
                }
            } else {
                if (isOnline())
                    skyLinesTrackingWriter.dequeAndSendFix();
            }
        }
        Log.d("SkyLines", "onLocationChanged, isOnline()=" + isOnline());
        startTimer();
    }


    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        if (i != LocationProvider.AVAILABLE) {
            sendPositionWaitStatus();
        }
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
        if (app.guiActive)
            sendPositionWaitStatus();
    }

    public static boolean isOnline() {
        NetworkInfo networkInfo = ((ConnectivityManager) app.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        } else {
            return false;
        }
    }

    public void broadcastReceiver() {
        if (skyLinesTrackingWriter != null) {
            skyLinesTrackingWriter.dequeAndSendFix();
        }
    }

    private SkyLinesTrackingWriter getOrCreateSkyLinesTrackingWriter() {
        if (skyLinesTrackingWriter == null) {
            try {
                skyLinesTrackingWriter = new SkyLinesTrackingWriter(prefs.getTrackingKey(), ipAddress);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return skyLinesTrackingWriter;
    }

    private void sendPositionStatus() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentPosStatus);
    }

    private void sendPositionWaitStatus() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentWaitStatus);
    }

    private void sendConnectionStatus() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentConStatus);
    }

    private void startTimer() {
        delayHandler.postDelayed(timerRunnable, 2 * (prefs.getTrackingInterval() * 1000));
    }

    private void stopTimer() {
        delayHandler.removeCallbacks(timerRunnable);
    }

    private class DequeueTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            if (skyLinesTrackingWriter != null) {
                skyLinesTrackingWriter.dequeAndSendFix();
            }
            return null;
        }
    }

}
