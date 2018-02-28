package org.moandor.securemessage;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.moandor.securemessage.models.Account;
import org.moandor.securemessage.models.PendingMessage;
import org.moandor.securemessage.networking.AddPreKeysDao;
import org.moandor.securemessage.networking.FetchAccountDao;
import org.moandor.securemessage.networking.FetchPreKeyDao;
import org.moandor.securemessage.networking.IdRegistrationDao;
import org.moandor.securemessage.services.MessageService;
import org.moandor.securemessage.utils.NotifyException;
import org.moandor.securemessage.utils.PreferenceUtils;
import org.moandor.securemessage.utils.SecureMessageProtocolStore;
import org.moandor.securemessage.utils.UrlHelper;
import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
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
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MainFragment extends Fragment {
    private TextView mReceivedMessages;
    private EditText mTarget;
//    private MessageTask mMessageTask;
    private final LinkedBlockingQueue<String> mPendingMessages = new LinkedBlockingQueue<>();

    private static final String TAG = MainFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        TextView localId = view.findViewById(R.id.local_id);
        mReceivedMessages = view.findViewById(R.id.received_messages);
        mTarget = view.findViewById(R.id.target_id);
        Optional<UUID> accountId = PreferenceUtils.getLocalAccountId();
        if (accountId.isPresent()) {
            setIdText(localId, accountId.get());
        } else {
            IdRegistrationTask task = new IdRegistrationTask(localId);
            task.setOnFinishListener(new IdRegistrationTask.OnFinishListener() {
                @Override
                public void onFinish(Account account) {
                    if (account == null) {
                        return;
                    }
//                    mMessageTask = new MessageTask(mReceivedMessages, mTarget, mPendingMessages);
//                    mMessageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        Button sendButton = view.findViewById(R.id.send_button);
        final EditText sendMessage = view.findViewById(R.id.send_text);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try {
//                    mPendingMessages.put(sendMessage.getText().toString());
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                PendingMessage message = new PendingMessage(
                        UUID.fromString(mTarget.getText().toString()),
                        sendMessage.getText().toString());
                Intent intent = new Intent(GlobalContext.getInstance(), MessageService.class);
                intent.setAction(MessageService.ACTION_SEND_MESSAGE);
                intent.putExtra(MessageService.EXTRA_MESSAGE, message);
                GlobalContext.getInstance().startService(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PreferenceUtils.getLocalAccountId().isPresent()) {
//            mMessageTask = new MessageTask(mReceivedMessages, mTarget, mPendingMessages);
//            mMessageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
//        if (mMessageTask != null) {
//            mMessageTask.cancel(true);
//        }
    }

    private static void setIdText(TextView text, UUID id) {
        text.setText(GlobalContext.getInstance().getString(R.string.local_id, id));
    }

    private static class MessageTask extends AsyncTask<Void, String, Void> {
        private WeakReference<TextView> mReceivedMessages = new WeakReference<>(null);
        private WeakReference<EditText> mTarget = new WeakReference<>(null);
        private LinkedBlockingQueue<String> mPendingMessages;
        private String mTargetId;
        private String mLocalAccountId;

        private MessageTask(
                TextView receivedMessages,
                EditText target,
                LinkedBlockingQueue<String> pendingMessages) {
            mReceivedMessages = new WeakReference<>(receivedMessages);
            mTarget = new WeakReference<>(target);
            mPendingMessages = pendingMessages;
        }

        @Override
        protected void onPreExecute() {
            mLocalAccountId = PreferenceUtils.getLocalAccountId().get().toString();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (!isCancelled()) {
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
                                    publishProgress(String.format("%s: %s\n", sender, new String(
                                            decryptedMessage, "UTF-8")));
                                }
                            } catch (JSONException |
                                    LegacyMessageException |
                                    InvalidMessageException |
                                    UnsupportedEncodingException |
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
                    while (!isCancelled()) {
                        String message = mPendingMessages.poll(1, TimeUnit.MINUTES);
                        final EditText target = mTarget.get();
                        if (message != null && target != null) {
                            mTargetId = null;
                            GlobalContext.getInstance().runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTargetId = target.getText().toString();
                                    synchronized (MessageTask.this) {
                                        MessageTask.this.notifyAll();
                                    }
                                }
                            });
                            synchronized (this) {
                                while (mTargetId == null) {
                                    wait();
                                }
                            }
                            UUID targetId;
                            try {
                                targetId = UUID.fromString(mTargetId);
                            } catch (IllegalArgumentException e) {
                                throw new NotifyException(R.string.invalid_target_id);
                            }
                            SignalProtocolAddress address = new SignalProtocolAddress(
                                    targetId.toString(), 0);
                            if (!store.containsSession(address)) {
                                FetchPreKeyDao fetchPreKeyDao = new FetchPreKeyDao(targetId);
                                PreKeyRecord preKeyRecord = fetchPreKeyDao.execute();
                                FetchAccountDao fetchAccountDao = new FetchAccountDao(targetId);
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
                                    message.getBytes("UTF-8"));
                            JSONObject data1 = new JSONObject();
                            data1.put("receiver", targetId);
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
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            TextView view = mReceivedMessages.get();
            if (view != null) {
                view.append(values[0]);
            }
        }
    }

    private static class IdRegistrationTask extends AsyncTask<Void, Void, Account> {
        private WeakReference<TextView> mLocalId;
        private Optional<NotifyException> mException = Optional.absent();
        private OnFinishListener mListener;

        private static WeakReference<IdRegistrationTask> sTask = new WeakReference<>(null);

        public IdRegistrationTask(TextView localId) {
            mLocalId = new WeakReference<>(localId);
        }

        public void setOnFinishListener(OnFinishListener listener) {
            mListener = listener;
        }

        @Override
        protected void onPreExecute() {
            IdRegistrationTask task = sTask.get();
            if (task != null && task.getStatus() != Status.FINISHED) {
                task.cancel(true);
            }
            sTask = new WeakReference<>(this);
        }

        @Override
        protected Account doInBackground(Void... voids) {
            SecureMessageProtocolStore store = new SecureMessageProtocolStore();

            IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
            store.saveIdentityKeyPair(identityKeyPair);

            int localRegistrationId = KeyHelper.generateRegistrationId(true);
            store.saveLocalRegistrationId(localRegistrationId);

            List<PreKeyRecord> preKeyRecords = KeyHelper.generatePreKeys(0, 100);
            for (PreKeyRecord preKeyRecord : preKeyRecords) {
                store.storePreKey(preKeyRecord.getId(), preKeyRecord);
            }

            try {
                SignedPreKeyRecord signedPreKeyRecord = KeyHelper.generateSignedPreKey(
                        identityKeyPair, 0);
                store.storeSignedPreKey(0, signedPreKeyRecord);

                ECPrivateKey emptyPrivateKey = new ECPrivateKey() {
                    @Override
                    public byte[] serialize() {
                        return new byte[0];
                    }

                    @Override
                    public int getType() {
                        return 0;
                    }
                };

                IdRegistrationDao idRegistrationDao = new IdRegistrationDao(
                        identityKeyPair.getPublicKey().serialize(),
                        new SignedPreKeyRecord(
                                signedPreKeyRecord.getId(),
                                signedPreKeyRecord.getTimestamp(),
                                new ECKeyPair(
                                        signedPreKeyRecord.getKeyPair().getPublicKey(),
                                        emptyPrivateKey),
                                signedPreKeyRecord.getSignature()
                        ).serialize());
                Account account = idRegistrationDao.execute();
                PreferenceUtils.setLocalAccountId(account.getId());

                List<Integer> preKeyIds = new ArrayList<>();
                List<byte[]> serializedPreKeys = new ArrayList<>();
                for (PreKeyRecord preKeyRecord : preKeyRecords) {
                    preKeyIds.add(preKeyRecord.getId());
                    serializedPreKeys.add(
                            new PreKeyRecord(
                                    preKeyRecord.getId(),
                                    new ECKeyPair(
                                            preKeyRecord.getKeyPair().getPublicKey(),
                                            emptyPrivateKey)
                            ).serialize());
                }
                AddPreKeysDao addPreKeysDao = new AddPreKeysDao(
                        account.getId(), preKeyIds, serializedPreKeys);
                addPreKeysDao.execute();

                return account;
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (NotifyException e) {
                mException = Optional.of(e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Account account) {
            if (account != null) {
                TextView localId = mLocalId.get();
                if (localId != null) {
                    setIdText(localId, account.getId());
                }
            } else if (mException.isPresent()) {
                Toast.makeText(
                        GlobalContext.getInstance(),
                        mException.get().getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
            if (mListener != null) {
                mListener.onFinish(account);
            }
        }

        public interface OnFinishListener {
            void onFinish(Account account);
        }
    }
}
