package net.royling.lsp.phone.data;

import net.royling.lsp.LetterSignalPhone;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

public class PhoneSavedData extends SavedData {
    private static final int MAX_NUMBER = 9999;
    private static final String SEPARATOR = "\\|";

    public static final Codec<PhoneSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("numbers").forGetter(data -> data.numberOwners)
    ).apply(instance, PhoneSavedData::new));

    public static final SavedDataType<PhoneSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "phone_numbers"),
            PhoneSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<String, String> numberOwners = new HashMap<>();

    public PhoneSavedData() {
    }

    private PhoneSavedData(Map<String, String> numberOwners) {
        this.numberOwners.putAll(numberOwners);
    }

    public static PhoneSavedData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Registration> register(UUID owner, long now) {
        if (numberOwners.size() > MAX_NUMBER) {
            return Optional.empty();
        }

        for (int attempt = 0; attempt < 64; attempt++) {
            String number = format(ThreadLocalRandom.current().nextInt(MAX_NUMBER + 1));
            if (!numberOwners.containsKey(number)) {
                return Optional.of(assign(number, owner, now));
            }
        }

        for (int value = 0; value <= MAX_NUMBER; value++) {
            String number = format(value);
            if (!numberOwners.containsKey(number)) {
                return Optional.of(assign(number, owner, now));
            }
        }

        return Optional.empty();
    }

    public boolean unregister(String number) {
        if (numberOwners.remove(number) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean exists(String number) {
        return numberOwners.containsKey(number);
    }

    public Optional<UUID> ownerOf(String number) {
        Entry entry = entry(number);
        if (entry == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(entry.ownerId));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public boolean matches(String number, String token) {
        Entry entry = entry(number);
        return entry != null && !token.isEmpty() && token.equals(entry.token);
    }

    public void touchIfCurrent(String number, String token, long now) {
        Entry entry = entry(number);
        if (entry != null && token.equals(entry.token) && entry.lastSeen != now) {
            numberOwners.put(number, encode(entry.ownerId, entry.token, now));
            setDirty();
        }
    }

    public void reclaimStale(long now, long maxInactiveTicks) {
        boolean changed = numberOwners.entrySet().removeIf(entry -> {
            Entry value = parse(entry.getValue());
            return value != null && value.lastSeen > 0 && now - value.lastSeen > maxInactiveTicks;
        });
        if (changed) {
            setDirty();
        }
    }

    private Registration assign(String number, UUID owner, long now) {
        String token = UUID.randomUUID().toString();
        numberOwners.put(number, encode(owner.toString(), token, now));
        setDirty();
        return new Registration(number, token);
    }

    private Entry entry(String number) {
        return parse(numberOwners.get(number));
    }

    private static String encode(String ownerId, String token, long lastSeen) {
        return ownerId + "|" + token + "|" + lastSeen;
    }

    private static Entry parse(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        String[] parts = encoded.split(SEPARATOR, -1);
        if (parts.length == 1) {
            return new Entry(parts[0], "", 0L);
        }
        if (parts.length < 3) {
            return null;
        }
        try {
            return new Entry(parts[0], parts[1], Long.parseLong(parts[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String format(int value) {
        return String.format("%04d", value);
    }

    public record Registration(String number, String token) {
    }

    private record Entry(String ownerId, String token, long lastSeen) {
    }
}
