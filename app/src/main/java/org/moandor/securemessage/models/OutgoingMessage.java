package org.moandor.securemessage.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.UUID;

public class OutgoingMessage implements Parcelable {
    public OutgoingMessage(UUID recipient, String message, Date dateSent) {
        mRecipient = recipient;
        mMessage = message;
        mDateSent = dateSent;
    }

    public UUID getTargetId() {
        return mRecipient;
    }

    public String getMessage() {
        return mMessage;
    }

    public Date getDateSent() {
        return mDateSent;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<OutgoingMessage> CREATOR = new Creator<OutgoingMessage>() {
        @Override
        public OutgoingMessage createFromParcel(Parcel in) {
            long msb = in.readLong();
            long lsb = in.readLong();
            UUID targetId = new UUID(msb, lsb);
            String message = in.readString();
            Date dateSent = new Date(in.readLong());
            return new OutgoingMessage(targetId, message, dateSent);
        }

        @Override
        public OutgoingMessage[] newArray(int size) {
            return new OutgoingMessage[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mRecipient.getMostSignificantBits());
        dest.writeLong(mRecipient.getLeastSignificantBits());
        dest.writeString(mMessage);
        dest.writeLong(mDateSent.getTime());
    }

    private UUID mRecipient;
    private String mMessage;
    private Date mDateSent;
}
