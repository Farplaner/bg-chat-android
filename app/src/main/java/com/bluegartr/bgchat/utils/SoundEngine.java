package com.bluegartr.bgchat.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.bluegartr.bgchat.R;

/**
 * Created by Daniel on 8/1/14.
 */
public class SoundEngine {
    private boolean ready = false;

    private SoundPool soundPool;
    private int soundID, soundID2;

    public SoundEngine(Context context) {
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i2) {
                ready = true;
            }
        });
        soundID = soundPool.load(context, R.raw.shoryuken, 1);
        soundID2 = soundPool.load(context, R.raw.alert, 1);
    }

    public void shoryuken(float volume) {
        if (ready) {
            soundPool.play(soundID, volume, volume, 1, 0, 1f);
        }
    }

    public void alert(float volume) {
        if (ready) {
            soundPool.play(soundID2, volume, volume, 1, 0, 1f);
        }
    }

    public boolean isReady() {
        return ready;
    }
}
