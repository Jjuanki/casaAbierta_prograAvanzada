package com.juanc.casaabierta_prograavanzada.dragon;


import android.opengl.GLES20;

import com.juanc.casaabierta_prograavanzada.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Malla de un cubo unitario (de -0.5 a 0.5 en cada eje), compartida por todas
 * las figuras de "pixel art" (voxeles). Se construye UNA sola vez (static)
 * y todas las instancias de VoxelFigure la reutilizan.
 */

public class Cube {

    private final int program;
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer normalBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    public Cube() {
        program = ShaderUtils.createProgram(ShaderUtils.LIT_VERTEX_SHADER, ShaderUtils.LIT_FRAGMENT_SHADER);

        // 6 caras * 4 vertices; cada cara tiene su propia normal (aristas marcadas, look "voxel")
        float[] verts = {
                // +X
                0.5f,-0.5f,-0.5f,  0.5f,-0.5f,0.5f,  0.5f,0.5f,0.5f,  0.5f,0.5f,-0.5f,
                // -X
                -0.5f,-0.5f,0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, -0.5f,0.5f,0.5f,
                // +Y
                -0.5f,0.5f,-0.5f,  0.5f,0.5f,-0.5f,  0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f,
                // -Y
                -0.5f,-0.5f,0.5f,  0.5f,-0.5f,0.5f,  0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f,
                // +Z
                -0.5f,-0.5f,0.5f,  0.5f,-0.5f,0.5f,  0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f,
                // -Z
                0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f,  0.5f,0.5f,-0.5f
        };

        float[] norms = {
                1,0,0, 1,0,0, 1,0,0, 1,0,0,
                -1,0,0, -1,0,0, -1,0,0, -1,0,0,
                0,1,0, 0,1,0, 0,1,0, 0,1,0,
                0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0,
                0,0,1, 0,0,1, 0,0,1, 0,0,1,
                0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1
        };

        short[] idx = new short[36];
        for (int face = 0; face < 6; face++) {
            short base = (short) (face * 4);
            int o = face * 6;
            idx[o] = base;         idx[o + 1] = (short) (base + 1); idx[o + 2] = (short) (base + 2);
            idx[o + 3] = base;     idx[o + 4] = (short) (base + 2); idx[o + 5] = (short) (base + 3);
        }

        ByteBuffer vb = ByteBuffer.allocateDirect(verts.length * 4);
        vb.order(ByteOrder.nativeOrder());
        vertexBuffer = vb.asFloatBuffer();
        vertexBuffer.put(verts);
        vertexBuffer.position(0);

        ByteBuffer nb = ByteBuffer.allocateDirect(norms.length * 4);
        nb.order(ByteOrder.nativeOrder());
        normalBuffer = nb.asFloatBuffer();
        normalBuffer.put(norms);
        normalBuffer.position(0);

        ByteBuffer ib = ByteBuffer.allocateDirect(idx.length * 2);
        ib.order(ByteOrder.nativeOrder());
        indexBuffer = ib.asShortBuffer();
        indexBuffer.put(idx);
        indexBuffer.position(0);
        indexCount = idx.length;
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle, float[] color) {
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
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aNormal);
    }
}
