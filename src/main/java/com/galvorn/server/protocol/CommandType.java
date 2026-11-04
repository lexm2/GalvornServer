/*
 * Decompiled with CFR 0.152.
 */
package com.galvorn.server.protocol;

public enum CommandType {
    LOGIN(48),
    LOCATION(49),
    CHUNKS(50),
    CHAT(51),
    STATUS(52),
    SET_ENCHANT(53),
    ADD_ENCHANTS(54);

    private final int wireValue;

    private CommandType(int wireValue) {
        this.wireValue = wireValue;
    }

    public int toWire() {
        return this.wireValue;
    }

    public static CommandType fromWire(int value) {
        for (CommandType type : CommandType.values()) {
            if (type.wireValue != value) continue;
            return type;
        }
        throw new IllegalArgumentException("Unknown command type: 0x" + Integer.toHexString(value));
    }
}
