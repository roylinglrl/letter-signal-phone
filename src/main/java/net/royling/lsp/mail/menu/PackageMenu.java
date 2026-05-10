package net.royling.lsp.mail.menu;

import net.minecraft.core.registries.BuiltInRegistries;
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

public class PackageMenu extends AbstractContainerMenu {
    private static final int CONTENT_SLOTS = 4;
    private static final int PLAYER_INV_START = CONTENT_SLOTS;
    private final SimpleContainer container;
    private final ServerPlayer serverPlayer;
    private final InteractionHand hand;
    private boolean unpacked;

    public PackageMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(CONTENT_SLOTS), null, InteractionHand.MAIN_HAND);
    }

    public PackageMenu(int id, Inventory inventory, ServerPlayer player, InteractionHand hand) {
        this(id, inventory, new SimpleContainer(CONTENT_SLOTS), player, hand);
        ItemStack parcel = player.getItemInHand(hand);
        if (parcel.is(ModItems.PACKAGE.get())) {
            int index = 0;
            for (MailItemCodec.PackedStack packed : MailItemCodec.decodePackedStacks(PackageData.getItems(parcel))) {
                if (index >= CONTENT_SLOTS) {
                    break;
                }
                container.setItem(index++, packed.toStack());
            }
            parcel.shrink(1);
        }
    }

    private PackageMenu(int id, Inventory inventory, SimpleContainer container, ServerPlayer serverPlayer, InteractionHand hand) {
        super(ModMenus.PACKAGE.get(), id);
        this.container = container;
        this.serverPlayer = serverPlayer;
        this.hand = hand;
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            addSlot(new FilteredSlot(container, i, 53 + i * 18, 32, stack -> !stack.is(ModItems.PACKAGE.get()), 64));
        }
        addStandardInventorySlots(inventory, 8, 90);
    }

    public void unpack() {
        unpacked = true;
        if (serverPlayer == null) {
            return;
        }
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            ItemStack stack = container.removeItemNoUpdate(i);
            if (!stack.isEmpty() && !serverPlayer.addItem(stack)) {
                serverPlayer.drop(stack, false);
            }
        }
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
        } else if (!original.is(ModItems.PACKAGE.get())) {
            if (!moveItemStackTo(original, 0, CONTENT_SLOTS, false)) {
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
        if (player.level().isClientSide()) {
            return;
        }
        if (unpacked) {
            clearContainer(player, container);
            return;
        }
        List<MailItemCodec.PackedStack> stacks = new ArrayList<>();
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                stacks.add(new MailItemCodec.PackedStack(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount()));
                container.setItem(i, ItemStack.EMPTY);
            }
        }
        ItemStack parcel = new ItemStack(ModItems.PACKAGE.get());
        PackageData.setItems(parcel, MailItemCodec.encodePackedStacks(stacks));
        if (!player.addItem(parcel)) {
            player.drop(parcel, false);
        }
    }
}
