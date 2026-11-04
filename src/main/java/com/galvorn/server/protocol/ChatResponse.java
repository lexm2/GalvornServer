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

public class ChatResponse
implements ServerResponse {
    private final String username;
    private final String text;

    public ChatResponse(String username, String text) {
        this.username = username;
        this.text = text;
    }

    public String username() {
        return this.username;
    }

    public String text() {
        return this.text;
    }

    @Override
    public CommandType type() {
        return CommandType.CHAT;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
            DataOutputStream out = new DataOutputStream(baos);
            byte[] userBytes = this.username.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)userBytes.length);
            out.write(userBytes);
            byte[] textBytes = this.text.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)textBytes.length);
            out.write(textBytes);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode ChatResponse", e);
        }
    }
}
