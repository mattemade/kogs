package net.mattemade.gfxtest.shader

import com.littlekt.Context
import com.littlekt.graphics.shader.FragmentShaderModel
import com.littlekt.graphics.shader.ShaderParameter
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.graphics.shader.VertexShaderModel

object MsdfBevelEffectShader {

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

        val uTime = ShaderParameter.UniformFloat("u_time")
        // language=GLSL
        override var source: String = """
            uniform sampler2D u_texture;
            uniform float u_time;
            uniform vec3 u_lightPos;
            uniform vec3 u_baseColor;
            uniform vec4 u_outlineStyle; // RBG color + A width
            uniform vec2 u_bevelStyle; // width + height
            
            in vec4 v_color;
            in vec2 v_texCoords;
            
            const float distanceRange = 4.0;
            const float bevelHeight = 10.0;
            
            float median(float r, float g, float b) {
                return max(min(r, g), min(max(r, g), b));
            }
            
            float random(vec2 co){
                return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
            }
            
            float screenPxDistance(float sd, vec2 uv) {
                vec2 unitRange = vec2(distanceRange) / vec2(textureSize(u_texture, 0));
                vec2 screenTexSize = vec2(1.0) / fwidth(uv);
                return sd * dot(unitRange, 0.5 * screenTexSize);
            }
            
            float distance(vec2 uv) {
                vec4 sampleColor = texture(u_texture, uv);
                return median(sampleColor.r, sampleColor.g, sampleColor.b) - 0.5;
            }
            
            vec3 complexBevelNormal(float sd, vec2 uv) {
                vec2 texel_size = 1.0 / vec2(textureSize(u_texture, 0));
                vec2 offset = texel_size * 1.5;
                
                float right = distance(uv + vec2(offset.x, 0.0));
                float left  = distance(uv - vec2(offset.x, 0.0));
                float up    = distance(uv + vec2(0.0, offset.y));
                float down  = distance(uv - vec2(0.0, offset.y));
                
                // smooth spatial derivatives
                float dx = (right - left) / (2.0 * offset.x);
                float dy = (up - down)    / (2.0 * offset.y);
                                            
                // smooth arc profile
                float bevel_factor = 1.0 - smoothstep(0.0, u_bevelStyle.x, sd);
                //bevel_factor = sin(bevel_factor * 1.5707963);
            
                vec2 smooth_slope = vec2(dx, dy) * bevel_factor * u_bevelStyle.y;
                return normalize(vec3(-smooth_slope.x, -smooth_slope.y, 1.0));
            }
            
            void main() {
                vec2 v_displaceEffect = vec2(u_time, 0.0);
                vec2 displacement = vec2(random(vec2(v_texCoords.y, 0.0)) - 0.5, random(vec2(0.0, v_texCoords.x)) - 0.5);// * u_time;
                vec2 displaced_coord = v_texCoords + displacement * v_displaceEffect;
                
                float sd = distance(displaced_coord);
                float letter_px_dist = screenPxDistance(sd, displaced_coord); // antialiased
                float letter_alpha = clamp(letter_px_dist + 0.5, 0.0, 1.0);
                float px_dist = screenPxDistance(sd + u_outlineStyle.a, displaced_coord); // increased by outline width
                float alpha = clamp(px_dist + 0.5, 0.0, 1.0);
//                float alpha = clamp(letterPxDist + u_outlineStyle.a + 0.5, 0.0, 1.0);
                
                if (alpha <= 0.001) {
                    // stop computations outside of the distance field
                    discard;
                }
//                float dsdx = dFdx(sd);
//                float dsdy = dFdy(sd);
//                vec3 bevelNormal = normalize(vec3(-dsdx * bevelHeight, -dsdy * bevelHeight, 1.0));
                
                vec3 bevelNormal = complexBevelNormal(sd, displaced_coord);
                
                //vec3 light_dir = u_lightPos;
                vec3 light_dir = normalize(u_lightPos - vec3(displaced_coord, 0.0));
                
                vec3 viewDir = vec3(0.0, 0.0, 1.0); // Orthographic top-down view
                vec3 halfDir = normalize(light_dir + viewDir);
                
                vec3 ambient = u_baseColor * 0.25;

                float diff = max(dot(bevelNormal, light_dir), 0.0);
                vec3 diffuse = u_baseColor * diff;

                float shininess = 16.0; 
                float specStrength = 0.25;
                float spec = pow(max(dot(bevelNormal, halfDir), 0.0), shininess);
                vec3 specular = vec3(1.0) * spec * specStrength;
            
                vec3 final_color = mix(u_outlineStyle.rgb, ambient + diffuse + specular, letter_alpha);
                gl_FragColor = vec4(final_color, alpha);
            
            }
        """.trimIndent()
        val uTexture = ShaderParameter.UniformSample2D("u_texture")
        val uLightPos = ShaderParameter.UniformVec3("u_lightPos")
        val uBaseColor = ShaderParameter.UniformVec3("u_baseColor")
        val uOutlineStyle = ShaderParameter.UniformVec4("u_outlineStyle")
        val uBevelStyle = ShaderParameter.UniformVec2("u_bevelStyle")

        override val parameters: LinkedHashSet<ShaderParameter> =
            linkedSetOf(
                uTexture, uLightPos, uBaseColor, uOutlineStyle, uBevelStyle
            )
    }
}