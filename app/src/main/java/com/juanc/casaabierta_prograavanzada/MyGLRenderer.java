package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.juanc.casaabierta_prograavanzada.Dibujos.Butterfly;
import com.juanc.casaabierta_prograavanzada.Dibujos.PixelArtFigure;
import com.juanc.casaabierta_prograavanzada.Dibujos.Sunflower;

import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private final float[] mModelMatrix = new float[16];
    private final float[] mTemporaryMatrix = new float[16];
    private final float[] localMatrix = new float[16]; // transform local (traslacion+escala) de cada pieza

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

    // Tamaño del cono de luz (2 dedos, pellizcar, o con el SeekBar del boton)
    public float spotlightAngle = 20f;
    public static final float MIN_SPOT_ANGLE = 5f;
    public static final float MAX_SPOT_ANGLE = 60f;

    // Prender/apagar la luz (controlado desde el boton en MainActivity)
    public volatile boolean isLightOn = true;

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

        float cellSize = 0.07f;   // antes 0.12f — lo hace más pequeño
        float depth = 0.07f;      // antes 0.12f — mismo factor para que no se vea "aplastado"
        float originX = -(rows[0].length() * cellSize) / 2f - 0.6f;
        float originY = 1.3f;      // antes 1.9f — un poco más abajo, ya que ahora es más chico
        float originZ = -0.6f;

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

        // Si la luz esta apagada, el cono efectivo es 0: como las figuras no tienen
        // termino ambiental, quedan completamente negras (luz "apagada").
        float effectiveSpotAngle = isLightOn ? spotlightAngle : 0f;

        hemisphere.draw(mvpMatrix, modelMatrix, lightPos, effectiveSpotAngle);
        cylinder.draw(mvpMatrix, modelMatrix, lightPos, effectiveSpotAngle);
        dragon.draw(mvpMatrix, modelMatrix, lightPos, effectiveSpotAngle);

        // ---- Pared 1 ----
        // NOTA: escalas x3 respecto a la version anterior porque el Cube unificado
        // ahora es de -0.5 a 0.5 (antes era de -1.5 a 1.5 en la version de "separation").
        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, 0f, 0.5f, 0f);
        Matrix.scaleM(localMatrix, 0, 2.7f, 2.1f, 0.06f);
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, localMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, mvpMatrix, 0, localMatrix, 0);
        pared.draw(mTemporaryMatrix, mModelMatrix, lightPos, effectiveSpotAngle);

        // ---- Pared 2 (perpendicular a la primera) ----
        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, 0f, 0.8f, 0f);
        Matrix.rotateM(localMatrix, 0, 90f, 0f, 0f, 1f);
        Matrix.scaleM(localMatrix, 0, 1.5f, 0.06f, 2.7f);
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, localMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, mvpMatrix, 0, localMatrix, 0);
        pared.draw(mTemporaryMatrix, mModelMatrix, lightPos, effectiveSpotAngle);

        // ---- Mariposa en otro cuadrante ----
        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, .6f, 0.5f, 0.6f);
        Matrix.scaleM(localMatrix, 0, 0.6f, 0.6f, 0.6f);
        Matrix.multiplyMM(butterflyModel, 0, modelMatrix, 0, localMatrix, 0);
        butterfly.draw(viewMatrix, projMatrix, butterflyModel, lightPos, effectiveSpotAngle);

        // ---- Girasol ----
        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, -0.65f, 1.0f, 0.60f);
        Matrix.scaleM(localMatrix, 0, 0.58f, 0.58f, 0.58f);
        Matrix.multiplyMM(sunflowerModel, 0, modelMatrix, 0, localMatrix, 0);
        sunflower.draw(viewMatrix, projMatrix, sunflowerModel, lightPos, effectiveSpotAngle);
    }
}