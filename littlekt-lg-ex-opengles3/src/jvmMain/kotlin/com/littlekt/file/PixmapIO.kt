package com.littlekt.file

import com.littlekt.Releasable
import com.littlekt.graphics.Pixmap
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/** Writes Pixmaps to various formats.
 * @author mzechner
 * @author Nathan Sweet
 */
object PixmapIO {
    /** Writes the pixmap as a PNG. See [PNG] to write out multiple PNGs with minimal allocation.
     * @param compression sets the deflate compression level. Default is [Deflater.DEFAULT_COMPRESSION]
     * @param flipY flips the Pixmap vertically if true
     */
    /** Writes the pixmap as a PNG with compression. See [PNG] to configure the compression level, more efficiently flip the
     * pixmap vertically, and to write out multiple PNGs with minimal allocation.  */
    @JvmOverloads
    fun writePNG(
        file: File,
        pixmap: Pixmap,
        format: Pixmap.Format,
        compression: Int = Deflater.DEFAULT_COMPRESSION,
        flipY: Boolean = false
    ) {
        try {
            val writer =
                PNG((pixmap.width * pixmap.height * 1.5f).toInt()) // Guess at deflated size.
            try {
                writer.setFlipY(flipY)
                writer.setCompression(compression)
                writer.write(file, pixmap, format)
            } finally {
                writer.release()
            }
        } catch (ex: IOException) {
            throw RuntimeException("Error writing PNG: $file", ex)
        }
    }

    const val GDX2D_FORMAT_ALPHA: Int = 1
    const val GDX2D_FORMAT_LUMINANCE_ALPHA: Int = 2
    const val GDX2D_FORMAT_RGB888: Int = 3
    const val GDX2D_FORMAT_RGBA8888: Int = 4
    const val GDX2D_FORMAT_RGB565: Int = 5
    const val GDX2D_FORMAT_RGBA4444: Int = 6

    const val GDX2D_SCALE_NEAREST: Int = 0
    const val GDX2D_SCALE_LINEAR: Int = 1

    const val GDX2D_BLEND_NONE: Int = 0
    const val GDX2D_BLEND_SRC_OVER: Int = 1

    fun toGdx2DPixmapFormat(format: Pixmap.Format): Int {
        if (format == Pixmap.Format.ALPHA) return GDX2D_FORMAT_ALPHA
        if (format == Pixmap.Format.INTENSITY) return GDX2D_FORMAT_ALPHA
        if (format == Pixmap.Format.LUMINANCE_ALPHA) return GDX2D_FORMAT_LUMINANCE_ALPHA
        if (format == Pixmap.Format.RGB565) return GDX2D_FORMAT_RGB565
        if (format == Pixmap.Format.RGBA4444) return GDX2D_FORMAT_RGBA4444
        if (format == Pixmap.Format.RGB8888) return GDX2D_FORMAT_RGB888
        if (format == Pixmap.Format.RGBA8888) return GDX2D_FORMAT_RGBA8888
        throw RuntimeException("Unknown Format: $format")
    }

    /** @author mzechner
     */
    private object CIM {
        private const val BUFFER_SIZE = 32000
        private val writeBuffer = ByteArray(BUFFER_SIZE)
        private val readBuffer = ByteArray(BUFFER_SIZE)

