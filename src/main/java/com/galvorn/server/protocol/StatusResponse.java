/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.galvorn.agent.ipc.VarInt
 */
package com.galvorn.server.protocol;

import com.galvorn.server.ipc.VarInt;
import com.galvorn.server.protocol.CommandType;
import com.galvorn.server.protocol.ServerResponse;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StatusResponse
implements ServerResponse {
    private final String status;
    private final long uptime;
    private final int playerCount;

    public StatusResponse(String status, long uptime, int playerCount) {
        this.status = status;
        this.uptime = uptime;
        this.playerCount = playerCount;
    }

    public String status() {
        return this.status;
    }

    public long uptime() {
        return this.uptime;
    }

    public int playerCount() {
        return this.playerCount;
    }

    @Override
    public CommandType type() {
        return CommandType.STATUS;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
            DataOutputStream out = new DataOutputStream(baos);
            byte[] statusBytes = this.status.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)statusBytes.length);
            out.write(statusBytes);
            out.writeLong(this.uptime);
            out.writeInt(this.playerCount);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode StatusResponse", e);
        }
    }
}
