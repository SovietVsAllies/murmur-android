package org.moandor.securemessage.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

public class IncomingMessage implements Parcelable {
    public IncomingMessage(UUID sender, String message) {
        mSender = sender;
        mMessage = message;
    }

    public UUID getSender() {
        return mSender;
    }

    public String getMessage() {
        return mMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mSender.getMostSignificantBits());
        dest.writeLong(mSender.getLeastSignificantBits());
        dest.writeString(mMessage);
    }

    public static final Creator<IncomingMessage> CREATOR = new Creator<IncomingMessage>() {
        @Override
        public IncomingMessage createFromParcel(Parcel in) {
            long msb = in.readLong();
            long lsb = in.readLong();
            UUID sender = new UUID(msb, lsb);
            String message = in.readString();
            return new IncomingMessage(sender, message);
        }

        @Override
        public IncomingMessage[] newArray(int size) {
            return new IncomingMessage[size];
        }
    };

    private UUID mSender;
    private String mMessage;
}
