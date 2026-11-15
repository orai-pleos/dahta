package com.orai.dahta.settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orai.dahta.constant.SettingsConstants;

/** {@link SettingsStore} 를 앱 내부 SQLite 에 저장하는 구현. 같은 키는 마지막 값으로 덮어쓴다. */
public final class SqliteSettingsStore implements SettingsStore {
    private final SettingsDbHelper helper;

    public SqliteSettingsStore(Context context) {
        helper = new SettingsDbHelper(context.getApplicationContext());
    }

    @Override
    public String sttLanguage() {
        return get(SettingsConstants.KEY_STT_LANGUAGE, SettingsConstants.DEFAULT_LANGUAGE);
    }

    @Override
    public void setSttLanguage(String languageCode) {
        put(SettingsConstants.KEY_STT_LANGUAGE, languageCode);
    }

    @Override
    public String ttsLanguage() {
        return get(SettingsConstants.KEY_TTS_LANGUAGE, SettingsConstants.DEFAULT_LANGUAGE);
    }

    @Override
    public void setTtsLanguage(String languageCode) {
        put(SettingsConstants.KEY_TTS_LANGUAGE, languageCode);
    }

    @Override
    public int speechSpeedStep() {
        String saved = get(SettingsConstants.KEY_SPEECH_SPEED_STEP, null);
        if (saved == null) {
            return SettingsConstants.SPEED_STEP_DEFAULT;
        }
        try {
            int step = Integer.parseInt(saved);
            return Math.max(0, Math.min(SettingsConstants.SPEED_STEP_MAX, step));
        } catch (NumberFormatException e) {
            return SettingsConstants.SPEED_STEP_DEFAULT;
        }
    }

    @Override
    public void setSpeechSpeedStep(int step) {
        put(SettingsConstants.KEY_SPEECH_SPEED_STEP, Integer.toString(step));
    }

    @Override
    public boolean careMode() {
        String saved = get(SettingsConstants.KEY_CARE_MODE, null);
        return saved == null ? SettingsConstants.DEFAULT_CARE_MODE : Boolean.parseBoolean(saved);
    }

    @Override
    public void setCareMode(boolean enabled) {
        put(SettingsConstants.KEY_CARE_MODE, Boolean.toString(enabled));
    }

    @Override
    public void close() {
        helper.close();
    }

    private String get(String key, String defaultValue) {
        try (Cursor cursor = helper.getReadableDatabase().query(
                SettingsConstants.TABLE,
                new String[]{SettingsConstants.COLUMN_VALUE},
                SettingsConstants.COLUMN_KEY + " = ?",
                new String[]{key},
                null, null, null)) {
            return cursor.moveToFirst() ? cursor.getString(0) : defaultValue;
        }
    }

    private void put(String key, String value) {
        ContentValues values = new ContentValues();
        values.put(SettingsConstants.COLUMN_KEY, key);
        values.put(SettingsConstants.COLUMN_VALUE, value);
        helper.getWritableDatabase().insertWithOnConflict(
                SettingsConstants.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
