package net.mattemade.utils.msdf

import com.littlekt.Context
import com.littlekt.graphics.shader.FragmentShaderModel
import com.littlekt.graphics.shader.ShaderParameter
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.graphics.shader.VertexShaderModel

object MsdfFontShader {

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
            
            const float distanceRange = 2.0;
            
            float median(vec3 c) {
                return max(min(c.r, c.g), min(max(c.r, c.g), c.b));
            }
            
            void main() {
                vec3 color = texture2D(u_texture, v_texCoords).rgb;
                float distance = median(color) - 0.5; // thickness can be controlled here
                float dxy = fwidth(distance);
                float opacity = smoothstep(-dxy, dxy, distance);
                gl_FragColor = vec4(v_color.rgb, v_color.a * opacity);
            }
        """.trimIndent()

        val uTexture = ShaderParameter.UniformSample2D("u_texture")

        override val parameters: LinkedHashSet<ShaderParameter> =
            linkedSetOf(
                uTexture,
            )
    }
}