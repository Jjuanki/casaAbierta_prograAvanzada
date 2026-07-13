package com.juanc.casaabierta_prograavanzada.Dibujos;

import android.opengl.Matrix;

import com.juanc.casaabierta_prograavanzada.Cube;
import com.juanc.casaabierta_prograavanzada.ShaderUtils;
import com.juanc.casaabierta_prograavanzada.SpotlitSphere;
import com.juanc.casaabierta_prograavanzada.Dibujos.TaperedCylinder;

/**
 * Cohete v3, armado con la misma logica que Sunflower/Goku: piezas
 * primitivas posicionadas con matrices locales que se combinan con la
 * matriz del "figureModel" del padre.
 *
 * Cambios clave respecto a la v2 (la de las esferas infladas):
 *
 * 1) BUG DE ESCALA CORREGIDO: SpotlitSphere es una esfera de radio 1
 *    (revisando su codigo: x = sin(phi)*cos(theta), sin *0.5), no de
 *    radio 0.5 como Cube. Antes yo escalaba las esferas como si fueran
 *    del mismo tamano que un Cube, asi que nariz/ventanas/llama salian
 *    el DOBLE de grandes de lo que correspondia -> por eso se veian
 *    como globos. Ahora solo se usa SpotlitSphere para las ventanas, con
 *    la escala ya corregida (escala = radio real deseado).
 *
 * 2) NUEVA PIEZA: TaperedCylinder (cilindro/cono, con el mismo rango
 *    -0.5..0.5 en Y que Cube, asi que la matematica de "apilar sin
 *    huecos" sigue funcionando igual). Reemplaza:
 *      - El cuerpo: antes Cube (con esquinas), ahora cilindro liso.
 *      - La nariz: antes esfera alargada (ovalo), ahora cono real con
 *        punta.
 *      - La tobera: antes un cubo chico, ahora una tobera acampanada de
 *        verdad (mas angosta arriba, mas ancha en la salida).
 *      - La llama: antes 3 esferas apiladas (se veian como cuentas de
 *        collar), ahora un cono que se afina limpiamente hacia la punta.
 */
public class Rocket {

    // ---- Cuerpo (cilindro liso) ----
    private final TaperedCylinder body;
    private final Cube collar;       // flange/anillo entre el cuerpo y la nariz
    private final Cube bandRed;      // franja roja decorativa
    private final Cube bandBlue;     // franja azul decorativa
    private final TaperedCylinder noseCone;  // cono real, con punta
    private final SpotlitSphere window;      // ojo de buey (reutilizada, escala corregida)

    // ---- Aletas ----
    private final Cube fin;          // reutilizada para las 3 aletas

    // ---- Motor ----
    private final TaperedCylinder nozzle;      // tobera acampanada
    private final TaperedCylinder flameOuter;  // llama naranja, cono que se afina
    private final TaperedCylinder flameInner;  // nucleo amarillo, cono interior mas angosto

    private final float[] localMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ---- Radios base, en unidades de mundo reales (ya no "escala tipo Cube") ----
    private static final float BODY_RADIUS = 0.13f;

    public Rocket() {
        body   = new TaperedCylinder(BODY_RADIUS, BODY_RADIUS, 0.86f, 0.88f, 0.92f, 1.0f);
        collar = new Cube(0.22f, 0.22f, 0.24f, 1.0f);
        bandRed  = new Cube(0.85f, 0.10f, 0.08f, 1.0f);
        bandBlue = new Cube(0.10f, 0.30f, 0.75f, 1.0f);

        noseCone = new TaperedCylinder(0.005f, BODY_RADIUS, 0.85f, 0.10f, 0.08f, 1.0f);
        window   = new SpotlitSphere(0.55f, 0.85f, 0.95f, 1.0f);

        fin = new Cube(0.82f, 0.08f, 0.07f, 1.0f);

        // Tobera: mas angosta arriba (garganta, pegada al cuerpo) y mas
        // ancha abajo (campana de salida) -> forma real de motor cohete.
        nozzle = new TaperedCylinder(BODY_RADIUS * 0.65f, BODY_RADIUS * 1.15f, 0.20f, 0.20f, 0.22f, 1.0f);

        // Llama: ancha arriba (sale de la campana) y se afina a un punto abajo.
        flameOuter = new TaperedCylinder(BODY_RADIUS * 1.05f, 0.02f, 1.00f, 0.45f, 0.05f, 1.0f);
        flameInner = new TaperedCylinder(BODY_RADIUS * 0.55f, 0.005f, 1.00f, 0.85f, 0.20f, 1.0f);
    }

