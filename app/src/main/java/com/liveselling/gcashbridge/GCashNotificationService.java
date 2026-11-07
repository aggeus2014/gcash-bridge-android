package com.liveselling.gcashbridge;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCashNotificationService extends NotificationListenerService {
    private static final String TAG = "GCashNotifService";
    public static final String GCASH_PACKAGE = "com.globe.gcash.android";
    public static final String ACTION_TRANSFER_RECEIVED = "com.liveselling.gcashbridge.TRANSFER_RECEIVED";

    // Regex for GCash transfers:
    // e.g., "You have received PHP 799.00 of GCash from JUAN DELA CRUZ with Ref. No. 100234567891 on 11/08/2026."
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(?:PHP|Php|₱|P)\\s*([\\d,]+\\.?\\d{0,2})");
    private static final Pattern REF_PATTERN = Pattern.compile("(?i)(?:Ref(?:\\s*No\\.?|\\s*#)?|Reference(?:\\s*No\\.?)?)\\s*[:.\\s]*(\\d{10,14})");
    private static final Pattern SENDER_PATTERN = Pattern.compile("(?i)(?:from|sent\\s+by)\\s+([A-Za-z0-9\\s.]+?)(?:\\s+with|\\s+on|\\.|\\s+ref|$)");

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String packageName = sbn.getPackageName();

        // Also permit test package when testing locally
        if (!GCASH_PACKAGE.equals(packageName) && !"com.liveselling.gcashbridge".equals(packageName)) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        String fullMessage = (title != null ? title + " " : "") +
                (text != null ? text + " " : "") +
                (bigText != null ? bigText : "");

        Log.i(TAG, "Inbound notification from " + packageName + ": " + fullMessage);
        processNotificationText(fullMessage);
    }

    public void processNotificationText(String text) {
        if (text == null || text.trim().isEmpty()) return;

        // Check if message is a received money transaction
        String lower = text.toLowerCase(Locale.US);
        if (!lower.contains("received") && !lower.contains("sent you") && !lower.contains("transferred") && !lower.contains("payment")) {
            return;
        }

        // 1. Extract Amount
        double amount = 0.0;
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(text);
        if (amountMatcher.find()) {
            try {
                String cleanAmount = amountMatcher.group(1).replace(",", "").trim();
                amount = Double.parseDouble(cleanAmount);
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse amount: " + e.getMessage());
            }
        }

        // 2. Extract Reference Number
        String refNo = "";
        Matcher refMatcher = REF_PATTERN.matcher(text);
        if (refMatcher.find()) {
            refNo = refMatcher.group(1).trim();
        }

        // If no explicit Ref pattern match, search for any standalone 10-14 digit sequence
        if (refNo.isEmpty()) {
            Matcher numMatcher = Pattern.compile("\\b(\\d{10,14})\\b").matcher(text);
            if (numMatcher.find()) {
                refNo = numMatcher.group(1).trim();
            }
        }

        if (refNo.isEmpty()) {
            Log.w(TAG, "No reference number found in notification: " + text);
            return;
        }

        // 3. Extract Sender Name
        String sender = "GCash Sender";
        Matcher senderMatcher = SENDER_PATTERN.matcher(text);
        if (senderMatcher.find()) {
            String s = senderMatcher.group(1).trim();
            if (!s.isEmpty()) {
                sender = s;
            }
        }

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        Log.i(TAG, "Captured Transfer: ₱" + amount + " | Ref: " + refNo + " | Sender: " + sender);

        // 4. Save to SQLite Queue
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        TransferRecord tr = new TransferRecord(amount, refNo, sender, text, now, "QUEUED");
        boolean inserted = db.insertTransfer(tr);

        if (inserted) {
            // Trigger instant sync in background
            NetworkSyncManager.getInstance(this).triggerSync();

            // Broadcast to MainActivity to refresh UI immediately
            Intent intent = new Intent(ACTION_TRANSFER_RECEIVED);
            intent.putExtra("ref_no", refNo);
            intent.putExtra("amount", amount);
            intent.putExtra("sender", sender);
            sendBroadcast(intent);
        }
    }
}
