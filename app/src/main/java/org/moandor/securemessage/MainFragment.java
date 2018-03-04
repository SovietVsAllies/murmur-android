package org.moandor.securemessage;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.moandor.securemessage.models.Account;
import org.moandor.securemessage.models.ConversationMessage;
import org.moandor.securemessage.models.IncomingMessage;
import org.moandor.securemessage.models.OutgoingMessage;
import org.moandor.securemessage.networking.AddPreKeysDao;
import org.moandor.securemessage.networking.IdRegistrationDao;
import org.moandor.securemessage.services.MessageService;
import org.moandor.securemessage.utils.NotifyException;
import org.moandor.securemessage.utils.PreferenceUtils;
import org.moandor.securemessage.utils.SecureMessageProtocolStore;
import org.moandor.securemessage.utils.Utilities;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class MainFragment extends Fragment {
    public static final String ACTION_MESSAGE_RECEIVED =
            Utilities.addPackagePrefix("ACTION_MESSAGE_RECEIVED");

    public static final String EXTRA_MESSAGE_RECEIVED =
            Utilities.addPackagePrefix("EXTRA_MESSAGE_RECEIVED");

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
        {
            Optional<UUID> target = PreferenceUtils.getTargetId();
            if (target.isPresent()) {
                mTarget.setText(target.get().toString());
            }
        }
        Optional<UUID> accountId = PreferenceUtils.getLocalAccountId();
        if (accountId.isPresent()) {
            setIdText(localId, accountId.get());
        } else {
            IdRegistrationTask task = new IdRegistrationTask(localId);
            task.setOnFinishListener(account -> {
                if (account == null) {
                    return;
                }
//                    mMessageTask = new MessageTask(mReceivedMessages, mTarget, mPendingMessages);
//                    mMessageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        Button sendButton = view.findViewById(R.id.send_button);
        final EditText sendMessage = view.findViewById(R.id.send_text);
        sendButton.setOnClickListener(v -> {
            UUID target = UUID.fromString(mTarget.getText().toString());
            OutgoingMessage message = new OutgoingMessage(
                    target,
                    sendMessage.getText().toString(),
                    new Date());
            Intent intent = new Intent(GlobalContext.getInstance(), MessageService.class);
            intent.setAction(MessageService.ACTION_SEND_MESSAGE);
            intent.putExtra(MessageService.EXTRA_MESSAGE, message);
            GlobalContext.getInstance().startService(intent);
            PreferenceUtils.setTargetId(target);
        });
        GlobalContext.getInstance().registerReceiver(
                mMessageReceiver, new IntentFilter(ACTION_MESSAGE_RECEIVED));
        new AsyncTask<Void, Void, List<ConversationMessage>>() {
            @Override
            protected void onPreExecute() {
                mConversationId = UUID.fromString(mTarget.getText().toString());
            }

            @Override
            protected List<ConversationMessage> doInBackground(Void... voids) {
                return DatabaseHelper.getInstance().getMessages(
                        mConversationId, new Date(), 100);
            }

            @Override
            protected void onPostExecute(List<ConversationMessage> result) {
                StringBuilder builder = new StringBuilder();
                for (int i = result.size() - 1; i >= 0; --i) {
                    ConversationMessage message = result.get(i);
                    UUID sender = message.getDirection() == ConversationMessage.Direction.IN ?
                            PreferenceUtils.getLocalAccountId().get() : message.getConversationId();
                    builder.append(String.format("%s: %s\n",
                            sender.toString(), message.getContent()));
                }
                mReceivedMessages.setText(builder.toString() + mReceivedMessages.getText());
            }

            private UUID mConversationId;
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onDestroy() {
        GlobalContext.getInstance().unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private TextView mReceivedMessages;
    private EditText mTarget;
    //    private MessageTask mMessageTask;
    private final LinkedBlockingQueue<String> mPendingMessages = new LinkedBlockingQueue<>();

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            assert extras != null;
            IncomingMessage message = extras.getParcelable(EXTRA_MESSAGE_RECEIVED);
            assert message != null;
            mReceivedMessages.append(String.format("%s: %s\n",
                    message.getSender().toString(), message.getMessage()));
        }
    };

    private static void setIdText(TextView text, UUID id) {
        text.setText(GlobalContext.getInstance().getString(R.string.local_id, id));
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
