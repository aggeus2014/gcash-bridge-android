package com.liveselling.gcashbridge;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "gcash_bridge.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TRANSFERS = "transfers";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_REF_NO = "ref_no";
    public static final String COLUMN_SENDER_NAME = "sender_name";
    public static final String COLUMN_RAW_TEXT = "raw_text";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_SYNCED_AT = "synced_at";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_TRANSFERS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_AMOUNT + " REAL, "
                + COLUMN_REF_NO + " TEXT UNIQUE, "
                + COLUMN_SENDER_NAME + " TEXT, "
                + COLUMN_RAW_TEXT + " TEXT, "
                + COLUMN_TIMESTAMP + " TEXT, "
                + COLUMN_STATUS + " TEXT, "
                + COLUMN_SYNCED_AT + " TEXT"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSFERS);
        onCreate(db);
    }

    public synchronized boolean insertTransfer(TransferRecord record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AMOUNT, record.getAmount());
        values.put(COLUMN_REF_NO, record.getRefNo());
        values.put(COLUMN_SENDER_NAME, record.getSenderName());
        values.put(COLUMN_RAW_TEXT, record.getRawText());
        values.put(COLUMN_TIMESTAMP, record.getTimestamp());
        values.put(COLUMN_STATUS, record.getStatus());
        values.put(COLUMN_SYNCED_AT, record.getSyncedAt());

        long result = db.insertWithOnConflict(TABLE_TRANSFERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return result != -1;
    }

    public synchronized void updateStatus(String refNo, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, status);
        if ("SYNCED".equalsIgnoreCase(status)) {
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            values.put(COLUMN_SYNCED_AT, now);
        }
        db.update(TABLE_TRANSFERS, values, COLUMN_REF_NO + " = ?", new String[]{refNo});
    }

    public synchronized List<TransferRecord> getAllTransfers() {
        List<TransferRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSFERS + " ORDER BY " + COLUMN_ID + " DESC LIMIT 100", null);

        if (cursor.moveToFirst()) {
            do {
                TransferRecord tr = new TransferRecord();
                tr.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                tr.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                tr.setRefNo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REF_NO)));
                tr.setSenderName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_NAME)));
                tr.setRawText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RAW_TEXT)));
                tr.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                tr.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
                tr.setSyncedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNCED_AT)));
                list.add(tr);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public synchronized List<TransferRecord> getPendingTransfers() {
        List<TransferRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSFERS + " WHERE " + COLUMN_STATUS + " != 'SYNCED' ORDER BY " + COLUMN_ID + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                TransferRecord tr = new TransferRecord();
                tr.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                tr.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                tr.setRefNo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REF_NO)));
                tr.setSenderName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_NAME)));
                tr.setRawText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RAW_TEXT)));
                tr.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                tr.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
                list.add(tr);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public synchronized double getTodayTotalAmount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String todayPrefix = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSFERS + " WHERE " + COLUMN_TIMESTAMP + " LIKE ?", new String[]{todayPrefix + "%"});
        double total = 0.0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public synchronized int getPendingCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRANSFERS + " WHERE " + COLUMN_STATUS + " != 'SYNCED'", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}
