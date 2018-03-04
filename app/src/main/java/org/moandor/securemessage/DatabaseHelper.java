package org.moandor.securemessage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import org.moandor.securemessage.models.Conversation;
import org.moandor.securemessage.models.ConversationMessage;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_IDENTITY);
        db.execSQL(CREATE_TABLE_CONVERSATION);
        db.execSQL(CREATE_TABLE_MESSAGE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                db.execSQL("alter table identities rename to " + Table.Identity.TABLE_NAME);
                db.execSQL(CREATE_TABLE_CONVERSATION);
                db.execSQL(CREATE_TABLE_MESSAGE);
        }
    }

    public synchronized void saveIdentity(IdentityRecord record) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Table.Identity.ADDRESS, record.getAddress().getName());
        values.put(Table.Identity.IDENTITY_KEY, Base64.encodeToString(
                record.getIdentityKey().serialize(), Base64.DEFAULT));
        database.replace(Table.Identity.TABLE_NAME, null, values);
    }

    public synchronized Optional<IdentityRecord> getIdentity(SignalProtocolAddress address) {
        SQLiteDatabase database = getReadableDatabase();
        try (Cursor cursor = database.query(
                Table.Identity.TABLE_NAME,
                null,
                Table.Identity.ADDRESS + " = ?",
                new String[]{address.getName()},
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return Optional.of(new IdentityRecord(
                        address,
                        new IdentityKey(Base64.decode(
                                cursor.getString(cursor.getColumnIndex(
                                        Table.Identity.IDENTITY_KEY)),
                                Base64.DEFAULT), 0)));
            }
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return Optional.absent();
    }

    public synchronized List<ConversationMessage> getMessages(
            UUID conversationId, Date maxDate, int maxCount) {
        SQLiteDatabase database = getReadableDatabase();
        List<ConversationMessage> result = new ArrayList<>();
        try (Cursor cursor = database.query(
                Table.Message.TABLE_NAME,
                null,
                Table.Message.CONVERSATION_ID + " = ? and " +
                        Table.Message.DATE_RECEIVED + " <= ?",
                new String[]{conversationId.toString(), String.valueOf(maxDate.getTime())},
                null,
                null,
                Table.Message.DATE_RECEIVED + " desc",
                String.valueOf(maxCount))) {
            while (cursor.moveToNext()) {
                result.add(new ConversationMessage(
                        UUID.fromString(cursor.getString(cursor.getColumnIndex(
                                Table.Message.CONVERSATION_ID))),
                        new Date(cursor.getLong(cursor.getColumnIndex(Table.Message.DATE_SENT))),
                        new Date(cursor.getLong(cursor.getColumnIndex(
                                Table.Message.DATE_RECEIVED))),
                        cursor.getString(cursor.getColumnIndex(Table.Message.CONTENT)),
                        cursor.getInt(cursor.getColumnIndex(Table.Message.DIRECTION)) == 0 ?
                                ConversationMessage.Direction.IN :
                                ConversationMessage.Direction.OUT));
            }
        }
        return result;
    }

    public synchronized void saveMessage(ConversationMessage message) {
        SQLiteDatabase database = getWritableDatabase();
        try (Cursor cursor = database.query(
                Table.Conversation.TABLE_NAME,
                new String[]{Table.Conversation.ID},
                Table.Conversation.ID + " = ?",
                new String[]{message.getConversationId().toString()},
                null,
                null,
                null)) {
            if (!cursor.moveToFirst()) {
                Conversation conversation = new Conversation(message.getConversationId());
                saveConversation(conversation);
            }
        }
        ContentValues values = new ContentValues();
        values.put(Table.Message.CONVERSATION_ID, message.getConversationId().toString());
        values.put(Table.Message.DATE_SENT, message.getDateSent().getTime());
        values.put(Table.Message.DATE_RECEIVED, message.getDateReceived().getTime());
        values.put(Table.Message.CONTENT, message.getContent());
        values.put(Table.Message.DIRECTION,
                message.getDirection() == ConversationMessage.Direction.IN ? 0 : 1);
        database.insert(Table.Message.TABLE_NAME, null, values);
    }

    public static synchronized DatabaseHelper getInstance() {
        if (sInstance == null) {
            sInstance = new DatabaseHelper();
        }
        return sInstance;
    }

    private DatabaseHelper() {
        super(GlobalContext.getInstance(), "sm.db", null, VERSION);
    }

    private void saveConversation(Conversation conversation) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Table.Conversation.ID, conversation.getId().toString());
        database.insert(Table.Conversation.TABLE_NAME, null, values);
    }

    private static DatabaseHelper sInstance;

    private static final String CREATE_TABLE_IDENTITY =
            "create table " + Table.Identity.TABLE_NAME + "(" +
                    Table.Identity.ADDRESS + " text unique, " +
                    Table.Identity.IDENTITY_KEY + " text);";

    private static final String CREATE_TABLE_CONVERSATION =
            "create table " + Table.Conversation.TABLE_NAME + "(" +
                    Table.Conversation.ID + " text unique);";

    private static final String CREATE_TABLE_MESSAGE =
            "Create table " + Table.Message.TABLE_NAME + "(" +
                    Table.Message.CONVERSATION_ID + " text, " +
                    Table.Message.DATE_SENT + " integer, " +
                    Table.Message.DATE_RECEIVED + " integer, " +
                    Table.Message.CONTENT + " text, " +
                    Table.Message.DIRECTION + " integer);";

    private static final int VERSION = 2;

    private static class Table {
        private static class Identity {
            private static final String TABLE_NAME = "identity";
            private static final String ADDRESS = "address";
            private static final String IDENTITY_KEY = "identity_key";
        }

        private static class Conversation {
            private static final String TABLE_NAME = "conversation";
            private static final String ID = "id";
        }

        private static class Message {
            private static final String TABLE_NAME = "message";
            private static final String CONVERSATION_ID = "conversation_id";
            private static final String DATE_SENT = "date_sent";
            private static final String DATE_RECEIVED = "date_received";
            private static final String CONTENT = "content";
            private static final String DIRECTION = "direction";
        }
    }
}
