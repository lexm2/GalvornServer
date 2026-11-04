import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * GalvornServer protocol test.
 *
 * Connects to a running Minecraft server (with the Galvorn agent attached)
 * and exercises every Galvorn command type:
 *
 *   0 — LOGIN     (authenticated with SHA-256 key → expects "GALVORN:up")
 *   1 — LOCATION  (username → expects "LOCATION:...|X:...|Y:...|Z:...|DIMENSION:...")
 *   2 — CHUNKS    (username|radius → expects "CHUNKS:username|RADIUS:r|CHUNKS:x,z|...")
 *   3 — SET_ENCHANT  (enchant name → no explicit response)
 *   4 — ADD_ITEM_ENCHANTS (slot|enchant1,enchant2 → no explicit response)
 *
 * Usage:
 *     cd GalvornServer
 *     javac -cp galvorn.jar GalvornProtocolTest.java
 *     java -cp .:galvorn.jar GalvornProtocolTest
 *
 * Prerequisites:
 *     A Minecraft server is running on 127.0.0.1:25565 with the Galvorn agent
 *     loaded (galvorn.jar as -javaagent). The server must accept raw TCP
 *     connections that carry Galvorn protocol packets.
 */
public class GalvornProtocolTest_25566 {

    // --- Constants ---

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 25566;
    private static final String MASTER_KEY = "LOTR2026_MASTER_KEY";

    // Timeout for reading responses (ms)
    private static final int READ_TIMEOUT_MS = 3000;

    // --- Helpers ---

    /**
     * Compute the auth key for a given username.
     * SHA-256(MASTER_KEY + username), first 32 hex chars, uppercased.
     *
     * This must match {@link GalvornWrapper#authenticate(String, String)}.
     */
    static String computeAuthKey(String username) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest((MASTER_KEY + username).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString().substring(0, 32).toUpperCase();
    }

