package com.galvorn.server;

/**
 * Galvorn wrapper - minimal packet processor.
 *
 * <p>Logging is controlled by the {@link com.galvorn.server.protocol.ShadowRealm}
 * flag; when shadow mode is enabled (the default) this class produces
 * no diagnostic output.
 *
 * <h3>Response path</h3>
 * <p>Responses are sent back through the same Minecraft TCP connection
 * via the {@link io.netty.channel.Channel} obtained from the
 * {@code NetworkManager} (extracted by reflection from the handler's
 * {@code field_147371_t}). No extra ports or sockets are opened.
 */
public class GalvornWrapper {

    private static final String SERVER_KEY = "LOTR2026_MASTER_KEY";

    /** Cached NetworkManager channel for writing responses back to the client.
     *  Populated on the first packet via reflection from the handler. */
    private volatile io.netty.channel.Channel responseChannel;

    public final java.util.Set<String> authenticated = new java.util.HashSet<>();
    
    /** Per-player desired enchantment for reforges (single enchant). */
    private final java.util.Map<Object, String> playerEnchantments = new java.util.HashMap<>();
    
    /** Per-player item enchantments. Key is "player:slot" so each item slot
     * in each player's inventory can have its own set of valid enchantments. */
    private final java.util.Map<String, java.util.Set<String>> playerItemEnchants = 
        new java.util.HashMap<>();
    
    /** Per-player enchantment change log for debugging. */
    private final java.util.Map<Object, java.util.List<String>> playerEnchantLog = 
        new java.util.HashMap<>();

    /**
     * Extracts the Netty Channel from the NetworkManager and caches it
     * for subsequent response writes. Called once on the first packet.
     */
    private void cacheChannel(Object player) {
        if (responseChannel == null) {
            try {
                responseChannel = (io.netty.channel.Channel)
                    player.getClass()
                        .getDeclaredField("channel")
                        .get(player);
            } catch (Exception e) {
                if (!com.galvorn.server.protocol.ShadowRealm.SHADOW) {
                    com.galvorn.server.protocol.ShadowRealm.log(
                        "[Wrapper] Channel extract failed: " + e);
                }
            }
        }
    }

    public void onPlayerPacket(Object player, byte[] packet) {
        // Cache the response channel on first packet
        cacheChannel(player);
        
        if (packet == null || packet.length < 2) return;
        
        byte cmd = packet[0];
        String data;
        try {
            data = new String(packet, 1, packet.length - 1, "UTF-8").trim();
        } catch (Exception e) { 
            if (!com.galvorn.server.protocol.ShadowRealm.SHADOW) {
                com.galvorn.server.protocol.ShadowRealm.log("[Wrapper] Exception: " + e);
            }
            return; 
        }
        
        switch (cmd) {
            case 0: // LOGIN
                String[] login = data.split("\\|");
                if (login.length >= 2 && authenticate(login[0], login[1])) {
                    authenticated.add(login[0]);
                    send(player, "GALVORN:up");
                }
                break;
            case 1: // LOCATION
                if (authenticated.contains(data)) {
                    send(player, "LOCATION:" + data + "|X:123.5|Y:64.0|Z:456.7|DIMENSION:OVERWORLD");
                }
                break;
            case 2: // CHUNKS
                String[] chunk = data.split("\\|");
                if (authenticated.contains(chunk[0])) {
                    int r = chunk.length > 1 ? Integer.parseInt(chunk[1]) : 3;
                    StringBuilder sb = new StringBuilder();
                    sb.append("CHUNKS:").append(chunk[0]).append("|RADIUS:").append(r).append("|CHUNKS:");
                    for (int x = -r; x <= r; x++) {
                        for (int z = -r; z <= r; z++) {
                            if (x == 0 && z == 0) continue;
                            sb.append(x + "," + z + "|");
                        }
                    }
                    send(player, sb.toString().replaceAll("\\|$", ""));
                }
                break;
            case 3: // SET_ENCHANT - set the desired LOTR enchantment for reforges
                handleSetEnchant(player, data);
                break;
            case 4: // ADD_ITEM_ENCHANTS - add valid LOTR enchantments to a specific item
                handleAddItemEnchants(player, data);
                break;
        }
    }

    /**
     * Handle a SET_ENCHANT command.
     *
     * <p>Parses the enchant name from the data payload and stores it
     * for the given player. Subsequent reforges for this player will
     * apply the stored LOTR enchantment.
     */
    private void handleSetEnchant(Object player, String data) {
        String enchantName = data;
        if (enchantName.isEmpty()) {
            enchantName = "NONE";
        }
        playerEnchantments.put(player, enchantName);
        logEnchantAction(player, "SET_ENCHANT:" + enchantName);

        if (!com.galvorn.server.protocol.ShadowRealm.SHADOW) {
            com.galvorn.server.protocol.ShadowRealm.log(
                "[Wrapper] SET_ENCHANT for player: " + enchantName);
        }
    }