        fun write(file: File, pixmap: Pixmap, format: Pixmap.Format) {
            var out: DataOutputStream? = null

            try {
                val deflaterOutputStream = DeflaterOutputStream(FileOutputStream(file))
                out = DataOutputStream(deflaterOutputStream)
                out.writeInt(pixmap.width)
                out.writeInt(pixmap.height)
                out.writeInt(toGdx2DPixmapFormat(format))

                val pixelBuf = pixmap.pixels
                val buffer: ByteBuffer = (pixelBuf as ByteBufferImpl).buffer
                buffer.position(0)
                buffer.limit(buffer.capacity())

                val remainingBytes = buffer.capacity() % BUFFER_SIZE
                val iterations = buffer.capacity() / BUFFER_SIZE

                synchronized(writeBuffer) {
                    for (i in 0 until iterations) {
                        buffer.get(writeBuffer)
                        out.write(writeBuffer)
                    }
                    buffer.get(writeBuffer, 0, remainingBytes)
                    out.write(writeBuffer, 0, remainingBytes)
                }

                buffer.position(0)
                buffer.limit(buffer.capacity())
            } catch (e: Exception) {
                throw RuntimeException("Couldn't write Pixmap to file '$file'", e)
            } finally {
                try {
                    checkNotNull(out)
                    out.close()
                } catch (e: IOException) {
                }
            }
        }
    }

    /** PNG encoder with compression. An instance can be reused to encode multiple PNGs with minimal allocation.
     *
     * <pre>
     * Copyright (c) 2007 Matthias Mann - www.matthiasmann.de
     * Copyright (c) 2014 Nathan Sweet
     *
     * Permission is hereby granted, free of charge, to any person obtaining a copy
     * of this software and associated documentation files (the "Software"), to deal
     * in the Software without restriction, including without limitation the rights
     * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
     * copies of the Software, and to permit persons to whom the Software is
     * furnished to do so, subject to the following conditions:
     *
     * The above copyright notice and this permission notice shall be included in
     * all copies or substantial portions of the Software.
     *
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
     * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
     * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
     * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
     * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
     * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
     * THE SOFTWARE.
    </pre> *
     *
     * @author Matthias Mann
     * @author Nathan Sweet
     */
    class PNG @JvmOverloads constructor(initialBufferSize: Int = 128 * 128) : Releasable {
        private val buffer = ChunkBuffer(initialBufferSize)
        private val deflater = Deflater()
        private var lineOutBytes: GdxByteArray? = null
        private var curLineBytes: GdxByteArray? = null
        private var prevLineBytes: GdxByteArray? = null
        private var flipY = true
        private var lastLineLen = 0

        /** If true, the resulting PNG is flipped vertically. Default is true.  */
        fun setFlipY(flipY: Boolean) {
            this.flipY = flipY
        }

        /** Sets the deflate compression level. Default is [Deflater.DEFAULT_COMPRESSION].  */
        fun setCompression(level: Int) {
            deflater.setLevel(level)
        }

        @Throws(IOException::class)
        fun write(file: File?, pixmap: Pixmap, format: Pixmap.Format) {
            val output: OutputStream = FileOutputStream(file)
            try {
                write(output, pixmap, format)
            } finally {
                output.flush()
                output.close()
            }
        }

