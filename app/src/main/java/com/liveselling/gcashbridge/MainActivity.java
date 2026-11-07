package com.liveselling.gcashbridge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatusPill, tvTodayTotal, tvPendingCount, tvTransferCount, tvEmptyState, tvServerSub;
    private View bannerPermission;
    private Button btnGrantPermission, btnTestTransfer;
    private RecyclerView rvTransfers;
    private SwipeRefreshLayout swipeRefresh;

    private DatabaseHelper db;
    private TransferAdapter adapter;
    private final List<TransferRecord> transferList = new ArrayList<>();

    private final BroadcastReceiver transferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = DatabaseHelper.getInstance(this);

        initViews();
        setupRecyclerView();
        checkPermissions();
        refreshData();

        // Register local broadcast receiver for incoming transfers
        IntentFilter filter = new IntentFilter(GCashNotificationService.ACTION_TRANSFER_RECEIVED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(transferReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(transferReceiver, filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        refreshData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(transferReceiver);
        } catch (Exception ignored) {}
    }

    private void initViews() {
        tvStatusPill = findViewById(R.id.tvStatusPill);
        tvTodayTotal = findViewById(R.id.tvTodayTotal);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvTransferCount = findViewById(R.id.tvTransferCount);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvServerSub = findViewById(R.id.tvServerSub);

        bannerPermission = findViewById(R.id.bannerPermission);
        btnGrantPermission = findViewById(R.id.btnGrantPermission);
        btnTestTransfer = findViewById(R.id.btnTestTransfer);
        rvTransfers = findViewById(R.id.rvTransfers);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        btnGrantPermission.setOnClickListener(v -> openNotificationAccessSettings());
        btnTestTransfer.setOnClickListener(v -> sendTestTransfer());

        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettingsDialog());

        swipeRefresh.setOnRefreshListener(() -> {
            NetworkSyncManager.getInstance(this).triggerSync();
            refreshData();
            swipeRefresh.setRefreshing(false);
        });

        String url = NetworkSyncManager.getInstance(this).getServerUrl();
        tvServerSub.setText("Server: " + url.replace("https://", "").replace("http://", "").split("/")[0]);
    }

    private void setupRecyclerView() {
        rvTransfers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransferAdapter(transferList);
        rvTransfers.setAdapter(adapter);
    }

    private void checkPermissions() {
        boolean hasAccess = isNotificationServiceEnabled();
        if (hasAccess) {
            bannerPermission.setVisibility(View.GONE);
            tvStatusPill.setText("🟢 Active & Listening");
            tvStatusPill.setBackgroundResource(R.drawable.badge_bg_green);
        } else {
            bannerPermission.setVisibility(View.VISIBLE);
            tvStatusPill.setText("🔴 Permission Needed");
            tvStatusPill.setBackgroundResource(R.drawable.badge_bg_red);
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && TextUtils.equals(pkgName, cn.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void openNotificationAccessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshData() {
        transferList.clear();
        transferList.addAll(db.getAllTransfers());
        adapter.notifyDataSetChanged();

        double total = db.getTodayTotalAmount();
        int pending = db.getPendingCount();

        tvTodayTotal.setText(String.format(Locale.US, "₱ %.2f", total));
        tvPendingCount.setText(pending + " pending");

        tvTransferCount.setText(transferList.size() + " items");
        if (transferList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvTransfers.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvTransfers.setVisibility(View.VISIBLE);
        }
    }

    private void sendTestTransfer() {
        long randomRef = 100000000000L + (long)(new Random().nextDouble() * 899999999999L);
        String refStr = String.valueOf(randomRef);
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        TransferRecord testTr = new TransferRecord(
                59.00,
                refStr,
                "TEST BUYER (CHINO)",
                "You have received PHP 59.00 of GCash from TEST BUYER (CHINO) with Ref. No. " + refStr + " on " + now,
                now,
                "QUEUED"
        );

        db.insertTransfer(testTr);
        NetworkSyncManager.getInstance(this).triggerSync();
        refreshData();
        Toast.makeText(this, "Simulated ₱59.00 GCash Transfer (Ref #" + refStr + ")", Toast.LENGTH_SHORT).show();
    }

    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
        final EditText inputUrl = new EditText(this);
        inputUrl.setHint("Server API URL");
        inputUrl.setText(NetworkSyncManager.getInstance(this).getServerUrl());

        new AlertDialog.Builder(this)
                .setTitle("Bridge Configuration")
                .setView(inputUrl)
                .setPositiveButton("Save", (dialog, which) -> {
                    String url = inputUrl.getText().toString().trim();
                    if (!url.isEmpty()) {
                        NetworkSyncManager.getInstance(this).setServerUrl(url);
                        tvServerSub.setText("Server: " + url.replace("https://", "").replace("http://", "").split("/")[0]);
                        Toast.makeText(this, "Server URL updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
