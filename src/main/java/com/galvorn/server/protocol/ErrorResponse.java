/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.galvorn.server.ipc.VarInt
 */
package com.galvorn.server.protocol;

import com.galvorn.server.ipc.VarInt;
import com.galvorn.server.protocol.CommandType;
import com.galvorn.server.protocol.ServerResponse;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ErrorResponse
implements ServerResponse {
    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String message() {
        return this.message;
    }

    @Override
    public CommandType type() {
        return CommandType.STATUS;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
            DataOutputStream out = new DataOutputStream(baos);
            byte[] msgBytes = this.message.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)msgBytes.length);
            out.write(msgBytes);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode ErrorResponse", e);
        }
    }
}
