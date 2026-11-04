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

public class LocationResponse
implements ServerResponse {
    private final String username;
    private final double x;
    private final double y;
    private final double z;
    private final String dimension;

    public LocationResponse(String username, double x, double y, double z, String dimension) {
        this.username = username;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    public String username() {
        return this.username;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public double z() {
        return this.z;
    }

    public String dimension() {
        return this.dimension;
    }

    @Override
    public CommandType type() {
        return CommandType.LOCATION;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
            DataOutputStream out = new DataOutputStream(baos);
            byte[] userBytes = this.username.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)userBytes.length);
            out.write(userBytes);
            out.writeDouble(this.x);
            out.writeDouble(this.y);
            out.writeDouble(this.z);
            byte[] dimBytes = this.dimension.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)dimBytes.length);
            out.write(dimBytes);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode LocationResponse", e);
        }
    }
}
