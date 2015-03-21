package com.yyxu.download.utils;

public class DownloadManagerIntent {

	public static final String PROCESS_SPEED = "process_speed";
	public static final String PROCESS_PROGRESS = "process_progress";
	public static final String URL = "url";
	public static final String ERROR_CODE = "error_code";
	public static final String ERROR_INFO = "error_info";
	public static final String IS_PAUSED = "is_paused";
	
    public class Action {
        public static final String START = "com.ereadz.downloadmanager.ACTION_START";
        public static final String STOP = "com.ereadz.downloadmanager.ACTION_STOP";

        public static final String ADD = "com.ereadz.downloadmanager.ACTION_ADD";
        public static final String PAUSE = "com.ereadz.downloadmanager.ACTION_PAUSE";
        public static final String CONTINUE = "com.ereadz.downloadmanager.ACTION_CONTINUE";
        public static final String DELETE = "com.ereadz.downloadmanager.ACTION_DELETE";

        public static final String ADD_COMPLETED = "com.ereadz.downloadmanager.ADD_COMPLETED";
        public static final String PROGRESS_UPDATED = "com.ereadz.downloadmanager.ACTION_PROGRESS_UPDATED";
        public static final String DOWNLOAD_COMPLETED = "com.ereadz.downloadmanager.ACTION_DOWNLOAD_COMPLETED";
        public static final String ERROR = "com.ereadz.downloadmanager.ACTION_ERROR";
    }
}