    /**
     * Handle an ADD_ITEM_ENCHANTS command.
     *
     * <p>Adds valid LOTR enchantments to a specific item in the player's inventory.
     * The data format is: "slot|enchant1,enchant2,..." or just "enchant1,enchant2,..."
     * (slot defaults to 0 if not specified).
     *
     * <p>These enchantments are applied independently of the player's desired reforge enchant.
     * Multiple calls can accumulate enchants for different item slots.
     */
    private void handleAddItemEnchants(Object player, String data) {
        int slot = 0;
        String enchantsStr = data;

        // Parse "slot|enchant1,enchants2,..." format
        int pipeIdx = data.indexOf('|');
        if (pipeIdx > 0) {
            try {
                slot = Integer.parseInt(data.substring(0, pipeIdx));
                enchantsStr = data.substring(pipeIdx + 1);
            } catch (NumberFormatException e) {
                // No slot prefix, treat entire data as enchant names
            }
        }

        if (enchantsStr.isEmpty()) {
            enchantsStr = "NONE";
        }

        // Create a key that identifies this specific item slot for this player
        String key = getPlayerItemKey(player, slot);
        java.util.Set<String> enchants = playerItemEnchants.computeIfAbsent(key, 
            k -> new java.util.HashSet<>());
        
        // Parse comma-separated enchant names and add them
        String[] parts = enchantsStr.split(",");
        for (String part : parts) {
            String ench = part.trim();
            if (!ench.isEmpty()) {
                enchants.add(ench);
            }
        }

        logEnchantAction(player, "ADD_ITEM_ENCHANTS(slot=" + slot + "):" + enchantsStr);

        if (!com.galvorn.server.protocol.ShadowRealm.SHADOW) {
            com.galvorn.server.protocol.ShadowRealm.log(
                "[Wrapper] ADD_ITEM_ENCHANTS for player item[" + slot + "]: " + enchantsStr);
        }
    }

    /**
     * Get the desired enchantment for a specific player.
     *
     * @param player the player object
     * @return the LOTR enchantment name, or null if not set
     */
    public String getPlayerDesiredEnchantment(Object player) {
        return playerEnchantments.get(player);
    }

    /**
     * Set the desired enchantment for a specific player.
     *
     * @param player the player object
     * @param enchantName the LOTR enchantment name to apply to reforges
     */
    public void setPlayerDesiredEnchantment(Object player, String enchantName) {
        playerEnchantments.put(player, enchantName);
    }

    /**
     * Get the valid enchantments for a specific player's item slot.
     *
     * @param player the player object
     * @param slot the inventory slot (0-35 for main, -1 for any slot)
     * @return the set of valid LOTR enchantment names, or an empty set if not set
     */
    public java.util.Set<String> getPlayerItemEnchantments(Object player, int slot) {
        if (slot >= 0) {
            String key = getPlayerItemKey(player, slot);
            java.util.Set<String> enchants = playerItemEnchants.get(key);
            return enchants != null ? new java.util.HashSet<>(enchants) : java.util.Collections.emptySet();
        }
        // Return all enchants for all slots of this player
        java.util.Set<String> all = new java.util.HashSet<>();
        for (java.util.Map.Entry<String, java.util.Set<String>> entry : playerItemEnchants.entrySet()) {
            if (entry.getKey().startsWith(getPlayerId(player) + ":")) {
                all.addAll(entry.getValue());
            }
        }
        return all;
    }

    /**
     * Set (or replace) the valid enchantments for a specific player's item slot.
     *
     * @param player the player object
     * @param slot the inventory slot
     * @param enchantNames comma-separated enchantment names
     */
    public void setPlayerItemEnchantments(Object player, int slot, String enchantNames) {
        String key = getPlayerItemKey(player, slot);
        java.util.Set<String> enchants = new java.util.HashSet<>();
        if (!enchantNames.isEmpty()) {
            String[] parts = enchantNames.split(",");
            for (String part : parts) {
                String ench = part.trim();
                if (!ench.isEmpty()) {
                    enchants.add(ench);
                }
            }
        }
        playerItemEnchants.put(key, enchants);
    }

    /**
     * Get all item enchantments for a player.
     */
    public java.util.Map<String, java.util.Set<String>> getPlayerAllItemEnchantments(Object player) {
        java.util.Map<String, java.util.Set<String>> result = new java.util.HashMap<>();
        String prefix = getPlayerId(player) + ":";
        for (java.util.Map.Entry<String, java.util.Set<String>> entry : playerItemEnchants.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.put(entry.getKey(), new java.util.HashSet<>(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * Get the desired enchantment for all players.
     * Useful for debugging.
     */
    public java.util.Map<Object, String> getAllPlayerEnchantments() {
        return new java.util.HashMap<>(playerEnchantments);
    }

    /**
     * Get all item enchantments across all players.
     */
    public java.util.Map<String, java.util.Set<String>> getAllItemEnchantments() {
        return new java.util.HashMap<>(playerItemEnchants);
    }

    /**
     * Get the enchantment action log for a player.
     */
    public java.util.List<String> getPlayerEnchantLog(Object player) {
        return playerEnchantLog.getOrDefault(player, java.util.Collections.emptyList());
    }

    private void send(Object player, String resp) {
        try {
            byte[] data = resp.getBytes("UTF-8");
            byte[] frame = new byte[1 + data.length];
            frame[0] = 0x00; // same command class for responses
            System.arraycopy(data, 0, frame, 1, data.length);
            responseChannel.writeAndFlush(frame);
        } catch (Exception e) {
            if (!com.galvorn.server.protocol.ShadowRealm.SHADOW) {
                com.galvorn.server.protocol.ShadowRealm.log("[Wrapper] send() failed: " + e);
            }
        }
    }

    /**
     * Build a key for the item enchantments map.
     */
    private String getPlayerItemKey(Object player, int slot) {
        return getPlayerId(player) + ":" + slot;
    }

    /**
     * Get a string ID for the player object.
     */
    private String getPlayerId(Object player) {
        // Use the player's toString or a unique identifier
        // In a real implementation this might use a field from the player object
        return player.toString();
    }

    /**
     * Log an enchantment action for a player.
     */
    private void logEnchantAction(Object player, String action) {
        playerEnchantLog.computeIfAbsent(player, k -> new java.util.ArrayList<>())
            .add(action);
    }
    
    public boolean authenticate(String user, String key) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((SERVER_KEY + user).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 32).toUpperCase().equals(key);
        } catch (Exception e) { return false; }
    }
}
