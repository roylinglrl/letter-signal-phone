package net.royling.lsp.mail;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.royling.lsp.registry.ModItems;

public final class StampPackData {
    public static final String PACK = "lsp_stamp_pack";
    public static final Identifier DEFAULT_PACK = Identifier.fromNamespaceAndPath(net.royling.lsp.LetterSignalPhone.MODID, "default");

    private StampPackData() {
    }

    public static ItemStack stackFor(StampPackDefinition pack) {
        ItemStack stack = new ItemStack(ModItems.STAMP_PACK.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(PACK, pack.id().toString()));
        stack.set(DataComponents.CUSTOM_NAME, pack.name().copy().withStyle(style -> style.withItalic(false)));
        stack.set(DataComponents.ITEM_MODEL, pack.itemModel());
        return stack;
    }

    public static Identifier packId(ItemStack stack) {
        String value = tag(stack).getStringOr(PACK, DEFAULT_PACK.toString());
        return Identifier.parse(value);
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
