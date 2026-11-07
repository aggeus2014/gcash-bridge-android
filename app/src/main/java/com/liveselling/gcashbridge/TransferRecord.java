package com.liveselling.gcashbridge;

public class TransferRecord {
    private long id;
    private double amount;
    private String refNo;
    private String senderName;
    private String rawText;
    private String timestamp;
    private String status; // "SYNCED", "QUEUED", "FAILED"
    private String syncedAt;

    public TransferRecord() {}

    public TransferRecord(double amount, String refNo, String senderName, String rawText, String timestamp, String status) {
        this.amount = amount;
        this.refNo = refNo;
        this.senderName = senderName;
        this.rawText = rawText;
        this.timestamp = timestamp;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSyncedAt() { return syncedAt; }
    public void setSyncedAt(String syncedAt) { this.syncedAt = syncedAt; }
}
