package net.royling.lsp.mail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.registry.ModItems;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MessageInBottleLetterPool implements ResourceManagerReloadListener {
    public static final MessageInBottleLetterPool INSTANCE = new MessageInBottleLetterPool();
    private static final String PATH = LetterSignalPhone.MODID + "/message_in_bottle_letters";
    private List<BottleLetter> letters = defaultLetters();

    private MessageInBottleLetterPool() {
    }

    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "message_in_bottle_letters"), INSTANCE);
    }

    public ItemStack randomLetter(RandomSource random) {
        if (letters.isEmpty()) {
            return ItemStack.EMPTY;
        }
        BottleLetter letter = letters.get(random.nextInt(letters.size()));
        List<StampVariant> variants = List.copyOf(StampVariantManager.INSTANCE.variants());
        StampVariant stamp = variants.isEmpty()
                ? StampVariantManager.INSTANCE.byId(StampData.DEFAULT_VARIANT)
                : variants.get(random.nextInt(variants.size()));
        ItemStack stack = new ItemStack(ModItems.LETTER.get());
        LetterData.seal(stack, letter.text(), stamp.id().toString(), stamp.id().toString(), stamp.guiTexture().toString(), letter.signer(), "");
        return stack;
    }

    public ItemStack randomLetter(ServerPlayer player) {
        RandomSource random = player.getRandom();
        MessageInBottleSavedData data = MessageInBottleSavedData.get(player.level().getServer());
        int playerLetterCount = data.eligiblePlayerLetterCount(player.getUUID().toString());
        int total = letters.size() + playerLetterCount;
        if (total <= 0) {
            return ItemStack.EMPTY;
        }
        if (playerLetterCount > 0 && random.nextInt(total) < playerLetterCount) {
            ItemStack letter = data.consumeRandomPlayerLetter(player.getUUID().toString(), random);
            if (!letter.isEmpty()) {
                return letter;
            }
        }
        return randomLetter(random);
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        letters = load(manager);
    }

    private List<BottleLetter> load(ResourceManager manager) {
        List<BottleLetter> loaded = new ArrayList<>();
        Map<Identifier, Resource> resources = manager.listResources(PATH, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonArray()) {
                    addLetters(loaded, root.getAsJsonArray());
                } else if (root.isJsonObject()) {
                    JsonObject object = root.getAsJsonObject();
                    JsonElement entries = object.get("letters");
                    if (entries instanceof JsonArray array) {
                        addLetters(loaded, array);
                    } else {
                        addLetter(loaded, object);
                    }
                }
            } catch (Exception exception) {
                LetterSignalPhone.LOGGER.warn("Failed to load message in bottle letters {}", entry.getKey(), exception);
            }
        }
        return loaded.isEmpty() ? defaultLetters() : List.copyOf(loaded);
    }

    private static void addLetters(List<BottleLetter> loaded, JsonArray array) {
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                addLetter(loaded, element.getAsJsonObject());
            }
        }
    }

    private static void addLetter(List<BottleLetter> loaded, JsonObject object) {
        String text = string(object, "text", "");
        String signer = string(object, "signer", "");
        if (!text.isBlank() && !signer.isBlank()) {
            loaded.add(new BottleLetter(text, signer));
        }
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element == null ? fallback : element.getAsString();
    }

    private static List<BottleLetter> defaultLetters() {
        return List.of(
                new BottleLetter("The tide carried this farther than I ever walked. If you found it, please remember the sea for me.", "A Stranger"),
                new BottleLetter("I left before sunrise and forgot to say goodbye. Some part of me is still waving from the shore.", "Noah"),
                new BottleLetter("A bottle is a poor mailbox, but the ocean has never lost patience.", "Mira")
        );
    }

    private record BottleLetter(String text, String signer) {
    }
}
