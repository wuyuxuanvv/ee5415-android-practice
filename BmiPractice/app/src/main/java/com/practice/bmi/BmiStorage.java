package com.practice.bmi;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// SharedPreferences 用于自动回填。SQLite 用于跨天查询 BMI 历史。
final class BmiStorage extends SQLiteOpenHelper {
    static final String PREFERENCES = "bmi_preferences";
    private static final String TABLE = "bmi_results";

    record Draft(String height, String age, BmiModel.Gender gender) {}
    record Entry(LocalDate date, double bmi) {}

    private final SharedPreferences preferences;

    BmiStorage(Context context) {
        super(context, "bmi_history.db", null, 1);
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    Draft draft() {
        String gender = preferences.getString("gender", "");
        return new Draft(preferences.getString("height", ""), preferences.getString("age", ""),
                gender.isEmpty() ? null : BmiModel.Gender.valueOf(gender));
    }

    void saveDraft(String height, String age, BmiModel.Gender gender) {
        SharedPreferences.Editor edit = preferences.edit()
                .putString("height", height).putString("age", age);
        if (gender == null) {
            edit.remove("gender");
        } else {
            edit.putString("gender", gender.name());
        }
        edit.apply();
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        // ISO 日期既是唯一主键，也能直接按字符串排序。
        database.execSQL("CREATE TABLE " + TABLE + " (day TEXT PRIMARY KEY NOT NULL, bmi REAL NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        throw new IllegalStateException("Unexpected schema upgrade to version " + newVersion);
    }

    void saveResult(LocalDate day, double bmi) {
        ContentValues values = new ContentValues();
        values.put("day", day.toString());
        values.put("bmi", bmi);
        // 同日主键发生冲突时，由 SQLite 一次完成替换。
        long row = getWritableDatabase().replaceOrThrow(TABLE, null, values);
        if (row == -1) {
            throw new IllegalStateException("BMI result could not be saved");
        }
    }

    List<Entry> history() {
        List<Entry> entries = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE, new String[]{"day", "bmi"},
                null, null, null, null, "day ASC")) {
            while (cursor.moveToNext()) {
                entries.add(new Entry(LocalDate.parse(cursor.getString(0)), cursor.getDouble(1)));
            }
        }
        return entries;
    }
}
