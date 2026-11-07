package com.liveselling.gcashbridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkSyncManager {
    private static final String TAG = "NetworkSyncManager";
    public static final String PREF_SERVER_URL = "server_url";
    public static final String PREF_BRIDGE_KEY = "bridge_key";

    public static final String DEFAULT_SERVER_URL = "https://livesellingadmin.com/api/payments/gcash/inbound-notification";
    public static final String DEFAULT_BRIDGE_KEY = "liveselling_gcash_bridge_secret_2026";

    private static NetworkSyncManager instance;
    private final Context context;
    private final DatabaseHelper db;
    private final OkHttpClient client;
    private final ExecutorService executor;

    public static synchronized NetworkSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkSyncManager(context.getApplicationContext());
        }
        return instance;
    }

    private NetworkSyncManager(Context context) {
        this.context = context;
        this.db = DatabaseHelper.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public String getServerUrl() {
        SharedPreferences prefs = context.getSharedPreferences("bridge_settings", Context.MODE_PRIVATE);
        return prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL);
    }

    public void setServerUrl(String url) {
        SharedPreferences prefs = context.getSharedPreferences("bridge_settings", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_SERVER_URL, url).apply();
    }

    public String getBridgeKey() {
        SharedPreferences prefs = context.getSharedPreferences("bridge_settings", Context.MODE_PRIVATE);
        return prefs.getString(PREF_BRIDGE_KEY, DEFAULT_BRIDGE_KEY);
    }

    public void setBridgeKey(String key) {
        SharedPreferences prefs = context.getSharedPreferences("bridge_settings", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_BRIDGE_KEY, key).apply();
    }

    public void triggerSync() {
        executor.execute(this::syncPendingInternal);
    }

    private void syncPendingInternal() {
        List<TransferRecord> pending = db.getPendingTransfers();
        if (pending.isEmpty()) return;

        String serverUrl = getServerUrl();
        String bridgeKey = getBridgeKey();

        for (TransferRecord tr : pending) {
            try {
                JSONObject json = new JSONObject();
                json.put("amount", tr.getAmount());
                json.put("reference_number", tr.getRefNo());
                json.put("sender_name", tr.getSenderName());
                json.put("raw_notification", tr.getRawText());
                json.put("timestamp", tr.getTimestamp());

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request req = new Request.Builder()
                        .url(serverUrl)
                        .addHeader("X-Bridge-Key", bridgeKey)
                        .post(body)
                        .build();

                try (Response res = client.newCall(req).execute()) {
                    if (res.isSuccessful()) {
                        Log.i(TAG, "Transfer " + tr.getRefNo() + " synced successfully (HTTP " + res.code() + ")");
                        db.updateStatus(tr.getRefNo(), "SYNCED");
                    } else {
                        Log.w(TAG, "Sync failed for " + tr.getRefNo() + ": HTTP " + res.code());
                        db.updateStatus(tr.getRefNo(), "FAILED");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Network error syncing " + tr.getRefNo() + ": " + e.getMessage());
                db.updateStatus(tr.getRefNo(), "QUEUED");
            }
        }
    }
}
