package com.juanc.casaabierta_prograavanzada;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Cylinder {

    private final int program;

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer normalBuffer;
    private final short[] indices;
    private final ShortBuffer indexBuffer;

    // Parametros (ajusta a gusto)
    private static final float RADIUS = 0.05f;   // delgado, como un cable
    private static final float TOP_Y = -1.45f;    // debe coincidir con el polo inferior de Hemisphere
    private static final float BOTTOM_Y = -4.0f;  // donde termina (hacia las nubes)
    private static final int SEGMENTS = 10;

    private final float[] color = {0.05f, 0.05f, 0.05f, 1.0f}; // casi negro

    public Cylinder() {
        program = ShaderUtils.createProgram(ShaderUtils.LIT_VERTEX_SHADER, ShaderUtils.LIT_FRAGMENT_SHADER);

        // anillo superior + anillo inferior, formando una tira de triangulos
        float[] verts = new float[(SEGMENTS + 1) * 2 * 3];
        float[] norms = new float[(SEGMENTS + 1) * 2 * 3];
        int idx = 0;
        for (int i = 0; i <= SEGMENTS; i++) {
            float theta = (float) (2 * Math.PI * i / SEGMENTS);
            float x = RADIUS * (float) Math.cos(theta);
            float z = RADIUS * (float) Math.sin(theta);
            // normal lateral: apunta hacia afuera del eje del cilindro (sin componente Y)
            float nx = (float) Math.cos(theta);
            float nz = (float) Math.sin(theta);

            // vertice superior
            verts[idx] = x; verts[idx + 1] = TOP_Y; verts[idx + 2] = z;
            norms[idx] = nx; norms[idx + 1] = 0f; norms[idx + 2] = nz;
            idx += 3;

            // vertice inferior
            verts[idx] = x; verts[idx + 1] = BOTTOM_Y; verts[idx + 2] = z;
            norms[idx] = nx; norms[idx + 1] = 0f; norms[idx + 2] = nz;
            idx += 3;
        }

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

        int vertexCount = verts.length / 3;
        indices = new short[vertexCount];
        for (short i = 0; i < vertexCount; i++) indices[i] = i;

        ByteBuffer ib = ByteBuffer.allocateDirect(indices.length * 2);
        ib.order(ByteOrder.nativeOrder());
        indexBuffer = ib.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle) {
        GLES20.glUseProgram(program);

        int aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        int aNormal = GLES20.glGetAttribLocation(program, "aNormal");
        int uColorHandle = GLES20.glGetUniformLocation(program, "uColor");

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uModelMatrix"), 1, false, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uMVPMatrix"), 1, false, mvpMatrix, 0);

        // Borde interior al 60% del angulo total -> transicion suave en vez de un corte duro
        float innerAngle = spotlightAngle * 0.6f;
        ShaderUtils.applyLightUniforms(program, lightPos, spotlightAngle, innerAngle, new float[]{0f, 1f, 8f});

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glEnableVertexAttribArray(aNormal);
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);

        GLES20.glUniform4fv(uColorHandle, 1, color, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aNormal);
    }
}