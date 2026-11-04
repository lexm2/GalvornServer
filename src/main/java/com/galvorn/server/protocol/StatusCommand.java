/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.galvorn.server.ipc.VarInt
 */
package com.galvorn.server.protocol;

import com.galvorn.server.ipc.VarInt;
import com.galvorn.server.protocol.CommandType;
import com.galvorn.server.protocol.ServerCommand;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StatusCommand
implements ServerCommand {
    private final String username;

    public StatusCommand(String username) {
        this.username = username;
    }

    public String username() {
        return this.username;
    }

    @Override
    public CommandType type() {
        return CommandType.STATUS;
    }

    @Override
    public byte[] payload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
            DataOutputStream out = new DataOutputStream(baos);
            if (this.username != null && !this.username.isEmpty()) {
                byte[] nameBytes = this.username.getBytes(StandardCharsets.UTF_8);
                VarInt.write((OutputStream)out, (int)nameBytes.length);
                out.write(nameBytes);
            }
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode StatusCommand", e);
        }
    }

    @Override
    public byte[] apply() {
        return this.payload();
    }
}
