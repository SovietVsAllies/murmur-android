package org.moandor.securemessage.services;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.moandor.securemessage.GlobalContext;
import org.moandor.securemessage.MainFragment;
import org.moandor.securemessage.models.Account;
import org.moandor.securemessage.models.IncomingMessage;
import org.moandor.securemessage.models.OutgoingMessage;
import org.moandor.securemessage.networking.FetchAccountDao;
import org.moandor.securemessage.networking.FetchPreKeyDao;
import org.moandor.securemessage.utils.NotifyException;
import org.moandor.securemessage.utils.PreferenceUtils;
import org.moandor.securemessage.utils.SecureMessageProtocolStore;
import org.moandor.securemessage.utils.UrlHelper;
import org.moandor.securemessage.utils.Utilities;
import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SessionBuilder;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MessageService extends Service {
    public static final String ACTION_SEND_MESSAGE =
            Utilities.addPackagePrefix("ACTION_SEND_MESSAGE");

    public static final String EXTRA_MESSAGE = Utilities.addPackagePrefix("EXTRA_MESSAGE");

    private boolean mRunning = false;
    private String mLocalAccountId;
    private LinkedBlockingQueue<OutgoingMessage> mPendingMessages = new LinkedBlockingQueue<>();
    private Thread mWorkerThread;

    private static final String TAG = MessageService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalAccountId = PreferenceUtils.getLocalAccountId().get().toString();
        mRunning = true;
        mWorkerThread = new Thread(new Worker());
        mWorkerThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            handleCommand(intent);
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mRunning = false;
        mWorkerThread.interrupt();
        super.onDestroy();
    }

    private void handleCommand(Intent intent) {
        if (Objects.equals(intent.getAction(), ACTION_SEND_MESSAGE)) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }
            OutgoingMessage message = extras.getParcelable(EXTRA_MESSAGE);
            mPendingMessages.add(message);
        }
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            while (mRunning) {
                final SecureMessageProtocolStore store = new SecureMessageProtocolStore();
                WebSocket socket = null;
                try {
                    socket = new WebSocketFactory().createSocket(
                            UrlHelper.WEB_SOCKET_MESSAGE + "?account_id=" + mLocalAccountId);
                    socket.addListener(new WebSocketAdapter() {
                        @Override
                        public void onTextMessage(WebSocket websocket, String text) {
                            Log.d(TAG, "Message received: " + text);
                            try {
                                JSONObject json = new JSONObject(text);
                                if (json.getString("type").equals("received_message")) {
                                    JSONObject data = json.getJSONObject("data");
                                    String sender = data.getString("sender");
                                    String messageType = data.getString("message_type");
                                    byte[] content = Base64.decode(
                                            data.getString("content"), Base64.DEFAULT);
                                    SessionCipher cipher = new SessionCipher(
                                            store, new SignalProtocolAddress(sender, 0));
                                    byte[] decryptedMessage;
                                    switch (messageType) {
                                        case "signal_message":
                                            decryptedMessage = cipher.decrypt(
                                                    new SignalMessage(content));
                                            break;
                                        case "pre_key_signal_message":
                                            decryptedMessage = cipher.decrypt(
                                                    new PreKeySignalMessage(content));
                                            break;
                                        default:
                                            Log.d(TAG, "Unknown message type: " +
                                                    messageType);
                                            return;
                                    }
                                    try {
                                        IncomingMessage message = new IncomingMessage(
                                                UUID.fromString(sender),
                                                new String(decryptedMessage, "UTF-8"));
                                        Intent intent =
                                                new Intent(MainFragment.ACTION_MESSAGE_RECEIVED);
                                        intent.putExtra(
                                                MainFragment.EXTRA_MESSAGE_RECEIVED, message);
                                        GlobalContext.getInstance().sendBroadcast(intent);
                                    } catch (UnsupportedEncodingException e) {
                                        throw new AssertionError(e);
                                    }
                                }
                            } catch (JSONException |
                                    LegacyMessageException |
                                    InvalidMessageException |
                                    InvalidKeyException |
                                    InvalidKeyIdException |
                                    DuplicateMessageException |
                                    InvalidVersionException |
                                    UntrustedIdentityException |
                                    NoSessionException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    socket.connect();
                    while (mRunning) {
                        OutgoingMessage message = mPendingMessages.poll(
                                1, TimeUnit.MINUTES);
                        if (message == null) {
                            continue;
                        }
                        SignalProtocolAddress address = new SignalProtocolAddress(
                                message.getTargetId().toString(), 0);
                        if (!store.containsSession(address)) {
                            FetchPreKeyDao fetchPreKeyDao =
                                    new FetchPreKeyDao(message.getTargetId());
                            PreKeyRecord preKeyRecord = fetchPreKeyDao.execute();
                            FetchAccountDao fetchAccountDao =
                                    new FetchAccountDao(message.getTargetId());
                            Account targetAccount = fetchAccountDao.execute();
                            SignedPreKeyRecord signedPreKeyRecord = new SignedPreKeyRecord(
                                    targetAccount.getSignedPreKey());
                            IdentityKey identityKey = new IdentityKey(
                                    targetAccount.getIdentityKey(), 0);
                            PreKeyBundle preKeyBundle = new PreKeyBundle(
                                    0, 0,
                                    preKeyRecord.getId(),
                                    preKeyRecord.getKeyPair().getPublicKey(),
                                    signedPreKeyRecord.getId(),
                                    signedPreKeyRecord.getKeyPair().getPublicKey(),
                                    signedPreKeyRecord.getSignature(),
                                    identityKey);
                            new SessionBuilder(store, address).process(preKeyBundle);
                        }
                        SessionCipher cipher = new SessionCipher(store, address);
                        JSONObject data = new JSONObject();
                        data.put("type", "send_message");
                        CiphertextMessage encryptedMessage = cipher.encrypt(
                                message.getMessage().getBytes("UTF-8"));
                        JSONObject data1 = new JSONObject();
                        data1.put("receiver", message.getTargetId());
                        data1.put("content", Base64.encodeToString(
                                encryptedMessage.serialize(),
                                Base64.DEFAULT));
                        if (encryptedMessage instanceof SignalMessage) {
                            data1.put("message_type", "signal_message");
                        } else if (encryptedMessage instanceof PreKeySignalMessage) {
                            data1.put("message_type", "pre_key_signal_message");
                        } else {
                            throw new AssertionError("Unknown message type");
                        }
                        data.put("data", data1);
                        socket.sendText(data.toString());
                        Log.d(TAG, "Message sent: " + data.toString());
                    }
                } catch (IOException | InterruptedException | JSONException | WebSocketException |
                        InvalidKeyException | UntrustedIdentityException e) {
                    e.printStackTrace();
                } catch (final NotifyException e) {
                    final GlobalContext context = GlobalContext.getInstance();
                    context.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    if (socket != null) {
                        socket.disconnect();
                    }
                }
            }
        }
    }
}
