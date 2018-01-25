package org.moandor.securemessage.utils;

import android.support.annotation.NonNull;

import org.moandor.securemessage.GlobalContext;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SessionStore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SecureMessageSessionStore implements SessionStore {
    private static final int FILE_VERSION = 0;
    private static final Object LOCK = new Object();

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        synchronized (LOCK) {
            try (DataInputStream in = new DataInputStream(
                    new FileInputStream(getSessionFile(address)))) {
                int version = in.readInt();
                if (version != FILE_VERSION) {
                    throw new IOException("Unknown file format");
                }
                byte[] data = readBinary(in);
                return new SessionRecord(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new SessionRecord();
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        List<Integer> result = new ArrayList<>();
        try {
            File dir = getSessionDir();
            String[] files = dir.list();
            for (String file : files) {
                String[] parts = file.split("\\.", 2);
                if (parts[0].equals(name) && parts.length == 2) {
                    try {
                        result.add(Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        synchronized (LOCK) {
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(getSessionFile(address)))) {
                out.writeInt(FILE_VERSION);
                writeBinary(out, record.serialize());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        try {
            if (!getSessionFile(address).exists()) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        SessionRecord sessionRecord = loadSession(address);
        return sessionRecord.getSessionState().hasSenderChain() &&
                sessionRecord.getSessionState().getSessionVersion() ==
                        CiphertextMessage.CURRENT_VERSION;
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        try {
            if (!getSessionFile(address).delete()) {
                throw new IOException("Failed to delete session");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAllSessions(String name) {
        for (int device : getSubDeviceSessions(name)) {
            deleteSession(new SignalProtocolAddress(name, device));
        }
    }

    private File getSessionFile(SignalProtocolAddress address) throws IOException {
        return new File(getSessionDir(), address.getName() + '.' + address.getDeviceId());
    }

    private File getSessionDir() throws IOException {
        File directory = new File(GlobalContext.getInstance().getFilesDir(), "sessions");
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Failed to create session directory");
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
