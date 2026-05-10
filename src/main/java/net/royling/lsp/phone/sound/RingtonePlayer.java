package net.royling.lsp.phone.sound;

import net.royling.lsp.registry.ModSounds;
import net.royling.lsp.LetterSignalPhone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.royling.lsp.phone.client.PhoneSettings;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class RingtonePlayer {
    private static final String CUSTOM_RINGTONE_PATH = "config/letter_signal_phone/ringtone.wav";
    private static final List<Clip> ACTIVE_CLIPS = new ArrayList<>();
    private static SoundInstance activeSound;

    private RingtonePlayer() {
    }

    public static void play() {
        stop();
        if (playCustomWav()) {
            return;
        }
        activeSound = SimpleSoundInstance.forUI(ModSounds.DEFAULT_RING.get(), 1.0F, PhoneSettings.ringtoneVolume());
        Minecraft.getInstance().getSoundManager().play(activeSound);
    }

    public static void stop() {
        if (activeSound != null) {
            Minecraft.getInstance().getSoundManager().stop(activeSound);
            activeSound = null;
        }

        synchronized (ACTIVE_CLIPS) {
            for (Clip clip : ACTIVE_CLIPS) {
                clip.stop();
                clip.close();
            }
            ACTIVE_CLIPS.clear();
        }
    }

    private static boolean playCustomWav() {
        File file = new File(Minecraft.getInstance().gameDirectory, CUSTOM_RINGTONE_PATH);
        if (!file.isFile()) {
            return false;
        }

        Thread thread = new Thread(() -> {
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                applyClipVolume(clip, PhoneSettings.ringtoneVolume());
                synchronized (ACTIVE_CLIPS) {
                    ACTIVE_CLIPS.add(clip);
                }
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        synchronized (ACTIVE_CLIPS) {
                            ACTIVE_CLIPS.remove(event.getLine());
                        }
                        event.getLine().close();
                    }
                });
                clip.start();
            } catch (Exception exception) {
                LetterSignalPhone.LOGGER.warn("Unable to play custom ringtone {}", file.getAbsolutePath(), exception);
            }
        }, "LetterSignalPhone Custom Ringtone");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private static void applyClipVolume(Clip clip, float volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float clamped = Math.max(0.0001F, volume);
        float db = 20.0F * (float) Math.log10(clamped);
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
    }
}
