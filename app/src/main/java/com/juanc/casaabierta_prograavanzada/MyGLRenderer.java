package com.juanc.casaabierta_prograavanzada;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.juanc.casaabierta_prograavanzada.Dibujos.Butterfly;
import com.juanc.casaabierta_prograavanzada.Dibujos.PixelArtFigure;
import com.juanc.casaabierta_prograavanzada.Dibujos.Rocket;
import com.juanc.casaabierta_prograavanzada.Dibujos.SceneryProps;
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
    private Rocket rocket;
    private final float[] rocketModel = new float[16];
    private SceneryProps scenery; // rocas, flores, chatarra espacial, trigo: 1 set por cuadrante
    // ---- Fondo dinamico (skybox) y particulas de polvo brillante ----
    private Skybox skybox;
    private ParticleSystem dustParticles;    // "polvo" anclado a la figura (rota/escala con ella)
    private ParticleSystem ambientParticles; // particulas de fondo: llenan todo el escenario,
    // sin verse afectadas por el zoom/paneo de la figura
    private final float[] skyboxMvp = new float[16];
    private long lastFrameTimeNanos = 0L;
    private float elapsedTime = 0f;

    private float[] viewMatrix = new float[16];
    private float[] projMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    // Rotacion del modelo (controlada con 1 dedo, ver MyGLSurfaceView)
    public float mAngleX = 0f;
    public float mAngleY = 0f;

    // Offset de rotacion controlado por las flechas del D-pad: se SUMA a
    // mAngleX/mAngleY del giroscopio (que MainActivity reescribe en cada
    // evento del sensor), en vez de pisarlo. Asi las flechas "orbitan"
    // alrededor de la isla igual que si inclinaras el telefono, y podes
    // combinar ambos controles sin que se pisen entre si.
    public float mAngleXOffset = 0f; // orbita horizontal (eje Y) -> izquierda/derecha
    public float mAngleYOffset = 0f; // inclinacion vertical (eje X) -> arriba/abajo

    private static final float ROTATE_STEP = 2.5f;         // grados que gira por cada "tick" de flecha
    private static final float PITCH_OFFSET_LIMIT = 60f;   // limite del offset vertical (arriba/abajo), para no voltear la escena

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

    private FloorTile tileDragon;      // caverna volcanica
    private FloorTile tileRocket;      // plataforma espacial
    private FloorTile tileButterfly;   // jardin/pradera
    private FloorTile tileSunflower;   // campo soleado
    private Cube dividerBeam;

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
        rocket = new Rocket();
        scenery = new SceneryProps();

        skybox = new Skybox();

        // Polvo brillante flotando alrededor de las figuras (anclado: rota/escala con la escena).
        dustParticles = new ParticleSystem(
                140,                             // cantidad de particulas (antes 90)
                0f, 1.1f, 0.25f,                 // centro (x, y, z) de la nube
                1.8f,                             // radio horizontal (antes 1.5)
                2.4f,                             // rango vertical (antes 2.0)
                new float[]{1.0f, 0.92f, 0.7f, 0.85f}, // color calido tipo "luciernaga"
                9f                                 // tamaño del punto en pixeles

        );

        // Polvo ambiental: nube mucho mas grande que cubre TODO el escenario visible.
        // Se dibuja solo con la matriz de camara (sin mScale/mPanX/mPanY), por lo que
        // sigue llenando toda la pantalla aunque el usuario acerque o aleje la figura.
        ambientParticles = new ParticleSystem(
                160,
                0f, 0.5f, -1.5f,
                4.5f,                              // radio mucho mayor: cubre todo el diorama
                7.0f,                              // rango vertical amplio
                new float[]{0.75f, 0.85f, 1.0f, 0.5f}, // brillo frio, sutil, de fondo
                5f
        );

        // Colores elegidos para que cada cuadrante se note SIEMPRE contra el fondo
        // negro estrellado (antes el de la Mariposa era casi negro y se confundia
        // con el espacio, dando la sensacion de que no tenia piso propio / se
        // "salia" hacia el cuadrante vecino). Ademas Dragon y Mariposa ya no
        // comparten un verde parecido: Dragon = roca volcanica grisacea/rojiza,
        // Mariposa = verde pradera bien vivo.
        tileDragon    = new FloorTile(0.32f, 0.22f, 0.20f, 1f ); // roca volcanica grisacea/rojiza
        tileRocket    = new FloorTile(0.16f, 0.20f, 0.34f, 1f ); // plataforma espacial, azul oscuro (un poco mas visible que antes)
        tileButterfly = new FloorTile(0.28f, 0.62f, 0.30f, 1f ); // pradera verde vivo (antes casi negro)
        tileSunflower = new FloorTile(0.85f, 0.70f, 0.20f, 1f );
        dividerBeam   = new Cube(0.15f, 0.10f, 0.08f, 1f);

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

        // ---- Tiempo transcurrido (para animar estrellas y particulas) ----
        long now = System.nanoTime();
        float dt = (lastFrameTimeNanos == 0L) ? 0.016f : (now - lastFrameTimeNanos) / 1_000_000_000f;
        lastFrameTimeNanos = now;
        elapsedTime += dt;

        Matrix.setLookAtM(viewMatrix, 0,
                0f, 3f, 9f,
                0f, -1f, 0f,
                0f, 1f, 0f);

        // ---- Skybox: se dibuja primero, fijo (sin el modelMatrix) para que quede
        // como un fondo que no rota con la inclinacion del telefono ----
        // "reveal": 0 mientras se esta en zoom normal/acercado (se ve la caja negra
        // de siempre), y sube a 1 a medida que el usuario ACHICA la escena (pellizco
        // de 2 dedos), revelando el otro escenario (nebulosa) alrededor de la caja.
        float reveal = (1f - mScale) / (1f - MIN_SCALE);
        if (reveal < 0f) reveal = 0f;
        if (reveal > 1f) reveal = 1f;

        Matrix.multiplyMM(skyboxMvp, 0, projMatrix, 0, viewMatrix, 0);
        skybox.draw(skyboxMvp, elapsedTime, reveal);

        // Orden: pan -> escala -> rotacion.
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, mPanX, mPanY, 0f);
        Matrix.scaleM(modelMatrix, 0, mScale, mScale, mScale);
        Matrix.rotateM(modelMatrix, 0, mAngleX + mAngleXOffset, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, mAngleY + mAngleYOffset, 1f, 0f, 0f);

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

        // ---- Escenario del Dragon: rocas y brasas de lava (caverna volcanica) ----
        // Se generan desde GROUND_Y (la coordenada de la "tapa" del hemisferio,
        // y=0), igual que las FloorTile, para que queden paradas sobre el piso.
        scenery.drawVolcanicRocks(viewMatrix, projMatrix, modelMatrix, lightPos, effectiveSpotAngle);

        float R = 1.48f;      // un pelin menos que 1.5 para no sobresalir del domo
        float THICK = 0.03f;
        float Y = 0.015f;     // apenas sobre y=0, evita z-fighting con la tapa del hemisferio

        // NOTA (fix): cada FloorTile es una cuña circular de 90 grados. El rotY
        // determina en QUE cuadrante del mundo cae esa cuña. El Dragon vive en
        // x<0,z<0 y la Mariposa en x>0,z>0 (ver sus translate mas abajo), pero
        // antes tenian el rotY cruzado entre si: la mariposa (y sus flores)
        // quedaban paradas sobre la losa/cuadrante circular del dragon (y el
        // dragon sobre la de la mariposa), por eso el "escenario de la
        // mariposa" se veia salido de su cuadrante. Con 180/0 corregidos, cada
        // figura y su set de props quedan dentro de su propia cuña circular.
        drawTile(tileDragon,    180f, R, THICK, Y, modelMatrix, mvpMatrix, lightPos, effectiveSpotAngle);
        drawTile(tileRocket,    90f,  R, THICK, Y, modelMatrix, mvpMatrix, lightPos, effectiveSpotAngle);
        drawTile(tileButterfly, 0f,   R, THICK, Y, modelMatrix, mvpMatrix, lightPos, effectiveSpotAngle);
        drawTile(tileSunflower, 270f, R, THICK, Y, modelMatrix, mvpMatrix, lightPos, effectiveSpotAngle);


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

        // ---- Escenario de la Mariposa: flores y pasto (jardin/pradera) ----
        // OJO: se pasa "modelMatrix" (el de la escena, sobre la tapa), NO
        // "butterflyModel" (que ya trae el translate+escala propios de la
        // mariposa) para que las flores queden generadas desde GROUND_Y y no
        // desde adentro de la mariposa.
        scenery.drawGardenFlowers(viewMatrix, projMatrix, modelMatrix, lightPos, effectiveSpotAngle);

        // ---- Girasol ----
        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, -0.65f, 1.0f, 0.60f);
        Matrix.scaleM(localMatrix, 0, 0.58f, 0.58f, 0.58f);
        Matrix.multiplyMM(sunflowerModel, 0, modelMatrix, 0, localMatrix, 0);
        sunflower.draw(viewMatrix, projMatrix, sunflowerModel, lightPos, effectiveSpotAngle);

        // ---- Escenario del Girasol: espigas de trigo y rocas (campo soleado) ----
        scenery.drawSunnyFieldProps(viewMatrix, projMatrix, modelMatrix, lightPos, effectiveSpotAngle);

        // ---- Cohete (cuadrante de tileRocket, rotY=90) ----
        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, 0.65f, 1.0f, -0.6f);
        Matrix.scaleM(localMatrix, 0, 0.6f, 0.6f, 0.6f);
        Matrix.multiplyMM(rocketModel, 0, modelMatrix, 0, localMatrix, 0);
        rocket.draw(viewMatrix, projMatrix, rocketModel, lightPos, effectiveSpotAngle);

        // ---- Escenario del Cohete: asteroides, antena y estrellas (plataforma espacial) ----
        scenery.drawSpacePlatformProps(viewMatrix, projMatrix, modelMatrix, lightPos, effectiveSpotAngle);

        // ---- Polvo brillante flotando: se transforma con el mismo mvpMatrix que
        // el resto de la escena, para que quede "anclado" al diorama y rote junto
        // con el giroscopio igual que las figuras ----
        dustParticles.update(dt, elapsedTime);
        dustParticles.draw(mvpMatrix);

        // ---- Polvo ambiental: usa solo proyeccion*camara (igual que el skybox),
        // por lo que NO se achica ni se agranda con el pellizco de zoom de la figura.
        // Asi, aunque el usuario acerque o aleje la figura, siempre hay particulas
        // repartidas por todo el escenario visible. ----
        ambientParticles.update(dt, elapsedTime);
        ambientParticles.draw(skyboxMvp);

    }

    private void drawTile(FloorTile tile, float rotY, float radius, float thickness, float y,
                          float[] modelMatrix, float[] mvpMatrix, float[] lightPos, float spotAngle) {
        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, 0f, y, 0f);
        Matrix.rotateM(localMatrix, 0, rotY, 0f, 1f, 0f);
        Matrix.scaleM(localMatrix, 0, radius, thickness, radius);
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, localMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, mvpMatrix, 0, localMatrix, 0);
        tile.draw(mTemporaryMatrix, mModelMatrix, lightPos, spotAngle);
    }

    /**
     * Recentra la figura: deshace cualquier zoom (pellizco de 2 dedos) y paneo
     * acumulado, dejando el diorama en su posicion y tamaño original.
     * (La rotacion la vuelve a calibrar MainActivity con el giroscopio.)
     */
    public void centrarFigura() {
        mScale = 1f;
        mPanX = 0f;
        mPanY = 0f;
        mAngleXOffset = 0f;
        mAngleYOffset = 0f;
    }

    // ---- Movimiento por botones de flecha (D-pad de la UI) ----
    // En vez de desplazar la camara en linea recta (paneo), giran la escena
    // sobre los MISMOS ejes que el giroscopio: izquierda/derecha orbitan
    // alrededor de la isla (para ir viendo Dragon -> Cohete -> Mariposa ->
    // Girasol, que estan repartidos en un circulo), y arriba/abajo inclinan
    // la vista como si te agacharas o te pararas de puntitas. El offset se
    // SUMA a mAngleX/mAngleY del giroscopio en onDrawFrame, no lo reemplaza,
    // asi ambos controles conviven sin pisarse.
    public void moveLeft() {
        mAngleXOffset = normalizeAngle(mAngleXOffset - ROTATE_STEP);
    }

    public void moveRight() {
        mAngleXOffset = normalizeAngle(mAngleXOffset + ROTATE_STEP);
    }

    public void moveUp() {
        mAngleYOffset = clamp(mAngleYOffset + ROTATE_STEP, -PITCH_OFFSET_LIMIT, PITCH_OFFSET_LIMIT);
    }

    public void moveDown() {
        mAngleYOffset = clamp(mAngleYOffset - ROTATE_STEP, -PITCH_OFFSET_LIMIT, PITCH_OFFSET_LIMIT);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float normalizeAngle(float angle) {
        while (angle > 180f) angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }
}