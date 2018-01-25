package org.moandor.securemessage.models;

import java.util.UUID;

public class Account {
    private UUID mId;
    private byte[] mIdentityKey;
    private byte[] mSignedPreKey;

    public Account(UUID id, byte[] identityKey, byte[] signedPreKey) {
        mId = id;
        mIdentityKey = identityKey;
        mSignedPreKey = signedPreKey;
    }

    public UUID getId() {
        return mId;
    }

    public byte[] getIdentityKey() {
        return mIdentityKey;
    }

    public byte[] getSignedPreKey() {
        return mSignedPreKey;
    }
}
