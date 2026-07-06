package com.juanc.casaabierta_prograavanzada;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {

    private MyGLRenderer mRenderer;
    private float mPreviousX;
    private float mPreviousY;
    private float mPreviousDist;
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;

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

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(0);
        float y = e.getY(0);

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                // Reinicia la referencia para que no "salte" al agregar/quitar un dedo
                mPreviousX = x;
                mPreviousY = y;
                if (e.getPointerCount() >= 2) {
                    mPreviousDist = spacing(e);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                if (mRenderer != null) {
                    if (e.getPointerCount() >= 2) {
                        // DOS DEDOS -> mover la luz y cambiar tamaño
                        float newDist = spacing(e);
                        if (newDist > 10f) {
                            float deltaDist = newDist - mPreviousDist;
                            // Ajustar el ángulo de la luz (tamaño)
                            mRenderer.spotlightAngle -= deltaDist * 0.1f;
                    
                            if (mRenderer.spotlightAngle < 2f) mRenderer.spotlightAngle = 2f;
                            if (mRenderer.spotlightAngle > 45f) mRenderer.spotlightAngle = 45f;
                            mPreviousDist = newDist;
                        }

                        // Mover la luz 
                        mRenderer.lightAngleY += dx * TOUCH_SCALE_FACTOR;
                        mRenderer.lightAngleX += dy * TOUCH_SCALE_FACTOR;
                    } else {
                        // UN DEDO -> rotar el modelo 
                        mRenderer.mAngleX += dx * TOUCH_SCALE_FACTOR;
                       
                    }
                    requestRender();
                }

                mPreviousX = x;
                mPreviousY = y;
                break;
        }

        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}
