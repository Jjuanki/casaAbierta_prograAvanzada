package com.juanc.casaabierta_prograavanzada.Dibujos;


import android.opengl.Matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Convierte una grilla de texto (cada caracter = un "pixel"/voxel) en una figura 3D
 * hecha de cubos, con la misma iluminacion tipo spotlight que el resto de la escena
 * (o sea: no se ve nada hasta que la luz le pega).
 *
 * Uso:
 *   Map<Character,float[]> paleta = new HashMap<>();
 *   paleta.put('G', new float[]{0.3f,0.7f,0.3f,1f});
 *   String[] filas = { " G ", "GGG", " G " };
 *   PixelArtFigure figura = new PixelArtFigure(filas, paleta, 0.12f, 0.12f, -0.5f, 1.5f, 0f);
 *   // en onDrawFrame: figura.draw(mvpMatrix, modelMatrix, lightPos, spotlightAngle);
 */

import com.juanc.casaabierta_prograavanzada.Cube;

/**
 * Convierte una grilla de texto (cada caracter = un "pixel"/voxel) en una figura 3D
 * hecha de cubos, con la misma iluminacion tipo spotlight que el resto de la escena
 * (o sea: no se ve nada hasta que la luz le pega).
 *
 * Uso:
 *   Map<Character,float[]> paleta = new HashMap<>();
 *   paleta.put('G', new float[]{0.3f,0.7f,0.3f,1f});
 *   String[] filas = { " G ", "GGG", " G " };
 *   PixelArtFigure figura = new PixelArtFigure(filas, paleta, 0.12f, 0.12f, -0.5f, 1.5f, 0f);
 *   // en onDrawFrame: figura.draw(mvpMatrix, modelMatrix, lightPos, spotlightAngle);
 */
public class PixelArtFigure {

    private static class Cell {
        int row, col;
        float[] color;
        Cell(int row, int col, float[] color) { this.row = row; this.col = col; this.color = color; }
    }

    private final Cube cube;
    private final List<Cell> cells = new ArrayList<>();
    private final float cellSize;
    private final float depth;
    private final float originX, originY, originZ;

    /**
     * @param rows      filas de texto (todas del mismo largo); un espacio ' ' significa "sin bloque"
     * @param palette   mapa caracter -> color RGBA
     * @param cellSize  tamaño de cada "pixel" en unidades del mundo (ancho/alto)
     * @param depth     grosor de la figura en Z (que tan "plana" o "gruesa" se ve)
     * @param originX/Y/Z posicion en el mundo de la esquina superior-izquierda de la grilla
     */
    public PixelArtFigure(String[] rows, Map<Character, float[]> palette,
                          float cellSize, float depth,
                          float originX, float originY, float originZ) {
        this.cube = new Cube();
        this.cellSize = cellSize;
        this.depth = depth;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;

        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                float[] color = palette.get(ch);
                if (color != null) {
                    cells.add(new Cell(r, c, color));
                }
            }
        }
    }

    public void draw(float[] mvpMatrix, float[] modelMatrix, float[] lightPos, float spotlightAngle) {
        float[] blockLocal = new float[16];
        float[] blockModel = new float[16];
        float[] blockMvp = new float[16];

        for (Cell cell : cells) {
            float px = originX + cell.col * cellSize;
            float py = originY - cell.row * cellSize; // fila 0 = arriba
            float pz = originZ;

            Matrix.setIdentityM(blockLocal, 0);
            Matrix.translateM(blockLocal, 0, px, py, pz);
            Matrix.scaleM(blockLocal, 0, cellSize, cellSize, depth);

            // El bloque hereda tambien la rotacion/pan/escala de la escena (modelMatrix/mvpMatrix
            // ya la traen incluida), asi que solo agregamos su transformacion local.
            Matrix.multiplyMM(blockModel, 0, modelMatrix, 0, blockLocal, 0);
            Matrix.multiplyMM(blockMvp, 0, mvpMatrix, 0, blockLocal, 0);

            cube.draw(blockMvp, blockModel, lightPos, spotlightAngle, cell.color);
        }
    }
}
