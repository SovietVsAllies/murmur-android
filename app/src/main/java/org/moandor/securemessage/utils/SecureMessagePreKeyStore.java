package org.moandor.securemessage.utils;

import android.support.annotation.NonNull;

import org.moandor.securemessage.GlobalContext;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SecureMessagePreKeyStore implements PreKeyStore, SignedPreKeyStore {
    private static final int FILE_VERSION = 0;
    private static final Object LOCK = new Object();

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        synchronized (LOCK) {
            try (DataInputStream in = new DataInputStream(
                    new FileInputStream(getPreKeyFile(preKeyId)))) {
                return new PreKeyRecord(loadRecordData(in));
            } catch (IOException e) {
                e.printStackTrace();
                throw new InvalidKeyIdException(e);
            }
        }
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        synchronized (LOCK) {
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(getPreKeyFile(preKeyId)))) {
                saveRecordData(out, record.serialize());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        try {
            return getPreKeyFile(preKeyId).exists();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void removePreKey(int preKeyId) {
        try {
            if (!getPreKeyFile(preKeyId).delete()) {
                throw new IOException("Failed to delete PreKey: " + preKeyId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        synchronized (LOCK) {
            try (DataInputStream in = new DataInputStream(
                    new FileInputStream(getSignedPreKeyFile(signedPreKeyId)))) {
                return new SignedPreKeyRecord(loadRecordData(in));
            } catch (IOException e) {
                e.printStackTrace();
                throw new InvalidKeyIdException(e);
            }
        }
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        synchronized (LOCK) {
            List<SignedPreKeyRecord> result = new ArrayList<>();
            try {
                File dir = getSignedPreKeyDir();
                for (File file : dir.listFiles()) {
                    try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
                        if (!file.getName().equals("index.dat")) {
                            result.add(new SignedPreKeyRecord(loadRecordData(in)));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
        synchronized (LOCK) {
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(getSignedPreKeyFile(signedPreKeyId)))) {
                saveRecordData(out, record.serialize());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        try {
            return getSignedPreKeyFile(signedPreKeyId).exists();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        try {
            if (!getSignedPreKeyFile(signedPreKeyId).delete()) {
                throw new IOException("Failed to delete SignedPreKey: " + signedPreKeyId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] loadRecordData(DataInputStream in) throws IOException {
        int version = in.readInt();
        if (version != FILE_VERSION) {
            throw new IOException("Unknown record file format");
        }
        return readBinary(in);
    }

    private void saveRecordData(DataOutputStream out, byte[] data) throws IOException {
        out.writeInt(FILE_VERSION);
        writeBinary(out, data);
    }

    private File getPreKeyFile(int id) throws IOException {
        return new File(getPreKeyDir(), String.valueOf(id));
    }

    private File getSignedPreKeyFile(int id) throws IOException {
        return new File(getSignedPreKeyDir(), String.valueOf(id));
    }

    private File getPreKeyDir() throws IOException {
        return getRecordDir("prekeys");
    }

    private File getSignedPreKeyDir() throws IOException {
        return getRecordDir("signed-prekeys");
    }

    private File getRecordDir(String dirName) throws IOException {
        File directory = new File(GlobalContext.getInstance().getFilesDir(), dirName);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Failed to create record directory: " + dirName);
            }
        }
        return directory;
    }

    private byte[] readBinary(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.readFully(buffer);
        return buffer;
    }

    private void writeBinary(DataOutputStream out, @NonNull byte[] data) throws IOException {
        out.writeInt(data.length);
        out.write(data);
    }
}
