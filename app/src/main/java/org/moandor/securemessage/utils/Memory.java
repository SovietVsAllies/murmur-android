package org.moandor.securemessage.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Memory {
    public static byte[] longToBytes(long x, ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).order(byteOrder);
        buffer.putLong(x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes, ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).order(byteOrder);
        buffer.put(bytes);
        buffer.position(0);
        return buffer.getLong();
    }

    public static byte[] intToBytes(int x, ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).order(byteOrder);
        buffer.putInt(x);
        return buffer.array();
    }

    public static int bytesToInt(byte[] bytes, ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).order(byteOrder);
        buffer.put(bytes);
        buffer.position(0);
        return buffer.getInt();
    }

    public static byte[] shortToBytes(short x, ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(Short.SIZE / Byte.SIZE).order(byteOrder);
        buffer.putShort(x);
        return buffer.array();
    }

    public static short bytesToShort(byte[] bytes, ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(Short.SIZE / Byte.SIZE).order(byteOrder);
        buffer.put(bytes);
        buffer.position(0);
        return buffer.getShort();
    }
}