    public void draw(float[] viewMatrix, float[] projMatrix, float[] figureModel,
                     float[] lightPos, float spotlightAngle) {

        // ==== Dimensiones clave (para que las piezas se toquen) ====
        float bodySY = 0.80f;
        float bodyTopY    =  bodySY / 2f;   // 0.40
        float bodyBottomY = -bodySY / 2f;   // -0.40

        // ---- Cuerpo principal: cilindro liso, mismo radio arriba y abajo ----
        drawPart(body, figureModel,
                0f, 0f, 0f,
                0f, 0f, 0f,
                1f, bodySY, 1f,   // el radio ya esta "horneado" en la pieza (BODY_RADIUS)
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        // ---- Collar / flange: un poco mas ancho que el cuerpo, se
        //      superpone con el tope del cuerpo para que no haya costura ----
        float collarSY = 0.07f;
        float collarY = bodyTopY - 0.015f;
        drawPart(collar, figureModel,
                0f, collarY, 0f,
                0f, 0f, 0f,
                (BODY_RADIUS * 2.15f), collarSY, (BODY_RADIUS * 2.15f),
                viewMatrix, projMatrix, lightPos, spotlightAngle);
        float collarTopY = collarY + collarSY / 2f;

        // ---- Nariz: cono real (radio de base = BODY_RADIUS, punta casi
        //      cerrada), apoyado justo sobre el collar ----
        float noseSY = 0.62f;
        float noseY = collarTopY + noseSY / 2f - 0.02f;
        drawPart(noseCone, figureModel,
                0f, noseY, 0f,
                0f, 0f, 0f,
                1f, noseSY, 1f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        // ---- Franjas decorativas (envuelven el cuerpo, un pelo mas anchas
        //      que el radio para que no queden "hundidas") ----
        float bandRadius = BODY_RADIUS * 2.05f;
        drawPart(bandRed, figureModel,
                0f, bodyTopY - 0.14f, 0f,
                0f, 0f, 0f,
                bandRadius, 0.06f, bandRadius,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        drawPart(bandBlue, figureModel,
                0f, bodyBottomY + 0.16f, 0f,
                0f, 0f, 0f,
                bandRadius, 0.06f, bandRadius,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        // ---- Ventanas: esfera de radio REAL (ya no hay que duplicar la
        //      escala), incrustadas en la cara frontal del cuerpo ----
        float windowRadius = 0.045f;
        float bodyFrontZ = BODY_RADIUS;
        drawPart(window, figureModel,
                0f, 0.10f, bodyFrontZ + windowRadius * 0.5f,
                0f, 0f, 0f,
                windowRadius, windowRadius, windowRadius,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        drawPart(window, figureModel,
                0f, -0.18f, bodyFrontZ + windowRadius * 0.5f,
                0f, 0f, 0f,
                windowRadius, windowRadius, windowRadius,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        // ---- Aletas: 3 alrededor de la base, tocando el cuerpo.
        //      Offset de 60 grados para que ninguna quede alineada con
        //      el frente (0 grados, donde estan las ventanas). ----
        int finCount = 3;
        float finAngleOffset = 60f;
        float finSY = 0.34f;
        float finSZ = 0.24f;
        float finRadius = BODY_RADIUS * 0.55f; // se mete bien adentro del cuerpo
        float finY = bodyBottomY + finSY / 2f - 0.10f;

        for (int i = 0; i < finCount; i++) {
            float angle = finAngleOffset + i * (360f / finCount);
            float rad = (float) Math.toRadians(angle);

            float x = (float) Math.sin(rad) * finRadius;
            float z = (float) Math.cos(rad) * finRadius;

            drawPart(fin, figureModel,
                    x, finY, z,
                    0f, angle, 0f,
                    0.045f, finSY, finSZ,
                    viewMatrix, projMatrix, lightPos, spotlightAngle);
        }

        // ---- Tobera: acampanada, pegada a la base del cuerpo ----
        float nozzleSY = 0.20f;
        float nozzleY = bodyBottomY - nozzleSY / 2f + 0.02f;
        drawPart(nozzle, figureModel,
                0f, nozzleY, 0f,
                0f, 0f, 0f,
                1f, nozzleSY, 1f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);
        float nozzleBottomY = nozzleY - nozzleSY / 2f;

        // ---- Llama: dos conos anidados (naranja ancho por fuera, nucleo
        //      amarillo mas angosto por dentro), afinandose hacia la punta ----
        float flameOuterSY = 0.40f;
        float flameOuterY = nozzleBottomY - flameOuterSY / 2f + 0.03f;
        drawPart(flameOuter, figureModel,
                0f, flameOuterY, 0f,
                0f, 0f, 0f,
                1f, flameOuterSY, 1f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        float flameOuterTopY = flameOuterY + flameOuterSY / 2f;
        float flameInnerSY = 0.28f;
        float flameInnerY = flameOuterTopY - flameInnerSY / 2f - 0.02f;
        drawPart(flameInner, figureModel,
                0f, flameInnerY, 0f,
                0f, 0f, 0f,
                1f, flameInnerSY, 1f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);
    }

    /**
     * Igual que en Sunflower, pero con rotacion en los 3 ejes (para las
     * aletas) y valido tanto para ShaderUtils.SpotlitShape (Cube,
     * SpotlitSphere) como para TaperedCylinder, que tambien lo implementa.
     */
    private void drawPart(ShaderUtils.SpotlitShape shape, float[] figureModel,
                          float tx, float ty, float tz,
                          float rotXDeg, float rotYDeg, float rotZDeg,
                          float sx, float sy, float sz,
                          float[] viewMatrix, float[] projMatrix,
                          float[] lightPos, float spotlightAngle) {

        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, tx, ty, tz);
        Matrix.rotateM(localMatrix, 0, rotYDeg, 0f, 1f, 0f);
        Matrix.rotateM(localMatrix, 0, rotXDeg, 1f, 0f, 0f);
        Matrix.rotateM(localMatrix, 0, rotZDeg, 0f, 0f, 1f);
        Matrix.scaleM(localMatrix, 0, sx, sy, sz);

        Matrix.multiplyMM(modelMatrix, 0, figureModel, 0, localMatrix, 0);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, tempMatrix, 0);

        shape.draw(mvpMatrix, modelMatrix, lightPos, spotlightAngle);
    }
}