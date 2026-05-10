package net.royling.lsp.mail.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.royling.lsp.mail.MailItemCodec;
import net.royling.lsp.mail.MailSavedData;
import net.royling.lsp.registry.ModMenus;

import java.util.ArrayList;
import java.util.List;

public class MailboxMenu extends AbstractContainerMenu {
    private static final int SEND_SLOT = 0;
    private static final int MAIL_START = 1;
    private static final int MAIL_SLOTS = 16;
    private static final int PLAYER_INV_START = MAIL_START + MAIL_SLOTS;
    private final SimpleContainer container;
    private final ServerPlayer serverPlayer;
    private final List<String> overflowMail = new ArrayList<>();

    public MailboxMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MAIL_START + MAIL_SLOTS), null);
    }

    public MailboxMenu(int id, Inventory inventory, ServerPlayer player) {
        this(id, inventory, new SimpleContainer(MAIL_START + MAIL_SLOTS), player);
        MailSavedData data = MailSavedData.get(player.level().getServer());
        List<String> mails = data.inboxSnapshot(player.getUUID());
        int visibleCount = Math.min(mails.size(), MAIL_SLOTS);
        for (int i = 0; i < visibleCount; i++) {
            container.setItem(MAIL_START + i, MailItemCodec.decodeMail(mails.get(i)));
        }
        if (mails.size() > visibleCount) {
            overflowMail.addAll(mails.subList(visibleCount, mails.size()));
        }
        data.refreshMailboxState(player.level().getServer(), player.getUUID());
    }

    private MailboxMenu(int id, Inventory inventory, SimpleContainer container, ServerPlayer serverPlayer) {
        super(ModMenus.MAILBOX.get(), id);
        this.container = container;
        this.serverPlayer = serverPlayer;
        addSlot(new FilteredSlot(container, SEND_SLOT, 14, 27, MailItemCodec::canSend, 1));
        for (int i = 0; i < MAIL_SLOTS; i++) {
            int col = i % 4;
            int row = i / 4;
            addSlot(new FilteredSlot(container, MAIL_START + i, 107 + col * 18, 34 + row * 18, stack -> false, 64));
        }
        addStandardInventorySlots(inventory, 17, 130);
    }

    public Status checkRecipient(String recipientName) {
        if (serverPlayer == null || recipientName == null || recipientName.isBlank()) {
            return new Status("message.letter_signal_phone.mailbox.recipient_empty", "");
        }
        String recipient = recipientName.trim();
        MailSavedData data = MailSavedData.get(serverPlayer.level().getServer());
        return data.findOwnerByName(recipient).isPresent()
                ? new Status("message.letter_signal_phone.mailbox.recipient_ready", recipient)
                : new Status("message.letter_signal_phone.mailbox.no_recipient", recipient);
    }

    public Status sendTo(String recipientName) {
        Status recipientStatus = checkRecipient(recipientName);
        if (!recipientStatus.key().equals("message.letter_signal_phone.mailbox.recipient_ready")) {
            return recipientStatus;
        }
        ItemStack stack = container.getItem(SEND_SLOT);
        if (!MailItemCodec.canSend(stack)) {
            return new Status("message.letter_signal_phone.mailbox.need_mail", "");
        }
        MailSavedData data = MailSavedData.get(serverPlayer.level().getServer());
        data.findOwnerByName(recipientName.trim()).ifPresent(owner -> {
            data.send(owner, MailItemCodec.encodeMail(stack), serverPlayer.level().getServer().getTickCount() + 1200);
            container.setItem(SEND_SLOT, ItemStack.EMPTY);
            broadcastChanges();
        });
        return new Status("message.letter_signal_phone.mailbox.sent", "");
    }

    public record Status(String key, String arg) {
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
        } else if (MailItemCodec.canSend(original)) {
            if (!moveItemStackTo(original, SEND_SLOT, SEND_SLOT + 1, false)) {
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
        if (!player.level().isClientSide() && serverPlayer != null) {
            ItemStack sendStack = container.getItem(SEND_SLOT);
            if (!sendStack.isEmpty()) {
                container.setItem(SEND_SLOT, ItemStack.EMPTY);
                if (!player.addItem(sendStack)) {
                    player.drop(sendStack, false);
                }
            }

            List<String> remaining = new ArrayList<>();
            for (int i = 0; i < MAIL_SLOTS; i++) {
                ItemStack mail = container.getItem(MAIL_START + i);
                if (!mail.isEmpty()) {
                    remaining.add(MailItemCodec.encodeMail(mail));
                    container.setItem(MAIL_START + i, ItemStack.EMPTY);
                }
            }
            remaining.addAll(overflowMail);

            MailSavedData data = MailSavedData.get(serverPlayer.level().getServer());
            data.replaceInbox(serverPlayer.getUUID(), remaining);
            data.refreshMailboxState(serverPlayer.level().getServer(), serverPlayer.getUUID());
        }
    }
}
