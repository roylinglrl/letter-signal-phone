package net.royling.lsp.mail.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.client.MailClientHooks;
import net.royling.lsp.mail.LetterData;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.mail.menu.LetterMenu;
import net.royling.lsp.mail.menu.MailboxMenu;
import net.royling.lsp.mail.menu.PackageMenu;
import net.royling.lsp.mail.menu.PackingMenu;
import net.royling.lsp.mail.menu.StampAlbumMenu;
import net.royling.lsp.registry.ModItems;

public final class MailPayloads {
    private MailPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("mail");
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            registrar.playToClient(OpenLetterPayload.TYPE, OpenLetterPayload.STREAM_CODEC, MailClientHooks::openLetter);
            registrar.playToClient(MailboxStatusPayload.TYPE, MailboxStatusPayload.STREAM_CODEC, MailClientHooks::mailboxStatus);
            registrar.playToClient(MailboxNoticePayload.TYPE, MailboxNoticePayload.STREAM_CODEC, MailClientHooks::mailboxNotice);
        } else {
            registrar.playToClient(OpenLetterPayload.TYPE, OpenLetterPayload.STREAM_CODEC);
            registrar.playToClient(MailboxStatusPayload.TYPE, MailboxStatusPayload.STREAM_CODEC);
            registrar.playToClient(MailboxNoticePayload.TYPE, MailboxNoticePayload.STREAM_CODEC);
        }
        registrar.playToServer(MailboxCheckPayload.TYPE, MailboxCheckPayload.STREAM_CODEC, MailPayloads::handleMailboxCheck);
        registrar.playToServer(MailboxSendPayload.TYPE, MailboxSendPayload.STREAM_CODEC, MailPayloads::handleMailboxSend);
        registrar.playToServer(LetterActionPayload.TYPE, LetterActionPayload.STREAM_CODEC, MailPayloads::handleLetterAction);
        registrar.playToServer(LetterSavePayload.TYPE, LetterSavePayload.STREAM_CODEC, MailPayloads::handleLetterSave);
        registrar.playToServer(LetterSealPayload.TYPE, LetterSealPayload.STREAM_CODEC, MailPayloads::handleLetterSeal);
        registrar.playToServer(PackPayload.TYPE, PackPayload.STREAM_CODEC, MailPayloads::handlePack);
        registrar.playToServer(UnpackPayload.TYPE, UnpackPayload.STREAM_CODEC, MailPayloads::handleUnpack);
        registrar.playToServer(StampAlbumPagePayload.TYPE, StampAlbumPagePayload.STREAM_CODEC, MailPayloads::handleStampAlbumPage);
    }

    private static void handleMailboxSend(MailboxSendPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof MailboxMenu menu) {
                MailboxMenu.Status status = menu.sendTo(payload.recipient());
                PacketDistributor.sendToPlayer(player, new MailboxStatusPayload(status.key(), status.arg()));
            }
        });
    }

    private static void handleMailboxCheck(MailboxCheckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof MailboxMenu menu) {
                MailboxMenu.Status status = menu.checkRecipient(payload.recipient());
                PacketDistributor.sendToPlayer(player, new MailboxStatusPayload(status.key(), status.arg()));
            }
        });
    }

    private static void handleLetterAction(LetterActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof LetterMenu menu) {
                switch (payload.action()) {
                    case SAVE -> {
                        saveMainHandLetter(player, payload.text());
                        player.closeContainer();
                    }
                    case SEAL -> {
                        sealMainHandLetter(player, menu, payload.text());
                        player.closeContainer();
                    }
                    case UNSEAL -> menu.unseal(player);
                }
            }
        });
    }

    private static void handleLetterSave(LetterSavePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                saveMainHandLetter(player, payload.text());
                player.closeContainer();
            }
        });
    }

    private static void handleLetterSeal(LetterSealPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof LetterMenu menu) {
                    sealMainHandLetter(player, menu, payload.text());
                }
                player.closeContainer();
            }
        });
    }

    private static void saveMainHandLetter(ServerPlayer player, String text) {
        ItemStack letter = player.getMainHandItem();
        if (letter.is(ModItems.LETTER.get()) && !LetterData.isReadOnly(letter)) {
            LetterData.setText(letter, text);
            syncMainHand(player, letter);
        }
    }

    private static void sealMainHandLetter(ServerPlayer player, LetterMenu menu, String text) {
        ItemStack letter = player.getMainHandItem();
        if (letter.is(ModItems.LETTER.get()) && !LetterData.isReadOnly(letter)) {
            ItemStack stamp = menu.removeStamp();
            if (stamp.is(ModItems.STAMP.get())) {
                String stampId = BuiltInRegistries.ITEM.getKey(stamp.getItem()).toString();
                LetterData.seal(letter, text, stampId, StampData.variant(stamp).toString(), StampData.guiTexture(stamp).toString(), StampData.rarity(stamp), StampData.foilEffect(stamp), player.getName().getString(), player.getUUID().toString());
                syncMainHand(player, letter);
            } else if (!stamp.isEmpty() && !player.addItem(stamp)) {
                player.drop(stamp, false);
            }
        }
    }

    private static void syncMainHand(ServerPlayer player, ItemStack letter) {
        player.setItemInHand(InteractionHand.MAIN_HAND, letter);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static void handlePack(PackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PackingMenu menu) {
                menu.pack();
            }
        });
    }

    private static void handleUnpack(UnpackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PackageMenu menu) {
                menu.unpack();
            }
        });
    }

    private static void handleStampAlbumPage(StampAlbumPagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof StampAlbumMenu menu) {
                menu.setPage(payload.page());
            }
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, path);
    }

    public static void sendOpenLetter(ServerPlayer player, ItemStack letter) {
        PacketDistributor.sendToPlayer(player, new OpenLetterPayload(
                LetterData.getText(letter),
                LetterData.isReadOnly(letter),
                LetterData.getSigner(letter),
                LetterData.getStamp(letter),
                LetterData.getSignerUuid(letter),
                LetterData.getStampGuiTexture(letter),
                LetterData.getStampRarity(letter),
                LetterData.getStampFoilEffect(letter)
        ));
    }

    public record OpenLetterPayload(String text, boolean readOnly, String signer, String stamp, String signerUuid, String stampGuiTexture, String stampRarity, String stampFoilEffect) implements CustomPacketPayload {
        public static final Type<OpenLetterPayload> TYPE = new Type<>(id("open_letter"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenLetterPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.text(), 512);
                    buf.writeBoolean(payload.readOnly());
                    buf.writeUtf(payload.signer(), 64);
                    buf.writeUtf(payload.stamp(), 128);
                    buf.writeUtf(payload.signerUuid(), 40);
                    buf.writeUtf(payload.stampGuiTexture(), 160);
                    buf.writeUtf(payload.stampRarity(), 32);
                    buf.writeUtf(payload.stampFoilEffect(), 64);
                },
                buf -> new OpenLetterPayload(buf.readUtf(512), buf.readBoolean(), buf.readUtf(64), buf.readUtf(128), buf.readUtf(40), buf.readUtf(160), buf.readUtf(32), buf.readUtf(64))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MailboxSendPayload(String recipient) implements CustomPacketPayload {
        public static final Type<MailboxSendPayload> TYPE = new Type<>(id("mailbox_send"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MailboxSendPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.recipient(), 32),
                buf -> new MailboxSendPayload(buf.readUtf(32))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MailboxCheckPayload(String recipient) implements CustomPacketPayload {
        public static final Type<MailboxCheckPayload> TYPE = new Type<>(id("mailbox_check"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MailboxCheckPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.recipient(), 32),
                buf -> new MailboxCheckPayload(buf.readUtf(32))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MailboxStatusPayload(String key, String arg) implements CustomPacketPayload {
        public static final Type<MailboxStatusPayload> TYPE = new Type<>(id("mailbox_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MailboxStatusPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.key(), 96);
                    buf.writeUtf(payload.arg(), 32);
                },
                buf -> new MailboxStatusPayload(buf.readUtf(96), buf.readUtf(32))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MailboxNoticePayload(int x, int y, int z, boolean active, boolean hasMail) implements CustomPacketPayload {
        public static final Type<MailboxNoticePayload> TYPE = new Type<>(id("mailbox_notice"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MailboxNoticePayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeInt(payload.x());
                    buf.writeInt(payload.y());
                    buf.writeInt(payload.z());
                    buf.writeBoolean(payload.active());
                    buf.writeBoolean(payload.hasMail());
                },
                buf -> new MailboxNoticePayload(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record LetterActionPayload(LetterAction action, String text) implements CustomPacketPayload {
        public static final Type<LetterActionPayload> TYPE = new Type<>(id("letter_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LetterActionPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.action().name(), 16);
                    buf.writeUtf(payload.text(), 512);
                },
                buf -> new LetterActionPayload(LetterAction.valueOf(buf.readUtf(16)), buf.readUtf(512))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record LetterSavePayload(String text) implements CustomPacketPayload {
        public static final Type<LetterSavePayload> TYPE = new Type<>(id("letter_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LetterSavePayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.text(), 512),
                buf -> new LetterSavePayload(buf.readUtf(512))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record LetterSealPayload(String text) implements CustomPacketPayload {
        public static final Type<LetterSealPayload> TYPE = new Type<>(id("letter_seal"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LetterSealPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.text(), 512),
                buf -> new LetterSealPayload(buf.readUtf(512))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public enum LetterAction {
        SAVE,
        SEAL,
        UNSEAL
    }

    public record PackPayload() implements CustomPacketPayload {
        public static final Type<PackPayload> TYPE = new Type<>(id("pack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PackPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                },
                buf -> new PackPayload()
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UnpackPayload() implements CustomPacketPayload {
        public static final Type<UnpackPayload> TYPE = new Type<>(id("unpack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UnpackPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                },
                buf -> new UnpackPayload()
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StampAlbumPagePayload(int page) implements CustomPacketPayload {
        public static final Type<StampAlbumPagePayload> TYPE = new Type<>(id("stamp_album_page"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StampAlbumPagePayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeVarInt(payload.page()),
                buf -> new StampAlbumPagePayload(buf.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
