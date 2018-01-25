package org.moandor.securemessage;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.SessionBuilder;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.impl.InMemorySignalProtocolStore;
import org.whispersystems.libsignal.util.KeyHelper;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager manager = getFragmentManager();
        Fragment fragment = manager.findFragmentById(android.R.id.content);
        if (fragment == null) {
            fragment = new MainFragment();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(android.R.id.content, fragment);
            transaction.commit();
        }

        testMessage();
    }

    private void testMessage() {
        IdentityKeyPair senderKeyPair = KeyHelper.generateIdentityKeyPair();
        IdentityKeyPair receiverKeyPair = KeyHelper.generateIdentityKeyPair();

        int senderRegId = KeyHelper.generateRegistrationId(true);
        int receiverRegId = KeyHelper.generateRegistrationId(true);

        SignalProtocolStore senderStore = new InMemorySignalProtocolStore(
                senderKeyPair, senderRegId);
        SignalProtocolStore receiverStore = new InMemorySignalProtocolStore(
                receiverKeyPair, receiverRegId);

        List<PreKeyRecord> senderPreKeys = KeyHelper.generatePreKeys(0, 100);
        for (PreKeyRecord preKeyRecord : senderPreKeys) {
            senderStore.storePreKey(preKeyRecord.getId(), preKeyRecord);
        }
        List<PreKeyRecord> receiverPreKeys = KeyHelper.generatePreKeys(0, 100);
        for (PreKeyRecord preKeyRecord : receiverPreKeys) {
            receiverStore.storePreKey(preKeyRecord.getId(), preKeyRecord);
        }

        try {
            SignedPreKeyRecord senderSignedPreKey = KeyHelper.generateSignedPreKey(
                    senderKeyPair, 0);
            senderStore.storeSignedPreKey(senderSignedPreKey.getId(), senderSignedPreKey);
            SignedPreKeyRecord receiverSignedPreKey = KeyHelper.generateSignedPreKey(
                    receiverKeyPair, 0);
            receiverStore.storeSignedPreKey(receiverSignedPreKey.getId(), receiverSignedPreKey);

            SignalProtocolAddress senderAddress = new SignalProtocolAddress(
                    "alice", 0);
            SignalProtocolAddress receiverAddress = new SignalProtocolAddress(
                    "bob", 0);

            SessionBuilder senderBuilder = new SessionBuilder(senderStore, receiverAddress);

            PreKeyBundle receiverPreKeyBundle = new PreKeyBundle(
                    receiverRegId,
                    0,
                    receiverPreKeys.get(0).getId(),
                    receiverPreKeys.get(0).getKeyPair().getPublicKey(),
                    receiverSignedPreKey.getId(),
                    receiverSignedPreKey.getKeyPair().getPublicKey(),
                    receiverSignedPreKey.getSignature(),
                    receiverKeyPair.getPublicKey());

            senderBuilder.process(receiverPreKeyBundle);

            SessionCipher senderSessionCipher = new SessionCipher(senderStore, receiverAddress);
            SessionCipher receiverSessionCipher = new SessionCipher(receiverStore, senderAddress);

            CiphertextMessage senderMessage = senderSessionCipher.encrypt("Hello world"
                    .getBytes("UTF-8"));
            PreKeySignalMessage receiverMessage = new PreKeySignalMessage(senderMessage.serialize());
            Log.d("message", new String(receiverSessionCipher.decrypt(receiverMessage)));
        } catch (InvalidKeyException | UntrustedIdentityException | UnsupportedEncodingException | LegacyMessageException | InvalidMessageException | DuplicateMessageException | InvalidVersionException | InvalidKeyIdException e) {
            e.printStackTrace();
        }
    }
}
