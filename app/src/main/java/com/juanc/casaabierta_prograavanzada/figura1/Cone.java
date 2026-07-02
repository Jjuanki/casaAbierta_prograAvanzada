package com.juanc.casaabierta_prograavanzada.figura1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

public class Cone {
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer; // NUEVO: Búfer para almacenar las normales del cono
    private final int mProgram;
    private int mPositionHandle;
    private int mNormalHandle;       // NUEVO: Handle para el atributo de normales
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private int mLightPosHandle;      // NUEVO: Handle para la posición de la luz

    private final int COORD_PER_VERTEX = 3;
    private float[] coneVertices;

    private float[] colorCono = {0.85f, 0.65f, 0.4f, 1.0f};

    // SHADERS ACTUALIZADOS: Calculan el rebote de luz por cada cara del cono
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec3 vNormal;" +      // Recibe la normal del vértice
                    "uniform vec3 uLightPos;" +      // Recibe la posición de la luz
                    "uniform vec4 vColor;" +
                    "varying vec4 vOutColor;" +      // Envía el color iluminado al Fragment Shader
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  vec3 normal = normalize(vNormal);" +
                    "  vec3 lightDir = normalize(uLightPos - vPosition.xyz);" +
                    "  float diffuse = max(dot(normal, lightDir), 0.15);" + // Mínimo 0.15 de luz ambiental
                    "  vOutColor = vColor * diffuse;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 vOutColor;" +      // Recibe el color iluminado
                    "void main() {" +
                    "  gl_FragColor = vOutColor;" +
                    "}";

    public Cone(int points, float radius, float height) {

        coneVertices = new float[(points + 2) * 3];
        float[] normales = new float[(points + 2) * 3]; // NUEVO: Arreglo para normales

        // Vértice 0: La punta inferior del cono
        coneVertices[0] = 0.0f;
        coneVertices[1] = -height / 2;
        coneVertices[2] = 0.0f;

        // La punta apunta hacia abajo, su normal inicial va hacia abajo en el eje Y
        normales[0] = 0.0f;
        normales[1] = -1.0f;
        normales[2] = 0.0f;

        // Vértices de la base circular superior y sus respectivas normales inclinadas
        for (int i = 0; i <= points; i++) {
            float angle = (float) (2 * Math.PI * i / points);
            int idx = (i + 1) * 3;

            coneVertices[idx] = (float) (radius * Math.cos(angle));
            coneVertices[idx + 1] = height / 2;
            coneVertices[idx + 2] = (float) (radius * Math.sin(angle));

            // Calculamos la dirección hacia afuera de las paredes laterales del cono
            normales[idx] = (float) Math.cos(angle);
            normales[idx + 1] = 0.3f; // Componente de inclinación hacia arriba para suavizar el cono
            normales[idx + 2] = (float) Math.sin(angle);
        }

        // Búfer de Vértices
        ByteBuffer bb = ByteBuffer.allocateDirect(coneVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(coneVertices);
        vertexBuffer.position(0);

        // NUEVO: Inicializar el búfer para las normales del cono
        ByteBuffer nbb = ByteBuffer.allocateDirect(normales.length * 4);
        nbb.order(ByteOrder.nativeOrder());
        normalBuffer = nbb.asFloatBuffer();
        normalBuffer.put(normales);
        normalBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    // MODIFICADO: Ahora el método draw pide la matriz MVP y las coordenadas [x, y, z] de la luz
    public void draw(float[] mvpMatrix, float[] lightPos) {
        GLES20.glUseProgram(mProgram);

        // Enlazar Vértices
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORD_PER_VERTEX,
                GLES20.GL_FLOAT, false, COORD_PER_VERTEX * 4, vertexBuffer);

        // NUEVO: Enlazar Normales
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal");
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glVertexAttribPointer(mNormalHandle, 3,
                GLES20.GL_FLOAT, false, 3 * 4, normalBuffer);

        // Asignar Color Interno
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, colorCono, 0);

        // NUEVO: Enviar la posición de la luz al shader
        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "uLightPos");
        GLES20.glUniform3fv(mLightPosHandle, 1, lightPos, 0);

        // Asignar Matriz MVP
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, coneVertices.length / 3);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mNormalHandle); // Deshabilitar normales al terminar
    }

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}