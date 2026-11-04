package com.galvorn.server.ipc;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Byte-for-byte mirror of
 * {@code crates/galvorn-protocol/src/codec/varint.rs}.
 *
 * <p>Java has no unsigned 32-bit type, so the on-the-wire encoding
 * treats the value as {@code i32}-bits-as-{@code u32} (negative
 * numbers occupy the full 5 bytes). The Rust side does the exact same
 * {@code value as u32} cast — that's why VarIntTest pins the bit
 * patterns for {@code -1} and {@code Integer.MIN_VALUE}.
 */
public final class VarInt {

    private VarInt() {}

    /** Max bytes a 32-bit VarInt can occupy on the wire. */
    public static final int MAX_BYTES = 5;

    /** Write {@code value} to {@code out} as a Minecraft VarInt. */
    public static void write(OutputStream out, int value) throws IOException {
        int v = value;
        while (true) {
            int bits = v & 0x7F;
            v >>>= 7;
            if (v == 0) {
                out.write(bits);
                return;
            }
            out.write(bits | 0x80);
        }
    }

    /**
     * Read a Minecraft VarInt from {@code in}.
     *
     * @throws EOFException     stream ended mid-VarInt
     * @throws IOException      VarInt body exceeded {@link #MAX_BYTES}
     */
    public static int read(InputStream in) throws IOException {
        int value = 0;
        int shift = 0;
        for (int i = 0; i < MAX_BYTES; i++) {
            int b = in.read();
            if (b < 0) {
                throw new EOFException("end of stream while reading VarInt at byte " + i);
            }
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
            shift += 7;
        }
        throw new IOException("VarInt longer than " + MAX_BYTES + " bytes");
    }

    /** How many bytes {@code value} would occupy on the wire. */
    public static int sizeOf(int value) {
        int v = value;
        int n = 1;
        while ((v & ~0x7F) != 0) {
            v >>>= 7;
            n++;
        }
        return n;
    }
}
