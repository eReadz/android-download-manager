
package com.yyxu.download.services;

import com.yyxu.download.utils.DownloadManagerIntent;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class DownloadService extends Service {
    public static final String TAG = "DownloadService";

    private DownloadManager mDownloadManager;

    private final IBinder downloadServiceBinder = new DownloadServiceBinder();

    public class DownloadServiceBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return downloadServiceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        mDownloadManager = new DownloadManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            Log.d(TAG, "onStartCommand(): " + intent.getAction());
            String url;
            if (DownloadManagerIntent.Action.START.equalsIgnoreCase(intent.getAction())) {
                if (!mDownloadManager.isRunning()) {
                    mDownloadManager.startManage();
                } else {
                    mDownloadManager.rebroadcastAddAllTask();
                }
            } else if (DownloadManagerIntent.Action.ADD.equalsIgnoreCase(intent.getAction())) {
                url = intent.getData().toString();
                if (!TextUtils.isEmpty(url) && !mDownloadManager.hasTask(url)) {
                    // A negative downloadId will result in an new one being created
                    addTask(url);
                }
            } else if (DownloadManagerIntent.Action.CONTINUE.equalsIgnoreCase(intent.getAction())) {
                url = intent.getData().toString();
                if (!TextUtils.isEmpty(url)) {
                    mDownloadManager.continueTask(url);
                }
            } else if (DownloadManagerIntent.Action.DELETE.equalsIgnoreCase(intent.getAction())) {
                url = intent.getData().toString();
                if (!TextUtils.isEmpty(url)) {
                    mDownloadManager.deleteTask(url);
                }
            } else if (DownloadManagerIntent.Action.PAUSE.equalsIgnoreCase(intent.getAction())) {
                url = intent.getData().toString();
                if (!TextUtils.isEmpty(url)) {
                    mDownloadManager.pauseTask(url);
                }
            } else if (DownloadManagerIntent.Action.STOP.equalsIgnoreCase(intent.getAction())) {
                mDownloadManager.close();
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    public boolean isDownloadingTask(long downloadId) {
        return mDownloadManager.isDownloadingTask(downloadId);
    }

    public long addTask(String url) {
        return mDownloadManager.addTask(-1, url);
    }
}
