package org.moandor.securemessage.utils;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.util.UUID;

public class TextUtils {
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static String uuidToBase64(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return Base64.encodeToString(buffer.array(), Base64.NO_PADDING);
    }

    public static UUID uuidFromBase64(String base64) {
        ByteBuffer buffer = ByteBuffer.wrap(Base64.decode(base64, Base64.NO_PADDING));
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        return new UUID(msb, lsb);
    }
}
