package org.moandor.securemessage;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.moandor.securemessage.models.Account;
import org.moandor.securemessage.networking.AddPreKeysDao;
import org.moandor.securemessage.networking.IdRegistrationDao;
import org.moandor.securemessage.utils.NotifyException;
import org.moandor.securemessage.utils.PreferenceUtils;
import org.moandor.securemessage.utils.SecureMessageProtocolStore;
import org.moandor.securemessage.utils.TextUtils;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        TextView mLocalId = view.findViewById(R.id.local_id);
        Optional<UUID> accountId = PreferenceUtils.getLocalAccountId();
        if (accountId.isPresent()) {
            setIdText(mLocalId, accountId.get());
        } else {
            new IdRegistrationTask(mLocalId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private static void setIdText(TextView text, UUID id) {
        text.setText(GlobalContext.getInstance().getString(
                R.string.local_id, TextUtils.uuidToBase64(id)));
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
            super.onPreExecute();
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
            super.onPostExecute(account);
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
