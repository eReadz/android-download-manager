package com.yyxu.download.activities;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.yyxu.download.R;
import com.yyxu.download.utils.DownloadManagerStorage;
import com.yyxu.download.utils.StorageUtils;

public class TrafficStatActivity extends Activity {

	private TextView netText;
	private TextView appWifiText;
	private TextView appGprsText;
	private TextView testText;
	private Button clearButton;
    private DownloadManagerStorage downloadManagerStorage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.traffic_stat_activity);

		netText = (TextView) findViewById(R.id.net_text);
		appWifiText = (TextView) findViewById(R.id.app_wifi_text);
		appGprsText = (TextView) findViewById(R.id.app_gprs_text);
		testText = (TextView) findViewById(R.id.test_text);
		clearButton = (Button) findViewById(R.id.btn_clear);

		clearButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				clearTrafficStats();
			}
		});

        downloadManagerStorage = new DownloadManagerStorage(this);
		getTrafficStats();
	}

	private void clearTrafficStats() {
		downloadManagerStorage.setLong(DownloadManagerStorage.KEY_RX_MOBILE, 0L);
		downloadManagerStorage.setLong(DownloadManagerStorage.KEY_RX_WIFI, 0L);
		downloadManagerStorage.setLong(DownloadManagerStorage.KEY_TX_MOBILE, 0L);
		downloadManagerStorage.setLong(DownloadManagerStorage.KEY_TX_WIFI, 0L);
		downloadManagerStorage.setString(DownloadManagerStorage.KEY_Network_Operator_Name, "");

		getTrafficStats();
	}

	private void getTrafficStats() {

		ConnectivityManager conn = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (conn.getActiveNetworkInfo() == null) {
			netText.setText("当前联网方式: 未连接"
					+ "\n运营商："
					+ downloadManagerStorage.getString(DownloadManagerStorage.KEY_Network_Operator_Name));
		} else {
			netText.setText("当前联网方式:"
					+ conn.getActiveNetworkInfo().getTypeName()
					+ "\n运营商："
					+ downloadManagerStorage.getString(DownloadManagerStorage.KEY_Network_Operator_Name));
		}

		long mobileRx = downloadManagerStorage.getLong(DownloadManagerStorage.KEY_RX_MOBILE);
		long mobileTx = downloadManagerStorage.getLong(DownloadManagerStorage.KEY_TX_MOBILE);
		appGprsText.setText("接收 " + StorageUtils.size(mobileRx) + " / 发送 "
				+ StorageUtils.size(mobileTx));

		long wifiRx = downloadManagerStorage.getLong(DownloadManagerStorage.KEY_RX_WIFI);
		long wifiTx = downloadManagerStorage.getLong(DownloadManagerStorage.KEY_TX_WIFI);
		appWifiText.setText("接收 " + StorageUtils.size(wifiRx) + " / 发送 "
				+ StorageUtils.size(wifiTx));

		try {
			PackageManager packageManager = this.getPackageManager();
			ApplicationInfo info = packageManager.getApplicationInfo(
					this.getPackageName(), PackageManager.GET_META_DATA);
			testText.setText("Totoal: " + TrafficStats.getUidRxBytes(info.uid)
					+ " / " + "Totoal: " + TrafficStats.getUidTxBytes(info.uid));
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
}
