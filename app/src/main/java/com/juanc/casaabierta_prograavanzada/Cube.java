package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


/**
 * Cubo unitario (de -0.5 a 0.5 en cada eje), COMPARTIDO por todo el proyecto:
 * - Los voxeles del dragon (PixelArtFigure) lo usan llamando a
 *   draw(mvp, model, luz, angulo, color) muchas veces por frame, con distinto
 *   color cada vez y SIN aristas (se veria muy recargado en piezas tan chicas).
 * - Las paredes, petalos, cabeza de mariposa, etc. lo usan como SpotlitShape
 *   (constructor con color fijo + draw(mvp, model, luz, angulo) de 4 argumentos),
 *   que SI dibuja aristas negras para el look tipo comic/outline.
 */
public class Cube implements ShaderUtils.SpotlitShape {

    // Shader simple (sin luz), solo para las lineas negras que marcan las aristas
    private final String lineVertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String lineFragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private final int litProgram;
    private final int lineProgram;

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer normalBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    private final FloatBuffer edgeVertexBuffer;
    private final ShortBuffer edgeIndexBuffer;
    private final int edgeIndexCount;
    private final float[] edgeColor = {0.05f, 0.05f, 0.05f, 1.0f};

    // Color por defecto, usado solo por el draw() de 4 argumentos (SpotlitShape)
    private final float[] instanceColor;

    public Cube() {
        this(0.08f, 0.18f, 0.32f, 1.0f);
    }

    public Cube(float r, float g, float b, float a) {
        instanceColor = new float[]{r, g, b, a};

        litProgram = ShaderUtils.createProgram(ShaderUtils.LIT_VERTEX_SHADER, ShaderUtils.LIT_FRAGMENT_SHADER);
        lineProgram = ShaderUtils.createProgram(lineVertexShaderCode, lineFragmentShaderCode);

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

        vertexBuffer = toFloatBuffer(verts);
        normalBuffer = toFloatBuffer(norms);

        ByteBuffer ib = ByteBuffer.allocateDirect(idx.length * 2);
        ib.order(ByteOrder.nativeOrder());
        indexBuffer = ib.asShortBuffer();
        indexBuffer.put(idx);
        indexBuffer.position(0);
        indexCount = idx.length;

        // ---- Aristas: 8 esquinas unicas, 12 lineas ----
        float s = 0.5f;
        float[] edgeVerts = new float[]{
                -s,-s,-s,  s,-s,-s,  s, s,-s,  -s, s,-s,
                -s,-s, s,  s,-s, s,  s, s, s,  -s, s, s
        };
        edgeVertexBuffer = toFloatBuffer(edgeVerts);

        short[] edgeIdx = new short[]{
                0,1, 1,2, 2,3, 3,0,
                4,5, 5,6, 6,7, 7,4,
                0,4, 1,5, 2,6, 3,7
        };
        edgeIndexCount = edgeIdx.length;
        ByteBuffer eb = ByteBuffer.allocateDirect(edgeIdx.length * 2);
        eb.order(ByteOrder.nativeOrder());
        edgeIndexBuffer = eb.asShortBuffer();
        edgeIndexBuffer.put(edgeIdx);
        edgeIndexBuffer.position(0);
    }

    private FloatBuffer toFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    /** Uso tipo SpotlitShape (paredes, petalos, cabeza de mariposa...): color fijo + aristas. */
    @Override
    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle) {
        drawInternal(mvpMatrix, modelMatrix, lightPos, spotlightAngle, instanceColor, true);
    }

    /** Uso tipo voxel (dragon): color distinto en cada llamada, sin aristas. */
    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle, float[] color) {
        drawInternal(mvpMatrix, modelMatrix, lightPos, spotlightAngle, color, false);
    }

    private void drawInternal(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle,
                              float[] color, boolean withEdges) {
        GLES20.glUseProgram(litProgram);

        int aPosition = GLES20.glGetAttribLocation(litProgram, "aPosition");
        int aNormal = GLES20.glGetAttribLocation(litProgram, "aNormal");
        int uColorHandle = GLES20.glGetUniformLocation(litProgram, "uColor");

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(litProgram, "uModelMatrix"), 1, false, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(litProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0);

        float innerAngle = spotlightAngle * 0.6f;
        ShaderUtils.applyLightUniforms(litProgram, lightPos, spotlightAngle, innerAngle, new float[]{0f, 1f, 8f});

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glEnableVertexAttribArray(aNormal);
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);

        GLES20.glUniform4fv(uColorHandle, 1, color, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aNormal);

        if (!withEdges) return;

        GLES20.glUseProgram(lineProgram);
        int linePos = GLES20.glGetAttribLocation(lineProgram, "vPosition");
        int lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "vColor");
        int lineMvpHandle = GLES20.glGetUniformLocation(lineProgram, "uMVPMatrix");

        GLES20.glUniformMatrix4fv(lineMvpHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4fv(lineColorHandle, 1, edgeColor, 0);
        GLES20.glLineWidth(3f);

        GLES20.glEnableVertexAttribArray(linePos);
        GLES20.glVertexAttribPointer(linePos, 3, GLES20.GL_FLOAT, false, 0, edgeVertexBuffer);
        GLES20.glDrawElements(GLES20.GL_LINES, edgeIndexCount, GLES20.GL_UNSIGNED_SHORT, edgeIndexBuffer);
        GLES20.glDisableVertexAttribArray(linePos);
    }
}
