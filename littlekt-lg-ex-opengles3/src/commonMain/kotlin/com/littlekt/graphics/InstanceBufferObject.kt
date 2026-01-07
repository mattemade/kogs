package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.file.createFloatBuffer
import com.littlekt.graphics.gl.BufferTarget
import com.littlekt.graphics.gl.GlBuffer
import com.littlekt.graphics.gl.GlVertexArray
import com.littlekt.graphics.gl.Usage
import com.littlekt.graphics.shader.ShaderProgram
import kotlin.math.max
import kotlin.math.round

class InstanceBufferObject(
    val gl: GL,
    val isStatic: Boolean,
    val attributes: VertexAttributes,
    var buffer: FloatBuffer,
) : Releasable {
    private val glBuffer: GlBuffer = gl.createBuffer()
    private val vaoGl: GlVertexArray? = if (gl.isG30) gl.createVertexArray() else null
    private val usage = if (isStatic) Usage.STATIC_DRAW else Usage.DYNAMIC_DRAW
    private var bound = false

    var growFactor = 2f
    var grow = false

    val isBound get() = bound
    val numVertices get() = buffer.limit * 4 / attributes.vertexSize
    val maxNumVertices get() = buffer.capacity * 4 / attributes.vertexSize

    constructor(gl: GL, isStatic: Boolean, numVertices: Int, attributes: VertexAttributes) : this(
        gl,
        isStatic,
        attributes,
        createFloatBuffer(attributes.vertexSize / 4 * numVertices)
    )

    init {
        allocBuffer()
        buffer.flip()
    }

    private fun allocBuffer() {
        vaoGl?.let {
            gl.bindVertexArray(it)
        }
        gl.bindBuffer(BufferTarget.ARRAY, glBuffer)
        gl.bufferData(BufferTarget.ARRAY, buffer, usage)
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        vaoGl?.let {
            gl.bindDefaultVertexArray()
        }
    }

    fun setVertices(vertices: FloatArray, srcOffset: Int = 0, count: Int = vertices.size) {
        buffer.clear()
        buffer.position = 0
        checkBufferSizes(count)
        buffer.put(vertices, srcOffset, count)
        buffer.position = 0
        buffer.limit = count
        onBufferChanged()
    }

    fun updateVertices(destOffset: Int, vertices: FloatArray, srcOffset: Int = 0, count: Int = vertices.size) {
        val pos = buffer.position
        buffer.position = destOffset
        checkBufferSizes(count)
        buffer.put(vertices, srcOffset, count)
        buffer.position = pos
        onBufferChanged()
    }

    private fun checkBufferSizes(reqSpace: Int) {
        if (!grow) return
        if (buffer.remaining < reqSpace) {
            increaseBufferSize(
                max(
                    round(buffer.capacity * growFactor).toInt(),
                    (numVertices + reqSpace) * attributes.vertexSize
                )
            )
        }
    }

    private fun increaseBufferSize(newSize: Int) {
        val newData = createFloatBuffer(newSize)
        buffer.flip()
        newData.put(buffer)
        buffer = newData
    }

    fun bind(shader: ShaderProgram<*, *>? = null, locations: IntArray? = null) {
        vaoGl?.let {
            gl.bindVertexArray(it)
        }
        gl.bindBuffer(BufferTarget.ARRAY, glBuffer)
        if (buffer.dirty) {
            gl.bufferSubData(BufferTarget.ARRAY, 0, buffer)
            buffer.dirty = false
        }
        if (shader != null) {
            attributes.forEachIndexed { index, attribute ->
                val location = locations?.get(index) ?: shader.getAttrib(attribute.alias)
                if (location < 0) {
                    return@forEachIndexed
                }
                gl.enableVertexAttribArray(location)
                gl.vertexAttribPointer(
                    location,
                    attribute.numComponents,
                    attribute.type,
                    attribute.normalized,
                    attributes.vertexSize,
                    attribute.offset
                )
            }
        }
        bound = true
    }

    fun unbind(shader: ShaderProgram<*, *>? = null, locations: IntArray? = null) {
        if (shader != null) {
            attributes.forEachIndexed { index, attribute ->
                gl.disableVertexAttribArray(locations?.get(index) ?: shader.getAttrib(attribute.alias))
            }
        }
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        vaoGl?.let {
            gl.bindDefaultVertexArray()
        }
        bound = false
    }

    private fun onBufferChanged() {
        if (bound) {
            gl.bufferSubData(BufferTarget.ARRAY, 0, buffer)
            buffer.dirty = false
        }
    }

    override fun release() {
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        gl.deleteBuffer(glBuffer)
    }

}