package org.moandor.securemessage;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;

public class IdentityRecord {
    private SignalProtocolAddress mAddress;
    private IdentityKey mIdentityKey;

    public IdentityRecord(SignalProtocolAddress address, IdentityKey identityKey) {
        mAddress = address;
        mIdentityKey = identityKey;
    }

    public SignalProtocolAddress getAddress() {
        return mAddress;
    }

    public IdentityKey getIdentityKey() {
        return mIdentityKey;
    }
}
