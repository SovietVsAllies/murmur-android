package org.moandor.securemessage.networking;

import android.util.Log;

import org.moandor.securemessage.models.StunMessage;
import org.moandor.securemessage.utils.Memory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;

public class PeerNetwork {
    public PeerNetwork() throws IOException {
        DatagramSocket socket = new DatagramSocket();
        StunMessage message = StunMessage.requestBinding();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.serialize(out);
        byte[] data = out.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length,
                new InetSocketAddress("stun.l.google.com", 19302));
        socket.send(packet);
        packet = new DatagramPacket(new byte[0x1000], 0x1000);
        socket.receive(packet);
        InputStream in = new ByteArrayInputStream(packet.getData());
        message = new StunMessage(in);
        Log.d("net", String.valueOf(message.getMessageClass()));
        Log.d("net", String.valueOf(message.getMethod()));
        for (StunMessage.Attribute attribute : message.getAttributes()) {
            if (attribute.getType() == StunMessage.Attribute.Type.XOR_MAPPED_ADDRESS) {
                StunMessage.XorMappedAddressAttribute xorMappedAddressAttribute =
                        (StunMessage.XorMappedAddressAttribute) attribute;
                int address = Memory.bytesToInt(xorMappedAddressAttribute.getXAddress(),
                        ByteOrder.BIG_ENDIAN) ^ StunMessage.MAGIC;
                StringBuilder builder = new StringBuilder(String.valueOf(address >>> 24));
                for (int i = 2; i >= 0; --i) {
                    builder.append(".").append((address >>> (8 * i)) & 0xff);
                }
                Log.d("net", builder.toString());
                Log.d("net", String.valueOf(
                        xorMappedAddressAttribute.getXPort() ^ (StunMessage.MAGIC >>> 16)));
            }
        }
        while (true) {
            packet = new DatagramPacket(new byte[0x1000], 0x1000);
            socket.receive(packet);
            Log.d("received", new String(packet.getData()));
        }
    }
}
