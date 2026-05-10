package net.royling.lsp.telegraph.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.royling.lsp.registry.ModMenus;
import net.royling.lsp.telegraph.TelegraphBlockEntity;

import java.util.function.Predicate;

public class TelegraphMenu extends AbstractContainerMenu {
    private static final int PLAYER_INV_START = TelegraphBlockEntity.SLOT_COUNT;
    private final Container container;
    private final TelegraphBlockEntity telegraph;
    private int composingData;

    public TelegraphMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(TelegraphBlockEntity.SLOT_COUNT), null);
    }

    public TelegraphMenu(int id, Inventory inventory, TelegraphBlockEntity telegraph) {
        this(id, inventory, telegraph, telegraph);
    }

    private TelegraphMenu(int id, Inventory inventory, Container container, TelegraphBlockEntity telegraph) {
        super(ModMenus.TELEGRAPH.get(), id);
        this.container = container;
        this.telegraph = telegraph;
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return telegraph != null && telegraph.isComposing() ? 1 : composingData;
            }

            @Override
            public void set(int value) {
                composingData = value;
            }
        });

        addSlot(new FilteredSlot(container, TelegraphBlockEntity.SLOT_FREQ_A, 19, 56, stack -> true, 1));
        addSlot(new FilteredSlot(container, TelegraphBlockEntity.SLOT_FREQ_B, 19, 78, stack -> true, 1));
        addSlot(new FilteredSlot(container, TelegraphBlockEntity.SLOT_SEND_INPUT, 64, 50, stack -> stack.is(Items.PAPER), 64));
        addSlot(new WorkSlot(container, TelegraphBlockEntity.SLOT_WORK, 114, 50));
        addSlot(new FilteredSlot(container, TelegraphBlockEntity.SLOT_RECEIVE_INPUT, 160, 28, stack -> stack.is(Items.PAPER), 64));
        for (int i = 0; i < TelegraphBlockEntity.RECEIVE_OUTPUT_COUNT; i++) {
            int col = i % 2;
            int row = i / 2;
            addSlot(new FilteredSlot(container, TelegraphBlockEntity.SLOT_RECEIVE_OUTPUT_START + i, 150 + col * 20, 54 + row * 20, stack -> false, 64));
        }
        addStandardInventorySlots(inventory, 17, 130);
    }

    public void confirmFrequency() {
        if (telegraph != null) {
            telegraph.confirmFrequency();
        }
    }

    public void press(Action action, ServerPlayer player) {
        if (telegraph == null) {
            return;
        }
        switch (action) {
            case DOT -> telegraph.pressDot();
            case DASH -> telegraph.pressDash();
            case SPACE -> telegraph.pressSpace();
            case START -> telegraph.start();
            case END -> telegraph.finish();
        }
        broadcastChanges();
    }

    public String message() {
        return telegraph == null ? "" : telegraph.message();
    }

    public boolean isComposing() {
        return telegraph != null ? telegraph.isComposing() : composingData != 0;
    }

    public enum Action {
        DOT,
        DASH,
        SPACE,
        START,
        END
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < PLAYER_INV_START) {
            if (!moveItemStackTo(original, PLAYER_INV_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (original.is(Items.PAPER)) {
            if (!moveItemStackTo(original, TelegraphBlockEntity.SLOT_SEND_INPUT, TelegraphBlockEntity.SLOT_SEND_INPUT + 1, false)
                    && !moveItemStackTo(original, TelegraphBlockEntity.SLOT_RECEIVE_INPUT, TelegraphBlockEntity.SLOT_RECEIVE_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(original, TelegraphBlockEntity.SLOT_FREQ_A, TelegraphBlockEntity.SLOT_FREQ_B + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        }
        slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && telegraph != null) {
            telegraph.endUse(player.getUUID());
        }
    }

    private class WorkSlot extends FilteredSlot {
        WorkSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y, stack -> false, 1);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isComposing();
        }
    }

    private static class FilteredSlot extends Slot {
        private final Predicate<ItemStack> filter;
        private final int maxStackSize;

        FilteredSlot(Container container, int slot, int x, int y, Predicate<ItemStack> filter, int maxStackSize) {
            super(container, slot, x, y);
            this.filter = filter;
            this.maxStackSize = maxStackSize;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return filter.test(stack);
        }

        @Override
        public int getMaxStackSize() {
            return maxStackSize;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return maxStackSize;
        }
    }
}
