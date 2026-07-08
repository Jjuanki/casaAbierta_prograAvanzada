package com.juanc.casaabierta_prograavanzada;

/**
 * Contrato común para figuras que usan el shader tipo spotlight
 * (uMVPMatrix, uModelMatrix, uLightPos/uLightDir, uCutOff, uColor).
 * Permite dibujar Cube y SpotlitSphere con el mismo código de composición.
 */
public interface SpotlitShape {
    void draw(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle);
}