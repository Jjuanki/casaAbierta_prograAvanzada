package com.juanc.casaabierta_prograavanzada;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;

import com.juanc.casaabierta_prograavanzada.MyGLRenderer;
import com.juanc.casaabierta_prograavanzada.MyGLSurfaceView;


public class MainActivity extends AppCompatActivity {

    private MyGLSurfaceView glView;
    private MyGLRenderer renderer;

    // ---- Sonidos de click para los botones (en vez de musica de fondo) ----
    private SoundPool soundPool;
    private int clickSoundId = -1;   // boton de luz / recentrar
    private int arrowSoundId = -1;   // las 4 flechas del D-pad
    private boolean clickSoundLoaded = false;
    private boolean arrowSoundLoaded = false;

    // ---- Repeticion continua de las flechas del D-pad mientras se mantienen
    // presionadas (en vez de un solo paso por tap). ----
    private final Handler dpadHandler = new Handler(Looper.getMainLooper());
    private Runnable dpadRepeatRunnable;
    private static final long DPAD_REPEAT_DELAY_MS = 60L;

    // ---- Giroscopio / sensor de rotacion: inclina el modelo 3D segun la
    // inclinacion fisica del telefono (efecto "ventana al mundo 3D"). ----
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private boolean gyroBaselineSet = false;
    private float baseAzimuthDeg = 0f;
    private float basePitchDeg = 0f;
    private static final float MAX_TILT = 80f; // limite de inclinacion, para no "voltear" la escena

    private final float[] rotationMatrix = new float[9];
    private final float[] remappedMatrix = new float[9];
    private final float[] orientationValues = new float[3];

    private final SensorEventListener rotationListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            // Remapea los ejes como si el telefono se sostuviera parado (pantalla
            // mirando al usuario), que es como se usa aca: como una ventanita 3D.
            SensorManager.remapCoordinateSystem(rotationMatrix,
                    SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix);
            SensorManager.getOrientation(remappedMatrix, orientationValues);

            float azimuthDeg = (float) Math.toDegrees(orientationValues[0]);
            float pitchDeg = (float) Math.toDegrees(orientationValues[1]);

            // Se toma como "referencia" la orientacion del telefono en el primer
            // dato que llega, para que el modelo arranque derecho sin importar
            // hacia donde estaba apuntando el telefono al abrir la app.
            if (!gyroBaselineSet) {
                baseAzimuthDeg = azimuthDeg;
                basePitchDeg = pitchDeg;
                gyroBaselineSet = true;
            }

            float relativeYaw = normalizeAngle(azimuthDeg - baseAzimuthDeg);
            float relativePitch = clamp(pitchDeg - basePitchDeg, -MAX_TILT, MAX_TILT);

            if (renderer != null) {
                renderer.mAngleX = relativeYaw;   // inclinar el telefono a los lados -> gira el modelo
                renderer.mAngleY = relativePitch; // inclinar el telefono adelante/atras -> lo tilta
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };

