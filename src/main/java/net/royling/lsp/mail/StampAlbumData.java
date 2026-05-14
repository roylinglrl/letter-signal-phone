package net.royling.lsp.mail;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.royling.lsp.registry.ModItems;

public final class StampAlbumData {
    public static final int PAGES = 10;
    public static final int STAMPS_PER_PAGE = 24;
    public static final int SLOTS = PAGES * STAMPS_PER_PAGE;
    private static final String STAMPS = "lsp_stamp_album_stamps";
    private static final String SLOT = "slot";
    private static final String STACK = "stack";

    private StampAlbumData() {
    }

    public static ItemStack get(ItemStack album, int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = tag(album);
        ListTag stamps = tag.getListOrEmpty(STAMPS);
        for (Tag entryTag : stamps) {
            if (!(entryTag instanceof CompoundTag entry) || entry.getIntOr(SLOT, -1) != slot) {
                continue;
            }
            Tag stackTag = entry.get(STACK);
            if (stackTag == null) {
                return ItemStack.EMPTY;
            }
            return ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, stackTag).result().orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    public static void set(ItemStack album, int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, album, tag -> {
            ListTag stamps = tag.getListOrEmpty(STAMPS);
            ListTag updated = new ListTag();
            for (Tag entryTag : stamps) {
                if (entryTag instanceof CompoundTag entry && entry.getIntOr(SLOT, -1) != slot) {
                    updated.add(entry.copy());
                }
            }
            ItemStack copy = normalizedStamp(stack);
            if (!copy.isEmpty()) {
                ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, copy).result().ifPresent(stackTag -> {
                    CompoundTag entry = new CompoundTag();
                    entry.putInt(SLOT, slot);
                    entry.put(STACK, stackTag);
                    updated.add(entry);
                });
            }
            tag.put(STAMPS, updated);
        });
    }

    public static ItemStack[] loadAll(ItemStack album) {
        ItemStack[] stacks = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            stacks[i] = ItemStack.EMPTY;
        }
        CompoundTag tag = tag(album);
        for (Tag entryTag : tag.getListOrEmpty(STAMPS)) {
            if (!(entryTag instanceof CompoundTag entry)) {
                continue;
            }
            int slot = entry.getIntOr(SLOT, -1);
            Tag stackTag = entry.get(STACK);
            if (!validSlot(slot) || stackTag == null) {
                continue;
            }
            ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, stackTag).result().orElse(ItemStack.EMPTY);
            stacks[slot] = normalizedStamp(stack);
        }
        return stacks;
    }

    public static void saveAll(ItemStack album, ItemStack[] stacks) {
        CustomData.update(DataComponents.CUSTOM_DATA, album, tag -> {
            ListTag saved = new ListTag();
            for (int i = 0; i < Math.min(stacks.length, SLOTS); i++) {
                ItemStack stack = normalizedStamp(stacks[i]);
                if (stack.isEmpty()) {
                    continue;
                }
                final int slot = i;
                ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).result().ifPresent(stackTag -> {
                    CompoundTag entry = new CompoundTag();
                    entry.putInt(SLOT, slot);
                    entry.put(STACK, stackTag);
                    saved.add(entry);
                });
            }
            tag.put(STAMPS, saved);
        });
    }

    private static ItemStack normalizedStamp(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.STAMP.get())) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOTS;
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
