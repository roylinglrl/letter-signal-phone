package net.royling.lsp.mail;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ThrowableMessageInBottleData {
    public static final String LETTER = "lsp_bottle_letter";
    public static final String OWNER = "lsp_bottle_owner";
    public static final String OWNER_NAME = "lsp_bottle_owner_name";

    private ThrowableMessageInBottleData() {
    }

    public static ItemStack pack(ItemStack bottle, ItemStack letter, String ownerId, String ownerName) {
        ItemStack packed = bottle.copyWithCount(1);
        String encodedLetter = MailItemCodec.packedStack(letter.copyWithCount(1)).encodedStack();
        CustomData.update(DataComponents.CUSTOM_DATA, packed, tag -> {
            tag.putString(LETTER, encodedLetter);
            tag.putString(OWNER, ownerId);
            tag.putString(OWNER_NAME, ownerName);
        });
        return packed;
    }

    public static boolean hasLetter(ItemStack stack) {
        return !tag(stack).getStringOr(LETTER, "").isBlank() && !tag(stack).getStringOr(OWNER, "").isBlank();
    }

    public static String encodedLetter(ItemStack stack) {
        return tag(stack).getStringOr(LETTER, "");
    }

    public static String owner(ItemStack stack) {
        return tag(stack).getStringOr(OWNER, "");
    }

    public static String ownerName(ItemStack stack) {
        return tag(stack).getStringOr(OWNER_NAME, "");
    }

    public static ItemStack letter(ItemStack stack) {
        String encoded = encodedLetter(stack);
        if (encoded.isBlank()) {
            return ItemStack.EMPTY;
        }
        return new MailItemCodec.PackedStack(encoded, 1).toStack();
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
