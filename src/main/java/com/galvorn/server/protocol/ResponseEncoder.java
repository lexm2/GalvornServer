/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.galvorn.server.ipc.VarInt
 */
package com.galvorn.server.protocol;

import com.galvorn.server.ipc.VarInt;
import com.galvorn.server.protocol.ServerResponse;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;

public final class ResponseEncoder {
    private ResponseEncoder() {
    }

    public static byte[] encode(ServerResponse response) {
        try {
            byte[] payload = response.encode();
            int type = response.type().toWire();
            int totalLen = 1 + payload.length;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(totalLen + 5);
            DataOutputStream out = new DataOutputStream(baos);
            VarInt.write((OutputStream)out, (int)totalLen);
            out.writeByte(type);
            out.write(payload);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode response", e);
        }
    }

    public static byte[] encodePayload(ServerResponse response) {
        return response.encode();
    }

    public static byte[] encodeFrame(byte[] body) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(body.length + 5);
            DataOutputStream out = new DataOutputStream(baos);
            VarInt.write((OutputStream)out, (int)body.length);
            out.write(body);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode frame", e);
        }
    }
}
