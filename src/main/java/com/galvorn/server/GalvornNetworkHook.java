package com.galvorn.server;

import com.galvorn.server.protocol.GalvornProtocol;
import com.galvorn.server.protocol.ShadowRealm;

/**
 * Galvorn network hook — intercepts packets from Minecraft's existing
 * network stream and routes them to the typed protocol layer.
 *
 * <h3>Network path (no new ports)</h3>
 * <pre>
 *   Client ──MC packet──→ NetworkMgr.inputStream (field_149177_d)
 *                          │
 *                          │  reflection read
 *                          ▼
 *                   GalvornNetworkHook.onPacket()
 *                          │  raw bytes
 *                          ▼
 *                   CommandDecoder.decode(frame)
 *                          │  ServerCommand
 *                          ▼
 *                   GalvornWrapper.onCommand(cmd)
 *                          │  ServerResponse
 *                          ▼
 *                   ResponseEncoder.encode() → wire bytes
 *                          │
 *                          └──→ same MC stream → Client
 * </pre>
 *
 * <p>The hook reads bytes from the packet's {@code InputStream} via
 * reflection on the obfuscated field {@code field_149177_d}. It does
 * <b>not</b> open a separate TCP socket — it taps into the existing
 * Minecraft network stream. The server only needs the standard MC
 * port open (e.g. 25565).
 *
 * <h3>Loopback IpcServer</h3>
 * <p>The Galvorn agent (sidecar) hosts {@code IpcServer} for
 * agent↔bridge IPC. This is bound to {@code 127.0.0.1:0}
 * (loopback, ephemeral port) and is completely transparent to
 * external firewalls — it does not require any open port.
 *
 * <p>Logging is controlled by the {@link ShadowRealm#SHADOW} flag.
 */
public class GalvornNetworkHook {

    static boolean hooked;
    static GalvornProtocol protocol;

    static {
        protocol = (GalvornProtocol) new com.galvorn.server.GalvornWrapper();
    }

    public static void hookNetwork() {
        if (hooked) return;
        hooked = true;
    }

    public static void setProtocol(GalvornProtocol newProtocol) {
        protocol = newProtocol;
    }

    /**
     * Called when MC delivers a packet. Reads raw bytes from the
     * packet's InputStream via reflection and delegates to the
     * protocol layer.
     *
     * @param handler the network handler (e.g. NetHandlerPlayServer)
     * @param packet  the network packet (e.g. CPacketCustomPayload)
     */
    public static void onPacket(Object handler, Object packet) {
        if (!hooked) return;
        try {
            byte[] data = new byte[1024];
            int len = ((java.io.InputStream) packet.getClass()
                .getDeclaredField("field_149177_d").get(packet)).read(data);
            byte[] frame = java.util.Arrays.copyOf(data, len);

            if (!ShadowRealm.SHADOW) {
                ShadowRealm.log("[NetworkHook] Read " + len + " bytes, decoding...");
            }

            Object player = handler.getClass()
                .getDeclaredField("field_147371_t").get(handler);
            protocol.onPlayerPacket(player, frame);
        } catch (Exception e) {
            if (!ShadowRealm.SHADOW) {
                ShadowRealm.log("[NetworkHook] Exception: " + e);
            }
        }
    }
}
