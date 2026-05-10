package net.royling.lsp.telegraph;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class TelegraphData {
    private static final String MESSAGE = "lsp_telegraph_message";

    private TelegraphData() {
    }

    public static String getMessage(ItemStack stack) {
        return customTag(stack).getStringOr(MESSAGE, "");
    }

    public static void setMessage(ItemStack stack, String message) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(MESSAGE, message));
    }

    public static boolean hasMessage(ItemStack stack) {
        return !getMessage(stack).isBlank();
    }

    private static CompoundTag customTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
