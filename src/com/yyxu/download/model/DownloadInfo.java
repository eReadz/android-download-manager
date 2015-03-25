package com.yyxu.download.model;

import java.io.Serializable;
import java.util.UUID;

public class DownloadInfo implements Serializable {
    private String credentials;
    private long downloadId;
    private String url;
    private String path;
    private String accountName;
    private String publicationId;
    private String facebookAuthToken;

    public DownloadInfo(String credentials, long downloadId, String url, String path, String accountName, String publicationId, String facebookAuthToken) {
        this.credentials = credentials;
        this.downloadId = downloadId;
        this.url = url;
        this.path = path;
        this.accountName = accountName;
        this.publicationId = publicationId;
        this.facebookAuthToken = facebookAuthToken;
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

    public String getAccountName() {
        return accountName;
    }

    public String getPublicationId() {
        return publicationId;
    }

    public String getFacebookAuthToken() {
        return facebookAuthToken;
    }
}
