package com.juanc.casaabierta_prograavanzada;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class MyGLSurfaceView extends GLSurfaceView {

    private MyGLRenderer mRenderer;

    // Distancia entre los 2 dedos del pellizco de zoom (se resetea cada vez que
    // vuelve a haber exactamente 2 dedos en pantalla, para que no "salte").
    private float mPreviousDist;

    private static final float ZOOM_SENSITIVITY = 0.008f; // sensibilidad del pellizco de zoom (2 dedos)

    // Mapeo ABSOLUTO pantalla -> angulos de la luz: tocar un punto de la pantalla
    // manda la luz a ese punto, para poder moverla a cualquier parte solo tocando ahi.
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
    //  - 1 dedo (tocar / arrastrar) -> la luz sigue la posicion del dedo en la pantalla
    //  - 2 dedos, pellizcar          -> acercar/alejar (zoom) el escenario
    //  (la rotacion del escenario ahora la maneja el giroscopio, ver MainActivity)
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mRenderer == null) return true;

        int action = e.getActionMasked();
        int pointerCount = e.getPointerCount();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Recien empieza a tocar con 1 dedo: la luz salta directo a ese punto.
                updateLightFromTouch(e, 0);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // Llego un segundo dedo: arranca el pellizco de zoom.
                if (pointerCount == 2) {
                    mPreviousDist = spacing(e);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (pointerCount == 1) {
                    // 1 dedo: la luz sigue al dedo en tiempo real.
                    updateLightFromTouch(e, 0);
                } else if (pointerCount >= 2) {
                    // 2 (o mas) dedos: pellizcar para acercar/alejar (zoom).
                    // Se usan siempre los primeros 2 dedos activos como referencia.
                    float newDist = spacing(e);
                    if (mPreviousDist > 10f) {
                        float deltaDist = newDist - mPreviousDist;
                        mRenderer.mScale += deltaDist * ZOOM_SENSITIVITY;
                        if (mRenderer.mScale < MyGLRenderer.MIN_SCALE) mRenderer.mScale = MyGLRenderer.MIN_SCALE;
                        if (mRenderer.mScale > MyGLRenderer.MAX_SCALE) mRenderer.mScale = MyGLRenderer.MAX_SCALE;
                    }
                    mPreviousDist = newDist;
                }
                requestRender();
                break;

            case MotionEvent.ACTION_POINTER_UP: {
                // Si queda 1 solo dedo despues de soltar uno, retoma el seguimiento de luz
                // con ese dedo restante para que no se quede "pegada".
                int remaining = pointerCount - 1;
                if (remaining == 1) {
                    int keptIndex = (e.getActionIndex() == 0) ? 1 : 0;
                    updateLightFromTouch(e, keptIndex);
                } else if (remaining >= 2) {
                    mPreviousDist = spacing(e);
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        return true;
    }

    /** Mapea la posicion del dedo indicado directamente a los angulos de la luz. */
    private void updateLightFromTouch(MotionEvent e, int pointerIndex) {
        if (getWidth() == 0 || getHeight() == 0) return;
        if (pointerIndex >= e.getPointerCount()) return;

        float x = e.getX(pointerIndex);
        float y = e.getY(pointerIndex);

        float normX = x / getWidth();
        float normY = y / getHeight();

        mRenderer.lightAngleY = LIGHT_YAW_MIN + normX * (LIGHT_YAW_MAX - LIGHT_YAW_MIN);
        mRenderer.lightAngleX = LIGHT_PITCH_MAX - normY * (LIGHT_PITCH_MAX - LIGHT_PITCH_MIN);
    }

    /** Distancia entre los primeros dos punteros (para el pellizco de zoom) */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}
