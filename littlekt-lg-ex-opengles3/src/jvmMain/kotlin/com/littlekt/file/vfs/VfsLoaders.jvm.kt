package com.littlekt.file.vfs

import com.littlekt.LwjglContext
import com.littlekt.async.onRenderingThread
import com.littlekt.audio.AudioClip
import com.littlekt.audio.AudioClipEx
import com.littlekt.audio.AudioStream
import com.littlekt.audio.AudioStreamEx
import com.littlekt.audio.OpenALAudioClip
import com.littlekt.audio.OpenALAudioStream
import com.littlekt.file.ImageUtils
import com.littlekt.file.JvmByteSequenceStream
import com.littlekt.file.PixmapIO
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.Texture
import com.littlekt.graphics.gl.PixmapTextureData
import com.littlekt.graphics.gl.TexMagFilter
import com.littlekt.graphics.gl.TexMinFilter
import com.littlekt.graphics.gl.TextureFormat
import com.littlekt.log.Logger
import fr.delthas.javamp3.Sound
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import javax.sound.sampled.AudioSystem

private val logger = Logger("VfsLoaders")
private val imageIoLock = Any()

/**
 * Loads an image from the path as a [Texture]. This will call [Texture.prepare] before returning!
 * @return the loaded texture
 */
actual suspend fun VfsFile.readTexture(minFilter: TexMinFilter, magFilter: TexMagFilter, mipmaps: Boolean): Texture {
    val data = PixmapTextureData(readPixmap(), mipmaps)

    return Texture(data).also {
        it.minFilter = minFilter
        it.magFilter = magFilter
        vfs.context as LwjglContext
        onRenderingThread {
            it.prepare(vfs.context)
        }
    }
}

/**
 * Reads Base64 encoded ByteArray for embedded images.
 */
internal actual suspend fun ByteArray.readPixmap(): Pixmap {
    return readPixmap(this)
}

actual suspend fun VfsFile.readPixmap(): Pixmap {
    val bytes = readBytes()
    return readPixmap(bytes)
}

private fun readPixmap(bytes: ByteArray): Pixmap {
    // ImageIO.read is not thread safe!
    val img = synchronized(imageIoLock) {
        ImageIO.read(ByteArrayInputStream(bytes))
    }
    val result =
        ImageUtils.bufferedImageToBuffer(img, TextureFormat.RGBA, img.width, img.height)
    return Pixmap(
        img.width,
        img.height,
        result.buffer
    ).apply {
        glFormat = result.format
    }
}

/**
 * Loads audio from the path as an [AudioClip].
 *
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClip(): AudioClip = readAudioClipEx()

/**
 * Loads audio from the path as an [AudioClipEx].
 *
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClipEx(): AudioClipEx {
    if (true) {
        // sound does not really work in JVM :/
        vfs.context as LwjglContext
        return OpenALAudioClip(vfs.context.audioContext, ByteArray(0), 1, 1)
    }
    try {
        val asset = read()
        // TODO refactor the sound handling to check the actual file headers
        val (source, channels, sampleRate) =
            if (pathInfo.extension == "mp3") {
                val decoder = kotlin.run { Sound(ByteArrayInputStream(asset.toArray())) }
                val source = decoder.readBytes().also { run { decoder.close() } }
                val channels = if (decoder.isStereo) 2 else 1
                Triple(source, channels, decoder.samplingFrequency.toFloat())
            } else {
                val source = asset.toArray()
                val clip =
                    run { AudioSystem.getAudioFileFormat(ByteArrayInputStream(asset.toArray())) }
                Triple(source, clip.format.channels, clip.format.sampleRate)
            }

        vfs.context as LwjglContext
        return OpenALAudioClip(vfs.context.audioContext, source, channels, sampleRate.toInt())
    } catch (e: Exception) {
        vfs.context as LwjglContext
        return OpenALAudioClip(vfs.context.audioContext, ByteArray(0), 1, 1)
    }
}

/**
 * Streams audio from the path as an [AudioStream].
 * @return a new [AudioStream]
 */
