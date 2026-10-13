package com.poker.view;

import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

/** Short, cached effects played only by the JavaFX client. */
public final class SoundEffects {
    public enum Sound {
        DEAL("deal.wav", 0.8), FLIP("flip.wav", 0.55), CHECK("check.wav", 0.7),
        CALL("call.wav", 0.75), BET_RAISE("bet_raise.wav", 0.75), FOLD("fold.wav", 0.75),
        ALL_IN("all_in.wav", 0.8), PAYOUT("payout.wav", 0.65),
        WIN("win.wav", 0.42), LOSE("lose.wav", 0.45),
        YOUR_TURN("your_turn.wav", 0.7), COUNTDOWN("countdown.wav", 0.65),
        BUTTON_CLICK("button_click.wav", 0.45);

        private final String file;
        private final double volume;

        Sound(String file, double volume) {
            this.file = file;
            this.volume = volume;
        }
    }

    private static final Map<Sound, AudioClip> clips = new EnumMap<>(Sound.class);

    private SoundEffects() { }

    public static void play(Sound sound) {
        try {
            AudioClip clip = clips.get(sound);
            if (clip == null) {
                URL resource = SoundEffects.class.getResource("/assets/sounds/" + sound.file);
                if (resource == null) {
                    System.err.println("Missing sound: " + sound.file);
                    return;
                }
                clip = new AudioClip(resource.toExternalForm());
                clips.put(sound, clip);
            }
            clip.play(sound.volume);
        } catch (RuntimeException error) {
            System.err.println("Cannot play sound " + sound.file + ": " + error.getMessage());
        }
    }

    public static void stop(Sound sound) {
        AudioClip clip = clips.get(sound);
        if (clip != null) clip.stop();
    }
}
