/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.galvorn.agent.ipc.VarInt
 */
package com.galvorn.server.protocol;

import com.galvorn.server.ipc.VarInt;
import com.galvorn.server.protocol.ChatCommand;
import com.galvorn.server.protocol.ChunksCommand;
import com.galvorn.server.protocol.CommandType;
import com.galvorn.server.protocol.LocationCommand;
import com.galvorn.server.protocol.LoginCommand;
import com.galvorn.server.protocol.ServerCommand;
import com.galvorn.server.protocol.SetEnchantCommand;
import com.galvorn.server.protocol.StatusCommand;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class CommandDecoder {
    private CommandDecoder() {
    }

    public static ServerCommand decode(byte[] frame) throws Exception {
        int n;
        ByteArrayInputStream in = new ByteArrayInputStream(frame);
        int totalLen = VarInt.read((InputStream)in);
        if (totalLen < 1) {
            throw new Exception("Empty frame");
        }
        int msgType = in.read();
        if (msgType < 0) {
            throw new Exception("Invalid message type");
        }
        int bodyLen = totalLen - 1;
        byte[] body = new byte[bodyLen];
        for (int off = 0; off < bodyLen; off += n) {
            n = in.read(body, off, bodyLen - off);
            if (n >= 0) continue;
            throw new Exception("Short read on frame body");
        }
        return CommandDecoder.decodeCommand(msgType, body);
    }

    public static ServerCommand decodeCommand(int msgType, byte[] body) throws Exception {
        CommandType type = CommandType.fromWire(msgType);
        ByteArrayInputStream in = new ByteArrayInputStream(body);
        if (type == CommandType.LOGIN) {
            return CommandDecoder.decodeLogin(in);
        }
        if (type == CommandType.LOCATION) {
            return CommandDecoder.decodeLocation(in);
        }
        if (type == CommandType.CHUNKS) {
            return CommandDecoder.decodeChunks(in);
        }
        if (type == CommandType.CHAT) {
            return CommandDecoder.decodeChat(in);
        }
        if (type == CommandType.STATUS) {
            return CommandDecoder.decodeStatus(in);
        }
        if (type == CommandType.SET_ENCHANT) {
            return CommandDecoder.decodeSetEnchant(in);
        }
        throw new IllegalArgumentException("Unknown command type: " + (Object)((Object)type));
    }

    private static LoginCommand decodeLogin(ByteArrayInputStream in) throws Exception {
        int nameLen = VarInt.read((InputStream)in);
        byte[] nameBytes = new byte[nameLen];
        if (in.read(nameBytes) != nameLen) {
            throw new Exception("Short read on login name");
        }
        String username = new String(nameBytes, StandardCharsets.UTF_8);
        int keyLen = VarInt.read((InputStream)in);
        byte[] keyBytes = new byte[keyLen];
        if (in.read(keyBytes) != keyLen) {
            throw new Exception("Short read on login key");
        }
        String key = new String(keyBytes, StandardCharsets.UTF_8);
        return new LoginCommand(username, key);
    }

    private static LocationCommand decodeLocation(ByteArrayInputStream in) throws Exception {
        int nameLen = VarInt.read((InputStream)in);
        byte[] nameBytes = new byte[nameLen];
        if (in.read(nameBytes) != nameLen) {
            throw new Exception("Short read on location name");
        }
        String username = new String(nameBytes, StandardCharsets.UTF_8);
        return new LocationCommand(username);
    }

    private static ChunksCommand decodeChunks(ByteArrayInputStream in) throws Exception {
        int nameLen = VarInt.read((InputStream)in);
        byte[] nameBytes = new byte[nameLen];
        if (in.read(nameBytes) != nameLen) {
            throw new Exception("Short read on chunks name");
        }
        String username = new String(nameBytes, StandardCharsets.UTF_8);
        int radius = in.read();
        if (radius < 0) {
            throw new Exception("Invalid radius");
        }
        return new ChunksCommand(username, radius);
    }

    private static ChatCommand decodeChat(ByteArrayInputStream in) throws Exception {
        int nameLen = VarInt.read((InputStream)in);
        byte[] nameBytes = new byte[nameLen];
        if (in.read(nameBytes) != nameLen) {
            throw new Exception("Short read on chat name");
        }
        String username = new String(nameBytes, StandardCharsets.UTF_8);
        int textLen = VarInt.read((InputStream)in);
        byte[] textBytes = new byte[textLen];
        if (in.read(textBytes) != textLen) {
            throw new Exception("Short read on chat text");
        }
        String text = new String(textBytes, StandardCharsets.UTF_8);
        return new ChatCommand(username, text);
    }

    private static StatusCommand decodeStatus(ByteArrayInputStream in) throws Exception {
        int nameLen = VarInt.read((InputStream)in);
        if (nameLen == 0) {
            return new StatusCommand("");
        }
        byte[] nameBytes = new byte[nameLen];
        if (in.read(nameBytes) != nameLen) {
            throw new Exception("Short read on status name");
        }
        String username = new String(nameBytes, StandardCharsets.UTF_8);
        return new StatusCommand(username);
    }

    private static SetEnchantCommand decodeSetEnchant(ByteArrayInputStream in) throws Exception {
        int nameLen = VarInt.read((InputStream)in);
        byte[] nameBytes = new byte[nameLen];
        if (in.read(nameBytes) != nameLen) {
            throw new Exception("Short read on set enchant name");
        }
        String username = new String(nameBytes, StandardCharsets.UTF_8);
        int enchantLen = VarInt.read((InputStream)in);
        byte[] enchantBytes = new byte[enchantLen];
        if (in.read(enchantBytes) != enchantLen) {
            throw new Exception("Short read on set enchant name data");
        }
        String enchantName = new String(enchantBytes, StandardCharsets.UTF_8);
        return new SetEnchantCommand(username, enchantName);
    }
}