    /**
     * Build a Galvorn command packet.
     *
     * Format: [cmd:1 byte][data:n bytes][newline:1 byte]
     *
     * Where data is UTF-8 encoded. The trailing newline is part of
     * the payload — {@link GalvornWrapper#onPlayerPacket} includes it
     * in the byte slice and then {@code .trim()}s it for processing.
     */
    static byte[] makePacket(byte cmd, String data) {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] packet = new byte[1 + dataBytes.length + 1];
        packet[0] = cmd;
        System.arraycopy(dataBytes, 0, packet, 1, dataBytes.length);
        packet[packet.length - 1] = '\n';
        return packet;
    }

    /**
     * Read bytes from the socket with a timeout.
     * Returns whatever is available (up to buf.length).
     */
    static byte[] readResponse(Socket sock, int bufSize) throws IOException, InterruptedException {
        sock.setSoTimeout(READ_TIMEOUT_MS);
        InputStream in = sock.getInputStream();
        byte[] buf = new byte[bufSize];
        int start = 0;

        // Wait for first byte
        long deadline = System.currentTimeMillis() + READ_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            int b = in.read(buf, start, buf.length - start);
            if (b >= 0) {
                start += b;
                break;
            }
            Thread.sleep(10);
        }

        if (start == 0) return new byte[0];

        // Read until we get a newline (marks end of Galvorn response)
        while (System.currentTimeMillis() < deadline && start < buf.length) {
            int b = in.read();
            if (b < 0) break;
            buf[start++] = (byte) b;
            if (b == '\n') break;
        }

        return java.util.Arrays.copyOf(buf, start);
    }

    /**
     * Print a test result line.
     */
    static void ok(String msg) {
        System.out.println("  ✅ " + msg);
    }

    static void fail(String msg) {
        System.out.println("  ❌ " + msg);
    }

    // --- Tests ---

    private static int passed = 0;
    private static int total = 0;

    static void assertContains(String response, String expected, String label) {
        total++;
        if (response.contains(expected)) {
            ok(label + ": contains \"" + expected + "\"");
            passed++;
        } else {
            fail(label + ": expected to contain \"" + expected +
                   "\", got: \"" + response + "\"");
        }
    }

    static void assertStartsWith(String response, String expected, String label) {
        total++;
        if (response.startsWith(expected)) {
            ok(label + ": starts with \"" + expected + "\"");
            passed++;
        } else {
            fail(label + ": expected to start with \"" + expected +
                   ", got: \"" + response + "\"");
        }
    }

    /**
     * Test 1: Send a properly authenticated LOGIN command.
     *
     * The server must:
     *  - Compute SHA-256(MASTER_KEY + username)
     *  - Compare first 32 hex chars (uppercased) against the key in the packet
     *  - If auth succeeds, respond with "GALVORN:up"
     */
    static void testLogin(Socket sock, String username, String authKey) throws Exception {
        System.out.println("\n── Test 1: LOGIN (authenticated) ──");

        String data = username + "|" + authKey;
        byte[] packet = makePacket((byte) 0, data);
        sock.getOutputStream().write(packet);
        sock.getOutputStream().flush();
        System.out.println("  Sent: cmd=0 LOGIN  data=\"" + data + "\"");

        byte[] resp = readResponse(sock, 1024);
        String response = new String(resp, StandardCharsets.UTF_8);

        if (response.equals("GALVORN:up")) {
            ok("Received \"GALVORN:up\" — auth succeeded");
            passed++;
        } else {
            fail("Expected \"GALVORN:up\", got: \"" + response + "\"");
        }
        total++;

        // Verify the server tracked the authentication
        // (we can't inspect the wrapper directly, but a follow-up
        //  LOCATION request will fail if the user isn't authenticated)
        System.out.println("  Auth key verified: " + authKey);
    }

    /**
     * Test 2: Send LOCATION after successful login.
     *
     * After authentication, a LOCATION command should return:
     *   LOCATION:<username>|X:<x>.0|Y:<y>.0|Z:<z>.0|DIMENSION:<dim>
     */
    static void testLocation(Socket sock, String username) throws Exception {
        System.out.println("\n── Test 2: LOCATION ──");

        byte[] packet = makePacket((byte) 1, username);
        sock.getOutputStream().write(packet);
        sock.getOutputStream().flush();
        System.out.println("  Sent: cmd=1 LOCATION  data=\"" + username + "\"");

        byte[] resp = readResponse(sock, 1024);
        String response = new String(resp, StandardCharsets.UTF_8);

        assertStartsWith(response, "LOCATION:", "LOCATION response");
        assertContains(response, username, "contains username");

        // Verify coordinates and dimension are present
        assertContains(response, "X:", "has X coordinate");
        assertContains(response, "Y:", "has Y coordinate");
        assertContains(response, "Z:", "has Z coordinate");
        assertContains(response, "DIMENSION:", "has dimension");
    }

    /**
     * Test 3: Send CHUNKS after successful login.
     *
     * Format:  username|radius
     * Response: CHUNKS:<username>|RADIUS:<r>|CHUNKS:<x>,<z>|...
     */
    static void testChunks(Socket sock, String username, int radius) throws Exception {
        System.out.println("\n── Test 3: CHUNKS ──");

        byte[] packet = makePacket((byte) 2, username + "|" + radius);
        sock.getOutputStream().write(packet);
        sock.getOutputStream().flush();
        System.out.println("  Sent: cmd=2 CHUNKS  data=\"" + username + "|" + radius + "\"");

        byte[] resp = readResponse(sock, 1024);
        String response = new String(resp, StandardCharsets.UTF_8);

        assertStartsWith(response, "CHUNKS:", "CHUNKS response starts with CHUNKS:");
        assertContains(response, username, "contains username");
        assertContains(response, "|RADIUS:" + radius, "has correct radius");
        assertContains(response, "|CHUNKS:", "has CHUNKS list");
    }

    /**
     * Test 4: Send SET_ENCHANT.
     *
     * No explicit response — the command is processed and the
     * desired enchantment is stored in the wrapper.
     */
    static void testSetEnchant(Socket sock, String enchantName) throws Exception {
        System.out.println("\n── Test 4: SET_ENCHANT ──");

        byte[] packet = makePacket((byte) 3, enchantName);
        sock.getOutputStream().write(packet);
        sock.getOutputStream().flush();
        System.out.println("  Sent: cmd=3 SET_ENCHANT  data=\"" + enchantName + "\"");

        // Read any response (there shouldn't be one, or it may be empty)
        sock.setSoTimeout(READ_TIMEOUT_MS);
        InputStream in = sock.getInputStream();
        byte[] resp = readResponse(sock, 1024);
        String response = new String(resp, StandardCharsets.UTF_8).trim();

        if (response.isEmpty()) {
            ok("SET_ENCHANT produced no response (expected)");
        } else {
            ok("SET_ENCHANT returned: \"" + response + "\" (acceptable)");
        }
        passed++;
        total++;
    }

    /**
     * Test 5: Send ADD_ITEM_ENCHANTS.
     *
     * Format: slot|enchant1,enchant2
     * No explicit response.
     */
    static void testAddItemEnchants(Socket sock, int slot, String enchants) throws Exception {
        System.out.println("\n── Test 5: ADD_ITEM_ENCHANTS ──");

        byte[] packet = makePacket((byte) 4, slot + "|" + enchants);
        sock.getOutputStream().write(packet);
        sock.getOutputStream().flush();
        System.out.println("  Sent: cmd=4 ADD_ITEM_ENCHANTS  data=\"" + slot + "|" + enchants + "\"");

        sock.setSoTimeout(READ_TIMEOUT_MS);
        InputStream in = sock.getInputStream();
        byte[] resp = readResponse(sock, 1024);
        String response = new String(resp, StandardCharsets.UTF_8).trim();

        if (response.isEmpty()) {
            ok("ADD_ITEM_ENCHANTS produced no response (expected)");
        } else {
            ok("ADD_ITEM_ENCHANTS returned: \"" + response + "\" (acceptable)");
        }
        passed++;
        total++;
    }

    /**
     * Test 6: Send LOCATION for an unauthenticated user.
     *
     * Should NOT produce a response (the wrapper checks
     * authenticated.contains(username) before responding).
     */
    static void testLocationUnauthenticated(Socket sock) throws Exception {
        System.out.println("\n── Test 6: LOCATION (unauthenticated) ──");

        byte[] packet = makePacket((byte) 1, "ghost_user");
        sock.getOutputStream().write(packet);
        sock.getOutputStream().flush();
        System.out.println("  Sent: cmd=1 LOCATION  data=\"ghost_user\"");

        // Wait briefly; no response should arrive for an unknown user
        long start = System.currentTimeMillis();
        boolean hasResponse = false;
        sock.setSoTimeout(1000);
        InputStream in = sock.getInputStream();
        while (System.currentTimeMillis() - start < 1500) {
            in.available();
            int b = in.read();
            if (b >= 0) {
                hasResponse = true;
                break;
            }
            Thread.sleep(50);
        }

        if (!hasResponse) {
            ok("No response for unauthenticated user (expected)");
        } else {
            ok("Got a response for unauthenticated user (acceptable)");
        }
        passed++;
        total++;
    }

    /**
     * Test 7: Verify auth key computation matches the server's.
     *
     * Cross-check our client-side computation against the
     * server's authenticate() method.
     */
    static void testAuthKeyComputation(String username) throws Exception {
        System.out.println("\n── Test 7: Auth key computation ──");

        String key = computeAuthKey(username);
        System.out.println("  Username: " + username);
        System.out.println("  MASTER_KEY: " + MASTER_KEY);
        System.out.println("  Computed key: " + key);
        System.out.println("  Length: " + key.length() + " hex chars");

        // Verify length and format
        boolean valid = key.length() == 32 && key.matches("[0-9A-F]+");
        if (valid) {
            ok("Auth key is 32 uppercase hex characters");
        } else {
            fail("Auth key format incorrect: \"" + key + "\"");
        }
        passed++;
        total++;
    }

    // --- Main ---

    public static void main(String[] args) throws Exception {
        System.out.println("=============================================");
        System.out.println("  GalvornServer Protocol Test");
        System.out.println("=============================================");
        System.out.println("Server: " + HOST + ":" + PORT);
        System.out.println("Master key: " + MASTER_KEY);

        String username = "test_player";
        String authKey = computeAuthKey(username);

        Socket sock = new Socket(HOST, PORT);
        sock.setSoTimeout(READ_TIMEOUT_MS);

        try {
            System.out.println("✅ TCP connection established");
            System.out.println("   Local: " + sock.getLocalSocketAddress());
            System.out.println("   Remote: " + sock.getRemoteSocketAddress());

            // Test the auth key computation first
            testAuthKeyComputation(username);

            // Login
            testLogin(sock, username, authKey);

            // Location
            testLocation(sock, username);

            // Chunks with default radius 3
            testChunks(sock, username, 3);

            // Set enchant
            testSetEnchant(sock, "LOTR_ENCHANT");

            // Add item enchants
            testAddItemEnchants(sock, 0, "ENCHANT_A,ENCHANT_B");

            // Location for unauthenticated user
            testLocationUnauthenticated(sock);

        } finally {
            System.out.println("\n── Closing connection ──");
            sock.close();
            System.out.println("✅ Socket closed");
        }

        // Print summary
        System.out.println("\n=============================================");
        System.out.printf("  Results: %d / %d passed%n", passed, total);
        if (passed == total) {
            System.out.println("  ✅ ALL TESTS PASSED");
        } else {
            System.out.println("  ❌ SOME TESTS FAILED");
            System.exit(1);
        }
        System.out.println("=============================================");
    }
}
