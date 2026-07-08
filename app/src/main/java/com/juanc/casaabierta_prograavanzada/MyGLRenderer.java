package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.juanc.casaabierta_prograavanzada.dragon.PixelArtFigure;
import com.juanc.casaabierta_prograavanzada.separation.Cube;

import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private final float[] mModelMatrix = new float[16];
    private final float[] mTemporaryMatrix = new float[16];

    private HemiSphere hemisphere;
    private Cylinder cylinder;
    private PixelArtFigure dragon;

    private Sunflower sunflower;
    private final float[] sunflowerModel = new float[16];
    private Cube pared;
    private Butterfly butterfly;
    private final float[] butterflyModel = new float[16];

    private float[] viewMatrix = new float[16];
    private float[] projMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    // Rotacion del modelo (controlada con 1 dedo, ver MyGLSurfaceView)
    public float mAngleX = 0f;
    public float mAngleY = 0f;

    // Pan del escenario en X/Y. Disponible por si conectas algun gesto a esto.
    public float mPanX = 0f;
    public float mPanY = 0f;

    // Zoom del escenario. Disponible por si conectas algun gesto a esto (ej. 3 dedos).
    public float mScale = 1f;
    public static final float MIN_SCALE = 0.3f;
    public static final float MAX_SCALE = 3.0f;

    // Posicion de la luz (2 dedos, arrastrar = mover a cualquier parte de la pantalla)
    public float lightAngleX = 30f; // "altura" de la luz (latitud)
    public float lightAngleY = 45f; // giro alrededor de la escena (longitud)

    // Tamaño del cono de luz (2 dedos, pellizcar)
    public float spotlightAngle = 20f;
    public static final float MIN_SPOT_ANGLE = 5f;
    public static final float MAX_SPOT_ANGLE = 60f;

    private static final float LIGHT_RADIUS = 6f;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f); // fondo negro: todo se revela con la luz
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        hemisphere = new HemiSphere();
        cylinder = new Cylinder();
        dragon = buildDragonFigure();

        sunflower = new Sunflower();
        pared = new Cube();
        butterfly = new Butterfly();
    }

    /**
     * Dragoncito (Bub)
     */
    private PixelArtFigure buildDragonFigure() {
        Map<Character, float[]> palette = new HashMap<>();
        palette.put('Y', new float[]{0.97f, 0.77f, 0.13f, 1.0f});
        palette.put('G', new float[]{0.54f, 0.76f, 0.35f, 1.0f});
        palette.put('W', new float[]{1.00f, 1.00f, 1.00f, 1.0f});
        palette.put('K', new float[]{0.05f, 0.05f, 0.05f, 1.0f});
        palette.put('R', new float[]{0.90f, 0.32f, 0.33f, 1.0f});

        String[] rows = {
                "        Y       ",
                "       YYY      ",
                "   YYYYGGGGG    ",
                "    YYGGGGGGG   ",
                "     GGGGWWGWG  ",
                "  YYYGGGWWKGKW  ",
                "   YGGGGWWKGKW  ",
                "    GGYYWWKGKWG ",
                "    GYYYWWKGKWY ",
                "    GGKYGWWGWGG ",
                "    GRGKKKKWKK  ",
                "   GRRRGGGGGGG  ",
                "G  GRRRGGWWWW   ",
                "GYYGRRGGWWWWWW  ",
                " GGGGGGRRRWWWW  ",
                "  GGGGRRRRRWWRRR"
        };

        float cellSize = 0.12f;
        float depth = 0.12f;
        // Centrado horizontalmente (16 columnas * cellSize), parado sobre la isla (tapa en y=0)
        float originX = -(rows[0].length() * cellSize) / 2f;
        float originY = 1.9f;
        float originZ = 0f;

        return new PixelArtFigure(rows, palette, cellSize, depth, originX, originY, originZ);
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

        // Orden: pan -> escala -> rotacion.
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, mPanX, mPanY, 0f);
        Matrix.scaleM(modelMatrix, 0, mScale, mScale, mScale);
        Matrix.rotateM(modelMatrix, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, mAngleY, 1f, 0f, 0f);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0);

        // ---- Calcular la posicion de la luz en base a los angulos controlados por el usuario ----
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
        dragon.draw(mvpMatrix, modelMatrix, lightPos, spotlightAngle);

        // ---- Pared 1 ----
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(mModelMatrix, 0, mAngleY, 1f, 0f, 0f);
        Matrix.translateM(mModelMatrix, 0, 0f, 0.5f, 0f);
        Matrix.scaleM(mModelMatrix, 0, 0.9f, 0.7f, 0.02f);
        Matrix.multiplyMM(mTemporaryMatrix, 0, viewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, projMatrix, 0, mTemporaryMatrix, 0);
        pared.draw(mTemporaryMatrix, mModelMatrix, lightPos, spotlightAngle);

        // ---- Pared 2 (perpendicular a la primera) ----
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(mModelMatrix, 0, mAngleY, 1f, 0f, 0f);
        Matrix.translateM(mModelMatrix, 0, 0f, 0.8f, 0f);
        Matrix.rotateM(mModelMatrix, 0, 90f, 0f, 0f, 1f);
        Matrix.scaleM(mModelMatrix, 0, 0.5f, 0.02f, 0.9f);
        Matrix.multiplyMM(mTemporaryMatrix, 0, viewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, projMatrix, 0, mTemporaryMatrix, 0);
        pared.draw(mTemporaryMatrix, mModelMatrix, lightPos, spotlightAngle);

        // ---- Mariposa en otro cuadrante ----
        Matrix.setIdentityM(butterflyModel, 0);
        Matrix.rotateM(butterflyModel, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(butterflyModel, 0, mAngleY, 1f, 0f, 0f);
        Matrix.translateM(butterflyModel, 0, .6f, 0.5f, 0.6f);
        Matrix.scaleM(butterflyModel, 0, 0.6f, 0.6f, 0.6f);
        butterfly.draw(viewMatrix, projMatrix, butterflyModel, lightPos, spotlightAngle);

        // ---- Girasol ----
        Matrix.setIdentityM(sunflowerModel, 0);
        Matrix.rotateM(sunflowerModel, 0, mAngleX, 0f, 1f, 0f);
        Matrix.rotateM(sunflowerModel, 0, mAngleY, 1f, 0f, 0f);
        Matrix.translateM(sunflowerModel, 0, -0.65f, 1.0f, 0.60f);
        Matrix.scaleM(sunflowerModel, 0, 0.58f, 0.58f, 0.58f);
        sunflower.draw(viewMatrix, projMatrix, sunflowerModel, lightPos, spotlightAngle);
    }
}