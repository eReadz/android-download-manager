
package com.yyxu.download.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.yyxu.download.model.DownloadInfo;
import com.yyxu.download.utils.DownloadManagerIntent;
import com.yyxu.download.utils.DownloadManagerStorage;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class DownloadManagerWithQueues extends Thread {
    private static final String TAG = "DownloadManager";
    private static final int MAX_TASK_COUNT = 100;
    private static final int MAX_DOWNLOAD_THREAD_COUNT = 3;

    private Context mContext;

    private TaskQueue mTaskQueue;
    private List<DownloadTask> mDownloadingTasks;
    private List<DownloadTask> mPausingTasks;

    private Boolean isRunning = false;

    private DownloadManagerStorage mDownloadManagerStorage;

    public DownloadManagerWithQueues(Context context) {
        mContext = context;
        mTaskQueue = new TaskQueue();
        mDownloadingTasks = new ArrayList<DownloadTask>();
        mPausingTasks = new ArrayList<DownloadTask>();
        mDownloadManagerStorage = new DownloadManagerStorage(context);
    }

    public void startManage() {
        isRunning = true;
        this.start();
        checkUncompleteTasks();
    }

    public void close() {
        isRunning = false;
        pauseAllTask();
        this.stop();
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void run() {
        super.run();
        while (isRunning) {
            DownloadTask task = mTaskQueue.poll();
            if (task != null) {
                Log.d(TAG, "Added task " + task.getDownloadId() + " to Download Queue");
                mDownloadingTasks.add(task);
                task.execute();
            } else {
                Log.d(TAG, "Download Task Queue Empty");
            }
        }
    }

    public boolean createAndAddDownloadTask(DownloadInfo downloadInfo) {
        if (getTotalTaskCount() >= MAX_TASK_COUNT) {
            Toast.makeText(mContext, "Exceeded Max Task Count", Toast.LENGTH_LONG).show();
            return false;
        }

        try {
            DownloadTask newTask = createDownloadTask(downloadInfo);
            addTaskToQueue(newTask);
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private DownloadTask createDownloadTask(DownloadInfo downloadInfo) throws MalformedURLException {
        DownloadTaskListener taskListener = new DownloadTaskListener() {

            @Override
            public void updateProcess(DownloadTask task) {

                Intent updateIntent = new Intent(DownloadManagerIntent.Action.PROGRESS_UPDATED);
                updateIntent.putExtra(DownloadManagerIntent.PROCESS_SPEED, task.getDownloadSpeed() + "kbps | "
                        + task.getDownloadSize() + " / " + task.getTotalSize());
                updateIntent.putExtra(DownloadManagerIntent.PROCESS_PROGRESS, task.getDownloadPercent() + "");
                updateIntent.setData(Uri.parse(task.getUrl()));
                mContext.sendBroadcast(updateIntent);
            }

            @Override
            public void preDownload(DownloadTask task) {
                mDownloadManagerStorage.storeDownloadTask(task.getDownloadId(), task.getDownloadInfo());
            }

            @Override
            public void finishDownload(DownloadTask task) {
                completeTask(task);
            }

            @Override
            public void errorDownload(DownloadTask task, Throwable error) {
                //TODO: Delete partial file download
                if (error != null) {
                    Toast.makeText(mContext, "Error: " + error.getMessage(), Toast.LENGTH_LONG)
                            .show();
                }

                // Intent errorIntent = new
                // Intent("com.yyxu.download.activities.DownloadListActivity");
                // errorIntent.putExtra(MyIntents.TYPE, MyIntents.Types.ERROR);
                // errorIntent.putExtra(MyIntents.ERROR_CODE, error);
                // errorIntent.putExtra(MyIntents.ERROR_INFO,
                // DownloadTask.getErrorInfo(error));
                // errorIntent.putExtra(MyIntents.URL, task.getUrl());
                // mContext.sendBroadcast(errorIntent);
                //
                // if (error != DownloadTask.ERROR_UNKOWN_HOST
                // && error != DownloadTask.ERROR_BLOCK_INTERNET
                // && error != DownloadTask.ERROR_TIME_OUT) {
                // completeTask(task);
                // }
            }
        };
        return new DownloadTask(mContext, downloadInfo, taskListener);
    }

    private void addTaskToQueue(DownloadTask task) {

        broadcastAddTask(task.getUrl());

        mTaskQueue.offer(task);

        Log.d(TAG, "Added download: " + task.getDownloadId() + " to task queue");
        if (!this.isAlive()) {
            this.startManage();
        }
    }

    private void broadcastAddTask(String url) {
        broadcastAddTask(url, false);
    }

    private void broadcastAddTask(String url, boolean isInterrupt) {
        Log.d(TAG, "broadcastAddTask: " + url);
        Intent nofityIntent = new Intent(DownloadManagerIntent.Action.ADD_COMPLETED);
//        nofityIntent.setData(Uri.parse(url));
        nofityIntent.putExtra(DownloadManagerIntent.IS_PAUSED, isInterrupt);
        mContext.sendBroadcast(nofityIntent);
    }

    public void rebroadcastAddAllTask() {

        DownloadTask task;
        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            broadcastAddTask(task.getUrl(), task.isInterrupt());
        }
        for (int i = 0; i < mTaskQueue.size(); i++) {
            task = mTaskQueue.get(i);
            broadcastAddTask(task.getUrl());
        }
        for (int i = 0; i < mPausingTasks.size(); i++) {
            task = mPausingTasks.get(i);
            broadcastAddTask(task.getUrl());
        }
    }

    public boolean hasTask(String url) {

        DownloadTask task;
        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            if (task.getUrl().equals(url)) {
                return true;
            }
        }
        for (int i = 0; i < mTaskQueue.size(); i++) {
            task = mTaskQueue.get(i);
        }
        return false;
    }

    public boolean hasTask(long downloadId) {
        DownloadTask task;
        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            if (task.getDownloadId() == downloadId) {
                return true;
            }
        }
        for (int i = 0; i < mTaskQueue.size(); i++) {
            task = mTaskQueue.get(i);
            if (task.getDownloadId() == downloadId) {
                return true;
            }
        }
        return false;
    }

    public int getQueueTaskCount() {

        return mTaskQueue.size();
    }

    public int getDownloadingTaskCount() {

        return mDownloadingTasks.size();
    }

    public int getPausingTaskCount() {

        return mPausingTasks.size();
    }

    public int getTotalTaskCount() {
        return getQueueTaskCount() + getDownloadingTaskCount() + getPausingTaskCount();
    }

    public void checkUncompleteTasks() {
        HashMap<Long, DownloadInfo> downloadInformation = mDownloadManagerStorage.getDownloadInformationList();
        for (Long downloadId : downloadInformation.keySet()) {
            createAndAddDownloadTask(downloadInformation.get(downloadId));
        }
    }

    public synchronized void pauseAllTask() {

        DownloadTask task;

        for (int i = 0; i < mTaskQueue.size(); i++) {
            task = mTaskQueue.get(i);
            mTaskQueue.remove(task);
            mPausingTasks.add(task);
        }

        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            if (task != null) {
                pauseTask(task);
            }
        }
    }

    public synchronized void deleteTask(long downloadId) {
        //Delete the task from all the queues/lists
        for (DownloadTask downloadTask : mDownloadingTasks) {
            if (downloadTask.getDownloadId() == downloadId) {
                downloadTask.cancelDownload();
                File file = new File(downloadTask.getDownloadInfo().getPath());
                if (file.exists()) {
                    file.delete();
                }
                completeTask(downloadTask);
                return;
            }
        }

        for (DownloadTask downloadTask : mTaskQueue.getQueue()) {
            if (downloadTask.getDownloadId() == downloadId) {
                mTaskQueue.remove(downloadTask);
            }
        }

        for (DownloadTask downloadTask : mPausingTasks) {
            if (downloadTask.getDownloadId() == downloadId) {
                mPausingTasks.remove(downloadTask);
            }
        }
    }

    public synchronized void pauseTask(long downloadId) {
        for (DownloadTask downloadTaskToBePaused : mDownloadingTasks) {
            if (downloadTaskToBePaused.getDownloadId() == downloadId) {
                pauseTask(downloadTaskToBePaused);
            }
        }
    }

    public synchronized void pauseTask(DownloadTask downloadTaskToBePaused) {
        if (mDownloadingTasks.contains(downloadTaskToBePaused)) {
            downloadTaskToBePaused.cancelDownload();
            // move to pausing list
            mDownloadingTasks.remove(downloadTaskToBePaused);
            mPausingTasks.add(downloadTaskToBePaused);
        }
    }

    public synchronized void continueTask(long downloadId) {
        for (DownloadTask task : mPausingTasks) {
            if (task.getDownloadId() == downloadId) {
                mPausingTasks.remove(task);
                mTaskQueue.offer(task);
            }
        }
    }

    public synchronized void completeTask(DownloadTask task) {

        if (mDownloadingTasks.contains(task)) {
            Log.d(TAG, "DownloadTask " + task.getDownloadId() + " completed");
            mDownloadManagerStorage.clearDownloadTask(task.getDownloadId());
            mDownloadingTasks.remove(task);

            // notify list changed
            Intent nofityIntent = new Intent(DownloadManagerIntent.Action.DOWNLOAD_COMPLETED);
//            nofityIntent.setData(Uri.parse(task.getUrl()));
            mContext.sendBroadcast(nofityIntent);
        }
    }

    /**
     * A obstructed task queue
     * 
     * @author Yingyi Xu
     */
    private class TaskQueue {
        private Queue<DownloadTask> taskQueue;

        public TaskQueue() {

            taskQueue = new LinkedList<DownloadTask>();
        }

        public void offer(DownloadTask task) {

            taskQueue.offer(task);
        }

        public DownloadTask poll() {
            DownloadTask task;
            while (mDownloadingTasks.size() >= MAX_DOWNLOAD_THREAD_COUNT
                    || (task = taskQueue.poll()) == null) {
                try {
                    Thread.sleep(1000); // sleep
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return task;
        }

        public DownloadTask get(int position) {

            if (position >= size()) {
                return null;
            }
            return ((LinkedList<DownloadTask>) taskQueue).get(position);
        }

        public int size() {
            return taskQueue.size();
        }

        public boolean remove(DownloadTask task) {
            return taskQueue.remove(task);
        }

        public Queue<DownloadTask> getQueue() {
            return taskQueue;
        }
    }

}
