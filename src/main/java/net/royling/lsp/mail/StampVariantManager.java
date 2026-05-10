package net.royling.lsp.mail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.royling.lsp.LetterSignalPhone;

import java.io.Reader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StampVariantManager implements ResourceManagerReloadListener {
    public static final StampVariantManager INSTANCE = new StampVariantManager();
    private static final String PATH = LetterSignalPhone.MODID + "/stamps";
    private Map<Identifier, StampVariant> variants = defaultVariants();

    private StampVariantManager() {
    }

    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "stamps"), INSTANCE);
    }

    public Collection<StampVariant> variants() {
        return variants.values();
    }

    public StampVariant byId(Identifier id) {
        return variants.getOrDefault(id, variants.get(StampData.DEFAULT_VARIANT));
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        variants = load(manager);
    }

    private Map<Identifier, StampVariant> load(ResourceManager manager) {
        Map<Identifier, StampVariant> loaded = defaultVariants();
        Map<Identifier, Resource> resources = manager.listResources(PATH, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier variantId = variantId(resourceId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String nameKey = string(json, "name_key", string(json, "name", "stamp." + variantId.getNamespace() + "." + variantId.getPath()));
                Identifier itemModel = Identifier.parse(string(json, "item_model", variantId.toString()));
                Identifier guiTexture = Identifier.parse(string(json, "gui_texture", variantId.getNamespace() + ":textures/stamp/" + variantId.getPath() + ".png"));
                loaded.put(variantId, new StampVariant(variantId, nameKey, itemModel, guiTexture));
            } catch (Exception exception) {
                LetterSignalPhone.LOGGER.warn("Failed to load stamp variant {}", resourceId, exception);
            }
        }
        return loaded;
    }

    private static Identifier variantId(Identifier resourceId) {
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

    private static Map<Identifier, StampVariant> defaultVariants() {
        Map<Identifier, StampVariant> defaults = new LinkedHashMap<>();
        defaults.put(StampData.DEFAULT_VARIANT, new StampVariant(
                StampData.DEFAULT_VARIANT,
                "stamp.letter_signal_phone.default",
                Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "stamp"),
                StampData.DEFAULT_GUI_TEXTURE
        ));
        return defaults;
    }
}
