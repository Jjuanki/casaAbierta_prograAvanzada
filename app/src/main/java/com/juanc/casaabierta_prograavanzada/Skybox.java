package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Fondo dinamico tipo "skybox": un cubo gigante que envuelve toda la escena.
 * No usa texturas (no hay imagenes cargadas en el proyecto): el "espacio
 * profundo" con estrellas titilantes y un leve degrade se genera con formulas
 * dentro del fragment shader, en base a la direccion de cada pixel.
 *
 * Se dibuja SIEMPRE primero, con glDepthMask(false) para no pisar el buffer
 * de profundidad: asi queda detras de todo sin importar el orden de dibujo
 * del resto de la escena.
 */
public class Skybox {

    private static final String VERTEX_SHADER =
            "attribute vec3 aPosition;" +
                    "uniform mat4 uMVPMatrix;" +
                    "varying vec3 vDir;" +
                    "void main() {" +
                    "  vDir = aPosition;" + // la posicion local (sin transformar) sirve como direccion
                    "  gl_Position = uMVPMatrix * vec4(aPosition, 1.0);" +
                    "}";

    // Hash pseudo-aleatorio barato para "sembrar" estrellas en una grilla direccional,
    // y una animacion simple de titileo con el tiempo.
    //
    // uReveal (0..1) mezcla entre DOS escenarios:
    //  - 0 = "caja negra": practicamente negro, para no romper el efecto actual
    //        de que todo se revela solo con el spotlight.
    //  - 1 = "otro escenario": una nebulosa violeta/magenta bien visible, que
    //        se revela cuando el usuario aleja la camara (encoge con 2 dedos).
    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "varying vec3 vDir;" +
                    "uniform float uTime;" +
                    "uniform float uReveal;" +
                    "float hash(vec3 p) {" +
                    "  return fract(sin(dot(p, vec3(12.9898, 78.233, 37.719))) * 43758.5453123);" +
                    "}" +
                    "void main() {" +
                    "  vec3 dir = normalize(vDir);" +
                    "  float t = dir.y * 0.5 + 0.5;" +
                    "  vec3 dimBottom = vec3(0.0, 0.0, 0.0);" +
                    "  vec3 dimTop = vec3(0.02, 0.015, 0.05);" +
                    "  vec3 dimColor = mix(dimBottom, dimTop, t);" +
                    "  vec3 vividBottom = vec3(0.05, 0.02, 0.22);" +
                    "  vec3 vividTop = vec3(0.45, 0.22, 0.55);" +
                    "  vec3 vividColor = mix(vividBottom, vividTop, t);" +
                    "  vec3 base = mix(dimColor, vividColor, uReveal);" +
                    "  vec3 cell = floor(dir * 50.0);" +
                    "  float star = hash(cell);" +
                    "  float twinkle = 0.5 + 0.5 * sin(uTime * 3.0 + star * 60.0);" +
                    "  float starMask = step(0.975, star);" +
                    "  float starBrightness = starMask * twinkle * mix(0.35, 1.0, uReveal);" +
                    "  vec3 color = base + vec3(starBrightness);" +
                    "  gl_FragColor = vec4(color, 1.0);" +
                    "}";

    private final int program;
    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    public Skybox() {
        this(40f);
    }

    public Skybox(float size) {
        program = ShaderUtils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        float s = size;
        float[] verts = {
                -s, -s, -s,  s, -s, -s,  s, s, -s,  -s, s, -s, // 0..3 cara trasera
                -s, -s,  s,  s, -s,  s,  s, s,  s,  -s, s,  s  // 4..7 cara delantera
        };
        vertexBuffer = toFloatBuffer(verts);

        // No hay culling de caras activado en el proyecto, asi que el orden de
        // los indices no importa: solo necesitamos las 6 caras del cubo.
        short[] idx = {
                0, 1, 2,  0, 2, 3, // -Z
                4, 6, 5,  4, 7, 6, // +Z
                0, 3, 7,  0, 7, 4, // -X
                1, 5, 6,  1, 6, 2, // +X
                3, 2, 6,  3, 6, 7, // +Y
                0, 4, 5,  0, 5, 1  // -Y
        };
        indexCount = idx.length;
        ByteBuffer ib = ByteBuffer.allocateDirect(idx.length * 2);
        ib.order(ByteOrder.nativeOrder());
        indexBuffer = ib.asShortBuffer();
        indexBuffer.put(idx);
        indexBuffer.position(0);
    }

    private FloatBuffer toFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    /**
     * @param mvpMatrix debe ser proyeccion * vista SIN el modelo (para que el fondo
     *                  quede fijo y no rote junto con la escena/el giroscopio).
     * @param time      segundos acumulados, para animar el titileo de las estrellas.
     * @param reveal    0..1. En 0 se ve la "caja negra" (escenario actual, casi
     *                  invisible). En 1 se revela el otro escenario (nebulosa
     *                  violeta bien visible). Se calcula en base al zoom (mScale).
     */
    public void draw(float[] mvpMatrix, float time, float reveal) {
        GLES20.glDepthMask(false);

        GLES20.glUseProgram(program);
        int aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        int uMvp = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        int uTime = GLES20.glGetUniformLocation(program, "uTime");
        int uReveal = GLES20.glGetUniformLocation(program, "uReveal");

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTime, time);
        GLES20.glUniform1f(uReveal, reveal);

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        GLES20.glDisableVertexAttribArray(aPosition);

        GLES20.glDepthMask(true);
    }
}
