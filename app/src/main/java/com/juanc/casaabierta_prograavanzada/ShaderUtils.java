package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderUtils {

    // ---- Shader compartido para todas las figuras con iluminacion tipo spotlight ----
    // Vertex shader: pasa posicion y normal en espacio de mundo a los varyings
    public static final String LIT_VERTEX_SHADER =
            "attribute vec4 aPosition;" +
                    "attribute vec3 aNormal;" +
                    "uniform mat4 uMVPMatrix;" +
                    "uniform mat4 uModelMatrix;" +
                    "varying vec3 vNormal;" +
                    "varying vec3 vPosition;" +
                    "void main() {" +
                    "  vec4 worldPos = uModelMatrix * aPosition;" +
                    "  vPosition = worldPos.xyz;" +
                    "  vNormal = mat3(uModelMatrix) * aNormal;" +
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "}";

    // Fragment shader: spotlight con borde SUAVE (smoothstep entre inner/outer cutoff)
    // y atenuacion por distancia. SIN ambiente: las figuras son completamente negras
    // (invisibles contra el fondo negro) hasta que la luz las toca directamente.
    public static final String LIT_FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform vec3 uLightPos;" +
                    "uniform vec3 uLightDir;" +
                    "uniform vec3 uEyePos;" +
                    "uniform float uInnerCutOff;" +
                    "uniform float uOuterCutOff;" +
                    "uniform vec4 uColor;" +
                    "varying vec3 vNormal;" +
                    "varying vec3 vPosition;" +
                    "void main() {" +
                    "  vec3 normalN = normalize(vNormal);" +
                    "  vec3 toLight = uLightPos - vPosition;" +
                    "  float dist = length(toLight);" +
                    "  vec3 lightDir = toLight / dist;" +
                    "  float theta = dot(lightDir, normalize(-uLightDir));" +
                    "  float epsilon = max(uInnerCutOff - uOuterCutOff, 0.0001);" +
                    "  float spotFactor = clamp((theta - uOuterCutOff) / epsilon, 0.0, 1.0);" +
                    "  float attenuation = 1.0 / (1.0 + 0.02 * dist + 0.01 * dist * dist);" +
                    "  float diff = max(dot(normalN, lightDir), 0.0);" +
                    "  vec3 diffuse = diff * uColor.rgb * spotFactor * attenuation;" +
                    "  vec3 viewDir = normalize(uEyePos - vPosition);" +
                    "  vec3 reflectDir = reflect(-lightDir, normalN);" +
                    "  float spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);" +
                    "  vec3 specular = spec * vec3(0.4) * spotFactor * attenuation;" +
                    "  vec3 result = diffuse + specular;" + // sin termino ambiente -> negro fuera de la luz
                    "  gl_FragColor = vec4(result, uColor.a);" +
                    "}";

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("ShaderUtils", "Error compilando shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
        }
        return shader;
    }

    public static int createProgram(String vertex, String fragment) {
        int vertexS = loadShader(GLES20.GL_VERTEX_SHADER, vertex);
        int fragmentS = loadShader(GLES20.GL_FRAGMENT_SHADER, fragment);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexS);
        GLES20.glAttachShader(program, fragmentS);
        GLES20.glLinkProgram(program);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e("ShaderUtils", "Error enlazando programa: " + GLES20.glGetProgramInfoLog(program));
        }
        return program;
    }

    /**
     * Centraliza el calculo y seteo de los uniforms de iluminacion (posicion/direccion
     * de la luz, camara y los angulos del cono) para que cada figura no repita esta logica.
     *
     * @param outerAngleDeg angulo total del cono (borde exterior, donde la luz llega a 0)
     * @param innerAngleDeg angulo del nucleo totalmente iluminado (borde interior, sin degradado)
     */
    public static void applyLightUniforms(int program, float[] lightPos, float outerAngleDeg, float innerAngleDeg, float[] eyePos) {
        float dirX = -lightPos[0], dirY = -lightPos[1], dirZ = -lightPos[2];
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len > 0f) { dirX /= len; dirY /= len; dirZ /= len; }

        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uLightPos"), lightPos[0], lightPos[1], lightPos[2]);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uLightDir"), dirX, dirY, dirZ);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uEyePos"), eyePos[0], eyePos[1], eyePos[2]);

        float outerCos = (float) Math.cos(Math.toRadians(outerAngleDeg));
        float innerCos = (float) Math.cos(Math.toRadians(innerAngleDeg));
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uOuterCutOff"), outerCos);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uInnerCutOff"), innerCos);
    }
}