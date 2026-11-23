package com.ngstudio.maze.core

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Compact binary level pack (assets/levels/levels.bin and daily.bin).
 *
 * File:   "SGLV" | version u8 | count u16 | offsets u32[count] (from file start) | level records
 * Record: w u8 | h u8 | start u16 | par u8 | difficulty u8 (0..255) | intro u8 (Mechanic.code or 0)
 *         | portalPairs u8 | portal cells u16[2*pairs] | tiles (4 bits each, high nibble first)
 *         | solutionLen u16 | solution (2 bits per move, first move in the high bits)
 */
object LevelCodec {
    private val MAGIC = byteArrayOf('S'.code.toByte(), 'G'.code.toByte(), 'L'.code.toByte(), 'V'.code.toByte())
    const val VERSION = 1

    fun encode(levels: List<Level>): ByteArray {
        val records = levels.map { encodeLevel(it) }
        val headerSize = 4 + 1 + 2 + 4 * levels.size
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)
        data.write(MAGIC)
        data.writeByte(VERSION)
        data.writeShort(levels.size)
        var offset = headerSize
        for (r in records) { data.writeInt(offset); offset += r.size }
        for (r in records) data.write(r)
        data.flush()
        return out.toByteArray()
    }

    private fun encodeLevel(l: Level): ByteArray {
        require(l.width in 1..255 && l.height in 1..255)
        require(l.solution.size <= 0xFFFF)
        val out = ByteArrayOutputStream()
        val d = DataOutputStream(out)
        d.writeByte(l.width); d.writeByte(l.height); d.writeShort(l.start)
        d.writeByte(l.par.coerceIn(0, 255))
        d.writeByte((l.difficulty * 255f).toInt().coerceIn(0, 255))
        d.writeByte(l.intro?.code ?: 0)
        d.writeByte(l.portals.size / 2)
        for (p in l.portals) d.writeShort(p)
        var i = 0
        while (i < l.size) {
            val hi = l.tile(i)
            val lo = if (i + 1 < l.size) l.tile(i + 1) else 0
            d.writeByte((hi shl 4) or lo)
            i += 2
        }
        d.writeShort(l.solution.size)
        var k = 0
        while (k < l.solution.size) {
            var b = 0
            for (j in 0 until 4) {
                val m = if (k + j < l.solution.size) l.solution[k + j].toInt() and 3 else 0
                b = b or (m shl (6 - 2 * j))
            }
            d.writeByte(b)
            k += 4
        }
        d.flush()
        return out.toByteArray()
    }

    /** Random-access reader over the whole pack (kept in memory; ~1 MB). */
    class Pack(private val bytes: ByteArray) {
        val count: Int

        init {
            require(bytes.size >= 7 && bytes.copyOfRange(0, 4).contentEquals(MAGIC)) { "not a level pack" }
            require(bytes[4].toInt() == VERSION) { "unsupported level pack version ${bytes[4]}" }
            count = u16(5)
        }

        private fun u8(p: Int) = bytes[p].toInt() and 0xFF
        private fun u16(p: Int) = (u8(p) shl 8) or u8(p + 1)
        private fun u32(p: Int) = (u16(p) shl 16) or u16(p + 2)

        /** index is 0-based. */
        fun level(index: Int): Level {
            require(index in 0 until count) { "level $index out of range" }
            var p = u32(7 + 4 * index)
            val w = u8(p); val h = u8(p + 1); val start = u16(p + 2)
            val par = u8(p + 4); val diff = u8(p + 5) / 255f; val intro = Mechanic.of(u8(p + 6))
            val pairs = u8(p + 7)
            p += 8
            val portals = IntArray(pairs * 2) { u16(p + 2 * it) }
            p += pairs * 4
            val size = w * h
            val tiles = ByteArray(size)
            for (i in 0 until size) {
                val b = u8(p + i / 2)
                tiles[i] = (if (i % 2 == 0) b ushr 4 else b and 0xF).toByte()
            }
            p += (size + 1) / 2
            val solLen = u16(p); p += 2
            val sol = ByteArray(solLen) { k -> ((u8(p + k / 4) ushr (6 - 2 * (k % 4))) and 3).toByte() }
            return Level(w, h, tiles, start, portals, par, sol, diff, intro)
        }
    }
}
