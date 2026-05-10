package net.royling.lsp.mail.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.royling.lsp.mail.MailItemCodec;
import net.royling.lsp.mail.PackageData;
import net.royling.lsp.registry.ModItems;
import net.royling.lsp.registry.ModMenus;

import java.util.ArrayList;
import java.util.List;

public class PackingMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOTS = 4;
    private static final int PLAYER_INV_START = INPUT_SLOTS;
    private final SimpleContainer container;
    private final ServerPlayer serverPlayer;
    private final InteractionHand hand;

    public PackingMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(INPUT_SLOTS), null, InteractionHand.MAIN_HAND);
    }

    public PackingMenu(int id, Inventory inventory, ServerPlayer player, InteractionHand hand) {
        this(id, inventory, new SimpleContainer(INPUT_SLOTS), player, hand);
    }

    private PackingMenu(int id, Inventory inventory, SimpleContainer container, ServerPlayer serverPlayer, InteractionHand hand) {
        super(ModMenus.PACKING.get(), id);
        this.container = container;
        this.serverPlayer = serverPlayer;
        this.hand = hand;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            int x = 26 + (i % 2) * 18;
            int y = 20 + (i / 2) * 18;
            addSlot(new FilteredSlot(container, i, x, y, stack -> !stack.is(ModItems.PACKAGE.get()) && !stack.is(ModItems.PACKING_BOX.get()), 64));
        }
        addStandardInventorySlots(inventory, 8, 84);
    }

    public void pack() {
        if (serverPlayer == null || !serverPlayer.getItemInHand(hand).is(ModItems.PACKING_BOX.get())) {
            return;
        }
        List<MailItemCodec.PackedStack> stacks = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                stacks.add(MailItemCodec.packedStack(stack));
                container.setItem(i, ItemStack.EMPTY);
            }
        }
        if (stacks.isEmpty()) {
            return;
        }
        serverPlayer.getItemInHand(hand).shrink(1);
        ItemStack parcel = new ItemStack(ModItems.PACKAGE.get());
        PackageData.setItems(parcel, MailItemCodec.encodePackedStacks(stacks));
        if (!serverPlayer.addItem(parcel)) {
            serverPlayer.drop(parcel, false);
        }
        serverPlayer.closeContainer();
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
        } else if (!original.is(ModItems.PACKAGE.get()) && !original.is(ModItems.PACKING_BOX.get())) {
            if (!moveItemStackTo(original, 0, INPUT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            clearContainer(player, container);
        }
    }
}
