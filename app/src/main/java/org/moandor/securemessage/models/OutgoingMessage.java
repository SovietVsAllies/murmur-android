package org.moandor.securemessage.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

public class OutgoingMessage implements Parcelable {
    private UUID mRecipient;
    private String mMessage;

    public OutgoingMessage(UUID recipient, String message) {
        mRecipient = recipient;
        mMessage = message;
    }

    public UUID getTargetId() {
        return mRecipient;
    }

    public String getMessage() {
        return mMessage;
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
            return new OutgoingMessage(targetId, message);
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
    }
}
