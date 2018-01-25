package org.moandor.securemessage;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import org.moandor.securemessage.networking.AddPreKeysDao;
import org.moandor.securemessage.networking.IdRegistrationDao;
import org.moandor.securemessage.utils.NotifyException;
import org.moandor.securemessage.utils.PreferenceUtils;
import org.moandor.securemessage.utils.SecureMessageProtocolStore;
import org.moandor.securemessage.utils.TextUtils;
import org.moandor.securemessage.utils.UrlHelper;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MainFragment extends Fragment {
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
        TextView receivedMessages = view.findViewById(R.id.received_messages);
        Optional<UUID> accountId = PreferenceUtils.getLocalAccountId();
        if (accountId.isPresent()) {
            setIdText(localId, accountId.get());
        } else {
            new IdRegistrationTask(localId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        final LinkedBlockingQueue<String> pendingMessages = new LinkedBlockingQueue<>();
        Button sendButton = view.findViewById(R.id.send_button);
        final EditText sendMessage = view.findViewById(R.id.send_text);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    pendingMessages.put(sendMessage.getText().toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        EditText target = view.findViewById(R.id.target_id);
        new MessageTask(receivedMessages, target, pendingMessages).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void setIdText(TextView text, UUID id) {
        text.setText(GlobalContext.getInstance().getString(
                R.string.local_id, TextUtils.uuidToBase64(id)));
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
            try {
                WebSocket socket = new WebSocketFactory().createSocket(
                        UrlHelper.WEB_SOCKET_MESSAGE + "?user=" + mLocalAccountId);
                socket.addListener(new WebSocketAdapter() {
                    @Override
                    public void onTextMessage(WebSocket websocket, String text) throws Exception {
                        publishProgress(text);
                    }
                });
                socket.connect();
                while (true) {
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
                        mTargetId = TextUtils.uuidFromBase64(mTargetId).toString();
                        JSONObject data = new JSONObject();
                        data.put("receiver", mTargetId);
                        data.put("content", message);
                        socket.sendText(data.toString());
                        Log.d(TAG, "Message sent: " + data.toString());
                    }
                    if (isCancelled()) {
                        break;
                    }
                }
            } catch (IOException | InterruptedException | JSONException | WebSocketException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String message = values[0];
            TextView view = mReceivedMessages.get();
            if (view != null) {
                view.append(message + '\n');
            }
        }
    }

    private static class IdRegistrationTask extends AsyncTask<Void, Void, Account> {
        private WeakReference<TextView> mLocalId;
        private Optional<NotifyException> mException = Optional.absent();

        private static WeakReference<IdRegistrationTask> sTask = new WeakReference<>(null);

        private IdRegistrationTask(TextView localId) {
            mLocalId = new WeakReference<>(localId);
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

                IdRegistrationDao idRegistrationDao = new IdRegistrationDao(
                        identityKeyPair.getPublicKey().serialize(),
                        signedPreKeyRecord.serialize());
                Account account = idRegistrationDao.execute();
                PreferenceUtils.setLocalAccountId(account.getId());

                List<Integer> preKeyIds = new ArrayList<>();
                List<byte[]> serializedPreKeys = new ArrayList<>();
                for (PreKeyRecord preKeyRecord : preKeyRecords) {
                    preKeyIds.add(preKeyRecord.getId());
                    serializedPreKeys.add(preKeyRecord.serialize());
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
        }
    }
}
