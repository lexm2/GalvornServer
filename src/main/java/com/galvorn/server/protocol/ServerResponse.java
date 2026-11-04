/*
 * Decompiled with CFR 0.152.
 */
package com.galvorn.server.protocol;

import com.galvorn.server.protocol.CommandType;

public interface ServerResponse {
    public CommandType type();

    public byte[] encode();
}
