package org.moandor.securemessage.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

public class PendingMessage implements Parcelable {
    private UUID mTargetId;
    private String mMessage;

    public PendingMessage(UUID targetId, String message) {
        mTargetId = targetId;
        mMessage = message;
    }

    public UUID getTargetId() {
        return mTargetId;
    }

    public String getMessage() {
        return mMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PendingMessage> CREATOR = new Creator<PendingMessage>() {
        @Override
        public PendingMessage createFromParcel(Parcel in) {
            long msb = in.readLong();
            long lsb = in.readLong();
            UUID targetId = new UUID(msb, lsb);
            String message = in.readString();
            return new PendingMessage(targetId, message);
        }

        @Override
        public PendingMessage[] newArray(int size) {
            return new PendingMessage[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mTargetId.getMostSignificantBits());
        dest.writeLong(mTargetId.getLeastSignificantBits());
        dest.writeString(mMessage);
    }
}
