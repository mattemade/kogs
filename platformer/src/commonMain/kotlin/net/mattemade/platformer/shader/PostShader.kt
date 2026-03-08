package net.mattemade.platformer.shader

import com.littlekt.Context
import com.littlekt.graphics.shader.FragmentShaderModel
import com.littlekt.graphics.shader.ShaderParameter
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.graphics.shader.VertexShaderModel

object PostShader {

    val program: ShaderProgram<Vertex, Fragment> =
        ShaderProgram(vertexShader = Vertex(), fragmentShader = Fragment())

    private var prepared = false
    fun prepare(context: Context) {
        if (!prepared) {
            prepared = true
            program.prepare(context)
        }
    }

    class Vertex : VertexShaderModel() {
        // language=GLSL
        override var source: String = """
            uniform mat4 u_projTrans;
            
            in vec4 a_position;
            in vec4 a_color;
            in vec2 a_texCoord0;
            
            out vec4 v_color;
            out vec2 v_texCoords;
            
            void main() {
                v_color = a_color;
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
        """.trimIndent()

        val uProjTrans = ShaderParameter.UniformMat4("u_projTrans")
        val aPosition = ShaderParameter.Attribute("a_position")
        val aColor = ShaderParameter.Attribute("a_color")
        val aTexCoord0 = ShaderParameter.Attribute("a_texCoord0")

        override val parameters: LinkedHashSet<ShaderParameter> =
            linkedSetOf(
                uProjTrans, aPosition, aColor, aTexCoord0,
            )
    }

    class Fragment : FragmentShaderModel() {
        // language=GLSL
        override var source: String = """
            uniform sampler2D u_texture;
            
            in vec4 v_color;
            in vec2 v_texCoords;
            
            
            float noise(vec2 co) {
                return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453) * 10.0;
            }
            
            void main() {
                vec4 color = v_color * texture2D(u_texture, v_texCoords);
                float dither = (noise(v_texCoords.xy*1024.0) - 5.0) / 255.0;
                gl_FragColor = vec4(color.r + dither, color.g + dither, color.b + dither, 1.0);//v_color * texture2D(u_texture, v_texCoords);
            }
        """.trimIndent()

        val uTexture = ShaderParameter.UniformSample2D("u_texture")

        override val parameters: LinkedHashSet<ShaderParameter> =
            linkedSetOf(
                uTexture,
            )
    }
}