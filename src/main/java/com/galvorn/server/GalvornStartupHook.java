package com.galvorn.server;

import com.galvorn.server.GalvornWrapper;
import com.galvorn.server.protocol.ShadowRealm;

import java.lang.instrument.Instrumentation;

/**
 * Galvorn startup hook - loads on class load.
 *
 * <p>This is the thin entry point in the GalvornServer JAR that
 * delegates to the agent's GalvornWrapper implementation.
 *
 * <p>Logging is controlled by the {@link ShadowRealm#SHADOW} flag.
 */
public class GalvornStartupHook {

    static final GalvornWrapper wrapper = new GalvornWrapper();
    static volatile boolean ready = false;

    static {
        try {
            ready = true;
        } catch (Exception e) {}
    }

    public static void premain(String args, Instrumentation inst) {
        if (!ShadowRealm.SHADOW) {
            System.err.println("[Galvorn] Agent loaded via premain!");
        }
        GalvornNetworkHook.hookNetwork();
        if (!ShadowRealm.SHADOW) {
            System.err.println("[Galvorn] Network hook installed!");
        }
    }

    public static void onPlayerPacket(Object player, byte[] packet) {
        if (ready) {
            wrapper.onPlayerPacket(player, packet);
        }
    }
}
