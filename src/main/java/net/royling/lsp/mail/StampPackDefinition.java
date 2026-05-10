package net.royling.lsp.mail;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public record StampPackDefinition(
        Identifier id,
        String nameKey,
        Identifier itemModel,
        int stampsPerPack,
        int guaranteedRareOpens,
        List<Entry> entries,
        List<RarityEntry> rarities
) {
    public Component name() {
        return Component.translatable(nameKey);
    }

    public record Entry(Identifier stampId, int weight) {
    }

    public record RarityEntry(String rarity, String foilEffect, float chance) {
    }
}
