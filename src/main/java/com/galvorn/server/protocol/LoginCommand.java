/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.galvorn.agent.ipc.VarInt
 */
package com.galvorn.server.protocol;

import com.galvorn.server.ipc.VarInt;
import com.galvorn.server.protocol.CommandType;
import com.galvorn.server.protocol.ServerCommand;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LoginCommand
implements ServerCommand {
    private final String username;
    private final String key;

    public LoginCommand(String username, String key) {
        this.username = username;
        this.key = key;
    }

    public String username() {
        return this.username;
    }

    public String key() {
        return this.key;
    }

    @Override
    public CommandType type() {
        return CommandType.LOGIN;
    }

    @Override
    public byte[] payload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
            DataOutputStream out = new DataOutputStream(baos);
            byte[] nameBytes = this.username.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)nameBytes.length);
            out.write(nameBytes);
            byte[] keyBytes = this.key.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)keyBytes.length);
            out.write(keyBytes);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode LoginCommand", e);
        }
    }

    @Override
    public byte[] apply() {
        return this.payload();
    }
}
