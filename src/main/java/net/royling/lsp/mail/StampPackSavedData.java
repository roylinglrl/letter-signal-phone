package net.royling.lsp.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.royling.lsp.LetterSignalPhone;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StampPackSavedData extends SavedData {
    private static final String SEP = "|";

    public static final Codec<StampPackSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("uses_since_rare").forGetter(data -> data.usesSinceRare)
    ).apply(instance, StampPackSavedData::new));

    public static final SavedDataType<StampPackSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "stamp_packs"),
            StampPackSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<String, Integer> usesSinceRare = new HashMap<>();

    public StampPackSavedData() {
    }

    private StampPackSavedData(Map<String, Integer> usesSinceRare) {
        this.usesSinceRare.putAll(usesSinceRare);
    }

    public static StampPackSavedData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public int recordOpen(UUID playerId, Identifier packId) {
        String key = key(playerId, packId);
        int count = Math.max(0, usesSinceRare.getOrDefault(key, 0)) + 1;
        usesSinceRare.put(key, count);
        setDirty();
        return count;
    }

    public void reset(UUID playerId, Identifier packId) {
        if (usesSinceRare.remove(key(playerId, packId)) != null) {
            setDirty();
        }
    }

    private static String key(UUID playerId, Identifier packId) {
        return playerId + SEP + packId;
    }
}
