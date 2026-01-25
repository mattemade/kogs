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
            
            float screenPxRange() {
                vec2 unitRange = vec2(distanceRange)/vec2(textureSize(u_texture, 0));
                vec2 screenTexSize = vec2(1.0)/fwidth(v_texCoords);
                return max(0.5*dot(unitRange, screenTexSize), 1.0);
            }
            
            void main() {
                vec3 color = texture2D(u_texture, v_texCoords).rgb;
                float distance = median(color) - 0.5; // thickness can be controlled here
                
//              simplier, but worse results:
              float dxy = fwidth(distance);
              float opacity = smoothstep(-dxy, dxy, distance);
                
//                float dxy = screenPxRange() * distance;
//                float opacity = clamp(dxy + 0.5, 0.0, 1.0);
                
                gl_FragColor = vec4(v_color.rgb, v_color.a * opacity);
            }
        """.trimIndent()

        /*void main() {
    vec3 msd = texture(msdf, texCoord).rgb;
    float sd = median(msd.r, msd.g, msd.b);
    float screenPxDistance = screenPxRange()*(sd - 0.5);
    float opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);
    color = mix(bgColor, fgColor, opacity);
}

float screenPxRange() {
    vec2 unitRange = vec2(distanceRange)/vec2(textureSize(u_texture, 0));
    vec2 screenTexSize = vec2(1.0)/fwidth(texCoord);
    return max(0.5*dot(unitRange, screenTexSize), 1.0);
}*/

        val uTexture = ShaderParameter.UniformSample2D("u_texture")

        override val parameters: LinkedHashSet<ShaderParameter> =
            linkedSetOf(
                uTexture,
            )
    }
}