
package com.yyxu.download.services;

import com.yyxu.download.utils.DownloadManagerIntent;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class DownloadService extends Service {
    public static final String TAG = "DownloadService";

    private DownloadManager mDownloadManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        mDownloadManager = new DownloadManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

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
                mDownloadManager.addTask(url);
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
//        if (intent.getAction().equals("com.yyxu.download.services.IDownloadService")) {
//            int type = intent.getIntExtra(DownloadManagerIntent.TYPE, -1);
//            String url;
//
//            switch (type) {
//                case DownloadManagerIntent.Types.START:
//                    if (!mDownloadManager.isRunning()) {
//                        mDownloadManager.startManage();
//                    } else {
//                        mDownloadManager.rebroadcastAddAllTask();
//                    }
//                    break;
//                case DownloadManagerIntent.Types.ADD:
//                    url = intent.getStringExtra(DownloadManagerIntent.URL);
//                    if (!TextUtils.isEmpty(url) && !mDownloadManager.hasTask(url)) {
//                        mDownloadManager.addTask(url);
//                    }
//                    break;
//                case DownloadManagerIntent.Types.CONTINUE:
//                    url = intent.getStringExtra(DownloadManagerIntent.URL);
//                    if (!TextUtils.isEmpty(url)) {
//                        mDownloadManager.continueTask(url);
//                    }
//                    break;
//                case DownloadManagerIntent.Types.DELETE:
//                    url = intent.getStringExtra(DownloadManagerIntent.URL);
//                    if (!TextUtils.isEmpty(url)) {
//                        mDownloadManager.deleteTask(url);
//                    }
//                    break;
//                case DownloadManagerIntent.Types.PAUSE:
//                    url = intent.getStringExtra(DownloadManagerIntent.URL);
//                    if (!TextUtils.isEmpty(url)) {
//                        mDownloadManager.pauseTask(url);
//                    }
//                    break;
//                case DownloadManagerIntent.Types.STOP:
//                    mDownloadManager.close();
//                    stopSelf();
//                    break;
//
//                default:
//                    break;
//            }
//        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
