package net.mattemade.gfxtest.shader

import com.littlekt.Context
import com.littlekt.graphics.shader.FragmentShaderModel
import com.littlekt.graphics.shader.ShaderParameter
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.graphics.shader.VertexShaderModel

object CrtVhsShader {

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
            
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            
            varying vec4 v_color;
            varying vec2 v_texCoords;
            
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
            linkedSetOf(uProjTrans, aPosition, aColor, aTexCoord0)
    }

    class Fragment : FragmentShaderModel() {
        // language=GLSL
        override var source: String = """
            #ifdef GL_ES
            precision mediump float;
            #endif

            uniform sampler2D u_texture;
            uniform float u_time;
            uniform float u_strength;
            uniform vec2 u_resolution;

            varying vec4 v_color;
            varying vec2 v_texCoords;

            // CRT screen curve
            vec2 curve(vec2 uv) {
                uv = (uv - 0.5) * 2.0; // [0.0, 1.0] -> [-1.0, 1.0]
                uv *= 1.0 + pow((abs(uv.yx) / 5.0), vec2(2.0)) * u_strength;
                uv = (uv / 2.0) + 0.5;  // [-1.0, 1.0] -> [0.0, 1.0] 
                return uv;
            }

            float random(vec2 st) {
                return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
            }

            void main() {
                vec2 uv = curve(v_texCoords);

                // nothing outside of the curved CRT, but maybe add some soft reflections?
                if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
                    gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
                    return;
                }

                // rolling VHS tape tearing
                float tearing_time = u_time * 10.0;
                float tape_tearing = smoothstep(0.85, 1.0, sin(uv.y * 3.0 + u_time * 2.0)) * 0.012;
                
                // random line shift
                float line_noise = random(vec2(0.0, uv.y + u_time));
                if (line_noise > 0.95) {
                    tape_tearing += (random(vec2(uv.y, u_time)) - 0.5) * 0.008;
                }
                
                vec2 distorted_uv = uv;
                distorted_uv.x += tape_tearing * u_strength;

                // chromatic aberration
                float aberration_shift = (0.003 + (sin(u_time * 2.0) * 0.001)) * u_strength;
                float col_r = texture2D(u_texture, vec2(distorted_uv.x + aberration_shift, distorted_uv.y)).r;
                float col_g = texture2D(u_texture, distorted_uv).g;
                float col_b = texture2D(u_texture, vec2(distorted_uv.x - aberration_shift, distorted_uv.y)).b;
                vec3 color = vec3(col_r, col_g, col_b);

                // simple CRT scanlines shadow mask
                float scanline_shadow = sin(distorted_uv.y * u_resolution.y * 1.5) * 0.15;
                color -= scanline_shadow * u_strength;

                // red color bleed
                color.r += texture2D(u_texture, distorted_uv - vec2(0.005, 0.0)).r * 0.1 * u_strength;
                
                // boost contrast eaten by scanlines
                color *= 1.0 + 0.15 * u_strength;

                // analogue grain
                float grain = (random(distorted_uv * u_time) - 0.5) * 0.05;
                color += grain * u_strength;

                // vignette
                vec2 vignette_uv = uv * (1.0 - uv.yx);
                float vignette_factor = vignette_uv.x * vignette_uv.y * 15.0;
                vignette_factor = clamp(pow(vignette_factor, 0.25), 0.0, 1.0);
                color *= 1.0 + (vignette_factor - 1.0) * u_strength;

                gl_FragColor = vec4(color, v_color.a);
            }
        """.trimIndent()

        val uTexture = ShaderParameter.UniformSample2D("u_texture")
        val uTime = ShaderParameter.UniformFloat("u_time")
        val uStrength = ShaderParameter.UniformFloat("u_strength")
        val uResolution = ShaderParameter.UniformVec2("u_resolution")

        override val parameters: LinkedHashSet<ShaderParameter> =
            linkedSetOf(uTexture, uTime, uStrength, uResolution)
    }
}