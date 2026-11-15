package com.orai.dahta.settings;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.orai.dahta.constant.SettingsConstants;

/** 앱 내부 SQLite 데이터베이스. 안내 설정을 키-값 테이블 하나에 저장한다. */
final class SettingsDbHelper extends SQLiteOpenHelper {

    SettingsDbHelper(Context context) {
        super(context, SettingsConstants.DB_NAME, null, SettingsConstants.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + SettingsConstants.TABLE + " ("
                + SettingsConstants.COLUMN_KEY + " TEXT PRIMARY KEY, "
                + SettingsConstants.COLUMN_VALUE + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 아직 버전 1뿐이다. 테이블이 바뀌면 여기서 마이그레이션한다.
    }
}
