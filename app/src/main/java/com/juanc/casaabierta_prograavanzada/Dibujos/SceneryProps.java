package com.juanc.casaabierta_prograavanzada.Dibujos;

import android.opengl.Matrix;

import com.juanc.casaabierta_prograavanzada.Cube;
import com.juanc.casaabierta_prograavanzada.ShaderUtils;
import com.juanc.casaabierta_prograavanzada.SpotlitSphere;

/**
 * Decoraciones pequenas ("props") para cada uno de los 4 cuadrantes del
 * diorama, para que cada figura (Dragon, Cohete, Mariposa, Girasol) no se
 * vea sola sobre su losa.
 *
 * MUY IMPORTANTE (pedido explicito): todas las piezas se generan desde
 * GROUND_Y, que es la altura de la "tapa" plana del HemiSphere (y = 0 en
 * espacio de modelo, ver HemiSphere.capVertexBuffer / MyGLRenderer.drawTile).
 * Cada pieza se para EXACTAMENTE sobre esa coordenada (base en GROUND_Y,
 * creciendo hacia +Y desde ahi), igual que ya hacen las FloorTile, para que
 * no queden flotando ni enterradas bajo el piso.
 *
 * Se pasan como parametro (viewMatrix, projMatrix, sceneModel, lightPos,
 * spotlightAngle) igual que Rocket/Sunflower/Butterfly, para heredar el
 * mismo spotlight de la escena (nada se ve hasta que la luz les pega).
 */
public class SceneryProps {

    // altura de la tapa del hemisferio (y=0) + un pelin para evitar z-fighting
    // con el tope de las FloorTile (que ya usan Y=0.015 + THICK=0.03 -> ~0.045)
    private static final float GROUND_Y = 0.05f;

    // ---- Cuadrante Dragon: caverna volcanica ----
    private final TaperedCylinder rockSpikeBig;   // roca/estalagmita grande
    private final TaperedCylinder rockSpikeSmall; // roca/estalagmita chica
    private final SpotlitSphere lavaGlow;         // brasa de lava brillando

    // ---- Cuadrante Cohete: plataforma espacial ----
    private final TaperedCylinder asteroidChunk;  // roca/asteroide gris
    private final TaperedCylinder antennaMast;    // mastil fino de antena
    private final SpotlitSphere antennaLight;     // luz roja en la punta
    private final SpotlitSphere starGlow;         // estrellita flotante

    // ---- Cuadrante Mariposa: jardin / pradera ----
    private final Cube flowerStem;                // tallito verde
    private final SpotlitSphere flowerBloomPink;
    private final SpotlitSphere flowerBloomPurple;
    private final SpotlitSphere flowerBloomWhite;
    private final TaperedCylinder grassBlade;      // brizna de pasto

    // ---- Cuadrante Girasol: campo soleado ----
    private final TaperedCylinder wheatStalk;      // espiga de trigo dorada
    private final TaperedCylinder fieldRock;       // roca chata de campo

    private final float[] localMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    public SceneryProps() {
        // Radios ya "horneados" en la pieza (igual que Rocket): se dibujan
        // con sx = sz = 1 y solo se escala sy (altura).
        rockSpikeBig   = new TaperedCylinder(0.015f, 0.16f, 0.30f, 0.27f, 0.24f, 1.0f);
        rockSpikeSmall = new TaperedCylinder(0.010f, 0.10f, 0.24f, 0.21f, 0.19f, 1.0f);
        lavaGlow       = new SpotlitSphere(1.0f, 0.42f, 0.05f, 1.0f);

        asteroidChunk  = new TaperedCylinder(0.09f, 0.12f, 0.42f, 0.42f, 0.45f, 1.0f);
        antennaMast    = new TaperedCylinder(0.008f, 0.012f, 0.55f, 0.55f, 0.58f, 1.0f);
        antennaLight   = new SpotlitSphere(0.95f, 0.15f, 0.15f, 1.0f);
        starGlow       = new SpotlitSphere(1.0f, 0.95f, 0.75f, 1.0f);

        flowerStem     = new Cube(0.15f, 0.55f, 0.18f, 1.0f);
        flowerBloomPink   = new SpotlitSphere(0.95f, 0.45f, 0.65f, 1.0f);
        flowerBloomPurple = new SpotlitSphere(0.65f, 0.40f, 0.90f, 1.0f);
        flowerBloomWhite  = new SpotlitSphere(0.98f, 0.98f, 0.95f, 1.0f);
        grassBlade     = new TaperedCylinder(0.006f, 0.02f, 0.20f, 0.55f, 0.18f, 1.0f);

        wheatStalk     = new TaperedCylinder(0.006f, 0.018f, 0.85f, 0.70f, 0.20f, 1.0f);
        fieldRock      = new TaperedCylinder(0.07f, 0.10f, 0.55f, 0.48f, 0.38f, 1.0f);
    }

