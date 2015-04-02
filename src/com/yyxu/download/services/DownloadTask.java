
package com.yyxu.download.services;

import com.yyxu.download.R;
import com.yyxu.download.error.FileAlreadyExistException;
import com.yyxu.download.error.NoMemoryException;
import com.yyxu.download.http.AndroidHttpClient;
import com.yyxu.download.model.DownloadInfo;
import com.yyxu.download.utils.NetworkUtils;
import com.yyxu.download.utils.StorageUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import android.accounts.NetworkErrorException;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DownloadTask extends AsyncTask<Void, Integer, Long> {

    public final static int TIME_OUT = 30000;
    private final static int BUFFER_SIZE = 1024 * 8;

    private static final String TAG = "DownloadTask";
    private static final boolean DEBUG = true;
    private static final String TEMP_SUFFIX = ".download";

    byte[] salt = { (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
            (byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03 };

    private DownloadInfo downloadInfo;
    private URL URL;
    private File file;
    private File tempFile;
    private RandomAccessFile outputStream;
    private DownloadTaskListener listener;
    private Context context;

    private long downloadSize;
    private long previousFileSize;
    private long totalSize;
    private long downloadPercent;
    private long networkSpeed;
    private long previousTime;
    private long totalTime;
    private Throwable error = null;
    private boolean interrupt = false;

    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;

    private final class ProgressReportingRandomAccessFile extends RandomAccessFile {
        private int progress = 0;

        public ProgressReportingRandomAccessFile(File file, String mode)
                throws FileNotFoundException {
            super(file, mode);
        }

        @Override
        public void write(byte[] buffer, int offset, int count) throws IOException {
            super.write(buffer, offset, count);
            progress += count;
            publishProgress(progress);
        }
    }

    public DownloadTask(Context context, DownloadInfo downloadInfo) throws MalformedURLException {
        this(context, downloadInfo, null);
    }

    public DownloadTask(Context context, DownloadInfo downloadInfo, DownloadTaskListener listener) throws MalformedURLException {
        super();
        this.context = context;
        this.downloadInfo = downloadInfo;
        this.URL = new URL(downloadInfo.getUrl());
        this.listener = listener;
        this.file = new File(downloadInfo.getPath());
        this.tempFile = new File(file.getPath() + TEMP_SUFFIX);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context);
    }

    public long getDownloadId() {
        return downloadInfo.getDownloadId();
    }

    public String getUrl() {
        return downloadInfo.getUrl();
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public boolean isInterrupt() {
        return interrupt;
    }

    public long getDownloadPercent() {
        return downloadPercent;
    }

    public long getDownloadSize() {
        return downloadSize + previousFileSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getDownloadSpeed() {
        return this.networkSpeed;
    }

    public long getTotalTime() {
        return this.totalTime;
    }

    public DownloadTaskListener getListener() {
        return this.listener;
    }

    @Override
    protected void onPreExecute() {
        previousTime = System.currentTimeMillis();
        notificationBuilder.setContentTitle("Downloading")
                .setSmallIcon(R.drawable.ic_action_download)
                .setContentText("Title of book").setProgress(100,0,true);
        notificationManager.notify((int)getDownloadId(), notificationBuilder.build());
//                .setSmallIcon(R.drawable.ic_notification);
        if (listener != null) {
            listener.preDownload(this);
        }
    }

    @Override
    protected Long doInBackground(Void... params) {

        long result = -1;
        try {
            result = download();
        } catch (NetworkErrorException | IOException | FileAlreadyExistException
                | NoMemoryException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException | NoSuchProviderException
                | NullPointerException e) {
            error = e;
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (client != null) {
                client.close();
            }
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        Log.d(TAG, "onProgressUpdate");
        if (progress.length > 1) {
            totalSize = progress[1];
            if (totalSize == -1) {
                if (listener != null)
                    listener.errorDownload(this, error);
            } else {

            }
        } else {
            totalTime = System.currentTimeMillis() - previousTime;
            downloadSize = progress[0];
            downloadPercent = (downloadSize + previousFileSize) * 100 / totalSize;
            notificationBuilder.setProgress(100, (int)downloadPercent, false);
            notificationManager.notify((int)getDownloadId(), notificationBuilder.build());
            networkSpeed = downloadSize / totalTime;
            Log.d(TAG, "Download Speed: " + networkSpeed);
            Log.d(TAG, "Download Progess: " + downloadPercent);
            if (listener != null)
                listener.updateProcess(this);
        }
    }

    @Override
    protected void onPostExecute(Long result) {
        if (interrupt) {
            notificationManager.cancel((int)getDownloadId());
            return;
        } else if (result == -1 || error != null) {
            if (DEBUG && error != null) {
                Log.v(TAG, "Download failed." + error.getMessage());
            }
            notificationBuilder.setProgress(0,0,false).setContentText("Download Failed");
            notificationManager.notify((int)getDownloadId(), notificationBuilder.build());
            if (listener != null) {
                listener.errorDownload(this, error);
            }
            return;
        } else {
            // finish download
            notificationBuilder.setProgress(0,0,false).setContentText("Download Complete");
            notificationManager.notify((int)getDownloadId(), notificationBuilder.build());
            tempFile.renameTo(file);
        }

        if (listener != null) {
            listener.finishDownload(this);
        }
    }

    public void cancelDownload() {
        interrupt = true;
    }

    private AndroidHttpClient client;
    private HttpGet httpGet;
    private HttpResponse response;

    private long download() throws NetworkErrorException, IOException, FileAlreadyExistException,
            NoMemoryException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, NullPointerException {

        if (DEBUG) {
            Log.v(TAG, "totalSize: " + totalSize);
        }

        /*
         * check net work
         */
        if (!NetworkUtils.isNetworkAvailable(context)) {
            throw new NetworkErrorException("Network blocked.");
        }

        /*
         * check file length
         */
        client = AndroidHttpClient.newInstance("DownloadTask");
        httpGet = new HttpGet(downloadInfo.getUrl());
        if (!TextUtils.isEmpty(downloadInfo.getFacebookAuthToken())) {
            httpGet.setHeader("FACEBOOK-AUTH-TOKEN", downloadInfo.getFacebookAuthToken());
        } else if (!TextUtils.isEmpty(downloadInfo.getCredentials())) {
            httpGet.setHeader("EREADZ-AUTHENTICATION", downloadInfo.getCredentials());
        }
        response = client.execute(httpGet);
        totalSize = response.getEntity().getContentLength();

        if (file.exists() && totalSize == file.length()) {
            if (DEBUG) {
                Log.v(null, "Output file already exists. Skipping download.");
            }

            throw new FileAlreadyExistException("Output file already exists. Skipping download.");
        } else if (tempFile.exists()) {
            httpGet.addHeader("Range", "bytes=" + tempFile.length() + "-");
            previousFileSize = tempFile.length();

            client.close();
            client = AndroidHttpClient.newInstance("DownloadTask");
            response = client.execute(httpGet);

            if (DEBUG) {
                Log.v(TAG, "File is not complete, download now.");
                Log.v(TAG, "File length:" + tempFile.length() + " totalSize:" + totalSize);
            }
        }

        /*
         * check memory
         */
        long storage = StorageUtils.getAvailableStorage();
        if (DEBUG) {
            Log.i(null, "storage:" + storage + " totalSize:" + totalSize);
        }

        if (totalSize - tempFile.length() > storage) {
            throw new NoMemoryException("SD card no memory.");
        }

        /*
         * start download
         */
        outputStream = new ProgressReportingRandomAccessFile(tempFile, "rw");

        publishProgress(0, (int) totalSize);

        InputStream input = response.getEntity().getContent();
        int bytesCopied = copy(input, outputStream);

        if ((previousFileSize + bytesCopied) != totalSize && totalSize != -1 && !interrupt) {
            throw new IOException("Download incomplete: " + bytesCopied + " != " + totalSize);
        }

        if (DEBUG) {
            Log.v(TAG, "Download completed successfully.");
        }

        return bytesCopied;

    }

    public int copy(InputStream input, RandomAccessFile randomAccessFileOutput) throws IOException,
            NetworkErrorException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, NullPointerException {

        if (input == null || randomAccessFileOutput == null) {
            return -1;
        }

        byte[] buffer = new byte[BUFFER_SIZE];


        byte[] key = generateKey(downloadInfo.getPublicationId(), downloadInfo.getAccountName());
        StringBuilder sb = new StringBuilder();
        for (byte b : key) {
            sb.append(String.format("%02X ", b));
        }
//        Log.d(TAG, "key: " + sb.toString());

        Cipher c = Cipher.getInstance("AES/CTR/NoPadding", "BC");
        SecretKeySpec k = new SecretKeySpec(key, "AES");
        byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        c.init(Cipher.ENCRYPT_MODE, k, ivspec);

        BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
        if (DEBUG) {
            Log.v(TAG, "length" + randomAccessFileOutput.length());
        }

        int count = 0, bytesRead = 0;
        long errorBlockTimePreviousTime = -1, expireTime = 0;

        try {

            randomAccessFileOutput.seek(randomAccessFileOutput.length());
            byte[] output;
            while (!interrupt) {
                bytesRead = in.read(buffer, 0, BUFFER_SIZE);
                if (bytesRead == -1) {
                    break;
                }
                output = c.update(buffer, 0, bytesRead);
                randomAccessFileOutput.write(output);
                count += bytesRead;

                /*
                 * check network
                 */
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    throw new NetworkErrorException("Network blocked.");
                }

                if (networkSpeed == 0) {
                    if (errorBlockTimePreviousTime > 0) {
                        expireTime = System.currentTimeMillis() - errorBlockTimePreviousTime;
                        if (expireTime > TIME_OUT) {
                            throw new ConnectTimeoutException("connection time out.");
                        }
                    } else {
                        errorBlockTimePreviousTime = System.currentTimeMillis();
                    }
                } else {
                    expireTime = 0;
                    errorBlockTimePreviousTime = -1;
                }
            }
            Log.d(TAG, "Stopping Download: Should happen before crash hopefully");
            //NOTE: Only finalize the file if the download was not interrupted and exited because it reached the end of the input stream
            if (!interrupt) {
                output = c.doFinal();
                randomAccessFileOutput.seek(randomAccessFileOutput.length());
                randomAccessFileOutput.write(output);
            }
        } finally {
            client.close(); // must close client first
            client = null;
            randomAccessFileOutput.close();
            in.close();
            input.close();
        }
        return count;

    }

    public String getUniqueId(String username) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)  + "-" + username.toLowerCase();
    }

    public byte[] generateKey(String uuid, String username) {
        StringBuilder p = new StringBuilder();
        p.append("doubleeye").append(getUniqueId(username)).append(uuid);

        PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(p.toString().toCharArray()), salt,1024);
        KeyParameter params = (KeyParameter)generator.generateDerivedParameters(128);

        return params.getKey();
    }

}
