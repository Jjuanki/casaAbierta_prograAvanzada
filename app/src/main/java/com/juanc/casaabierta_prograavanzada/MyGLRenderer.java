package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.juanc.casaabierta_prograavanzada.separation.Cube;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private final float[] mModelMatrix = new float[16];
    private final float[] mTemporaryMatrix = new float[16];
    private Sunflower sunflower;
    private final float[] sunflowerModel = new float[16];

    private HemiSphere hemisphere;
    private Cylinder cylinder;
    private Cube pared;
    private final float[] vaderModel = new float[16];
    private Butterfly butterfly;
    private final float[] butterflyModel = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    public float mAngleX = 0f;
    public float mAngleY = 0f;

    public float lightAngleX = 30f;
    public float lightAngleY = 45f;
    public float spotlightAngle = 20f;
    private static final float LIGHT_RADIUS = 6f;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.4f, 0.6f, 0.7f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        hemisphere = new HemiSphere();
        cylinder = new Cylinder();
        sunflower = new Sunflower();


        pared = new Cube();
        butterfly = new Butterfly();
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

        Matrix.setLookAtM(viewMatrix, 0,
                0f, 3f, 9f,
                0f, -1f, 0f,
                0f, 1f, 0f);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, mAngleY, 1f, 0f, 0f);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0);

        if (lightAngleX > 89f) lightAngleX = 89f;
        if (lightAngleX < -89f) lightAngleX = -89f;

        float radLat = (float) Math.toRadians(lightAngleX);
        float radLon = (float) Math.toRadians(lightAngleY);

        float lightX = LIGHT_RADIUS * (float) (Math.cos(radLat) * Math.sin(radLon));
        float lightY = LIGHT_RADIUS * (float) Math.sin(radLat);
        float lightZ = LIGHT_RADIUS * (float) (Math.cos(radLat) * Math.cos(radLon));
        float[] lightPos = {lightX, lightY, lightZ};

        hemisphere.draw(mvpMatrix, modelMatrix, lightPos, spotlightAngle);
        cylinder.draw(mvpMatrix, modelMatrix, lightPos, spotlightAngle);


        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(mModelMatrix, 0, mAngleY, 1f, 0f, 0f);
        Matrix.translateM(mModelMatrix, 0, 0f, 0.5f, 0f);
        Matrix.scaleM(mModelMatrix, 0, 0.9f, 0.7f, 0.02f);
        Matrix.multiplyMM(mTemporaryMatrix, 0, viewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, projMatrix, 0, mTemporaryMatrix, 0);
        pared.draw(mTemporaryMatrix, mModelMatrix, lightPos, spotlightAngle);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(mModelMatrix, 0, mAngleY, 1f, 0f, 0f);
        Matrix.translateM(mModelMatrix, 0, 0f, 0.8f, 0f);
        Matrix.rotateM(mModelMatrix, 0, 90f, 0f, 0f, 1f);   // rota 90° para que quede perpendicular al primero
        Matrix.scaleM(mModelMatrix, 0, 0.5f, 0.02f, 0.9f);
        Matrix.multiplyMM(mTemporaryMatrix, 0, viewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, projMatrix, 0, mTemporaryMatrix, 0);
        pared.draw(mTemporaryMatrix, mModelMatrix, lightPos, spotlightAngle);

        // ---- Mariposa en otro cuadrante ----
        // (-0.6, 0.6) cae en el cuadrante -X/+Z; ajusta según tus paredes.
        Matrix.setIdentityM(butterflyModel, 0);
        Matrix.rotateM(butterflyModel, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(butterflyModel, 0, mAngleY, 1f, 0f, 0f);
        Matrix.translateM(butterflyModel, 0, .6f, 0.5f, 0.6f);
        Matrix.scaleM(butterflyModel, 0, 0.6f, 0.6f, 0.6f);
        butterfly.draw(viewMatrix, projMatrix, butterflyModel, lightPos, spotlightAngle);

        // ---- Girasol en el cuadrante
        Matrix.setIdentityM(sunflowerModel, 0);
        Matrix.rotateM(sunflowerModel, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(sunflowerModel, 0, mAngleY, 1f, 0f, 0f);
        Matrix.translateM(sunflowerModel, 0, -0.65f, 1.0f, 0.60f);
        Matrix.scaleM(sunflowerModel, 0, 0.58f, 0.58f, 0.58f);
        sunflower.draw(viewMatrix, projMatrix, sunflowerModel, lightPos, spotlightAngle);


    }
}