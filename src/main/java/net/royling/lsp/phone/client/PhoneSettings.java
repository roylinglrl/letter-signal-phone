package net.royling.lsp.phone.client;

import net.royling.lsp.LetterSignalPhone;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class PhoneSettings {
    private static final float MIN_VOLUME = 0.0F;
    private static final float MAX_VOLUME = 2.0F;
    private static String microphoneName = "";
    private static float inputVolume = 1.0F;
    private static float outputVolume = 1.0F;
    private static float ringtoneVolume = 1.0F;
    private static boolean loaded;

    private PhoneSettings() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Properties properties = new Properties();
        Path path = path();
        if (Files.isRegularFile(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            } catch (IOException exception) {
                LetterSignalPhone.LOGGER.warn("Unable to load phone settings", exception);
            }
        }
        microphoneName = properties.getProperty("microphone", "");
        inputVolume = parseVolume(properties.getProperty("inputVolume"), 1.0F);
        outputVolume = parseVolume(properties.getProperty("outputVolume"), 1.0F);
        ringtoneVolume = parseVolume(properties.getProperty("ringtoneVolume"), 1.0F);
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("microphone", microphoneName);
        properties.setProperty("inputVolume", Float.toString(inputVolume));
        properties.setProperty("outputVolume", Float.toString(outputVolume));
        properties.setProperty("ringtoneVolume", Float.toString(ringtoneVolume));
        try {
            Files.createDirectories(path().getParent());
            try (var writer = Files.newBufferedWriter(path())) {
                properties.store(writer, "Letter,Signal,Phone client settings");
            }
        } catch (IOException exception) {
            LetterSignalPhone.LOGGER.warn("Unable to save phone settings", exception);
        }
    }

    public static List<String> microphones() {
        List<String> names = new ArrayList<>();
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, null);
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.isLineSupported(targetInfo)) {
                names.add(info.getName());
            }
        }
        return names;
    }

    public static Mixer.Info selectedMicrophone() {
        load();
        if (microphoneName.isEmpty()) {
            return null;
        }
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(microphoneName)) {
                return info;
            }
        }
        return null;
    }

    public static String microphoneLabel() {
        load();
        return microphoneName.isEmpty() ? "Default" : microphoneName;
    }

    public static boolean usesDefaultMicrophone() {
        load();
        return microphoneName.isEmpty();
    }

    public static void cycleMicrophone() {
        load();
        List<String> names = microphones();
        if (names.isEmpty()) {
            microphoneName = "";
        } else {
            int index = names.indexOf(microphoneName);
            microphoneName = names.get((index + 1) % names.size());
        }
        save();
    }

    public static float inputVolume() {
        load();
        return inputVolume;
    }

    public static float outputVolume() {
        load();
        return outputVolume;
    }

    public static float ringtoneVolume() {
        load();
        return ringtoneVolume;
    }

    public static void adjustInputVolume(float delta) {
        inputVolume = clamp(inputVolume() + delta);
        save();
    }

    public static void adjustOutputVolume(float delta) {
        outputVolume = clamp(outputVolume() + delta);
        save();
    }

    public static void adjustRingtoneVolume(float delta) {
        ringtoneVolume = clamp(ringtoneVolume() + delta);
        save();
    }

    private static float parseVolume(String value, float fallback) {
        try {
            return clamp(Float.parseFloat(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float clamp(float value) {
        return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, value));
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("letter_signal_phone").resolve("phone_settings.properties");
    }
}
