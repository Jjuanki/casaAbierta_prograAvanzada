package com.juanc.casaabierta_prograavanzada;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Cylinder {

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

    // ---- Spotlight (mismo esquema que Pyramid) ----
    // private float spotlightAngle = 20f, angleDelta = 0.1f;
    // private final float minAngle = 2f, maxAngle = 20f;

    public Cylinder() {
        program = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);

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

        // La luz apunta siempre hacia el centro de la escena (como una linterna que sigue al objeto)
        float dirX = -lightPos[0], dirY = -lightPos[1], dirZ = -lightPos[2];
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len > 0f) { dirX /= len; dirY /= len; dirZ /= len; }

        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uLightPos"), lightPos[0], lightPos[1], lightPos[2]);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uLightDir"), dirX, dirY, dirZ);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uEyePos"), 0f, 1f, 8f);
        float cutOff = (float) Math.cos(Math.toRadians(spotlightAngle));
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uCutOff"), cutOff);

        // spotlightAngle += angleDelta;
        // if (spotlightAngle > maxAngle || spotlightAngle < minAngle) angleDelta *= -1;

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