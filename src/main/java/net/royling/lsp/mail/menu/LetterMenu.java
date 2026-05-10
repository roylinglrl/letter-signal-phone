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
import net.royling.lsp.mail.LetterData;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.mail.StampVariantManager;
import net.royling.lsp.registry.ModItems;
import net.royling.lsp.registry.ModMenus;

public class LetterMenu extends AbstractContainerMenu {
    private static final int LETTER_SLOT = 0;
    private static final int STAMP_SLOT = 1;
    private static final int PLAYER_INV_START = 2;
    private final SimpleContainer container;
    private final ServerPlayer serverPlayer;
    private final InteractionHand hand;

    public LetterMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(2), null, InteractionHand.MAIN_HAND);
    }

    public LetterMenu(int id, Inventory inventory, Player player, InteractionHand hand) {
        this(id, inventory, new SimpleContainer(2), player instanceof ServerPlayer serverPlayer ? serverPlayer : null, hand);
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.LETTER.get())) {
            container.setItem(LETTER_SLOT, held.copyWithCount(1));
        }
    }

    private LetterMenu(int id, Inventory inventory, SimpleContainer container, ServerPlayer serverPlayer, InteractionHand hand) {
        super(ModMenus.LETTER.get(), id);
        this.container = container;
        this.serverPlayer = serverPlayer;
        this.hand = hand;
        addSlot(new FilteredSlot(container, LETTER_SLOT, -1000, -1000, stack -> stack.is(ModItems.LETTER.get()), 1));
        addSlot(new FilteredSlot(container, STAMP_SLOT, 44, 27, stack -> stack.is(ModItems.STAMP.get()), 1));
        addStandardInventorySlots(inventory, 17, 130);
    }

    public ItemStack letter() {
        if (serverPlayer != null) {
            ItemStack held = serverPlayer.getItemInHand(hand);
            if (held.is(ModItems.LETTER.get())) {
                return held;
            }
        }
        return container.getItem(LETTER_SLOT);
    }

    public void saveText(String text) {
        ItemStack letter = letter();
        if (letter.is(ModItems.LETTER.get()) && !LetterData.isReadOnly(letter)) {
            LetterData.setText(letter, text);
            refreshLetterSlot(letter);
            syncHeldLetter(letter);
        }
    }

    public void seal(String text) {
        ItemStack letter = letter();
        ItemStack stamp = container.getItem(STAMP_SLOT);
        if (letter.is(ModItems.LETTER.get()) && !LetterData.isReadOnly(letter) && stamp.is(ModItems.STAMP.get())) {
            String stampId = BuiltInRegistries.ITEM.getKey(stamp.getItem()).toString();
            String signer = serverPlayer == null ? "" : serverPlayer.getName().getString();
            stamp.shrink(1);
            LetterData.seal(letter, text, stampId, StampData.variant(stamp).toString(), StampData.guiTexture(stamp).toString(), StampData.rarity(stamp), StampData.foilEffect(stamp), signer, serverPlayer == null ? "" : serverPlayer.getUUID().toString());
            refreshLetterSlot(letter);
            syncHeldLetter(letter);
        }
    }

    public ItemStack removeStamp() {
        ItemStack stamp = container.removeItem(STAMP_SLOT, 1);
        container.setChanged();
        broadcastChanges();
        return stamp;
    }

    public void unseal(Player player) {
        ItemStack letter = letter();
        if (!letter.is(ModItems.LETTER.get()) || !LetterData.isSealed(letter)) {
            return;
        }
        String stampVariant = LetterData.getStampVariant(letter);
        String stampRarity = LetterData.getStampRarity(letter);
        String stampFoilEffect = LetterData.getStampFoilEffect(letter);
        LetterData.unseal(letter);
        ItemStack stamp = StampData.stackFor(StampVariantManager.INSTANCE.byId(StampData.DEFAULT_VARIANT));
        if (!stampVariant.isEmpty()) {
            stamp = StampData.stackFor(StampVariantManager.INSTANCE.byId(net.minecraft.resources.Identifier.parse(stampVariant)));
        }
        StampData.setRarityAndFoil(stamp, stampRarity, stampFoilEffect);
        ItemStack existing = container.getItem(STAMP_SLOT);
        if (existing.isEmpty()) {
            container.setItem(STAMP_SLOT, stamp);
        } else if (!stamp.isEmpty() && !player.addItem(stamp)) {
            player.drop(stamp, false);
        }
        refreshLetterSlot(letter);
        syncHeldLetter(letter);
    }

    private void refreshLetterSlot(ItemStack letter) {
        container.setItem(LETTER_SLOT, letter.copy());
        container.setChanged();
        broadcastChanges();
    }

    private void syncHeldLetter(ItemStack letter) {
        if (serverPlayer != null && serverPlayer.getItemInHand(hand).is(ModItems.LETTER.get())) {
            serverPlayer.setItemInHand(hand, letter);
            serverPlayer.getInventory().setChanged();
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
        } else if (original.is(ModItems.STAMP.get())) {
            if (!moveItemStackTo(original, STAMP_SLOT, STAMP_SLOT + 1, false)) {
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
            ItemStack stamp = container.removeItemNoUpdate(STAMP_SLOT);
            if (!stamp.isEmpty() && !player.addItem(stamp)) {
                player.drop(stamp, false);
            }
            container.setItem(LETTER_SLOT, ItemStack.EMPTY);
        }
    }
}
