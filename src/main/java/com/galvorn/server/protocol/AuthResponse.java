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

public class AuthResponse
implements ServerResponse {
    private final boolean ok;
    private final String reason;

    public AuthResponse(boolean ok, String reason) {
        this.ok = ok;
        this.reason = reason;
    }

    public boolean ok() {
        return this.ok;
    }

    public String reason() {
        return this.reason;
    }

    @Override
    public CommandType type() {
        return CommandType.LOGIN;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
            DataOutputStream out = new DataOutputStream(baos);
            byte[] reasonBytes = this.reason.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)reasonBytes.length);
            out.write(reasonBytes);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode AuthResponse", e);
        }
    }
}
