package com.juanc.casaabierta_prograavanzada.Dibujos;

import android.opengl.GLES20;

import com.juanc.casaabierta_prograavanzada.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Cilindro/cono unitario reutilizable, en el mismo rango -0.5..0.5 en Y
 * que Cube (asi que encaja con la misma logica de "apilar piezas sin
 * huecos" que ya usan Sunflower / Goku / Rocket). El eje va de
 * Y = -0.5 (abajo) a Y = +0.5 (arriba).
 *
 * - Si topRadius == bottomRadius -> cilindro recto (cuerpo liso, sin las
 *   esquinas que se ven con un Cube).
 * - Si topRadius != bottomRadius -> cono o frustum (nariz puntiaguda,
 *   tobera de motor acampanada, llama que se afina hacia la punta).
 * - Si uno de los dos radios es 0 (o casi 0) -> punta cerrada (cono real).
 *
 * A diferencia de Cube, esta clase NO dibuja aristas negras: en una
 * superficie curva las lineas rectas se ven raras, asi que queda lisa.
 * La normal del costado esta inclinada segun la pendiente del cono
 * (no es puramente radial), para que la luz se vea bien tanto en un
 * cilindro recto como en un cono cerrado.
 *
 * No tiene tapas (no dibuja el circulo de arriba ni el de abajo), igual
 * que el Cylinder original del proyecto: pensada para que sus extremos
 * queden ocultos contra otra pieza (el cuerpo contra el collar, la nariz
 * contra el cuerpo, etc.), tal como ya se arma todo en Rocket.
 */
public class TaperedCylinder implements ShaderUtils.SpotlitShape {

    private final int program;
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer normalBuffer;
    private final int vertexCount;
    private final float[] color;

    public TaperedCylinder(float topRadius, float bottomRadius, float r, float g, float b, float a) {
        this(topRadius, bottomRadius, r, g, b, a, 16);
    }

    public TaperedCylinder(float topRadius, float bottomRadius, float r, float g, float b, float a, int segments) {
        program = ShaderUtils.createProgram(ShaderUtils.LIT_VERTEX_SHADER, ShaderUtils.LIT_FRAGMENT_SHADER);
        color = new float[]{r, g, b, a};

        float topY = 0.5f;
        float bottomY = -0.5f;

        // Pendiente del costado (en el plano radio-Y), para inclinar la
        // normal: asi la luz "resbala" correctamente tanto en un cilindro
        // recto (normal puramente radial) como en un cono (normal inclinada
        // hacia el eje, como en la vida real).
        float dr = bottomRadius - topRadius; // cuanto se abre/cierra el radio al bajar
        float dy = topY - bottomY;           // = 1.0
        float len = (float) Math.sqrt(dr * dr + dy * dy);
        float nRadial = dy / len;
        float nY = dr / len;

        float[] verts = new float[(segments + 1) * 2 * 3];
        float[] norms = new float[(segments + 1) * 2 * 3];
        int idx = 0;

        for (int i = 0; i <= segments; i++) {
            float theta = (float) (2 * Math.PI * i / segments);
            float cosT = (float) Math.cos(theta);
            float sinT = (float) Math.sin(theta);

            // vertice de arriba
            verts[idx] = topRadius * cosT; verts[idx + 1] = topY; verts[idx + 2] = topRadius * sinT;
            norms[idx] = nRadial * cosT;   norms[idx + 1] = nY;   norms[idx + 2] = nRadial * sinT;
            idx += 3;

            // vertice de abajo
            verts[idx] = bottomRadius * cosT; verts[idx + 1] = bottomY; verts[idx + 2] = bottomRadius * sinT;
            norms[idx] = nRadial * cosT;      norms[idx + 1] = nY;      norms[idx + 2] = nRadial * sinT;
            idx += 3;
        }

        vertexCount = idx / 3;

        ByteBuffer bb = ByteBuffer.allocateDirect(verts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(verts);
        vertexBuffer.position(0);

        ByteBuffer nb = ByteBuffer.allocateDirect(norms.length * 4);
        nb.order(ByteOrder.nativeOrder());
        normalBuffer = nb.asFloatBuffer();
        normalBuffer.put(norms);
        normalBuffer.position(0);
    }

    @Override
    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle) {
        GLES20.glUseProgram(program);

        int aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        int aNormal = GLES20.glGetAttribLocation(program, "aNormal");
        int uColorHandle = GLES20.glGetUniformLocation(program, "uColor");

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uModelMatrix"), 1, false, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uMVPMatrix"), 1, false, mvpMatrix, 0);

        float innerAngle = spotlightAngle * 0.6f;
        ShaderUtils.applyLightUniforms(program, lightPos, spotlightAngle, innerAngle, new float[]{0f, 1f, 8f});

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glEnableVertexAttribArray(aNormal);
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);

        GLES20.glUniform4fv(uColorHandle, 1, color, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aNormal);
    }
}