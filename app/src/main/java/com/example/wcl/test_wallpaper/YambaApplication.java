package com.example.wcl.test_wallpaper;

import android.app.Application;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClientException;

import java.util.List;

/**
 * Created by wangchenlong on 15-1-16.
 */
public class YambaApplication extends Application
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "DEUBG-WCL: " +
            YambaApplication.class.getSimpleName();

    public YambaClient mYambaClient;
    private SharedPreferences mSharedPreferences;
    private boolean mIsServiceRunning;
    private StatusData mStatusData;

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        Log.i(TAG, "onCreated");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "onTerminated");
    }

    public synchronized YambaClient getYambaClient() {
        if (mYambaClient == null) {
            String username = mSharedPreferences.getString("username", "student");
            String password = mSharedPreferences.getString("password", "password");
            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                mYambaClient = new YambaClient(username, password);
            }
        }
        return mYambaClient;
    }

    public synchronized void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
            mYambaClient = null;
    }

    public boolean isServiceRunning() {
        return mIsServiceRunning;
    }

    public void setServiceRunning(boolean isServiceRunning) {
        mIsServiceRunning = isServiceRunning;
    }

    public StatusData getStatusData() {
        if (mStatusData == null) {
            mStatusData = new StatusData(this);
        }
        return mStatusData;
    }

    public synchronized int fetchStatusUpdates() {
        Log.d(TAG, "Fetching status updates");
        YambaClient yambaClient = getYambaClient();

        if (yambaClient == null) {
            Log.d(TAG, "Yamba connection into not initialized");
            return 0;
        }

        try {
            List<YambaClient.Status> statusUpdates = yambaClient.getTimeline(20);
            long lastestStatusCreatedAtTime = getStatusData().getLastestStatusCreatedAtTime();
            int count = 0;
            ContentValues values = new ContentValues();
            for (YambaClient.Status status : statusUpdates) {
                values.put(StatusData.C_ID, status.getId());
                long createdAt = status.getCreatedAt().getTime();
                values.put(StatusData.C_CREATED_AT, createdAt);
                values.put(StatusData.C_TEXT, status.getMessage());
                values.put(StatusData.C_USER, status.getUser());

                Log.d(TAG, "Got update with id " + status.getId() + ". Saving");
                getStatusData().insertOrIgnore(values);
                if (lastestStatusCreatedAtTime < createdAt) {
                    count ++;
                }
            }

            Log.d(TAG, count > 0 ? "Got " + count + " status updates"
                    : "No new status updates");
            return count;
        } catch (YambaClientException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }
}















