package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;

import com.juanc.casaabierta_prograavanzada.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuña de piso circular (90°), pensada para reemplazar los cuadrantes cuadrados
 * de {@link Cube} en el piso de la escena. Es la version "unitaria" (radio 1,
 * espesor de -0.5 a 0.5, igual que Cube) de una porcion de torta, que va de
 * angulo 0° a 90° alrededor del eje Y.
 *
 * Para armar el circulo completo con 4 colores distintos (uno por escenario:
 * dragon, cohete, mariposa, girasol) se instancia esta clase 4 veces (una por
 * color) y se dibuja cada una rotada 0°, 90°, 180° y 270° en Y, escalada al
 * radio deseado (ideal: el mismo RADIUS = 1.5 de HemiSphere) y a un espesor
 * fino en Y (ej. 0.03), levemente sobre el origen (translateY ~0.01) para que
 * no compita en z-fighting con la tapa plana del hemisferio.
 *
 * Implementa SpotlitShape (como Cube) asi que se puede pasar directo al mismo
 * shader compartido (litProgram) y respeta la misma convencion de iluminacion.
 */
public class FloorTile implements ShaderUtils.SpotlitShape {

    private static final int SEGMENTS = 12; // subdivisiones del arco de 90° (mas = mas redondo)

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

    private final float[] color;

    public FloorTile(float r, float g, float b, float a) {
        color = new float[]{r, g, b, a};

        litProgram = ShaderUtils.createProgram(ShaderUtils.LIT_VERTEX_SHADER, ShaderUtils.LIT_FRAGMENT_SHADER);
        lineProgram = ShaderUtils.createProgram(lineVertexShaderCode, lineFragmentShaderCode);

        List<Float> verts = new ArrayList<>();
        List<Float> norms = new ArrayList<>();
        List<Short> idx = new ArrayList<>();

        // ---- Tapa superior (abanico de triangulos desde el centro) ----
        short topCenter = addVertex(verts, norms, 0f, 0.5f, 0f, 0f, 1f, 0f);
        short[] topRing = new short[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float ang = (float) Math.toRadians(90.0 * i / SEGMENTS);
            float x = (float) Math.cos(ang);
            float z = (float) Math.sin(ang);
            topRing[i] = addVertex(verts, norms, x, 0.5f, z, 0f, 1f, 0f);
        }
        for (int i = 0; i < SEGMENTS; i++) {
            idx.add(topCenter); idx.add(topRing[i]); idx.add(topRing[i + 1]);
        }

        // ---- Tapa inferior (abanico invertido) ----
        short bottomCenter = addVertex(verts, norms, 0f, -0.5f, 0f, 0f, -1f, 0f);
        short[] bottomRing = new short[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float ang = (float) Math.toRadians(90.0 * i / SEGMENTS);
            float x = (float) Math.cos(ang);
            float z = (float) Math.sin(ang);
            bottomRing[i] = addVertex(verts, norms, x, -0.5f, z, 0f, -1f, 0f);
        }
        for (int i = 0; i < SEGMENTS; i++) {
            idx.add(bottomCenter); idx.add(bottomRing[i + 1]); idx.add(bottomRing[i]);
        }

        // ---- Pared curva exterior (normal radial suave, como un trozo de cilindro) ----
        short[] wallTop = new short[SEGMENTS + 1];
        short[] wallBottom = new short[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float ang = (float) Math.toRadians(90.0 * i / SEGMENTS);
            float x = (float) Math.cos(ang);
            float z = (float) Math.sin(ang);
            wallTop[i] = addVertex(verts, norms, x, 0.5f, z, x, 0f, z);
            wallBottom[i] = addVertex(verts, norms, x, -0.5f, z, x, 0f, z);
        }
        for (int i = 0; i < SEGMENTS; i++) {
            idx.add(wallTop[i]);     idx.add(wallBottom[i]);     idx.add(wallTop[i + 1]);
            idx.add(wallTop[i + 1]); idx.add(wallBottom[i]);     idx.add(wallBottom[i + 1]);
        }

        // ---- Pared plana en angulo 0° (el corte recto "de entrada" de la cuña) ----
        short ct0 = addVertex(verts, norms, 0f, 0.5f, 0f, 0f, 0f, -1f);
        short et0 = addVertex(verts, norms, 1f, 0.5f, 0f, 0f, 0f, -1f);
        short cb0 = addVertex(verts, norms, 0f, -0.5f, 0f, 0f, 0f, -1f);
        short eb0 = addVertex(verts, norms, 1f, -0.5f, 0f, 0f, 0f, -1f);
        idx.add(ct0); idx.add(cb0); idx.add(eb0);
        idx.add(ct0); idx.add(eb0); idx.add(et0);

        // ---- Pared plana en angulo 90° (el corte recto "de salida" de la cuña) ----
        short ct90 = addVertex(verts, norms, 0f, 0.5f, 0f, 1f, 0f, 0f);
        short et90 = addVertex(verts, norms, 0f, 0.5f, 1f, 1f, 0f, 0f);
        short cb90 = addVertex(verts, norms, 0f, -0.5f, 0f, 1f, 0f, 0f);
        short eb90 = addVertex(verts, norms, 0f, -0.5f, 1f, 1f, 0f, 0f);
        idx.add(ct90); idx.add(eb90); idx.add(cb90);
        idx.add(ct90); idx.add(et90); idx.add(eb90);

        vertexBuffer = toFloatBuffer(verts);
        normalBuffer = toFloatBuffer(norms);
        indexBuffer = toShortBuffer(idx);
        indexCount = idx.size();

        // ---- Lineas de borde: arco superior + los 2 cortes rectos (look "comic") ----
        List<Float> edgeVerts = new ArrayList<>();
        List<Short> edgeIdx = new ArrayList<>();
        short[] rimIdx = new short[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float ang = (float) Math.toRadians(90.0 * i / SEGMENTS);
            rimIdx[i] = addEdgeVertex(edgeVerts, (float) Math.cos(ang), 0.5f, (float) Math.sin(ang));
        }
        for (int i = 0; i < SEGMENTS; i++) { edgeIdx.add(rimIdx[i]); edgeIdx.add(rimIdx[i + 1]); }

        short edgeCenter = addEdgeVertex(edgeVerts, 0f, 0.5f, 0f);
        edgeIdx.add(edgeCenter); edgeIdx.add(rimIdx[0]);
        edgeIdx.add(edgeCenter); edgeIdx.add(rimIdx[SEGMENTS]);

        edgeVertexBuffer = toFloatBuffer(edgeVerts);
        edgeIndexBuffer = toShortBuffer(edgeIdx);
        edgeIndexCount = edgeIdx.size();
    }

    private short addVertex(List<Float> verts, List<Float> norms,
                            float x, float y, float z, float nx, float ny, float nz) {
        short index = (short) (verts.size() / 3);
        verts.add(x); verts.add(y); verts.add(z);
        norms.add(nx); norms.add(ny); norms.add(nz);
        return index;
    }

    private short addEdgeVertex(List<Float> verts, float x, float y, float z) {
        short index = (short) (verts.size() / 3);
        verts.add(x); verts.add(y); verts.add(z);
        return index;
    }

    private FloatBuffer toFloatBuffer(List<Float> data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.size() * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        for (float v : data) fb.put(v);
        fb.position(0);
        return fb;
    }

    private ShortBuffer toShortBuffer(List<Short> data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.size() * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        for (short v : data) sb.put(v);
        sb.position(0);
        return sb;
    }

    @Override
    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle) {
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

        // ---- Borde negro (arco + 2 cortes rectos) ----
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