package org.moandor.securemessage.models;

import org.moandor.securemessage.utils.Memory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StunMessage {
    public StunMessage(InputStream stream) throws IOException {
        byte[] bytes = new byte[8];
        int count = stream.read(bytes);
        if (count != bytes.length) {
            throw new IOException("Incomplete stun message");
        }
        short type = Memory.bytesToShort(
                Arrays.copyOfRange(bytes, 0, 2), ByteOrder.BIG_ENDIAN);
        int cls = ((type >> 4) & 1) | (((type >> 8) & 1) << 1);
        switch (cls) {
            case 0b00:
                mClass = Class.REQUEST;
                break;
            case 0b01:
                mClass = Class.INDICATION;
                break;
            case 0b10:
                mClass = Class.SUCCESS_RESPONSE;
                break;
            case 0b11:
                mClass = Class.ERROR_RESPONSE;
                break;
            default:
                throw new RuntimeException("Invalid class: " + cls);
        }
        int method = (type & 0xf) | (((type >> 5) & 0x7) << 4) | (((type >> 9) & 0x1f) << 7);
        switch (method) {
            case 1:
                mMethod = Method.BINDING;
                break;
            default:
                throw new IOException("Invalid method: " + method);
        }
        int magic = Memory.bytesToInt(
                Arrays.copyOfRange(bytes, 4, 8), ByteOrder.BIG_ENDIAN);
        if (magic != MAGIC) {
            throw new IOException("Invalid magic cookie: " + Integer.toHexString(magic));
        }
        mTransactionId = new TransactionId(stream);
        byte[] attrHeader = new byte[4];
        while (stream.read(attrHeader) == attrHeader.length) {
            short attrType = Memory.bytesToShort(
                    Arrays.copyOfRange(attrHeader, 0, 2), ByteOrder.BIG_ENDIAN);
            short length = Memory.bytesToShort(
                    Arrays.copyOfRange(attrHeader, 2, 4), ByteOrder.BIG_ENDIAN);
            switch (attrType) {
                case 0x0020:
                    mAttributes.add(new XorMappedAddressAttribute(length, stream));
                    break;
                default:
                    if (stream.skip(length) != length) {
                        throw new IOException("Invalid attributes");
                    }
                    break;
            }
        }
    }

    public void serialize(OutputStream stream) throws IOException {
        int cls;
        switch (mClass) {
            case REQUEST:
                cls = 0b00;
                break;
            case INDICATION:
                cls = 0b01;
                break;
            case SUCCESS_RESPONSE:
                cls = 0b10;
                break;
            case ERROR_RESPONSE:
                cls = 0b11;
                break;
            default:
                throw new RuntimeException("Invalid message class: " + mClass);
        }
        int method;
        switch (mMethod) {
            case BINDING:
                method = 1;
                break;
            default:
                throw new RuntimeException("Invalid message method: " + mMethod);
        }
        short type = (short) (((cls & 1) << 4) | ((cls >> 1) << 8) |
                (method & 0xf) | (((method >> 4) & 0x7) << 5) | (((method >> 7) & 0x1f) << 9));
        stream.write(Memory.shortToBytes(type, ByteOrder.BIG_ENDIAN));
        int length = 0;
        for (Attribute attribute : mAttributes) {
            length += attribute.getLength();
        }
        stream.write(Memory.shortToBytes((short) length, ByteOrder.BIG_ENDIAN));
        stream.write(Memory.intToBytes(MAGIC, ByteOrder.BIG_ENDIAN));
        mTransactionId.serialize(stream);
        for (Attribute attribute : mAttributes) {
            attribute.serialize(stream);
        }
    }

    public Class getMessageClass() {
        return mClass;
    }

    public Method getMethod() {
        return mMethod;
    }

    public TransactionId getTransactionId() {
        return mTransactionId;
    }

    public List<Attribute> getAttributes() {
        return mAttributes;
    }

    public static final int MAGIC = 0x2112a442;

    public enum Class {
        REQUEST, INDICATION, SUCCESS_RESPONSE, ERROR_RESPONSE
    }

    public enum Method {
        BINDING
    }

    public static StunMessage requestBinding() {
        return new StunMessage(Class.REQUEST, Method.BINDING, TransactionId.random());
    }

    public static class TransactionId {
        public long getLeastSignificantBits() {
            return mLeastSigBits;
        }

        public int getMostSignificantBits() {
            return mMostSigBits;
        }

        private long mLeastSigBits;
        private int mMostSigBits;

        private TransactionId(InputStream stream) throws IOException {
            byte[] bytes = new byte[12];
            int count = stream.read(bytes);
            if (count != bytes.length) {
                throw new IOException("Incomplete transaction id");
            }
            fromBytes(bytes);
        }

        private TransactionId(byte[] bytes) {
            fromBytes(bytes);
        }

        private void serialize(OutputStream stream) throws IOException {
            stream.write(Memory.intToBytes(mMostSigBits, ByteOrder.BIG_ENDIAN));
            stream.write(Memory.longToBytes(mLeastSigBits, ByteOrder.BIG_ENDIAN));
        }

        private void fromBytes(byte[] bytes) {
            mLeastSigBits = Memory.bytesToLong(
                    Arrays.copyOfRange(bytes, 4, 12), ByteOrder.BIG_ENDIAN);
            mMostSigBits = Memory.bytesToInt(
                    Arrays.copyOfRange(bytes, 0, 4), ByteOrder.BIG_ENDIAN);
        }

        private static SecureRandom sRandom;

        private static TransactionId random() {
            byte[] data = new byte[12];
            synchronized (TransactionId.class) {
                if (sRandom == null) {
                    sRandom = new SecureRandom();
                }
            }
            sRandom.nextBytes(data);
            return new TransactionId(data);
        }
    }

    public static class Attribute {
        public Type getType() {
            return mType;
        }

        public int getLength() {
            return mLength;
        }

        public enum Type {
            XOR_MAPPED_ADDRESS
        }

        protected void serialize(OutputStream stream) throws IOException {
            switch (mType) {
                case XOR_MAPPED_ADDRESS:
                    stream.write(Memory.shortToBytes((short) 0x0020, ByteOrder.BIG_ENDIAN));
                    break;
                default:
                    throw new IOException("Invalid attribute type: " + mType);
            }
            stream.write(Memory.shortToBytes((short) mLength, ByteOrder.BIG_ENDIAN));
        }

        private Type mType;
        private int mLength;

        private Attribute(Type type, int length) {
            mType = type;
            mLength = length;
        }
    }

    public static class XorMappedAddressAttribute extends Attribute {
        public Family getFamily() {
            return mFamily;
        }

        public int getXPort() {
            return mXPort;
        }

        public byte[] getXAddress() {
            return mXAddress;
        }

        public enum Family {
            IPV4, IPV6
        }

        @Override
        protected void serialize(OutputStream stream) throws IOException {
            super.serialize(stream);
            switch (mFamily) {
                case IPV4:
                    stream.write(Memory.shortToBytes((short) 0x0001, ByteOrder.BIG_ENDIAN));
                    break;
                case IPV6:
                    stream.write(Memory.shortToBytes((short) 0x0002, ByteOrder.BIG_ENDIAN));
                    break;
                default:
                    throw new IOException("Invalid family: " + mFamily);
            }
            stream.write(Memory.shortToBytes((short) mXPort, ByteOrder.BIG_ENDIAN));
            stream.write(mXAddress);
        }

        private Family mFamily;
        private int mXPort;
        private byte[] mXAddress;

        private XorMappedAddressAttribute(int length, InputStream stream) throws IOException {
            super(Type.XOR_MAPPED_ADDRESS, length);
            if (length < 8) {
                throw new IOException("Invalid length: " + length);
            }
            byte[] bytes = new byte[length];
            int count = stream.read(bytes);
            if (count != bytes.length) {
                throw new IOException("Incomplete xor mapped address attribute");
            }
            byte family = bytes[1];
            switch (family) {
                case 0x01:
                    mFamily = Family.IPV4;
                    if (length != 8) {
                        throw new IOException("Invalid length for IPv4: " + length);
                    }
                    break;
                case 0x02:
                    mFamily = Family.IPV6;
                    if (length != 20) {
                        throw new IOException("Invalid length for IPv6: " + length);
                    }
                    break;
                default:
                    throw new IOException("Invalid family: " + family);
            }
            mXPort = Memory.bytesToShort(
                    Arrays.copyOfRange(bytes, 2, 4), ByteOrder.BIG_ENDIAN);
            if (mXPort < 0) {
                mXPort += (1 << 16);
            }
            mXAddress = Arrays.copyOfRange(bytes, 4, bytes.length);
        }
    }

    private Class mClass;
    private Method mMethod;
    private TransactionId mTransactionId;
    private final List<Attribute> mAttributes = new ArrayList<>();

    private StunMessage(Class cls, Method method, TransactionId transactionId) {
        mClass = cls;
        mMethod = method;
        mTransactionId = transactionId;
    }
}
