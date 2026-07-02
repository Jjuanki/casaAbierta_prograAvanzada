package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Cylinder {

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private final int program;
    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    private final FloatBuffer vertexBuffer;
    private final short[] indices;
    private final ShortBuffer indexBuffer;

    // Parametros (ajusta a gusto)
    private static final float RADIUS = 0.05f;   // delgado, como un cable
    private static final float TOP_Y = -1.45f;   // donde empieza: debe coincidir con el polo inferior de la Hemisphere (-RADIUS de Hemisphere, con un poco de superposicion para que no se vea el hueco)
    private static final float BOTTOM_Y = -4.0f; // donde termina (hacia las nubes)
    private static final int SEGMENTS = 10;

    private final float[] color = {1.0f, 1.0f, 1.0f, 1.0f}; // casi negro

    public Cylinder() {
        program = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);

        // anillo superior + anillo inferior, formando una tira de triangulos
        float[] verts = new float[(SEGMENTS + 1) * 2 * 3];
        int idx = 0;
        for (int i = 0; i <= SEGMENTS; i++) {
            float theta = (float) (2 * Math.PI * i / SEGMENTS);
            float x = RADIUS * (float) Math.cos(theta);
            float z = RADIUS * (float) Math.sin(theta);

            // vertice superior
            verts[idx++] = x;
            verts[idx++] = TOP_Y;
            verts[idx++] = z;

            // vertice inferior
            verts[idx++] = x;
            verts[idx++] = BOTTOM_Y;
            verts[idx++] = z;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(verts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(verts);
        vertexBuffer.position(0);

        int vertexCount = verts.length / 3;
        indices = new short[vertexCount];
        for (short i = 0; i < vertexCount; i++) indices[i] = i;

        ByteBuffer ib = ByteBuffer.allocateDirect(indices.length * 2);
        ib.order(ByteOrder.nativeOrder());
        indexBuffer = ib.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix) {
        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}