package com.yyxu.download.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class DownloadManagerStorage {

    private static final String TAG = "DownloadManagerStorage";
	public final String PREFERENCE_NAME = "com.yyxu.download";

    public static final String DATA_DIRECTORY = "data";
    public static final String DOWNLOAD_TASKS_FILE = "downloads";

    private SharedPreferences preferences;
    private Context context;
    private HashMap<Long, String> downloadInformation;

    public DownloadManagerStorage(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        downloadInformation = new HashMap<>();
        downloadInformation = readDownloadTasksObjectFromFile();
    }

	public void storeDownloadTask(long downloadId, String url) {
		downloadInformation.put(downloadId, url);
        writeDownloadTasksObjectToFile(downloadInformation);
	}

	public void clearDownloadTask(long downloadId) {
		downloadInformation.remove(downloadId);
        writeDownloadTasksObjectToFile(downloadInformation);
	}

	public String getURL(long downloadId) {
		return downloadInformation.get(downloadId);
	}

    public HashMap<Long, String> getDownloadInformation() {
        return downloadInformation;
    }

    private HashMap<Long, String> readDownloadTasksObjectFromFile() {
        try {
            File downloadsFile = new File(getDataDir(), DOWNLOAD_TASKS_FILE);
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(downloadsFile));
            return (HashMap<Long, String>) objectInputStream.readObject();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private void writeDownloadTasksObjectToFile(Object objectToWrite) {
        try {
            File downloadsFile = new File(getDataDir(), DOWNLOAD_TASKS_FILE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(downloadsFile));
            objectOutputStream.writeObject(objectToWrite);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public File getDataDir() {
        File dataDirectory = new File(context.getFilesDir(), DATA_DIRECTORY);
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdirs()) {
                Log.e(TAG, "Failed to create internal data directory");
            }
        }
        return dataDirectory;
    }

    //region Shared Preferences Storage
    public static final String KEY_RX_WIFI = "rx_wifi";
	public static final String KEY_TX_WIFI = "tx_wifi";
	public static final String KEY_RX_MOBILE = "tx_mobile";
	public static final String KEY_TX_MOBILE = "tx_mobile";
	public static final String KEY_Network_Operator_Name = "operator_name";

    public String getString(String key) {
        if (preferences != null)
            return preferences.getString(key, "");
        else
            return "";
    }

    public void setString(String key, String value) {
        Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

	public int getInt(String key) {
        return preferences.getInt(key, 0);
	}

	public void setInt(String key, int value) {
        Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.commit();
	}

	public long getLong(String key) {
        return preferences.getLong(key, 0L);
	}

	public void setLong(String key, long value) {
        Editor editor = preferences.edit();
        editor.putLong(key, value);
        editor.commit();
	}

	public void addLong(String key, long value) {
		setLong(key, getLong(key) + value);
	}
    //endregion
}
