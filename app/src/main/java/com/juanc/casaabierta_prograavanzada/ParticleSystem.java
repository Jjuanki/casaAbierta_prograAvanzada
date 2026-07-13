package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Sistema de particulas simple: polvo brillante que flota alrededor de las
 * figuras. Cada particula sube lentamente y se hamaca un poco de lado a lado;
 * al llegar arriba del todo, reaparece abajo (efecto de flotar sin fin).
 *
 * Se dibujan como GL_POINTS con blending ADITIVO (glow), sin necesitar
 * texturas: el circulo suave de cada particula se dibuja en el fragment
 * shader usando gl_PointCoord.
 */
public class ParticleSystem {

    private static final String VERTEX_SHADER =
            "attribute vec3 aPosition;" +
                    "uniform mat4 uMVPMatrix;" +
                    "uniform float uPointSize;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vec4(aPosition, 1.0);" +
                    "  gl_PointSize = uPointSize;" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main() {" +
                    "  vec2 c = gl_PointCoord - vec2(0.5);" +
                    "  float d = length(c);" +
                    "  float alpha = smoothstep(0.5, 0.0, d);" + // circulo suave, sin bordes duros
                    "  gl_FragColor = vec4(uColor.rgb, uColor.a * alpha);" +
                    "}";

    private final int program;
    private final int count;

    private final float[] pos;     // x,y,z por particula
    private final float[] speedY;  // velocidad de ascenso, distinta por particula
    private final float[] phase;   // fase aleatoria para el hamaqueo lateral
    private final FloatBuffer posBuffer;

    private final float centerX, centerZ, radiusXZ;
    private final float minY, maxY;
    private final float[] color;
    private final float pointSize;

    public ParticleSystem(int count, float centerX, float centerY, float centerZ,
                           float radiusXZ, float heightRange, float[] color, float pointSize) {
        this.count = count;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radiusXZ = radiusXZ;
        this.minY = centerY - heightRange * 0.5f;
        this.maxY = centerY + heightRange * 0.5f;
        this.color = color;
        this.pointSize = pointSize;

        pos = new float[count * 3];
        speedY = new float[count];
        phase = new float[count];

        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            spawnAt(i, rnd, rnd.nextFloat() * (maxY - minY) + minY);
            speedY[i] = 0.12f + rnd.nextFloat() * 0.22f;
            phase[i] = rnd.nextFloat() * 6.2832f;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(pos.length * 4);
        bb.order(ByteOrder.nativeOrder());
        posBuffer = bb.asFloatBuffer();

        program = ShaderUtils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    private void spawnAt(int i, Random rnd, float y) {
        float ang = rnd.nextFloat() * 6.2832f;
        float r = rnd.nextFloat() * radiusXZ;
        pos[i * 3] = centerX + (float) Math.cos(ang) * r;
        pos[i * 3 + 1] = y;
        pos[i * 3 + 2] = centerZ + (float) Math.sin(ang) * r;
    }

    /** Avanza la animacion. Llamar una vez por frame antes de draw(). */
    public void update(float dt, float time) {
        for (int i = 0; i < count; i++) {
            pos[i * 3 + 1] += speedY[i] * dt;
            if (pos[i * 3 + 1] > maxY) {
                pos[i * 3 + 1] = minY;
            }
            // Hamaqueo lateral suave, distinto por particula gracias a "phase".
            pos[i * 3] += (float) Math.sin(time * 0.8f + phase[i]) * 0.05f * dt;
            pos[i * 3 + 2] += (float) Math.cos(time * 0.8f + phase[i]) * 0.05f * dt;
        }
    }

    public void draw(float[] mvpMatrix) {
        posBuffer.clear();
        posBuffer.put(pos);
        posBuffer.position(0);

        GLES20.glDepthMask(false);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE); // aditivo: brillo tipo particula

        GLES20.glUseProgram(program);
        int aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        int uMvp = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        int uColor = GLES20.glGetUniformLocation(program, "uColor");
        int uPointSize = GLES20.glGetUniformLocation(program, "uPointSize");

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0);
        GLES20.glUniform4fv(uColor, 1, color, 0);
        GLES20.glUniform1f(uPointSize, pointSize);

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, posBuffer);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, count);
        GLES20.glDisableVertexAttribArray(aPosition);

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
    }
}
