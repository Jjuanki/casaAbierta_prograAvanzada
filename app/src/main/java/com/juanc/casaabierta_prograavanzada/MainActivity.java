package com.juanc.casaabierta_prograavanzada;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glView = new MyGLSurfaceView(this);
        renderer = new MyGLRenderer();
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glView != null) glView.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
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
