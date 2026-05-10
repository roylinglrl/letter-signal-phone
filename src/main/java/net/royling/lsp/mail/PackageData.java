package net.royling.lsp.mail;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PackageData {
    public static final String ITEMS = "lsp_package_items";

    private PackageData() {
    }

    public static String getItems(ItemStack stack) {
        return customTag(stack).getStringOr(ITEMS, "");
    }

    public static void setItems(ItemStack stack, String items) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(ITEMS, items));
    }

    public static boolean isPacked(ItemStack stack) {
        return !getItems(stack).isEmpty();
    }

    private static CompoundTag customTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }
}
