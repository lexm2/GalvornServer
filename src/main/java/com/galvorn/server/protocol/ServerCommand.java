/*
 * Decompiled with CFR 0.152.
 */
package com.galvorn.server.protocol;

import com.galvorn.server.protocol.CommandType;

public interface ServerCommand {
    public CommandType type();

    public byte[] payload();

    default public byte[] apply() {
        return this.payload();
    }
}
