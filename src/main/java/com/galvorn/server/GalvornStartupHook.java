package com.galvorn.server;

import com.galvorn.server.GalvornWrapper;
import com.galvorn.server.protocol.ShadowRealm;



/**
 * Galvorn startup hook — baked into the Forge JAR at compile time.
 *
 * <p>Loaded by Forge's classloader on class initialization. The {@code
 * static} block creates {@link GalvornWrapper} and calls
 * {@link GalvornNetworkHook#hookNetwork()} to install the ASM
 * transformer that intercepts {@code NetworkManager} — all without
 * any {@code -javaagent} flag or sidecar JAR.
 *
 * <p>Logging is controlled by the {@link ShadowRealm#SHADOW} flag.
 */
public class GalvornStartupHook {

    static final GalvornWrapper wrapper = new GalvornWrapper();
    static volatile boolean ready = false;

    static {
        try {
            GalvornNetworkHook.hookNetwork();
            ready = true;
            if (!ShadowRealm.SHADOW) {
                System.err.println("[Galvorn] Baked into Forge JAR, network hook installed!");
            }
        } catch (Exception e) {
            if (!ShadowRealm.SHADOW) {
                System.err.println("[Galvorn] Failed to initialize: " + e);
            }
        }
    }

    /**
     * Called by Forge when the network handler receives a packet.
     */
    public static void onPlayerPacket(Object player, byte[] packet) {
        if (ready) {
            wrapper.onPlayerPacket(player, packet);
        }
    }
}
