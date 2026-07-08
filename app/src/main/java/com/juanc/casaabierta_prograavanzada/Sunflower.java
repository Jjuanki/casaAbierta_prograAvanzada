package com.juanc.casaabierta_prograavanzada;
import android.opengl.Matrix;
import com.juanc.casaabierta_prograavanzada.separation.Cube;

/**

 * Girasol 3D compuesto por:
 * * 12 pétalos amarillos exteriores
 * * 8 pétalos naranjas interiores
 * * Centro café
 * * Tallo y dos hojas verdes
 */
public class Sunflower {

    private final SpotlitSphere yellowPetal;
    private final SpotlitSphere orangePetal;
    private final SpotlitSphere center;
    private final SpotlitSphere leaf;
    private final Cube stem;

    private final float[] localMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    public Sunflower() {
        yellowPetal = new SpotlitSphere(1.0f, 0.78f, 0.05f, 1.0f);
        orangePetal = new SpotlitSphere(1.0f, 0.35f, 0.02f, 1.0f);
        center = new SpotlitSphere(0.30f, 0.12f, 0.03f, 1.0f);
        leaf = new SpotlitSphere(0.08f, 0.55f, 0.12f, 1.0f);


        // Si tu Cube usa RGBA, este tallo será verde.
        stem = new Cube(0.08f, 0.45f, 0.08f, 1.0f);


    }

    public void draw(float[] viewMatrix, float[] projMatrix, float[] figureModel,
                     float[] lightPos, float spotlightAngle) {


        // ---- Tallo ----
        drawPart(stem, figureModel,
                0f, -0.80f, 0f,
                0f,
                0.055f, 0.80f, 0.055f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        // ---- Hojas ----
        drawPart(leaf, figureModel,
                0.22f, -0.65f, 0.02f,
                -48f,
                0.13f, 0.30f, 0.045f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        drawPart(leaf, figureModel,
                -0.22f, -0.92f, 0.02f,
                48f,
                0.13f, 0.30f, 0.045f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        // ---- Pétalos exteriores: 12 amarillos ----
        for (int i = 0; i < 12; i++) {
            float angle = i * 30f;
            float rad = (float) Math.toRadians(angle);

            float x = (float) Math.sin(rad) * 0.40f;
            float y = (float) Math.cos(rad) * 0.40f;

            drawPart(yellowPetal, figureModel,
                    x, y + 0.10f, -0.02f,
                    -angle,
                    0.15f, 0.38f, 0.055f,
                    viewMatrix, projMatrix, lightPos, spotlightAngle);
        }

        // ---- Pétalos interiores: 8 naranjas ----
        for (int i = 0; i < 8; i++) {
            float angle = i * 45f + 22.5f;
            float rad = (float) Math.toRadians(angle);

            float x = (float) Math.sin(rad) * 0.24f;
            float y = (float) Math.cos(rad) * 0.24f;

            drawPart(orangePetal, figureModel,
                    x, y + 0.10f, 0.01f,
                    -angle,
                    0.11f, 0.25f, 0.06f,
                    viewMatrix, projMatrix, lightPos, spotlightAngle);
        }

        // ---- Centro elevado del girasol ----
        drawPart(center, figureModel,
                0f, 0.10f, 0.08f,
                0f,
                0.27f, 0.27f, 0.11f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        // ---- Semillas decorativas sobre el centro ----
        for (int i = 0; i < 8; i++) {
            float angle = i * 45f;
            float rad = (float) Math.toRadians(angle);

            float x = (float) Math.sin(rad) * 0.14f;
            float y = (float) Math.cos(rad) * 0.14f;

            drawPart(center, figureModel,
                    x, y + 0.10f, 0.18f,
                    0f,
                    0.035f, 0.035f, 0.025f,
                    viewMatrix, projMatrix, lightPos, spotlightAngle);
        }


    }

    private void drawPart(SpotlitShape shape, float[] figureModel,
                          float tx, float ty, float tz, float rotZDeg,
                          float sx, float sy, float sz,
                          float[] viewMatrix, float[] projMatrix,
                          float[] lightPos, float spotlightAngle) {


        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, tx, ty, tz);
        Matrix.rotateM(localMatrix, 0, rotZDeg, 0f, 0f, 1f);
        Matrix.scaleM(localMatrix, 0, sx, sy, sz);

        Matrix.multiplyMM(modelMatrix, 0, figureModel, 0, localMatrix, 0);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, tempMatrix, 0);

        shape.draw(mvpMatrix, modelMatrix, lightPos, spotlightAngle);


    }
}
