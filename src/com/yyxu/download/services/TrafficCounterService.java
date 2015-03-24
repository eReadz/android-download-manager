
package com.yyxu.download.services;

import com.yyxu.download.utils.DownloadManagerStorage;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class TrafficCounterService extends Service {
    private static final String TAG = "TrafficCounterService";
    private static final int SAMPLING_RATE = 1000;

    private Timer timer;
    private TimerTask timerTask;

    private ConnectivityManager connectivityManager;
    private TelephonyManager telephonyManager;
    private DownloadManagerStorage downloadManagerStorage;
    private int mUid;

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {

        Log.d(TAG, "onCreate()");

        downloadManagerStorage = new DownloadManagerStorage(this);
        connectivityManager = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        PackageManager packageManager = this.getPackageManager();
        ApplicationInfo info = null;
        try {
            info = packageManager.getApplicationInfo(this.getPackageName(),
                    PackageManager.GET_META_DATA);
            mUid = info.uid;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        timer = new Timer();
        timerTask = new TimerTask() {

            @Override
            public void run() {

                getTrafficStats();
            }
        };

        if (timer != null && timerTask != null) {
            preTx = curTx = TrafficStats.getUidTxBytes(mUid);
            preRx = curRx = TrafficStats.getUidRxBytes(mUid);
            Log.d(TAG, "cur" + curRx);
            timer.schedule(timerTask, 0, SAMPLING_RATE);

            // check the network operator is changed or not
            operatorName = telephonyManager.getNetworkOperatorName();
            if (!downloadManagerStorage.getString(DownloadManagerStorage.KEY_Network_Operator_Name).equals(
                    operatorName)) {
                downloadManagerStorage.setString(DownloadManagerStorage.KEY_Network_Operator_Name, operatorName);
                downloadManagerStorage.setLong(DownloadManagerStorage.KEY_RX_MOBILE, 0L);
                downloadManagerStorage.setLong(DownloadManagerStorage.KEY_TX_MOBILE, 0L);
                // TODO Network operator has changed
            }
        }

    }

    @Override
    public void onStart(Intent intent, int startId) {

        Log.e(TAG, "onStart()");
        // do nothing to avoid multi-start
    }

    @Override
    public void onDestroy() {

        Log.e("Download", "ondestroy");

        getTrafficStats(); // last count

        if (timerTask != null) {
            timerTask.cancel();
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    private String operatorName;
    private long curTx, curRx, preTx, preRx;
    private NetworkInfo networkInfo;

    private void getTrafficStats() {

        curTx = TrafficStats.getUidTxBytes(mUid);
        curRx = TrafficStats.getUidRxBytes(mUid);

        networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            if (networkInfo.getTypeName().equalsIgnoreCase("MOBILE")) {

                if (curTx != -1) {
                    downloadManagerStorage.addLong(DownloadManagerStorage.KEY_TX_MOBILE, curTx - preTx);
                    preTx = curTx;
                }
                if (curRx != -1) {
                    downloadManagerStorage.addLong(DownloadManagerStorage.KEY_RX_MOBILE, curRx - preRx);
                    preRx = curRx;
                }

            } else {

                if (curTx != -1) {
                    downloadManagerStorage.addLong(DownloadManagerStorage.KEY_TX_WIFI, curTx - preTx);
                    preTx = curTx;
                }
                if (curRx != -1) {
                    downloadManagerStorage.addLong(DownloadManagerStorage.KEY_RX_WIFI, curRx - preRx);
                    preRx = curRx;
                }
            }
        }

    }

}
