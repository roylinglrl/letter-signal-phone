package net.royling.lsp.telegraph.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.telegraph.menu.TelegraphMenu;

public final class TelegraphPayloads {
    private TelegraphPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("telegraph");
        registrar.playToServer(TelegraphButtonPayload.TYPE, TelegraphButtonPayload.STREAM_CODEC, TelegraphPayloads::handleButton);
    }

    private static void handleButton(TelegraphButtonPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof TelegraphMenu menu) {
                if (payload.action() == TelegraphAction.CONFIRM) {
                    menu.confirmFrequency();
                } else {
                    menu.press(payload.action().menuAction(), player);
                }
            }
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, path);
    }

    public record TelegraphButtonPayload(TelegraphAction action) implements CustomPacketPayload {
        public static final Type<TelegraphButtonPayload> TYPE = new Type<>(id("telegraph_button"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TelegraphButtonPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.action().name(), 16),
                buf -> new TelegraphButtonPayload(TelegraphAction.valueOf(buf.readUtf(16)))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public enum TelegraphAction {
        CONFIRM(null),
        DOT(TelegraphMenu.Action.DOT),
        DASH(TelegraphMenu.Action.DASH),
        SPACE(TelegraphMenu.Action.SPACE),
        START(TelegraphMenu.Action.START),
        END(TelegraphMenu.Action.END);

        private final TelegraphMenu.Action menuAction;

        TelegraphAction(TelegraphMenu.Action menuAction) {
            this.menuAction = menuAction;
        }

        TelegraphMenu.Action menuAction() {
            return menuAction;
        }
    }
}
