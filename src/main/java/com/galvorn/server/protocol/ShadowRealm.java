/*
 * Decompiled with CFR 0.152.
 */
package com.galvorn.server.protocol;

public final class ShadowRealm {
    public static volatile boolean SHADOW;

    private ShadowRealm() {
    }

    public static void enable() {
        SHADOW = false;
    }

    public static void disable() {
        SHADOW = true;
    }

    public static void log(String message) {
        if (!SHADOW) {
            System.err.println("[Galvorn] " + message);
        }
    }

    public static void log(String prefix, String message) {
        if (!SHADOW) {
            System.err.println("[Galvorn]" + prefix + " " + message);
        }
    }

    public static void testShadowRealmSuppression() {
        boolean wasShadow = SHADOW;
        try {
            SHADOW = true;
            ShadowRealm.log("test message");
            ShadowRealm.log("[prefix] prefixed");
            SHADOW = false;
            ShadowRealm.log("visible message");
            SHADOW = wasShadow;
        }
        catch (Exception e) {
            SHADOW = wasShadow;
            throw e;
        }
    }

    static {
        String prop = System.getProperty("ShadowRealm");
        SHADOW = prop == null || Boolean.parseBoolean(prop);
    }
}