    private static float normalizeAngle(float angle) {
        while (angle > 180f) angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glView = new MyGLSurfaceView(this);
        renderer = new MyGLRenderer();
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Contenedor: la escena 3D de fondo + el panel de controles (boton
        // de luz y slider del cono) flotando encima.
        FrameLayout root = new FrameLayout(this);
        root.addView(glView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        View controls = LayoutInflater.from(this).inflate(R.layout.activity_main, root, false);
        root.addView(controls);
        setContentView(root);

        setupLightControls(controls);
        setupMovementControls(controls);

        // Sonidos de click para los botones.
        // IMPORTANTE: debes agregar 2 archivos de audio cortos en res/raw:
        // "click" (boton de luz / recentrar) y "arrow" (las 4 flechas).
        // Ver instrucciones en el chat.
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status != 0) return;
            if (sampleId == clickSoundId) clickSoundLoaded = true;
            if (sampleId == arrowSoundId) arrowSoundLoaded = true;
        });
        clickSoundId = soundPool.load(this, R.raw.click, 1);
        arrowSoundId = soundPool.load(this, R.raw.click, 1);
    }

    /** Reproduce el sonido de click (luz / recentrar), si ya termino de cargar. */
    private void playClickSound() {
        if (soundPool != null && clickSoundLoaded) {
            soundPool.play(clickSoundId, 1f, 1f, 1, 0, 1f);
        }
    }

    /** Reproduce el sonido de las flechas del D-pad, si ya termino de cargar. */
    private void playArrowSound() {
        if (soundPool != null && arrowSoundLoaded) {
            soundPool.play(arrowSoundId, 1f, 1f, 1, 0, 1f);
        }
    }

    /**
     * Conecta las 4 flechas del D-pad con MyGLRenderer.moveUp/Down/Left/Right().
     * Usa OnTouchListener (no OnClickListener) para poder repetir el movimiento
     * mientras el dedo se mantiene presionado sobre el boton, no solo en el tap.
     */
    private void setupMovementControls(View controls) {
        ImageButton btnUp = controls.findViewById(R.id.btnMoveUp);
        ImageButton btnDown = controls.findViewById(R.id.btnMoveDown);
        ImageButton btnLeft = controls.findViewById(R.id.btnMoveLeft);
        ImageButton btnRight = controls.findViewById(R.id.btnMoveRight);

        setupHoldToRepeat(btnUp, () -> renderer.moveUp());
        setupHoldToRepeat(btnDown, () -> renderer.moveDown());
        setupHoldToRepeat(btnLeft, () -> renderer.moveLeft());
        setupHoldToRepeat(btnRight, () -> renderer.moveRight());
    }

    /** Ejecuta `action` una vez al presionar, y sigue repitiendo cada DPAD_REPEAT_DELAY_MS mientras se mantenga apretado. */
    private void setupHoldToRepeat(View button, Runnable action) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    playArrowSound();
                    dpadRepeatRunnable = new Runnable() {
                        @Override
                        public void run() {
                            action.run();
                            dpadHandler.postDelayed(this, DPAD_REPEAT_DELAY_MS);
                        }
                    };
                    dpadHandler.post(dpadRepeatRunnable);
                    v.setPressed(true);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (dpadRepeatRunnable != null) {
                        dpadHandler.removeCallbacks(dpadRepeatRunnable);
                        dpadRepeatRunnable = null;
                    }
                    v.setPressed(false);
                    v.performClick();
                    return true;

                default:
                    return false;
            }
        });
    }

    /** Conecta el boton on/off y el SeekBar del cono de luz con el renderer. */
    private void setupLightControls(View controls) {
        Button btnToggleLight = controls.findViewById(R.id.btnToggleLight);
        SeekBar seekSpotAngle = controls.findViewById(R.id.seekSpotAngle);
        Button btnCenterFigure = controls.findViewById(R.id.btnCenterFigure);

        // Boton: recentra la figura. Deshace el zoom (pellizco) y el paneo, y ademas
        // vuelve a tomar la inclinacion actual del telefono como "derecho", por si el
        // giroscopio quedo desviado.
        btnCenterFigure.setOnClickListener(v -> {
            playClickSound();
            if (renderer != null) {
                renderer.centrarFigura();
            }
            gyroBaselineSet = false;
        });

        // Boton: prender/apagar la luz.
        btnToggleLight.setOnClickListener(v -> {
            playClickSound();
            renderer.isLightOn = !renderer.isLightOn;
            btnToggleLight.setText(renderer.isLightOn ? "Apagar luz" : "Prender luz");
        });

        // Slider: tamaño del cono de luz, entre MIN_SPOT_ANGLE y MAX_SPOT_ANGLE.
        int range = (int) (MyGLRenderer.MAX_SPOT_ANGLE - MyGLRenderer.MIN_SPOT_ANGLE);
        seekSpotAngle.setMax(range);
        seekSpotAngle.setProgress((int) (renderer.spotlightAngle - MyGLRenderer.MIN_SPOT_ANGLE));
        seekSpotAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    renderer.spotlightAngle = MyGLRenderer.MIN_SPOT_ANGLE + progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glView != null) glView.onResume();

        // Recalibra el "derecho" del modelo cada vez que se reanuda la app,
        // usando la orientacion actual del telefono como punto de partida.
        gyroBaselineSet = false;
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(rotationListener, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glView != null) glView.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(rotationListener);
        dpadHandler.removeCallbacksAndMessages(null);
        dpadRepeatRunnable = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}