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

  
    private final FloatBuffer domeVertexBuffer;
    private final FloatBuffer domeNormalBuffer;
    private final ShortBuffer[] bandIndexBuffers;
    private final int[] bandIndexCounts;
    private final float[][] bandColors;

  
    private final FloatBuffer capVertexBuffer;
    private final FloatBuffer capNormalBuffer;
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
        litProgram = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);
        lineProgram = ShaderUtils.createProgram(lineVertexShaderCode, lineFragmentShaderCode);

       
        bandColors = new float[][]{
                {0.82f, 0.35f, 0.10f, 1.0f}, // naranja
                {0.96f, 0.93f, 0.85f, 1.0f}, // crema
                {0.82f, 0.35f, 0.10f, 1.0f}, // naranja
                {0.96f, 0.93f, 0.85f, 1.0f}, // crema
                {0.20f, 0.40f, 0.60f, 1.0f}, // azul
                {0.08f, 0.18f, 0.32f, 1.0f}  // azul oscuro (punta)
        };

        // ---- Vertices y normales del domo (esfera centrada en el origen, radio RADIUS) ----
        float[] domeVerts = new float[(LAT_SEGMENTS + 1) * (LON_SEGMENTS + 1) * 3];
        float[] domeNorms = new float[(LAT_SEGMENTS + 1) * (LON_SEGMENTS + 1) * 3];
        int idx = 0;
        for (int lat = 0; lat <= LAT_SEGMENTS; lat++) {
            float phi = (float) (Math.PI / 2 * lat / LAT_SEGMENTS); 
            float y = -RADIUS * (float) Math.sin(phi); 
            float r = RADIUS * (float) Math.cos(phi);

            for (int lon = 0; lon <= LON_SEGMENTS; lon++) {
                float theta = (float) (2 * Math.PI * lon / LON_SEGMENTS);
                float x = r * (float) Math.cos(theta);
                float z = r * (float) Math.sin(theta);

                domeVerts[idx] = x;
                domeVerts[idx + 1] = y;
                domeVerts[idx + 2] = z;

         
                domeNorms[idx] = x / RADIUS;
                domeNorms[idx + 1] = y / RADIUS;
                domeNorms[idx + 2] = z / RADIUS;

                idx += 3;
            }
        }

        domeVertexBuffer = toFloatBuffer(domeVerts);
        domeNormalBuffer = toFloatBuffer(domeNorms);

       
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
            bandIndexBuffers[band] = toShortBuffer(bandIndices);
        }

       
        ringLineBuffers = new ShortBuffer[NUM_BANDS];
        for (int band = 0; band < NUM_BANDS; band++) {
            int lat = band * SEGMENTS_PER_BAND;
            short[] ringIndices = new short[ringLineCount];
            for (int lon = 0; lon <= LON_SEGMENTS; lon++) {
                ringIndices[lon] = (short) (lat * (LON_SEGMENTS + 1) + lon);
            }
            ringLineBuffers[band] = toShortBuffer(ringIndices);
        }

        float[] capVerts = new float[(LON_SEGMENTS + 2) * 3];
        float[] capNorms = new float[(LON_SEGMENTS + 2) * 3];
        idx = 0;
        capVerts[idx] = 0f;
        capVerts[idx + 1] = 0f;
        capVerts[idx + 2] = 0f;
        capNorms[idx] = 0f;
        capNorms[idx + 1] = 1f;
        capNorms[idx + 2] = 0f;
        idx += 3;
        for (int lon = 0; lon <= LON_SEGMENTS; lon++) {
            float theta = (float) (2 * Math.PI * lon / LON_SEGMENTS);
            capVerts[idx] = RADIUS * (float) Math.cos(theta);
            capVerts[idx + 1] = 0f;
            capVerts[idx + 2] = RADIUS * (float) Math.sin(theta);
            capNorms[idx] = 0f;
            capNorms[idx + 1] = 1f;
            capNorms[idx + 2] = 0f;
            idx += 3;
        }

        capVertexBuffer = toFloatBuffer(capVerts);
        capNormalBuffer = toFloatBuffer(capNorms);

        int capVertexCount = capVerts.length / 3;
        short[] capIndices = new short[capVertexCount];
        for (short i = 0; i < capVertexCount; i++) capIndices[i] = i;
        capIndexCount = capIndices.length;
        capIndexBuffer = toShortBuffer(capIndices);
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

        // Tapa plana de arriba
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, capVertexBuffer);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 0, capNormalBuffer);
        GLES20.glUniform4fv(uColorHandle, 1, capColor, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_FAN, capIndexCount, GLES20.GL_UNSIGNED_SHORT, capIndexBuffer);

        // Domo: una banda de color a la vez
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, domeVertexBuffer);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 0, domeNormalBuffer);
        for (int band = 0; band < NUM_BANDS; band++) {
            GLES20.glUniform4fv(uColorHandle, 1, bandColors[band], 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, bandIndexCounts[band], GLES20.GL_UNSIGNED_SHORT, bandIndexBuffers[band]);
        }

        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aNormal);

        // Lineas negras separando cada banda de color 
        GLES20.glUseProgram(lineProgram);
        int linePos = GLES20.glGetAttribLocation(lineProgram, "vPosition");
        int lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "vColor");
        int lineMvpHandle = GLES20.glGetUniformLocation(lineProgram, "uMVPMatrix");

        GLES20.glUniformMatrix4fv(lineMvpHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4fv(lineColorHandle, 1, lineColor, 0);
        GLES20.glLineWidth(3f);

        GLES20.glEnableVertexAttribArray(linePos);
        GLES20.glVertexAttribPointer(linePos, 3, GLES20.GL_FLOAT, false, 0, domeVertexBuffer);
        for (int band = 0; band < NUM_BANDS; band++) {
            GLES20.glDrawElements(GLES20.GL_LINE_LOOP, ringLineCount, GLES20.GL_UNSIGNED_SHORT, ringLineBuffers[band]);
        }
        GLES20.glDisableVertexAttribArray(linePos);
    }
}
