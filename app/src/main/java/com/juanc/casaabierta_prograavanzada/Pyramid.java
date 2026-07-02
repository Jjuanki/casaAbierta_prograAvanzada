package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Pyramid {
    private final FloatBuffer vertexBuffer, normalBuffer;
    private final int vertexCount = 18; // 6 triángulos * 3 vértices
    private final int COORDS_PER_VERTEX = 3;
    private final int program;

    private float spotlightAngle = 20f, angleDelta = 0.1f;
    private final float minAngle = 2f, maxAngle = 20f;

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
                    "varying vec3 vNormal;" +
                    "varying vec3 vPosition;" +
                    "void main() {" +
                    "  vec3 normalN = normalize(vNormal);" +
                    "  vec3 lightDir = normalize(uLightPos - vPosition);" +
                    "  float theta = dot(lightDir, normalize(-uLightDir));" +
                    "  vec3 ambient = vec3(0.1);" +
                    "  vec3 diffuse = vec3(0.0);" +
                    "  vec3 specular = vec3(0.0);" +
                    "  if (theta > uCutOff) {" +
                    "    float diff = max(dot(normalN, lightDir), 0.0);" +
                    "    diffuse = diff * vec3(1.0, 0.7, 0.3);" +
                    "    vec3 viewDir = normalize(uEyePos - vPosition);" +
                    "    vec3 reflectDir = reflect(-lightDir, normalN);" +
                    "    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);" +
                    "    specular = spec * vec3(1.0);" +
                    "  }" +
                    "  vec3 result = ambient + diffuse + specular;" +
                    "  gl_FragColor = vec4(result, 1.0);" +
                    "}";

    private final float[] vertices = {
            // Base
            -1,-1,-1,  1,-1,-1,  1,-1,1,
            -1,-1,-1,  1,-1,1,  -1,-1,1,
            // Caras
            -1,-1,-1,   0,1,0,   1,-1,-1,
            1,-1,-1,   0,1,0,   1,-1,1,
            1,-1,1,    0,1,0,  -1,-1,1,
            -1,-1,1,    0,1,0,  -1,-1,-1
    };

    private final float[] normals = {
            // Base normales hacia abajo
            0,-1,0, 0,-1,0, 0,-1,0,
            0,-1,0, 0,-1,0, 0,-1,0,
            // Caras normales aproximadas
            0,0.707f,-0.707f,  0,0.707f,-0.707f,  0,0.707f,-0.707f,
            0.707f,0.707f,0,   0.707f,0.707f,0,   0.707f,0.707f,0,
            0,0.707f,0.707f,   0,0.707f,0.707f,   0,0.707f,0.707f,
            -0.707f,0.707f,0,  -0.707f,0.707f,0,  -0.707f,0.707f,0
    };

    public Pyramid() {
        ByteBuffer vb = ByteBuffer.allocateDirect(vertices.length * 4);
        vb.order(ByteOrder.nativeOrder());
        vertexBuffer = vb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer nb = ByteBuffer.allocateDirect(normals.length * 4);
        nb.order(ByteOrder.nativeOrder());
        normalBuffer = nb.asFloatBuffer();
        normalBuffer.put(normals);
        normalBuffer.position(0);

        program = ShaderUtils.createProgram(vertexShaderCode,fragmentShaderCode);
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix) {

        GLES20.glUseProgram(program);

        int aPos = GLES20.glGetAttribLocation(program, "aPosition");
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        int aNorm = GLES20.glGetAttribLocation(program, "aNormal");
        GLES20.glEnableVertexAttribArray(aNorm);
        GLES20.glVertexAttribPointer(aNorm, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);

        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uLightPos"), 3f, 3f, 3f);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uLightDir"), -1f, -1f, -1f);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uEyePos"), 0f, 2f, 6f);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uModelMatrix"), 1, false, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uMVPMatrix"), 1, false, mvpMatrix, 0);
        int uCutOffHandle = GLES20.glGetUniformLocation(program, "uCutOff");
        float cutOff = (float) Math.cos(Math.toRadians(spotlightAngle));
        GLES20.glUniform1f(uCutOffHandle, cutOff);
        // ANIMAR CONO
        spotlightAngle += angleDelta;
        if (spotlightAngle > maxAngle || spotlightAngle < minAngle)
            angleDelta *= -1;


        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aNorm);
    }
}
