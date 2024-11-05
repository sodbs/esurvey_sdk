package com.esurvey.esurvey_sdk_demo;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import java.io.IOException;

/**
 * @author jokerliang
 * 播放语音提示
 */
public class TipsSoundsService {
    private final SoundPool soundPool;
    private final AssetManager assetManager;
    private int voiceId = 0;

    private static  TipsSoundsService instance = null;

    public static TipsSoundsService init(Context context) {
        if (instance == null) {
            instance = new TipsSoundsService(context);
        }
        return instance;
    }

    public static TipsSoundsService getInstance() {
        return instance;
    }







    private TipsSoundsService(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setMaxStreams(1000);
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            builder.setAudioAttributes(attrBuilder.build());
            builder.setMaxStreams(1);
            soundPool = builder.build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        }

        assetManager = context.getAssets();
        soundPool.setOnLoadCompleteListener((soundPool, i, status) -> {
            if (status == 0) {
                soundPool.play(voiceId, 1f, 1f, 1, 0, 1f);
                // soundPool.release()
            }
        });
    }



    public void playJtSounds(int type) {
        playSound("jt_" + type+".mp3");
    }



    private void playSound(String resourceName) {
        try {
            voiceId = soundPool.load(assetManager.openFd( resourceName), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
