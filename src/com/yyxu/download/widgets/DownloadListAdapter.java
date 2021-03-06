package com.yyxu.download.widgets;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.yyxu.download.R;
import com.yyxu.download.utils.DownloadManagerIntent;

public class DownloadListAdapter extends BaseAdapter {

	private Context mContext;
	private ArrayList<HashMap<Integer, String>> dataList;

	public DownloadListAdapter(Context context) {
		mContext = context;
		dataList = new ArrayList<HashMap<Integer, String>>();
	}

	@Override
	public int getCount() {
		return dataList.size();
	}

	@Override
	public Object getItem(int position) {
		return dataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void addItem(String url) {
		addItem(url, false);
	}

	public void addItem(String url, boolean isPaused) {
		HashMap<Integer, String> item = ViewHolder.getItemDataMap(url, null,
				null, isPaused + "");
		dataList.add(item);
		this.notifyDataSetChanged();
	}

	public void removeItem(String url) {
		String tmp;
		for (int i = 0; i < dataList.size(); i++) {
			tmp = dataList.get(i).get(ViewHolder.KEY_URL);
			if (tmp.equals(url)) {
				dataList.remove(i);
				this.notifyDataSetChanged();
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.download_list_item, null);
		}

		HashMap<Integer, String> itemData = dataList.get(position);
		String url = itemData.get(ViewHolder.KEY_URL);
		convertView.setTag(url);

		ViewHolder viewHolder = new ViewHolder(convertView);
		viewHolder.setData(itemData);

		viewHolder.continueButton.setOnClickListener(new DownloadBtnListener(
				url, viewHolder));
		viewHolder.pauseButton.setOnClickListener(new DownloadBtnListener(url,
				viewHolder));
		viewHolder.deleteButton.setOnClickListener(new DownloadBtnListener(url,
				viewHolder));

		return convertView;
	}

	private class DownloadBtnListener implements View.OnClickListener {
		private String url;
		private ViewHolder mViewHolder;

		public DownloadBtnListener(String url, ViewHolder viewHolder) {
			this.url = url;
			this.mViewHolder = viewHolder;
		}

        @Override
        public void onClick(View v) {
            Intent downloadIntent = new Intent();

            if (v.getId() == R.id.btn_continue) {
                // mDownloadManager.continueTask(mPosition);
                downloadIntent.setAction(DownloadManagerIntent.Action.CONTINUE);
                downloadIntent.setData(Uri.parse(url));
                mContext.startService(downloadIntent);

                mViewHolder.continueButton.setVisibility(View.GONE);
                mViewHolder.pauseButton.setVisibility(View.VISIBLE);
            } else if (v.getId() == R.id.btn_pause) {
                // mDownloadManager.pauseTask(mPosition);
                downloadIntent.setAction(DownloadManagerIntent.Action.PAUSE);
                downloadIntent.setData(Uri.parse(url));
                mContext.startService(downloadIntent);

                mViewHolder.continueButton.setVisibility(View.VISIBLE);
                mViewHolder.pauseButton.setVisibility(View.GONE);
            } else if (v.getId() == R.id.btn_delete) {
                // mDownloadManager.deleteTask(mPosition);
                downloadIntent.setAction(DownloadManagerIntent.Action.DELETE);
                downloadIntent.setData(Uri.parse(url));
                mContext.startService(downloadIntent);

                removeItem(url);
            }
        }
    }

	// private class DownloadBtnListener implements View.OnClickListener {
	// private int mPosition;
	// private ViewHolder mViewHolder;
	//
	// public DownloadBtnListener(int position, ViewHolder viewHolder) {
	// this.mPosition = position;
	// this.mViewHolder = viewHolder;
	// }
	//
	// @Override
	// public void onClick(View v) {
	// switch (v.getId()) {
	// case R.id.btn_continue:
	// // mDownloadManager.continueTask(mPosition);
	// mViewHolder.continueButton.setVisibility(View.GONE);
	// mViewHolder.pauseButton.setVisibility(View.VISIBLE);
	// break;
	// case R.id.btn_pause:
	// // mDownloadManager.pauseTask(mPosition);
	// mViewHolder.continueButton.setVisibility(View.VISIBLE);
	// mViewHolder.pauseButton.setVisibility(View.GONE);
	// break;
	// case R.id.btn_delete:
	// // mDownloadManager.deleteTask(mPosition);
	// DownloadListAdapter.this.notifyDataSetChanged();
	// break;
	// }
	// }
	// }
}