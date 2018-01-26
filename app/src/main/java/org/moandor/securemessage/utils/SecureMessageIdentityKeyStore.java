package org.moandor.securemessage.utils;

import android.util.Base64;

import org.moandor.securemessage.IdentityDatabaseHelper;
import org.moandor.securemessage.IdentityRecord;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.util.guava.Optional;

public class SecureMessageIdentityKeyStore implements IdentityKeyStore {
    private static final Object LOCK = new Object();

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        try {
            IdentityKey publicKey = new IdentityKey(Base64.decode(
                    PreferenceUtils.getIdentityPublic(),
                    Base64.NO_PADDING | Base64.NO_WRAP), 0);
            ECPrivateKey privateKey = Curve.decodePrivatePoint(Base64.decode(
                    PreferenceUtils.getIdentityPrivate(),
                    Base64.NO_PADDING | Base64.NO_WRAP));
            return new IdentityKeyPair(publicKey, privateKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new AssertionError(e);
        }
    }

    @Override
    public int getLocalRegistrationId() {
        return PreferenceUtils.getLocalRegistrationId();
    }

    @Override
    public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        synchronized (LOCK) {
            IdentityDatabaseHelper helper = new IdentityDatabaseHelper();
            Optional<IdentityRecord> record = helper.getIdentity(address);
            if (!record.isPresent()) {
                helper.saveIdentity(new IdentityRecord(address, identityKey));
                return false;
            }
            helper.saveIdentity(new IdentityRecord(address, identityKey));
            return true;
        }
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {
        return true;
    }

    public void saveIdentityKeyPair(IdentityKeyPair identityKeyPair) {
        PreferenceUtils.setIdentityPublic(
                Base64.encodeToString(identityKeyPair.getPublicKey().serialize(),
                        Base64.NO_PADDING | Base64.NO_WRAP));
        PreferenceUtils.setIdentityPrivate(
                Base64.encodeToString(identityKeyPair.getPrivateKey().serialize(),
                        Base64.NO_PADDING | Base64.NO_WRAP));
    }

    public void saveLocalRegistrationId(int id) {
        PreferenceUtils.setLocalRegistrationId(id);
    }
}
