package com.juanc.casaabierta_prograavanzada;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private MyGLSurfaceView glView;
    private MyGLRenderer renderer;
    private MediaPlayer mediaPlayer;

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

        // Musica de fondo en loop.
        // IMPORTANTE: debes agregar un archivo de audio en res/raw llamado
        // "background_music" (ej: background_music.mp3) para que esto compile.
        // Ver instrucciones en el chat.
        mediaPlayer = MediaPlayer.create(this, R.raw.danza);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0.5f, 0.5f);
        }
    }

    /** Conecta el boton on/off y el SeekBar del cono de luz con el renderer. */
    private void setupLightControls(View controls) {
        Button btnToggleLight = controls.findViewById(R.id.btnToggleLight);
        SeekBar seekSpotAngle = controls.findViewById(R.id.seekSpotAngle);

        // Boton: prender/apagar la luz.
        btnToggleLight.setOnClickListener(v -> {
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
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) mediaPlayer.start();

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
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
        if (sensorManager != null) sensorManager.unregisterListener(rotationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
