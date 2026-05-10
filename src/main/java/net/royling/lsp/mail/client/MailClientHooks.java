package net.royling.lsp.mail.client;

import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.royling.lsp.mail.menu.LetterScreen;
import net.royling.lsp.mail.menu.MailboxScreen;
import net.royling.lsp.mail.network.MailPayloads;
import net.royling.lsp.registry.ModBlocks;
import net.royling.lsp.registry.ModParticles;

public final class MailClientHooks {
    private static BlockPos mailboxPos;
    private static boolean mailboxHasMail;
    private static int noticeCooldown;

    private MailClientHooks() {
    }

    public static void openLetter(MailPayloads.OpenLetterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof LetterScreen screen) {
                screen.loadLetter(payload.text(), payload.readOnly(), payload.signer(), payload.stamp(), payload.signerUuid(), payload.stampGuiTexture(), payload.stampFoilEffect());
            }
        });
    }

    public static void mailboxStatus(MailPayloads.MailboxStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof MailboxScreen screen) {
                if (payload.arg().isEmpty()) {
                    screen.setStatus(net.minecraft.network.chat.Component.translatable(payload.key()));
                } else {
                    screen.setStatus(net.minecraft.network.chat.Component.translatable(payload.key(), payload.arg()));
                }
            }
        });
    }

    public static void mailboxNotice(MailPayloads.MailboxNoticePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            mailboxPos = payload.active() ? new BlockPos(payload.x(), payload.y(), payload.z()) : null;
            mailboxHasMail = payload.active() && payload.hasMail();
            noticeCooldown = 0;
        });
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || mailboxPos == null || !mailboxHasMail) {
            return;
        }
        if (!minecraft.level.getBlockState(mailboxPos).is(ModBlocks.MAILBOX.get())) {
            mailboxPos = null;
            mailboxHasMail = false;
            return;
        }
        if (noticeCooldown-- > 0) {
            return;
        }
        noticeCooldown = 9;
        minecraft.level.addParticle(
                ModParticles.MAILBOX_NOTICE.get(),
                mailboxPos.getX() + 0.5D,
                mailboxPos.getY() + 1.45D,
                mailboxPos.getZ() + 0.5D,
                0.0D,
                0.0D,
                0.0D
        );
    }
}
