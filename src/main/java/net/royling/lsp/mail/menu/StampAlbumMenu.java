package net.royling.lsp.mail.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.royling.lsp.mail.StampAlbumData;
import net.royling.lsp.registry.ModItems;
import net.royling.lsp.registry.ModMenus;

public class StampAlbumMenu extends AbstractContainerMenu {
    public static final int PAGE_SLOTS = StampAlbumData.STAMPS_PER_PAGE;
    private static final int PLAYER_INV_START = PAGE_SLOTS;
    private static final int PLAYER_INVENTORY_X = 27;
    private static final int PLAYER_INVENTORY_Y = 163;
    private final SimpleContainer stamps = new SimpleContainer(StampAlbumData.SLOTS);
    private final ServerPlayer serverPlayer;
    private final InteractionHand hand;
    private int page;

    public StampAlbumMenu(int id, Inventory inventory) {
        this(id, inventory, null, InteractionHand.MAIN_HAND);
    }

    public StampAlbumMenu(int id, Inventory inventory, ServerPlayer serverPlayer, InteractionHand hand) {
        super(ModMenus.STAMP_ALBUM.get(), id);
        this.serverPlayer = serverPlayer;
        this.hand = hand;
        loadFromAlbum();
        for (int i = 0; i < PAGE_SLOTS; i++) {
            addSlot(new AlbumSlot(stamps, i, stampX(i) + 8, stampY(i) + 8, this));
        }
        addStandardInventorySlots(inventory, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
    }

    public int page() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, Math.min(StampAlbumData.PAGES - 1, page));
        broadcastChanges();
    }

    public void previousPage() {
        setPage(page - 1);
    }

    public void nextPage() {
        setPage(page + 1);
    }

    public ItemStack displayStack(int localSlot) {
        int slot = globalSlot(localSlot);
        return slot >= 0 && slot < stamps.getContainerSize() ? stamps.getItem(slot) : ItemStack.EMPTY;
    }

    public int globalSlot(int localSlot) {
        return page * PAGE_SLOTS + localSlot;
    }

    public static int stampX(int localSlot) {
        int half = localSlot / 12;
        int inHalf = localSlot % 12;
        return (half == 0 ? 8 : 109) + (inHalf % 3) * 33;
    }

    public static int stampY(int localSlot) {
        int inHalf = localSlot % 12;
        return 11 + (inHalf / 3) * 33;
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
            if (!moveOneStampToAlbum(original)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        }
        slot.setChanged();
        saveToAlbum();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return serverPlayer == null || player.getItemInHand(hand).is(ModItems.STAMP_ALBUM.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            saveToAlbum();
        }
    }

    private boolean moveOneStampToAlbum(ItemStack stack) {
        for (int i = 0; i < stamps.getContainerSize(); i++) {
            if (stamps.getItem(i).isEmpty()) {
                ItemStack copy = stack.copyWithCount(1);
                stamps.setItem(i, copy);
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private void loadFromAlbum() {
        if (serverPlayer == null) {
            return;
        }
        ItemStack[] loaded = StampAlbumData.loadAll(serverPlayer.getItemInHand(hand));
        for (int i = 0; i < loaded.length; i++) {
            stamps.setItem(i, loaded[i]);
        }
    }

    private void saveToAlbum() {
        if (serverPlayer == null) {
            return;
        }
        ItemStack album = serverPlayer.getItemInHand(hand);
        if (!album.is(ModItems.STAMP_ALBUM.get())) {
            return;
        }
        ItemStack[] saved = new ItemStack[StampAlbumData.SLOTS];
        for (int i = 0; i < saved.length; i++) {
            saved[i] = stamps.getItem(i).copy();
        }
        StampAlbumData.saveAll(album, saved);
        serverPlayer.setItemInHand(hand, album);
        serverPlayer.getInventory().setChanged();
    }

    private static final class AlbumSlot extends Slot {
        private final StampAlbumMenu menu;
        private final int localSlot;

        private AlbumSlot(Container container, int localSlot, int x, int y, StampAlbumMenu menu) {
            super(container, localSlot, x, y);
            this.localSlot = localSlot;
            this.menu = menu;
        }

        @Override
        public ItemStack getItem() {
            return container.getItem(menu.globalSlot(localSlot));
        }

        @Override
        public void set(ItemStack stack) {
            container.setItem(menu.globalSlot(localSlot), normalize(stack));
            setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            return container.removeItem(menu.globalSlot(localSlot), amount);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModItems.STAMP.get());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        private static ItemStack normalize(ItemStack stack) {
            if (stack.isEmpty() || !stack.is(ModItems.STAMP.get())) {
                return ItemStack.EMPTY;
            }
            return stack.copyWithCount(1);
        }
    }
}
