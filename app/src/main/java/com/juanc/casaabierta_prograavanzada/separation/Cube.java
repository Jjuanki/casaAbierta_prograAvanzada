package com.juanc.casaabierta_prograavanzada.separation;
import android.opengl.GLES20;

import com.juanc.casaabierta_prograavanzada.ShaderUtils;
import com.juanc.casaabierta_prograavanzada.SpotlitShape;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Cube implements SpotlitShape {

    private final String vertexShaderCode =
            "attribute vec4 aPosition;" +
                    "attribute vec3 aNormal;" +
                    "uniform mat4 uMVPMatrix;" +
                    "uniform mat4 uModelMatrix;" +
                    "varying vec3 vNormal;" +
                    "varying vec3 vPosition;" +
                    "void main() {" +
                    "  vec4 worldPos = uModelMatrix * aPosition;" +
                    "  vPosition = worldPos.xyz;" +
                    "  vNormal = mat3(uModelMatrix) * aNormal;" +
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec3 uLightPos;" +
                    "uniform vec3 uLightDir;" +
                    "uniform vec3 uEyePos;" +
                    "uniform float uCutOff;" +
                    "uniform vec4 uColor;" +
                    "varying vec3 vNormal;" +
                    "varying vec3 vPosition;" +
                    "void main() {" +
                    "  vec3 normalN = normalize(vNormal);" +
                    "  vec3 lightDir = normalize(uLightPos - vPosition);" +
                    "  float theta = dot(lightDir, normalize(-uLightDir));" +
                    "  vec3 ambient = uColor.rgb * 0.25;" +
                    "  vec3 diffuse = vec3(0.0);" +
                    "  vec3 specular = vec3(0.0);" +
                    "  if (theta > uCutOff) {" +
                    "    float diff = max(dot(normalN, lightDir), 0.0);" +
                    "    diffuse = diff * uColor.rgb;" +
                    "    vec3 viewDir = normalize(uEyePos - vPosition);" +
                    "    vec3 reflectDir = reflect(-lightDir, normalN);" +
                    "    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);" +
                    "    specular = spec * vec3(0.4);" +
                    "  }" +
                    "  vec3 result = ambient + diffuse + specular;" +
                    "  gl_FragColor = vec4(result, uColor.a);" +
                    "}";

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

    // ---- Geometria de las caras (con normales, 24 vertices: 4 por cara) ----
    private final FloatBuffer faceVertexBuffer;
    private final FloatBuffer faceNormalBuffer;
    private final ShortBuffer[] faceIndexBuffers;
    private final int[] faceIndexCounts;
    private final float[][] faceColors;

    // ---- Aristas del cubo (8 vertices unicos, 12 lineas) ----
    private final FloatBuffer edgeVertexBuffer;
    private final ShortBuffer edgeIndexBuffer;
    private final int edgeIndexCount;
    private final float[] lineColor = {0.05f, 0.05f, 0.05f, 1.0f};

    private static final float SIZE = 1.5f; // "radio" == media arista, analogo a RADIUS de la esfera

    public Cube() {
        this(0.08f, 0.18f, 0.32f, 1.0f);
    }

    // Constructor con color propio: permite reusar la misma clase para paredes,
    // cuerpo de Vader en negro, panel de pecho plateado, etc.
    public Cube(float r, float g, float b, float a) {
        litProgram = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);
        lineProgram = ShaderUtils.createProgram(lineVertexShaderCode, lineFragmentShaderCode);

        float[] baseColor = {r, g, b, a};
        faceColors = new float[][]{
                baseColor, baseColor, baseColor, baseColor, baseColor, baseColor
        };

        // 6 caras x 4 vertices x 3 componentes
        float[] faceVerts = new float[6 * 4 * 3];
        float[] faceNorms = new float[6 * 4 * 3];

        // Definicion de cada cara: 4 esquinas (en orden para TRIANGLE_FAN/TRIANGLES) + normal
        float s = SIZE;
        float[][][] faces = new float[][][]{
                // +X
                {{ s,-s,-s}, { s, s,-s}, { s, s, s}, { s,-s, s}},
                // -X
                {{-s,-s, s}, {-s, s, s}, {-s, s,-s}, {-s,-s,-s}},
                // +Y
                {{-s, s,-s}, {-s, s, s}, { s, s, s}, { s, s,-s}},
                // -Y
                {{-s,-s, s}, {-s,-s,-s}, { s,-s,-s}, { s,-s, s}},
                // +Z
                {{-s,-s, s}, { s,-s, s}, { s, s, s}, {-s, s, s}},
                // -Z
                {{ s,-s,-s}, {-s,-s,-s}, {-s, s,-s}, { s, s,-s}}
        };
        float[][] normals = new float[][]{
                { 1, 0, 0},
                {-1, 0, 0},
                { 0, 1, 0},
                { 0,-1, 0},
                { 0, 0, 1},
                { 0, 0,-1}
        };

        int idx = 0;
        for (int f = 0; f < 6; f++) {
            for (int v = 0; v < 4; v++) {
                faceVerts[idx] = faces[f][v][0];
                faceVerts[idx + 1] = faces[f][v][1];
                faceVerts[idx + 2] = faces[f][v][2];

                faceNorms[idx] = normals[f][0];
                faceNorms[idx + 1] = normals[f][1];
                faceNorms[idx + 2] = normals[f][2];

                idx += 3;
            }
        }

        faceVertexBuffer = toFloatBuffer(faceVerts);
        faceNormalBuffer = toFloatBuffer(faceNorms);

        // Indices por cara: 2 triangulos (0,1,2) y (0,2,3), relativos al bloque de 4 vertices de esa cara
        faceIndexBuffers = new ShortBuffer[6];
        faceIndexCounts = new int[6];
        for (int f = 0; f < 6; f++) {
            short base = (short) (f * 4);
            short[] tris = new short[]{
                    base, (short) (base + 1), (short) (base + 2),
                    base, (short) (base + 2), (short) (base + 3)
            };
            faceIndexCounts[f] = tris.length;
            faceIndexBuffers[f] = toShortBuffer(tris);
        }

        // ---- Aristas: 8 esquinas unicas ----
        float[] edgeVerts = new float[]{
                -s,-s,-s,
                s,-s,-s,
                s, s,-s,
                -s, s,-s,
                -s,-s, s,
                s,-s, s,
                s, s, s,
                -s, s, s
        };
        edgeVertexBuffer = toFloatBuffer(edgeVerts);

        // 12 aristas del cubo (indices de las 8 esquinas)
        short[] edgeIdx = new short[]{
                0,1, 1,2, 2,3, 3,0, // cara trasera
                4,5, 5,6, 6,7, 7,4, // cara frontal
                0,4, 1,5, 2,6, 3,7  // conexiones
        };
        edgeIndexCount = edgeIdx.length;
        edgeIndexBuffer = toShortBuffer(edgeIdx);
    }

    private FloatBuffer toFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    private ShortBuffer toShortBuffer(short[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        sb.put(data);
        sb.position(0);
        return sb;
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle) {
        GLES20.glUseProgram(litProgram);

        int aPosition = GLES20.glGetAttribLocation(litProgram, "aPosition");
        int aNormal = GLES20.glGetAttribLocation(litProgram, "aNormal");
        int uColorHandle = GLES20.glGetUniformLocation(litProgram, "uColor");

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(litProgram, "uModelMatrix"), 1, false, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(litProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0);

        // La luz apunta siempre hacia el centro de la escena (como una linterna que sigue al objeto)
        float dirX = -lightPos[0], dirY = -lightPos[1], dirZ = -lightPos[2];
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len > 0f) { dirX /= len; dirY /= len; dirZ /= len; }

        GLES20.glUniform3f(GLES20.glGetUniformLocation(litProgram, "uLightPos"), lightPos[0], lightPos[1], lightPos[2]);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(litProgram, "uLightDir"), dirX, dirY, dirZ);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(litProgram, "uEyePos"), 0f, 1f, 8f);
        float cutOff = (float) Math.cos(Math.toRadians(spotlightAngle));
        GLES20.glUniform1f(GLES20.glGetUniformLocation(litProgram, "uCutOff"), cutOff);

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glEnableVertexAttribArray(aNormal);

        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, faceVertexBuffer);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 0, faceNormalBuffer);

        for (int f = 0; f < 6; f++) {
            GLES20.glUniform4fv(uColorHandle, 1, faceColors[f], 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, faceIndexCounts[f], GLES20.GL_UNSIGNED_SHORT, faceIndexBuffers[f]);
        }

        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aNormal);

        // Lineas negras marcando las aristas del cubo
        GLES20.glUseProgram(lineProgram);
        int linePos = GLES20.glGetAttribLocation(lineProgram, "vPosition");
        int lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "vColor");
        int lineMvpHandle = GLES20.glGetUniformLocation(lineProgram, "uMVPMatrix");

        GLES20.glUniformMatrix4fv(lineMvpHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4fv(lineColorHandle, 1, lineColor, 0);
        GLES20.glLineWidth(3f);

        GLES20.glEnableVertexAttribArray(linePos);
        GLES20.glVertexAttribPointer(linePos, 3, GLES20.GL_FLOAT, false, 0, edgeVertexBuffer);
        GLES20.glDrawElements(GLES20.GL_LINES, edgeIndexCount, GLES20.GL_UNSIGNED_SHORT, edgeIndexBuffer);
        GLES20.glDisableVertexAttribArray(linePos);
    }
}