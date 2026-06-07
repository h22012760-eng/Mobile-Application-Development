package com.puzzleverse.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

public class SoundManager {

    // Sound effect IDs
    private static final int SOUND_SNAP    = 0;
    private static final int SOUND_WIN     = 1;
    private static final int SOUND_TAP     = 2;
    private static final int SOUND_PEEK    = 3;
    private static final int TOTAL_SOUNDS  = 4;

    private SoundPool soundPool;
    private MediaPlayer musicPlayer;
    private int[] soundIds;
    private boolean[] soundLoaded;

    private final GamePreferences prefs;
    private final Context context;

    private static SoundManager instance;

    public static SoundManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new SoundManager(ctx.getApplicationContext());
        }
        return instance;
    }

    private SoundManager(Context context) {
        this.context    = context;
        this.prefs      = new GamePreferences(context);
        this.soundIds   = new int[TOTAL_SOUNDS];
        this.soundLoaded = new boolean[TOTAL_SOUNDS];

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            for (int i = 0; i < TOTAL_SOUNDS; i++) {
                if (soundIds[i] == sampleId && status == 0) {
                    soundLoaded[i] = true;
                }
            }
        });

        loadSounds();
    }

    // ─── Load sounds from raw folder ──────────────────────────────────
    private void loadSounds() {
        // Only load if the raw resource exists — safe fallback if missing
        soundIds[SOUND_SNAP] = loadIfExists(R.raw.sound_snap);
        soundIds[SOUND_WIN]  = loadIfExists(R.raw.sound_win);
        soundIds[SOUND_TAP]  = loadIfExists(R.raw.sound_tap);
        soundIds[SOUND_PEEK] = loadIfExists(R.raw.sound_peek);
    }

    private int loadIfExists(int resId) {
        try {
            return soundPool.load(context, resId, 1);
        } catch (Exception e) {
            return 0;
        }
    }

    // ─── Play sound effects ───────────────────────────────────────────
    public void playSnap() {
        playSound(SOUND_SNAP);
    }

    public void playWin() {
        playSound(SOUND_WIN);
    }

    public void playTap() {
        playSound(SOUND_TAP);
    }

    public void playPeek() {
        playSound(SOUND_PEEK);
    }

    private void playSound(int index) {
        if (!prefs.isSoundEnabled()) return;
        if (soundIds[index] == 0) return;
        if (!soundLoaded[index]) return;
        soundPool.play(soundIds[index], 1f, 1f, 1, 0, 1f);
    }

    // ─── Background music ─────────────────────────────────────────────
    public void startMusic() {
        if (!prefs.isMusicEnabled()) return;
        try {
            if (musicPlayer == null) {
                musicPlayer = MediaPlayer.create(context, R.raw.music_background);
                if (musicPlayer != null) {
                    musicPlayer.setLooping(true);
                    musicPlayer.setVolume(0.4f, 0.4f);
                }
            }
            if (musicPlayer != null && !musicPlayer.isPlaying()) {
                musicPlayer.start();
            }
        } catch (Exception e) {
            // Music file missing — silent fallback
        }
    }

    public void pauseMusic() {
        try {
            if (musicPlayer != null && musicPlayer.isPlaying()) {
                musicPlayer.pause();
            }
        } catch (Exception e) { /* ignore */ }
    }

    public void resumeMusic() {
        if (!prefs.isMusicEnabled()) return;
        try {
            if (musicPlayer != null && !musicPlayer.isPlaying()) {
                musicPlayer.start();
            }
        } catch (Exception e) { /* ignore */ }
    }

    public void stopMusic() {
        try {
            if (musicPlayer != null) {
                musicPlayer.stop();
                musicPlayer.release();
                musicPlayer = null;
            }
        } catch (Exception e) { /* ignore */ }
    }

    // Called when settings change at runtime
    public void refreshSettings() {
        if (prefs.isMusicEnabled()) {
            resumeMusic();
        } else {
            pauseMusic();
        }
    }

    public void release() {
        stopMusic();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        instance = null;
    }
}