    /**
     * Rocas y brasas de lava alrededor del Dragon (cuadrante x<0, z<0,
     * mismo lado donde MyGLRenderer.buildDragonFigure ubica al dragoncito).
     */
    public void drawVolcanicRocks(float[] viewMatrix, float[] projMatrix, float[] sceneModel,
                                   float[] lightPos, float spotlightAngle) {
        drawTaper(rockSpikeBig,   sceneModel, -0.35f, -0.30f, 12f,  0.30f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(rockSpikeSmall, sceneModel, -0.90f, -0.32f, -18f, 0.20f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(rockSpikeBig,   sceneModel, -0.55f, -0.85f, 40f,  0.24f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(rockSpikeSmall, sceneModel, -0.95f, -0.75f, -50f, 0.16f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(rockSpikeSmall, sceneModel, -0.60f, -0.55f, 65f,  0.14f, viewMatrix, projMatrix, lightPos, spotlightAngle); // roca extra al centro
        drawTaper(rockSpikeBig,   sceneModel, -1.10f, -0.20f, -8f,  0.18f, viewMatrix, projMatrix, lightPos, spotlightAngle); // roca extra pegada al borde

        drawSphere(lavaGlow, sceneModel, -0.85f, GROUND_Y + 0.02f, -0.55f, 0.045f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawSphere(lavaGlow, sceneModel, -0.30f, GROUND_Y + 0.015f, -0.80f, 0.035f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawSphere(lavaGlow, sceneModel, -0.55f, GROUND_Y + 0.015f, -0.35f, 0.03f,  viewMatrix, projMatrix, lightPos, spotlightAngle); // brasa extra
    }

    /**
     * Asteroides + antena + estrellitas alrededor del Cohete
     * (cuadrante x>0, z<0, mismo lado que MyGLRenderer.rocketModel).
     */
    public void drawSpacePlatformProps(float[] viewMatrix, float[] projMatrix, float[] sceneModel,
                                        float[] lightPos, float spotlightAngle) {
        drawTaper(asteroidChunk, sceneModel, 0.32f, -0.32f, 0f,  0.16f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(asteroidChunk, sceneModel, 0.95f, -0.30f, 0f,  0.12f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(asteroidChunk, sceneModel, 0.55f, -0.95f, 0f,  0.10f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(asteroidChunk, sceneModel, 0.75f, -0.60f, 0f,  0.13f, viewMatrix, projMatrix, lightPos, spotlightAngle); // asteroide extra al centro

        // Antena: mastil parado desde el piso + lucecita roja en la punta
        float mastH = 0.34f;
        drawPart(antennaMast, sceneModel, 0.30f, GROUND_Y + mastH / 2f, -0.90f, 0f, 0f,
                1f, mastH, 1f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawSphere(antennaLight, sceneModel, 0.30f, GROUND_Y + mastH, -0.90f, 0.035f, viewMatrix, projMatrix, lightPos, spotlightAngle);

        // Estrellitas flotando un poco por encima del piso (no tocan la tapa)
        drawSphere(starGlow, sceneModel, 0.95f, GROUND_Y + 0.28f, -0.75f, 0.03f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawSphere(starGlow, sceneModel, 0.45f, GROUND_Y + 0.40f, -0.55f, 0.025f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawSphere(starGlow, sceneModel, 0.65f, GROUND_Y + 0.33f, -0.85f, 0.022f, viewMatrix, projMatrix, lightPos, spotlightAngle); // estrellita extra
    }

    /**
     * Flores alrededor de la Mariposa (cuadrante x>0, z>0, mismo lado que
     * MyGLRenderer.butterflyModel). Antes tambien tenia brizanas de pasto
     * (grassBlade), pero por pedido se sacaron y quedan solo las florcitas.
     */
    public void drawGardenFlowers(float[] viewMatrix, float[] projMatrix, float[] sceneModel,
                                  float[] lightPos, float spotlightAngle) {
        // Distribuimos varias florecitas en el cuadrante x>0, z>0.
        // Se usan distintos colores y alturas para dar variedad.
        drawFlower(flowerBloomPink,   sceneModel, 0.35f, 0.35f, 0.16f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawFlower(flowerBloomPurple, sceneModel, 0.90f, 0.30f, 0.13f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawFlower(flowerBloomWhite,  sceneModel, 0.55f, 0.85f, 0.15f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawFlower(flowerBloomPink,   sceneModel, 0.95f, 0.75f, 0.12f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawFlower(flowerBloomPurple, sceneModel, 0.60f, 0.55f, 0.14f, viewMatrix, projMatrix, lightPos, spotlightAngle); // florecita extra al centro
        drawFlower(flowerBloomWhite,  sceneModel, 1.10f, 0.45f, 0.11f, viewMatrix, projMatrix, lightPos, spotlightAngle); // florecita extra cerca del borde
    }

    /**
     * Espigas de trigo + rocas chatas alrededor del Girasol
     * (cuadrante x<0, z>0, mismo lado que MyGLRenderer.sunflowerModel).
     */
    public void drawSunnyFieldProps(float[] viewMatrix, float[] projMatrix, float[] sceneModel,
                                     float[] lightPos, float spotlightAngle) {
        drawTaper(wheatStalk, sceneModel, -0.30f, 0.85f, 0f,   0.28f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(wheatStalk, sceneModel, -0.95f, 0.35f, 15f,  0.24f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(wheatStalk, sceneModel, -0.90f, 0.85f, -12f, 0.22f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(wheatStalk, sceneModel, -0.45f, 0.65f, 8f,   0.18f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(wheatStalk, sceneModel, -0.65f, 0.50f, -6f,  0.20f, viewMatrix, projMatrix, lightPos, spotlightAngle); // espiga extra al centro
        drawTaper(wheatStalk, sceneModel, -1.10f, 0.55f, 10f,  0.16f, viewMatrix, projMatrix, lightPos, spotlightAngle); // espiga extra cerca del borde

        drawTaper(fieldRock, sceneModel, -0.55f, 0.40f, 0f, 0.14f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawTaper(fieldRock, sceneModel, -0.35f, 0.55f, 20f, 0.11f, viewMatrix, projMatrix, lightPos, spotlightAngle); // roca extra
    }

    // ---- Helpers ----

    /** Cono/roca/tallo apoyado desde GROUND_Y hacia arriba (base = GROUND_Y, altura = height). */
    private void drawTaper(ShaderUtils.SpotlitShape shape, float[] sceneModel,
                           float x, float z, float tiltZDeg, float height,
                           float[] viewMatrix, float[] projMatrix,
                           float[] lightPos, float spotlightAngle) {
        drawPart(shape, sceneModel, x, GROUND_Y + height / 2f, z, 0f, tiltZDeg,
                1f, height, 1f, viewMatrix, projMatrix, lightPos, spotlightAngle);
    }

    /** Esfera (brasa/estrella/flor) centrada en (x, y, z) con el radio indicado. */
    private void drawSphere(ShaderUtils.SpotlitShape shape, float[] sceneModel,
                            float x, float y, float z, float radius,
                            float[] viewMatrix, float[] projMatrix,
                            float[] lightPos, float spotlightAngle) {
        drawPart(shape, sceneModel, x, y, z, 0f, 0f,
                radius, radius, radius, viewMatrix, projMatrix, lightPos, spotlightAngle);
    }

    /** Florcita: tallo parado desde GROUND_Y + capullo esferico en la punta. */
    private void drawFlower(SpotlitSphere bloom, float[] sceneModel,
                            float x, float z, float stemHeight,
                            float[] viewMatrix, float[] projMatrix,
                            float[] lightPos, float spotlightAngle) {
        drawPart(flowerStem, sceneModel, x, GROUND_Y + stemHeight / 2f, z, 0f, 0f,
                0.035f, stemHeight, 0.035f, viewMatrix, projMatrix, lightPos, spotlightAngle);
        drawSphere(bloom, sceneModel, x, GROUND_Y + stemHeight, z, 0.06f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);
    }

    private void drawPart(ShaderUtils.SpotlitShape shape, float[] sceneModel,
                          float tx, float ty, float tz, float rotYDeg, float rotZDeg,
                          float sx, float sy, float sz,
                          float[] viewMatrix, float[] projMatrix,
                          float[] lightPos, float spotlightAngle) {

        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, tx, ty, tz);
        Matrix.rotateM(localMatrix, 0, rotYDeg, 0f, 1f, 0f);
        Matrix.rotateM(localMatrix, 0, rotZDeg, 0f, 0f, 1f);
        Matrix.scaleM(localMatrix, 0, sx, sy, sz);

        Matrix.multiplyMM(modelMatrix, 0, sceneModel, 0, localMatrix, 0);
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, tempMatrix, 0);

        shape.draw(mvpMatrix, modelMatrix, lightPos, spotlightAngle);
    }
}
