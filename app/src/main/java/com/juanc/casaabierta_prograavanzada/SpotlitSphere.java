package com.juanc.casaabierta_prograavanzada;
import android.opengl.GLES20;

import com.juanc.casaabierta_prograavanzada.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class SpotlitSphere implements ShaderUtils.SpotlitShape {

    private final int program;
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer normalBuffer;
    private final int vertexCount;
    private final float[] color;

    public SpotlitSphere(float r, float g, float b, float a) {
        this(r, g, b, a, 16, 16);
    }

    public SpotlitSphere(float r, float g, float b, float a, int rings, int sectors) {
        program = ShaderUtils.createProgram(ShaderUtils.LIT_VERTEX_SHADER, ShaderUtils.LIT_FRAGMENT_SHADER);
        color = new float[]{r, g, b, a};

        float[] vertices = new float[rings * sectors * 2 * 3];
        float[] normals = new float[rings * sectors * 2 * 3];
        int idx = 0;

        for (int ring = 0; ring < rings; ring++) {
            float phi1 = (float) (Math.PI * ring / (rings - 1));
            float phi2 = (float) (Math.PI * (ring + 1) / (rings - 1));

            for (int s = 0; s < sectors; s++) {
                float theta = (float) (2.0 * Math.PI * s / (sectors - 1));

                float x1 = (float) (Math.sin(phi1) * Math.cos(theta));
                float y1 = (float) Math.cos(phi1);
                float z1 = (float) (Math.sin(phi1) * Math.sin(theta));

                vertices[idx] = x1; vertices[idx + 1] = y1; vertices[idx + 2] = z1;
                normals[idx] = x1; normals[idx + 1] = y1; normals[idx + 2] = z1;
                idx += 3;

                float x2 = (float) (Math.sin(phi2) * Math.cos(theta));
                float y2 = (float) Math.cos(phi2);
                float z2 = (float) (Math.sin(phi2) * Math.sin(theta));

                vertices[idx] = x2; vertices[idx + 1] = y2; vertices[idx + 2] = z2;
                normals[idx] = x2; normals[idx + 1] = y2; normals[idx + 2] = z2;
                idx += 3;
            }
        }

        vertexCount = idx / 3;

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer nb = ByteBuffer.allocateDirect(normals.length * 4);
        nb.order(ByteOrder.nativeOrder());
        normalBuffer = nb.asFloatBuffer();
        normalBuffer.put(normals);
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