package com.juanc.casaabierta_prograavanzada.Dibujos;

import android.opengl.Matrix;

import com.juanc.casaabierta_prograavanzada.Cube;
import com.juanc.casaabierta_prograavanzada.ShaderUtils;
import com.juanc.casaabierta_prograavanzada.SpotlitSphere;

public class Butterfly {

    private final Cube darkCube;          // cabeza y varillas de las antenas
    private final SpotlitSphere darkBall; // cuerpo y puntas de antena
    private final SpotlitSphere wingBall; // alas

    private final float[] localMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    public Butterfly() {
        darkCube = new Cube(0.05f, 0.05f, 0.05f, 1.0f);
        darkBall = new SpotlitSphere(0.05f, 0.05f, 0.05f, 1.0f);
        wingBall = new SpotlitSphere(0.95f, 0.55f, 0.10f, 1.0f); // naranja tipo monarca
    }

    public void draw(float[] viewMatrix, float[] projMatrix, float[] figureModel,
                     float[] lightPos, float spotlightAngle) {

        // ---- Alas: 4 esferas achatadas y rotadas, dos por lado ----
        drawPart(wingBall, figureModel, 0.55f, 0.62f, 0f, 35f, 0.42f, 0.20f, 0.05f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);   // superior derecha
        drawPart(wingBall, figureModel, 0.55f, 0.18f, 0f, -35f, 0.42f, 0.20f, 0.05f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);   // inferior derecha
        drawPart(wingBall, figureModel, -0.55f, 0.62f, 0f, -35f, 0.42f, 0.20f, 0.05f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);   // superior izquierda
        drawPart(wingBall, figureModel, -0.55f, 0.18f, 0f, 35f, 0.42f, 0.20f, 0.05f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);   // inferior izquierda

        // ---- Cuerpo: cadena de 6 bolitas bajando desde la cabeza ----
        float bodyTopY = 0.86f;
        float step = 0.13f;
        for (int i = 0; i < 6; i++) {
            drawPart(darkBall, figureModel, 0f, bodyTopY - i * step, 0.01f, 0f, 0.06f, 0.06f, 0.06f,
                    viewMatrix, projMatrix, lightPos, spotlightAngle);
        }

        // ---- Cabeza: cuadrado plano ----
        // NOTA: escalas x3 respecto a la version anterior porque el Cube unificado
        // ahora es de -0.5 a 0.5 (antes era de -1.5 a 1.5 en la version de "separation").
        drawPart(darkCube, figureModel, 0f, 0.95f, 0f, 0f, 0.24f, 0.24f, 0.06f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);

        // ---- Antenas: varilla + bolita en la punta ----
        drawPart(darkCube, figureModel, 0.10f, 1.12f, 0f, -25f, 0.036f, 0.42f, 0.036f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);   // varilla derecha
        drawPart(darkBall, figureModel, 0.20f, 1.24f, 0f, 0f, 0.035f, 0.035f, 0.035f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);   // punta derecha
        drawPart(darkCube, figureModel, -0.10f, 1.12f, 0f, 25f, 0.036f, 0.42f, 0.036f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);   // varilla izquierda
        drawPart(darkBall, figureModel, -0.20f, 1.24f, 0f, 0f, 0.035f, 0.035f, 0.035f,
                viewMatrix, projMatrix, lightPos, spotlightAngle);   // punta izquierda
    }

    private void drawPart(ShaderUtils.SpotlitShape shape, float[] figureModel,
                          float tx, float ty, float tz, float rotZDeg,
                          float sx, float sy, float sz,
                          float[] viewMatrix, float[] projMatrix,
                          float[] lightPos, float spotlightAngle) {

        // Local: primero escala (forma de la pieza), luego rota en Z (inclinación),
        // luego traslada dentro de la figura
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