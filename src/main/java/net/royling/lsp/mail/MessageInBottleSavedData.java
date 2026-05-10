package net.royling.lsp.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.royling.lsp.LetterSignalPhone;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageInBottleSavedData extends SavedData {
    public static final Codec<MessageInBottleSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("last_attempt_day").forGetter(data -> data.lastAttemptDay),
            Codec.STRING.listOf().optionalFieldOf("player_letters", List.of()).forGetter(data -> data.playerLetters)
    ).apply(instance, MessageInBottleSavedData::new));
    public static final SavedDataType<MessageInBottleSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "message_in_bottle"),
            MessageInBottleSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private long lastAttemptDay = -1L;
    private final List<String> playerLetters = new ArrayList<>();

    public MessageInBottleSavedData() {
    }

    private MessageInBottleSavedData(long lastAttemptDay, List<String> playerLetters) {
        this.lastAttemptDay = lastAttemptDay;
        this.playerLetters.addAll(playerLetters);
    }

    public static MessageInBottleSavedData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean shouldAttempt(long day) {
        return day > 0L && day % 4L == 0L && lastAttemptDay != day;
    }

    public void markAttempted(long day) {
        lastAttemptDay = day;
        setDirty();
    }

    public void addPlayerLetter(String ownerId, String encodedLetter) {
        if (ownerId == null || ownerId.isBlank() || encodedLetter == null || encodedLetter.isBlank()) {
            return;
        }
        playerLetters.add(UUID.randomUUID() + "|" + ownerId + "|" + encodedLetter);
        setDirty();
    }

    public ItemStack consumeRandomPlayerLetter(String playerId, RandomSource random) {
        List<PlayerBottleLetter> eligible = new ArrayList<>();
        for (String encoded : playerLetters) {
            PlayerBottleLetter letter = parse(encoded);
            if (letter != null && !letter.ownerId().equals(playerId)) {
                eligible.add(letter);
            }
        }
        if (eligible.isEmpty()) {
            return ItemStack.EMPTY;
        }
        PlayerBottleLetter selected = eligible.get(random.nextInt(eligible.size()));
        playerLetters.removeIf(encoded -> {
            PlayerBottleLetter letter = parse(encoded);
            return letter != null && letter.id().equals(selected.id());
        });
        setDirty();
        return new MailItemCodec.PackedStack(selected.encodedLetter(), 1).toStack();
    }

    public int eligiblePlayerLetterCount(String playerId) {
        int count = 0;
        for (String encoded : playerLetters) {
            PlayerBottleLetter letter = parse(encoded);
            if (letter != null && !letter.ownerId().equals(playerId)) {
                count++;
            }
        }
        return count;
    }

    private static PlayerBottleLetter parse(String encoded) {
        if (encoded == null) {
            return null;
        }
        String[] parts = encoded.split("\\|", 3);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            return null;
        }
        return new PlayerBottleLetter(parts[0], parts[1], parts[2]);
    }

    private record PlayerBottleLetter(String id, String ownerId, String encodedLetter) {
    }
}
