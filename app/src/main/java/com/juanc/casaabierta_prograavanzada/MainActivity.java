package com.juanc.casaabierta_prograavanzada;

import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private MyGLSurfaceView glView;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glView = new MyGLSurfaceView(this);
        MyGLRenderer renderer = new MyGLRenderer();
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setContentView(glView);

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
