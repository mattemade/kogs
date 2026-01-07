package com.littlekt.file

import com.littlekt.Context
import com.littlekt.log.Logger
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class WebVfs(context: Context, logger: Logger, assetsBaseDir: String) :
    Vfs(context, logger, assetsBaseDir) {

    companion object {
        internal suspend fun <T> loadRaw(
            job: Job,
            url: String,
            processRawData: (ArrayBuffer) -> T,
            onError: (Event) -> Unit
        ): T? {
            val data = CompletableDeferred<T?>(job)
            val req = XMLHttpRequest()
            req.responseType = XMLHttpRequestResponseType.ARRAYBUFFER
            req.onload = {
                data.complete(processRawData(req.response as ArrayBuffer))
            }
            req.onerror = {
                data.complete(null)
                onError(it)
            }
            req.open("GET", url)
            req.send()

            return data.await()
        }
    }

    override suspend fun loadRawAsset(rawRef: RawAssetRef) =
        LoadedRawAsset(rawRef, loadRaw(rawRef.url))

    override suspend fun loadSequenceStreamAsset(
        sequenceRef: SequenceAssetRef
    ): SequenceStreamCreatedAsset {
        val buffer = loadRaw(sequenceRef.url)
        val stream = if (buffer != null) JsByteSequenceStream(buffer) else null
        return SequenceStreamCreatedAsset(sequenceRef, stream)
    }

    private suspend fun loadRaw(url: String): ByteBuffer? =
        Companion.loadRaw(
            job = job,
            url = url,
            processRawData = { data -> ByteBufferImpl(Uint8Array(data)) },
            onError = { event -> logger.error { "Failed loading resource $url: $event" } })

    override fun store(key: String, data: ByteArray): Boolean {
        return try {
            // base64 will ignore all the trailing zeros in the data array, so we need to keep the size
            store("$key.size", data.size.toString())

            val binToBase64 = binToBase64(Uint8Array(data.toTypedArray()))
            localStorage[key] = binToBase64
            true
        } catch (e: Exception) {
            logger.error { "Failed storing data '$key' to localStorage: $e" }
            false
        }
    }

    override fun store(key: String, data: String): Boolean {
        return try {
            localStorage[key] = data
            true
        } catch (e: Exception) {
            logger.error { "Failed storing string '$key' to localStorage: $e" }
            false
        }
    }

    override fun load(key: String): ByteBuffer? {
        return localStorage[key]?.let {
            println("loaded ${it.length}")
            println("loaded $it")
            val size = loadString("$key.size")?.toInt()
            val array = base64ToBin(it, size)
            println("loaded binary ${array.length}")
            ByteBufferImpl(array)
        }
    }

    override fun loadString(key: String): String? {
        return localStorage[key]
    }

    private val base64abc =
        arrayOf(
            "A",
            "B",
            "C",
            "D",
            "E",
            "F",
            "G",
            "H",
            "I",
            "J",
            "K",
            "L",
            "M",
            "N",
            "O",
            "P",
            "Q",
            "R",
            "S",
            "T",
            "U",
            "V",
            "W",
            "X",
            "Y",
            "Z",
            "a",
            "b",
            "c",
            "d",
            "e",
            "f",
            "g",
            "h",
            "i",
            "j",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t",
            "u",
            "v",
            "w",
            "x",
            "y",
            "z",
            "0",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "+",
            "/"
        )

    private fun binToBase64(data: Uint8Array): String {
        var result = ""
        val l = data.length
        var j = 2
        for (i in 2 until l step 3) {
            j = i
            result += base64abc[data[i - 2].toInt() shr 2]
            result += base64abc[data[i - 2].toInt() and 0x03 shl 4 or (data[i - 1].toInt() shr 4)]
            result += base64abc[data[i - 1].toInt() and 0x0F shl 2 or (data[i].toInt() shr 6)]
            result += base64abc[data[i].toInt() and 0x3F]
        }
        if (j == l + 1) { // 1 octet yet to write
            result += base64abc[data[j - 2].toInt() shr 2]
            result += base64abc[data[j - 2].toInt() and 0x03 shl 4]
            result += "=="
        }
        if (j == l) { // 2 octets yet to write
            result += base64abc[data[j - 2].toInt() shr 2]
            result += base64abc[data[j - 2].toInt() and 0x03 shl 4 or (data[j - 1].toInt() shr 4)]
            result += base64abc[data[j - 1].toInt() and 0x0F shl 2]
            result += "="
        }
        return result
    }

    private fun base64ToBin(base64: String, size: Int?): Uint8Array {
        val binaryString = window.atob(base64)
        val bytes = Uint8Array(size ?: binaryString.length)
        for (i in binaryString.indices) {
            bytes[i] = binaryString[i].code.toByte()//.digitToInt().toByte()
        }
        return bytes
    }
}
