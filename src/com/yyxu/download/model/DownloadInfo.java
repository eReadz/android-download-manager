package com.yyxu.download.model;

import java.io.Serializable;
import java.util.UUID;

public class DownloadInfo implements Serializable {
    private String credentials;
    private long downloadId;
    private String url;
    private String path;

    public DownloadInfo(String credentials, long downloadId, String url, String path) {
        this.credentials = credentials;
        this.downloadId = downloadId;
        this.url = url;
        this.path = path;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    public String getCredentials() {
        return credentials;
    }
}
