package com.juanc.casaabierta_prograavanzada;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;




public class MyGLSurfaceView extends GLSurfaceView {

    private MyGLRenderer mRenderer;

    // Guarda la ultima posicion conocida de cada dedo, indexada por su ID (estable).
    // Importante: el INDICE de un dedo puede cambiar cuando otro se levanta, pero su ID no.
    // Usar indices (como getX(0)/getX(1) sin mas) es lo que causaba los saltos raros.
    private final SparseArray<float[]> mLastPositions = new SparseArray<>();

    private float mPreviousDist; // distancia entre los 2 dedos de luz (para el pellizco del cono)
    private float mPreviousDist3; // distancia entre dedos con 3+ dedos (para el pellizco de zoom)

    private static final float ROTATION_SENSITIVITY = 0.4f; // grados por pixel arrastrado
    private static final float SPOT_ANGLE_SENSITIVITY = 0.08f; // sensibilidad del pellizco (mas baja = mas suave)
    private static final float ZOOM_SENSITIVITY = 0.005f; // sensibilidad del pellizco de zoom (3 dedos)
    private static final float MAX_TILT = 80f; // limite de inclinacion vertical, para no "voltear" la escena

    // Mapeo ABSOLUTO pantalla -> angulos de la luz: tocar un extremo de la pantalla
    // manda la luz a ese extremo, para poder moverla a cualquier parte con solo tocar ahi.
    private static final float LIGHT_YAW_MIN = -180f;
    private static final float LIGHT_YAW_MAX = 180f;
    private static final float LIGHT_PITCH_MIN = -80f;
    private static final float LIGHT_PITCH_MAX = 80f;

    public MyGLSurfaceView(Context context) {
        super(context);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRenderer(MyGLRenderer renderer) {
        super.setRenderer(renderer);
        mRenderer = renderer;
    }

    // Gestos:
    //  - 1 dedo               -> rotar la escena (arrastre horizontal = giro, vertical = inclinacion)
    //  - 2 dedos, arrastrar   -> mover la luz a cualquier parte de la pantalla
    //  - 2 dedos, pellizcar   -> agrandar/achicar el cono de luz
    //  - 3 dedos, pellizcar   -> acercar/alejar (zoom) el escenario
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mRenderer == null) return true;

        int action = e.getActionMasked();
        int actionIndex = e.getActionIndex();
        int actionPointerId = e.getPointerId(actionIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                mLastPositions.put(actionPointerId, new float[]{e.getX(actionIndex), e.getY(actionIndex)});
                if (e.getPointerCount() == 2) {
                    mPreviousDist = spacing(e);
                    updateLightFromTouch(e);
                } else if (e.getPointerCount() >= 3) {
                    mPreviousDist3 = spacing(e);
                }
                break;

            case MotionEvent.ACTION_MOVE: {
                int pointerCount = e.getPointerCount();

                if (pointerCount == 1) {
                    // 1 dedo: rotar la escena (arrastre horizontal = giro, vertical = inclinacion)
                    int id = e.getPointerId(0);
                    float x = e.getX(0), y = e.getY(0);
                    float[] last = mLastPositions.get(id);
                    if (last != null) {
                        float dx = x - last[0];
                        //float dy = y - last[1];
                        mRenderer.mAngleX += dx * ROTATION_SENSITIVITY; // giro alrededor del eje Y
                        //mRenderer.mAngleY += dy * ROTATION_SENSITIVITY; // inclinacion alrededor del eje X
                        if (mRenderer.mAngleY > MAX_TILT) mRenderer.mAngleY = MAX_TILT;
                        if (mRenderer.mAngleY < -MAX_TILT) mRenderer.mAngleY = -MAX_TILT;
                    }
                    mLastPositions.put(id, new float[]{x, y});

                } else if (pointerCount == 2) {
                    // 2 dedos: mover la luz + pellizcar para el cono de luz
                    updateLightFromTouch(e);

                    float newDist = spacing(e);
                    if (mPreviousDist > 10f) {
                        float deltaDist = newDist - mPreviousDist;
                        mRenderer.spotlightAngle += deltaDist * SPOT_ANGLE_SENSITIVITY;
                        if (mRenderer.spotlightAngle < MyGLRenderer.MIN_SPOT_ANGLE) mRenderer.spotlightAngle = MyGLRenderer.MIN_SPOT_ANGLE;
                        if (mRenderer.spotlightAngle > MyGLRenderer.MAX_SPOT_ANGLE) mRenderer.spotlightAngle = MyGLRenderer.MAX_SPOT_ANGLE;
                    }
                    mPreviousDist = newDist;

                    for (int i = 0; i < pointerCount; i++) {
                        int id = e.getPointerId(i);
                        mLastPositions.put(id, new float[]{e.getX(i), e.getY(i)});
                    }

                } else if (pointerCount >= 3) {
                    // 3 (o mas) dedos: pellizcar para acercar/alejar (zoom)
                    // Se usan siempre los primeros 2 dedos activos como referencia de distancia;
                    // dedos adicionales solo se ignoran para este calculo.
                    float newDist3 = spacing(e);
                    if (mPreviousDist3 > 10f) {
                        float deltaDist = newDist3 - mPreviousDist3;
                        mRenderer.mScale += deltaDist * ZOOM_SENSITIVITY;
                        if (mRenderer.mScale < MyGLRenderer.MIN_SCALE) mRenderer.mScale = MyGLRenderer.MIN_SCALE;
                        if (mRenderer.mScale > MyGLRenderer.MAX_SCALE) mRenderer.mScale = MyGLRenderer.MAX_SCALE;
                    }
                    mPreviousDist3 = newDist3;

                    for (int i = 0; i < pointerCount; i++) {
                        int id = e.getPointerId(i);
                        mLastPositions.put(id, new float[]{e.getX(i), e.getY(i)});
                    }
                }

                requestRender();
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                mLastPositions.remove(actionPointerId);
                int remaining = e.getPointerCount() - 1;
                // Reinicia las referencias de posicion/distancia segun cuantos dedos quedan,
                // para que el gesto correspondiente no salte al perder un dedo.
                if (remaining >= 1) {
                    int updated = 0;
                    for (int i = 0; i < e.getPointerCount() && updated < remaining; i++) {
                        if (i == actionIndex) continue;
                        int id = e.getPointerId(i);
                        mLastPositions.put(id, new float[]{e.getX(i), e.getY(i)});
                        updated++;
                    }
                }
                if (remaining == 2) {
                    mPreviousDist = spacing(e);
                } else if (remaining >= 3) {
                    mPreviousDist3 = spacing(e);
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPositions.clear();
                break;
        }

        return true;
    }

    /** Mapea la posicion (centroide de los primeros 2 dedos) directamente a los angulos de la luz. */
    private void updateLightFromTouch(MotionEvent e) {
        if (getWidth() == 0 || getHeight() == 0) return;

        float cx = (e.getX(0) + e.getX(1)) / 2f;
        float cy = (e.getY(0) + e.getY(1)) / 2f;

        float normX = cx / getWidth();
        float normY = cy / getHeight();

        mRenderer.lightAngleY = LIGHT_YAW_MIN + normX * (LIGHT_YAW_MAX - LIGHT_YAW_MIN);
        mRenderer.lightAngleX = LIGHT_PITCH_MAX - normY * (LIGHT_PITCH_MAX - LIGHT_PITCH_MIN);
    }

    /** Distancia entre los primeros dos punteros (para el pellizco del cono de luz) */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}