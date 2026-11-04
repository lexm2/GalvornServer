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
import java.util.List;

public class ChunksResponse
implements ServerResponse {
    private final String username;
    private final int radius;
    private final List<Chunk> chunks;

    public ChunksResponse(String username, int radius, List<Chunk> chunks) {
        this.username = username;
        this.radius = radius;
        this.chunks = chunks;
    }

    public String username() {
        return this.username;
    }

    public int radius() {
        return this.radius;
    }

    public List<Chunk> chunks() {
        return this.chunks;
    }

    @Override
    public CommandType type() {
        return CommandType.CHUNKS;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            DataOutputStream out = new DataOutputStream(baos);
            byte[] userBytes = this.username.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)userBytes.length);
            out.write(userBytes);
            out.writeByte(this.radius & 0xFF);
            VarInt.write((OutputStream)out, (int)this.chunks.size());
            for (Chunk chunk : this.chunks) {
                byte[] chunkBytes = chunk.data().getBytes(StandardCharsets.UTF_8);
                VarInt.write((OutputStream)out, (int)chunkBytes.length);
                out.write(chunkBytes);
            }
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode ChunksResponse", e);
        }
    }

    public static class Chunk {
        private final int x;
        private final int z;
        private final String data;

        public Chunk(int x, int z, String data) {
            this.x = x;
            this.z = z;
            this.data = data;
        }

        public int x() {
            return this.x;
        }

        public int z() {
            return this.z;
        }

        public String data() {
            return this.data;
        }
    }
}
