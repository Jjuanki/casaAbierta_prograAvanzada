package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;

public class ShaderUtils {
    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static int createProgram(String vertex, String fragment){
        int vertexS = loadShader(GLES20.GL_VERTEX_SHADER,vertex);
        int fragmentS = loadShader(GLES20.GL_FRAGMENT_SHADER,fragment);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexS);
        GLES20.glAttachShader(program, fragmentS);
        GLES20.glLinkProgram(program);
        return program;
    }
}
