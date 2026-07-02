package com.juanc.casaabierta_prograavanzada;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private HemiSphere hemisphere;
    private Cylinder cylinder;

    private float[] viewMatrix = new float[16];
    private float[] projMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] modelMatrix = new float[16];
    public volatile float mAngleX = 0;
    public volatile float mAngleY = 0;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Cambiado a un azul cielo para que ambiente la plataforma de Kamisama
        GLES20.glClearColor(0.0f, 0.65f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        hemisphere = new HemiSphere();
        cylinder = new Cylinder();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.perspectiveM(projMatrix, 0, 45f, ratio, 1f, 100f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Configuración de la cámara que ya tenías instalada
        Matrix.setLookAtM(viewMatrix, 0,
                0f, 1f, 9f,
                0f, -1f, 0f,
                0f, 1f, 0f);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, mAngleY, 1f, 0f, 0f);
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0);
        hemisphere.draw(mvpMatrix, modelMatrix);
        cylinder.draw(mvpMatrix, modelMatrix);
    }
}