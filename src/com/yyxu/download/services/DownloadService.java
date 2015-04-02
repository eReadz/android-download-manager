
package com.yyxu.download.services;

import com.yyxu.download.model.DownloadInfo;
import com.yyxu.download.utils.DownloadManagerIntent;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class DownloadService extends Service {
    public static final String TAG = "DownloadService";

    private final IBinder downloadServiceBinder = new DownloadServiceBinder();

    private List<DownloadTask> downloadingTasks;

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
        downloadingTasks = new ArrayList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            Log.d(TAG, "onStartCommand(): " + intent.getAction());
            String url;
            if (DownloadManagerIntent.Action.ADD.equalsIgnoreCase(intent.getAction())) {
                //TODO: Enable adding download tasks via intents
//                url = intent.getData().toString();
//                if (!TextUtils.isEmpty(url) && !mDownloadManager.hasTask(url)) {
                    // A negative downloadId will result in an new one being created
//                    addTask(url);
//                }
            } else if (DownloadManagerIntent.Action.CONTINUE.equalsIgnoreCase(intent.getAction())) {
                //TODO: Enable continue download tasks via intents
//                url = intent.getData().toString();
//                if (!TextUtils.isEmpty(url)) {
//                    mDownloadManager.continueTask(url);
//                }
            } else if (DownloadManagerIntent.Action.DELETE.equalsIgnoreCase(intent.getAction())) {
                //TODO: Enable delete download tasks via intents
//                url = intent.getData().toString();
//                if (!TextUtils.isEmpty(url)) {
//                    mDownloadManager.deleteTask(url);
//                }
            } else if (DownloadManagerIntent.Action.PAUSE.equalsIgnoreCase(intent.getAction())) {
                //TODO: Enable pause download tasks via intents
//                url = intent.getData().toString();
//                if (!TextUtils.isEmpty(url)) {
//                    mDownloadManager.pauseTask(url);
//                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    public boolean hasTask(long downloadId) {
        for (DownloadTask downloadTask : downloadingTasks) {
            if (downloadTask.getDownloadId() == downloadId) {
                return true;
            }
        }
        return false;
    }

    public boolean addTask(DownloadInfo downloadInfo) {
        try {
            DownloadTask newDownloadTask = createDownloadTask(downloadInfo);
            downloadingTasks.add(newDownloadTask);
            newDownloadTask.execute();
            Intent nofityIntent = new Intent(DownloadManagerIntent.Action.ADD_COMPLETED);
            sendBroadcast(nofityIntent);
            return true;
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    private DownloadTask createDownloadTask(DownloadInfo downloadInfo) throws MalformedURLException {
        DownloadTaskListener taskListener = new DownloadTaskListener() {
            @Override
            public void preDownload(DownloadTask task) {

            }

            @Override
            public void updateProcess(DownloadTask task) {
                Intent updateIntent = new Intent(DownloadManagerIntent.Action.PROGRESS_UPDATED);
                updateIntent.putExtra(DownloadManagerIntent.PROCESS_SPEED, task.getDownloadSpeed() + "kbps | "
                        + task.getDownloadSize() + " / " + task.getTotalSize());
                updateIntent.putExtra(DownloadManagerIntent.PROCESS_PROGRESS, task.getDownloadPercent() + "");
                updateIntent.setData(Uri.parse(task.getUrl()));
                sendBroadcast(updateIntent);
            }

            @Override
            public void finishDownload(DownloadTask task) {
                completeTask(task);
            }

            @Override
            public void errorDownload(DownloadTask task, Throwable error) {
                if (error != null) {
                    Toast.makeText(DownloadService.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG)
                            .show();
                }
            }
        };
        return new DownloadTask(this, downloadInfo, taskListener);
    }

    public void completeTask(DownloadTask task) {
        if (downloadingTasks.contains(task)) {
            Log.d(TAG, "DownloadTask " + task.getDownloadId() + " completed");
            downloadingTasks.remove(task);

            // notify list changed
            Intent nofityIntent = new Intent(DownloadManagerIntent.Action.DOWNLOAD_COMPLETED);
            sendBroadcast(nofityIntent);
        }
    }

    public void cancelTask(long downloadId) {
        for (DownloadTask downloadTask : downloadingTasks) {
            if (downloadTask.getDownloadId() == downloadId) {
                Log.d(TAG, "DownloadTask " + downloadTask.getDownloadId() + " cancelled");

                downloadTask.cancelDownload();

                //TODO: Delete partial download ending in .download
                File file = new File(downloadTask.getDownloadInfo().getPath());
                if (file.exists()) {
                    file.delete();
                }
                completeTask(downloadTask);
                break;
            }
        }
    }

}
