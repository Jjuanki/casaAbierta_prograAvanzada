package com.juanc.casaabierta_prograavanzada;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class HemiSphere {

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

    private final FloatBuffer domeVertexBuffer;

    private final ShortBuffer[] bandIndexBuffers;
    private final int[] bandIndexCounts;
    private final float[][] bandColors;

    private final FloatBuffer capVertexBuffer;
    private final ShortBuffer capIndexBuffer;
    private final int capIndexCount;
    private final float[] capColor = {0.96f, 0.93f, 0.85f, 1.0f};
    private final ShortBuffer[] ringLineBuffers;
    private final int ringLineCount = LON_SEGMENTS + 1;
    private final float[] lineColor = {0.05f, 0.05f, 0.05f, 1.0f};

    private static final float RADIUS = 1.5f;
    private static final int NUM_BANDS = 6;
    private static final int SEGMENTS_PER_BAND = 5;
    private static final int LAT_SEGMENTS = NUM_BANDS * SEGMENTS_PER_BAND;
    private static final int LON_SEGMENTS = 20;

    public HemiSphere() {
        program = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);

        // Colores de las franjas, de arriba hacia abajo- intercalados
        bandColors = new float[][]{
                {0.82f, 0.35f, 0.10f, 1.0f}, // naranja
                {0.96f, 0.93f, 0.85f, 1.0f}, // crema
                {0.82f, 0.35f, 0.10f, 1.0f}, // naranja
                {0.96f, 0.93f, 0.85f, 1.0f}, // crema
                {0.20f, 0.40f, 0.60f, 1.0f}, // azul
                {0.08f, 0.18f, 0.32f, 1.0f}  // azul oscuro (punta)
        };

        float[] domeVerts = new float[(LAT_SEGMENTS + 1) * (LON_SEGMENTS + 1) * 3];
        int idx = 0;
        for (int lat = 0; lat <= LAT_SEGMENTS; lat++) {
            float phi = (float) (Math.PI / 2 * lat / LAT_SEGMENTS);
            float y = -RADIUS * (float) Math.sin(phi);
            float r = RADIUS * (float) Math.cos(phi);

            for (int lon = 0; lon <= LON_SEGMENTS; lon++) {
                float theta = (float) (2 * Math.PI * lon / LON_SEGMENTS);
                float x = r * (float) Math.cos(theta);
                float z = r * (float) Math.sin(theta);

                domeVerts[idx++] = x;
                domeVerts[idx++] = y;
                domeVerts[idx++] = z;
            }
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(domeVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        domeVertexBuffer = bb.asFloatBuffer();
        domeVertexBuffer.put(domeVerts);
        domeVertexBuffer.position(0);

        bandIndexBuffers = new ShortBuffer[NUM_BANDS];
        bandIndexCounts = new int[NUM_BANDS];

        for (int band = 0; band < NUM_BANDS; band++) {
            int latStart = band * SEGMENTS_PER_BAND;
            int latEnd = latStart + SEGMENTS_PER_BAND;

            List<Short> indexList = new ArrayList<>();
            for (int lat = latStart; lat < latEnd; lat++) {
                for (int lon = 0; lon <= LON_SEGMENTS; lon++) {
                    int current = lat * (LON_SEGMENTS + 1) + lon;
                    int next = (lat + 1) * (LON_SEGMENTS + 1) + lon;
                    indexList.add((short) current);
                    indexList.add((short) next);
                }
            }

            short[] bandIndices = new short[indexList.size()];
            for (int i = 0; i < bandIndices.length; i++) bandIndices[i] = indexList.get(i);
            bandIndexCounts[band] = bandIndices.length;

            ByteBuffer ib = ByteBuffer.allocateDirect(bandIndices.length * 2);
            ib.order(ByteOrder.nativeOrder());
            ShortBuffer sb = ib.asShortBuffer();
            sb.put(bandIndices);
            sb.position(0);
            bandIndexBuffers[band] = sb;
        }

        // ---- Anillos de linea: un borde por cada inicio de banda (para separar colores) ----
        ringLineBuffers = new ShortBuffer[NUM_BANDS];
        for (int band = 0; band < NUM_BANDS; band++) {
            int lat = band * SEGMENTS_PER_BAND;
            short[] ringIndices = new short[ringLineCount];
            for (int lon = 0; lon <= LON_SEGMENTS; lon++) {
                ringIndices[lon] = (short) (lat * (LON_SEGMENTS + 1) + lon);
            }
            ByteBuffer rb = ByteBuffer.allocateDirect(ringIndices.length * 2);
            rb.order(ByteOrder.nativeOrder());
            ShortBuffer rsb = rb.asShortBuffer();
            rsb.put(ringIndices);
            rsb.position(0);
            ringLineBuffers[band] = rsb;
        }

        // ---- Tapa plana superior (disco en y = 0) ----
        float[] capVerts = new float[(LON_SEGMENTS + 2) * 3];
        idx = 0;
        capVerts[idx++] = 0f;
        capVerts[idx++] = 0f;
        capVerts[idx++] = 0f;
        for (int lon = 0; lon <= LON_SEGMENTS; lon++) {
            float theta = (float) (2 * Math.PI * lon / LON_SEGMENTS);
            capVerts[idx++] = RADIUS * (float) Math.cos(theta);
            capVerts[idx++] = 0f;
            capVerts[idx++] = RADIUS * (float) Math.sin(theta);
        }

        ByteBuffer cb = ByteBuffer.allocateDirect(capVerts.length * 4);
        cb.order(ByteOrder.nativeOrder());
        capVertexBuffer = cb.asFloatBuffer();
        capVertexBuffer.put(capVerts);
        capVertexBuffer.position(0);

        int capVertexCount = capVerts.length / 3;
        short[] capIndices = new short[capVertexCount];
        for (short i = 0; i < capVertexCount; i++) capIndices[i] = i;
        capIndexCount = capIndices.length;

        ByteBuffer cib = ByteBuffer.allocateDirect(capIndices.length * 2);
        cib.order(ByteOrder.nativeOrder());
        capIndexBuffer = cib.asShortBuffer();
        capIndexBuffer.put(capIndices);
        capIndexBuffer.position(0);
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix) {
        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glEnableVertexAttribArray(positionHandle);

        // Tapa plana de arriba
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, capVertexBuffer);
        GLES20.glUniform4fv(colorHandle, 1, capColor, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_FAN, capIndexCount, GLES20.GL_UNSIGNED_SHORT, capIndexBuffer);

        // Domo: una banda de color a la vez
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, domeVertexBuffer);
        for (int band = 0; band < NUM_BANDS; band++) {
            GLES20.glUniform4fv(colorHandle, 1, bandColors[band], 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, bandIndexCounts[band], GLES20.GL_UNSIGNED_SHORT, bandIndexBuffers[band]);
        }

        // Lineas negras separando cada banda de color
        GLES20.glUniform4fv(colorHandle, 1, lineColor, 0);
        GLES20.glLineWidth(3f);
        for (int band = 0; band < NUM_BANDS; band++) {
            GLES20.glDrawElements(GLES20.GL_LINE_LOOP, ringLineCount, GLES20.GL_UNSIGNED_SHORT, ringLineBuffers[band]);
        }

        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}