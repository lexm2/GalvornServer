/*
 * Decompiled with CFR 0.152.
 */
package com.galvorn.server.protocol;

import com.galvorn.server.protocol.ServerCommand;
import com.galvorn.server.protocol.ServerResponse;
import java.util.Set;

public interface GalvornProtocol {
    public ServerResponse onCommand(ServerCommand var1);

    public void sendResponse(Object var1, ServerResponse var2);

    public boolean authenticate(String var1, String var2);

    public Set<String> getAuthenticated();

    public void onPlayerPacket(Object var1, byte[] var2);

    public String getPlayerDesiredEnchantment(Object var1);

    public void setPlayerDesiredEnchantment(Object var1, String var2);

    public Set<String> getPlayerItemEnchantments(Object var1, int var2);

    public void setPlayerItemEnchantments(Object var1, int var2, String var3);
}
