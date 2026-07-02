package com.juanc.casaabierta_prograavanzada;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {

    private MyGLRenderer mRenderer;
    private float mPreviousX;
    //private float mPreviousY;
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
                //mPreviousY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = x - mPreviousX;
                //float dy = y - mPreviousY;

                if (mRenderer != null) {
                    if (e.getPointerCount() >= 2) {
                        // DOS DEDOS -> mover la luz (orbita alrededor de la escena)
                        mRenderer.lightAngleY += dx * TOUCH_SCALE_FACTOR;
                       // mRenderer.lightAngleX += dy * TOUCH_SCALE_FACTOR;
                    } else {
                        // UN DEDO -> rotar el modelo (comportamiento original)
                        mRenderer.mAngleX += dx * TOUCH_SCALE_FACTOR;
                        //mRenderer.mAngleY += dy * TOUCH_SCALE_FACTOR;
                    }
                    requestRender();
                }

                mPreviousX = x;
                //mPreviousY = y;
                break;
        }

        return true;
    }
}