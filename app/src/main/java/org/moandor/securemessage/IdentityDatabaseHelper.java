package org.moandor.securemessage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.util.guava.Optional;

public class IdentityDatabaseHelper extends SQLiteOpenHelper {
    private static final String TABLE_NAME = "identities";
    private static final String ADDRESS = "address";
    private static final String IDENTITY_KEY = "identity_key";

    private static final String CREATE_TABLE = "create table " + TABLE_NAME + "(" +
            ADDRESS + " text unique, " + IDENTITY_KEY + " text);";

    private static final int VERSION = 1;

    public IdentityDatabaseHelper() {
        super(GlobalContext.getInstance(), "sm.db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void saveIdentity(IdentityRecord record) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ADDRESS, record.getAddress().getName());
        values.put(IDENTITY_KEY, Base64.encodeToString(
                record.getIdentityKey().serialize(), Base64.DEFAULT));
        database.replace(TABLE_NAME, null, values);
    }

    public Optional<IdentityRecord> getIdentity(SignalProtocolAddress address) {
        SQLiteDatabase database = getReadableDatabase();
        try (Cursor cursor = database.query(TABLE_NAME, null, ADDRESS + " = ?",
                new String[]{address.getName()}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return Optional.of(new IdentityRecord(
                        address,
                        new IdentityKey(
                                Base64.decode(
                                        cursor.getString(cursor.getColumnIndex(IDENTITY_KEY)),
                                        Base64.DEFAULT),
                                0)));
            }
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return Optional.absent();
    }
}
