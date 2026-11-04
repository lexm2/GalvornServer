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

public class ChunksCommand
implements ServerCommand {
    private final String username;
    private final int radius;

    public ChunksCommand(String username, int radius) {
        this.username = username;
        this.radius = radius;
    }

    public String username() {
        return this.username;
    }

    public int radius() {
        return this.radius;
    }

    @Override
    public CommandType type() {
        return CommandType.CHUNKS;
    }

    @Override
    public byte[] payload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
            DataOutputStream out = new DataOutputStream(baos);
            byte[] nameBytes = this.username.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)nameBytes.length);
            out.write(nameBytes);
            out.writeByte(this.radius & 0xFF);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode ChunksCommand", e);
        }
    }

    @Override
    public byte[] apply() {
        return this.payload();
    }
}
