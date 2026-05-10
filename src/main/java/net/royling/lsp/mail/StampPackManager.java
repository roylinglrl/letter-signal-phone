package net.royling.lsp.mail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.royling.lsp.LetterSignalPhone;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StampPackManager implements ResourceManagerReloadListener {
    public static final StampPackManager INSTANCE = new StampPackManager();
    private static final String PATH = LetterSignalPhone.MODID + "/stamp_packs";
    private static final Identifier DEFAULT_ITEM_MODEL = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "stamp_pack");
    private Map<Identifier, StampPackDefinition> packs = defaultPacks();

    private StampPackManager() {
    }

    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "stamp_packs"), INSTANCE);
    }

    public Collection<StampPackDefinition> packs() {
        return packs.values();
    }

    public StampPackDefinition byId(Identifier id) {
        return packs.getOrDefault(id, packs.get(StampPackData.DEFAULT_PACK));
    }

    public StampVariant randomStamp(StampPackDefinition pack, RandomSource random) {
        int totalWeight = 0;
        for (StampPackDefinition.Entry entry : pack.entries()) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return StampVariantManager.INSTANCE.byId(StampData.DEFAULT_VARIANT);
        }
        int target = random.nextInt(totalWeight);
        for (StampPackDefinition.Entry entry : pack.entries()) {
            target -= Math.max(0, entry.weight());
            if (target < 0) {
                return StampVariantManager.INSTANCE.byId(entry.stampId());
            }
        }
        return StampVariantManager.INSTANCE.byId(StampData.DEFAULT_VARIANT);
    }

    public StampPackDefinition.RarityEntry randomRarity(StampPackDefinition pack, RandomSource random) {
        float roll = random.nextFloat();
        float cursor = 0.0F;
        for (StampPackDefinition.RarityEntry entry : pack.rarities()) {
            cursor += Math.max(0.0F, entry.chance());
            if (roll < cursor) {
                return entry;
            }
        }
        return null;
    }

    public StampPackDefinition.RarityEntry guaranteedRarity(StampPackDefinition pack, RandomSource random) {
        float totalChance = totalRarityChance(pack);
        if (totalChance <= 0.0F) {
            return null;
        }
        float target = random.nextFloat() * totalChance;
        for (StampPackDefinition.RarityEntry entry : pack.rarities()) {
            target -= Math.max(0.0F, entry.chance());
            if (target < 0.0F) {
                return entry;
            }
        }
        return pack.rarities().isEmpty() ? null : pack.rarities().get(0);
    }

    public float totalRarityChance(StampPackDefinition pack) {
        float total = 0.0F;
        for (StampPackDefinition.RarityEntry entry : pack.rarities()) {
            total += Math.max(0.0F, entry.chance());
        }
        return Math.min(1.0F, total);
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        packs = load(manager);
    }

    private Map<Identifier, StampPackDefinition> load(ResourceManager manager) {
        Map<Identifier, StampPackDefinition> loaded = new LinkedHashMap<>();
        Map<Identifier, Resource> resources = manager.listResources(PATH, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier packId = packId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loaded.put(packId, parse(packId, json));
            } catch (Exception exception) {
                LetterSignalPhone.LOGGER.warn("Failed to load stamp pack {}", entry.getKey(), exception);
            }
        }
        return loaded.isEmpty() ? defaultPacks() : loaded;
    }

    private static StampPackDefinition parse(Identifier packId, JsonObject json) {
        String nameKey = string(json, "name_key", "stamp_pack." + packId.getNamespace() + "." + packId.getPath());
        Identifier itemModel = Identifier.parse(string(json, "item_model", DEFAULT_ITEM_MODEL.toString()));
        int stampsPerPack = Math.max(1, integer(json, "stamps_per_pack", 3));
        int guaranteedRareOpens = Math.max(1, integer(json, "guaranteed_rare_opens", 100));
        List<StampPackDefinition.Entry> entries = entries(json);
        if (entries.isEmpty()) {
            entries = List.of(new StampPackDefinition.Entry(StampData.DEFAULT_VARIANT, 1));
        }
        List<StampPackDefinition.RarityEntry> rarities = rarities(json);
        if (rarities.isEmpty()) {
            rarities = defaultRarities();
        }
        return new StampPackDefinition(packId, nameKey, itemModel, stampsPerPack, guaranteedRareOpens, entries, rarities);
    }

    private static List<StampPackDefinition.Entry> entries(JsonObject json) {
        List<StampPackDefinition.Entry> entries = new ArrayList<>();
        JsonElement stamps = json.get("stamps");
        if (!(stamps instanceof JsonArray array)) {
            return entries;
        }
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                entries.add(new StampPackDefinition.Entry(Identifier.parse(element.getAsString()), 1));
            } else if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                Identifier stampId = Identifier.parse(string(object, "id", StampData.DEFAULT_VARIANT.toString()));
                int weight = Math.max(0, integer(object, "weight", 1));
                entries.add(new StampPackDefinition.Entry(stampId, weight));
            }
        }
        return List.copyOf(entries);
    }

    private static List<StampPackDefinition.RarityEntry> rarities(JsonObject json) {
        List<StampPackDefinition.RarityEntry> rarities = new ArrayList<>();
        JsonElement rarityElement = json.get("rarities");
        if (!(rarityElement instanceof JsonArray array)) {
            return rarities;
        }
        float usedChance = 0.0F;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String rarity = string(object, "rarity", StampData.RARITY_RARE);
            String foilEffect = string(object, "foil_effect", defaultFoilFor(rarity));
            float remaining = Math.max(0.0F, 1.0F - usedChance);
            float chance = Math.min(remaining, chance(object, "chance", 0.0F));
            if (chance > 0.0F && !StampData.RARITY_COMMON.equals(rarity)) {
                rarities.add(new StampPackDefinition.RarityEntry(rarity, foilEffect, chance));
                usedChance += chance;
            }
        }
        return List.copyOf(rarities);
    }

    private static String defaultFoilFor(String rarity) {
        return switch (rarity) {
            case StampData.RARITY_HIGH_RARE -> StampData.FOIL_COLOR_CRYSTAL;
            case StampData.RARITY_UNIQUE_RARE -> StampData.FOIL_DIAMOND;
            case StampData.RARITY_RGB_RARE -> StampData.FOIL_RGB_SHIFT;
            default -> StampData.FOIL_HOLOGRAPHIC_STRIPES;
        };
    }

    private static Identifier packId(Identifier resourceId) {
        String path = resourceId.getPath();
        String prefix = PATH + "/";
        String suffix = ".json";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(suffix)) {
            path = path.substring(0, path.length() - suffix.length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element == null ? fallback : element.getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        return element == null ? fallback : element.getAsInt();
    }

    private static float chance(JsonObject json, String key, float fallback) {
        JsonElement element = json.get(key);
        float value = element == null ? fallback : element.getAsFloat();
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static Map<Identifier, StampPackDefinition> defaultPacks() {
        Map<Identifier, StampPackDefinition> defaults = new LinkedHashMap<>();
        defaults.put(StampPackData.DEFAULT_PACK, new StampPackDefinition(
                StampPackData.DEFAULT_PACK,
                "stamp_pack.letter_signal_phone.default",
                DEFAULT_ITEM_MODEL,
                3,
                100,
                List.of(new StampPackDefinition.Entry(StampData.DEFAULT_VARIANT, 1)),
                defaultRarities()
        ));
        return defaults;
    }

    private static List<StampPackDefinition.RarityEntry> defaultRarities() {
        return List.of(
                new StampPackDefinition.RarityEntry(StampData.RARITY_RARE, StampData.FOIL_HOLOGRAPHIC_STRIPES, 0.002F),
                new StampPackDefinition.RarityEntry(StampData.RARITY_HIGH_RARE, StampData.FOIL_COLOR_CRYSTAL, 0.0015F),
                new StampPackDefinition.RarityEntry(StampData.RARITY_RGB_RARE, StampData.FOIL_RGB_SHIFT, 0.001F),
                new StampPackDefinition.RarityEntry(StampData.RARITY_UNIQUE_RARE, StampData.FOIL_DIAMOND, 0.0005F)
        );
    }
}
