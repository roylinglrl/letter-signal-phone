package net.royling.lsp.mail;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public record StampVariant(Identifier id, String nameKey, Identifier itemModel, Identifier guiTexture) {
    public Component name() {
        return Component.translatable(nameKey);
    }
}