        /** Writes the pixmap to the stream without closing the stream.  */
        @Throws(IOException::class)
        fun write(output: OutputStream, pixmap: Pixmap, format: Pixmap.Format) {
            val deflaterOutput = DeflaterOutputStream(buffer, deflater)
            val dataOutput = DataOutputStream(output)
            dataOutput.write(SIGNATURE)

            buffer.writeInt(IHDR)
            buffer.writeInt(pixmap.width)
            buffer.writeInt(pixmap.height)
            buffer.writeByte(8) // 8 bits per component.
            buffer.writeByte(COLOR_ARGB.toInt())
            buffer.writeByte(COMPRESSION_DEFLATE.toInt())
            buffer.writeByte(FILTER_NONE.toInt())
            buffer.writeByte(INTERLACE_NONE.toInt())
            buffer.endChunk(dataOutput)

            buffer.writeInt(IDAT)
            deflater.reset()

            val lineLen = pixmap.width * 4
            val lineOut: ByteArray
            var curLine: ByteArray
            var prevLine: ByteArray
            if (lineOutBytes == null) {
                lineOut = GdxByteArray(lineLen).also { lineOutBytes = it }.items
                curLine = GdxByteArray(lineLen).also { curLineBytes = it }.items
                prevLine = GdxByteArray(lineLen).also { prevLineBytes = it }.items
            } else {
                lineOut = lineOutBytes!!.ensureCapacity(lineLen)
                curLine = curLineBytes!!.ensureCapacity(lineLen)
                prevLine = prevLineBytes!!.ensureCapacity(lineLen)
                var i = 0
                val n = lastLineLen
                while (i < n) {
                    prevLine[i] = 0
                    i++
                }
            }
            lastLineLen = lineLen

            val pixels = pixmap.pixels
            val pixelBuf: ByteBuffer = (pixels as ByteBufferImpl).buffer
            val oldPosition = pixelBuf.position()
            val rgba8888 = format == Pixmap.Format.RGBA8888
            var y = 0
            val h = pixmap.height
            while (y < h) {
                val py = if (flipY) (h - y - 1) else y
                if (rgba8888) {
                    pixelBuf.position(py * lineLen)
                    pixelBuf.get(curLine, 0, lineLen)
                } else {
                    var px = 0
                    var x = 0
                    while (px < pixmap.width) {
                        val pixel = pixmap.get(px, py, false)
                        curLine[x++] = ((pixel shr 24) and 0xff).toByte()
                        curLine[x++] = ((pixel shr 16) and 0xff).toByte()
                        curLine[x++] = ((pixel shr 8) and 0xff).toByte()
                        curLine[x++] = (pixel and 0xff).toByte()
                        px++
                    }
                }

                lineOut[0] = (curLine[0] - prevLine[0]).toByte()
                lineOut[1] = (curLine[1] - prevLine[1]).toByte()
                lineOut[2] = (curLine[2] - prevLine[2]).toByte()
                lineOut[3] = (curLine[3] - prevLine[3]).toByte()

                for (x in 4 until lineLen) {
                    val a = curLine[x - 4].toInt() and 0xff
                    val b = prevLine[x].toInt() and 0xff
                    var c = prevLine[x - 4].toInt() and 0xff
                    val p = a + b - c
                    var pa = p - a
                    if (pa < 0) pa = -pa
                    var pb = p - b
                    if (pb < 0) pb = -pb
                    var pc = p - c
                    if (pc < 0) pc = -pc
                    if (pa <= pb && pa <= pc) c = a
                    else if (pb <= pc) //
                        c = b
                    lineOut[x] = (curLine[x] - c).toByte()
                }

                deflaterOutput.write(PAETH.toInt())
                deflaterOutput.write(lineOut, 0, lineLen)

                val temp = curLine
                curLine = prevLine
                prevLine = temp
                y++
            }
            pixelBuf.position(oldPosition)
            deflaterOutput.finish()
            buffer.endChunk(dataOutput)

            buffer.writeInt(IEND)
            buffer.endChunk(dataOutput)

            output.flush()
        }

        /** Disposal will happen automatically in finalize() but can be done explicitly if desired.  */
        override fun release() {
            deflater.end()
        }

        internal class ChunkBuffer private constructor(
            val buffer: ByteArrayOutputStream,
            val crc: CRC32
        ) : DataOutputStream(
            CheckedOutputStream(
                buffer, crc
            )
        ) {
            constructor(initialSize: Int) : this(ByteArrayOutputStream(initialSize), CRC32())

            @Throws(IOException::class)
            fun endChunk(target: DataOutputStream) {
                flush()
                target.writeInt(buffer.size() - 4)
                buffer.writeTo(target)
                target.writeInt(crc.value.toInt())
                buffer.reset()
                crc.reset()
            }
        }

        companion object {
            private val SIGNATURE = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
            private const val IHDR = 0x49484452
            private const val IDAT = 0x49444154
            private const val IEND = 0x49454E44
            private const val COLOR_ARGB: Byte = 6
            private const val COMPRESSION_DEFLATE: Byte = 0
            private const val FILTER_NONE: Byte = 0
            private const val INTERLACE_NONE: Byte = 0
            private const val PAETH: Byte = 4
        }
    }
}