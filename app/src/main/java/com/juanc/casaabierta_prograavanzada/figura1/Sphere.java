package com.juanc.casaabierta_prograavanzada.figura1;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

public class Sphere {
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer; // NUEVO: Búfer para almacenar las normales
    private final int mProgram;
    private int mPositionHandle;
    private int mNormalHandle;       // NUEVO: Handle para el atributo de normales
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private int mLightPosHandle;      // NUEVO: Handle para la posición de la luz
    private int vertexCount;

    float[] colorHelado = {1.0f, 0.8f, 0.85f, 1.0f};

    // SHADERS ACTUALIZADOS: Ahora calculan matemáticamente el rebote de la luz en el Vertex Shader
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec3 vNormal;" +      // Recibe la normal del vértice
                    "uniform vec3 uLightPos;" +      // Recibe la posición de la luz
                    "uniform vec4 vColor;" +
                    "varying vec4 vOutColor;" +      // Pasa el color calculado al Fragment Shader
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  vec3 normal = normalize(vNormal);" +
                    "  vec3 lightDir = normalize(uLightPos - vPosition.xyz);" +
                    "  float diffuse = max(dot(normal, lightDir), 0.15);" + // 0.15 de luz ambiental mínima para que no sea negro total
                    "  vOutColor = vColor * diffuse;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 vOutColor;" +      // Recibe el color iluminado
                    "void main() {" +
                    "  gl_FragColor = vOutColor;" +
                    "}";

    public Sphere(float radius, int rings, int sectors) {

        float[] vertices = new float[rings * sectors * 2 * 3];
        float[] normales = new float[rings * sectors * 2 * 3]; // NUEVO: Arreglo temporal de normales
        int vIdx = 0;

        for (int r = 0; r < rings; r++) {
            for (int s = 0; s < sectors; s++) {
                float phi = (float) (Math.PI * r / (rings - 1));
                float theta = (float) (2.0 * Math.PI * s / (sectors - 1));

                float x1 = (float) (Math.sin(phi) * Math.cos(theta));
                float y1 = (float) Math.cos(phi);
                float z1 = (float) (Math.sin(phi) * Math.sin(theta));

                vertices[vIdx] = x1 * radius;
                vertices[vIdx + 1] = y1 * radius;
                vertices[vIdx + 2] = z1 * radius;

                // En una esfera, la dirección de la normal es la misma posición sin escalar (radio = 1)
                normales[vIdx] = x1;
                normales[vIdx + 1] = y1;
                normales[vIdx + 2] = z1;
                vIdx += 3;

                float phi2 = (float) (Math.PI * (r + 1) / (rings - 1));
                float x2 = (float) (Math.sin(phi2) * Math.cos(theta));
                float y2 = (float) Math.cos(phi2);
                float z2 = (float) (Math.sin(phi2) * Math.sin(theta));

                vertices[vIdx] = x2 * radius;
                vertices[vIdx + 1] = y2 * radius;
                vertices[vIdx + 2] = z2 * radius;

                normales[vIdx] = x2;
                normales[vIdx + 1] = y2;
                normales[vIdx + 2] = z2;
                vIdx += 3;
            }
        }

        vertexCount = vIdx / 3;

        // Búfer de Vértices
        ByteBuffer bb = ByteBuffer.allocateDirect(vIdx * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // NUEVO: Inicializar el búfer nativo para las normales
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

    // MODIFICADO: Ahora el método draw pide obligatoriamente el arreglo con las coordenadas de la luz
    public void draw(float[] mvpMatrix, float[] lightPos) {
        GLES20.glUseProgram(mProgram);

        // Enlazar Vértices
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false, 3 * 4, vertexBuffer);

        // NUEVO: Enlazar Normales
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal");
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glVertexAttribPointer(mNormalHandle, 3,
                GLES20.GL_FLOAT, false, 3 * 4, normalBuffer);

        // Asignar Color Interno
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, colorHelado, 0);

        // NUEVO: Pasar la posición de la luz al shader
        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "uLightPos");
        GLES20.glUniform3fv(mLightPosHandle, 1, lightPos, 0);

        // Asignar Matriz MVP
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);

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