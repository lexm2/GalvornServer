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

public class SetEnchantResponse
implements ServerResponse {
    private final String username;
    private final String enchantName;

    public SetEnchantResponse(String username, String enchantName) {
        this.username = username;
        this.enchantName = enchantName;
    }

    public String username() {
        return this.username;
    }

    public String enchantName() {
        return this.enchantName;
    }

    @Override
    public CommandType type() {
        return CommandType.SET_ENCHANT;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
            DataOutputStream out = new DataOutputStream(baos);
            byte[] nameBytes = this.username.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)nameBytes.length);
            out.write(nameBytes);
            byte[] enchantBytes = this.enchantName.getBytes(StandardCharsets.UTF_8);
            VarInt.write((OutputStream)out, (int)enchantBytes.length);
            out.write(enchantBytes);
            return baos.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to encode SetEnchantResponse", e);
        }
    }
}