actual suspend fun VfsFile.readAudioStream(): AudioStream = readAudioStreamEx()

private suspend fun VfsFile.createAudioStreamMp3(): OpenALAudioStream {
    vfs.context as LwjglContext
    var decoder = runCatching { Sound((readStream() as JvmByteSequenceStream).stream) }.getOrThrow()
    val channels = if (decoder.isStereo) 2 else 1
    val read: (ByteArray) -> Int = {
        decoder.read(it)
    }
    val reset: suspend () -> Unit = {
        runCatching {
            decoder.close()
            decoder = Sound((readStream() as JvmByteSequenceStream).stream)
        }.getOrThrow()
    }

    return OpenALAudioStream(vfs.context.audioContext, read, reset, channels, decoder.samplingFrequency)
}

private suspend fun VfsFile.createAudioStreamWav(): OpenALAudioStream {
    vfs.context as LwjglContext
    var clip =
        runCatching { AudioSystem.getAudioInputStream((readStream() as JvmByteSequenceStream).stream) }.getOrThrow()
    val read: (ByteArray) -> Int = {
        val result = clip.read(it)
        result
    }
    val reset: suspend () -> Unit = {
        runCatching {
            clip.close()
            clip = AudioSystem.getAudioInputStream((readStream() as JvmByteSequenceStream).stream)
        }.getOrThrow()
    }

    return OpenALAudioStream(
        vfs.context.audioContext,
        read,
        reset,
        clip.format.channels,
        clip.format.sampleRate.toInt()
    )
}

/**
 * Streams audio from the path as an [AudioStreamEx].
 *
 * @return a new [AudioStreamEx]
 */
actual suspend fun VfsFile.readAudioStreamEx(): AudioStreamEx {
    if (
        pathInfo.extension == "mp3"
    ) { // TODO refactor the sound handling to check the actual file headers
        return createAudioStreamMp3()
    }

    return createAudioStreamWav()
}

actual suspend fun VfsFile.writePixmap(pixmap: Pixmap) {
    // TODO write to an actual png vs bmp - currently bmps write bottom to top while pixmaps pixels are top to bottom
    /*val buffer = pixmap.pixels
    val width = pixmap.width
    val height = pixmap.height
    val bmpBuffer = createByteBuffer(54 + buffer.capacity)
    bmpBuffer.run {
        putByte('B'.code.toByte())
        putByte('M'.code.toByte())
        putUInt(54 + width * height * 4) // size
        putUShort(0) // res1
        putUShort(0) // res2
        putUInt(54) // offset
        putUInt(40) // biSize
        putUInt(width)
        putUInt(height)
        putUShort(1.toShort()) // planes
        putUShort((8 * 4).toShort()) // bitCount
        putUInt(0) // compression
        putUInt(width * height * 4) // image size bytes
        putUInt(0) // x pixels per meter
        putUInt(0) // y pixels per meter
        putUInt(0) // colors used
        putUInt(0) // important colors
        putByte(buffer.toArray(), 0, buffer.capacity)
    }
    runCatching {
        FileOutputStream(File(path)).use { it.write(bmpBuffer.toArray()) }
    }.getOrThrow()*/

    val file = File(path)
    PixmapIO.writePNG(file, pixmap, Pixmap.Format.RGBA8888)
}

actual fun VfsFile.readAudioClipExDeferred(callback: (AudioClipEx) -> Unit) {
    TODO()
}

actual fun VfsFile.readPixmapDeferred(callback: (Pixmap) -> Unit) {
    TODO()
}

actual fun VfsFile.readTextureDeferred(
    minFilter: TexMinFilter,
    magFilter: TexMagFilter,
    mipmaps: Boolean,
    callback: (Texture) -> Unit
) {
    TODO()
}