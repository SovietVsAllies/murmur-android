package org.moandor.securemessage.models;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

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

    public static Account parseJson(String jsonStr) throws JSONException {
        JSONObject json = new JSONObject(jsonStr);
        return new Account(
                UUID.fromString(json.getString("id")),
                Base64.decode(json.getString("identity_key"), Base64.DEFAULT),
                Base64.decode(json.getString("signed_pre_key"), Base64.DEFAULT));
    }
}
