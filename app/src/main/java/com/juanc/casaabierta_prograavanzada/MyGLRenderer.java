package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.juanc.casaabierta_prograavanzada.figura1.Cone;
import com.juanc.casaabierta_prograavanzada.figura1.Sphere;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private Cone mCone;
    private Sphere mMainScoop;
    private Sphere mEarScoop;

    private final float[] mModelMatrix = new float[16];
    private final float[] mTemporaryMatrix = new float[16];

    private HemiSphere hemisphere;
    private Cylinder cylinder;

    private float[] viewMatrix = new float[16];
    private float[] projMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    // Rotacion del modelo, controlada por MyGLSurfaceView (1 dedo)
    public float mAngleX = 0f;
    public float mAngleY = 0f;

    // Angulo de la luz, controlado por MyGLSurfaceView (2 dedos)
    public float lightAngleX = 30f; // "altura" de la luz (latitud)
    public float lightAngleY = 45f; // giro alrededor de la escena (longitud)
    public float spotlightAngle = 20f; // Tamaño del foco (cono)
    private static final float LIGHT_RADIUS = 6f;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.4f, 0.6f, 0.7f, 1f); // color cielo de fondo
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        hemisphere = new HemiSphere();
        cylinder = new Cylinder();

        mCone = new Cone(30, 0.5f, 1.2f);
        mMainScoop = new Sphere(0.6f, 30, 30);
        mEarScoop = new Sphere(0.35f, 20, 20);
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
                0f, 3f, 8f,
                0f, -1f, 0f,
                0f, 1f, 0f);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, mAngleY, 1f, 0f, 0f);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0);

        // ---- Calcular la posicion de la luz en base a los angulos controlados por el usuario ----
        // Limitar la latitud para que no se "invierta" al pasar los polos
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

        // ... (Aquí va tu código que calcula la posición de la luz: lightX, lightY, lightZ, lightPos) ...

// ==========================================
// 1. CONO (BASE)
// ==========================================
        android.opengl.Matrix.setIdentityM(mModelMatrix, 0);
        android.opengl.Matrix.translateM(mModelMatrix, 0, 0.0f, 0.6f, 0.0f);
        android.opengl.Matrix.multiplyMM(mTemporaryMatrix, 0, mvpMatrix, 0, mModelMatrix, 0);
        mCone.draw(mTemporaryMatrix, lightPos); // <--- Le pasamos tu variable lightPos

// ==========================================
// 2. BOLA CENTRAL (CABEZA)
// ==========================================
        android.opengl.Matrix.setIdentityM(mModelMatrix, 0);
        android.opengl.Matrix.translateM(mModelMatrix, 0, 0.0f, 1.2f, 0.0f);
        android.opengl.Matrix.multiplyMM(mTemporaryMatrix, 0, mvpMatrix, 0, mModelMatrix, 0);
        mMainScoop.draw(mTemporaryMatrix, lightPos); // <--- Le pasamos tu variable lightPos

// ==========================================
// 3. OREJA IZQUIERDA
// ==========================================
        android.opengl.Matrix.setIdentityM(mModelMatrix, 0);
        android.opengl.Matrix.translateM(mModelMatrix, 0, -0.45f, 1.7f, 0.0f);
        android.opengl.Matrix.multiplyMM(mTemporaryMatrix, 0, mvpMatrix, 0, mModelMatrix, 0);
        mEarScoop.draw(mTemporaryMatrix, lightPos); // <--- Le pasamos tu variable lightPos

// ==========================================
// 4. OREJA DERECHA
// ==========================================
        android.opengl.Matrix.setIdentityM(mModelMatrix, 0);
        android.opengl.Matrix.translateM(mModelMatrix, 0, 0.45f, 1.7f, 0.0f);
        android.opengl.Matrix.multiplyMM(mTemporaryMatrix, 0, mvpMatrix, 0, mModelMatrix, 0);
        mEarScoop.draw(mTemporaryMatrix, lightPos); // <--- Le pasamos tu variable lightPos
    }
}